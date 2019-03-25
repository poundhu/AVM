package org.aion.avm.core.unification;


public class CommonSuperClassTarget_combineArraysOfClasses {
    public static byte[] main() {
        return new byte[0];
    }

    public static String combineOverlappingArrays(boolean flag, CommonSuperClassTypes.ClassRoot[] root, CommonSuperClassTypes.ClassChild[] child) {
        return (flag ? root : child)[0].getClassRoot();
    }
}
