package org.aion.avm.core.types;

import org.aion.avm.core.classgeneration.CommonGenerators;
import org.aion.avm.core.classgeneration.CommonGenerators.JclException;
import org.aion.avm.internal.RuntimeAssertionError;

public class PocHierarchyAppender {
    private static final String I_OBJECT_ARRAY = "org.aion.avm.internal.IObjectArray";
    private static final String OBJECT_ARRAY = "org.aion.avm.arraywrapper.ObjectArray";
    private static final String I_ARRAY = "org.aion.avm.arraywrapper.IArray";
    private static final String ARRAY = "org.aion.avm.arraywrapper.Array";
    private static final String ARRAY_ELEMENT = "org.aion.avm.arraywrapper.ArrayElement";
    private static final String BOOLEAN_ARRAY = "org.aion.avm.arraywrapper.BooleanArray";
    private static final String BYTE_ARRAY = "org.aion.avm.arraywrapper.ByteArray";
    private static final String CHAR_ARRAY = "org.aion.avm.arraywrapper.CharArray";
    private static final String DOUBLE_ARRAY = "org.aion.avm.arraywrapper.DoubleArray";
    private static final String FLOAT_ARRAY = "org.aion.avm.arraywrapper.FloatArray";
    private static final String INT_ARRAY = "org.aion.avm.arraywrapper.IntArray";
    private static final String LONG_ARRAY = "org.aion.avm.arraywrapper.LongArray";
    private static final String SHORT_ARRAY = "org.aion.avm.arraywrapper.ShortArray";

    //TODO: add Cloneable to white list ?? probably, since now it is possible to unify to
    private static final String CLONEABLE = "org.aion.avm.shadow.java.lang.Cloneable";

    /**
     * Appends the primitive array wrapper types to the hierarchy.
     */
    public PocClassHierarchy appendPostRenamePrimitiveArrayWrapperTypes(PocClassHierarchy hierarchy) {
        RuntimeAssertionError.assertTrue(hierarchy != null);

        // We just manually construct them and add them to the hierarchy (better way to get these??)...

        hierarchy.add(PocClassInfo.postRenameInfoFor(true, false, I_ARRAY, null, new String[]{ PocClassInfo.SHADOW_IOBJECT }));
        hierarchy.add(PocClassInfo.postRenameInfoFor(true, false, I_OBJECT_ARRAY, null, new String[]{ PocClassInfo.SHADOW_IOBJECT, I_ARRAY }));
        hierarchy.add(PocClassInfo.postRenameInfoFor(true, false, CLONEABLE, PocClassInfo.SHADOW_IOBJECT, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, ARRAY, PocClassInfo.SHADOW_OBJECT, new String[]{ CLONEABLE, I_ARRAY }));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, OBJECT_ARRAY, ARRAY, new String[]{ I_OBJECT_ARRAY }));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, true, ARRAY_ELEMENT, PocClassInfo.SHADOW_ENUM , null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, BOOLEAN_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, BYTE_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, CHAR_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, DOUBLE_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, FLOAT_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, INT_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, LONG_ARRAY, ARRAY, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, SHORT_ARRAY, ARRAY, null));

        return hierarchy;
    }

    //TODO -- why are these non-shadows? Is this ok? How many of these do we need to support?

    private static final String ERROR = "java.lang.Error";
    private static final String LINKAGE_ERROR = "java.lang.LinkageError";
    private static final String INCOMPATIBLE_CHANGE_ERROR = "java.lang.IncompatibleClassChangeError";
    private static final String NO_FIELD_ERROR = "java.lang.NoSuchFieldError";

    private static final String SHADOW_ERROR = "org.aion.avm.shadow.java.lang.Error";
    private static final String SHADOW_LINKAGE_ERROR = "org.aion.avm.shadow.java.lang.LinkageError";
    private static final String SHADOW_INCOMPATIBLE_CHANGE_ERROR = "org.aion.avm.shadow.java.lang.IncompatibleClassChangeError";
    private static final String SHADOW_NO_FIELD_ERROR = "org.aion.avm.shadow.java.lang.NoSuchFieldError";

    public void appendPreRenameErrorTypes(PocClassHierarchy hierarchy) {
        RuntimeAssertionError.assertTrue(hierarchy != null);

        // These are not renamed, because we actually need this part of the hierarchy built up .... investigate why ???
        // Maybe just because of all the jcl exceptions coming in, this actually can probably be handled without this.
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, ERROR, PocClassInfo.JAVA_LANG_THROWABLE, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, LINKAGE_ERROR, ERROR, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, INCOMPATIBLE_CHANGE_ERROR, LINKAGE_ERROR, null));
        hierarchy.add(PocClassInfo.postRenameInfoFor(false, false, NO_FIELD_ERROR, INCOMPATIBLE_CHANGE_ERROR, null));
    }

    public void appendPostRenameExceptionTypes(PocClassHierarchy hierarchy) {
        RuntimeAssertionError.assertTrue(hierarchy != null);

        for (PocClassInfo exceptions : CommonGenerators.getAllShadowExceptionClassInfos()) {
            hierarchy.add(exceptions);
        }
    }

    public static boolean isJavaLangErrorType(String name) {
        return name.equals(PocClassInfo.JAVA_LANG_THROWABLE) || name.equals(ERROR) || name.equals(LINKAGE_ERROR) || name.equals(INCOMPATIBLE_CHANGE_ERROR) || name.equals(NO_FIELD_ERROR);
    }

}
