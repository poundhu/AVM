package org.aion.avm.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aion.avm.core.classgeneration.CommonGenerators;
import org.aion.avm.core.classloading.AvmClassLoader;
import org.aion.avm.core.classloading.AvmSharedClassLoader;
import org.aion.avm.core.util.Assert;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.internal.IHelper;
import org.aion.avm.internal.PackageConstants;
import org.aion.kernel.KernelInterface;


/**
 * Represents the long-lived global state of a specific "node" instance.
 * For now, this just contains the AvmSharedClassLoader (since it is stateless and shared by all transactions run on this
 * NodeEnvironment - that is, each AvmImpl instance).
 * Note that this is also responsible for any bootstrap initialization of the shared environment.  Specifically, this involves
 * eagerly loading the shadow JDK in order to run their <clinit> methods.
 */
public class NodeEnvironment {
    // NOTE:  This is only temporarily a singleton and will probably see its relationship inverted, in the future:  becoming the Avm factory.
    public static final NodeEnvironment singleton = new NodeEnvironment();

    private final AvmSharedClassLoader sharedClassLoader;
    private final Map<Long, org.aion.avm.shadow.java.lang.Object> constantMap;

    private Set<String> jclClassNames;

    private NodeEnvironment() {
        this.sharedClassLoader = new AvmSharedClassLoader(CommonGenerators.generateShadowJDK());
        try {
            this.jclClassNames = loadShadowClasses(NodeEnvironment.class.getClassLoader());

            // we have to add the common generated exception/error classes as it's not pre-loaded
            this.jclClassNames.addAll(Stream.of(CommonGenerators.kExceptionClassNames)
                    .map(Helpers::fulllyQualifiedNameToInternalName)
                    .collect(Collectors.toList()));

            // TODO: confirm with Rom if this is correct
            this.jclClassNames.add("java/lang/invoke/MethodHandles");
            this.jclClassNames.add("java/lang/invoke/MethodHandles$Lookup");

        } catch (ClassNotFoundException e) {
            // This would be a fatal startup error.
            Assert.unexpected(e);
        }
        // Create the constant map.
        this.constantMap = Collections.unmodifiableMap(initializeConstantState());
    }

    // This is an example of the more "factory-like" nature of the NodeEnvironment.
    public AvmClassLoader createInvocationClassLoader(Map<String, byte[]> finalContractClasses) {
        return new AvmClassLoader(this.sharedClassLoader, finalContractClasses);
    }

