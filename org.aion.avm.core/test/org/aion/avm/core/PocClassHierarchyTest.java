package org.aion.avm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.aion.avm.core.types.PocClassHierarchy;
import org.aion.avm.core.types.PocClassInfo;
import org.aion.avm.core.types.PocHierarchyNode;
import org.aion.avm.core.types.PocHierarchyVerifier;
import org.aion.avm.core.types.PocNode;
import org.aion.avm.core.types.PocVerificationResult;
import org.junit.Ignore;
import org.junit.Test;

public class PocClassHierarchyTest {
    private PocHierarchyVerifier verifier = new PocHierarchyVerifier();

    @Test
    @Ignore
    public void testWhenHierarchyHasTwoNodeCycle() {
        //TODO -- need a proper way of detecting cycles in our walk, or a guarantee of an acyclic graph.
    }

    /**
     * Running the below test will cause the verifier to loop forever.
     */
    @Test
    @Ignore
    public void testWhenHierarchyHasCycle() {
        //TODO -- need a proper way of detecting cycles in our walk, or a guarantee of an acyclic graph.


        // This is here to fake the hierarchy into accepting the circularity.
        PocClassInfo fakeF = newInterface("F");

        // We dangle the cycle off of this parent node because we can't create a cycle off of
        // java/lang/Object.
        PocClassInfo parent = newInterface("parent");

        // We create a 6-node cycle.
        PocClassInfo A = newNonFinalClassWithConcreteAndInterfaceSupers("A", parent, fakeF);
        PocClassInfo B = newNonFinalClassWithConcreteSuper("B", A);
        PocClassInfo C = newNonFinalClassWithConcreteSuper("C", B);
        PocClassInfo D = newNonFinalClassWithConcreteSuper("D", C);
        PocClassInfo E = newNonFinalClassWithConcreteSuper("E", D);
        PocClassInfo F = newNonFinalClassWithConcreteSuper("F", E);

        Collection<PocClassInfo> classInfos = toCollection(parent, A, B, C, D, E, F);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        verifier.verifyHierarchy(hierarchy);
    }

