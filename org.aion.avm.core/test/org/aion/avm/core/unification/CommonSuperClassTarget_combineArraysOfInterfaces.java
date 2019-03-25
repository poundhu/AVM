package org.aion.avm.core.unification;


public class CommonSuperClassTarget_combineArraysOfInterfaces {
    public static byte[] main() {
        return new byte[0];
    }

    public static String combineOverlappingArrays(boolean flag, CommonSuperClassTypes.RootA[] root, CommonSuperClassTypes.ChildA[] child) {
        return (flag ? root : child)[0].getRootA();
    }
}
