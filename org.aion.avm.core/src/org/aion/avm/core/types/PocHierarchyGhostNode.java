package org.aion.avm.core.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.aion.avm.internal.RuntimeAssertionError;

/**
 * A ghost node. This is a node that we know (or at least, we are told) will eventually exist but
 * currently, because we have not yet encountered it and therefore have insufficient information
 * regarding it, we cannot fully represent this node. It will hang around in this ghost state until
 * we encounter it and can replace it with a {@link PocHierarchyNode} equivalent.
 *
 * To avoid confusion, this node will never point to a decorated node, even if it itself is currently
 * wrapped in a decorated node.
 *
 * You can safely assume that all child and parent pointers are to {@link PocHierarchyNode} or
 * {@link PocHierarchyGhostNode} types only.
 */
public class PocHierarchyGhostNode implements PocNode {
    private final String name;
    private Collection<PocNode> children;

    public PocHierarchyGhostNode(String name) {
        if (name == null) {
            throw new NullPointerException("Cannot construct ghost node with null name.");
        }

        this.name = name;
        this.children = new ArrayList<>();
    }

    @Override
    public void addChild(PocNode node) {
        if (node == null) {
            throw new NullPointerException("Cannot add null child to ghost node: " + this.name);
        }

        RuntimeAssertionError.assertTrue(!(node instanceof PocDecoratedNode));

        this.children.add(node);
    }

    @Override
    public void addParent(PocNode node) {
        throw RuntimeAssertionError.unimplemented("[" + this.name + "] A ghost node cannot have parent nodes.");
    }

    @Override
    public void removeParent(PocNode node) {
        throw RuntimeAssertionError.unimplemented("[" + this.name + "] A ghost node cannot have parent nodes.");
    }

    @Override
    public String getQualifiedName() {
        return this.name;
    }

    @Override
    public PocClassInfo getClassInfo() {
        throw RuntimeAssertionError.unimplemented("[" + this.name + "] A ghost node has no class information.");
    }

    @Override
    public boolean isGhostNode() {
        return true;
    }

    @Override
    public Collection<PocNode> getParents() {
        return Collections.emptyList();
    }

    @Override
    public Collection<PocNode> getChildren() {
        return new ArrayList<>(this.children);
    }

}
