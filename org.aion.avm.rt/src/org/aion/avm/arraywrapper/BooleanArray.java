package org.aion.avm.arraywrapper;

import org.aion.avm.internal.*;
import java.util.Arrays;

import org.aion.avm.RuntimeMethodFeeSchedule;

public class BooleanArray extends Array {

    private boolean[] underlying;

    /**
     * Static BooleanArray factory
     *
     * After instrumentation, NEWARRAY bytecode (with boolean as type) will be replaced by a INVOKESTATIC to
     * this method.
     *
     * @param size Size of the boolean array
     *
     * @return New empty boolean array wrapper
     */
    public static BooleanArray initArray(int size){
        chargeEnergy(size * ArrayElement.BYTE.getEnergy());
        return new BooleanArray(size);
    }

    @Override
    public int length() {
        lazyLoad();
        return this.underlying.length;
    }

    public boolean get(int idx) {
        lazyLoad();
        return this.underlying[idx];
    }

    public void set(int idx, boolean val) {
        lazyLoad();
        this.underlying[idx] = val;
    }

    @Override
    public IObject avm_clone() {
        IHelper.currentContractHelper.get().externalChargeEnergy(RuntimeMethodFeeSchedule.ByteArray_avm_clone + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * length());
        lazyLoad();
        return new BooleanArray(Arrays.copyOf(underlying, underlying.length));
    }

    @Override
    public IObject clone() {
        lazyLoad();
        return new BooleanArray(Arrays.copyOf(underlying, underlying.length));
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        lazyLoad();
        return obj instanceof BooleanArray && Arrays.equals(this.underlying, ((BooleanArray) obj).underlying);
    }

    @Override
    public java.lang.String toString() {
        lazyLoad();
        return Arrays.toString(this.underlying);
    }

    //========================================================
    // Internal Helper
    //========================================================

    public BooleanArray(int c) {
        IHelper.currentContractHelper.get().externalChargeEnergy(RuntimeMethodFeeSchedule.ByteArray_avm_constructor);
        this.underlying = new boolean[c];
    }

    public BooleanArray(boolean[] underlying) {
        IHelper.currentContractHelper.get().externalChargeEnergy(RuntimeMethodFeeSchedule.ByteArray_avm_constructor_1);
        RuntimeAssertionError.assertTrue(null != underlying);
        this.underlying = underlying;
    }

    public boolean[] getUnderlying() {
        lazyLoad();
        return underlying;
    }

    @Override
    public void setUnderlyingAsObject(java.lang.Object u){
        RuntimeAssertionError.assertTrue(null != u);
        lazyLoad();
        this.underlying = (boolean[]) u;
    }

    @Override
    public java.lang.Object getUnderlyingAsObject(){
        lazyLoad();
        return underlying;
    }

    @Override
    public java.lang.Object getAsObject(int idx){
        lazyLoad();
        return this.underlying[idx];
    }

    //========================================================
    // Persistent Memory Support
    //========================================================

    public BooleanArray(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
    }

    public void deserializeSelf(java.lang.Class<?> firstRealImplementation, IObjectDeserializer deserializer) {
        super.deserializeSelf(BooleanArray.class, deserializer);

        this.underlying = CodecIdioms.deserializeBooleanArray(deserializer);
    }

    public void serializeSelf(java.lang.Class<?> firstRealImplementation, IObjectSerializer serializer) {
        super.serializeSelf(BooleanArray.class, serializer);

        CodecIdioms.serializeBooleanArray(serializer, this.underlying);
    }
}
