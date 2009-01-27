/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific 
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/LICENSE.TXT.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 * 
 */ 
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
