package org.aion.avm.core;

import java.util.HashSet;
import java.util.Set;

import org.aion.avm.core.types.ClassHierarchy;
import org.aion.avm.core.types.ClassHierarchyNode;
import org.aion.avm.core.types.ClassInfo;
import org.aion.avm.core.types.Forest;
import org.aion.avm.core.types.Node;
import org.aion.avm.internal.PackageConstants;


/**
 * A high-level abstraction over the questions related to "what classes can be referenced by this contract".
 * This encompasses 3 kinds of classes:
 * 1)  The JDK types we have shadowed.
 * 2)  The AVM runtime package.
 * 3)  The types defined within the user contract, itself.
 * 
 * Note that all predicates here are requested in terms of "slash-style" (aka "internal") class names.
 * TODO:  We need to add some additional restrictions to our JDK filter since we won't shadow all java/lang sub-packages ("ref", for example).
 */
public class ClassWhiteList {
    /**
     * Checks if the class given is in any of our white-lists.
     * 
     * @param slashClassName The class to check.
     * @return True if we are allowed to access this class by any means we know.
     */
    // Node that this does not consider debug mode assumptions
    public boolean isInWhiteList(String slashClassName) {
        return (slashClassName.startsWith(PackageConstants.kUserSlashPrefix)
                || slashClassName.startsWith(PackageConstants.kShadowSlashPrefix)
                || slashClassName.startsWith(PackageConstants.kShadowApiSlashPrefix)
                );
    }

    /**
     * Checks if the given class is in our JDK white-list.
     *
     * @param slashClassName The class to check.
     * @return True if we are allowed to access this class due to it being in our JDK white-list.
     */
    public boolean isJdkClass(String slashClassName) {
        return slashClassName.startsWith(PackageConstants.kShadowSlashPrefix);
    }


    public static Set<String> extractDeclaredClasses(ClassHierarchy<String, ClassInfo> classHierarchy) {
        Set<String> providedClassNames = new HashSet<>();
        // We will build this set by walking the roots (note that roots, by definition, are not provided by the application, but the JDK)
        // and recursively collecting all reachable children.
        for (ClassHierarchyNode<String, ClassInfo> root : classHierarchy.getRoots()) {
            // Note that we don't add the roots, just walk their children.
            deepAddChildrenToSet(providedClassNames, root);
        }
        return providedClassNames;
    }

    private static void deepAddChildrenToSet(Set<String> providedClassNames, ClassHierarchyNode<String, ClassInfo> node) {
        for (ClassHierarchyNode<String, ClassInfo> child : node.getChildren()) {
            providedClassNames.add(child.getId());
            deepAddChildrenToSet(providedClassNames, child);
        }
    }
}
