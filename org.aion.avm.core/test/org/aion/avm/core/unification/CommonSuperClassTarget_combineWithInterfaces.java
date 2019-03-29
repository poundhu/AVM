package org.aion.avm.core.unification;

import java.util.Iterator;
import java.util.List;
import org.aion.avm.api.Address;
import org.aion.avm.core.unification.CommonSuperClassTypes.RootA;
import org.aion.avm.core.unification.CommonSuperClassTypes.RootB;
import org.aion.avm.core.unification.CommonSuperClassTypes.SubRootA1;
import org.aion.avm.core.unification.CommonSuperClassTypes.SubRootA2;
import org.aion.avm.core.unification.CommonSuperClassTypes.SubSubRootA1;
import org.aion.avm.userlib.abi.ABIEncoder;

public class CommonSuperClassTarget_combineWithInterfaces {

    private enum EmptyEnum {}

    // The associated test only checks that deployment succeeds, so main() can return null
    public static byte[] main() {
        return null;
    }

    public static String combineClassAndInterface(boolean flag, CommonSuperClassTarget_combineWithInterfaces a, RootA b) {
        return (flag ? a : b).toString();
    }

    public static String combineClassAndJclInterface(boolean flag, CommonSuperClassTarget_combineWithInterfaces a, Iterator<?> b) {
        return (flag ? a : b).toString();
    }

    public static Object combineJclInterfaces(boolean flag, Iterator<?> a, List<?> b) {
        return flag ? a : b;
    }

    public static RootA combineRelatedInterfaces(boolean flag, SubSubRootA1 a, SubRootA2 b) {
        return flag ? a : b;
    }

    public static Object combineUnrelatedInterfaces(boolean flag, SubRootA1 a, RootB b) {
        return flag ? a : b;
    }

    public static RootA[] combineInterfaceWithArrays1(boolean flag, SubSubRootA1[] a, SubRootA2[] b) {
        return flag ? a : b;
    }

    public static String combineInterfaceWithArrays2(boolean flag, SubRootA1 a, Integer[][] b) {
        return (flag ? a : b).toString();
    }

    public static Object combineInterfaceWithException(boolean flag, RootA a, EmptyException[] b) {
        return flag ? a : b[0];
    }

    public static String combineInterfaceWithEnum(boolean flag, RootB a, EmptyEnum b) {
        return (flag ? a : b).toString();
    }

    public static Object combineInterfaceWithApi(boolean flag, RootB a, Address b) {
        return flag ? a : b;
    }

    public static Object combineInterfaceWithUserlib(boolean flag, RootA a, ABIEncoder b) {
        return (flag ? a : b).toString();
    }

    private static abstract class EmptyException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
