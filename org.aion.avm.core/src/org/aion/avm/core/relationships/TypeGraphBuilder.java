package org.aion.avm.core.relationships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aion.avm.core.dappreading.LoadedJar;
import org.aion.avm.core.util.Helpers;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;


public class TypeGraphBuilder {
    public static TypeGraph buildFromJar(LoadedJar loadedJar) {
        // We interpret these maps a little differently so keep classes and interfaces distinctly.
        Map<String, TypeData> dotClasses = new HashMap<>();
        Map<String, TypeData> dotInterfaces = new HashMap<>();
        
        // First, walk the JAR contents to build our maps.
        Map<String, byte[]> classNameToBytes = loadedJar.classBytesByQualifiedNames;
        for (Map.Entry<String, byte[]> entry : classNameToBytes.entrySet()) {
            String classDotName = entry.getKey();
            
            // Decode the bytecode.
            ClassReader reader = new ClassReader(entry.getValue());
            CodeVisitor codeVisitor = new CodeVisitor();
            reader.accept(codeVisitor, ClassReader.SKIP_FRAMES);
            
            String[] interfaceDotNames = new String[codeVisitor.interfaceSlashNames.length];
            for (int i = 0; i < codeVisitor.interfaceSlashNames.length; ++i) {
                interfaceDotNames[i] = Helpers.internalNameToFulllyQualifiedName(codeVisitor.interfaceSlashNames[i]);
            }
            
            if (codeVisitor.isInterface) {
                TypeData data = TypeData.newInterface(classDotName, interfaceDotNames);
                dotInterfaces.put(classDotName, data);
            } else {
                String superDotName = Helpers.internalNameToFulllyQualifiedName(codeVisitor.parentSlashName);
                TypeData data = TypeData.newClass(classDotName, superDotName, codeVisitor.isFinal, interfaceDotNames);
                dotClasses.put(classDotName, data);
            }
        }
        
        // Now, evaluate each of these elements in each map to find all possible unifications to create the TypeGraph.
        Map<String, Set<String>> unificationsByName = new HashMap<>();
        processTypeSet(unificationsByName, dotClasses.keySet(), dotClasses, dotInterfaces);
        processTypeSet(unificationsByName, dotInterfaces.keySet(), dotClasses, dotInterfaces);
        return new TypeGraph(unificationsByName);
    }

    private static void processTypeSet(Map<String, Set<String>> unificationsByName, Set<String> types, Map<String, TypeData> dotClasses, Map<String, TypeData> dotInterfaces) {
        for (String typeName : types) {
            Set<String> unifications = getAllUnificationsOfType(typeName, dotClasses, dotInterfaces);
            unificationsByName.put(typeName, Collections.unmodifiableSet(unifications));
        }
    }

    private static Set<String> getAllUnificationsOfType(String typeName, Map<String, TypeData> dotClasses, Map<String, TypeData> dotInterfaces) {
        Set<String> dotTypes = new HashSet<>();
        List<String> toWalk = new ArrayList<>();
        dotTypes.add(typeName);
        toWalk.add(typeName);
        while (!toWalk.isEmpty()) {
            String next = toWalk.remove(0);
            TypeData clazz = dotClasses.get(next);
            if (null != clazz) {
                handleType(dotTypes, toWalk, clazz);
            } else {
                TypeData inter = dotInterfaces.get(next);
                if (null != inter) {
                    handleType(dotTypes, toWalk, clazz);
                }
            }
        }
        return dotTypes;
    }

    private static void handleType(Set<String> dotTypes, List<String> toWalk, TypeData oneType) {
        String superName = oneType.superName;
        if (dotTypes.add(superName)) {
            toWalk.add(superName);
        }
        for (String inter : oneType.implementedInterfaceNames) {
            if (dotTypes.add(inter)) {
                toWalk.add(inter);
            }
        }
    }


    private static final class CodeVisitor extends ClassVisitor {
        private String parentSlashName;
        private boolean isInterface;
        private boolean isFinal;
        private String[] interfaceSlashNames;

        private CodeVisitor() {
            super(Opcodes.ASM6);
        }

        // todo check nested parent
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            // todo parent may be null if the class is java.lang.Object. Add unit test for such a case
            this.parentSlashName = superName;
            this.isInterface = Opcodes.ACC_INTERFACE == (access & Opcodes.ACC_INTERFACE);
            this.isInterface = Opcodes.ACC_FINAL == (access & Opcodes.ACC_FINAL);
            this.interfaceSlashNames = interfaces;
        }
    }
}
