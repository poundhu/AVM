package org.aion.avm.testcontracts;

import org.aion.avm.rt.Contract;
import org.aion.avm.rt.BlockchainRuntime;

public class DummyContract extends Contract {

    @Override
    public byte[] run(byte[] input, BlockchainRuntime rt) {
        C1 c = new C1();
        c.getC2();

        return null;
    }
}
