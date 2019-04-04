package org.aion.avm.core;

import java.util.Stack;

import org.aion.avm.ArrayClassNameMapper;
import org.aion.avm.ClassNameExtractor;
import org.aion.avm.core.classgeneration.CommonGenerators;
import org.aion.avm.core.exceptionwrapping.ExceptionWrapperNameMapper;
import org.aion.avm.core.types.PocClassHierarchy;
import org.aion.avm.core.types.PocClassInfo;
import org.aion.avm.core.types.PocHierarchyAppender;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.internal.PackageConstants;
import org.aion.avm.internal.RuntimeAssertionError;
import org.objectweb.asm.ClassWriter;


/**
 * We extend the ClassWriter to override their implementation of getCommonSuperClass() with an implementation which knows how
 * to compute this relationship between our generated classes, before they can be loaded.
 */
public class TypeAwareClassWriter extends ClassWriter {
    private static final String IOBJECT_SLASH_NAME = PackageConstants.kInternalSlashPrefix + "IObject";
    // Note that we handle the wrapper of shadow "java.lang.Throwable" as a special-case, since that one is our manually-implemented root.
    private static final Class<?> WRAPPER_ROOT_THROWABLE = org.aion.avm.exceptionwrapper.org.aion.avm.shadow.java.lang.Throwable.class;

    private final IParentPointers staticClassHierarchy;


    // Keep around both hierarchies as we transition between the two...
    private final PocClassHierarchy pocHierarchy;
    private final boolean preserveDebuggability;

    public TypeAwareClassWriter(int flags, IParentPointers parentClassResolver) {
        super(flags);
        this.staticClassHierarchy = parentClassResolver;

        this.pocHierarchy = null;
        this.preserveDebuggability = false; // we don't care about this value here.
    }

    public TypeAwareClassWriter(int flags, PocClassHierarchy hierarchy, boolean preserveDebuggability) {
        super(flags);
        this.pocHierarchy = hierarchy;

        this.staticClassHierarchy = null;
        this.preserveDebuggability = preserveDebuggability;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        return (this.pocHierarchy == null)
            ? getCommonSuperClassViaOldPointers(type1, type2)
            : getCommonSuperClassViaNewClassHierarchy(type1, type2);
    }

    private String getCommonSuperClassViaOldPointers(String type1, String type2) {
        // NOTE:  The types we are receiving and returning here use slash-style names.
        String commonRoot = null;

        // TODO (issue-176): Generalize this interface handling instead of just using this IObject special-case.
        if (IOBJECT_SLASH_NAME.equals(type1) || IOBJECT_SLASH_NAME.equals(type2)) {
            commonRoot = IOBJECT_SLASH_NAME;
        } else {
            // We will use a relatively simple approach, here:  build a stack for each type (root on top), then find the common prefix by popping.
            Stack<String> stack1 = builTypeListFor(type1);
            Stack<String> stack2 = builTypeListFor(type2);

            while (!stack1.isEmpty() && !stack2.isEmpty() && stack1.peek().equals(stack2.peek())) {
                commonRoot = stack1.pop();
                stack2.pop();
            }
        }
        return commonRoot;
    }

    private String getCommonSuperClassViaNewClassHierarchy(String type1, String type2) {
        // Breaking these out into two separate methods so things stay comprehensible.
        return (this.preserveDebuggability)
            ? getCommonSuperWhenDebugPreservedTest(type1, type2)
            : getCommonSuperWhenNotPreservedTest(type1, type2);
    }

    /**
     * Returns the common super class of the two types only if one of them is a root type, since we
     * know all the root types and can give an answer immediately.
     *
     * The root types are: java/lang/Object, java/lang/Throwable, IObject, shadow Object
     *
     * Returns NULL if neither type is a root type and so we can not quickly determine the super.
     */
    private String findCommonSuperIfOneTypeIsRootType(String type1dotName, String type2dotName) {
        if (type1dotName.equals(PocClassInfo.JAVA_LANG_OBJECT) || type2dotName.equals(PocClassInfo.JAVA_LANG_OBJECT)) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
        }

