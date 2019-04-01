package org.aion.avm.core.unification;

import java.math.BigInteger;

import org.aion.avm.core.AvmConfiguration;
import org.aion.avm.core.AvmImpl;
import org.aion.avm.core.CommonAvmFactory;
import org.aion.avm.core.blockchainruntime.EmptyCapabilities;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.userlib.AionBuffer;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.abi.ABICodec;
import org.aion.avm.userlib.abi.ABIEncoder;
import org.aion.avm.userlib.abi.ABIException;
import org.aion.avm.userlib.abi.ABIToken;
import org.aion.kernel.AvmTransactionResult;
import org.aion.kernel.Block;
import org.aion.kernel.TestingKernel;
import org.aion.kernel.TransactionContextImpl;
import org.aion.types.Address;
import org.aion.kernel.Transaction;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * WARNING:  These tests currently fail in order to demonstrate the problem reported by issue-362.
 * TODO:  Ensure that none of these failures still count as "pass" before we close issue-362.
 */
public class CommonSuperClassTest {
    private static long ENERGY_LIMIT = 10_000_000L;
    private static long ENERGY_PRICE = 1L;
    private static Address DEPLOYER = TestingKernel.PREMINED_ADDRESS;
    private static Block BLOCK = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
    private static TestingKernel KERNEL = new TestingKernel();
    private AvmImpl avm;

    @Before
    public void setup() {
        AvmConfiguration config = new AvmConfiguration();
        config.enableVerboseContractErrors = true;
        avm = CommonAvmFactory.buildAvmInstanceForConfiguration(new EmptyCapabilities(), config);
    }

    @After
    public void tearDown() {
        try {
            avm.shutdown();
        } catch (Throwable t) {
            // Note that combineClassAndJclInterface() leaves the AVM instance in a broken state, which will be reported here, but we aren't concerned.
        }
    }

    @Test(expected = AssertionError.class)
    public void combineInterfaces() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithInterfaces.class, CommonSuperClassTypes.class,
            AionList.class, AionBuffer.class, ABIEncoder.class, ABICodec.class, ABIToken.class, ABIException.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test(expected = AssertionError.class)
    public void combineJcl() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithJcl.class, CommonSuperClassTypes.class, ABIException.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test(expected =  AssertionError.class)
    public void combineApi() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithApi.class, CommonSuperClassTypes.class, AionBuffer.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test(expected = AssertionError.class)
    public void combineUserlib() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithUserlib.class, CommonSuperClassTypes.class, AionMap.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test
    public void combineEnums() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithEnums.class, CommonSuperClassTypes.class, AionList.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.FAILED_EXCEPTION, deploymentResult.getResultCode());
    }

    @Test
    public void combineExceptions() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithExceptions.class, CommonSuperClassTypes.class, AionMap.class, ABIException.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();
        
        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.FAILED_EXCEPTION, deploymentResult.getResultCode());
    }

    @Test(expected = AssertionError.class)
    public void combineArrays() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineWithArrays.class, CommonSuperClassTypes.class, AionBuffer.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test
    public void combineAmbiguousUserClasses() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineAmbiguousClasses.class, CommonSuperClassTypes.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();

        // Should be rejected because of VerifyError for ambiguous classes
        Assert.assertEquals(AvmTransactionResult.Code.FAILED_EXCEPTION, deploymentResult.getResultCode());
    }

    @Test
    public void combineAmbiguousArrays() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineAmbiguousArrays.class, CommonSuperClassTypes.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();

        // Should be rejected because of VerifyError for ambiguous classes
        Assert.assertEquals(AvmTransactionResult.Code.FAILED_EXCEPTION, deploymentResult.getResultCode());
    }

    @Test
    public void combineAmbiguousEnums() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineAmbiguousEnums.class, CommonSuperClassTypes.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();

        // Should be rejected because of VerifyError for ambiguous classes ??????????
        // This compiles fine, but I suspect if we try to call into this something will break
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test
    public void combineAmbiguousExceptions() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineAmbiguousExceptions.class, CommonSuperClassTypes.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();

        // Should be rejected because of VerifyError for ambiguous classes ??????????
        // This compiles fine, but I suspect if we try to call into this something will break
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test
    public void combineAmbiguousJclClasses() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(CommonSuperClassTarget_combineAmbiguousJcl.class, CommonSuperClassTypes.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();

        // Should be rejected because of VerifyError for ambiguous classes ????????
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }


    @Test
    public void watchClassHierarchy() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(UnificationSample.class, AionBuffer.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }

    @Test
    public void watchArrayClassHierarchy() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(UnificationArraySample.class, AionBuffer.class);
        byte[] arguments = new byte[0];
        byte[] txData = new CodeAndArguments(jar, arguments).encodeToBytes();

        Transaction deployment = Transaction.create(DEPLOYER, KERNEL.getNonce(DEPLOYER), BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult deploymentResult = avm.run(KERNEL, new TransactionContext[] {TransactionContextImpl.forExternalTransaction(deployment, BLOCK)})[0].get();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, deploymentResult.getResultCode());
    }
}
