package org.aion.avm.core.types;

import java.util.Map;

import org.aion.avm.core.ClassHierarchyForest;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.dappreading.LoadedJar;


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
    public static RawDappModule readFromJar(byte[] jar) {
        // Note that ASM can fail with all kinds of exceptions so we will handle any exception as an error.
        try {
            LoadedJar loadedJar = LoadedJar.fromBytes(jar);
            ClassHierarchyForest forest = ClassHierarchyForest.createForestFrom(loadedJar);

            // Hierarchy is incomplete because enum & exception don't have real nodes yet. They won't until
            // we append them to the shadow hierarchy.
            PocClassHierarchy hierarchy = PocClassHierarchy.createPostRenameHierarchyFromPreRenameNames(loadedJar);

            PocClassHierarchy fullHierarchy = NodeEnvironment.singleton.getPocHierarchy();
            fullHierarchy.appendJarOfPreRenameClasses(loadedJar);

            PocHierarchyVerifier v = new PocHierarchyVerifier();
            PocVerificationResult r2 = v.verifyHierarchy(fullHierarchy);

            Map<String, byte[]> classes = loadedJar.classBytesByQualifiedNames;
            String mainClass = loadedJar.mainClassName;
            // To be a valid Dapp, this must specify a main class and have at least one class.
            return ((null != mainClass) && !classes.isEmpty())
                    ? new RawDappModule(classes, mainClass, forest, jar.length, classes.size())
                    : null;
        } catch (Throwable t) {
            // TODO:  Find the right place to log or right way to re-throw this once we see if it happens in unexpected places.
            return null;
        }
    }


    public final Map<String, byte[]> classes;
    public final String mainClass;
    public final ClassHierarchyForest classHierarchyForest;

    // For billing purpose
    public final long bytecodeSize;
    public final long numberOfClasses;
    
    private RawDappModule(Map<String, byte[]> classes, String mainClass, ClassHierarchyForest classHierarchyForest, long bytecodeSize, long numberOfClasses) {
        this.classes = classes;
        this.mainClass = mainClass;
        this.classHierarchyForest = classHierarchyForest;
        this.bytecodeSize = bytecodeSize;
        this.numberOfClasses = numberOfClasses;
    }
}