    @Test
    public void testEmptyPreRenameHierarchy() {
        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(Collections.emptyList());
        assertEquals(1, hierarchy.size());

        PocHierarchyNode root = hierarchy.getRoot();
        assertFalse(root.isGhostNode());
        assertTrue(root.getClassInfo().isJavaLangObject());
        assertEquals(PocHierarchyNode.javaLangObjectNode(), root);

        assertTrue(root.getChildren().isEmpty());
        assertTrue(root.getParents().isEmpty());

        // Verifies throws exception if hierarchy is corrupt, we expect no exceptions.
        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testEmptyPostRenameHierarchy() {
        PocClassHierarchy hierarchy = PocClassHierarchy.createPostRenameHierarchyFrom(Collections.emptyList());
        assertEquals(9, hierarchy.size());

        PocHierarchyNode root = hierarchy.getRoot();
        assertFalse(root.isGhostNode());
        assertTrue(root.getClassInfo().isJavaLangObject());
        assertEquals(PocHierarchyNode.javaLangObjectNode(), root);

        // Verify that java/lang/Object has 1 child: the shadow IObject class.
        assertEquals(1, root.getChildren().size());
        PocNode shadowIObject = root.getChildren().iterator().next();
        assertEquals(PocHierarchyNode.shadowIObjectNode(), shadowIObject);

        // Verify the shadow IObject class.
        assertFalse(shadowIObject.isGhostNode());
        assertTrue(shadowIObject.getClassInfo().isShadowIObject());
        assertTrue(shadowIObject.getClassInfo().isInterface);

        // Verify the shadow IObject has 1 parent (java/lang/Object) and 3 children (shadow Object/Comparable/Serializable).
        Collection<PocNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.shadowObjectNode());
        expectedNodes.add(PocHierarchyNode.shadowComparableNode());
        expectedNodes.add(PocHierarchyNode.shadowSerializableNode());

        assertEquals(1, shadowIObject.getParents().size());
        assertEquals(root, shadowIObject.getParents().iterator().next());
        assertEquals(expectedNodes, shadowIObject.getChildren());

        // Verify the shadow Object class. It has 1 parent (shadow IObject) and 2 children (shadow Enum/Throwable).
        PocNode shadowObject = getNodeByName(PocClassInfo.SHADOW_OBJECT, shadowIObject.getChildren());
        assertFalse(shadowObject.isGhostNode());
        assertTrue(shadowObject.getClassInfo().isShadowObject());
        assertFalse(shadowObject.getClassInfo().isInterface);

        expectedNodes.clear();
        expectedNodes.add(PocHierarchyNode.shadowEnumNode());
        expectedNodes.add(PocHierarchyNode.shadowThrowableNode());

        assertEquals(1, shadowObject.getParents().size());
        assertEquals(shadowIObject, shadowObject.getParents().iterator().next());
        assertEquals(expectedNodes, shadowObject.getChildren());

        // Verify the shadow Enum class. It has 3 parents (shadow Object, Comparable, Serializable) and no children.
        expectedNodes.clear();
        expectedNodes.add(PocHierarchyNode.shadowComparableNode());
        expectedNodes.add(PocHierarchyNode.shadowSerializableNode());
        expectedNodes.add(PocHierarchyNode.shadowObjectNode());

        PocNode shadowEnum = getNodeByName(PocClassInfo.SHADOW_ENUM, shadowObject.getChildren());
        assertFalse(shadowEnum.isGhostNode());
        assertFalse(shadowEnum.getClassInfo().isInterface);

        assertEquals(expectedNodes, shadowEnum.getParents());
        assertTrue(shadowEnum.getChildren().isEmpty());

        // Verify the shadow Comparable class. It has 1 parent (IObject) and 1 child (Enum).
        PocNode shadowComparable = getNodeByName(PocClassInfo.SHADOW_COMPARABLE, shadowEnum.getParents());
        assertFalse(shadowComparable.isGhostNode());
        assertTrue(shadowComparable.getClassInfo().isInterface);
        assertEquals(1, shadowComparable.getParents().size());
        assertEquals(shadowIObject, shadowComparable.getParents().iterator().next());
        assertEquals(1, shadowComparable.getChildren().size());
        assertEquals(shadowEnum, shadowComparable.getChildren().iterator().next());

        // Verify the shadow Serializable class. It has 1 parent (IObject) and 2 children (Enum/Throwable).
        PocNode shadowSerializable = getNodeByName(PocClassInfo.SHADOW_SERIALIZABLE, shadowEnum.getParents());

        expectedNodes.clear();
        expectedNodes.add(PocHierarchyNode.shadowEnumNode());
        expectedNodes.add(PocHierarchyNode.shadowThrowableNode());

        assertFalse(shadowSerializable.isGhostNode());
        assertTrue(shadowSerializable.getClassInfo().isInterface);
        assertEquals(1, shadowSerializable.getParents().size());
        assertEquals(shadowIObject, shadowSerializable.getParents().iterator().next());
        assertEquals(expectedNodes, shadowSerializable.getChildren());

        // Verify the shadow Throwable class. It has 2 parents (shadow Object/Serializable) and 1 child (shadow Exception).
        PocNode shadowThrowable = getNodeByName(PocClassInfo.SHADOW_THROWABLE, shadowObject.getChildren());

        expectedNodes.clear();
        expectedNodes.add(shadowObject);
        expectedNodes.add(shadowSerializable);

        assertFalse(shadowThrowable.isGhostNode());
        assertFalse(shadowThrowable.getClassInfo().isInterface);
        assertEquals(expectedNodes, shadowThrowable.getParents());
        assertEquals(1, shadowThrowable.getChildren().size());
        assertEquals(PocHierarchyNode.shadowExceptionNode(), shadowThrowable.getChildren().iterator().next());

        // Verify the shadow Exception class. It has 1 parent (shadow Throwable) and 1 child (shadow RuntimeException).
        PocNode shadowException = getNodeByName(PocClassInfo.SHADOW_EXCEPTION, shadowThrowable.getChildren());
        assertFalse(shadowException.isGhostNode());
        assertFalse(shadowException.getClassInfo().isInterface);
        assertEquals(1, shadowException.getParents().size());
        assertEquals(PocHierarchyNode.shadowThrowableNode(), shadowException.getParents().iterator().next());
        assertEquals(1, shadowException.getChildren().size());
        assertEquals(PocHierarchyNode.shadowRuntimeExceptionNode(), shadowException.getChildren().iterator().next());

        // Verify the shadow RuntimeException class. It has 1 parent (shadow Exception) and no children.
        PocNode shadowRuntimeException = getNodeByName(PocClassInfo.SHADOW_RUNTIME_EXCEPTION, shadowException.getChildren());
        assertFalse(shadowRuntimeException.isGhostNode());
        assertFalse(shadowRuntimeException.getClassInfo().isInterface);
        assertEquals(1, shadowRuntimeException.getParents().size());
        assertEquals(PocHierarchyNode.shadowExceptionNode(), shadowRuntimeException.getParents().iterator().next());
        assertTrue(shadowRuntimeException.getChildren().isEmpty());

        // Finally, ensure that the verifier recognizes this hierarchy as a valid hierarchy.
        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenChildHasMultipleInterfaceParents() {
        PocClassInfo interface1 = newInterface("int1");
        PocClassInfo interface2 = newInterface("int2");
        PocClassInfo child = newNonFinalClassWithConcreteAndInterfaceSupers("child", PocClassInfo.infoForJavaLangObject(), interface1, interface2);

        Collection<PocClassInfo> classInfos = toCollection(interface1, interface2, child);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(4, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its children are the two interfaces.
        Collection<PocHierarchyNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(interface1));
        expectedNodes.add(PocHierarchyNode.from(interface2));

        Collection<PocNode> interfaces = hierarchy.getRoot().getChildren();
        assertEquals(2, interfaces.size());
        assertEquals(expectedNodes, interfaces);

        // Remove the child so we only have the interfaces so that our next code block is easier.
        interfaces.remove(PocHierarchyNode.from(child));

        // Verify each child interface has java/lang/Object as its parent
        for (PocNode interfaceNode : interfaces) {
            Collection<PocNode> parentsOfInterface = interfaceNode.getParents();
            assertEquals(1, parentsOfInterface.size());

            assertEquals(PocHierarchyNode.javaLangObjectNode(), parentsOfInterface.iterator().next());
        }

        // Reuse the previous expected nodes, now for the child.
        expectedNodes.remove(PocHierarchyNode.from(child));

        // Verify each interface has the child class as its only child, and that the child has both
        // interfaces as its parents.
        for (PocNode interfaceNode : interfaces) {
            Collection<PocNode> childrenOfInterface = interfaceNode.getChildren();
            assertEquals(1, childrenOfInterface.size());

            PocNode childClass = childrenOfInterface.iterator().next();

            assertEquals(PocHierarchyNode.from(child), childClass);
            assertEquals(expectedNodes, childClass.getParents());
        }

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenChildHasMultipleInterfaceParentsAndOneConcreteParent() {
        PocClassInfo interface1 = newInterface("int1");
        PocClassInfo interface2 = newInterface("int2");
        PocClassInfo concrete = newNonFinalClass("concrete");
        PocClassInfo child = newNonFinalClassWithConcreteAndInterfaceSupers("child", concrete, interface1, interface2);

        Collection<PocClassInfo> classInfos = toCollection(interface1, interface2, concrete, child);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(5, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its children are the two interfaces as well as the concrete class.
        Collection<PocHierarchyNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(interface1));
        expectedNodes.add(PocHierarchyNode.from(interface2));
        expectedNodes.add(PocHierarchyNode.from(concrete));

        Collection<PocNode> childrenOfObject = hierarchy.getRoot().getChildren();
        assertEquals(3, childrenOfObject.size());
        assertEquals(expectedNodes, childrenOfObject);

        // Verify each child interface has java/lang/Object as its parent
        for (PocNode childOfObject : childrenOfObject) {
            Collection<PocNode> parentsOfInterface = childOfObject.getParents();
            assertEquals(1, parentsOfInterface.size());

            assertEquals(PocHierarchyNode.javaLangObjectNode(), parentsOfInterface.iterator().next());
        }

        // Verify each Object child has the 'child' class as its only child, and that the 'child'
        // has both interfaces, 'concrete' and java/lang/Object as its parents.
        for (PocNode interfaceNode : childrenOfObject) {
            Collection<PocNode> childrenOfInterface = interfaceNode.getChildren();
            assertEquals(1, childrenOfInterface.size());

            PocNode childClass = childrenOfInterface.iterator().next();

            assertEquals(PocHierarchyNode.from(child), childClass);
            assertEquals(expectedNodes, childClass.getParents());
        }

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenConcreteParentHasMultipleChildren() {
        PocClassInfo parent = newNonFinalClass("parent");
        PocClassInfo child1 = newNonFinalClassWithConcreteSuper("child1", parent);
        PocClassInfo child2 = newNonFinalClassWithConcreteSuper("child2", parent);

        Collection<PocClassInfo> classInfos = toCollection(child1, child2, parent);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(4, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its children is parent.
        Collection<PocNode> childrenOfObject = hierarchy.getRoot().getChildren();
        assertEquals(1, childrenOfObject.size());
        assertEquals(PocHierarchyNode.from(parent), childrenOfObject.iterator().next());

        // Verify parent has the two children child1,child2 and its parent is java/lang/object
        Collection<PocNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(child1));
        expectedNodes.add(PocHierarchyNode.from(child2));

        PocNode parentNode = childrenOfObject.iterator().next();
        assertEquals(1, parentNode.getParents().size());
        assertEquals(PocHierarchyNode.javaLangObjectNode(), parentNode.getParents().iterator().next());

        assertEquals(2, parentNode.getChildren().size());
        assertEquals(expectedNodes, parentNode.getChildren());

        // Verify each of parent's children has no children and only parent as a parent.
        for (PocNode childrenOfParent : parentNode.getChildren()) {
            assertEquals(1, childrenOfParent.getParents().size());
            assertEquals(PocHierarchyNode.from(parent), childrenOfParent.getParents().iterator().next());

            assertTrue(childrenOfParent.getChildren().isEmpty());
        }

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenInterfaceParentHasMultipleChildren() {
        PocClassInfo parent = newInterface("parent");
        PocClassInfo child1 = newNonFinalClassWithConcreteSuper("child1", parent);
        PocClassInfo child2 = newNonFinalClassWithConcreteSuper("child2", parent);

        Collection<PocClassInfo> classInfos = toCollection(child1, child2, parent);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(4, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its children is parent.
        Collection<PocNode> childrenOfObject = hierarchy.getRoot().getChildren();
        assertEquals(1, childrenOfObject.size());
        assertEquals(PocHierarchyNode.from(parent), childrenOfObject.iterator().next());

        // Verify parent has the two children child1,child2 and its parent is java/lang/object
        Collection<PocNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(child1));
        expectedNodes.add(PocHierarchyNode.from(child2));

        PocNode parentNode = childrenOfObject.iterator().next();
        assertEquals(1, parentNode.getParents().size());
        assertEquals(PocHierarchyNode.javaLangObjectNode(), parentNode.getParents().iterator().next());

        assertEquals(2, parentNode.getChildren().size());
        assertEquals(expectedNodes, parentNode.getChildren());

        // Verify each of parent's children has no children and only parent as a parent.
        for (PocNode childrenOfParent : parentNode.getChildren()) {
            assertEquals(1, childrenOfParent.getParents().size());
            assertEquals(PocHierarchyNode.from(parent), childrenOfParent.getParents().iterator().next());

            assertTrue(childrenOfParent.getChildren().isEmpty());
        }

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenChildGetsSecondParent() {
        PocClassInfo parentClass = newNonFinalClass("parentClass");
        PocClassInfo parentInterface = newInterface("parentInt");
        PocClassInfo child = newNonFinalClassWithConcreteAndInterfaceSupers("child", parentClass, parentInterface);

        // We are going to give the nodes to be constructed in an order so that the parentInterface
        // gets added after the parentClass and child have been (it processes in order).
        List<PocClassInfo> classInfos = toList(parentClass, child, parentInterface);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(4, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its children are parentClass and parentInterface.
        Collection<PocHierarchyNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(parentClass));
        expectedNodes.add(PocHierarchyNode.from(parentInterface));

        Collection<PocNode> childrenOfObject = hierarchy.getRoot().getChildren();
        assertEquals(2, childrenOfObject.size());
        assertEquals(expectedNodes, childrenOfObject);

        // Verify the parent of both these classes is java/lang/Object and that both have 1 child, 'child'.
        for (PocNode childOfObject : childrenOfObject) {
            assertEquals(1, childOfObject.getParents().size());
            assertEquals(PocHierarchyNode.javaLangObjectNode(), childOfObject.getParents().iterator().next());

            assertEquals(1, childOfObject.getChildren().size());
            assertEquals(PocHierarchyNode.from(child), childOfObject.getChildren().iterator().next());
        }

        // Verify that 'child' has 2 parents: parentClass, parentInterface, and has no children.
        PocNode childNode = childrenOfObject.iterator().next().getChildren().iterator().next();
        assertEquals(2, childNode.getParents().size());
        assertEquals(expectedNodes, childNode.getParents());

        assertTrue(childNode.getChildren().isEmpty());

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenParentGetsSecondChild() {
        PocClassInfo parent = newNonFinalClass("parent");
        PocClassInfo child1 = newNonFinalClassWithConcreteSuper("child1", parent);
        PocClassInfo child2 = newNonFinalClassWithConcreteSuper("child2", parent);

        // We are going to give the nodes to be constructed in an order so that the child2
        // gets added after the parent and child have been (it processes in order).
        List<PocClassInfo> classInfos = toList(parent, child1, child2);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(4, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its child is parent.
        Collection<PocNode> childrenOfObject = hierarchy.getRoot().getChildren();
        assertEquals(1, childrenOfObject.size());
        assertEquals(PocHierarchyNode.from(parent), childrenOfObject.iterator().next());

        // Verify the parent of both the 'parent' class is java/lang/Object and that it has the 2 children.
        Collection<PocNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(child1));
        expectedNodes.add(PocHierarchyNode.from(child2));

        for (PocNode childOfObject : childrenOfObject) {
            assertEquals(1, childOfObject.getParents().size());
            assertEquals(PocHierarchyNode.javaLangObjectNode(), childOfObject.getParents().iterator().next());

            assertEquals(2, childOfObject.getChildren().size());
            assertEquals(expectedNodes, childOfObject.getChildren());
        }

        // Verify that each child has only 'parent' as its parent and has no children.
        PocNode parentNode = childrenOfObject.iterator().next();
        for (PocNode childOfParent : parentNode.getChildren()) {
            assertEquals(1, childOfParent.getParents().size());
            assertEquals(parentNode, childOfParent.getParents().iterator().next());

            assertTrue(childOfParent.getChildren().isEmpty());
        }

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testConstructLinearHierarchy() {
        PocClassInfo top = newInterface("top");
        PocClassInfo upperMiddle = newNonFinalClassWithConcreteSuper("upperMiddle", top);
        PocClassInfo lowerMiddle = newNonFinalClassWithConcreteSuper("lowerMiddle", upperMiddle);
        PocClassInfo bottom = newNonFinalClassWithConcreteSuper("bottom", lowerMiddle);

        // We construct from the bottom up (with a zig-zag) just to get more coverage over the ghost nodes.
        List<PocClassInfo> classInfos = toList(bottom, lowerMiddle, top, upperMiddle);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(5, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        Collection<PocNode> expectedParents = new ArrayList<>();
        Collection<PocNode> expectedChildren = new ArrayList<>();
        expectedChildren.add(PocHierarchyNode.from(top));

        // Verify the linear hierarchy nodes...

        // Verify the root.
        assertEquals(expectedParents, hierarchy.getRoot().getParents());
        assertEquals(expectedChildren, hierarchy.getRoot().getChildren());

        // Verify top.
        PocNode topNode = hierarchy.getRoot().getChildren().iterator().next();
        expectedParents.add(PocHierarchyNode.javaLangObjectNode());
        expectedChildren.clear();
        expectedChildren.add(PocHierarchyNode.from(upperMiddle));

        assertEquals(expectedParents, topNode.getParents());
        assertEquals(expectedChildren, topNode.getChildren());

        // Verify upperMiddle.
        PocNode upperMiddleNode = topNode.getChildren().iterator().next();
        expectedParents.clear();
        expectedParents.add(topNode);
        expectedChildren.clear();
        expectedChildren.add(PocHierarchyNode.from(lowerMiddle));

        assertEquals(expectedParents, upperMiddleNode.getParents());
        assertEquals(expectedChildren, upperMiddleNode.getChildren());

        // Verify lowerMiddle.
        PocNode lowerMiddleNode = upperMiddleNode.getChildren().iterator().next();
        expectedParents.clear();
        expectedParents.add(upperMiddleNode);
        expectedChildren.clear();
        expectedChildren.add(PocHierarchyNode.from(bottom));

        assertEquals(expectedParents, lowerMiddleNode.getParents());
        assertEquals(expectedChildren, lowerMiddleNode.getChildren());

        // Verify bottom.
        PocNode bottomNode = lowerMiddleNode.getChildren().iterator().next();
        expectedParents.clear();
        expectedParents.add(lowerMiddleNode);
        expectedChildren.clear();

        assertEquals(expectedParents, bottomNode.getParents());
        assertEquals(expectedChildren, bottomNode.getChildren());

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testConstructComplexHierarchy() {
        List<PocClassInfo> classInfos = getClassInfosForComplexHierarchy();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(7, hierarchy.size());

        // Verify that java/lang/Object has the 3 interfaces as its children.
        Collection<PocNode> expectedChildren = new ArrayList<>();
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("int1", classInfos)));
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("int2", classInfos)));
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("int3", classInfos)));

        Collection<PocNode> expectedParents = new ArrayList<>();
        expectedParents.add(PocHierarchyNode.javaLangObjectNode());

        PocHierarchyNode root = hierarchy.getRoot();
        assertEquals(expectedChildren, root.getChildren());

        // Verify that int1 has java/lang/Object as its parent and child1, int4 as children
        expectedChildren.clear();
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("child1", classInfos)));
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("int4", classInfos)));

