/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright 2001-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License. You can obtain
 *  a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 *  or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  Sun designates this particular file as subject to the "Classpath" exception
 *  as provided by Sun in the GPL Version 2 section of the License file that
 *  accompanied this code.  If applicable, add the following below the License
 *  Header, with the fields enclosed by brackets [] replaced by your own
 *  identifying information: "Portions Copyrighted [year]
 *  [name of copyright owner]"
 * 
 *  Contributor(s):
 * 
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package org.glassfish.gmbal.typelib;

import org.glassfish.gmbal.generic.Algorithms;
import org.glassfish.gmbal.generic.Display;
import org.glassfish.gmbal.generic.DprintUtil;
import org.glassfish.gmbal.generic.Printer;
import org.glassfish.gmbal.generic.UnaryFunction;
import org.glassfish.gmbal.generic.Pair;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import java.lang.reflect.Type ;
import java.lang.reflect.GenericArrayType ;
import java.lang.reflect.Method;
import java.lang.reflect.WildcardType ;
import java.lang.reflect.ParameterizedType ;
import java.lang.reflect.TypeVariable ;
import java.util.IdentityHashMap;
import java.util.WeakHashMap;

/**
 *
 * @author ken
 */
public class TypeEvaluator {
    private static boolean DEBUG = false ;
    private static boolean DEBUG_EVALUATE = false ;

    public static class EvalMapKey extends Pair<Class<?>,List<EvaluatedType>> {
        public EvalMapKey( Class<?> cls, List<EvaluatedType> decls ) {
            super( cls, decls ) ;
        }
    }

    // Cache of representations of classes with bound type variables.
    // A class may be in many EvalMapKeys with different tvar bindings.
    // XXX EvaluatedClassDeclaration strongly references Class!
    private static Map<EvalMapKey,EvaluatedClassDeclaration> evalClassMap =
        new WeakHashMap<EvalMapKey,EvaluatedClassDeclaration>() ;

    /** Given any generic java type, evaluate all of its type bounds and
     * return an evaluated type.
     * 
     * @param jtype The java type to evaluate
     * @return The evaluated type
     */
    public static synchronized EvaluatedType getEvaluatedType( Type jtype ) {
        TypeEvaluationVisitor visitor = new TypeEvaluationVisitor() ;
        EvaluatedType etype = visitor.evaluateType( jtype ) ;
        return etype ;
    }
    
    private static class TypeEvaluationVisitor  {
        private final Display<String,EvaluatedType> display ;

        private final DprintUtil dputil ;
        private final Map<Class<?>,EvaluatedClassDeclaration> partialDefinitions ;
        
        public TypeEvaluationVisitor( ) {
            display = new Display<String,EvaluatedType>() ;

            if (DEBUG||DEBUG_EVALUATE) {
                dputil = new DprintUtil( this.getClass() ) ;
            } else {
                dputil = null ;
            }

            partialDefinitions = 
                new HashMap<Class<?>,EvaluatedClassDeclaration>() ;
        }
        
