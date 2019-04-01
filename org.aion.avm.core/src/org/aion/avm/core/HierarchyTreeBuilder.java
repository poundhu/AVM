package org.aion.avm.core;

import java.util.HashMap;
import java.util.Map;

import org.aion.avm.core.types.ClassHierarchy;
import org.aion.avm.core.types.ClassInfo;
import org.aion.avm.core.types.Forest;
import org.aion.avm.core.types.Node;
import org.aion.avm.internal.RuntimeAssertionError;


/**
 * Provides a minimal interface for quickly building the Forest() objects in tests.
 * Returns itself from addClass for easy chaining in boiler-plate test code.
 */
public class HierarchyTreeBuilder {
    private final ClassHierarchy<String, ClassInfo> classHierarchy = new Forest<>();
    private final Map<String, Node<String, ClassInfo>> nameCache = new HashMap<>();

    public HierarchyTreeBuilder addClass(String name, String superclass, boolean isInterface, byte[] code) {
        // NOTE:  These are ".-style" names.
        RuntimeAssertionError.assertTrue(-1 == name.indexOf("/"));
        RuntimeAssertionError.assertTrue(-1 == superclass.indexOf("/"));

        // already added as parent
        if (this.nameCache.containsKey(name)){
            Node<String, ClassInfo> cur = this.nameCache.get(name);
            cur.setContent(new ClassInfo(isInterface, code));

            Node<String, ClassInfo> parent = this.nameCache.get(superclass);
            if (null == parent) {
                parent = new Node<>(superclass, null);
                this.nameCache.put(superclass,  parent);
            }
            this.classHierarchy.add(parent, cur);

        }else {

            Node<String, ClassInfo> parent = this.nameCache.get(superclass);
            if (null == parent) {
                // Must be a root.
                parent = new Node<>(superclass, null);
                this.nameCache.put(superclass, parent);
            }

            // Inject into tree.
            Node<String, ClassInfo> child = new Node<>(name, new ClassInfo(isInterface, code));

            // Cache result.
            this.nameCache.put(name, child);

            // Add connection.
            this.classHierarchy.add(parent, child);
        }
        
        return this;
    }

    public ClassHierarchy<String, ClassInfo> asMutableForest() {
        return this.classHierarchy;
    }
}
