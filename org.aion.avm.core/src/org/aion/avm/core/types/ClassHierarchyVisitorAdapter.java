package org.aion.avm.core.types;

public class ClassHierarchyVisitorAdapter<I, C> implements ClassHierarchyVisitor<I, C> {
    @Override
    public void onVisitRoot(ClassHierarchyNode<I, C> root) {
    }

    @Override
    public void onVisitNotRootNode(ClassHierarchyNode<I, C> node) {
    }

    @Override
    public void afterAllNodesVisited() {
    }
}
