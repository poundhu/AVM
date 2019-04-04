package org.aion.avm.core.types;

import java.util.Arrays;
import org.aion.avm.internal.PackageConstants;
import org.aion.avm.internal.RuntimeAssertionError;

/**
 * This class is immutable.
 */
public final class PocClassInfo {
    public static final String JAVA_LANG_OBJECT = "java.lang.Object";
    public static final String SHADOW_IOBJECT = PackageConstants.kInternalDotPrefix + "IObject";
    public static final String SHADOW_OBJECT = PackageConstants.kShadowDotPrefix + JAVA_LANG_OBJECT;

    public static final String SHADOW_ENUM = "org.aion.avm.shadow.java.lang.Enum";
    public static final String SHADOW_COMPARABLE = "org.aion.avm.shadow.java.lang.Comparable";
    public static final String SHADOW_SERIALIZABLE = "org.aion.avm.shadow.java.io.Serializable";

    public static final String SHADOW_RUNTIME_EXCEPTION = "org.aion.avm.shadow.java.lang.RuntimeException";
    public static final String SHADOW_EXCEPTION = "org.aion.avm.shadow.java.lang.Exception";
    public static final String SHADOW_THROWABLE = "org.aion.avm.shadow.java.lang.Throwable";

    public final String parentQualifiedName;
    public final String selfQualifiedName;
    public final boolean isInterface;
    public final boolean isFinalClass;

    // The only time pre- and post- rename are not negations of one another is with java/lang/Object
    // and this is never added to a hierarchy as an input, so a single boolean suffices.
    public final boolean isPreRenameClass;

    private final String[] parentInterfaces;

    private PocClassInfo(boolean isPreRenameClass, boolean isInterface, boolean isFinalClass, String self, String parent, String[] interfaces) {
        if (self == null) {
            throw new NullPointerException("Cannot construct class info with null self.");
        }
        if (isInterface) {

            if (isFinalClass) {
                throw new IllegalArgumentException("Cannot construct class info for a final interface.");
            }

            // Note that the below rule doesn't translate to post-rename shadow IObject.
            if (isPreRenameClass) {
                if ((parent != null) && (!parent.equals(JAVA_LANG_OBJECT))) {
                    throw new IllegalArgumentException("Cannot construct pre-rename class info for an interface whose super class is not " + JAVA_LANG_OBJECT);
                }
            }

        }

        this.isPreRenameClass = isPreRenameClass;
        this.isInterface = isInterface;
        this.isFinalClass = isFinalClass;
        this.selfQualifiedName = self;
        this.parentQualifiedName = parent;
        this.parentInterfaces = interfaces;
    }

    public static PocClassInfo infoForJavaLangObject() {
        return new PocClassInfo(true, false, false, JAVA_LANG_OBJECT, null, null);
    }

    public static PocClassInfo infoForShadowIObject() {
        return new PocClassInfo(false, true, false, SHADOW_IOBJECT, JAVA_LANG_OBJECT, null);
    }

    public static PocClassInfo infoForShadowObject() {
        return new PocClassInfo(false, false, false, SHADOW_OBJECT, null, new String[]{ SHADOW_IOBJECT });
    }

    public static PocClassInfo infoForShadowEnum() {
        return new PocClassInfo(false, false, false, SHADOW_ENUM, SHADOW_OBJECT, new String[]{ SHADOW_COMPARABLE, SHADOW_SERIALIZABLE });
    }

    public static PocClassInfo infoForShadowComparable() {
        return new PocClassInfo(false, true, false, SHADOW_COMPARABLE, SHADOW_IOBJECT, null);
    }

    public static PocClassInfo infoForShadowSerializable() {
        return new PocClassInfo(false, true, false, SHADOW_SERIALIZABLE, SHADOW_IOBJECT, null);
    }

    public static PocClassInfo infoForShadowRuntimeException() {
        return new PocClassInfo(false, false, false, SHADOW_RUNTIME_EXCEPTION, SHADOW_EXCEPTION, null);
    }

