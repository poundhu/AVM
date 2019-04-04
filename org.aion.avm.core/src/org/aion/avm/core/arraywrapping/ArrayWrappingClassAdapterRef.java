package org.aion.avm.core.arraywrapping;

import org.aion.avm.core.ClassToolchain;
import org.aion.avm.core.types.PocClassHierarchy;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ArrayWrappingClassAdapterRef extends ClassToolchain.ToolChainClassVisitor {
    private final PocClassHierarchy hierarchy;
    private final boolean preserveDebuggability;

    public String className;

    public ArrayWrappingClassAdapterRef(PocClassHierarchy hierarchy, boolean preserveDebuggability) {
        super(Opcodes.ASM6);

        this.hierarchy = hierarchy;
        this.preserveDebuggability = preserveDebuggability;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new ArrayWrappingMethodAdapterRef(access, name, descriptor, signature, exceptions, mv, className, this.hierarchy, this.preserveDebuggability);
    }
}