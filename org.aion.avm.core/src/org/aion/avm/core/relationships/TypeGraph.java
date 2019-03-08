package org.aion.avm.core.relationships;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.aion.avm.internal.RuntimeAssertionError;


/**
 * We handle the type graph as pre-computed mappings from a type name to all the types with which it unifies.
 * This could make this somewhat large but querying the structure is quick (since finding the tightest
 * unification is complex) and the design is simple.
 * Note:  All names are .-style.
 */
public class TypeGraph {
    private Map<String, Set<String>> unificationsByType;

    public TypeGraph(Map<String, Set<String>> unificationsByType) {
        this.unificationsByType = Collections.unmodifiableMap(unificationsByType);
    }

    public String getTightestUnificationOfTypes(String type1, String type2) throws AmbiguousUnificationException {
        // NOTE:  These are ".-style" names.
        RuntimeAssertionError.assertTrue(-1 == type1.indexOf("/"));
        RuntimeAssertionError.assertTrue(-1 == type2.indexOf("/"));
        
        // Find the sets of possible unifications for each type.
        Set<String> type1Set = this.unificationsByType.get(type1);
        Set<String> type2Set = this.unificationsByType.get(type2);
        
        // If either of these doesn't exist, check 
    }


    public static class AmbiguousUnificationException extends Exception {
        private static final long serialVersionUID = 1L;
        public AmbiguousUnificationException() {
            
        }
    }
}
