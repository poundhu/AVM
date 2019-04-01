package org.aion.avm.core.types;

public class PocVerificationResult {
    public final boolean success;
    public final boolean foundGhost;
    public final boolean foundFinalSuper;
    public final boolean foundInterfaceWithConcreteSuper;
    public final boolean foundMultipleNonInterfaceSupers;
    public final boolean foundUnreachableNodes;

    public final String nodeName;
    public final int numberOfUnreachableNodes;

    private PocVerificationResult(boolean success, boolean foundGhost, boolean foundFinalSuper,
        boolean foundInterfaceWithConcreteSuper, boolean foundMultipleNonInterfaceSupers,
        boolean foundUnreachableNodes, String nodeName, int numberOfUnreachableNodes) {

        this.success = success;
        this.foundGhost = foundGhost;
        this.foundFinalSuper = foundFinalSuper;
        this.foundInterfaceWithConcreteSuper = foundInterfaceWithConcreteSuper;
        this.foundMultipleNonInterfaceSupers = foundMultipleNonInterfaceSupers;
        this.foundUnreachableNodes = foundUnreachableNodes;
        this.nodeName = nodeName;
        this.numberOfUnreachableNodes = numberOfUnreachableNodes;
    }

    public static PocVerificationResult successful() {
        return new PocVerificationResult(true, false, false, false, false, false, null, 0);
    }

    public static PocVerificationResult foundGhostNode(String nodeName) {
        return new PocVerificationResult(false,true, false, false, false, false, nodeName, 0);
    }

    public static PocVerificationResult foundSuperClassMarkedFinal(String nodeName) {
        return new PocVerificationResult(false, false, true, false, false, false, nodeName, 0);
    }

    public static PocVerificationResult foundInterfaceWithConcreteSuperClass(String nodeName) {
        return new PocVerificationResult(false, false, false, true, false, false, nodeName, 0);
    }

    public static PocVerificationResult foundMultipleNonInterfaceSuperClasses(String nodeName) {
        return new PocVerificationResult(false, false, false, false, true, false, nodeName, 0);
    }

    public static PocVerificationResult foundUnreachableNodes(int numberOfUnreachableNodes) {
        return new PocVerificationResult(false, false, false, false, false, true, null, numberOfUnreachableNodes);
    }

    @Override
    public String toString() {
        if (this.success) {
            return "PocVerificationResult { successful }";
        } else if (this.foundGhost) {
            return "PocVerificationResult { unsuccessful: found a ghost node '" + this.nodeName + "' }";
        } else if (this.foundFinalSuper) {
            return "PocVerificationResult { unsuccessful: found a super class marked final '" + this.nodeName + "' }";
        } else if (this.foundInterfaceWithConcreteSuper) {
            return "PocVerificationResult { unsuccessful: found an interface with a concrete super class '" + this.nodeName + "' }";
        } else if (this.foundMultipleNonInterfaceSupers) {
            return "PocVerificationResult { unsuccessful: found a class with multiple non-interface super classes '" + this.nodeName + "' }";
        } else {
            return "PocVerificationResult { unsuccessful: found " + this.numberOfUnreachableNodes + " unreachable nodes }";
        }
    }
}
