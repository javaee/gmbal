package org.glassfish.gmbal ;

import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestGenericReflection {
    // XXX finish this test or delete it!

    static class Test<S,T> {
        void m1( List<Map<S,T>> arg ) {
        }

        // @ManagedAttribute
        List<List<S>> m2() {
            return null ;
        }

        <V> List<S> m3( List<V> arg, Class<V> type ) {
            return null ;
        }
    }
    
    static abstract class Test2<A,B,C> extends Test<A,B> implements List<C> {
        
    }

    static class Test3<C> {
        // @ManagedAttribute
        Test<C,String> m4() {
            return null ;
        }
    }

    // @ManagedData
    static class Test4 {
        // @ManagedAttribute
        Test3<Date> m5() {
            return null ;
        }
    }

    private static void m( String arg ) {
        System.out.println( arg ) ;
    }

    private static void m( Object obj ) {
        m( obj.toString() ) ;
    }

    private static String m( Object[] arg ) {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "[" ) ;
        boolean first = true ;
        for (Object obj : arg) {
            if (first) {
                first = false ;
            } else {
                sb.append( ' ' ) ;
            }

            sb.append( obj.toString() ) ;
        }
        sb.append( "]" ) ;
        return sb.toString() ;
    }

    private static String prt( Type[] arg ) {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "[" ) ;
        boolean first = true ;
        for (Type t : arg) {
            if (first) {
                first = false ;
            } else {
                sb.append( ' ' ) ;
            }

            sb.append( prt(t) ) ;
        }
        sb.append( "]" ) ;
        return sb.toString() ;
    }

    private static String prt( Type type ) {
        if (type == null) {
            return "null" ;
        } else if (type instanceof Class) {
            return "Class(" + ((Class)type).getName() + ")" ;
        } else if (type instanceof GenericArrayType) {
            return "GenericArray(compType=" + 
                prt( ((GenericArrayType)type).getGenericComponentType() ) 
                + ")" ;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type ;
            return "ParameterizedType(" 
                + "typeArgs=" + prt( pt.getActualTypeArguments() ) 
                // mostly not interesting, except for nested classes? + ",ownerType=" + prt( pt.getOwnerType() ) 
                + ",rawType=" + prt( pt.getRawType() ) + ")" ;
        } else if (type instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable)type ;
            return "TypeVariable(" 
                + "bounds=" + prt( tv.getBounds() ) 
                + ",genericDecl=[" + tv.getGenericDeclaration().toString() + "]"
                + ",name=" + tv.getName() + ")" ;
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType)type ;
            return "WildcardType("
                + "lowerBounds=" + prt( wt.getLowerBounds() ) 
                + ",upperBounds=" + prt( wt.getUpperBounds() ) + ")" ;
        } else {
            return "UknownType???(" + type.toString()  + ")" ;
        }
    }

    private static String prt( GenericDeclaration decl ) {
        return "GenericDeclaration(" 
            + decl.toString() + ":" 
            + prt( decl.getTypeParameters() ) + ")" ;
    }

    private static void displayClass( Class cls ) {
        m( "Class " + cls.getName() ) ;    
        for (Method meth : cls.getDeclaredMethods() ) {
            m( "\tMethod " + prt( meth ) ) ;
            m( "\t\tType parameters" ) ;
            m( "\t\t\t" + prt( meth.getTypeParameters() ) ) ;
            m( "\t\tParameter types" ) ;
            m( "\t\t\t" + prt( meth.getGenericParameterTypes() ) ) ;
            m( "\t\tReturn type" ) ;
            m( "\t\t\t" + prt( meth.getGenericReturnType() ) ) ;
        }
    }
    
    private static void testClass( Class<?> cls ) {
        EvaluatedType et = TypeEvaluator.getEvaluatedType( cls ) ;
        
    }
    
    public void test4() {
        testClass( Test4.class ) ;
    }

    public static void main( String[] args ) throws Exception {
        TestGenericReflection test = new TestGenericReflection() ;
        test.test4() ;
    }
}
