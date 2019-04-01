package org.aion.avm.core.types;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

public class Node<I, C> implements ClassHierarchyNode<I, C> {
    private final Collection<ClassHierarchyNode<I, C>> childs = new LinkedHashSet<>();

    private I id;
    private C content;
    private ClassHierarchyNode<I, C> parent;

    public Node(I id, C content) {
        Objects.requireNonNull(id);
        this.id = id;
        this.content = content;
    }

    @Override
    public ClassHierarchyNode<I, C> getParent() {
        return parent;
    }

    @Override
    public Collection<ClassHierarchyNode<I, C>> getChildren() {
        return Collections.unmodifiableCollection(childs);
    }

    @Override
    public I getId() {
        return id;
    }

    @Override
    public void addChild(ClassHierarchyNode<I, C> child) {
        Objects.requireNonNull(child);
        childs.add(child);
    }

    @Override
    public void setParent(ClassHierarchyNode<I, C> parent) {
        this.parent = parent;
    }

    @Override
    public C getContent() {
        return content;
    }

    public void setContent(C c) {
        this.content = c;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        return (that instanceof Node) && id.equals(((Node) that).id);
    }
}
