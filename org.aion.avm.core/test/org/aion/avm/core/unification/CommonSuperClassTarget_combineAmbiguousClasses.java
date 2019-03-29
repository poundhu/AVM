package org.aion.avm.core.unification;

public class CommonSuperClassTarget_combineAmbiguousClasses {
    // The associated test only checks that deployment succeeds, so main() can return null
    public static byte[] main() {
        return null;
    }

    public static String combineAmbiguous1(boolean flag, CommonSuperClassTypes.ChildA a, CommonSuperClassTypes.ChildB b) {
        return (flag ? a : b).getRootA();
    }

    public static String combineAmbiguous2(boolean flag, CommonSuperClassTypes.ChildA a, CommonSuperClassTypes.ChildB b) {
        return (flag ? a : b).getRootB();
    }
}
