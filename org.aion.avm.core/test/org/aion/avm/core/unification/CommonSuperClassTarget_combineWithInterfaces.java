package org.aion.avm.core.unification;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import org.aion.avm.api.Address;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.core.unification.CommonSuperClassTypes.EnumA1;
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
        boolean flag = true;

        Object o = combineInterfaceWithException2(flag, EnumA1.ME, new EmptyException());

        // Calling the getName because in the background it prints out the real type, just quicker than javap to verify these things.
        String reportedDappName = CommonSuperClassTarget_combineWithInterfaces.class.getName();
        String reportedObjectName = o.getClass().getName();

        BlockchainRuntime.println("Dapp: " + reportedDappName);
        BlockchainRuntime.println("Object: " + reportedObjectName);
        BlockchainRuntime.println("Object.toString(): " + o.toString());

        if (flag) {
            EnumA1 b = (EnumA1) o;
            BlockchainRuntime.println("EnumA1.toString(): " + b.toString());
        } else {
            EmptyException ex = (EmptyException) o;
            BlockchainRuntime.println("EmptyException.toString(): " + ex.toString());
        }

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

    public static String combineA(boolean flag, int[] a, byte[] b) {
        return (flag ? a : b).toString();
    }

    public static int[] combineB(boolean flag, int[] a, int[] b) {
        return flag ? a : b;
    }

    public static String combineA(boolean flag, int[][] a, byte[][] b) {
        return (flag ? a : b).toString();
    }

    public static int[][][] combineB(boolean flag, int[][][] a, int[][][] b) {
        return flag ? a : b;
    }

    public static Object combineC(boolean flag, int[] a, int[][][] b) {
        return flag ? a : b;
    }

    public static Object combineD(boolean flag, int[] a, byte[][][] b) {
        return flag ? a : b;
    }

    public static String combineE(boolean flag, Integer[] a, Byte[] b) {
        return (flag ? a : b).toString();
    }

    public static Object combineF(boolean flag, Comparable[] a, Serializable[] b) {
        return flag ? a : b;
    }

    public static Object combineG(boolean flag, Comparable[] a, Serializable[][] b) {
        return flag ? a : b;
    }

    public static Object combineH(boolean flag, SubRootA1[] a, SubRootA2[] b) {
        return flag ? a : b;
    }

    public static String combineI(boolean flag, int[][] a, Serializable[][][] b) {
        return (flag ? a : b).toString();
    }

    public static RootA[] combineInterfaceWithArrays1(boolean flag, SubSubRootA1[] a, SubRootA2[] b) {
        return flag ? a : b;
    }

    public static String combineInterfaceWithArrays2(boolean flag, SubRootA1 a, Integer[][] b) {
        return (flag ? a : b).toString();
    }

    public static Object combineInterfaceWithException1(boolean flag, RootA a, EmptyException[] b) {
        return flag ? a : b[0];
    }

    public static Object combineInterfaceWithException2(boolean flag, RootA a, EmptyException b) {
        Object isNull = null;

        try {
            isNull.toString();
        } catch (NullPointerException e) {
            BlockchainRuntime.println("CAUGHT NULL!");
        }

        return flag ? a : b;
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

    private static class EmptyException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
