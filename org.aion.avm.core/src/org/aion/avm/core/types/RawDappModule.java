package org.aion.avm.core.types;

import java.util.Map;

import org.aion.avm.core.ClassHierarchyForest;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.dappreading.LoadedJar;
import org.aion.avm.core.rejection.RejectedClassException;


/**
 * Represents the original code submitted by the user, prior to validation or transformation.
 * Once transformed, the code this contains is moved into a TransformedDappModule.
 * All fields are public since this object is effectively an immutable struct.
 * See issue-134 for more details on this design.
 */
public class RawDappModule {
    /**
     * Reads the Dapp module from JAR bytes, in memory.
     * Note that a Dapp module is expected to specify a main class and contain at least one class.
     * 
     * @param jar The JAR bytes.
     * @return The module, or null if the contents of the JAR were insufficient for a Dapp.
     */
    public static RawDappModule readFromJar(byte[] jar, boolean preserveDebuggability) {
        // Note that ASM can fail with all kinds of exceptions so we will handle any exception as an error.
        try {
            LoadedJar loadedJar = LoadedJar.fromBytes(jar);
            ClassHierarchyForest forest = ClassHierarchyForest.createForestFrom(loadedJar);

            //---------------------------------------------------
            PocClassHierarchy fullHierarchy = NodeEnvironment.singleton.getPocHierarchy();
            fullHierarchy.appendJarOfPreRenameUserDefinedClasses(loadedJar, preserveDebuggability);

            PocHierarchyAppender appender = new PocHierarchyAppender();
            appender.appendPostRenamePrimitiveArrayWrapperTypes(fullHierarchy);
            appender.appendPreRenameErrorTypes(fullHierarchy);
            appender.appendPostRenameExceptionTypes(fullHierarchy);

            PocHierarchyVerifier verifier = new PocHierarchyVerifier();

            PocVerificationResult result = verifier.verifyHierarchy(fullHierarchy);
            if (!result.success) {
                throw new RejectedClassException(result.getError());
            }
            //---------------------------------------------------

            Map<String, byte[]> classes = loadedJar.classBytesByQualifiedNames;
            String mainClass = loadedJar.mainClassName;
            // To be a valid Dapp, this must specify a main class and have at least one class.
            return ((null != mainClass) && !classes.isEmpty())
                    ? new RawDappModule(classes, mainClass, forest, jar.length, classes.size(), fullHierarchy)
                    : null;
        } catch (RejectedClassException e) {
            throw e;
        } catch (Throwable t) {
            // TODO:  Find the right place to log or right way to re-throw this once we see if it happens in unexpected places.
            t.printStackTrace();
            return null;
        }
    }


    public final Map<String, byte[]> classes;
    public final String mainClass;
    public final ClassHierarchyForest classHierarchyForest;
    public final PocClassHierarchy pocHierarchy;

    // For billing purpose
    public final long bytecodeSize;
    public final long numberOfClasses;
    
    private RawDappModule(Map<String, byte[]> classes, String mainClass, ClassHierarchyForest classHierarchyForest, long bytecodeSize, long numberOfClasses, PocClassHierarchy hierarchy) {
        this.classes = classes;
        this.mainClass = mainClass;
        this.classHierarchyForest = classHierarchyForest;
        this.bytecodeSize = bytecodeSize;
        this.numberOfClasses = numberOfClasses;

        this.pocHierarchy = hierarchy;
    }
}
