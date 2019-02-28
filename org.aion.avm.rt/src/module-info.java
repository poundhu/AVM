module org.aion.avm.rt {
    exports org.aion.avm.arraywrapper;
    exports org.aion.avm.exceptionwrapper.java.lang;
    exports org.aion.avm.internal;
    exports org.aion.avm.shadow.java.lang;
    exports org.aion.avm.shadow.java.lang.invoke;
    exports org.aion.avm.shadow.java.math;
    exports org.aion.avm.shadow.java.util;
    exports org.aion.avm.shadow.java.util.concurrent;
    exports org.aion.avm.shadow.java.util.function;
    exports org.aion.avm.shadowapi.org.aion.avm.api;
    exports org.aion.avm;

    requires org.aion.avm.api;


    // When running unit tests in Eclipse, these are required (our Ant build process avoids this but it is probably more correct with them).
    opens org.aion.avm.shadow.java.lang;
    opens org.aion.avm.shadowapi.org.aion.avm.api;
    opens org.aion.avm.internal;
}
