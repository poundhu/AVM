package org.aion.avm.shadow.java.lang;

import org.aion.avm.ClassNameExtractor;
import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IPersistenceToken;
import org.aion.avm.shadow.java.io.Serializable;

/**
 * Our shadow implementation of java.lang.Throwable.
 * TODO:  Determine how to handle the calls that don't make sense in this environment or depend on types we aren't including.
 * 
 * NOTE:  Instances of this class never actually touch the underlying VM-generated exception.
 * If we want to carry that information around, we will need a new constructor, an addition to the generated stubs, and a sense of how to use it.
 * Avoiding carrying those instances around means that this implementation becomes very safely defined.
 * It does, however, mean that we can't expose stack traces since those are part of the VM-generated exceptions.
 *
 * NOTE: All shadow Throwable and its derived exceptions and errors' APIs are not billed; since the native exception object is not billed in the constructor,
 * and we replace them with the shadow instances only when it is caught (in a catch or finally block), to have a more consistent fee schedule, the shadow
 * methods are free of energy charges as well. Then the user doesn't experience different charges in slightly different scenarios (created and thrown, caught or not caught).
 * Also note that at the creation of these exception/error objects, the 'new' bytecode and the heap size are billed.
 */
public class Throwable extends Object implements Serializable {
    static {
        // Shadow classes MUST be loaded during bootstrap phase.
        IInstrumentation.attachedThreadInstrumentation.get().bootstrapOnly();
    }

    private String message;
    private Throwable cause;

    public Throwable() {
        this((String)null, (Throwable)null);
    }

    public Throwable(String message) {
        this(message, null);
    }

    public Throwable(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public Throwable(Throwable cause) {
        this.message = (cause == null ? null : cause.avm_toString());
        this.cause = cause;
    }

    // Deserializer support.
    public Throwable(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
    }

    public String avm_getMessage() {
        lazyLoad();
        return this.message;
    }

    public String avm_getLocalizedMessage() {
        lazyLoad();
        return this.message;
    }

    public Throwable avm_getCause() {
        lazyLoad();
        return this.cause;
    }

    public Throwable avm_initCause(Throwable cause) {
        lazyLoad();
        this.cause = cause;
        return this;
    }

    public String avm_toString() {
        lazyLoad();
        String s = new String(ClassNameExtractor.getOriginalClassName(getClass().getName()));
        return (this.message != null) ? new String(s + ": " + this.message) : s;
    }

    // TODO:  Determine if we should throw/fail when something calls a method which doesn't make sense in this environment.
    // Otherwise, these cases are commented-out since some of them would require types which can't be instantiated and this
    // will cause these to fail in a not silent way.
//    public void avm_printStackTrace() {
//    }

//    public void avm_printStackTrace(PrintStream s) {
//    }

//    private void avm_printStackTrace(PrintStreamOrWriter s) {
//    }

//    private void avm_printEnclosedStackTrace(PrintStreamOrWriter s,
//                                         StackTraceElement[] enclosingTrace,
//                                         String caption,
//                                         String prefix,
//                                         Set<Throwable> dejaVu) {
//    }

//    public void avm_printStackTrace(PrintWriter s) {
//    }

    public Throwable avm_fillInStackTrace() {
        // We don't expose stack traces.
        return this;
    }

    // TODO:  Can't implement until we wrap StackTraceElement.
//    public StackTraceElement[] avm_getStackTrace() {
//    }

//    public void avm_setStackTrace(StackTraceElement[] stackTrace) {
//    }

    //=======================================================
    // Methods below are used by runtime and test code only!
    //========================================================

    @Override
    public java.lang.String toString() {
        lazyLoad();
        return getClass().getName() + ": " + this.message;
    }
}
