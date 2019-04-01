package org.aion.avm.core.types;

public interface ClassHierarchyVisitor<I, C> {

    public void onVisitRoot(ClassHierarchyNode<I, C> root);

    public void onVisitNotRootNode(ClassHierarchyNode<I, C> node);

    public void afterAllNodesVisited();

}
