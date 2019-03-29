package org.aion.avm.core.unification;

import java.util.List;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.core.unification.CommonSuperClassTypes.ClassChild;
import org.aion.avm.core.unification.CommonSuperClassTypes.EnumA1;
import org.aion.avm.core.unification.CommonSuperClassTypes.EnumA2;
import org.aion.avm.core.unification.CommonSuperClassTypes.RootA;
import org.aion.avm.userlib.AionList;

public class CommonSuperClassTarget_combineWithEnums {

    private enum EmptyEnum1 {}

    private enum EmptyEnum2 {}

    public static byte[] main() {
        return null;
    }

    public static String combineDifferentEnums(boolean flag, EmptyEnum1 emptyEnum1, EmptyEnum2 emptyEnum2) {
        return (flag ? emptyEnum1 : emptyEnum2).name();
    }

    public static String combineDifferentEnums2(boolean flag, EmptyEnum1 emptyEnum1, EmptyEnum2 emptyEnum2) {
        return (flag ? emptyEnum1 : emptyEnum2).toString();
    }

    public static RootA combineCommonEnums(boolean flag, EnumA1 a1, EnumA2 a2) {
        return flag ? a1 : a2;
    }

    public static Object combineJcl(boolean flag, EmptyEnum1 emptyEnum1, List list) {
        return flag ? emptyEnum1 : list;
    }

    public static Object combineApi(boolean flag, EmptyEnum1 emptyEnum1, BlockchainRuntime runtime) {
        return flag ? emptyEnum1 : runtime;
    }

    public static Object combineUserlib(boolean flag, EmptyEnum1 emptyEnum1, AionList list) {
        return flag ? emptyEnum1 : list;
    }

    public static Object combineException(boolean flag, EmptyEnum1 emptyEnum1, NullPointerException e) {
        return flag ? emptyEnum1 : e;
    }

    public static Object combineArray(boolean flag, EmptyEnum1 emptyEnum1, NullPointerException[] e) {
        return flag ? emptyEnum1 : e;
    }

    public static String combineInterface(boolean flag, EmptyEnum1 emptyEnum1, RootA a) {
        return (flag ? emptyEnum1 : a).toString();
    }

    public static String combineClass(boolean flag, EmptyEnum1 emptyEnum1, ClassChild klazz) {
        return (flag ? emptyEnum1 : klazz).toString();
    }

    public static Object combineNull(boolean flag, EmptyEnum1 emptyEnum1) {
        return flag ? emptyEnum1 : null;
    }
}
