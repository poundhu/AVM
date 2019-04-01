package org.aion.avm.core.types;

import java.util.Collection;

public interface ClassHierarchyNode<I, C> {

    public I getId();

    public void addChild(ClassHierarchyNode<I, C> child);

    public void setParent(ClassHierarchyNode<I, C> parent);

    public ClassHierarchyNode<I, C> getParent();

    public C getContent();

    public Collection<ClassHierarchyNode<I, C>> getChildren();

}
