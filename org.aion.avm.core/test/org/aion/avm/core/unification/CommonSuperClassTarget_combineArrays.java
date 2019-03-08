package org.aion.avm.core.unification;

import java.math.BigInteger;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;


public class CommonSuperClassTarget_combineArrays {
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(CommonSuperClassTarget_combineArrays.class, BlockchainRuntime.getData());
    }

    public static String combineOverlappingArrays(boolean flag, CommonSuperClassTypes.RootA[] root, CommonSuperClassTypes.ChildA[] child) {
        return (flag ? root : child)[0].getRootA();
    }
/*
    public static String combineArrayAndException(boolean flag, CommonSuperClassTypes.RootA[] root, RuntimeException exception) {
        return (flag ? root[0] : exception).toString();
    }

    public static String combineArrayAndUserCode(boolean flag, CommonSuperClassTypes.RootA[] root, CommonSuperClassTarget_combineArrays object) {
        return (flag ? root[0] : object).toString();
    }

    public static String combineArrayAndJclCode(boolean flag, CommonSuperClassTypes.RootA[] root, BigInteger object) {
        return (flag ? root[0] : object).toString();
    }*/
}
