package org.aion.avm.shadow.java.lang;

import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IObject;
import org.aion.avm.internal.IPersistenceToken;
import org.aion.avm.RuntimeMethodFeeSchedule;
import org.aion.avm.shadow.java.io.Serializable;

public abstract class Enum<E extends Enum<E>> extends Object implements Serializable {
    static {
        // Shadow classes MUST be loaded during bootstrap phase.
        IInstrumentation.attachedThreadInstrumentation.get().bootstrapOnly();
    }

    // (note that these are not final since we want to be able to deserialize this type)
    private String name;
    private int ordinal;

    public final String avm_name() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_name);
        lazyLoad();
        return name;
    }

    public final int avm_ordinal() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_ordinal);
        lazyLoad();
        return ordinal;
    }

    protected Enum(String name, int ordinal) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_constructor);
        this.name = name;
        this.ordinal = ordinal;
    }

    // Deserializer support.
    public Enum(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
    }

    public String avm_toString() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_toString);
        lazyLoad();
        return name;
    }

    public final boolean avm_equals(IObject other) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_equals);
        lazyLoad();
        return this == other;
    }

    public final int avm_hashCode() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_hashCode);
        lazyLoad();
        return this.ordinal;
    }

    @Override
    public final Object avm_clone() throws CloneNotSupportedException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_clone);
        throw new CloneNotSupportedException();
    }

    public static <T extends Enum<T>> T avm_valueOf(Class<T> enumType,
                                                String name) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Enum_avm_valueOf);
        T result = enumType.enumConstantDirectory().get(name);
        if (result != null)
            return result;
        if (name == null)
            throw new NullPointerException("Name is null");
        throw new IllegalArgumentException(
                "No enum constant " + enumType.avm_getName() + "." + name);
    }

    @SuppressWarnings("deprecation")
    protected final void finalize() { }

}
