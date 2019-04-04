package org.aion.avm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
        PocClassInfo fakeF = newPreRenameInterface("F");

        // We dangle the cycle off of this parent node because we can't create a cycle off of
        // java/lang/Object.
        PocClassInfo parent = newPreRenameInterface("parent");

        // We create a 6-node cycle.
        PocClassInfo A = newNonFinalClassWithConcreteAndInterfaceSupers("A", parent, fakeF);
        PocClassInfo B = newPreRenameNonFinalClassWithConcreteSuper("B", A);
        PocClassInfo C = newPreRenameNonFinalClassWithConcreteSuper("C", B);
        PocClassInfo D = newPreRenameNonFinalClassWithConcreteSuper("D", C);
        PocClassInfo E = newPreRenameNonFinalClassWithConcreteSuper("E", D);
        PocClassInfo F = newPreRenameNonFinalClassWithConcreteSuper("F", E);

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
    public void testWhenChildHasMultipleInterfaceParents() {
        PocClassInfo interface1 = newPreRenameInterface("int1");
        PocClassInfo interface2 = newPreRenameInterface("int2");
        PocClassInfo child = newNonFinalClassWithConcreteAndInterfaceSupers("child", PocClassInfo.infoForJavaLangObject(), interface1, interface2);

        Collection<PocClassInfo> classInfos = toCollection(interface1, interface2, child);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertEquals(4, hierarchy.size());

        // Verify the root node is java/lang/Object
        checkRootIsJavaLangObject(hierarchy);

        // Verify its children are the two interfaces and child.
        Collection<PocHierarchyNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(PocHierarchyNode.from(interface1));
        expectedNodes.add(PocHierarchyNode.from(interface2));
        expectedNodes.add(PocHierarchyNode.from(child));

        Collection<PocNode> childrenOfObject = hierarchy.getRoot().getChildren();
        assertEquals(expectedNodes, childrenOfObject);

        // Verify each 'child' has java/lang/Object as its parent unless it is actually the child node,
        // in which case it has java/lang/Object and the interfaces as its parent.
        expectedNodes.clear();
        expectedNodes.add(PocHierarchyNode.from(interface1));
        expectedNodes.add(PocHierarchyNode.from(interface2));
        expectedNodes.add(PocHierarchyNode.from(PocClassInfo.infoForJavaLangObject()));

        for (PocNode childOfObject : childrenOfObject) {

            Collection<PocNode> parentsOfChild = childOfObject.getParents();

            if (childOfObject.getQualifiedName().equals(child.selfQualifiedName)) {
                assertEquals(expectedNodes, parentsOfChild);
            } else {
                assertEquals(1, parentsOfChild.size());
                assertEquals(PocHierarchyNode.javaLangObjectNode(), parentsOfChild.iterator().next());
            }

        }

        // Verify the two interfaces have child node as their child and that the child node has no children.
        for (PocNode childOfObject : childrenOfObject) {

            Collection<PocNode> childrenOfChild = childOfObject.getChildren();

            if (childOfObject.getQualifiedName().equals(child.selfQualifiedName)) {
                assertTrue(childrenOfChild.isEmpty());
            } else {
                assertEquals(1, childrenOfChild.size());
                assertEquals(PocHierarchyNode.from(child), childrenOfChild.iterator().next());
            }

        }

        assertTrue(verifier.verifyHierarchy(hierarchy).success);
    }

    @Test
    public void testWhenChildHasMultipleInterfaceParentsAndOneConcreteParent() {
        PocClassInfo interface1 = newPreRenameInterface("int1");
        PocClassInfo interface2 = newPreRenameInterface("int2");
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
        PocClassInfo child1 = newPreRenameNonFinalClassWithConcreteSuper("child1", parent);
        PocClassInfo child2 = newPreRenameNonFinalClassWithConcreteSuper("child2", parent);

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
        PocClassInfo parent = newPreRenameInterface("parent");
        PocClassInfo child1 = newPreRenameNonFinalClassWithConcreteSuper("child1", parent);
        PocClassInfo child2 = newPreRenameNonFinalClassWithConcreteSuper("child2", parent);

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
        PocClassInfo parentInterface = newPreRenameInterface("parentInt");
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
        PocClassInfo child1 = newPreRenameNonFinalClassWithConcreteSuper("child1", parent);
        PocClassInfo child2 = newPreRenameNonFinalClassWithConcreteSuper("child2", parent);

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
        PocClassInfo top = newPreRenameInterface("top");
        PocClassInfo upperMiddle = newPreRenameNonFinalClassWithConcreteSuper("upperMiddle", top);
        PocClassInfo lowerMiddle = newPreRenameNonFinalClassWithConcreteSuper("lowerMiddle", upperMiddle);
        PocClassInfo bottom = newPreRenameNonFinalClassWithConcreteSuper("bottom", lowerMiddle);

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
        PocClassInfo other = newPreRenameInterface("other");
        PocClassInfo duplicate1 = newPreRenameInterfaceWithInterfaceSupers("duplicate", other);
        PocClassInfo duplicate2 = newPreRenameInterfaceWithInterfaceSupers("duplicate", other);

        List<PocClassInfo> classInfos = toList(duplicate1, other, duplicate2);

        PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
    }

    @Test
    public void testWhenNodeIsChildOfFinalParent() {
        PocClassInfo finalParent = newFinalClass("final");
        PocClassInfo child = newPreRenameNonFinalClassWithConcreteSuper("child", finalParent);

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
        PocClassInfo iamabsent = newPreRenameInterface("iamabsent");
        PocClassInfo whereami = newPreRenameInterfaceWithInterfaceSupers("whereami", iamabsent);

        Collection<PocClassInfo> classInfos = toCollection(whereami);

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);

        PocVerificationResult result = verifier.verifyHierarchy(hierarchy);
        assertFalse(result.success);
        assertTrue(result.foundUnreachableNodes);
        assertEquals(2, result.numberOfUnreachableNodes);
    }

    @Test
    public void testTightestSuperClassOfTwoJavaLangObjects() {
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        PocVerificationResult r = verifier.verifyHierarchy(hierarchy);
        assertTrue(r.success);

        String commonSuper = hierarchy.getTightestCommonSuperClass(PocClassInfo.JAVA_LANG_OBJECT, PocClassInfo.JAVA_LANG_OBJECT);
        assertEquals(PocClassInfo.JAVA_LANG_OBJECT, commonSuper);
    }

    @Test
    public void testTightestSuperClassOfJavaLangObjectAndOther() {
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

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
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

        PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
        assertTrue(verifier.verifyHierarchy(hierarchy).success);

        String somebody = getClassInfoByName("int4", classInfos).selfQualifiedName;

        String commonSuper = hierarchy.getTightestCommonSuperClass(somebody, somebody);
        assertEquals(somebody, commonSuper);
    }

    @Test
    public void testTightestSuperClassOfTwoDistinctClasses() {
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

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
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

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
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

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
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();
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
        List<PocClassInfo> classInfos = getPreRenameClassInfosForCommonSuperTests();

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(classInfos);
            System.out.println("Permutation: " + classInfos);

            // Create the hierarchy and verify it's in tact.
            PocClassHierarchy hierarchy = PocClassHierarchy.createPreRenameHierarchyFrom(classInfos);
            assertTrue(verifier.verifyHierarchy(hierarchy).success);
        }
    }

    @Test
    public void testVerifierOnDeepCopyPermutations() {
        List<PocClassInfo> classInfos = getPostRenameClassInfosForCommonSuperTests();

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(classInfos);
            System.out.println("Permutation: " + classInfos);

            // Create a deep copy of the hierarchy and verify it's in tact.
            PocClassHierarchy copy = PocClassHierarchy.createPostRenameHierarchyFromPostRenameNames(classInfos).deepCopy();
            assertTrue(verifier.verifyHierarchy(copy).success);
        }
    }

    @Test
    public void testPreRenameDeepCopyDoesNotInfluenceOriginal() {
        PocClassHierarchy original = PocClassHierarchy.createPreRenameHierarchyFrom(getPreRenameClassInfosForCommonSuperTests());
        PocClassHierarchy copy = original.deepCopy();

        assertTrue(this.verifier.verifyHierarchy(original).success);
        assertTrue(this.verifier.verifyHierarchy(copy).success);

        // Verify the copy is equal to the original.
        assertEquals(original, copy);

        int originalSize = original.size();
        assertEquals(originalSize, copy.size());

        // We will add a new node to the copy, so its size will differ from the original.
        PocClassInfo newInterface = newPreRenameInterface("new");
        copy.add(newInterface);

        assertTrue(this.verifier.verifyHierarchy(copy).success);
        assertEquals(originalSize + 1, copy.size());
        assertNotEquals(original, copy);
    }

    @Test
    public void testPostRenameDeepCopyDoesNotInfluenceOriginal() {
        PocClassHierarchy original = PocClassHierarchy.createPostRenameHierarchyFromPostRenameNames(getPostRenameClassInfosForCommonSuperTests());
        PocClassHierarchy copy = original.deepCopy();

        assertTrue(this.verifier.verifyHierarchy(original).success);
        assertTrue(this.verifier.verifyHierarchy(copy).success);

        // Verify the copy is equal to the original.
        assertEquals(original, copy);

        int originalSize = original.size();
        assertEquals(originalSize, copy.size());

        // We will add a new node to the copy, so its size will differ from the original.
        PocClassInfo newInterface = newPostRenameInterface("new");
        copy.add(newInterface);

        assertTrue(this.verifier.verifyHierarchy(copy).success);
        assertEquals(originalSize + 1, copy.size());
        assertNotEquals(original, copy);
    }

    //TODO: test immutability

    //<-------------------------------------------------------------------------------------------->

    /**
     * Returns a new interface that descends from java/lang/Object and has no interface super classes.
     */
    private PocClassInfo newPreRenameInterface(String name) {
        return PocClassInfo.preRenameInfoFor(true, false, name, PocClassInfo.JAVA_LANG_OBJECT, null);
    }

    /**
     * Returns a new interface that descends from IObject.
     */
    private PocClassInfo newPostRenameInterface(String name) {
        return PocClassInfo.postRenameInfoFor(true, false, name, null, new String[]{ PocClassInfo.SHADOW_IOBJECT });
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

    private PocClassInfo newPreRenameInterfaceWithInterfaceSupers(String name, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(true, false, name, null, interfaces);
    }

    private PocClassInfo newPostRenameInterfaceWithInterfaceSupers(String name, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.postRenameInfoFor(true, false, name, null, interfaces);
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
    private PocClassInfo newPreRenameNonFinalClassWithConcreteSuper(String name, PocClassInfo superClass) {
        return PocClassInfo.preRenameInfoFor(false, false, name, superClass.selfQualifiedName, null);
    }

    private PocClassInfo newPostRenameNonFinalClassWithConcreteSuper(String name, PocClassInfo superClass) {
        return PocClassInfo.postRenameInfoFor(false, false, name, superClass.selfQualifiedName, null);
    }

    /**
     * Returns a new non-final class that descends from java/lang/Object and has the specified
     * interfaces as super classes.
     */
    private PocClassInfo newPreRenameNonFinalClassWithInterfaceSupers(String name, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.preRenameInfoFor(false, false, name, null, interfaces);
    }

    private PocClassInfo newPostRenameNonFinalClassWithInterfaceSupers(String name, PocClassInfo... interfaceInfos) {
        String[] interfaces = new String[interfaceInfos.length];

        for (int i = 0; i < interfaceInfos.length; i++) {
            assertTrue(interfaceInfos[i].isInterface);

            interfaces[i] = interfaceInfos[i].selfQualifiedName;
        }

        return PocClassInfo.postRenameInfoFor(false, false, name, null, interfaces);
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

    private List<PocClassInfo> getPreRenameClassInfosForCommonSuperTests() {
        PocClassInfo interface1 = newPreRenameInterface("int1");
        PocClassInfo interface2 = newPreRenameInterface("int2");
        PocClassInfo interface3 = newPreRenameInterface("int3");
        PocClassInfo interface4 = newPreRenameInterfaceWithInterfaceSupers("int4", interface1);
        PocClassInfo ambiguousClass1 = newPreRenameNonFinalClassWithInterfaceSupers("ambiguousClass1", interface2, interface3);
        PocClassInfo ambiguousClass2 = newPreRenameNonFinalClassWithInterfaceSupers("ambiguousClass2", interface2, interface3);
        PocClassInfo commonParentClass = newPreRenameNonFinalClassWithInterfaceSupers("commonParentClass", interface4);
        PocClassInfo unambiguousClass1 = newPreRenameNonFinalClassWithConcreteSuper("unambiguousClass1", commonParentClass);
        PocClassInfo unambiguousClass2 = newPreRenameNonFinalClassWithConcreteSuper("unambiguousClass2", commonParentClass);

        return toList(interface1, interface2, interface3, interface4, ambiguousClass1, ambiguousClass2, commonParentClass, unambiguousClass1, unambiguousClass2);
    }

    private List<PocClassInfo> getPostRenameClassInfosForCommonSuperTests() {
        PocClassInfo interface1 = newPostRenameInterface("int1");
        PocClassInfo interface2 = newPostRenameInterface("int2");
        PocClassInfo interface3 = newPostRenameInterface("int3");
        PocClassInfo interface4 = newPostRenameInterfaceWithInterfaceSupers("int4", interface1);
        PocClassInfo ambiguousClass1 = newPostRenameNonFinalClassWithInterfaceSupers("ambiguousClass1", interface2, interface3);
        PocClassInfo ambiguousClass2 = newPostRenameNonFinalClassWithInterfaceSupers("ambiguousClass2", interface2, interface3);
        PocClassInfo commonParentClass = newPostRenameNonFinalClassWithInterfaceSupers("commonParentClass", interface4);
        PocClassInfo unambiguousClass1 = newPostRenameNonFinalClassWithConcreteSuper("unambiguousClass1", commonParentClass);
        PocClassInfo unambiguousClass2 = newPostRenameNonFinalClassWithConcreteSuper("unambiguousClass2", commonParentClass);

        return toList(interface1, interface2, interface3, interface4, ambiguousClass1, ambiguousClass2, commonParentClass, unambiguousClass1, unambiguousClass2);
    }

    private List<PocClassInfo> getClassInfosForComplexHierarchy() {
        PocClassInfo interface1 = newPreRenameInterface("int1");
        PocClassInfo interface2 = newPreRenameInterface("int2");
        PocClassInfo interface3 = newPreRenameInterface("int3");
        PocClassInfo interface4 = newPreRenameInterfaceWithInterfaceSupers("int4", interface1, interface2);
        PocClassInfo child1 = newPreRenameNonFinalClassWithInterfaceSupers("child1", interface1, interface2);
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
