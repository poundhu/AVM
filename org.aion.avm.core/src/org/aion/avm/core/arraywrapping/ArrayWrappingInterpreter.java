package org.aion.avm.core.arraywrapping;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * A bytecode interpreter used for array type inference.
 *
 * See {@link org.aion.avm.core.arraywrapping.ArrayWrappingClassAdapterRef} for its usage.
 */

public class ArrayWrappingInterpreter extends BasicInterpreter{

    ArrayWrappingInterpreter() {
      super(Opcodes.ASM6);
    }

    @Override
    // Override this method to get unmasked type from BasicInterpreter
    public BasicValue newValue(final Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
            case Type.ARRAY:
            case Type.OBJECT:
                return new BasicValue(type);
            default:
                throw new AssertionError();
        }
    }

    @Override
    public BasicValue binaryOperation(
            final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2)
            throws AnalyzerException {
        switch (insn.getOpcode()) {
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
                return BasicValue.INT_VALUE;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return BasicValue.FLOAT_VALUE;
            case LALOAD:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                return BasicValue.LONG_VALUE;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return BasicValue.DOUBLE_VALUE;
            case AALOAD:
                System.out.println("AALOAD CRACKING: \"" + value1.toString() + "\"");
                return newValue(Type.getType(value1.toString().substring(1)));
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return BasicValue.INT_VALUE;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        System.out.println("MERGE: ");
        System.out.println("\t" +  value1);
        System.out.println("\t" +  value2);
        
        BasicValue result =  super.merge(value1, value2);
        if (("[Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$ChildA;".equals(value1.toString()) && "[Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$RootA;".equals(value2.toString())) 
            || ("[Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$ChildA;".equals(value2.toString()) && "[Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$RootA;".equals(value1.toString()))) {
            result = newValue(Type.getType("[Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$RootA;"));
        } else if (("Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$ChildA;".equals(value1.toString()) && "Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$RootA;".equals(value2.toString())) 
                || ("Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$ChildA;".equals(value2.toString()) && "Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$RootA;".equals(value1.toString()))) {
            result = newValue(Type.getType("Lorg/aion/avm/user/org/aion/avm/core/unification/CommonSuperClassTypes$RootA;"));
        }
        System.out.println("\t=> " +  result);
        return result;
    }

}