        if (type1dotName.equals(PocClassInfo.JAVA_LANG_THROWABLE) || type2dotName.equals(PocClassInfo.JAVA_LANG_THROWABLE)) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
        }

        if (type1dotName.equals(PocClassInfo.SHADOW_IOBJECT) || type2dotName.equals(PocClassInfo.SHADOW_IOBJECT)) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT);
        }

        if (type1dotName.equals(PocClassInfo.SHADOW_OBJECT) || type2dotName.equals(PocClassInfo.SHADOW_OBJECT)) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_OBJECT);
        }

        return null;
    }

    /**
     * Returns the only possible super class (java/lang/Object) if one of the two types is pre-rename
     * and the other is post-rename.
     *
     * Returns NULL if the two types are both pre-rename or else they are both post-rename, since
     * further investigation is required.
     */
    private String findCommonSuperIfOneTypeIsPreRenameAndOtherPostRenameDebugNotPreserved(String type1dotName, String type2dotName) {
        boolean type1isPreRename = !ClassNameExtractor.isPostRenameClass(type1dotName);
        boolean type2isPreRename = !ClassNameExtractor.isPostRenameClass(type2dotName);

        return (type1isPreRename == type2isPreRename)
            ? null
            : Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
    }

    private String findCommonSuperIfOneTypeIsPreRenameAndOtherPostRenameDebugPreserved(String type1dotName, String type2dotName) {
        // We do not count a user-defined class as pre-rename since it is in the hierarchy and may have post-rename ancestors.
        boolean type1isPreRename = !this.pocHierarchy.isUserDefinedClass(type1dotName) && !ClassNameExtractor.isPostRenameClass(type1dotName);
        boolean type2isPreRename = !this.pocHierarchy.isUserDefinedClass(type2dotName) && !ClassNameExtractor.isPostRenameClass(type2dotName);

        return (type1isPreRename == type2isPreRename)
            ? null
            : Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
    }

    /**
     * Returns the super class if one of the two types is an exception wrapper. This is only ever
     * java/lang/Throwable or java/lang/Object, but some inspection is required to determine which
     * one.
     *
     * Returns NULL if the neither type is an exception wrapper.
     */
    private String findCommonSuperIfOneTypeIsExceptionWrapperDebugNotPreserved(String type1dotName, String type2dotName) {
        boolean type1isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type1dotName);
        boolean type2isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type2dotName);

        // If both types are exception wrappers we can safely return java/lang/Throwable because this
        // wrapper is always unwrapped and the real type discovered then.
        if (type1isExceptionWrapper && type2isExceptionWrapper) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
        }

        // If one type is an exception wrapper then we essentially want java/lang/Object or java/lang/Throwable
        // But we have to query the hierarchy to actually determine which of these we want.
        if (type1isExceptionWrapper || type2isExceptionWrapper) {

            // If the other type is pre-rename then we have to unify to java/lang/Object or java/lang/Throwable.
            // We actually have to query the hierarchy to determine this result.
            boolean type1isPreRename = !ClassNameExtractor.isPostRenameClass(type1dotName);
            boolean type2isPreRename = !ClassNameExtractor.isPostRenameClass(type2dotName);

            if (type1isPreRename || type2isPreRename) {

                // First, unwrap the exception wrapper.
                String type1queryName = (type1isExceptionWrapper) ? ExceptionWrapperNameMapper.dotClassNameForWrapperName(type1dotName) : type1dotName;
                String type2queryName = (type2isExceptionWrapper) ? ExceptionWrapperNameMapper.dotClassNameForWrapperName(type2dotName) : type2dotName;

                // Second, rename the pre-renamed class.
                type1queryName = (type1isPreRename) ? PocClassInfo.rename(type1queryName) : type1queryName;
                type2queryName = (type2isPreRename) ? PocClassInfo.rename(type2queryName) : type2queryName;

                // Third, query the hierarchy.
                String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1queryName, type2queryName);

                // If we hit an Object type then we have to unify to java/lang/Object since the other type is not an exception.
                if (commonSuper.equals(PocClassInfo.JAVA_LANG_OBJECT) || commonSuper.equals(PocClassInfo.SHADOW_IOBJECT) || commonSuper.equals(PocClassInfo.SHADOW_OBJECT)) {
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
                }

                // Otherwise, the other type is an exception type so we can unify to java/lang/Throwable.
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            }

            // Then the other type is post-rename and we can only unify to java/lang/Object since the
            // wrapper descends from java/lang/Throwable, which is unreachable from the post-rename classes.
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
        }

        // Then neither type must be an exception wrapper, so we don't handle this here.
        return null;
    }

    private String findCommonSuperIfOneTypeIsExceptionWrapperDebugPreserved(String type1dotName, String type2dotName) {
        boolean type1isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type1dotName);
        boolean type2isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type2dotName);

        // If both types are exception wrappers we can safely return java/lang/Throwable because this
        // wrapper is always unwrapped and the real type discovered then.
        if (type1isExceptionWrapper && type2isExceptionWrapper) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
        }

        // If one type is an exception wrapper then we essentially want java/lang/Object or java/lang/Throwable
        // But we have to query the hierarchy to actually determine which of these we want.
        if (type1isExceptionWrapper || type2isExceptionWrapper) {

            // If the other type is pre-rename then we have to unify to java/lang/Object or java/lang/Throwable.
            // We actually have to query the hierarchy to determine this result.
            // We do not want a user-defined class to get renamed since it is in the hierarchy.
            boolean type1isPreRename = !this.pocHierarchy.isUserDefinedClass(type1dotName) && !ClassNameExtractor.isPostRenameClass(type1dotName);
            boolean type2isPreRename = !this.pocHierarchy.isUserDefinedClass(type2dotName) && !ClassNameExtractor.isPostRenameClass(type2dotName);

            if (type1isPreRename || type2isPreRename) {

                // First, unwrap the exception wrapper.
                String type1queryName = (type1isExceptionWrapper) ? ExceptionWrapperNameMapper.dotClassNameForWrapperName(type1dotName) : type1dotName;
                String type2queryName = (type2isExceptionWrapper) ? ExceptionWrapperNameMapper.dotClassNameForWrapperName(type2dotName) : type2dotName;

                // Second, rename the pre-renamed class.
                type1queryName = (type1isPreRename) ? PocClassInfo.rename(type1queryName) : type1queryName;
                type2queryName = (type2isPreRename) ? PocClassInfo.rename(type2queryName) : type2queryName;

                // Third, query the hierarchy.
                String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1queryName, type2queryName);

                // If we hit an Object type then we have to unify to java/lang/Object since the other type is not an exception.
                if (commonSuper.equals(PocClassInfo.JAVA_LANG_OBJECT) || commonSuper.equals(PocClassInfo.SHADOW_IOBJECT) || commonSuper.equals(PocClassInfo.SHADOW_OBJECT)) {
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
                }

                // Otherwise, the other type is an exception type so we can unify to java/lang/Throwable.
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            }

            // Then the other type is post-rename and we can only unify to java/lang/Object since the
            // wrapper descends from java/lang/Throwable, which is unreachable from the post-rename classes.
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
        }

        // Then neither type must be an exception wrapper, so we don't handle this here.
        return null;
    }

    /**
     * Returns the super class if one of the two types is an array wrapper. Note that an array wrapper
     * accounts for all array types EXCEPT one-dimensional primitive arrays.
     *
     * These are handled specially as regular objects, and since they are in the hierarchy, we can
     * unify other types against them simply by calling into the hierarchy - they require no special
     * handling, like array wrappers do. That is why they are not included here.
     *
     * Returns NULL if neither type is an array wrapper.
     */
    private String findCommonSuperIfOneTypeIsArrayWrapperDebugNotPreserved(String type1dotName, String type2dotName) {
        boolean type1isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type1dotName);
        boolean type2isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type2dotName);

        boolean type1isPreRenameJclException = !type1isExceptionWrapper && CommonGenerators.isJclExceptionType(type1dotName);

        boolean type1isPreRenameNonJclExceptionOrWrapper = !type1isExceptionWrapper && !type1isPreRenameJclException && !ClassNameExtractor.isPostRenameClass(type1dotName);

        // Since we know both are either pre- or post-rename.
        boolean bothTypesArePreRename = type1isPreRenameJclException || type1isPreRenameNonJclExceptionOrWrapper;

        boolean type1isMultiDimPrimitiveArray = !type1isExceptionWrapper && !bothTypesArePreRename && ArrayClassNameMapper.isMultiDimensionalPrimitiveArrayDotName(type1dotName);
        boolean type2isMultiDimPrimitiveArray = !type2isExceptionWrapper && !bothTypesArePreRename && ArrayClassNameMapper.isMultiDimensionalPrimitiveArrayDotName(type2dotName);

        boolean type1isObjectArray = !type1isExceptionWrapper && !bothTypesArePreRename && !type1isMultiDimPrimitiveArray && ArrayClassNameMapper.isObjectArrayWrapperDotName(type1dotName);
        boolean type2isObjectArray = !type2isExceptionWrapper && !bothTypesArePreRename && !type2isMultiDimPrimitiveArray && ArrayClassNameMapper.isObjectArrayWrapperDotName(type2dotName);

        boolean type1isArray = type1isMultiDimPrimitiveArray || type1isObjectArray;
        boolean type2isArray = type2isMultiDimPrimitiveArray || type2isObjectArray;

        // Handle the case where both types are object arrays.
        if (type1isObjectArray && type2isObjectArray) {

            boolean type1isInterfaceObjectArray = ArrayClassNameMapper.isInterfaceObjectArrayDotName(type1dotName);
            boolean type2isInterfaceObjectArray = ArrayClassNameMapper.isInterfaceObjectArrayDotName(type2dotName);

            // If one type is an interface object array and the other is not, then IObjectArray is their unifying type.
            if (type1isInterfaceObjectArray != type2isInterfaceObjectArray) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // If the two arrays differ in dimension, then we unify to IObjectArray.
            int array1dimension = ArrayClassNameMapper.getObjectArrayDotNameDimension(type1dotName);

            if (array1dimension != ArrayClassNameMapper.getObjectArrayDotNameDimension(type2dotName)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // Otherwise, we strip the arrays down to their base types and get the common super class of the base types
            // and then wrap them back up in the array wrappers and return.
            String type1stripped = ArrayClassNameMapper.stripObjectArrayWrapperDotNameToBaseType(type1dotName);
            String type2stripped = ArrayClassNameMapper.stripObjectArrayWrapperDotNameToBaseType(type2dotName);

            // We make a recursive call back into getCommonSuperClassViaNewClassHierarchy() -- this call can never
            // recurse any deeper than this, and it solves all our problems for us.
            String type1strippedSlashName = Helpers.fulllyQualifiedNameToInternalName(type1stripped);
            String type2strippedSlashName = Helpers.fulllyQualifiedNameToInternalName(type2stripped);

            String strippedCommonSuper = Helpers.internalNameToFulllyQualifiedName(getCommonSuperClassViaNewClassHierarchy(type1strippedSlashName, type2strippedSlashName));

            // If we hit the 'top' of the hierarchy (The Object types) then we just go to IObjectArray
            if (strippedCommonSuper.equals(PocClassInfo.JAVA_LANG_OBJECT) || strippedCommonSuper.equals(PocClassInfo.SHADOW_IOBJECT) || strippedCommonSuper.equals(PocClassInfo.SHADOW_OBJECT)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // Finally, we reconstruct our answer as an array wrapper and return. We only ask if type1 is
            // an interface object array because we know type2 has the same answer.
            return type1isInterfaceObjectArray
                ? Helpers.fulllyQualifiedNameToInternalName(ArrayClassNameMapper.wrapTypeDotNameAsInterfaceObjectArray(array1dimension, strippedCommonSuper))
                : Helpers.fulllyQualifiedNameToInternalName(ArrayClassNameMapper.wrapTypeDotNameAsNonInterfaceObjectArray(array1dimension, strippedCommonSuper));
        }

        boolean atLeastOneMultiDimPrimitiveArray = type1isMultiDimPrimitiveArray || type2isMultiDimPrimitiveArray;
        boolean atLeastOneObjectArray = type1isObjectArray || type2isObjectArray;

        // Handle the case where one of our types is a multi-dimensional primitive array
        if (atLeastOneMultiDimPrimitiveArray || atLeastOneObjectArray) {

            // Then we have a primitive and object array unifying, this must be IObjectArray.
            if (type1isArray && type2isArray) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // Otherwise, since we have a non-array unifying with an array wrapper we return java/lang/Object.
            //TODO: we could be more precise and distinguish pre-post rename to determine if shadow Object/IObject are more appropriate. Is it worth it??
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
        }

        // Neither type is an array wrapper then.
        return null;
    }

    private String findCommonSuperIfOneTypeIsArrayWrapperDebugPreserved(String type1dotName, String type2dotName) {
        boolean type1isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type1dotName);
        boolean type2isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type2dotName);

        boolean type1isPreRenameJclException = !type1isExceptionWrapper && CommonGenerators.isJclExceptionType(type1dotName);

        // user-defined classes don't count as pre-rename.
        boolean type1isPreRenameNonJclExceptionOrWrapper = !type1isExceptionWrapper && !type1isPreRenameJclException && !this.pocHierarchy.isUserDefinedClass(type1dotName) && !ClassNameExtractor.isPostRenameClass(type1dotName);

        // Since we know both are either pre- or post-rename.
        boolean bothTypesArePreRename = type1isPreRenameJclException || type1isPreRenameNonJclExceptionOrWrapper;

        boolean type1isMultiDimPrimitiveArray = !type1isExceptionWrapper && !bothTypesArePreRename && ArrayClassNameMapper.isMultiDimensionalPrimitiveArrayDotName(type1dotName);
        boolean type2isMultiDimPrimitiveArray = !type2isExceptionWrapper && !bothTypesArePreRename && ArrayClassNameMapper.isMultiDimensionalPrimitiveArrayDotName(type2dotName);

        boolean type1isObjectArray = !type1isExceptionWrapper && !bothTypesArePreRename && !type1isMultiDimPrimitiveArray && ArrayClassNameMapper.isObjectArrayWrapperDotName(type1dotName);
        boolean type2isObjectArray = !type2isExceptionWrapper && !bothTypesArePreRename && !type2isMultiDimPrimitiveArray && ArrayClassNameMapper.isObjectArrayWrapperDotName(type2dotName);

        boolean type1isArray = type1isMultiDimPrimitiveArray || type1isObjectArray;
        boolean type2isArray = type2isMultiDimPrimitiveArray || type2isObjectArray;

        // Handle the case where both types are object arrays.
        if (type1isObjectArray && type2isObjectArray) {

            boolean type1isInterfaceObjectArray = ArrayClassNameMapper.isInterfaceObjectArrayDotName(type1dotName);
            boolean type2isInterfaceObjectArray = ArrayClassNameMapper.isInterfaceObjectArrayDotName(type2dotName);

            // If one type is an interface object array and the other is not, then IObjectArray is their unifying type.
            if (type1isInterfaceObjectArray != type2isInterfaceObjectArray) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // If the two arrays differ in dimension, then we unify to IObjectArray.
            int array1dimension = ArrayClassNameMapper.getObjectArrayDotNameDimension(type1dotName);

            if (array1dimension != ArrayClassNameMapper.getObjectArrayDotNameDimension(type2dotName)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // Otherwise, we strip the arrays down to their base types and get the common super class of the base types
            // and then wrap them back up in the array wrappers and return.
            String type1stripped = ArrayClassNameMapper.stripObjectArrayWrapperDotNameToBaseType(type1dotName);
            String type2stripped = ArrayClassNameMapper.stripObjectArrayWrapperDotNameToBaseType(type2dotName);

            // We make a recursive call back into getCommonSuperClassViaNewClassHierarchy() -- this call can never
            // recurse any deeper than this, and it solves all our problems for us.
            String type1strippedSlashName = Helpers.fulllyQualifiedNameToInternalName(type1stripped);
            String type2strippedSlashName = Helpers.fulllyQualifiedNameToInternalName(type2stripped);

            String strippedCommonSuper = Helpers.internalNameToFulllyQualifiedName(getCommonSuperClassViaNewClassHierarchy(type1strippedSlashName, type2strippedSlashName));

            // If we hit the 'top' of the hierarchy (The Object types) then we just go to IObjectArray
            if (strippedCommonSuper.equals(PocClassInfo.JAVA_LANG_OBJECT) || strippedCommonSuper.equals(PocClassInfo.SHADOW_IOBJECT) || strippedCommonSuper.equals(PocClassInfo.SHADOW_OBJECT)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // Finally, we reconstruct our answer as an array wrapper and return. We only ask if type1 is
            // an interface object array because we know type2 has the same answer.
            return type1isInterfaceObjectArray
                ? Helpers.fulllyQualifiedNameToInternalName(ArrayClassNameMapper.wrapTypeDotNameAsInterfaceObjectArray(array1dimension, strippedCommonSuper))
                : Helpers.fulllyQualifiedNameToInternalName(ArrayClassNameMapper.wrapTypeDotNameAsNonInterfaceObjectArray(array1dimension, strippedCommonSuper));
        }

        boolean atLeastOneMultiDimPrimitiveArray = type1isMultiDimPrimitiveArray || type2isMultiDimPrimitiveArray;
        boolean atLeastOneObjectArray = type1isObjectArray || type2isObjectArray;

        // Handle the case where one of our types is a multi-dimensional primitive array
        if (atLeastOneMultiDimPrimitiveArray || atLeastOneObjectArray) {

            // Then we have a primitive and object array unifying, this must be IObjectArray.
            if (type1isArray && type2isArray) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
            }

            // Otherwise, since we have a non-array unifying with an array wrapper we return java/lang/Object.
            //TODO: we could be more precise and distinguish pre-post rename to determine if shadow Object/IObject are more appropriate. Is it worth it??
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
        }

        // Neither type is an array wrapper then.
        return null;
    }

    /**
     * This is the more terse representation I came up with as a simpler alternative. Testing it out.
     */
    private String getCommonSuperWhenNotPreservedTest(String type1, String type2) {
        // If the two types are the same, return either one.
        if (type1.equals(type2)) {
            return type1;
        }

        //TODO: just deal in terms of internal names in the hierarchy?
        String type1dotName = Helpers.internalNameToFulllyQualifiedName(type1);
        String type2dotName = Helpers.internalNameToFulllyQualifiedName(type2);

        // If we can immediately determine the super class becuase one of these is a root type, do it.
        String rootTypeSuper = findCommonSuperIfOneTypeIsRootType(type1dotName, type2dotName);
        if (rootTypeSuper != null) {
            return rootTypeSuper;
        }

        // If one type is an exception wrapper we determine the class now. We do this before the pre-post name
        // mixup check because exception wrappers have two possible unifications that require extra work
        // if the other type is a pre-rename type.
        String exceptionWrapperSuper = findCommonSuperIfOneTypeIsExceptionWrapperDebugNotPreserved(type1dotName, type2dotName);
        if (exceptionWrapperSuper != null) {
            return exceptionWrapperSuper;
        }

        // If one type is pre-rename and the other post-rename then the super class is java/lang/Object.
        String mixedNameSuper = findCommonSuperIfOneTypeIsPreRenameAndOtherPostRenameDebugNotPreserved(type1dotName, type2dotName);
        if (mixedNameSuper != null) {
            return mixedNameSuper;
        }

        // If one type is an array wrapper then we do the special-case wrapper handling here and return.
        String arrayWrapperSuper = findCommonSuperIfOneTypeIsArrayWrapperDebugNotPreserved(type1dotName, type2dotName);
        if (arrayWrapperSuper != null) {
            return arrayWrapperSuper;
        }

        // Finally, if we made it this far then we have no special-case handling remaining. In particular,
        // exception & array wrappers have been handled (and a few other easy-to-answer cases).
        // The only remaining special-casing left is renaming if we are dealing with pre-rename classes.
        // But otherwise, we expect all remaining types (once renamed if needed) are in the hierarchy and we
        // can resolve their super class by querying the hierarchy.


        // Since we know both are either pre- or post-rename we only need to query one.
        boolean bothTypesArePreRename = !ClassNameExtractor.isPostRenameClass(type1dotName);

        // Finally, if we have any pre-rename types, we rename them so that we can query the hierarchy.
        type1dotName = (bothTypesArePreRename) ? PocClassInfo.rename(type1dotName) : type1dotName;
        type2dotName = (bothTypesArePreRename) ? PocClassInfo.rename(type2dotName) : type2dotName;

        // Grab the super class.
        String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);

        // If we had any pre-rename types involved then we always un-name them when returning.
        if (bothTypesArePreRename) {

            // If the common super was IObject, this becomes Object because we are 'un-naming' now.
            if (commonSuper.equals(PocClassInfo.SHADOW_IOBJECT)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
            }

            // Otherwise, we simply undo the renaming we did and return the super class.
            String unnamedCommonSuper = ClassNameExtractor.getOriginalClassName(commonSuper);
            return Helpers.fulllyQualifiedNameToInternalName(unnamedCommonSuper);
        }

        // If we make it here, no pre-rename types were involved and we are done.
        return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
    }

    private String getCommonSuperWhenDebugPreservedTest(String type1, String type2) {
        // If the two types are the same, return either one.
        if (type1.equals(type2)) {
            return type1;
        }

        //TODO: just deal in terms of internal names in the hierarchy?
        String type1dotName = Helpers.internalNameToFulllyQualifiedName(type1);
        String type2dotName = Helpers.internalNameToFulllyQualifiedName(type2);

        // If we can immediately determine the super class becuase one of these is a root type, do it.
        String rootTypeSuper = findCommonSuperIfOneTypeIsRootType(type1dotName, type2dotName);
        if (rootTypeSuper != null) {
            return rootTypeSuper;
        }

        // If one type is an exception wrapper we determine the class now. We do this before the pre-post name
        // mixup check because exception wrappers have two possible unifications that require extra work
        // if the other type is a pre-rename type.
        String exceptionWrapperSuper = findCommonSuperIfOneTypeIsExceptionWrapperDebugPreserved(type1dotName, type2dotName);
        if (exceptionWrapperSuper != null) {
            return exceptionWrapperSuper;
        }

        // If one type is pre-rename and the other post-rename then the super class is java/lang/Object.
        String mixedNameSuper = findCommonSuperIfOneTypeIsPreRenameAndOtherPostRenameDebugPreserved(type1dotName, type2dotName);
        if (mixedNameSuper != null) {
            return mixedNameSuper;
        }

        // If one type is an array wrapper then we do the special-case wrapper handling here and return.
        String arrayWrapperSuper = findCommonSuperIfOneTypeIsArrayWrapperDebugPreserved(type1dotName, type2dotName);
        if (arrayWrapperSuper != null) {
            return arrayWrapperSuper;
        }

        // Finally, if we made it this far then we have no special-case handling remaining. In particular,
        // exception & array wrappers have been handled (and a few other easy-to-answer cases).
        // The only remaining special-casing left is renaming if we are dealing with pre-rename classes.
        // But otherwise, we expect all remaining types (once renamed if needed) are in the hierarchy and we
        // can resolve their super class by querying the hierarchy.


        // Determine which classes to rename
        boolean type1isUserDefinedType = this.pocHierarchy.isUserDefinedClass(type1dotName);
        boolean type2isUserDefinedType = this.pocHierarchy.isUserDefinedClass(type2dotName);

        boolean type1isPreRename = !type1isUserDefinedType && !ClassNameExtractor.isPostRenameClass(type1dotName);
        boolean type2isPreRename = !type2isUserDefinedType && !ClassNameExtractor.isPostRenameClass(type2dotName);

        // Finally, if we have any pre-rename types, we rename them so that we can query the hierarchy.
        type1dotName = (type1isPreRename) ? PocClassInfo.rename(type1dotName) : type1dotName;
        type2dotName = (type2isPreRename) ? PocClassInfo.rename(type2dotName) : type2dotName;

        // Grab the super class.
        String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);

        // If both types are user-defined then we can return whatever the hierarchy found as the super class.
        if (type1isUserDefinedType && type2isUserDefinedType) {
            return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
        }

        // If one is a user-defined type and the other is pre-rename, then if our common super is post-rename
        // we have to default to java/lang/Object as the only common super, otherwise, if it is pre-rename
        // we are safe to return it.
        if (type1isUserDefinedType && type2isPreRename) {

            if (ClassNameExtractor.isPostRenameClass(commonSuper)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
            }

            return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
        }

        // Same case as above, but reversed.
        if (type2isUserDefinedType && type1isPreRename) {

            if (ClassNameExtractor.isPostRenameClass(commonSuper)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
            }

            return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
        }

        // If we had any pre-rename types involved then we always un-name them when returning.
        if (type1isPreRename && type2isPreRename) {

            // If the common super was IObject, this becomes Object because we are 'un-naming' now.
            if (commonSuper.equals(PocClassInfo.SHADOW_IOBJECT)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
            }

            // Otherwise, we simply undo the renaming we did and return the super class.
            String unnamedCommonSuper = ClassNameExtractor.getOriginalClassName(commonSuper);
            return Helpers.fulllyQualifiedNameToInternalName(unnamedCommonSuper);
        }

        // If we make it here, no pre-rename types were involved and we are done.
        return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
    }


    // ***************** The old implementation below, which I know is correct, incase I need to revert back.

