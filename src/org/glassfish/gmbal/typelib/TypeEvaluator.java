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

/**
 *
 * @author ken
 */
public class TypeEvaluator {
    private static boolean DEBUG = true ;
    
    private static final Map<Type,EvaluatedType> evalClassMap =
        new HashMap<Type,EvaluatedType>() ;
    
    /** Given any generic java type, evaluated all of its type bounds and
     * return an evaluated type.
     * 
     * @param jtype The java type to evaluate
     * @return The evaluated type
     */
    public static synchronized EvaluatedType getEvaluatedType(
        Type jtype ) {
        
        EvaluatedType etype = evalClassMap.get( jtype ) ;
        if (etype == null) {
            Display<String,EvaluatedType> display = 
                new Display<String,EvaluatedType>() ;
            TypeEvaluationVisitor visitor = 
                new TypeEvaluationVisitor( display ) ;
            etype = visitor.evaluate( jtype ) ;
            evalClassMap.put( jtype, etype ) ;
        }
        
        return etype ;
    }
    
    private static class TypeEvaluationVisitor  {
        private Display<String,EvaluatedType> display  ;
        private DprintUtil dputil ;
        
        public TypeEvaluationVisitor( Display<String,EvaluatedType> display ) {
            this.display = display ;
            if (DEBUG) {
                dputil = new DprintUtil( this.getClass() ) ;
            }
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

                        result = evaluate( bounds[0] ) ;
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

	public EvaluatedType evaluate( Object type ) {
            if (DEBUG) {
                dputil.enter( "evaluate", "type=", type ) ;
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
                if (DEBUG) {
                    dputil.exit() ;
                }
            }
	}
        
        private EvaluatedType visitClassDeclaration( Class decl ) {
            if (DEBUG) {
                dputil.enter( "visitClassDeclaration", "decl=", decl ) ;
            }

            try {
                EvaluatedType result = evalClassMap.get( decl ) ;
                if (result != null) {
                    if (DEBUG) {
                        dputil.info( "found result=" + result ) ;
                    }
                    return result ;
                }

                if (DEBUG) {
                    dputil.info( "Evaluating bindings" ) ;
                }
                
                Map<String,EvaluatedType> bindings = 
                    new HashMap<String,EvaluatedType>() ;

                for (TypeVariable tv : decl.getTypeParameters()) {
                    bindings.put( tv.getName(), lookup( tv ) ) ;
                }

                return processClass( bindings, decl ) ;
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
        
        private EvaluatedType processClass( 
            Map<String,EvaluatedType> bindings,
                Class decl ) {
            
            if (DEBUG) {
                dputil.enter( "processClass", "bindings=", bindings,
                    "decl=", decl ) ;
            }
            
            display.enterScope() ;
            
            display.bind( bindings ) ;
            
            EvaluatedClassDeclaration newDecl ;
            
            try {
                List<EvaluatedClassDeclaration> inheritance = 
                    Algorithms.map( getInheritance( decl ),
                    new UnaryFunction<Type,EvaluatedClassDeclaration>() {
                        public EvaluatedClassDeclaration evaluate( Type pt ) {
                            return (EvaluatedClassDeclaration)TypeEvaluator
                                .getEvaluatedType( pt ) ;
                        } } ) ;

                if (DEBUG) {
                    dputil.info( "inheritance=" + inheritance ) ;
                }
                        
                newDecl = DeclarationFactory.ecdecl(
                    decl.getModifiers(), decl.getName(), inheritance, 
                    null, decl ) ;
                
                if (DEBUG) {
                    dputil.info( "newDecl=" + newDecl ) ;
                }

                // Must be put back early in case of recursive reference, e.g.
                // a method that returns the class in which it is contained.
                evalClassMap.put( decl, newDecl ) ;

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
                dputil.exit() ;
            }
            
            return newDecl ;
        }

        private EvaluatedMethodDeclaration visitMethodDeclaration( Method decl ) {
            if (DEBUG) {
                dputil.enter( "visitMethodDeclaration", "decl=", decl ) ;
            }
                
            try {
                StringBuilder sb = new StringBuilder() ;
                sb.append( decl.getName() ) ;

                List<EvaluatedType> eptypes = 
                    Algorithms.map( Arrays.asList( decl.getGenericParameterTypes() ),
                        new UnaryFunction<Type,EvaluatedType>() {
                            public EvaluatedType evaluate( Type type ) {
                                return TypeEvaluator.getEvaluatedType( type ) ;
                            } } ) ;
                            
                if (DEBUG) {
                    dputil.info( "eptypes=" + eptypes ) ;
                }

                if (eptypes.size() > 0) {
                    EvaluatedTypeBase.handleList( sb, "<", eptypes, ">" ) ;
                }

                EvaluatedMethodDeclaration result = DeclarationFactory.emdecl( 
                    decl.getModifiers(), 
                    getEvaluatedType( decl.getGenericReturnType() ), 
                    sb.toString(),
                    eptypes, decl ) ;
                
                if (DEBUG) {
                    dputil.info( "result=" + result ) ;
                }
                
                return result ;
            } finally {
                dputil.exit() ;
            }
        }

        private EvaluatedType visitTypeVariable( TypeVariable tvar ) {
            if (DEBUG) {
                dputil.enter( "visitTypeVariable" ) ;
            }
            
            try {
                return lookup( tvar ) ;
            } finally {
                dputil.exit() ;
            }
        }  

        private EvaluatedType visitGenericArrayType( GenericArrayType at ) {
            if (DEBUG) {
                dputil.enter( "visitTypeVariable" ) ;
            }
            
            try {
                return DeclarationFactory.egat(
                    getEvaluatedType( at ) ) ;
            } finally {
                dputil.exit() ;
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

                    result = getEvaluatedType( ub.get(0) ) ;
                } else {
                    result = EvaluatedType.EOBJECT ;                 
                }

                return result ;
            } finally {
                dputil.exit() ;
            }             
        }
 
        private EvaluatedType visitParameterizedType( ParameterizedType pt ) {
            if (DEBUG) {
                dputil.enter( "visitParameterizedType", "pt=", pt ) ;
            }
            
            try {
                EvaluatedType result = evalClassMap.get( pt.getRawType() ) ;
                if (result != null) {
                    return result ;
                }

                Map<String,EvaluatedType> bindings = 
                    new HashMap<String,EvaluatedType>() ;

                Iterator<Type> types = 
                    Arrays.asList(pt.getActualTypeArguments()).iterator() ;
                Iterator<TypeVariable> tvars = 
                    Arrays.asList(((Class)pt.getRawType()).getTypeParameters()).iterator() ;

                while (types.hasNext() && tvars.hasNext()) {
                    Type type = types.next() ;
                    TypeVariable tvar = tvars.next() ;
                    bindings.put( tvar.getName(), getEvaluatedType( tvar ) ) ;
                }

                if (DEBUG) {
                    dputil.info( "bindings=" + bindings ) ;
                }
                
                if (types.hasNext() != tvars.hasNext()) {
                    throw new IllegalArgumentException(
                        "Type list and TypeVariable list are not the same length");
                }

                return processClass( bindings, (Class)pt.getRawType() ) ;   
            } finally {
                dputil.exit() ;
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