    public static PocClassInfo infoForShadowException() {
        return new PocClassInfo(false, false, false, SHADOW_EXCEPTION, SHADOW_THROWABLE, null);
    }

    public static PocClassInfo infoForShadowThrowable() {
        return new PocClassInfo(false, false, false, SHADOW_THROWABLE, SHADOW_OBJECT, new String[]{ SHADOW_SERIALIZABLE });
    }

    public static PocClassInfo preRenameInfoFor(boolean isInterface, boolean isFinalClass, String self, String parent, String[] interfaces) {
        return infoFor(true, isInterface, isFinalClass, self, parent, interfaces);
    }

    public static PocClassInfo postRenameInfoFor(boolean isInterface, boolean isFinalClass, String self, String parent, String[] interfaces) {
        return infoFor(false, isInterface, isFinalClass, self, parent, interfaces);
    }

    private static PocClassInfo infoFor(boolean isPreRenameClass, boolean isInterface, boolean isFinalClass, String self, String parent, String[] interfaces) {
        if (self == null) {
            throw new NullPointerException("Cannot construct class info with null self.");
        }
        if ((parent == null) && (interfaces == null)) {
            throw new IllegalArgumentException("Cannot construct class info with no supers at all.");
        }

        // If we have a post-rename interface with java/lang/Object as a parent this is removed (it has IObject as an interface)
        if (!isPreRenameClass && isInterface && (parent != null) && (parent.equals(JAVA_LANG_OBJECT))) {
            parent = null;
        } else if (self.equals(SHADOW_OBJECT)) {
            parent = null;
        }

        return new PocClassInfo(isPreRenameClass, isInterface, isFinalClass, self, parent, interfaces);
    }

    public PocClassInfo toPostRenameClassInfo() {
        RuntimeAssertionError.assertTrue(this.isPreRenameClass);

        String renamedSelf = getSelfRenamed();
        String renamedParent = getParentRenamed();
        String[] renamedInterfaces = getInterfacesRenamed();

        return new PocClassInfo(false, this.isInterface, this.isFinalClass, renamedSelf, renamedParent, renamedInterfaces);
    }

    public String[] superClasses() {
        if (this.parentInterfaces == null) {
            return (this.parentQualifiedName == null) ? new String[0] : new String[]{ this.parentQualifiedName };
        } else {
            if (this.parentQualifiedName == null) {
                return Arrays.copyOf(this.parentInterfaces, this.parentInterfaces.length);
            } else {
                String[] parents = Arrays.copyOf(this.parentInterfaces, this.parentInterfaces.length + 1);
                parents[this.parentInterfaces.length] = this.parentQualifiedName;
                return parents;
            }
        }
    }

    public boolean isJavaLangObject() {
        return this.selfQualifiedName.equals(JAVA_LANG_OBJECT);
    }

    public boolean isShadowIObject() {
        return this.selfQualifiedName.equals(SHADOW_IOBJECT);
    }

    public boolean isShadowObject() {
        return this.selfQualifiedName.equals(SHADOW_OBJECT);
    }

    public String[] getInterfaces() {
        return Arrays.copyOf(this.parentInterfaces, this.parentInterfaces.length);
    }

    @Override
    public String toString() {
        return "PocClassInfo { " + rawString() + " }";
    }

