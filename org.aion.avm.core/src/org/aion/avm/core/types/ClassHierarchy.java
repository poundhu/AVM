package org.aion.avm.core.types;

import java.util.Collection;

public interface ClassHierarchy<I, C> {

    public Collection<ClassHierarchyNode<I, C>> getRoots();

    public void walkPreOrder(ClassHierarchyVisitor<I, C> visitor);

    public ClassHierarchyNode<I, C> getNodeById(I id);

    public void add(ClassHierarchyNode<I, C> parent, ClassHierarchyNode<I, C> child);

}
