package org.aion.avm.core;

import org.aion.avm.core.arraywrapping.ArrayWrappingClassAdapter;
import org.aion.avm.core.arraywrapping.ArrayWrappingClassAdapterRef;
import org.aion.avm.core.exceptionwrapping.ExceptionWrapping;
import org.aion.avm.core.instrument.ClassMetering;
import org.aion.avm.core.instrument.HeapMemoryCostCalculator;
import org.aion.avm.core.miscvisitors.ClinitStrippingVisitor;
import org.aion.avm.core.miscvisitors.ConstantVisitor;
import org.aion.avm.core.miscvisitors.InterfaceFieldMappingVisitor;
import org.aion.avm.core.miscvisitors.LoopingExceptionStrippingVisitor;
import org.aion.avm.core.miscvisitors.NamespaceMapper;
import org.aion.avm.core.miscvisitors.PreRenameClassAccessRules;
import org.aion.avm.core.miscvisitors.StrictFPVisitor;
import org.aion.avm.core.miscvisitors.UserClassMappingVisitor;
import org.aion.avm.core.persistence.AutomaticGraphVisitor;
import org.aion.avm.core.persistence.ContractEnvironmentState;
import org.aion.avm.core.persistence.IObjectGraphStore;
import org.aion.avm.core.persistence.LoadedDApp;
import org.aion.avm.core.persistence.ReflectionStructureCodec;
import org.aion.avm.core.persistence.keyvalue.KeyValueObjectGraph;
import org.aion.avm.core.rejection.MainMethodChecker;
import org.aion.avm.core.rejection.RejectedClassException;
import org.aion.avm.core.rejection.RejectionClassVisitor;
import org.aion.avm.core.shadowing.ClassShadowing;
import org.aion.avm.core.shadowing.InvokedynamicShadower;
import org.aion.avm.core.stacktracking.StackWatcherClassAdapter;
import org.aion.avm.core.types.ClassHierarchy;
import org.aion.avm.core.types.ClassHierarchyNode;
import org.aion.avm.core.types.ClassHierarchyVisitorAdapter;
import org.aion.avm.core.types.ClassInfo;
import org.aion.avm.core.types.Forest;
import org.aion.avm.core.types.GeneratedClassConsumer;
import org.aion.avm.core.types.ImmortalDappModule;
import org.aion.avm.core.types.PocClassHierarchy;
import org.aion.avm.core.types.RawDappModule;
import org.aion.avm.core.types.TransformedDappModule;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.DebugNameResolver;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.core.verification.Verifier;
import org.aion.avm.internal.*;
import org.aion.kernel.*;
import org.aion.parallel.TransactionTask;
import org.aion.types.Address;
import org.aion.vm.api.interfaces.KernelInterface;
import org.aion.vm.api.interfaces.TransactionContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.*;


public class DAppCreator {
    /**
     * Returns the sizes of all the user-space classes
     *
     * @param classHierarchy     the class hierarchy
     * @return The look-up map of the sizes of user objects
     * Class name is in the JVM internal name format, see {@link org.aion.avm.core.util.Helpers#fulllyQualifiedNameToInternalName(String)}
     */
    public static Map<String, Integer> computeUserObjectSizes(ClassHierarchy<String, ClassInfo> classHierarchy, Map<String, Integer> rootObjectSizes)
    {
        HeapMemoryCostCalculator objectSizeCalculator = new HeapMemoryCostCalculator();

        // compute the user object sizes
        objectSizeCalculator.calcClassesInstanceSize(classHierarchy, rootObjectSizes);

        // copy over the user object sizes
        Map<String, Integer> userObjectSizes = new HashMap<>();
        objectSizeCalculator.getClassHeapSizeMap().forEach((k, v) -> {
            if (!rootObjectSizes.containsKey(k)) {
                userObjectSizes.put(k, v);
            }
        });
        return userObjectSizes;
    }

