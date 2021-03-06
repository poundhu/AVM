package org.aion.avm.core;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import org.aion.avm.core.blockchainruntime.EmptyCapabilities;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.ABIUtil;
import org.aion.avm.core.util.Helpers;
import org.aion.kernel.AvmTransactionResult;
import org.aion.kernel.AvmTransactionResult.Code;
import org.aion.kernel.Block;
import org.aion.kernel.TestingKernel;
import org.aion.kernel.Transaction;
import org.aion.kernel.TransactionContextImpl;
import org.aion.types.Address;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ReentrantObjectArrayTest {
    // transaction
    private long energyLimit = 10_000_000L;
    private long energyPrice = 1L;

    // block
    private Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);

    // kernel & vm
    private TestingKernel kernel;
    private AvmImpl avm;

    private Address deployer = TestingKernel.PREMINED_ADDRESS;
    private Address dappAddress;

    @Before
    public void setup() {
        this.kernel = new TestingKernel();
        this.avm = CommonAvmFactory.buildAvmInstanceForConfiguration(new EmptyCapabilities(), new AvmConfiguration());
        
        byte[] jar = JarBuilder.buildJarForMainAndClassesAndUserlib(ReentrantObjectArrayTestResource.class);
        Transaction tx = Transaction.create(deployer, kernel.getNonce(deployer), BigInteger.ZERO, new CodeAndArguments(jar, null).encodeToBytes(), energyLimit, energyPrice);
        TransactionContext txContext = TransactionContextImpl.forExternalTransaction(tx, block);
        TransactionResult txResult = avm.run(this.kernel, new TransactionContext[] {txContext})[0].get();
        assertEquals(Code.SUCCESS, txResult.getResultCode());
        dappAddress = Address.wrap(txResult.getReturnData());
    }

    @After
    public void tearDown() {
        this.avm.shutdown();
    }

    @Test
    public void testReentrantStringArray() {
        byte[] data = ABIUtil.encodeMethodArguments("testString");
        Transaction tx = Transaction.call(deployer, dappAddress, kernel.getNonce(deployer), BigInteger.ZERO, data, energyLimit, energyPrice);
        TransactionContext txContext = TransactionContextImpl.forExternalTransaction(tx, block);
        AvmTransactionResult txResult = (AvmTransactionResult) avm.run(this.kernel, new TransactionContext[] {txContext})[0].get();

        assertEquals(Code.SUCCESS, txResult.getResultCode());
    }
}
