package org.aion.avm.core.types;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class PocClassVisitor extends ClassVisitor {
    private PocClassInfo classInfo;
    private boolean isPreRenameVisitor;

    /**
     * If {@code isPreRenameVisitor == true} then this visitor must be visiting classes that are
     * already renamed (shadow JCL, API).
     */
    public PocClassVisitor(boolean isPreRenameVisitor) {
        super(Opcodes.ASM6);
        this.isPreRenameVisitor = isPreRenameVisitor;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String parentQualifiedName = toQualifiedName(superName);
        String[] interfaceQualifiedNames = toQualifiedNames(interfaces);
        boolean isInterface = Opcodes.ACC_INTERFACE == (access & Opcodes.ACC_INTERFACE);
        boolean isFinalClass = Opcodes.ACC_FINAL == (access & Opcodes.ACC_FINAL);

        if (this.isPreRenameVisitor) {
            this.classInfo = PocClassInfo.preRenameInfoFor(isInterface, isFinalClass, toQualifiedName(name), parentQualifiedName, interfaceQualifiedNames);
        } else {
            this.classInfo = PocClassInfo.postRenameInfoFor(isInterface, isFinalClass, toQualifiedName(name), parentQualifiedName, interfaceQualifiedNames);
        }
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
    }


    public PocClassInfo getClassInfo() {
        return this.classInfo;
    }

    /**
     * Returns the class names as qualified (dot) names if classNames is not null and not empty.
     *
     * Returns null if classNames is null or empty.
     */
    private static String[] toQualifiedNames(String[] classNames) {
        if (classNames == null) {
            return null;
        }

        int length = classNames.length;
        if (length == 0) {
            return null;
        }

        String[] qualifiedNames = new String[length];
        for (int i = 0; i < length; i++) {
            qualifiedNames[i] = toQualifiedName(classNames[i]);
        }
        return qualifiedNames;
    }

    /**
     * Returns the class name as a qualified (dot) name if className is not null.
     *
     * Returns null if className is null.
     */
    private static String toQualifiedName(String className) {
        return (className == null) ? null : className.replaceAll("/", ".");
    }
}
