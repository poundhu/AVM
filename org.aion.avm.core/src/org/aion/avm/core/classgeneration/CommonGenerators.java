package org.aion.avm.core.classgeneration;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.aion.avm.core.ClassToolchain;
import org.aion.avm.core.arraywrapping.ArrayWrappingClassAdapter;
import org.aion.avm.core.arraywrapping.ArrayWrappingClassAdapterRef;
import org.aion.avm.core.miscvisitors.NamespaceMapper;
import org.aion.avm.core.miscvisitors.PreRenameClassAccessRules;
import org.aion.avm.core.miscvisitors.UserClassMappingVisitor;
import org.aion.avm.core.shadowing.ClassShadowing;
import org.aion.avm.core.types.PocClassInfo;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.internal.PackageConstants;
import org.aion.avm.internal.RuntimeAssertionError;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;


/**
 * Contains some of the common constants and code-generation idioms used in various tests and/or across the system, in general.
 */
public class CommonGenerators {
    // There doesn't appear to be any way to enumerate these classes in the existing class loader (even though they are part of java.lang)
    // so we will list the names of all the classes we need and assemble them that way.
    // We should at least be able to use the original Throwable's classloader to look up the subclasses (again, since they are in java.lang).
    // Note:  "java.lang.VirtualMachineError" and children are deliberately absent from this since user code can never see them.
    public static final String[] kExceptionClassNames = new String[] {
            "java.lang.Error",
            "java.lang.AssertionError",
            "java.lang.LinkageError",
            "java.lang.BootstrapMethodError",
            "java.lang.ClassCircularityError",
            "java.lang.ClassFormatError",
            "java.lang.UnsupportedClassVersionError",
            "java.lang.ExceptionInInitializerError",
            "java.lang.IncompatibleClassChangeError",
            "java.lang.AbstractMethodError",
            "java.lang.IllegalAccessError",
            "java.lang.InstantiationError",
            "java.lang.NoSuchFieldError",
            "java.lang.NoSuchMethodError",
            "java.lang.NoClassDefFoundError",
            "java.lang.UnsatisfiedLinkError",
            "java.lang.VerifyError",
            "java.lang.ThreadDeath",
            "java.lang.Exception",
            "java.lang.CloneNotSupportedException",
            "java.lang.InterruptedException",
            "java.lang.ReflectiveOperationException",
            "java.lang.ClassNotFoundException",
            "java.lang.IllegalAccessException",
            "java.lang.InstantiationException",
            "java.lang.NoSuchFieldException",
            "java.lang.NoSuchMethodException",
            "java.lang.RuntimeException",
            "java.lang.ArithmeticException",
            "java.lang.ArrayStoreException",
            "java.lang.ClassCastException",
            "java.lang.EnumConstantNotPresentException",
            "java.lang.IllegalArgumentException",
            "java.lang.IllegalThreadStateException",
            "java.lang.NumberFormatException",
            "java.lang.IllegalCallerException",
            "java.lang.IllegalMonitorStateException",
            "java.lang.IllegalStateException",
            "java.lang.IndexOutOfBoundsException",
            "java.lang.ArrayIndexOutOfBoundsException",
            "java.lang.StringIndexOutOfBoundsException",
            "java.lang.LayerInstantiationException",
            "java.lang.NegativeArraySizeException",
            "java.lang.NullPointerException",
            "java.lang.SecurityException",
            "java.lang.TypeNotPresentException",
            "java.lang.UnsupportedOperationException",

            "java.util.NoSuchElementException",
            "java.nio.BufferUnderflowException",
            "java.nio.BufferOverflowException"
    };

