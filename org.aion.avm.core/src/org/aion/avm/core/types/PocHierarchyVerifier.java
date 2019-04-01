package org.aion.avm.core.types;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public final class PocHierarchyVerifier {

    //TODO -- do we need to detect cycles?

    /**
     * Verifies that the specified hierarchy is a valid hierarchy.
     *
     * This verifier will throw an exception if any of the following faults are discovered in the
     * hierarchy:
     *
     *   1. hierarchy is null
     *   2. there exists a ghost node in the hierarchy
     *   3. there exists an interface node that is a child of a non-interface node
     *   4. there exists a child node that has multiple non-interface parent nodes
     *   5. there exists a child node whose parent is a final node
     *   6. there exists some node in the hierarchy that is not a descendant of the root node
     *   7. there exists some node that is a direct child of java/lang/Object (and is not shadow IObject)
     *      in the hierarchy AND the hierarchy is a post-rename hierarchy.
     *
     * If no exceptions are thrown then the hierarchy is a valid hierarchy.
     *
     * @param hierarchy The hierarchy to be verified.
     */
    public PocVerificationResult verifyHierarchy(PocClassHierarchy hierarchy) {
        if (hierarchy == null) {
            throw new NullPointerException("Cannot verify a null hierarchy.");
        }

        Set<PocNode> visited = new HashSet<>();

        LinkedList<PocNode> nodesToVisit = new LinkedList<>();
        nodesToVisit.add(hierarchy.getRoot());

        while (!nodesToVisit.isEmpty()) {
            PocNode nextNode = nodesToVisit.poll();

            // Verify that the node is not a ghost node. This should never happen.
            if (nextNode.isGhostNode()) {
                return PocVerificationResult.foundGhostNode(nextNode.getQualifiedName());
            }

            // Verify that if this node is final it has no children.
            if ((nextNode.getClassInfo().isFinalClass) && (!nextNode.getChildren().isEmpty())) {
                return PocVerificationResult.foundSuperClassMarkedFinal(nextNode.getQualifiedName());
            }

            for (PocNode child : nextNode.getChildren()) {

                // Verify no interface is a child of a non-interface.
                if ((child.getClassInfo().isInterface) && (!nextNode.getClassInfo().isInterface)) {

                    // The only exception to this rule is when parent is java/lang/Object!
                    if (!nextNode.getClassInfo().isJavaLangObject()) {
                        return PocVerificationResult.foundInterfaceWithConcreteSuperClass(child.getQualifiedName());
                    }
                }

                nodesToVisit.addFirst(child);
            }

            // Verify this node does not have multiple non-interface parents.
            int numberOfNonInterfaceParents = 0;
            for (PocNode parent : nextNode.getParents()) {
                if (!parent.getClassInfo().isInterface) {
                    numberOfNonInterfaceParents++;
                }
            }

            if (numberOfNonInterfaceParents > 1) {
                return PocVerificationResult.foundMultipleNonInterfaceSuperClasses(nextNode.getQualifiedName());
            }

            visited.add(nextNode);
        }

        // Verify that every node was in fact reached.
        if (visited.size() != hierarchy.size()) {
            return PocVerificationResult.foundUnreachableNodes(hierarchy.size() - visited.size());
        }
        
        return PocVerificationResult.successful();
    }

}
