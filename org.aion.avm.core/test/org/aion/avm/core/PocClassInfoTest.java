package org.aion.avm.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.aion.avm.core.types.PocClassInfo;
import org.aion.avm.internal.PackageConstants;
import org.aion.avm.internal.RuntimeAssertionError;
import org.junit.Test;

public class PocClassInfoTest {

    @Test
    public void testRenameJavaLangObject() {
        assertEquals(PocClassInfo.infoForJavaLangObject(), PocClassInfo.infoForJavaLangObject().toPostRenameClassInfo());
    }

    @Test(expected = RuntimeAssertionError.class)
    public void testRenameShadowIObject() {
        PocClassInfo.infoForShadowIObject().toPostRenameClassInfo();
    }

    @Test(expected = RuntimeAssertionError.class)
    public void testRenameShadowObject() {
        PocClassInfo.infoForShadowObject().toPostRenameClassInfo();
    }

    @Test
    public void testRenameInterfaceThatIsChildOfJavaLangObject() {
        PocClassInfo preRenameClassInfo = PocClassInfo.preRenameInfoFor(true, false, "class", PocClassInfo.JAVA_LANG_OBJECT, null);
        PocClassInfo renamedClassInfo = preRenameClassInfo.toPostRenameClassInfo();

        // We expect it to get re-parented under shadow IObject
        assertEquals(PackageConstants.kUserDotPrefix + preRenameClassInfo.selfQualifiedName, renamedClassInfo.selfQualifiedName);
        assertEquals(PocClassInfo.SHADOW_IOBJECT, renamedClassInfo.parentQualifiedName);
    }

    @Test
    public void testRenameClassThatIsChildOfJavaLangObject() {
        PocClassInfo preRenameClassInfo = PocClassInfo.preRenameInfoFor(false, false, "class", PocClassInfo.JAVA_LANG_OBJECT, null);
        PocClassInfo renamedClassInfo = preRenameClassInfo.toPostRenameClassInfo();

        // We expect it to get re-parented under shadow Object
        assertEquals(PackageConstants.kUserDotPrefix + preRenameClassInfo.selfQualifiedName, renamedClassInfo.selfQualifiedName);
        assertEquals(PocClassInfo.SHADOW_OBJECT, renamedClassInfo.parentQualifiedName);
    }

    @Test(expected = RuntimeAssertionError.class)
    public void testRenameClassWhoseParentIsShadowIObject() {
        PocClassInfo preRenameClassInfo = PocClassInfo.preRenameInfoFor(false, false, "class", PocClassInfo.SHADOW_IOBJECT, null);
        preRenameClassInfo.toPostRenameClassInfo();
    }

    @Test(expected = RuntimeAssertionError.class)
    public void testRenameClassWhoseParentIsShadowObject() {
        PocClassInfo preRenameClassInfo = PocClassInfo.preRenameInfoFor(false, false, "class", PocClassInfo.SHADOW_OBJECT, null);
        preRenameClassInfo.toPostRenameClassInfo();
    }

    @Test
    public void testRenameWithMultipleParentClasses() {
        String self = "self";
        String parent = "parentClass";
        String[] interfaces = new String[]{ "int1", "int2", "int3" };
        String[] renamedInterfaces = new String[]{ PackageConstants.kUserDotPrefix + "int1", PackageConstants.kUserDotPrefix + "int2", PackageConstants.kUserDotPrefix + "int3" };

        PocClassInfo preRenameClassInfo = PocClassInfo.preRenameInfoFor(false, true, self, parent, interfaces);
        PocClassInfo postRenameClassInfo = preRenameClassInfo.toPostRenameClassInfo();

        assertEquals(PackageConstants.kUserDotPrefix + self, postRenameClassInfo.selfQualifiedName);
        assertEquals(PackageConstants.kUserDotPrefix + parent, postRenameClassInfo.parentQualifiedName);
        assertArrayEquals(renamedInterfaces, postRenameClassInfo.getInterfaces());
    }

}