    public Class<?> loadSharedClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, this.sharedClassLoader);
    }

    /**
     * This method only exists for unit tests.  Returns true if clazz was loaded by the shared loader.
     */
    public boolean isClassFromSharedLoader(Class<?> clazz) {
        return (this.sharedClassLoader == clazz.getClassLoader());
    }

    /**
     * Returns whether the class is from our custom JCL.
     *
     * @param classNameSlash
     * @return
     */
    public boolean isClassFromJCL(String classNameSlash) {
        return this.jclClassNames.contains(classNameSlash);
    }

    public Map<Long, org.aion.avm.shadow.java.lang.Object> getConstantMap() {
        return this.constantMap;
    }

    /**
     * Creates a new long-lived AVM instance.  The intention is that only one AVM instance will be created and reused for each transaction.
     * NOTE:  This is only in the NodeEnvironment since it is a long-lived singleton but this method has no strong connection to it so it
     * could be moved in the future.
     * 
     * @param kernel The kernel interface exposed by the consumer.
     * @return The long-lived AVM instance.
     */
    public Avm buildAvmInstance(KernelInterface kernel) {
        return new AvmImpl(kernel);
    }


    private static Set<String> loadShadowClasses(ClassLoader loader) throws ClassNotFoundException {
        // Create the fake IHelper.
        IHelper.currentContractHelper.set(new IHelper() {
            @Override
            public void externalChargeEnergy(long cost) {
                Assert.unreachable("Shouldn't be called on this since we should be loading no instrumented classes");
            }
            @Override
            public void externalSetEnergy(long energy) {
                Assert.unreachable("Nobody should be calling this");
            }
            @Override
            public long externalGetEnergyRemaining() {
                Assert.unreachable("Nobody should be calling this");
                return 0L;
            }
            @Override
            public org.aion.avm.shadow.java.lang.Class<?> externalWrapAsClass(Class<?> input) {
                Assert.unreachable("Nobody should be calling this");
                return null;
            }
            @Override
            public int externalGetNextHashCode() {
                // We will just return 1 for all identity hash codes, for now.
                return 1;
            }
            @Override
            public void externalBootstrapOnly() {
                // This is ok since we are the bootstrapping helper.
            }});
        
        // Load all the classes - even just mentioning these might cause them to be loaded, even before the Class.forName().
        Set<String> loadedClassNames = loadAndInitializeClasses(loader
                , org.aion.avm.shadow.java.lang.Boolean.class
                , org.aion.avm.shadow.java.lang.Byte.class
                , org.aion.avm.shadow.java.lang.Character.class
                , org.aion.avm.shadow.java.lang.CharSequence.class
                , org.aion.avm.shadow.java.lang.Class.class
                , org.aion.avm.shadow.java.lang.Double.class
                , org.aion.avm.shadow.java.lang.Enum.class
                , org.aion.avm.shadow.java.lang.EnumConstantNotPresentException.class
                , org.aion.avm.shadow.java.lang.Exception.class
                , org.aion.avm.shadow.java.lang.Float.class
                , org.aion.avm.shadow.java.lang.Integer.class
                , org.aion.avm.shadow.java.lang.Long.class
                , org.aion.avm.shadow.java.lang.Math.class
                , org.aion.avm.shadow.java.lang.Number.class
                , org.aion.avm.shadow.java.lang.Object.class
                , org.aion.avm.shadow.java.lang.Runnable.class
                , org.aion.avm.shadow.java.lang.RuntimeException.class
                , org.aion.avm.shadow.java.lang.Short.class
                , org.aion.avm.shadow.java.lang.StrictMath.class
                , org.aion.avm.shadow.java.lang.String.class
                , org.aion.avm.shadow.java.lang.StringBuffer.class
                , org.aion.avm.shadow.java.lang.StringBuilder.class
                , org.aion.avm.shadow.java.lang.System.class
                , org.aion.avm.shadow.java.lang.Throwable.class
                , org.aion.avm.shadow.java.lang.TypeNotPresentException.class
                
                , org.aion.avm.shadow.java.lang.invoke.LambdaMetafactory.class
                , org.aion.avm.shadow.java.lang.invoke.StringConcatFactory.class
                
                , org.aion.avm.shadow.java.math.BigDecimal.class
                , org.aion.avm.shadow.java.math.BigInteger.class
                , org.aion.avm.shadow.java.math.MathContext.class
                , org.aion.avm.shadow.java.math.RoundingMode.class

                , org.aion.avm.shadow.java.nio.Buffer.class
                , org.aion.avm.shadow.java.nio.ByteBuffer.class
                , org.aion.avm.shadow.java.nio.ByteOrder.class
                , org.aion.avm.shadow.java.nio.CharBuffer.class
                , org.aion.avm.shadow.java.nio.DoubleBuffer.class
                , org.aion.avm.shadow.java.nio.FloatBuffer.class
                , org.aion.avm.shadow.java.nio.IntBuffer.class
                , org.aion.avm.shadow.java.nio.LongBuffer.class
                , org.aion.avm.shadow.java.nio.ShortBuffer.class

                , org.aion.avm.shadow.java.util.Arrays.class

                , org.aion.avm.shadow.java.util.function.Function.class

        );
        
        // Clean-up.
        IHelper.currentContractHelper.set(null);

        return loadedClassNames;
    }

    private static Set<String> loadAndInitializeClasses(ClassLoader loader, Class<?> ...classes) throws ClassNotFoundException {
        Set<String> classNames = new HashSet<>();

        // (note that the loader.loadClass() doesn't invoke <clinit> so we use Class.forName() - this "initialize" flag should do that).
        boolean initialize = true;
        for (Class<?> clazz : classes) {
            Class<?> instance = Class.forName(clazz.getName(), initialize, loader);
            Assert.assertTrue(clazz == instance);

            String className = Helpers.fulllyQualifiedNameToInternalName(clazz.getName());
            classNames.add(className.substring(PackageConstants.kShadowSlashPrefix.length()));
        }

        return classNames;
    }

    private Map<Long, org.aion.avm.shadow.java.lang.Object> initializeConstantState() {
        Map<Long, org.aion.avm.shadow.java.lang.Object> constantMap = new HashMap<>();
        
        // Assign the special "negative instanceId" values which we use for shadow JDK constants (public static objects and enum instances).
        // NOTE:  This list needs to be manually updated and we specify it as a list since these values CANNOT change, once assigned (these represent the serialized symbolic references from contracts).
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Boolean.avm_TRUE, -1l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Boolean.avm_FALSE, -2l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.BigDecimal.avm_ZERO, -3l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.BigDecimal.avm_ONE, -4l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.BigDecimal.avm_TEN, -5l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.MathContext.avm_UNLIMITED, -6l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.MathContext.avm_DECIMAL32, -7l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.MathContext.avm_DECIMAL64, -8l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.MathContext.avm_DECIMAL128, -89l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_UP, -10l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_DOWN, -11l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_CEILING, -12l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_FLOOR, -13l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_HALF_UP, -14l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_HALF_DOWN, -15l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_HALF_EVEN, -16l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.math.RoundingMode.avm_UNNECESSARY, -17l);
        
        // Note that (as explained in issue-146), we need to treat our primitive "TYPE" pseudo-classes as constants, not like normal Class references.
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Boolean.avm_TYPE, -18l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Byte.avm_TYPE, -19l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Character.avm_TYPE, -20l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Double.avm_TYPE, -21l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Float.avm_TYPE, -22l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Integer.avm_TYPE, -23l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Long.avm_TYPE, -24l);
        setConstantInstanceId(constantMap, org.aion.avm.shadow.java.lang.Short.avm_TYPE, -25l);
        
        return constantMap;
    }

    private void setConstantInstanceId(Map<Long, org.aion.avm.shadow.java.lang.Object> constantMap, org.aion.avm.shadow.java.lang.Object object, long instanceId) {
        object.instanceId = instanceId;
        constantMap.put(instanceId, object);
    }
}