        private EvaluatedType lookup( TypeVariable tvar ) {
            if (DEBUG) {
                dputil.enter( "lookup", "tvar=", tvar ) ;
            }
            try {
                EvaluatedType result = display.lookup( tvar.getName() ) ;
                if (result == null) {
                    if (DEBUG) {
                        dputil.info( "tvar not found in display" ) ;
                    }
                    Type[] bounds = tvar.getBounds() ;
                    if (bounds.length > 0) {
                        // XXX We need to create a union of the upper bounds.
                        // For now, only support a single upper bound.
                        if (bounds.length > 1) {
                            throw new UnsupportedOperationException(
                                "Not supported" ) ;
                        }

                        result = evaluateType( bounds[0] ) ;
                    } else {
                        result = EvaluatedType.EOBJECT ; 
                    }
                }
                
                if (DEBUG) {
                    dputil.info( "result=" + result ) ;
                }
                return result ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }

	public EvaluatedType evaluateType( Object type ) {
            if (DEBUG||DEBUG_EVALUATE) {
                dputil.enter( "evaluateType", "type=", type ) ;
            }
            
            try {
                if (type == null) {
                    return null ;
                } else if (type instanceof Class) {
                    Class cls = (Class)type ;
                    return visitClassDeclaration( cls ) ;
                } else if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)type ;
                    return visitParameterizedType( pt ) ;
                } else if (type instanceof TypeVariable) {
                    TypeVariable tvar = (TypeVariable)type ;
                    return visitTypeVariable( tvar ) ;
                } else if (type instanceof GenericArrayType) {
                    GenericArrayType gat = (GenericArrayType)type ;
                    return visitGenericArrayType( gat ) ;
                } else if (type instanceof WildcardType) {
                    WildcardType wt = (WildcardType)type ;
                    return visitWildcardType( wt ) ;
                } else if (type instanceof Method) {
                    Method method = (Method)type ;
                    return visitMethodDeclaration( method ) ;
                } else {
                    throw new IllegalArgumentException( "Unknown type???" + type ) ;
                }
            } finally {
                if (DEBUG||DEBUG_EVALUATE) {
                    dputil.exit() ;
                }
            }
	}
        
        private OrderedResult<String,EvaluatedType> getBindings( Class decl ) {
            OrderedResult<String,EvaluatedType> result = new
                OrderedResult<String,EvaluatedType>() ;

            for (TypeVariable tv : decl.getTypeParameters()) {
                EvaluatedType res = lookup( tv ) ;
                result.add( tv.getName(), res ) ;
            }

            return result ;
        }

        private EvaluatedType getCorrectDeclaration( OrderedResult<String,EvaluatedType> bindings,
            Class decl, EvaluatedClassDeclaration newDecl ) {
            
            EvalMapKey key = new EvalMapKey( decl, bindings.getList() ) ;
            EvaluatedType result = evalClassMap.get( key ) ;
            if (result == null) {
                evalClassMap.put( key, newDecl ) ;

                processClass( newDecl, bindings.getMap(), decl ) ;

                result = newDecl ;
            }

            return result ;
        }

