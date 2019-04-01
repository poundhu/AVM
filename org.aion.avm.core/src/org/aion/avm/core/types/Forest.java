package org.aion.avm.core.types;

import java.util.*;

import static java.lang.String.format;

/**
 * Note! Nodes are double-linked parent has link to child and child has a link to the parent
 *
 * @author Roman Katerinenko
 */
public class Forest<I, C> implements ClassHierarchy<I, C> {
    private final Collection<ClassHierarchyNode<I, C>> roots = new ArrayList<>();
    private final Map<I, ClassHierarchyNode<I, C>> nodesIndex = new HashMap<>();

    private ClassHierarchyVisitor<I, C> currentVisitor;
    private ClassHierarchyNode<I, C> currentVisitingRoot;

    @Override
    public Collection<ClassHierarchyNode<I, C>> getRoots() {
        return Collections.unmodifiableCollection(roots);
    }

    public int getNodesCount() {
        return nodesIndex.size();
    }

    @Override
    public ClassHierarchyNode<I, C> getNodeById(I id) {
        Objects.requireNonNull(id);
        return nodesIndex.get(id);
    }

    public ClassHierarchyNode<I, C> lookupNode(ClassHierarchyNode<I, C> target) {
        Objects.requireNonNull(target);
        return nodesIndex.get(target.getId());
    }

    @Override
    public void add(ClassHierarchyNode<I, C> parent, ClassHierarchyNode<I, C> child) {
        Objects.requireNonNull(child);
        Objects.requireNonNull(parent);
        if (parent.getId().equals(child.getId())) {
            throw new IllegalArgumentException(format("parent(%s) id must not be equal to child id (%s)", parent.getId(), child.getId()));
        }
        ClassHierarchyNode<I, C> parentCandidate = lookupExistingFor(parent);
        if (parentCandidate == null) {
            parentCandidate = parent;
            roots.add(parentCandidate);
            nodesIndex.put(parentCandidate.getId(), parentCandidate);
        }
        ClassHierarchyNode<I, C> childCandidate = lookupExistingFor(child);
        boolean childExisted = true;
        if (childCandidate == null) {
            childExisted = false;
            childCandidate = child;
            nodesIndex.put(childCandidate.getId(), childCandidate);
        }
        if (childExisted && roots.contains(childCandidate)) {
            roots.remove(childCandidate);
        }
        parentCandidate.addChild(childCandidate);
        childCandidate.setParent(parentCandidate);
    }

    // Prune the Forest and only keep the trees of the 'newRoots' roots.
    public void prune(Collection<ClassHierarchyNode<I, C>> newRoots) {
        Objects.requireNonNull(newRoots);
        final var pruneVisitor = new ClassHierarchyVisitor<I, C>() {
            @Override
            public void onVisitRoot(ClassHierarchyNode<I, C> root) {
                nodesIndex.remove(root.getId());
            }

            @Override
            public void onVisitNotRootNode(ClassHierarchyNode<I, C> node) {
                nodesIndex.remove(node.getId());
            }

            @Override
            public void afterAllNodesVisited() {
            }
        };
        Iterator<ClassHierarchyNode<I, C>> iterator = roots.iterator();
        while (iterator.hasNext()) {
            ClassHierarchyNode<I, C> root = iterator.next();
            if (!newRoots.contains(root)) {
                walkOneTreePreOrder(pruneVisitor, root);
                iterator.remove();
            }
        }
    }

    @Override
    public void walkPreOrder(ClassHierarchyVisitor<I, C> visitor) {
        Objects.requireNonNull(visitor);
        currentVisitor = visitor;
        for (ClassHierarchyNode<I, C> root : roots) {
            currentVisitingRoot = root;
            walkPreOrderInternal(root);
        }
        visitor.afterAllNodesVisited();
    }

    private void walkOneTreePreOrder(ClassHierarchyVisitor<I, C> visitor, ClassHierarchyNode<I, C> root) {
        Objects.requireNonNull(visitor);
        currentVisitor = visitor;
        currentVisitingRoot = root;
        walkPreOrderInternal(root);
        visitor.afterAllNodesVisited();
    }

    private void walkPreOrderInternal(ClassHierarchyNode<I, C> node) {
        if (node == currentVisitingRoot) {
            currentVisitor.onVisitRoot(node);
        } else {
            currentVisitor.onVisitNotRootNode(node);
        }
        for (ClassHierarchyNode<I, C> child : node.getChildren()) {
            walkPreOrderInternal(child);
        }
    }

    private ClassHierarchyNode<I, C> lookupExistingFor(ClassHierarchyNode<I, C> node) {
        return nodesIndex.get(node.getId());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Forest { roots = ");

        for (ClassHierarchyNode<?,?> root : this.roots) {
            builder.append(root.getId()).append(", number of children = ").append(root.getChildren().size()).append(", ");
        }

        return builder.append(" }").toString();
    }
}