        PocNode interface1Node = getNodeByName("int1", root.getChildren());
        assertEquals(expectedParents, interface1Node.getParents());
        assertEquals(expectedChildren, interface1Node.getChildren());

        // Verify that int2 has java/lang/Object as its parent and child1, int4 as children
        PocNode interface2Node = getNodeByName("int2", root.getChildren());
        assertEquals(expectedParents, interface1Node.getParents());
        assertEquals(expectedChildren, interface2Node.getChildren());

        // Verify that int3 has java/lang/Object as its parent and no children.
        expectedChildren.clear();

        PocNode interface3Node = getNodeByName("int3", root.getChildren());
        assertEquals(expectedParents, interface1Node.getParents());
        assertEquals(expectedChildren, interface3Node.getChildren());

        // Verify that int4 has child2 as its only child and int1, int2 as parents.
        expectedParents.clear();
        expectedParents.add(PocHierarchyNode.from(getClassInfoByName("int1", classInfos)));
        expectedParents.add(PocHierarchyNode.from(getClassInfoByName("int2", classInfos)));
        expectedChildren.clear();
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("child2", classInfos)));

        PocNode interface4Node = getNodeByName("int4", interface2Node.getChildren());
        assertEquals(expectedParents, interface4Node.getParents());
        assertEquals(expectedChildren, interface4Node.getChildren());

        // Verify that child1 has child2 as its only child and int1, int2 as parents.
        expectedParents.clear();
        expectedParents.add(PocHierarchyNode.from(getClassInfoByName("int1", classInfos)));
        expectedParents.add(PocHierarchyNode.from(getClassInfoByName("int2", classInfos)));
        expectedChildren.clear();
        expectedChildren.add(PocHierarchyNode.from(getClassInfoByName("child2", classInfos)));

        PocNode child1Node = getNodeByName("child1", interface2Node.getChildren());
        assertEquals(expectedParents, child1Node.getParents());
        assertEquals(expectedChildren, child1Node.getChildren());

        // Verify that child2 has no children and has child1, int4 as parents.
        expectedParents.clear();
        expectedParents.add(PocHierarchyNode.from(getClassInfoByName("int4", classInfos)));
        expectedParents.add(PocHierarchyNode.from(getClassInfoByName("child1", classInfos)));
        expectedChildren.clear();

        PocNode child2Node = getNodeByName("child2", child1Node.getChildren());
        assertEquals(expectedParents, child2Node.getParents());
        assertEquals(expectedChildren, child2Node.getChildren());

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testConstructComplexHierarchyMultipleWays() {
        // The hierarchy has a thorough verification done in the test: testConstructComplexHierarchy
        // We use the same hierarchy here, we just shuffle its input list to test out various ways
        // the hierarchy could be constructed.

        List<PocClassInfo> classInfos = getClassInfosForComplexHierarchy();

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(classInfos);
            System.out.println("Permutation: " + classInfos);

            // Create the hierarchy and verify it's in tact.
            PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
            assertTrue(verifier.verifyHierarchy(hierarchy).success);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddingNullNodeCollection() {
        PocHierarchyNode.from(null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddingCollectionWithNullNode() {
        List<PocClassInfo> classInfos = new ArrayList<>();
        classInfos.add(null);

        PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddJavaLangObjectToHierarchy() {
        Collection<PocClassInfo> classInfos = new ArrayList<>();
        classInfos.add(PocClassInfo.infoForJavaLangObject());

        PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddNodeTwice() {
        PocClassInfo other = newInterface("other");
        PocClassInfo duplicate1 = newInterfaceWithInterfaceSupers("duplicate", other);
        PocClassInfo duplicate2 = newInterfaceWithInterfaceSupers("duplicate", other);

        List<PocClassInfo> classInfos = toList(duplicate1, other, duplicate2);

        PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
    }

    @Test
    public void testWhenNodeIsChildOfFinalParent() {
        PocClassInfo finalParent = newFinalClass("final");
        PocClassInfo child = newNonFinalClassWithConcreteSuper("child", finalParent);

        Collection<PocClassInfo> classInfos = toCollection(child, finalParent);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);

        PocVerificationResult result = verifier.verifyHierarchy(hierarchy);
        assertFalse(result.success);
        assertTrue(result.foundFinalSuper);
    }

    @Test
    public void testWhenNodeIsChildOfMultipleConcreteClasses() {
        PocClassInfo concreteParent1 = newNonFinalClass("concrete1");
        PocClassInfo concreteParent2 = newNonFinalClass("concrete2");
        PocClassInfo child = newNonFinalClassWithMultipleConcreteSupers("child", concreteParent1, concreteParent2);

        Collection<PocClassInfo> classInfos = toCollection(child, concreteParent2, concreteParent1);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);

        PocVerificationResult result = verifier.verifyHierarchy(hierarchy);
        assertFalse(result.success);
        assertTrue(result.foundMultipleNonInterfaceSupers);
    }

    @Test
    public void testWhenInterfaceIsChildOfConcreteClass() {
        PocClassInfo concreteParent = newNonFinalClass("concrete");
        PocClassInfo interfaceChild = newInterfaceWithConcreteSuper("interface", concreteParent);

        Collection<PocClassInfo> classInfos = toCollection(interfaceChild, concreteParent);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);

        PocVerificationResult result = verifier.verifyHierarchy(hierarchy);
        assertFalse(result.success);
        assertTrue(result.foundInterfaceWithConcreteSuper);
    }

    @Test
    public void testWhenNodeIsUnreachable() {
        PocClassInfo iamabsent = newInterface("iamabsent");
        PocClassInfo whereami = newInterfaceWithInterfaceSupers("whereami", iamabsent);

        Collection<PocClassInfo> classInfos = toCollection(whereami);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);

        PocVerificationResult result = verifier.verifyHierarchy(hierarchy);
        assertFalse(result.success);
        assertTrue(result.foundUnreachableNodes);
        assertEquals(2, result.numberOfUnreachableNodes);
    }

    @Test
    public void testTightestSuperClassOfTwoJavaLangObjects() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        String commonSuper = hierarchy.getTightestCommonSuperClass(PocClassInfo.JAVA_LANG_OBJECT, PocClassInfo.JAVA_LANG_OBJECT);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);
    }

    @Test
    public void testTightestSuperClassOfJavaLangObjectAndOther() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        // Note that other here is not ambiguous in this context, only when paired with the other ambiguous class.
        String javaLangObject = PocClassInfo.JAVA_LANG_OBJECT;
        String other = getClassInfoByName("ambiguousClass2", classInfos).selfQualifiedName;

        String commonSuper = hierarchy.getTightestCommonSuperClass(javaLangObject, other);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);
    }

    @Test
    public void testTightestSuperClassOfTwoOfTheSameClass() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        String somebody = getClassInfoByName("int4", classInfos).selfQualifiedName;

        String commonSuper = hierarchy.getTightestCommonSuperClass(somebody, somebody);
        assertEquals(somebody, commonSuper);
    }

    @Test
    public void testTightestSuperClassOfTwoDistinctClasses() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        // Note the ambiguous class is not ambiguous in this case, only when paired with the other ambiguous class.
        String somebody = getClassInfoByName("unambiguousClass1", classInfos).selfQualifiedName;
        String somebodyElse = getClassInfoByName("ambiguousClass2", classInfos).selfQualifiedName;

        String commonSuper = hierarchy.getTightestCommonSuperClass(somebody, somebodyElse);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);
    }

    @Test
    public void testTightestSuperClassOfTwoClassesWithDeepLinearAncestries() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        String somebody = getClassInfoByName("unambiguousClass1", classInfos).selfQualifiedName;
        String somebodyElse = getClassInfoByName("unambiguousClass2", classInfos).selfQualifiedName;

        String expectedSuper = getClassInfoByName("commonParentClass", classInfos).selfQualifiedName;

        String commonSuper = hierarchy.getTightestCommonSuperClass(somebody, somebodyElse);
        assertEquals(expectedSuper, commonSuper);
    }

    @Test
    public void testTightestSuperClassGivenAmbiguousAncestries() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        String somebody = getClassInfoByName("ambiguousClass1", classInfos).selfQualifiedName;
        String somebodyElse = getClassInfoByName("ambiguousClass2", classInfos).selfQualifiedName;

        // Ambiguous super sets, so we expect null here.
        String commonSuper = hierarchy.getTightestCommonSuperClass(somebody, somebodyElse);
        assertNull(commonSuper);
    }

    @Test
    public void testMultipleTightestSuperClassQueries() {
        // This is mainly to test out that the hierarchy cleans itself up properly after each query.
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();
        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        // Grab all of the classes we will need to replicate the above queries.
        String ambiguousClass2 = getClassInfoByName("ambiguousClass2", classInfos).selfQualifiedName;
        String interface4 = getClassInfoByName("int4", classInfos).selfQualifiedName;
        String unambiguousClass1 = getClassInfoByName("unambiguousClass1", classInfos).selfQualifiedName;
        String unambiguousClass2 = getClassInfoByName("unambiguousClass2", classInfos).selfQualifiedName;
        String commonParentClass = getClassInfoByName("commonParentClass", classInfos).selfQualifiedName;
        String ambiguousClass1 = getClassInfoByName("ambiguousClass1", classInfos).selfQualifiedName;

        // Run each of the above queries one after the other on the same hierarchy.
        String commonSuper = hierarchy.getTightestCommonSuperClass(PocClassInfo.JAVA_LANG_OBJECT, PocClassInfo.JAVA_LANG_OBJECT);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);

        commonSuper = hierarchy.getTightestCommonSuperClass(PocClassInfo.JAVA_LANG_OBJECT, ambiguousClass2);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);

        commonSuper = hierarchy.getTightestCommonSuperClass(interface4, interface4);
        assertEquals(interface4, commonSuper);

        commonSuper = hierarchy.getTightestCommonSuperClass(unambiguousClass1, ambiguousClass2);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);

        commonSuper = hierarchy.getTightestCommonSuperClass(unambiguousClass1, unambiguousClass2);
        assertEquals(commonParentClass, commonSuper);

        commonSuper = hierarchy.getTightestCommonSuperClass(ambiguousClass1, ambiguousClass2);
        assertNull(commonSuper);
    }

    /**
     * We have another hierarchy, so why not just run some more verifications on different
     * permutations.
     */
    @Test
    public void testVerifierOnCommonSuperHierarchyPermutations() {
        List<PocClassInfo> classInfos = getClassInfosForCommonSuperTests();

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(classInfos);
            System.out.println("Permutation: " + classInfos);

            // Create the hierarchy and verify it's in tact.
            PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
            assertTrue(verifier.verifyHierarchy(hierarchy).success);
        }
    }

    //TODO: test immutability

    //<-------------------------------------------------------------------------------------------->

    /**
     * Returns a new interface that descends from java/lang/Object and has no interface super classes.
     */
    private PocClassInfo newInterface(String name) {
        return PocClassInfo.preRenameInfoFor(true, false, name, PocClassInfo.JAVA_LANG_OBJECT, null);
    }

    /**
     * Returns a new interface that has a concrete super class.
     *
     * We have to declare it as an interface to fool the hierarchy.
     */
    private PocClassInfo newInterfaceWithConcreteSuper(String name, PocClassInfo superInfo) {
        String[] superClass = new String[]{ superInfo.selfQualifiedName };
        return PocClassInfo.preRenameInfoFor(true, false, name, null, superClass);
    }

    private PocClassInfo newInterfaceWithInterfaceSupers(String name, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(true, false, name, null, interfaces);
    }

    /**
     * Returns a new non-interface class that descends from java/lang/Object and has no interface
     * super classes.
     */
    private PocClassInfo newNonFinalClass(String name) {
        return PocClassInfo.preRenameInfoFor(false, false, name, PocClassInfo.JAVA_LANG_OBJECT, null);
    }

    /**
     * Returns a new non-final class that descends from superClass and has no interface super classes.
     */
    private PocClassInfo newNonFinalClassWithConcreteSuper(String name, PocClassInfo superClass) {
        return PocClassInfo.preRenameInfoFor(false, false, name, superClass.selfQualifiedName, null);
    }

    /**
     * Returns a new non-final class that descends from java/lang/Object and has the specified
     * interfaces as super classes.
     */
    private PocClassInfo newNonFinalClassWithInterfaceSupers(String name, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(false, false, name, null, interfaces);
    }

    /**
     * Returns a new non-final class that descends from java/lang/Object and has the specified
     * concrete classes as super classes.
     *
     * Yes, we are declaring them as interfaces here. But this doesn't make them interfaces. We are
     * lying, and tricking the hierarchy.
     */
    private PocClassInfo newNonFinalClassWithMultipleConcreteSupers(String name, PocClassInfo... superInfos) {
        String[] interfaces = new String[superInfos.length];

        for (int i = 0; i < superInfos.length; i++) {
            interfaces[i] = superInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(false, false, name, null, interfaces);
    }

    /**
     * Returns a new non-final class that descends from superInfo and has the specified interfaces
     * as super classes as well.
     */
    private PocClassInfo newNonFinalClassWithConcreteAndInterfaceSupers(String name, PocClassInfo superInfo, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(false, false, name, superInfo.selfQualifiedName, interfaces);
    }

    /**
     * Returns a new final class that descends from java/lang/Object.
     */
    private PocClassInfo newFinalClass(String name) {
        return PocClassInfo.preRenameInfoFor(false, true, name, PocClassInfo.JAVA_LANG_OBJECT, null);
    }

    /**
     * Returns a new final class that descends from java/lang/Object and has the specified
     * interfaces as super classes.
     */
    private PocClassInfo newFinalClassWithConcreteAndInterfaceSupers(String name, PocClassInfo superInfo, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(false, true, name, superInfo.selfQualifiedName, interfaces);
    }

    private List<PocClassInfo> getClassInfosForCommonSuperTests() {
        PocClassInfo interface1 = newInterface("int1");
        PocClassInfo interface2 = newInterface("int2");
        PocClassInfo interface3 = newInterface("int3");
        PocClassInfo interface4 = newInterfaceWithInterfaceSupers("int4", interface1);
        PocClassInfo ambiguousClass1 = newNonFinalClassWithInterfaceSupers("ambiguousClass1", interface2, interface3);
        PocClassInfo ambiguousClass2 = newNonFinalClassWithInterfaceSupers("ambiguousClass2", interface2, interface3);
        PocClassInfo commonParentClass = newNonFinalClassWithInterfaceSupers("commonParentClass", interface4);
        PocClassInfo unambiguousClass1 = newNonFinalClassWithConcreteSuper("unambiguousClass1", commonParentClass);
        PocClassInfo unambiguousClass2 = newNonFinalClassWithConcreteSuper("unambiguousClass2", commonParentClass);

        return toList(interface1, interface2, interface3, interface4, ambiguousClass1, ambiguousClass2, commonParentClass, unambiguousClass1, unambiguousClass2);
    }

    private List<PocClassInfo> getClassInfosForComplexHierarchy() {
        PocClassInfo interface1 = newInterface("int1");
        PocClassInfo interface2 = newInterface("int2");
        PocClassInfo interface3 = newInterface("int3");
        PocClassInfo interface4 = newInterfaceWithInterfaceSupers("int4", interface1, interface2);
        PocClassInfo child1 = newNonFinalClassWithInterfaceSupers("child1", interface1, interface2);
        PocClassInfo child2 = newFinalClassWithConcreteAndInterfaceSupers("child2", child1, interface4);

        return toList(interface1, interface2, interface3, child1, interface4, child2);
    }

    private static PocNode getNodeByName(String name, Collection<PocNode> nodes) {
        for (PocNode node : nodes) {
            if (node.getQualifiedName().equals(name)) {
                return node;
            }
        }
        fail("Could not find name: " + name);
        return null;
    }

    private static PocClassInfo getClassInfoByName(String name, Collection<PocClassInfo> classInfos) {
        for (PocClassInfo classInfo : classInfos) {
            if (classInfo.selfQualifiedName.equals(name)) {
                return classInfo;
            }
        }
        fail("Could not find name: " + name);
        return null;
    }

    private Collection<PocClassInfo> toCollection(PocClassInfo... classInfos) {
        return Arrays.asList(classInfos);
    }

    private List<PocClassInfo> toList(PocClassInfo... classInfos) {
        return Arrays.asList(classInfos);
    }

    private void checkRootIsJavaLangObject(PocClassHierarchy hierarchy) {
        PocHierarchyNode root = hierarchy.getRoot();
        assertFalse(root.isGhostNode());
        assertEquals(PocClassInfo.infoForJavaLangObject(), root.getClassInfo());
        assertTrue(root.getParents().isEmpty());
    }

}
