package org.aion.avm.core.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import org.aion.avm.core.dappreading.LoadedJar;
import org.aion.avm.internal.RuntimeAssertionError;
import org.objectweb.asm.ClassReader;

public final class PocClassHierarchy {
    public final boolean isPreRenameHierarchy;
    private final PocDecoratedNode root;
    private Map<String, PocDecoratedNode> nameToNodeMapping;

    private PocClassHierarchy(boolean isPreRenameHierarchy) {
        this.isPreRenameHierarchy = isPreRenameHierarchy;
        this.nameToNodeMapping = new HashMap<>();

        PocHierarchyNode javaLangObjectNode = PocHierarchyNode.javaLangObjectNode();

        if (this.isPreRenameHierarchy) {

            // In this case we have java/lang/Object as the root, and nothing else in the hierarchy.
            this.root = PocDecoratedNode.decorate(javaLangObjectNode);
            this.nameToNodeMapping.put(this.root.getQualifiedName(), this.root);

        } else {

            // In this case we have IObject as a child of java/lang/Object and nothing else in the hierarchy.
            PocHierarchyNode shadowIObjectNode = PocHierarchyNode.shadowIObjectNode();

            PocHierarchyNode shadowObjectNode = PocHierarchyNode.shadowObjectNode();

            // Set up the parent-child pointers between the object types.
            shadowIObjectNode.addParent(javaLangObjectNode);
            javaLangObjectNode.addChild(shadowIObjectNode);

            shadowObjectNode.addParent(shadowIObjectNode);
            shadowIObjectNode.addChild(shadowObjectNode);

            // Set the root and add the object types to the map.
            this.root = PocDecoratedNode.decorate(javaLangObjectNode);
            this.nameToNodeMapping.put(this.root.getQualifiedName(), this.root);
            this.nameToNodeMapping.put(shadowIObjectNode.getQualifiedName(), PocDecoratedNode.decorate(shadowIObjectNode));
            this.nameToNodeMapping.put(shadowObjectNode.getQualifiedName(), PocDecoratedNode.decorate(shadowObjectNode));
        }
    }

    public static PocClassHierarchy createPreRenameHierarchyFromPreRenameJar(LoadedJar loadedJar) {
        return createHierarchyFromJarOfPreRenameClasses(loadedJar);
    }

    public static PocClassHierarchy createPostRenameHierarchyFromPreRenameNames(LoadedJar loadedJar) {
        if (loadedJar == null) {
            throw new NullPointerException("Cannot create hierarchy from null jar.");
        }

        List<PocClassInfo> classInfos = new ArrayList<>();

        Map<String, byte[]> classNameToBytes = loadedJar.classBytesByQualifiedNames;
        for (Entry<String, byte[]> classNameToBytesEntry : classNameToBytes.entrySet()) {
            classInfos.add(getClassInfo(classNameToBytesEntry.getValue(), true));
        }

        return createPostRenameHierarchyFromPostRenameNames(toPostRenameClassInfos(classInfos));
    }

    public static PocClassHierarchy createPreRenameHierarchyFrom(Collection<PocClassInfo> classInfos) {
        return createHierarchyFrom(classInfos, true);
    }

    public static PocClassHierarchy createPostRenameHierarchyFromPreRenameNames(Collection<PocClassInfo> classInfos) {
        return createHierarchyFrom(toPostRenameClassInfos(classInfos), false);
    }

    public static PocClassHierarchy createPostRenameHierarchyFromPostRenameNames(Collection<PocClassInfo> classInfos) {
        return createHierarchyFrom(classInfos, false);
    }

