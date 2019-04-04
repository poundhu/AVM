package org.aion.avm.core.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.aion.avm.internal.RuntimeAssertionError;

/**
 * A node.
 *
 * To avoid confusion, this node will never point to a decorated node, even if it itself is currently
 * wrapped in a decorated node.
 *
 * You can safely assume that all child and parent pointers are to {@link PocHierarchyNode} or
 * {@link PocHierarchyGhostNode} types only.
 *
 * The class info pertaining to this node is immutable. However, the lists of parents and children
 * is not.
 */
public class PocHierarchyNode implements PocNode {
    private final PocClassInfo classInfo;
    private List<PocNode> parents;
    private List<PocNode> children;

    private PocHierarchyNode(PocClassInfo classInfo) {
        if (classInfo == null) {
            throw new NullPointerException("Cannot construct node from null class info.");
        }

        this.classInfo = classInfo;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public static PocHierarchyNode javaLangObjectNode() {
        return new PocHierarchyNode(PocClassInfo.infoForJavaLangObject());
    }

    public static PocHierarchyNode shadowIObjectNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowIObject());
    }

    public static PocHierarchyNode shadowObjectNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowObject());
    }

    public static PocHierarchyNode shadowEnumNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowEnum());
    }

    public static PocHierarchyNode shadowComparableNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowComparable());
    }

    public static PocHierarchyNode shadowSerializableNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowSerializable());
    }

    public static PocHierarchyNode shadowRuntimeExceptionNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowRuntimeException());
    }

    public static PocHierarchyNode shadowExceptionNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowException());
    }

    public static PocHierarchyNode shadowThrowableNode() {
        return new PocHierarchyNode(PocClassInfo.infoForShadowThrowable());
    }

    public static PocHierarchyNode from(PocClassInfo classInfo) {
        return new PocHierarchyNode(classInfo);
    }

    @Override
    public void addChild(PocNode node) {
        if (node == null) {
            throw new NullPointerException("Cannot add pointer to null child node.");
        }

        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));

        this.children.add(node);
    }

    @Override
    public void addParent(PocNode node) {
        if (node == null) {
            throw new NullPointerException("Cannot add pointer to null parent node.");
        }

        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));

        this.parents.add(node);
    }

    @Override
    public void removeParent(PocNode node) {
        if (node == null) {
            throw new NullPointerException("Cannot remove pointer to null parent node.");
        }

        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));

        this.parents.remove(node);
    }

    @Override
    public Collection<PocNode> getParents() {
        return new ArrayList<>(this.parents);
    }

    @Override
    public Collection<PocNode> getChildren() {
        return new ArrayList<>(this.children);
    }

    @Override
    public String getQualifiedName() {
        return this.classInfo.selfQualifiedName;
    }

    @Override
    public PocClassInfo getClassInfo() {
        return this.classInfo;
    }

    @Override
    public boolean isGhostNode() {
        return false;
    }

    @Override
    public String toString() {
        return "PocHierarchyNode { " + this.classInfo.rawString() + " }";
    }

    /**
     * Returns true only if other is a {@link PocHierarchyNode} and its class info is equivalent
     * to this node's class info.
     *
     * Note that this means this node and the other node may have different sets of child and parent
     * pointers and still be equal. This is because pointers get populated when a node joins a
     * hierarchy and equals is decoupled from this dependency since what we typically want to know
     * is whether or not these nodes are talking about the same class as determined by its class
     * info.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PocHierarchyNode)) {
            return false;
        }

        return this.classInfo.equals(((PocHierarchyNode) other).classInfo);
    }

    @Override
    public int hashCode() {
        return this.classInfo.hashCode();
    }

}
