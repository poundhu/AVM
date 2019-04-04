package org.aion.avm.shadow.java.lang;

import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IPersistenceToken;
import org.aion.avm.RuntimeMethodFeeSchedule;
import org.aion.avm.shadow.java.io.Serializable;

public abstract class Number extends Object implements Serializable {
    static {
        // Shadow classes MUST be loaded during bootstrap phase.
        IInstrumentation.attachedThreadInstrumentation.get().bootstrapOnly();
    }

    public Number(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
    }

    public Number(){};

    public abstract int avm_intValue();

    public abstract long avm_longValue();

    public abstract float avm_floatValue();

    public abstract double avm_doubleValue();

    public byte avm_byteValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Number_avm_byteValue);
        return (byte)avm_intValue();
    }

    public short avm_shortValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Number_avm_shortValue);
        return (short)avm_intValue();
    }
}