    public static PocClassHierarchy createHierarchyFromJarOfPreRenameClasses(LoadedJar loadedJar) {
        if (loadedJar == null) {
            throw new NullPointerException("Cannot create hierarchy from null jar.");
        }

        List<PocClassInfo> classInfos = new ArrayList<>();

        Map<String, byte[]> classNameToBytes = loadedJar.classBytesByQualifiedNames;
        for (Entry<String, byte[]> classNameToBytesEntry : classNameToBytes.entrySet()) {
            classInfos.add(getClassInfo(classNameToBytesEntry.getValue(), true));
        }

        return createPreRenameHierarchyFrom(classInfos);
    }

    public static PocClassHierarchy createHierarchyFromJarOfPostRenameClasses(LoadedJar loadedJar) {
        if (loadedJar == null) {
            throw new NullPointerException("Cannot create hierarchy from null jar.");
        }

        List<PocClassInfo> classInfos = new ArrayList<>();

        Map<String, byte[]> classNameToBytes = loadedJar.classBytesByQualifiedNames;
        for (Entry<String, byte[]> classNameToBytesEntry : classNameToBytes.entrySet()) {

            String className = classNameToBytesEntry.getKey();

            // Since we are constructing from a post-rename jar, we don't want to re-add any object types.
            if (!className.equals(PocClassInfo.JAVA_LANG_OBJECT) && !className.equals(PocClassInfo.SHADOW_IOBJECT) && !className.equals(PocClassInfo.SHADOW_OBJECT)) {
                classInfos.add(getClassInfo(classNameToBytesEntry.getValue(), false));
            }

        }

        return createPostRenameHierarchyFromPostRenameNames(classInfos);
    }

    public void appendJarOfPreRenameClasses(LoadedJar loadedJar) {
        if (loadedJar == null) {
            throw new NullPointerException("Cannot create hierarchy from null jar.");
        }

        Map<String, byte[]> classNameToBytes = loadedJar.classBytesByQualifiedNames;
        for (Entry<String, byte[]> classNameToBytesEntry : classNameToBytes.entrySet()) {

            String className = classNameToBytesEntry.getKey();

            // Do not re-add java/lang/Object.
            if (!className.equals(PocClassInfo.JAVA_LANG_OBJECT)) {
                add(getClassInfo(classNameToBytesEntry.getValue(), true).toPostRenameClassInfo());
            }

        }
    }

    private static PocClassHierarchy createHierarchyFrom(Collection<PocClassInfo> classInfos, boolean isPreRenameHierarchy) {
        if (classInfos == null) {
            throw new NullPointerException("Cannot create hierarchy from null classes.");
        }

        PocClassHierarchy classHierarchy = new PocClassHierarchy(isPreRenameHierarchy);

        for (PocClassInfo classInfo : classInfos) {
            classHierarchy.add(classInfo);
        }

        return classHierarchy;
    }

    /**
     * Returns the class that is the tightest common super class of the two specified classes, if
     * such a super class does indeed exist.
     *
     * The returned name is the name before any renaming has been applied.
     *
     * A tightest common super class is a class that is a super class of both the specified classes
     * and does not have any child class that is a common super class of the two specified classes.
     *
     * It is possible that multiple classes fulfill this definition. If this is the case, then
     * {@code null} is returned because the single tightest class is not well defined.
     *
     * Otherwise, if there is exactly one such class, it is returned.
     *
     * @param class1 One of the classes.
     * @param class2 The other class.
     * @return The pre-rename name of the tightest common super class if one exists or else null if multiple exist.
     */
    public String getTightestCommonSuperClass(String class1, String class2) {
        if ((class1 == null) || (class2 == null)) {
            throw new NullPointerException("Cannot get the tightest super class of a null class: " + class1 + ", " + class2);
        }

        if (!this.nameToNodeMapping.containsKey(class1)) {
            throw new IllegalArgumentException("The hierarchy does not contain: " + class1);
        }
        if (!this.nameToNodeMapping.containsKey(class2)) {
            throw new IllegalArgumentException("The hierarchy does not contain: " + class2);
        }

        //TODO: optimization - if we encounter other type while visiting ancestors we can return immediately the other type.

        // Visit the ancestors of the two starting nodes and mark them differently.
        visitAncestorsAndMarkGreen(class1);
        visitAncestorsAndMarkRed(class2);

        // Now, starting at the root, discover all doubly marked leaf nodes.
        Collection<PocClassInfo> leafNodes = discoverAllDoublyMarkedLeafNodesFromRoot();

        // Clean up the mess we made.
        clearAllMarkings();

        // If these nodes have no super class in common something is very wrong.
        RuntimeAssertionError.assertTrue(!leafNodes.isEmpty());

        return (leafNodes.size() == 1) ? leafNodes.iterator().next().selfQualifiedName : null;
    }

