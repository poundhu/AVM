package org.aion.avm.shadow.java.lang;

import org.aion.avm.arraywrapper.CharArray;
import org.aion.avm.internal.CodecIdioms;
import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IObject;
import org.aion.avm.internal.IObjectDeserializer;
import org.aion.avm.internal.IObjectSerializer;
import org.aion.avm.internal.IPersistenceToken;
import org.aion.avm.RuntimeMethodFeeSchedule;
import org.aion.avm.shadow.java.io.Serializable;

/**
 * TODO:  Ensure that none of the interface we have provided exposes underlying implementation details (slack buffer space, etc), since we would
 * otherwise need to take that into account with our serialization strategy.
 * NOTE:  We may want to develop a policy around when an avm_ method can call another avm_ method since this can result in redundant billing
 * which needs to be part fo the specification.
 */
public final class StringBuilder extends Object implements CharSequence, Serializable, Appendable{
    static {
        // Shadow classes MUST be loaded during bootstrap phase.
        IInstrumentation.attachedThreadInstrumentation.get().bootstrapOnly();
    }

    public StringBuilder() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_constructor);
        this.v = new java.lang.StringBuilder();
    }

    public StringBuilder(int capacity) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_constructor_1);
        this.v = new java.lang.StringBuilder(capacity);
    }

    public StringBuilder(String str) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_constructor_2);
        this.v = new java.lang.StringBuilder(str.getUnderlying());
    }

    public StringBuilder(CharSequence seq){
        this.v = new java.lang.StringBuilder(seq.avm_toString().getUnderlying());
    }

    public StringBuilder avm_append(IObject obj) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append);
        // Note that we want to convert this to a string, at our level, so we can call avm_toString() - the lower-level will call toString().
        this.v.append(String.internalValueOfObject(obj));
        return this;
    }

    public StringBuilder avm_append(String str) {
        int lengthForBilling = (null != str)
                ? str.internalLength()
                : 0;
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_1 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * lengthForBilling);
        java.lang.String underlying = (null != str)
                ? str.getUnderlying()
                : null;
        this.v.append(underlying);
        return this;
    }

    public StringBuilder avm_append(StringBuffer sb) {
        int lengthForBilling = (null != sb)
                ? sb.internalLength()
                : 0;
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_2 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * lengthForBilling);
        java.lang.StringBuffer underlying = (null != sb)
                ? sb.getUnderlying()
                : null;
        this.v.append(underlying);
        return this;
    }

    public StringBuilder avm_append(CharArray str) {
        int lengthForBilling = (null != str)
                ? str.length()
                : 0;
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_3 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * lengthForBilling);
        char[] underlying = (null != str)
                ? str.getUnderlying()
                : null;
        // Note that this actually will throw NPE if given null.
        this.v.append(underlying);
        return this;
    }

    public StringBuilder avm_append(CharArray str, int offset, int len) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_4 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * len);
        char[] underlying = (null != str)
                ? str.getUnderlying()
                : null;
        // Note that this actually will throw NPE if given null.
        this.v.append(underlying, offset, len);
        return this;
    }

    public StringBuilder avm_append(CharSequence s){
        int lengthForBilling = (null != s)
                ? s.avm_length()
                : 0;
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_5 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * lengthForBilling);
        java.lang.String asString = (null != s)
                ? s.avm_toString().getUnderlying()
                : null;
        this.v.append(asString);
        return this;
    }

    public StringBuilder avm_append(CharSequence s, int start, int end){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_6 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * java.lang.Math.max(end - start, 0));
        java.lang.String asString = (null != s)
                ? s.avm_toString().getUnderlying()
                : null;
        this.v.append(asString, start, end);
        return this;
    }

    public StringBuilder avm_append(boolean b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_7);
        this.v.append(b);
        return this;
    }

    public StringBuilder avm_append(char c) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_8);
        this.v.append(c);
        return this;
    }

    public StringBuilder avm_append(int i) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_9);
        this.v.append(i);
        return this;
    }

    public StringBuilder avm_append(long lng) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_10);
        this.v.append(lng);
        return this;
    }

    public StringBuilder avm_append(float f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_11);
        this.v.append(f);
        return this;
    }

    public StringBuilder avm_append(double d) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_append_12);
        this.v.append(d);
        return this;
    }

    public StringBuilder avm_appendCodePoint(int codePoint) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_appendCodePoint);
        this.v.appendCodePoint(codePoint);
        return this;
    }

    public StringBuilder avm_delete(int start, int end) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_delete);
        this.v.delete(start, end);
        return this;
    }

    public StringBuilder avm_deleteCharAt(int index) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_deleteCharAt);
        this.v.deleteCharAt(index);
        return this;
    }

    public StringBuilder avm_replace(int start, int end, String str) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_replace + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * java.lang.Math.max(end - start, 0));
        this.v = this.v.replace(start, end, str.getUnderlying());
        return this;
    }

    public StringBuilder avm_insert(int index, CharArray str, int offset,
                                                int len)
    {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * len);
        this.v.insert(index, str.getUnderlying(), offset, len);
        return this;
    }

    public StringBuilder avm_insert(int offset, IObject obj) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_1 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * java.lang.Math.max(internalLength() - offset, 0));
        this.v.insert(offset, obj);
        return this;
    }

    public StringBuilder avm_insert(int offset, String str) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_2 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * (str.internalLength() + java.lang.Math.max(internalLength() - offset, 0)));
        this.v.insert(offset, str.getUnderlying());
        return this;
    }

    public StringBuilder avm_insert(int offset, CharArray str) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_3 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * (str.length() + java.lang.Math.max(internalLength() - offset, 0)));
        this.v.insert(offset, str.getUnderlying());
        return this;
    }

    public StringBuilder avm_insert(int dstOffset, CharSequence s) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_4 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * (s.avm_length() + java.lang.Math.max(internalLength() - dstOffset, 0)));
        this.v.insert(dstOffset, s.avm_toString().getUnderlying());
        return this;
    }

    public StringBuilder avm_insert(int dstOffset, CharSequence s, int start, int end) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_5 + java.lang.Math.max(end - start, 0) + java.lang.Math.max(internalLength() - dstOffset, 0));
        this.v.insert(dstOffset, s.avm_subSequence(start, end).avm_toString().getUnderlying());
        return this;
    }

    public StringBuilder avm_insert(int offset, boolean b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_6);
        this.v.insert(offset, b);
        return this;
    }

    public StringBuilder avm_insert(int offset, char c) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_7);
        this.v.insert(offset, c);
        return this;
    }

    public StringBuilder avm_insert(int offset, int i) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_8);
        this.v.insert(offset, i);
        return this;
    }

    public StringBuilder avm_insert(int offset, long l) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_9);
        this.v.insert(offset, l);
        return this;
    }

    public StringBuilder avm_insert(int offset, float f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_10);
        this.v.insert(offset, f);
        return this;
    }

    public StringBuilder avm_insert(int offset, double d) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_insert_11);
        this.v.insert(offset, d);
        return this;
    }

    public int avm_indexOf(String str) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_indexOf + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * internalLength());
        return this.v.indexOf(str.getUnderlying());
    }

    public int avm_indexOf(String str, int fromIndex) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_indexOf_1 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * java.lang.Math.max(internalLength() - fromIndex, 0));
        return this.v.indexOf(str.getUnderlying(), fromIndex);
    }

    public int avm_lastIndexOf(String str) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_lastIndexOf + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * internalLength());
        return this.v.lastIndexOf(str.getUnderlying());
    }

    public int avm_lastIndexOf(String str, int fromIndex) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_lastIndexOf_1 + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * java.lang.Math.max(internalLength() - fromIndex, 0));
        return this.v.lastIndexOf(str.getUnderlying(), fromIndex);
    }

    public StringBuilder avm_reverse() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_reverse + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * internalLength());
        this.v.reverse();
        return this;
    }

    public String avm_toString() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_toString);
        return internalToString();
    }

    public char avm_charAt(int index){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_charAt);
        return this.v.charAt(index);
    }

    public CharSequence avm_subSequence(int start, int end) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_subSequence + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * java.lang.Math.max(end - start, 0));
        // Call substring instead of subSequence, since our String wrapper wraps a String, not a CharSequence.
        return new String (this.getUnderlying().substring(start, end));
    }

    public int avm_length(){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.StringBuilder_avm_length);
        return internalLength();
    }

    //========================================================
    // Methods below are used by runtime and test code only!
    //========================================================

    private java.lang.StringBuilder v;

    public java.lang.StringBuilder getUnderlying() {
        return v;
    }

    // Deserializer support.
    public StringBuilder(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
    }

    public void deserializeSelf(java.lang.Class<?> firstRealImplementation, IObjectDeserializer deserializer) {
        super.deserializeSelf(String.class, deserializer);
        
        // We serialize this as a string.
        java.lang.String simpler = CodecIdioms.deserializeString(deserializer);
        this.v = new java.lang.StringBuilder(simpler);
    }

    public void serializeSelf(java.lang.Class<?> firstRealImplementation, IObjectSerializer serializer) {
        super.serializeSelf(String.class, serializer);
        
        // We serialize this as a string.
        CodecIdioms.serializeString(serializer, this.v.toString());
    }

    public int internalLength(){
        return new java.lang.String(getUnderlying()).length();
    }

    public String internalToString(){
        return new String(new java.lang.String(getUnderlying()));
    }

    //========================================================
    // Methods below are deprecated
    //========================================================


    //========================================================
    // Methods below are excluded from shadowing
    //========================================================

}