        private EvaluatedType visitClassDeclaration( Class decl ) {
            if (DEBUG) {
                dputil.enter( "visitClassDeclaration", "decl=", decl ) ;
            }

            try {
                EvaluatedType result = partialDefinitions.get( decl ) ;
                if (result == null) {
                    // Create the classdecl as early as possible, because it
                    // may be needed on methods or type bounds.
                    EvaluatedClassDeclaration newDecl = DeclarationFactory.ecdecl(
                        decl.getModifiers(), decl.getName(), decl ) ;

                    partialDefinitions.put( decl, newDecl ) ;

                    try {
                        OrderedResult<String,EvaluatedType> bindings =
                            getBindings( decl ) ;

                        result = getCorrectDeclaration( bindings, decl, newDecl ) ;
                    } finally {
                        partialDefinitions.remove( decl ) ;
                    }
                } else {
                    if (DEBUG) {
                        dputil.info( "found result=" + result ) ;
                    }
                }

                return result ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }
        
        private List<Type> getInheritance( Class cls ) {
            if (DEBUG) {
                dputil.enter( "getInheritance", "cls=", cls ) ;
            }
            
            try {
                List<Type> result = new ArrayList<Type>() ;
                result.add( cls.getGenericSuperclass() ) ;
                result.addAll( Arrays.asList( cls.getGenericInterfaces() ) ) ;
                
                if (DEBUG) {
                    dputil.info( "result=" + result ) ;
                }
                
                return result ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }
        
        private void processClass( EvaluatedClassDeclaration newDecl,
            Map<String,EvaluatedType> bindings, Class decl ) {
            
            if (DEBUG) {
                dputil.enter( "processClass", "bindings=", bindings,
                    "decl=", decl ) ;
            }
            
            display.enterScope() ;
            
            display.bind( bindings ) ;
            
            try {
                List<EvaluatedClassDeclaration> inheritance = 
                    Algorithms.map( getInheritance( decl ),
                    new UnaryFunction<Type,EvaluatedClassDeclaration>() {
                        public EvaluatedClassDeclaration evaluate( Type pt ) {
                            return (EvaluatedClassDeclaration)evaluateType( pt ) ;
                        } } ) ;

                if (DEBUG) {
                    dputil.info( "inheritance=" + inheritance ) ;
                }
                        
                newDecl.inheritance( inheritance ) ;

                if (DEBUG) {
                    dputil.info( "newDecl=" + newDecl ) ;
                }

                List<EvaluatedMethodDeclaration> newMethods = Algorithms.map(
                    Arrays.asList( decl.getDeclaredMethods() ), 
                    new UnaryFunction<Method,EvaluatedMethodDeclaration>() {
                        public EvaluatedMethodDeclaration evaluate( 
                            Method md ) {
                            
                            return visitMethodDeclaration( md ) ;
                        } } ) ;

                newDecl.methods( newMethods ) ; 
            } finally {
                display.exitScope() ;
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }

        private EvaluatedMethodDeclaration visitMethodDeclaration( Method decl ) {
            if (DEBUG) {
                dputil.enter( "visitMethodDeclaration", "decl=", decl ) ;
            }
                
            try {
                List<EvaluatedType> eptypes = 
                    Algorithms.map( Arrays.asList( decl.getGenericParameterTypes() ),
                        new UnaryFunction<Type,EvaluatedType>() {
                            public EvaluatedType evaluate( Type type ) {
                                return evaluateType( type ) ;
                            } } ) ;
                            
                if (DEBUG) {
                    dputil.info( "eptypes=" + eptypes ) ;
                }

                // Convenience for the test: all processing is done on a method
                // named getThing, and this is where we need to debug, out of the
                // many hundreds of other method calls.
                if (decl.getName().equals( "getThing" )) {
                    if (DEBUG) {
                        dputil.info( "processing getThing method from test" ) ;
                    }
                }

                EvaluatedMethodDeclaration result = DeclarationFactory.emdecl( 
                    decl.getModifiers(), 
                    evaluateType( decl.getGenericReturnType() ),
                    decl.getName(), eptypes, decl ) ;
                
                if (DEBUG) {
                    dputil.info( "result=" + result ) ;
                }
                
                return result ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }

        private EvaluatedType visitTypeVariable( TypeVariable tvar ) {
            if (DEBUG) {
                dputil.enter( "visitTypeVariable" ) ;
            }
            
            try {
                return lookup( tvar ) ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }  

        private EvaluatedType visitGenericArrayType( GenericArrayType at ) {
            if (DEBUG) {
                dputil.enter( "visitTypeVariable" ) ;
            }
            
            try {
                return DeclarationFactory.egat(
                    evaluateType( at.getGenericComponentType() ) ) ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }             
        }

        private EvaluatedType visitWildcardType( WildcardType wt ) {
            if (DEBUG) {
                dputil.enter( "visitTypeVariable" ) ;
            }
            
            try {
                EvaluatedType result = null ;
                // ignore lower bounds
                // Only support 1 upper bound
                List<Type> ub = Arrays.asList( wt.getUpperBounds() ) ;
                if (ub.size() > 0) {
                    if (ub.size() > 1) {
                        throw new UnsupportedOperationException("Not supported");
                    }

                    result = evaluateType( ub.get(0) ) ;
                } else {
                    result = EvaluatedType.EOBJECT ;                 
                }

                return result ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }             
        }

        public static class OrderedResult<K,V> {
            private List<V> list = new ArrayList<V>() ;
            private Map<K,V> map = new HashMap<K,V>() ;
    
            public List<V> getList() { return list ; } 
            public Map<K,V> getMap() { return map ; }

            public void add( K key, V value ) {
                list.add( value ) ;
                map.put( key, value ) ;
            }
        }

        private OrderedResult<String,EvaluatedType> getBindings( ParameterizedType pt ) {
            OrderedResult<String,EvaluatedType> result = new
                OrderedResult<String,EvaluatedType>() ;

            Iterator<Type> types =
                Arrays.asList(pt.getActualTypeArguments()).iterator() ;
            Iterator<TypeVariable> tvars =
                Arrays.asList(((Class)pt.getRawType()).getTypeParameters()).iterator() ;

            while (types.hasNext() && tvars.hasNext()) {
                Type type = types.next() ;
                TypeVariable tvar = tvars.next() ;
                result.add( tvar.getName(), evaluateType( type ) ) ;
            }

            if (types.hasNext() != tvars.hasNext()) {
                throw new IllegalArgumentException(
                    "Type list and TypeVariable list are not the same length");
            }
             
            return result ;
        }
 
        private EvaluatedType visitParameterizedType( ParameterizedType pt ) {
            if (DEBUG) {
                dputil.enter( "visitParameterizedType", "pt=", pt ) ;
            }
            
            Class<?> decl = (Class<?>)pt.getRawType() ;

            try {
                EvaluatedType result = partialDefinitions.get( decl ) ;
                if (result == null) {
                    // Create the classdecl as early as possible, because it
                    // may be needed on methods or type bounds.
                    EvaluatedClassDeclaration newDecl = DeclarationFactory.ecdecl(
                        decl.getModifiers(), decl.getName(), decl ) ;

                    partialDefinitions.put( decl, newDecl ) ;

                    try {
                        OrderedResult<String,EvaluatedType> bindings =
                            getBindings( pt ) ;

                        result = getCorrectDeclaration( bindings, decl, newDecl ) ;
                    } finally {
                        partialDefinitions.remove( decl ) ;
                    }
                }

                return result ;
            } finally {
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
        }
    }

    public static void displayEvaluatedType( PrintStream ps, EvaluatedType et ) {
        Printer printer = new Printer( ps ) ;
        Visitor<Object> visitor = new VisitorDisplayImpl( printer ) ;
        et.accept( visitor ) ;
    }

    public static class IdentitySetImpl {
        private IdentityHashMap<Object,Object> map = 
            new IdentityHashMap<Object,Object>() ;

        public boolean contains( Object arg ) {
            return map.keySet().contains( arg ) ;
        }

        public void add( Object arg ) {
            map.put( arg, null ) ;
        }
    }

    public static class VisitorDisplayImpl implements Visitor<Object> {
        private Printer pr ;
        private IdentitySetImpl visited = new IdentitySetImpl() ;

        public VisitorDisplayImpl( Printer printer ) {
            this.pr = printer ;
        }

        public Object visitEvaluatedType( EvaluatedType et ) {
            return null ;
        }
        
        public Object visitEvaluatedArrayType( EvaluatedArrayType eat ) {
            pr.nl().p( "EvaluatedArrayType" ).in() ;
            pr.nl().p( "componentType:" ).in() ;
            eat.componentType().accept( this ) ;
            pr.out().out() ;
            return null ;
        }
        
        public Object visitEvaluatedDeclaration( EvaluatedDeclaration ed ) {
            return null ;
        }
        
        public Object visitEvaluatedClassDeclaration( EvaluatedClassDeclaration ecd ) {
            pr.nl().p( "EvaluatedClassDeclaration" ).in() ;
            pr.nl().p( ecd.toString() ).in() ;
            for (EvaluatedMethodDeclaration emd : ecd.methods()) {
                pr.nl().p( "Method:" ).p( emd.toString() ) ;
            }
            pr.out().out() ;
            return null ;
        }
        
        public Object visitEvaluatedMethodDeclaration( EvaluatedMethodDeclaration emd ) {
            return null ;
        }
    }
}