    public PocHierarchyNode getRoot() {
        return this.root.unwrapRealNode();
    }

    public int size() {
        return this.nameToNodeMapping.size();
    }

    /**
     * Note that a deep copy of a hierarchy containing ghost nodes will cause an exception to be
     * thrown.
     *
     * Deep copies should only be made on valid hierarchies that have finished being constructed
     * (and ideally have been verified by {@link PocHierarchyVerifier}).
     */
    public PocClassHierarchy deepCopy() {
        PocClassHierarchy deepCopy = new PocClassHierarchy(this.isPreRenameHierarchy);

        // Since PocClassInfo is immutable and 'add' creates a tree out of these, we can just re-add
        // each class info to get the deeply copied hierarchy.

        Collection<PocClassInfo> classInfos = getClassInfosOfAllNodes();

        for (PocClassInfo classInfo : classInfos) {

            // Don't ever re-add java/lang/Object to the hierarchy, nor shadow Object / IObject for post-renaming.
            if (!classInfo.isJavaLangObject()) {

                if (this.isPreRenameHierarchy || (!classInfo.isShadowIObject() && !classInfo.isShadowObject())) {
                    deepCopy.add(classInfo);
                }

            }

        }

        return deepCopy;
    }

    private Collection<PocClassInfo> getClassInfosOfAllNodes() {
        Set<PocClassInfo> classInfos = new HashSet<>();

        for (PocDecoratedNode node : this.nameToNodeMapping.values()) {
            // Ghost nodes don't have associated class info (or they would be a real node).
            RuntimeAssertionError.assertTrue(!node.isGhostNode());
            classInfos.add(node.getClassInfo());
        }

        return classInfos;
    }

    /**
     * Visits all ancestor nodes of the provided starting node and marks them green.
     *
     * ASSUMPTION: startingNode is non-null and exists in the hierarchy.
     */
    private void visitAncestorsAndMarkGreen(String startingNode) {
        visitAncestors(startingNode, true);
    }

    /**
     * Visits all ancestor nodes of the provided starting node and marks them red.
     *
     * ASSUMPTION: startingNode is non-null and exists in the hierarchy.
     */
    private void visitAncestorsAndMarkRed(String startingNode) {
        visitAncestors(startingNode, false);
    }

    /**
     * Visists all descendants of the root node in the hierarchy only if they are doubly marked
     * (that is, marked both green and red).
     *
     * Returns the list of all such doubly-marked nodes that are leaf nodes in this node subset.
     */
    private Collection<PocClassInfo> discoverAllDoublyMarkedLeafNodesFromRoot() {
        RuntimeAssertionError.assertTrue(this.root.isMarkedGreen() && this.root.isMarkedRed());

        Queue<String> nodesToVisit = new LinkedList<>();
        nodesToVisit.add(this.root.getQualifiedName());

        Set<PocClassInfo> leafNodes = new HashSet<>();
        while (!nodesToVisit.isEmpty()) {

            PocDecoratedNode nextNode = this.nameToNodeMapping.get(nodesToVisit.poll());

            // A leaf node in our context is a node that has no doubly-marked children!
            boolean foundChild = false;

            for (PocNode child : nextNode.getChildren()) {

                // The child pointers are not decorated, so we need to graph the node from the map.
                PocDecoratedNode decoratedChild = this.nameToNodeMapping.get(child.getQualifiedName());

                // Only visit a doubly-marked node.
                if (decoratedChild.isMarkedGreen() && decoratedChild.isMarkedRed()) {
                    foundChild = true;
                    nodesToVisit.add(child.getQualifiedName());
                }

            }

            // If we did not find any children then this is a leaf node.
            if (!foundChild) {
                leafNodes.add(nextNode.getClassInfo());
            }

        }

        return leafNodes;
    }

