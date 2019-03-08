package org.aion.avm.core.relationships;

import java.util.Arrays;

import org.aion.avm.internal.RuntimeAssertionError;


public class TypeData {
    public static TypeData newInterface(String name, String[] implementedInterfaceNames) {
        // NOTE:  These are ".-style" names.
        RuntimeAssertionError.assertTrue(-1 == name.indexOf("/"));
        Arrays.stream(implementedInterfaceNames).forEach((string) -> RuntimeAssertionError.assertTrue(-1 == string.indexOf("/")));
        
        String superName = null;
        boolean isInterface = true;
        boolean isFinalClass = false;
        return new TypeData(name, superName, implementedInterfaceNames, isInterface, isFinalClass);
    }

    public static TypeData newClass(String name, String superName, boolean isFinalClass, String[] implementedInterfaceNames) {
        // NOTE:  These are ".-style" names.
        RuntimeAssertionError.assertTrue(-1 == name.indexOf("/"));
        RuntimeAssertionError.assertTrue(-1 == superName.indexOf("/"));
        Arrays.stream(implementedInterfaceNames).forEach((string) -> RuntimeAssertionError.assertTrue(-1 == string.indexOf("/")));
        
        boolean isInterface = false;
        return new TypeData(name, superName, implementedInterfaceNames, isInterface, isFinalClass);
    }


    public final String name;
    public final String superName;
    public final String[] implementedInterfaceNames;
    public final boolean isInterface;
    public final boolean isFinalClass;

    private TypeData(String name, String superName, String[] implementedInterfaceNames, boolean isInterface, boolean isFinalClass) {
        String[] interfaceCopy = new String[implementedInterfaceNames.length];
        System.arraycopy(implementedInterfaceNames, 0, interfaceCopy, 0, implementedInterfaceNames.length);
        
        this.name = name;
        this.superName = superName;
        this.implementedInterfaceNames = interfaceCopy;
        this.isInterface = isInterface;
        this.isFinalClass = isFinalClass;
    }
}