    public enum JclException {
        THROWABLE("java.lang.Throwable"),
        ERROR("java.lang.Error"),
        ASSERTION_ERROR("java.lang.AssertionError"),
        LINKAGE_ERROR("java.lang.LinkageError"),
        BOOTSTRAP_ERROR("java.lang.BootstrapMethodError"),
        CIRCULARITY_ERROR("java.lang.ClassCircularityError"),
        CLASS_FORMAT_ERROR("java.lang.ClassFormatError"),
        UNSUPPORTED_VERSION_ERROR("java.lang.UnsupportedClassVersionError"),
        INITIALIZER_ERROR("java.lang.ExceptionInInitializerError"),
        INCOMPATIBLE_CHANGE_ERROR("java.lang.IncompatibleClassChangeError"),
        ABSTRACT_METHOD_ERROR("java.lang.AbstractMethodError"),
        ILLEGAL_ACCESS_ERROR("java.lang.IllegalAccessError"),
        INSTANTIATION_ERROR("java.lang.InstantiationError"),
        NO_SUCH_FIELD_ERROR("java.lang.NoSuchFieldError"),
        NO_SUCH_METHOD_ERROR("java.lang.NoSuchMethodError"),
        NO_CLASS_DEF_ERROR("java.lang.NoClassDefFoundError"),
        UNSATISFIED_LINK_ERROR("java.lang.UnsatisfiedLinkError"),
        VERIFY_ERROR("java.lang.VerifyError"),
        THREAD_DEATH("java.lang.ThreadDeath"),
        EXCEPTION("java.lang.Exception"),
        CLONE_EXCEPTION("java.lang.CloneNotSupportedException"),
        INTERRUPTED_EXCEPTION("java.lang.InterruptedException"),
        REFLECTIVE_EXCEPTION("java.lang.ReflectiveOperationException"),
        CLASS_NOT_FOUND_EXCEPTION("java.lang.ClassNotFoundException"),
        ILLEGAL_ACCESS_EXCEPTION("java.lang.IllegalAccessException"),
        INSTANTIATION_EXCEPTION("java.lang.InstantiationException"),
        NO_SUCH_FIELD_EXCEPTION("java.lang.NoSuchFieldException"),
        NO_SUCH_METHOD_EXCEPTION("java.lang.NoSuchMethodException"),
        RUNTIME_EXCEPTION("java.lang.RuntimeException"),
        ARITHMETIC_EXCEPTION("java.lang.ArithmeticException"),
        ARRAY_STORE_EXCEPTION("java.lang.ArrayStoreException"),
        CLASS_CAST_EXCEPTION("java.lang.ClassCastException"),
        ENUM_CONSTANT_EXCEPTION("java.lang.EnumConstantNotPresentException"),
        ILLEGAL_ARGUMENT_EXCEPTION("java.lang.IllegalArgumentException"),
        ILLEGAL_THREAD_EXCEPTION("java.lang.IllegalThreadStateException"),
        NUMBER_FORMAT_EXCEPTION("java.lang.NumberFormatException"),
        ILLEGAL_CALLER_EXCEPTION("java.lang.IllegalCallerException"),
        ILLEGAL_MONITOR_EXCEPTION("java.lang.IllegalMonitorStateException"),
        ILLEGAL_STATE_EXCEPTION("java.lang.IllegalStateException"),
        INDEX_BOUNDS_EXCEPTION("java.lang.IndexOutOfBoundsException"),
        ARRAY_BOUNDS_EXCEPTION("java.lang.ArrayIndexOutOfBoundsException"),
        STRING_BOUNDS_EXCEPTION("java.lang.StringIndexOutOfBoundsException"),
        LAYER_INSTANTIATION_EXCEPTION("java.lang.LayerInstantiationException"),
        NEGATIVE_ARRAY_SIZE_EXCEPTION("java.lang.NegativeArraySizeException"),
        NULL_POINTER_EXCEPTION("java.lang.NullPointerException"),
        SECURITY_EXCEPTION("java.lang.SecurityException"),
        TYPE_NOT_PRESENT_EXCEPTION("java.lang.TypeNotPresentException"),
        UNSUPPORTED_OPERATION_EXCEPTION("java.lang.UnsupportedOperationException"),
        NO_SUCH_ELEMENT_EXCEPTION("java.util.NoSuchElementException"),
        BUFFER_UNDERFLOW_EXCEPTION("java.nio.BufferUnderflowException"),
        BUFFER_OVERFLOW_EXCEPTION("java.nio.BufferOverflowException");

        private String name;

        JclException(String name) {
            this.name = name;
        }

        public String getQualifiedName() {
            return this.name;
        }

        public String getShadowQualifiedName() {
            return PackageConstants.kShadowDotPrefix + this.name;
        }
    }