    /**
     * Clears all of the decorated nodes of any markings applied to them.
     */
    private void clearAllMarkings() {
        for (PocDecoratedNode node : this.nameToNodeMapping.values()) {
            node.clearMarkings();
        }
    }

    /**
     * Adds the specified class as a node to the hierarchy.
     *
     * This method will fail if:
     *   1. {@code classToAdd == null}
     *   2. The class being added has already been added to the hierarchy previously.
     *
     * But otherwise, this method does allow you to construct all sorts of corrupt hierarchies.
     * The {@link PocHierarchyVerifier} must be run on the hierarchy once it is considered finished
     * in order to verify that the hierarchy we have constructed is in fact valid.
     *
     * @param classToAdd The class to add to the hierarchy.
     */
    public void add(PocClassInfo classToAdd) {
        if (classToAdd == null) {
            throw new NullPointerException("Cannot add a null node to the hierarchy.");
        }

        // Verify we've got the correctly named class in our hands.
        RuntimeAssertionError.assertTrue(classToAdd.isPreRenameClass == this.isPreRenameHierarchy);

        // Add the new node to the hierarchy.
        PocHierarchyNode newNode = PocHierarchyNode.from(classToAdd);

        PocDecoratedNode nodeToAddFoundInMap = this.nameToNodeMapping.get(classToAdd.selfQualifiedName);

        if (nodeToAddFoundInMap == null) {
            // The node we want to add is not already present, so we create it.
            this.nameToNodeMapping.put(newNode.getQualifiedName(), PocDecoratedNode.decorate(newNode));
        } else {
            if (nodeToAddFoundInMap.isGhostNode()) {
                // The node we want to add is already present as a ghost node, so now we can make it
                // a real node since we have its information.
                replaceGhostNodeWithRealNode(nodeToAddFoundInMap.unwrapGhostNode(), newNode);
            } else {
                // The node we want to add already exists - something went wrong.
                throw new IllegalStateException("Attempted to re-add a node: " + classToAdd.selfQualifiedName);
            }
        }

        // Create all the child-parent pointers in the hierarchy for this node.
        String[] superClasses = classToAdd.superClasses();
        for (String superClass : superClasses) {

            // Verify that we are not attempting to add a node directly under java/lang/Object.
            if (!this.isPreRenameHierarchy) {
                if (superClass.equals(PocClassInfo.JAVA_LANG_OBJECT)) {
                    this.nameToNodeMapping.remove(classToAdd.selfQualifiedName);
                    throw new IllegalStateException("Attempted to subclass " + PocClassInfo.JAVA_LANG_OBJECT + " in a post-rename hierarchy: " + classToAdd.selfQualifiedName);
                }
            }

            PocDecoratedNode parentNode = this.nameToNodeMapping.get(superClass);

            if (parentNode == null) {
                // The parent isn't in the hierarchy yet, so we create a 'ghost' node as a placeholder for now.
                PocDecoratedNode ghost = PocDecoratedNode.decorate(new PocHierarchyGhostNode(superClass));
                this.nameToNodeMapping.put(ghost.getQualifiedName(), ghost);

                parentNode = ghost;
            }

            // Add the pointers.
            parentNode.addChild(newNode);
            newNode.addParent(parentNode.unwrap());
        }
    }