//    private String getCommonSuperWhenDebugPreservedTest(String type1, String type2) {
//        //TODO: we will simply need finer grained control here ... we need to know if we have a user-defined class, because then pre-rename has a new meaning.
//
//        // If the two types are the same, return either one.
//        if (type1.equals(type2)) {
//            return type1;
//        }
//
//        String type1dotName = Helpers.internalNameToFulllyQualifiedName(type1);
//        String type2dotName = Helpers.internalNameToFulllyQualifiedName(type2);
//
//        // If either type is java/lang/Object then our only possible answer is java/lang/Object
//        if (type1dotName.equals(PocClassInfo.JAVA_LANG_OBJECT) || type2dotName.equals(PocClassInfo.JAVA_LANG_OBJECT)) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
//        }
//
//        // If either type is java/lang/Throwable then our only answer is java/lang/Throwable
//        if (type1dotName.equals(PocClassInfo.JAVA_LANG_THROWABLE) || type2dotName.equals(PocClassInfo.JAVA_LANG_THROWABLE)) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
//        }
//
//        // If either type is IObject then we can return IObject
//        if (type1dotName.equals(PocClassInfo.SHADOW_IOBJECT) || type2dotName.equals(PocClassInfo.SHADOW_IOBJECT)) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT);
//        }
//
//        // Finally, if either type is shadow Object, we can return shadow Object
//        if (type1dotName.equals(PocClassInfo.SHADOW_OBJECT) || type2dotName.equals(PocClassInfo.SHADOW_OBJECT)) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_OBJECT);
//        }
//
//        // Requires slash name
//        boolean type1isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type1dotName);
//        boolean type2isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapperDotName(type2dotName);
//        boolean atLeastOneExceptionWrapper = type1isExceptionWrapper || type2isExceptionWrapper;
//
//        // Requires dot name
//        boolean type1isPreRenameJclException = !type1isExceptionWrapper && CommonGenerators.isJclExceptionType(type1dotName);
//        boolean type2isPreRenameJclException = !type2isExceptionWrapper && CommonGenerators.isJclExceptionType(type2dotName);
//        boolean atLeastOnePreRenameJclException = type1isPreRenameJclException || type2isPreRenameJclException;
//
//        // Requires dot name
//        boolean type1isPreRenameNonException = !type1isExceptionWrapper && !type1isPreRenameJclException && !ClassNameExtractor.isPostRenameClass(type1dotName);
//        boolean type2isPreRenameNonException = !type2isExceptionWrapper && !type2isPreRenameJclException && !ClassNameExtractor.isPostRenameClass(type2dotName);
//        boolean atLeastOnePreRenameNonException = type1isPreRenameNonException || type2isPreRenameNonException;
//
//        boolean type1isPreRename = type1isPreRenameJclException || type1isPreRenameNonException;
//        boolean type2isPreRename = type2isPreRenameJclException || type2isPreRenameNonException;
//        boolean atLeastOneIsPreRename = type1isPreRename || type2isPreRename;
//
//        boolean type1isMultiDimPrimitiveArray = !type1isExceptionWrapper && !type1isPreRename && ArrayClassNameMapper.isMultiDimensionalPrimitiveArrayDotName(type1dotName);
//        boolean type2isMultiDimPrimitiveArray = !type2isExceptionWrapper && !type2isPreRename && ArrayClassNameMapper.isMultiDimensionalPrimitiveArrayDotName(type2dotName);
//        boolean atLeastOneMultiDimPrimitiveArray = type1isMultiDimPrimitiveArray || type2isMultiDimPrimitiveArray;
//
//        boolean type1isObjectArray = !type1isExceptionWrapper && !type1isPreRename && !type1isMultiDimPrimitiveArray && ArrayClassNameMapper.isObjectArrayWrapperDotName(type1dotName);
//        boolean type2isObjectArray = !type2isExceptionWrapper && !type2isPreRename && !type2isMultiDimPrimitiveArray && ArrayClassNameMapper.isObjectArrayWrapperDotName(type2dotName);
//        boolean atLeastOneObjectArray = type1isObjectArray || type2isObjectArray;
//        boolean bothAreObjectArray = type1isObjectArray && type2isObjectArray;
//
//        boolean type1isArray = type1isMultiDimPrimitiveArray || type1isObjectArray;
//        boolean type2isArray = type2isMultiDimPrimitiveArray || type2isObjectArray;
//        boolean bothAreArray = type1isArray && type2isArray;
//
//        boolean type1isSomethingElse = !type1isExceptionWrapper && !type1isPreRename && !type1isMultiDimPrimitiveArray;
//        boolean type2isSomethingElse = !type2isExceptionWrapper && !type2isPreRename && !type2isMultiDimPrimitiveArray;
//        boolean atLeastOneIsSomethingElse = type1isSomethingElse || type2isSomethingElse;
//
//        //--------------------------------EXCEPTIONS------------------------------------------------
//
//        // Return Throwable if both are exception wrappers or if we have an exception wrapper and a JCL exception
//        if (type1isExceptionWrapper && type2isExceptionWrapper) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
//        }
//        if (atLeastOneExceptionWrapper && atLeastOnePreRenameJclException) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
//        }
//
//        // Return Object if we have a pre-rename JCL non-exception type and an exception wrapper
//        if (atLeastOneExceptionWrapper && atLeastOnePreRenameNonException) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
//        }
//
//        // Return Object if we have an exception wrapper and something else
//        if (atLeastOneExceptionWrapper && atLeastOneIsSomethingElse) {
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
//        }
//        //------------------------------------------------------------------------------------------
//
//        //----------------------------------------ARRAYS--------------------------------------------
//
//        if (bothAreObjectArray) {
//            //TODO: is this assertion correct? (If not, we probably have to return IObjectArray)
//            RuntimeAssertionError.assertTrue(ArrayClassNameMapper.isInterfaceObjectArrayDotName(type1dotName) == ArrayClassNameMapper.isInterfaceObjectArrayDotName(type2dotName));
//            boolean arraysAreInterfaceObjectTypes = ArrayClassNameMapper.isInterfaceObjectArrayDotName(type1dotName);
//
//            int array1dimension = ArrayClassNameMapper.getObjectArrayDotNameDimension(type1dotName);
//
//            // Return shadow Object if the two arrays have different dimensions. TODO: what about Array, ObjectArray ???
//            if (array1dimension != ArrayClassNameMapper.getObjectArrayDotNameDimension(type2dotName)) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
//            }
//
//            // Otherwise, we strip the arrays down to their base types and get the common super class of the base types
//            // and then wrap them back up in the array wrappers and return.
//            String type1stripped = ArrayClassNameMapper.stripObjectArrayWrapperDotNameToBaseType(type1dotName);
//            String type2stripped = ArrayClassNameMapper.stripObjectArrayWrapperDotNameToBaseType(type2dotName);
//
//            // We call back into this method since we essentially have to treat these as two new inputs.
//            String type1strippedSlashName = Helpers.fulllyQualifiedNameToInternalName(type1stripped);
//            String type2strippedSlashName = Helpers.fulllyQualifiedNameToInternalName(type2stripped);
//
//            String commonSuper = Helpers.internalNameToFulllyQualifiedName(getCommonSuperWhenNotPreservedTest(type1strippedSlashName, type2strippedSlashName));
//
//            // If we hit the 'top' of the hierarchy (The Object types) then we just go to IObjectArray
//            if (commonSuper.equals(PocClassInfo.JAVA_LANG_OBJECT) || commonSuper.equals(PocClassInfo.SHADOW_IOBJECT) || commonSuper.equals(PocClassInfo.SHADOW_OBJECT)) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
//            }
//
//            // Finally, reconstruct the super class as an array and return.
//            return arraysAreInterfaceObjectTypes
//                ? Helpers.fulllyQualifiedNameToInternalName(ArrayClassNameMapper.wrapTypeDotNameAsInterfaceObjectArray(array1dimension, commonSuper))
//                : Helpers.fulllyQualifiedNameToInternalName(ArrayClassNameMapper.wrapTypeDotNameAsNonInterfaceObjectArray(array1dimension, commonSuper));
//        }
//
//        if (atLeastOneMultiDimPrimitiveArray) {
//
//            // Return java/lang/Object if the other type is pre-rename.
//            if (atLeastOneIsPreRename) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
//            }
//
//            // Return IObjectArray if the other type is an array (means the other is an object array).
//            if (bothAreArray) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
//            }
//
//            // We have a primitive involved, we have to return shadow Object.
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_OBJECT);
//        }
//
//        if (atLeastOneObjectArray) {
//
//            // Return java/lang/Object if the other type is pre-rename.
//            if (atLeastOneIsPreRename) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
//            }
//
//            // Return IObjectArray if the other type is an array (means the other is an object array).
//            if (bothAreArray) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT_ARRAY);
//            }
//
//            //TODO: we could be more precise here, checking if we have interface array && checking the other type as interface/class
//            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_IOBJECT);
//        }
//        //------------------------------------------------------------------------------------------
//
//        // All possible array cases are handled above (this is not true for exceptions).
//        RuntimeAssertionError.assertTrue(!atLeastOneMultiDimPrimitiveArray && !atLeastOneObjectArray);
//
//        // Finally, if we have any pre-rename types, we rename them so that we can query the hierarchy.
//        type1dotName = (type1isPreRename) ? PocClassInfo.rename(type1dotName) : type1dotName;
//        type2dotName = (type2isPreRename) ? PocClassInfo.rename(type2dotName) : type2dotName;
//
//        // Grab the super class.
//        String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);
//
//        // If we had any pre-rename types involved then we always un-name them when returning.
//        if (atLeastOnePreRenameJclException || atLeastOnePreRenameNonException) {
//
//            // If the common super was IObject, this becomes Object because we are 'un-naming' now.
//            if (commonSuper.equals(PocClassInfo.SHADOW_IOBJECT)) {
//                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
//            }
//
//            String unnamedCommonSuper = ClassNameExtractor.getOriginalClassName(commonSuper);
//            return Helpers.fulllyQualifiedNameToInternalName(unnamedCommonSuper);
//        }
//
//        // If we make it here, no pre-rename types were involved and we are done.
//        return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
//    }

    private String getCommonSuperClassViaNewClassHierarchyWhenDebuggabilityPreserved(String type1, String type2) {
        // Basically if we are here then we can receive all sorts of pre-rename class names because these
        // have to be retained for debugging. In this case, we are not so strict and basically allow any
        // pre-rename class to get through.

        // We still go through and handle exception wrappers the same.
        boolean type1isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapper(type1);
        boolean type2isExceptionWrapper = ExceptionWrapperNameMapper.isExceptionWrapper(type2);

        boolean type1isThrowableExceptionWrapper = type1isExceptionWrapper && WRAPPER_ROOT_THROWABLE.getName().equals(Helpers.internalNameToFulllyQualifiedName(type1));
        boolean type2isThrowableExceptionWrapper = type2isExceptionWrapper && WRAPPER_ROOT_THROWABLE.getName().equals(Helpers.internalNameToFulllyQualifiedName(type2));

        String type1dotName = null;
        String type2dotName = null;

        // Strip off the exception wrapper prefix, this is what we want to search for.
        type1dotName = (type1isExceptionWrapper) ? Helpers.internalNameToFulllyQualifiedName(ExceptionWrapperNameMapper.slashClassNameForWrapperName(type1)) : type1dotName;
        type2dotName = (type2isExceptionWrapper) ? Helpers.internalNameToFulllyQualifiedName(ExceptionWrapperNameMapper.slashClassNameForWrapperName(type2)) : type2dotName;

        // The manual throwable wrapper should be asked for its superclass, directly, instead of inferring it from the underlying type.
        type1dotName = (type1isThrowableExceptionWrapper) ? WRAPPER_ROOT_THROWABLE.getSuperclass().getName() : type1dotName;
        type2dotName = (type2isThrowableExceptionWrapper) ? WRAPPER_ROOT_THROWABLE.getSuperclass().getName() : type2dotName;

        // If we did not encounter an exception wrapper then these dot names are still null, give them a value now.
        type1dotName = (type1dotName == null) ? Helpers.internalNameToFulllyQualifiedName(type1) : type1dotName;
        type2dotName = (type2dotName == null) ? Helpers.internalNameToFulllyQualifiedName(type2) : type2dotName;

        // Now, since we can receive pre-rename classes here, we check for any post-rename prefixes and if we don't find them
        // then we add them on so we can talk about the class temporarily.
        boolean type1isPreRename = !type1isExceptionWrapper && !ClassNameExtractor.isPostRenameClass(type1dotName);
        boolean type2isPreRename = !type2isExceptionWrapper && !ClassNameExtractor.isPostRenameClass(type2dotName);

        type1dotName = (type1isPreRename) ? PocClassInfo.rename(type1dotName) : type1dotName;
        type2dotName = (type2isPreRename) ? PocClassInfo.rename(type2dotName) : type2dotName;

        // We can receive JCL exceptions here too.
        if (CommonGenerators.isJclExceptionType(Helpers.internalNameToFulllyQualifiedName(type1))) {

            // If we are unifying with another exception type we go to Throwable, otherwise Object.
            if (type2isExceptionWrapper) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            } else {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
            }

        } else if (CommonGenerators.isJclExceptionType(Helpers.internalNameToFulllyQualifiedName(type2))) {

            // If we are unifying with another exception type we go to Throwable, otherwise Object.
            if (type1isExceptionWrapper) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            } else {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
            }

        }

        // If we have a shadow exception
        if (!type1isExceptionWrapper && CommonGenerators.isJclExceptionType(ClassNameExtractor.getOriginalClassName(type1dotName))) {

            // 1. If type2 is an exception wrapper then java/lang/Throwable is the unifying type.
            if (type2isExceptionWrapper) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            }

            // 2. If type2 is a JCL exception then we should return the proper JCL exception.
            if (!ClassNameExtractor.isPostRenameClass(type2dotName) && CommonGenerators.isJclExceptionType(type2dotName)) {
                String type2renamed = PocClassInfo.rename(type2dotName);

                String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type2renamed, type1dotName);
                return Helpers.fulllyQualifiedNameToInternalName(ClassNameExtractor.getOriginalClassName(commonSuper));
            }

            // 3. If type2 is a shadow JCL exception then we should return the proper shadow exception.
            if (ClassNameExtractor.isPostRenameClass(type2dotName) && CommonGenerators.isJclExceptionType(ClassNameExtractor.getOriginalClassName(type2dotName))) {
                String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type2dotName, type1dotName);
                return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
            }

            // Finally, we are either left with a user-defined exception or some other non-exception, so we can just try and unify here again.
            String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);
            return Helpers.fulllyQualifiedNameToInternalName(commonSuper);

        } else if (!type2isExceptionWrapper && CommonGenerators.isJclExceptionType(ClassNameExtractor.getOriginalClassName(type2dotName))) {

            // 1. If type1 is an exception wrapper then java/lang/Throwable is the unifying type.
            if (type1isExceptionWrapper) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            }

            // 2. If type1 is a JCL exception then we should return the proper JCL exception.
            if (!ClassNameExtractor.isPostRenameClass(type1dotName) && CommonGenerators.isJclExceptionType(type1dotName)) {
                String type1renamed = PocClassInfo.rename(type1dotName);

                String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1renamed, type2dotName);
                return Helpers.fulllyQualifiedNameToInternalName(ClassNameExtractor.getOriginalClassName(commonSuper));
            }

            // 3. If type1 is a shadow JCL exception then we should return the proper shadow exception.
            if (ClassNameExtractor.isPostRenameClass(type1dotName) && CommonGenerators.isJclExceptionType(ClassNameExtractor.getOriginalClassName(type1dotName))) {
                String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);
                return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
            }

            // Finally, we are either left with a user-defined exception or some other non-exception, so we can just try and unify here again.
            String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);
            return Helpers.fulllyQualifiedNameToInternalName(commonSuper);

        }

        if (type1isExceptionWrapper || type2isExceptionWrapper) {
            return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
        }

        // Now we can search for the super class!
        String commonSuper = this.pocHierarchy.getTightestCommonSuperClass(type1dotName, type2dotName);

        // If this fails then we had an ambiguous unification problem to solve, so throw an exception to fail out.
        RuntimeAssertionError.assertTrue(commonSuper != null);

        if ((type1isPreRename || type2isPreRename) && (type1isExceptionWrapper || type2isExceptionWrapper)) {
            // One of our types is an exception wrapper, the other is pre-renamed.

            // If the other type is also an exception we return java/lang/Throwable, otherwise java/lang/Object
            if (type1isPreRename) {

                if (CommonGenerators.isJclExceptionType(Helpers.internalNameToFulllyQualifiedName(type1))) {
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
                } else {
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
                }

            } else {

                if (CommonGenerators.isJclExceptionType(Helpers.internalNameToFulllyQualifiedName(type2))) {
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
                } else {
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
                }

            }

        }

        if (type1isExceptionWrapper || type2isExceptionWrapper) {

            if (commonSuper.equals(PocClassInfo.JAVA_LANG_OBJECT)) {

                if (PocHierarchyAppender.isJavaLangErrorType(type1dotName) || PocHierarchyAppender.isJavaLangErrorType(type2dotName)) {

                    // This is when we unify a legit JCL error/exception with a wrapped one. They should unify to throwable.
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
                } else {

                    // This is when we unify any non-legit/wrapped exception with an exception.
                    return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_OBJECT);
                }

            } else if (commonSuper.equals(PocClassInfo.JAVA_LANG_THROWABLE)) {

                // If exception wrapper caused us to unify with java/lang/Throwable that's what we return back.
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.JAVA_LANG_THROWABLE);
            } else {

                // Otherwise, we must have an unwrapped exception in our hands, so we wrap it back up and return it.
                return ExceptionWrapperNameMapper.slashWrapperNameForClassName(Helpers.fulllyQualifiedNameToInternalName(commonSuper));
            }

        } else if (type1isPreRename || type2isPreRename) {

            // TODO: very strange, shadow Enum works here but java/lang/Enum || java/lang/Object do not
            // This happened in debug mode but this could still be possible in non-debug mode.
            if (commonSuper.equals(PocClassInfo.SHADOW_ENUM)) {
                return Helpers.fulllyQualifiedNameToInternalName(PocClassInfo.SHADOW_ENUM);
            }

            // If both our types are pre-rename then we return the pre-renamed super class.
            return Helpers.fulllyQualifiedNameToInternalName(ClassNameExtractor.getOriginalClassName(commonSuper));

        } else {

            // If we didn't have an exception wrapper in the query we can just return the super here.
            return Helpers.fulllyQualifiedNameToInternalName(commonSuper);
        }
    }

    private Stack<String> builTypeListFor(String type) {
        // NOTE:  These are "/-style" names.
        RuntimeAssertionError.assertTrue(-1 == type.indexOf("."));
        
        // The complexity here is that we have 3 different sources of truth to consult, and they are non-overlapping (only meeting at edges):
        // 1) Static class hierarchy within the contract.
        // 2) Dynamic class hierarchy, built as we operate on the contract code.
        // 3) The JDK classes, themselves (which should probably only apply to "java/lang" exceptions and "Object".
        // (we consult these in that order).
        
        Stack<String> stack = new Stack<>();
        String nextType = type;
        while (!"java/lang/Object".equals(nextType)) {
            stack.push(nextType);
            
            // Exception wrappers work the same as their underlying type, but all relationships are mapped into the wrapper space.
            boolean isWrapper = ExceptionWrapperNameMapper.isExceptionWrapper(nextType);
            // The rest of the generated wrappers may not yet have been generated when we need to unify the types but they do have a very simple
            // structure, so we can infer their relationships from the original underlying types.
            // The hand-written root must be handled differently, and its superclass read directly (since, being the root, it has this special
            // relationship with java.lang.Throwable).
            boolean isThrowableWrapper = false;
            if (isWrapper) {
                isThrowableWrapper = WRAPPER_ROOT_THROWABLE.getName().equals(Helpers.internalNameToFulllyQualifiedName(nextType));
                nextType = ExceptionWrapperNameMapper.slashClassNameForWrapperName(nextType);
            }
            String nextDotType = Helpers.internalNameToFulllyQualifiedName(nextType);
            // The manual throwable wrapper should be asked for its superclass, directly, instead of inferring it from the underlying type.
            String superDotName = isThrowableWrapper
                    ? WRAPPER_ROOT_THROWABLE.getSuperclass().getName()
                    : this.staticClassHierarchy.getTightestSuperClassName(nextDotType);
            if (null == superDotName) {
                superDotName = getSuperAsJdkType(nextDotType);
            }
            
            // If we didn't find it by now, there is something very wrong.
            RuntimeAssertionError.assertTrue(null != superDotName);
            RuntimeAssertionError.assertTrue(-1 == superDotName.indexOf("/"));
            
            String superName = Helpers.fulllyQualifiedNameToInternalName(superDotName);
            // We now wrap the super-class, if we started with a wrapper and this is NOT the shadow throwable wrapper.
            if (isWrapper && !isThrowableWrapper) {
                superName = ExceptionWrapperNameMapper.slashWrapperNameForClassName(superName);
            }
            RuntimeAssertionError.assertTrue(-1 == superName.indexOf("."));
            nextType = superName;
        }
        stack.push(nextType);
        return stack;
    }

    /**
     * NOTE:  This takes and returns .-style names.
     */
    private String getSuperAsJdkType(String name) {
        // NOTE:  These are ".-style" names.
        RuntimeAssertionError.assertTrue(-1 == name.indexOf("/"));
        
        String superName = null;
        try {
            Class<?> clazz = NodeEnvironment.singleton.loadSharedClass(name);
            // issue-362: temporarily, force interfaces to drop directly to java.lang.Object (interface relationships will be fleshed out later under this item).
            if (clazz.isInterface()) {
                superName = "java.lang.Object";
            } else {
                superName = clazz.getSuperclass().getName();
            }
        } catch (ClassNotFoundException e) {
            // We can return null, in this case.
        }
        return superName;
    }
}