    public String rawString() {
        int numParents = this.parentQualifiedName == null ? 0 : 1;
        numParents += this.parentInterfaces == null ? 0 : this.parentInterfaces.length;

        return "name = '" + this.selfQualifiedName
            + ((this.isInterface) ? "', [interface" : "', [class") + ", "
            + ((this.isFinalClass) ? "final], " : "non-final], ")
            + "parents = " + numParents;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PocClassInfo)) {
            return false;
        }

        PocClassInfo otherInfo = (PocClassInfo) other;

        return ((this.isInterface == otherInfo.isInterface)
            && (this.isFinalClass == otherInfo.isFinalClass)
            && (this.selfQualifiedName.equals(otherInfo.selfQualifiedName))
            && (hasSameParentAs(otherInfo))
            && (hasSameInterfacesAs(otherInfo)));
    }

    @Override
    public int hashCode() {
        int hash = 37;
        hash += this.isInterface ? 1 : 0;
        hash += this.isFinalClass ? 1 : 0;
        hash += this.selfQualifiedName.hashCode();
        hash += this.parentQualifiedName == null ? 0 : this.parentQualifiedName.hashCode();
        return (this.parentInterfaces == null) ? hash : hash + Arrays.hashCode(this.parentInterfaces);
    }

    private boolean hasSameParentAs(PocClassInfo other) {
        if (this.parentQualifiedName == null) {
            return other.parentQualifiedName == null;
        } else {
            return this.parentQualifiedName.equals(other.parentQualifiedName);
        }
    }

    private boolean hasSameInterfacesAs(PocClassInfo other) {
        if (this.parentInterfaces == null) {
            return other.parentInterfaces == null;
        } else {
            return Arrays.equals(this.parentInterfaces, other.parentInterfaces);
        }
    }

    /**
     * Returns true only if name is {@value JAVA_LANG_OBJECT}, {@value SHADOW_IOBJECT} or
     * {@value SHADOW_OBJECT}
     */
    private static boolean isObjectType(String name) {
        return (name != null) && (name.equals(JAVA_LANG_OBJECT) || name.equals(SHADOW_IOBJECT) || name.equals(SHADOW_OBJECT));
    }

    // Borrowing the logic from ParentPointers for now, since currently we just want to be able to
    // answer everything it can (no focus on answering more, yet)...
    private String getSelfRenamed() {
        return rename(this.selfQualifiedName);
    }

    private String getParentRenamed() {
        if (this.parentQualifiedName == null) {
            return null;
        }

        RuntimeAssertionError.assertTrue(!this.parentQualifiedName.equals(SHADOW_IOBJECT));
        RuntimeAssertionError.assertTrue(!this.parentQualifiedName.equals(SHADOW_OBJECT));

        // If our parent is java/lang/Object then re-parent ourselves under shadow IObject for interfaces or else shadow Object
        if (this.parentQualifiedName.equals(JAVA_LANG_OBJECT)) {
            return (this.isInterface) ? SHADOW_IOBJECT : SHADOW_OBJECT;
        } else {
            return rename(this.parentQualifiedName);
        }
    }

    private String[] getInterfacesRenamed() {
        if (this.parentInterfaces == null) {
            return null;
        }

        String[] renamedInterfaces = new String[this.parentInterfaces.length];

        for (int i = 0; i < this.parentInterfaces.length; i++) {

            RuntimeAssertionError.assertTrue(!this.parentInterfaces[i].equals(SHADOW_IOBJECT));
            RuntimeAssertionError.assertTrue(!this.parentInterfaces[i].equals(SHADOW_OBJECT));

            if (this.parentInterfaces[i].equals(JAVA_LANG_OBJECT)) {
                 renamedInterfaces[i] = (this.isInterface) ? SHADOW_IOBJECT : SHADOW_OBJECT;
            } else {
                renamedInterfaces[i] = rename(this.parentInterfaces[i]);
            }

        }

        return renamedInterfaces;
    }

    private String rename(String name) {
        if (name == null) {
            return null;
        } else if (name.equals(JAVA_LANG_OBJECT)) {
            // Java/lang/Object is the only object that does not undergo re-naming.
            return JAVA_LANG_OBJECT;
        } else if (name.startsWith("java.lang.")) {
            return PackageConstants.kShadowDotPrefix + name;
        } else if (name.startsWith(PackageConstants.kPublicApiDotPrefix)) {
            return PackageConstants.kShadowApiDotPrefix + name;
        } else {
            return PackageConstants.kUserDotPrefix + name;
        }
    }

}
