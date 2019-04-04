package org.aion.avm.core.types;

import java.util.Collection;
import org.aion.avm.internal.RuntimeAssertionError;

/**
 * A decorated node is just a {@link PocNode} wrapper that allows for a node to be marked different
 * colours.
 *
 * This is used by the {@link PocClassHierarchy#getTightestCommonSuperClass(String, String)}
 * algorithm.
 *
 * A decorated node cannot 'decorate' (wrap) another decorated node. You can always assume the
 * wrapped node is not decorated.
 *
 * A decorated node directly exposes the node it wraps and so the immutability of this underlying
 * node is subject to the immutability guarantees of the wrapped node (typically not immutable), and
 * the markings on the decorated node are not immutable either.
 */
public class PocDecoratedNode implements PocNode {
    private PocNode node;
    private boolean isGreen;
    private boolean isRed;

    private PocDecoratedNode(PocNode node) {
        if (node == null) {
            throw new NullPointerException("Cannot decorate a null node.");
        }
        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));

        this.node = node;
        this.isGreen = false;
        this.isRed = false;
    }

    public static PocDecoratedNode decorate(PocNode node) {
        return new PocDecoratedNode(node);
    }

    public PocNode unwrap() {
        return this.node;
    }

    public PocHierarchyNode unwrapRealNode() {
        return (PocHierarchyNode) this.node;
    }

    public PocHierarchyGhostNode unwrapGhostNode() {
        return (PocHierarchyGhostNode) this.node;
    }

    public void markGreen() {
        this.isGreen = true;
    }

    public void markRed() {
        this.isRed = true;
    }

    public boolean isMarkedGreen() {
        return this.isGreen;
    }

    public boolean isMarkedRed() {
        return this.isRed;
    }

    public void clearMarkings() {
        this.isGreen = false;
        this.isRed = false;
    }

    @Override
    public boolean isGhostNode() {
        return this.node.isGhostNode();
    }

    @Override
    public PocClassInfo getClassInfo() {
        return this.node.getClassInfo();
    }

    @Override
    public String getQualifiedName() {
        return this.node.getQualifiedName();
    }

    @Override
    public void addChild(PocNode node) {
        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));
        this.node.addChild(node);
    }

    @Override
    public void addParent(PocNode node) {
        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));
        this.node.addParent(node);
    }

    @Override
    public void removeParent(PocNode node) {
        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));
        this.node.removeParent(node);
    }

    @Override
    public Collection<PocNode> getParents() {
        return this.node.getParents();
    }

    @Override
    public Collection<PocNode> getChildren() {
        return this.node.getChildren();
    }
}