    public static Set<PocClassInfo> getAllShadowExceptionClassInfos() {
        Set<PocClassInfo> classInfos = new HashSet<>();

        //TODO: not all of these are in our shadow lib -- is this ok? (more than just the 'handwritten' ones listed below)

//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.THROWABLE.getShadowQualifiedName(), PocClassInfo.SHADOW_OBJECT, null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ERROR.getShadowQualifiedName(), JclException.THROWABLE.getShadowQualifiedName(), null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ASSERTION_ERROR.getShadowQualifiedName(), JclException.ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.LINKAGE_ERROR.getShadowQualifiedName(), JclException.ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.BOOTSTRAP_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.CIRCULARITY_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.CLASS_FORMAT_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.UNSUPPORTED_VERSION_ERROR.getShadowQualifiedName(), JclException.CLASS_FORMAT_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.INITIALIZER_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.INCOMPATIBLE_CHANGE_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ABSTRACT_METHOD_ERROR.getShadowQualifiedName(), JclException.INCOMPATIBLE_CHANGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_ACCESS_ERROR.getShadowQualifiedName(), JclException.INCOMPATIBLE_CHANGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.INSTANTIATION_ERROR.getShadowQualifiedName(), JclException.INCOMPATIBLE_CHANGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NO_SUCH_FIELD_ERROR.getShadowQualifiedName(), JclException.INCOMPATIBLE_CHANGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NO_SUCH_METHOD_ERROR.getShadowQualifiedName(), JclException.INCOMPATIBLE_CHANGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NO_CLASS_DEF_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.UNSATISFIED_LINK_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.VERIFY_ERROR.getShadowQualifiedName(), JclException.LINKAGE_ERROR.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.THREAD_DEATH.getShadowQualifiedName(), JclException.ERROR.getShadowQualifiedName(), null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.EXCEPTION.getShadowQualifiedName(), JclException.THROWABLE.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.CLONE_EXCEPTION.getShadowQualifiedName(), JclException.EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.INTERRUPTED_EXCEPTION.getShadowQualifiedName(), JclException.EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.REFLECTIVE_EXCEPTION.getShadowQualifiedName(), JclException.EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.CLASS_NOT_FOUND_EXCEPTION.getShadowQualifiedName(), JclException.REFLECTIVE_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_ACCESS_EXCEPTION.getShadowQualifiedName(), JclException.REFLECTIVE_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.INSTANTIATION_EXCEPTION.getShadowQualifiedName(), JclException.REFLECTIVE_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NO_SUCH_FIELD_EXCEPTION.getShadowQualifiedName(), JclException.REFLECTIVE_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NO_SUCH_METHOD_EXCEPTION.getShadowQualifiedName(), JclException.REFLECTIVE_EXCEPTION.getShadowQualifiedName(), null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), JclException.EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ARITHMETIC_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ARRAY_STORE_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.CLASS_CAST_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ENUM_CONSTANT_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_ARGUMENT_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_THREAD_EXCEPTION.getShadowQualifiedName(), JclException.ILLEGAL_ARGUMENT_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NUMBER_FORMAT_EXCEPTION.getShadowQualifiedName(), JclException.ILLEGAL_ARGUMENT_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_CALLER_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_MONITOR_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ILLEGAL_STATE_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.INDEX_BOUNDS_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.ARRAY_BOUNDS_EXCEPTION.getShadowQualifiedName(), JclException.INDEX_BOUNDS_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.STRING_BOUNDS_EXCEPTION.getShadowQualifiedName(), JclException.INDEX_BOUNDS_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.LAYER_INSTANTIATION_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NEGATIVE_ARRAY_SIZE_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NULL_POINTER_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.SECURITY_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.TYPE_NOT_PRESENT_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.UNSUPPORTED_OPERATION_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
//        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.NO_SUCH_ELEMENT_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.BUFFER_UNDERFLOW_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));
        classInfos.add(PocClassInfo.postRenameInfoFor(false, false, JclException.BUFFER_OVERFLOW_EXCEPTION.getShadowQualifiedName(), JclException.RUNTIME_EXCEPTION.getShadowQualifiedName(), null));

        return classInfos;
    }

    private static Set<String> allJclExceptions = null;

    public static boolean isJclExceptionType(String className) {
        if (allJclExceptions == null) {
            Set<String> exceptions = new HashSet<>(Arrays.asList(CommonGenerators.kExceptionClassNames));
            exceptions.addAll(CommonGenerators.kHandWrittenExceptionClassNames);
            exceptions.addAll(CommonGenerators.kLegacyExceptionClassNames);
            exceptions.add("java.lang.Throwable");  // Missing from the other lists, but definitely an exception.
            allJclExceptions = exceptions;
        }
        return allJclExceptions.contains(className);
    }

    // We don't generate the shadows for these ones since we have hand-written them (but wrappers are still required).
    public static final Set<String> kHandWrittenExceptionClassNames = Set.of(new String[] {
            "java.lang.Error",
            "java.lang.AssertionError",
            "java.lang.Exception",
            "java.lang.RuntimeException",
            "java.lang.EnumConstantNotPresentException",
            "java.lang.TypeNotPresentException",

            "java.util.NoSuchElementException",
    });

    // We generate "legacy-style exception" shadows for these ones (and wrappers are still required).
    public static final Set<String> kLegacyExceptionClassNames = Set.of(new String[] {
            "java.lang.ExceptionInInitializerError",
            "java.lang.ClassNotFoundException",
    });

    public static final Set<String> kShadowEnumClassNames = Set.of(new String[] {
            PackageConstants.kShadowDotPrefix + "java.math.RoundingMode",
            PackageConstants.kShadowDotPrefix + "java.util.concurrent.TimeUnit",
    });