    /**
     * Replaces the ghost node with the real node.
     *
     * All child-parent pointers that the ghost node had will now be inherited by the real node.
     * Any nodes previously pointing to the ghost node will no longer do so.
     *
     * The ghost node will be entirely removed from the hierarchy.
     *
     * ASSUMPTIONS:
     *    ghostNode and realNode are both non-null
     *    ghostNode is currently in the hierarchy
     */
    private void replaceGhostNodeWithRealNode(PocHierarchyGhostNode ghostNode, PocHierarchyNode realNode) {
        RuntimeAssertionError.assertTrue(ghostNode.getQualifiedName().equals(realNode.getQualifiedName()));

        // First, add the real node to the hierarchy now. Since it has the same key as the ghost node,
        // this also removes the ghost node from the mapping.
        this.nameToNodeMapping.put(realNode.getQualifiedName(), PocDecoratedNode.decorate(realNode));

        // Second, inherit all of the child-parent pointer relationships from the ghost node.
        for (PocNode child : ghostNode.getChildren()) {
            realNode.addChild(child);
            child.addParent(realNode);

            child.removeParent(ghostNode);
        }
    }

    private static PocClassInfo getClassInfo(byte[] classBytes, boolean isPreRenameHierarchy) {
        ClassReader reader = new ClassReader(classBytes);
        PocClassVisitor codeVisitor = new PocClassVisitor(isPreRenameHierarchy);
        reader.accept(codeVisitor, ClassReader.SKIP_FRAMES);
        return codeVisitor.getClassInfo();
    }

    private static Collection<PocClassInfo> toPostRenameClassInfos(Collection<PocClassInfo> classInfos) {
        Collection<PocClassInfo> renamedClassInfos = new ArrayList<>();

        for (PocClassInfo classInfo : classInfos) {
            renamedClassInfos.add(classInfo.toPostRenameClassInfo());
        }

        return renamedClassInfos;
    }

    private void visitAncestors(String startingNode, boolean markGreen) {
        Queue<String> nodesToVisit = new LinkedList<>();
        nodesToVisit.add(startingNode);

        while (!nodesToVisit.isEmpty()) {

            String next = nodesToVisit.poll();

            PocDecoratedNode nextNode = this.nameToNodeMapping.get(next);

            if (markGreen) {
                nextNode.markGreen();
            } else {
                nextNode.markRed();
            }

            for (PocNode child : nextNode.getParents()) {
                nodesToVisit.add(child.getQualifiedName());
            }
        }
    }

    @Override
    public String toString() {
        if (this.isPreRenameHierarchy) {
            return "PocClassHierarchy { pre-rename hierarchy of " + this.nameToNodeMapping.size() + " classes. }";
        } else {
            return "PocClassHierarchy { post-rename hierarchy of " + this.nameToNodeMapping.size() + " classes. }";
        }
    }

    /**
     * Returns true only if other is a {@link PocClassHierarchy} and both this and the other
     * hierarchy are either both pre-rename or both post-rename, and both hierarchies contain the
     * same set of classes as determined by the class info object associated with each node.
     *
     * This equals method does not survery the entire hierarchy, it merely checks that the hierarchy
     * was built off the same set of class info as the other hierarchy. However, since all construction
     * of the hierarchy is completely determined by the set of class infos passed into it, this
     * method should also guarantee the structure of the two hierarchies are equal as well - at
     * least if both hierarchies have been verified by {@link PocHierarchyVerifier}.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PocClassHierarchy)) {
            return false;
        }

        PocClassHierarchy otherHierarchy = (PocClassHierarchy) other;
        return (this.isPreRenameHierarchy == otherHierarchy.isPreRenameHierarchy)
            && (this.getClassInfosOfAllNodes().equals(otherHierarchy.getClassInfosOfAllNodes()));
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash += (this.isPreRenameHierarchy) ? 1 : 0;

        for (PocClassInfo classInfo : this.getClassInfosOfAllNodes()) {
            hash += classInfo.hashCode();
        }

        return hash;
    }

}