    // NOTE:  This is only public because InvokedynamicTransformationTest calls it.
    public static Map<String, Integer> computeAllPostRenameObjectSizes(ClassHierarchy<String, ClassInfo> forest, boolean preserveDebuggability) {
        Map<String, Integer> preRenameUserObjectSizes = computeUserObjectSizes(forest, NodeEnvironment.singleton.preRenameRuntimeObjectSizeMap);

        Map<String, Integer> postRenameObjectSizes = new HashMap<>(NodeEnvironment.singleton.postRenameRuntimeObjectSizeMap);
        preRenameUserObjectSizes.forEach((k, v) -> postRenameObjectSizes.put(DebugNameResolver.getUserPackageSlashPrefix(k, preserveDebuggability), v));
        return postRenameObjectSizes;
    }

    /**
     * Replaces the <code>java.base</code> package with the shadow implementation.
     * Note that this is public since some unit tests call it, directly.
     *
     * @param inputClasses The class of DApp (names specified in .-style)
     * @param preRenameClassHierarchy The pre-rename hierarchy of user-defined classes in the DApp (/-style).
     * @return the transformed classes and any generated classes (names specified in .-style)
     */
    public static Map<String, byte[]> transformClasses(Map<String, byte[]> inputClasses, ClassHierarchy<String, ClassInfo> preRenameClassHierarchy, boolean preserveDebuggability, PocClassHierarchy pocHierarchy) {
        // Before anything, pass the list of classes through the verifier.
        // (this will throw UncaughtException, on verification failure).
        Verifier.verifyUntrustedClasses(inputClasses);
        
        // Note:  preRenameUserDefinedClasses includes ONLY classes while preRenameUserClassAndInterfaceSet includes classes AND interfaces.
        Set<String> preRenameUserDefinedClasses = ClassWhiteList.extractDeclaredClasses(preRenameClassHierarchy);
        IParentPointers parentClassResolver = new ParentPointers(preRenameUserDefinedClasses, preRenameClassHierarchy, preserveDebuggability);
        
        // We need to run our rejection filter and static rename pass.
        Map<String, byte[]> safeClasses = rejectionAndRenameInputClasses(inputClasses, preRenameUserDefinedClasses, parentClassResolver, preserveDebuggability, pocHierarchy);
        
        // merge the generated classes and processed classes, assuming the package spaces do not conflict.
        Map<String, byte[]> processedClasses = new HashMap<>();
        // merge the generated classes and processed classes, assuming the package spaces do not conflict.
        // We also want to expose this type to the class writer so it can compute common superclasses.
        GeneratedClassConsumer generatedClassesSink = (superClassSlashName, classSlashName, bytecode) -> {
            // Note that the processed classes are expected to use .-style names.
            String classDotName = Helpers.internalNameToFulllyQualifiedName(classSlashName);
            processedClasses.put(classDotName, bytecode);
        };
        Map<String, Integer> postRenameObjectSizes = computeAllPostRenameObjectSizes(preRenameClassHierarchy, preserveDebuggability);

        Map<String, byte[]> transformedClasses = new HashMap<>();

        int parsingOptions = preserveDebuggability ? ClassReader.EXPAND_FRAMES : ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG;

        for (String name : safeClasses.keySet()) {
            // Note that transformClasses requires that the input class names by the .-style names.
            RuntimeAssertionError.assertTrue(-1 == name.indexOf("/"));

            // We need to parse with EXPAND_FRAMES, since the StackWatcherClassAdapter uses a MethodNode to parse methods.
            // We also add SKIP_DEBUG since we aren't using debug data and skipping it removes extraneous labels which would otherwise
            // cause the BlockBuildingMethodVisitor to build lots of small blocks instead of a few big ones (each block incurs a Helper
            // static call, which is somewhat expensive - this is how we bill for energy).
            ClassToolchain.Builder builder = new ClassToolchain.Builder(safeClasses.get(name), parsingOptions)
                .addNextVisitor(new ConstantVisitor())
                .addNextVisitor(new ClassMetering(postRenameObjectSizes))
                .addNextVisitor(new InvokedynamicShadower(PackageConstants.kShadowSlashPrefix))
                .addNextVisitor(new ClassShadowing(PackageConstants.kShadowSlashPrefix))
                .addNextVisitor(new StackWatcherClassAdapter())
                .addNextVisitor(new ExceptionWrapping(parentClassResolver, generatedClassesSink))
                .addNextVisitor(new AutomaticGraphVisitor())
                .addNextVisitor(new StrictFPVisitor());

            ClassToolchain.Builder.Creator creator = (pocHierarchy == null)
                ? builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, parentClassResolver))
                : builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, pocHierarchy, preserveDebuggability));

            byte[] bytecode = creator.build().runAndGetBytecode();

            builder = new ClassToolchain.Builder(bytecode, parsingOptions)
                    .addNextVisitor(new ArrayWrappingClassAdapterRef(pocHierarchy, preserveDebuggability))
                    .addNextVisitor(new ArrayWrappingClassAdapter());

            creator = (pocHierarchy == null)
                ? builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, parentClassResolver))
                : builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, pocHierarchy, preserveDebuggability));


            bytecode = creator.build().runAndGetBytecode();
            transformedClasses.put(name, bytecode);
        }

        /*
         * Another pass to deal with static fields in interfaces.
         */
        GeneratedClassConsumer consumer = generatedClassesSink;
        Set<String> userInterfaceSlashNames = new HashSet<>();
        preRenameClassHierarchy.walkPreOrder(new ClassHierarchyVisitorAdapter<>() {
            public void onVisitRoot(ClassHierarchyNode<String, ClassInfo> root) {
                // TODO: we have any interface with fields?
            }
            public void onVisitNotRootNode(ClassHierarchyNode<String, ClassInfo> node) {
                if (node.getContent().isInterface()) {
                    userInterfaceSlashNames.add(Helpers.fulllyQualifiedNameToInternalName(DebugNameResolver.getUserPackageDotPrefix(node.getId(), preserveDebuggability)));
                }
            }
        });
        String javaLangObjectSlashName = PackageConstants.kShadowSlashPrefix + "java/lang/Object";
        for (String name : transformedClasses.keySet()) {
            ClassToolchain.Builder builder = new ClassToolchain.Builder(transformedClasses.get(name), parsingOptions)
                    .addNextVisitor(new InterfaceFieldMappingVisitor(consumer, userInterfaceSlashNames, javaLangObjectSlashName));

            ClassToolchain.Builder.Creator creator = (pocHierarchy != null)
                ? builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, pocHierarchy, preserveDebuggability))
                : builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, parentClassResolver));

            byte[] bytecode = creator.build().runAndGetBytecode();
            processedClasses.put(name, bytecode);
        }

        return processedClasses;
    }

    public static void create(IExternalCapabilities capabilities, KernelInterface kernel, AvmInternal avm, TransactionTask task, TransactionContext ctx, AvmTransactionResult result, boolean preserveDebuggability, boolean verboseErrors) {
        // Expose the DApp outside the try so we can detach from it, when we exit.
        LoadedDApp dapp = null;
        try {
            // read dapp module
            Address dappAddress = capabilities.generateContractAddress(ctx.getTransaction());
            CodeAndArguments codeAndArguments = CodeAndArguments.decodeFromBytes(ctx.getTransactionData());
            if (codeAndArguments == null) {
                if (verboseErrors) {
                    System.err.println("DApp deployment failed due to incorrectly packaged JAR and initialization arguments");
                }
                result.setResultCode(AvmTransactionResult.Code.FAILED_INVALID_DATA);
                result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());
                return;
            }

            RawDappModule rawDapp = RawDappModule.readFromJar(codeAndArguments.code, preserveDebuggability);
            if (rawDapp == null) {
                if (verboseErrors) {
                    System.err.println("DApp deployment failed due to corrupt JAR data");
                }
                result.setResultCode(AvmTransactionResult.Code.FAILED_INVALID_DATA);
                result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());
                return;
            }

            Helpers.writeBytesToFile(codeAndArguments.code, "/home/nick/Desktop/preTransform.jar");

            // Verify that the DApp contains the main class they listed and that it has a "public static byte[] main()" method.
            if (!rawDapp.classes.containsKey(rawDapp.mainClass) || !MainMethodChecker.checkForMain(rawDapp.classes.get(rawDapp.mainClass))) {
                if (verboseErrors) {
                    String explanation = !rawDapp.classes.containsKey(rawDapp.mainClass) ? "missing Main class" : "missing main() method";
                    System.err.println("DApp deployment failed due to " + explanation);
                }
                result.setResultCode(AvmTransactionResult.Code.FAILED_INVALID_DATA);
                result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());
                return;
            }
            ClassHierarchyForest dappClassesForest = rawDapp.classHierarchyForest;

            // transform
            Map<String, byte[]> transformedClasses = transformClasses(rawDapp.classes, dappClassesForest, preserveDebuggability, rawDapp.pocHierarchy);
            TransformedDappModule transformedDapp = TransformedDappModule.fromTransformedClasses(transformedClasses, rawDapp.mainClass);

            // We can now construct the abstraction of the loaded DApp which has the machinery for the rest of the initialization.
            IObjectGraphStore graphStore = new KeyValueObjectGraph(kernel,dappAddress);
            dapp = DAppLoader.fromTransformed(transformedDapp, preserveDebuggability);
            
            // We start the nextHashCode at 1.
            int nextHashCode = 1;
            InstrumentationHelpers.pushNewStackFrame(dapp.runtimeSetup, dapp.loader, ctx.getTransaction().getEnergyLimit() - result.getEnergyUsed(), nextHashCode, new IdentityHashMap<Class<?>, org.aion.avm.shadow.java.lang.Class<?>>());
            // (we pass a null reentrant state since we haven't finished initializing yet - nobody can call into us).
            IBlockchainRuntime previousRuntime = dapp.attachBlockchainRuntime(new BlockchainRuntimeImpl(capabilities, kernel, avm, null, task, ctx, codeAndArguments.arguments, dapp.runtimeSetup));

            // We have just created this dApp, there should be no previous runtime associated with it.
            RuntimeAssertionError.assertTrue(previousRuntime == null);

            IInstrumentation threadInstrumentation = IInstrumentation.attachedThreadInstrumentation.get();
            threadInstrumentation.chargeEnergy(BillingRules.getDeploymentFee(rawDapp.numberOfClasses, rawDapp.bytecodeSize));

            // Create the immortal version of the transformed DApp code by stripping the <clinit>.
            Map<String, byte[]> immortalClasses = new HashMap<>();
            for (Map.Entry<String, byte[]> elt : transformedClasses.entrySet()) {
                String className = elt.getKey();
                byte[] transformedClass = elt.getValue();
                byte[] immortalClass = new ClassToolchain.Builder(transformedClass, 0)
                        .addNextVisitor(new ClinitStrippingVisitor())
                        .addWriter(new ClassWriter(0))
                        .build()
                        .runAndGetBytecode();
                immortalClasses.put(className, immortalClass);
            }
            ImmortalDappModule immortalDapp = ImmortalDappModule.fromImmortalClasses(immortalClasses, transformedDapp.mainClass);

            // store transformed dapp
            byte[] immortalDappJar = immortalDapp.createJar(dappAddress, ctx);
            kernel.putCode(dappAddress, immortalDappJar);

            Helpers.writeBytesToFile(immortalDappJar, "/home/nick/Desktop/immortalDapp.jar");

            // Force the classes in the dapp to initialize so that the <clinit> is run (since we already saved the version without).
            dapp.forceInitializeAllClasses();

            // Save back the state before we return.
            // -first, save out the classes
            InstrumentationBasedStorageFees feeProcessor = new InstrumentationBasedStorageFees(threadInstrumentation);
            ReflectionStructureCodec directGraphData = dapp.createCodecForInitialStore(feeProcessor, graphStore);
            dapp.saveClassStaticsToStorage(feeProcessor, directGraphData, graphStore);
            // -finally, save back the final state of the environment so we restore it on the next invocation.
            ContractEnvironmentState.saveToGraph(graphStore, new ContractEnvironmentState(threadInstrumentation.peekNextHashCode()));
            graphStore.flushWrites();

            // TODO: whether we should return the dapp address is subject to change
            result.setResultCode(AvmTransactionResult.Code.SUCCESS);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit() - threadInstrumentation.energyLeft());
            result.setReturnData(dappAddress.toBytes());
            result.setStorageRootHash(graphStore.simpleHashCode());
        } catch (OutOfEnergyException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment failed due to Out-of-Energy EXCEPTION: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_OUT_OF_ENERGY);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

        } catch (OutOfStackException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment failed due to stack overflow EXCEPTION: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_OUT_OF_STACK);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

        } catch (CallDepthLimitExceededException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment failed due to call depth limit EXCEPTION: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_CALL_DEPTH_LIMIT_EXCEEDED);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

        } catch (RevertException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment to REVERT due to uncaught EXCEPTION: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_REVERT);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

        } catch (InvalidException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment INVALID due to uncaught EXCEPTION: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_INVALID);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

        } catch (UncaughtException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment failed due to uncaught EXCEPTION: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_EXCEPTION);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

            result.setUncaughtException(e.getCause());
        } catch (RejectedClassException e) {
            if (verboseErrors) {
                System.err.println("DApp deployment REJECTED with reason: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_REJECTED);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());

        } catch (EarlyAbortException e) {
            if (verboseErrors) {
                System.err.println("FYI - concurrent abort (will retry) in transaction \"" + Helpers.bytesToHexString(ctx.getTransactionHash()) + "\"");
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED_ABORT);
            result.setEnergyUsed(0);

        } catch (AvmException e) {
            // We handle the generic AvmException as some failure within the contract.
            if (verboseErrors) {
                System.err.println("DApp deployment failed due to AvmException: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            result.setResultCode(AvmTransactionResult.Code.FAILED);
            result.setEnergyUsed(ctx.getTransaction().getEnergyLimit());
        } catch (JvmError e) {
            // These are cases which we know we can't handle and have decided to handle by safely stopping the AVM instance so
            // re-throw this as the AvmImpl top-level loop will commute it into an asynchronous shutdown.
            if (verboseErrors) {
                System.err.println("FATAL JvmError: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            throw e;
        } catch (RuntimeAssertionError e) {
            // If one of these shows up here, we are wanting to pass it back up to the top, where we can shut down.
            if (verboseErrors) {
                System.err.println("FATAL internal error: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            throw new AssertionError(e);
        } catch (Throwable e) {
            // Anything else we couldn't handle more specifically needs to be passed further up to the top.
            if (verboseErrors) {
                System.err.println("FATAL unexpected Throwable: \"" + e.getMessage() + "\"");
                e.printStackTrace(System.err);
            }
            throw new AssertionError(e);
        } finally {
            // Once we are done running this, no matter how it ended, we want to detach our thread from the DApp.
            if (null != dapp) {
                InstrumentationHelpers.popExistingStackFrame(dapp.runtimeSetup);
            }
        }
    }


    private static Map<String, byte[]> rejectionAndRenameInputClasses(Map<String, byte[]> inputClasses, Set<String> preRenameUserDefinedClasses, IParentPointers parentClassResolver, boolean preserveDebuggability, PocClassHierarchy pocHierarchy) {
        Map<String, byte[]> safeClasses = new HashMap<>();
        Set<String> preRenameUserClassAndInterfaceSet = inputClasses.keySet();
        PreRenameClassAccessRules preRenameClassAccessRules = new PreRenameClassAccessRules(preRenameUserDefinedClasses, preRenameUserClassAndInterfaceSet);
        NamespaceMapper namespaceMapper = new NamespaceMapper(preRenameClassAccessRules);
        
        for (String name : inputClasses.keySet()) {
            // Note that transformClasses requires that the input class names by the .-style names.
            RuntimeAssertionError.assertTrue(-1 == name.indexOf("/"));

            int parsingOptions = preserveDebuggability ? 0: ClassReader.SKIP_DEBUG;
            ClassToolchain.Builder builder = new ClassToolchain.Builder(inputClasses.get(name), parsingOptions)
                    .addNextVisitor(new RejectionClassVisitor(preRenameClassAccessRules, namespaceMapper, preserveDebuggability))
                    .addNextVisitor(new LoopingExceptionStrippingVisitor())
                    .addNextVisitor(new UserClassMappingVisitor(namespaceMapper, preserveDebuggability));

            ClassToolchain.Builder.Creator creator = (pocHierarchy != null)
                ? builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, pocHierarchy, preserveDebuggability))
                : builder.addWriter(new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, parentClassResolver));

            byte[] bytecode = creator.build().runAndGetBytecode();

            String mappedName = DebugNameResolver.getUserPackageDotPrefix(name, preserveDebuggability);
            safeClasses.put(mappedName, bytecode);
        }
        return safeClasses;
    }
}