    // Record the parent class of each generated class. This information is needed by the heap size calculation.
    // Both class names are in the shadowed version.
    public static Map<String, String> parentClassMap;

    public static Map<String, byte[]> generateShadowJDK() {
        Map<String, byte[]> shadowJDK = new HashMap<>();

        Map<String, byte[]> shadowException = generateShadowException();
        //Map<String, byte[]> shadowEnum = generateShadowEnum();

        //shadowJDK.putAll(shadowEnum);
        shadowJDK.putAll(shadowException);

        return shadowJDK;
    }

    public static Map<String, byte[]> generateShadowException() {
        Map<String, byte[]> generatedClasses = new HashMap<>();
        parentClassMap = new HashMap<>();
        for (String className : kExceptionClassNames) {
            // We need to look this up to find the superclass.
            String superclassName = null;
            try {
                superclassName = Class.forName(className).getSuperclass().getName();
            } catch (ClassNotFoundException e) {
                // We are operating on built-in exception classes so, if these are missing, there is something wrong with the JDK.
                throw RuntimeAssertionError.unexpected(e);
            }
            
            // Generate the shadow.
            if (!kHandWrittenExceptionClassNames.contains(className)) {
                // Note that we are currently listing the shadow "java.lang." directly, so strip off the redundant "java.lang."
                // (this might change in the future).
                String shadowName = PackageConstants.kShadowDotPrefix + className;
                String shadowSuperName = PackageConstants.kShadowDotPrefix + superclassName;
                byte[] shadowBytes = null;
                if (kLegacyExceptionClassNames.contains(className)) {
                    // "Legacy" exception.
                    shadowBytes = generateLegacyExceptionClass(shadowName, shadowSuperName);
                } else {
                    // "Standard" exception.
                    shadowBytes = generateExceptionClass(shadowName, shadowSuperName);
                }
                
                generatedClasses.put(shadowName, shadowBytes);

                parentClassMap.put(shadowName, shadowSuperName);
            }
            
            // Generate the wrapper.
            String wrapperName = PackageConstants.kExceptionWrapperDotPrefix + PackageConstants.kShadowDotPrefix + className;
            String wrapperSuperName = PackageConstants.kExceptionWrapperDotPrefix + PackageConstants.kShadowDotPrefix + superclassName;
            byte[] wrapperBytes = generateWrapperClass(wrapperName, wrapperSuperName);
            generatedClasses.put(wrapperName, wrapperBytes);
        }
        return generatedClasses;
    }

    public static Map<String, byte[]> generateShadowEnum(boolean preserveDebuggability){
        Map<String, byte[]> generatedClasses = new HashMap<>();

        for (String name : kShadowEnumClassNames){
            byte[] cnt = Helpers.loadRequiredResourceAsBytes(name.replaceAll("\\.", "/") + ".class");

            PreRenameClassAccessRules emptyUserRuleRuleSet = new PreRenameClassAccessRules(Collections.emptySet(), Collections.emptySet());
            byte[] bytecode = new ClassToolchain.Builder(cnt, ClassReader.EXPAND_FRAMES)
                    .addNextVisitor(new UserClassMappingVisitor(new NamespaceMapper(emptyUserRuleRuleSet), preserveDebuggability))
                    .addNextVisitor(new ClassShadowing(PackageConstants.kShadowSlashPrefix))
                    .addWriter(new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS))
                    .build()
                    .runAndGetBytecode();
            bytecode = new ClassToolchain.Builder(bytecode, ClassReader.EXPAND_FRAMES)
                    .addNextVisitor(new ArrayWrappingClassAdapterRef(null, preserveDebuggability))
                    .addNextVisitor(new ArrayWrappingClassAdapter())
                    .addWriter(new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS))
                    .build()
                    .runAndGetBytecode();

            generatedClasses.put(name, bytecode);
        }

        return generatedClasses;
    }

    private static byte[] generateWrapperClass(String mappedName, String mappedSuperName) {
        String slashName = mappedName.replaceAll("\\.", "/");
        String superSlashName = mappedSuperName.replaceAll("\\.", "/");
        return StubGenerator.generateWrapperClass(slashName, superSlashName);
    }

    private static byte[] generateExceptionClass(String mappedName, String mappedSuperName) {
        String slashName = mappedName.replaceAll("\\.", "/");
        String superSlashName = mappedSuperName.replaceAll("\\.", "/");
        return StubGenerator.generateExceptionClass(slashName, superSlashName);
    }

    private static byte[] generateLegacyExceptionClass(String mappedName, String mappedSuperName) {
        String slashName = mappedName.replaceAll("\\.", "/");
        String superSlashName = mappedSuperName.replaceAll("\\.", "/");
        return StubGenerator.generateLegacyExceptionClass(slashName, superSlashName);
    }
}
