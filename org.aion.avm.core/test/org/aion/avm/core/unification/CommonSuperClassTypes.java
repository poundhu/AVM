package org.aion.avm.core.unification;


/**
 * issue-362: Defines the types used by the CommonSuperClassTest (specifically, the generated CommonSuperClassTarget class).
 */
public class CommonSuperClassTypes {
    public static interface RootA {
        String getRootA();
    }

    public static interface RootB {
        String getRootB();
    }

    public static interface SubRootA1 extends RootA {}

    public static interface SubRootA2 extends RootA {}

    public static interface SubSubRootA1 extends SubRootA1 {}

    public static interface ChildA extends RootA, RootB {
        String getChildA();
    }

    public static interface ChildB extends RootA, RootB {
        String getChildB();
    }

    public static enum EnumA1 implements RootA, RootB {
        ;

        @Override
        public String getRootA() {
            return null;
        }
        @Override
        public String getRootB() {
            return null;
        }
    }

    public static enum EnumA2 implements RootA {
        ;

        @Override
        public String getRootA() {
            return null;
        }
    }

    public static enum EnumB implements RootA, RootB {
        ;

        @Override
        public String getRootA() {
            return null;
        }
        @Override
        public String getRootB() {
            return null;
        }
    }

    public static abstract class ClassRoot {
        public abstract String getClassRoot();
    }

    public static abstract class ClassChild extends ClassRoot {
    }
}
