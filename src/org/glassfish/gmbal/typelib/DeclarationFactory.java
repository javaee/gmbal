/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2009 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.gmbal.typelib ;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import org.glassfish.gmbal.generic.DprintUtil ;
import org.glassfish.gmbal.generic.DumpToString;

/** Utility class used to construct instances of the typelib interfaces directly from
 * factory methods, rather than from actual Java classes.  This is useful for testing:
 * we can construct the expected result, then compare with the actual result.
 */
public class DeclarationFactory {
    private static boolean DEBUG = false ;
    private static DprintUtil dputil = new DprintUtil( DeclarationFactory.class ) ;

    private static final Map<EvaluatedType,EvaluatedArrayType> arrayMap =
        new HashMap<EvaluatedType,EvaluatedArrayType>() ;

    private static final Map<String,EvaluatedClassDeclaration> simpleClassMap =
        new HashMap<String,EvaluatedClassDeclaration>() ;

    /*
    private static final Map<Pair<String,List<EvaluatedType>>,
        EvaluatedMethodDeclaration> methodMap =
        new HashMap<Pair<String,List<EvaluatedType>>,EvaluatedMethodDeclaration>() ;
    */

    private DeclarationFactory() {}

    public static synchronized EvaluatedArrayType egat( final EvaluatedType compType ) {
        EvaluatedArrayType result = arrayMap.get( compType ) ;
        if (result == null) {
            if (DEBUG) {
                dputil.enter( "egat", "compType", compType ) ;
            }

            try {
                result = new EvaluatedArrayTypeImpl( compType ) ;
                arrayMap.put( compType, result ) ;
            } finally {
                if (DEBUG) {
                    dputil.exit( result ) ;
                }
            }
        }

        return result ;
    }

    public static synchronized EvaluatedClassDeclaration ecdecl( final int modifiers,
        final String name, final List<EvaluatedClassDeclaration> inheritance,
        final List<EvaluatedMethodDeclaration> methods, final Class cls ) {

        EvaluatedClassDeclaration result = null ;
        if (cls.getTypeParameters().length == 0) {
            // Try the cache first
            result = simpleClassMap.get( name ) ;
        }

        if (result == null) {
            if (DEBUG) {
                dputil.enter( "ecdecl", "name", name ) ;
            }

            try {
                result = new EvaluatedClassDeclarationImpl( modifiers, name,
                    inheritance, methods, cls ) ;
                if (result.simpleClass()) {
                    simpleClassMap.put( name, result ) ;
                }
            } finally {
                if (DEBUG) {
                    dputil.exit( result ) ;
                }
            }
        }

        return result ;
    }

    public static synchronized EvaluatedMethodDeclaration emdecl( 
        final EvaluatedClassDeclaration ecdecl, final int modifiers,
        final EvaluatedType rtype, final String name,
        final List<EvaluatedType> ptypes, final Method method ) {

        if (DEBUG) {
            dputil.enter( "emdecl", "name", name, "ptypes", ptypes ) ;
        }

        EvaluatedMethodDeclaration result = null ;

        try {
            result = new EvaluatedMethodDeclarationImpl( ecdecl, modifiers,
                rtype, name, ptypes, method ) ;
        } finally {
            if (DEBUG) {
                dputil.exit( result ) ;
            }
        }

        return result ;
    }
    
    public static EvaluatedClassDeclaration ecdecl( final int modifiers,
        final String name, final Class cls ) {

        return ecdecl( modifiers, name, 
            new ArrayList<EvaluatedClassDeclaration>(0),
            new ArrayList<EvaluatedMethodDeclaration>(0), cls ) ;
    }

    private static class EvaluatedArrayTypeImpl extends EvaluatedArrayTypeBase {
        private EvaluatedType compType ;

        public EvaluatedArrayTypeImpl( final EvaluatedType compType ) {
            this.compType = compType ;
        }

        public EvaluatedType componentType() { return compType ; }
    }

    private static class EvaluatedMethodDeclarationImpl extends EvaluatedMethodDeclarationBase {
        private final EvaluatedClassDeclaration container ;
        private final int modifiers ;
        private final EvaluatedType rtype ;
        private final String name ;
        private final List<EvaluatedType> ptypes ;
        @DumpToString
        private final Method method ;

        public EvaluatedMethodDeclarationImpl( EvaluatedClassDeclaration cdecl,
            final int modifiers, final EvaluatedType rtype,
            final String name, final List<EvaluatedType> ptypes,
            final Method method ) {

            this.container = cdecl ;
            this.modifiers = modifiers ;
            this.rtype = rtype ; 
            this.name = name ;
            this.ptypes = ptypes ;
            this.method = method ;
        }

        public String name() { return name ; }
        
        public int modifiers() { return modifiers ; }
        
        public List<EvaluatedType> parameterTypes() { return ptypes ; }

        public EvaluatedType returnType() { return rtype ; }
        
        public EvaluatedClassDeclaration containingClass() { return container ; }
        
        public java.lang.reflect.Method method() { return method ; }

        public <T extends Annotation> T annotation(Class<T> annotationType) {
            if (method == null) {
                throw new UnsupportedOperationException(
                    "Not supported in constructed ClassDeclaration.");
            } else {
                return method.getAnnotation( annotationType ) ;
            }
        }

        public List<Annotation> annotations() {
            if (method == null) {
                throw new UnsupportedOperationException(
                    "Not supported in constructed ClassDeclaration.");
            } else {
                return Arrays.asList( method.getAnnotations() ) ;
            }
        }

        public AnnotatedElement element() { return method ; }
    }

    private static class EvaluatedClassDeclarationImpl extends EvaluatedClassDeclarationBase {
        private final int modifiers ;
        private final String name ;
        private List<EvaluatedClassDeclaration> inheritance ;
        private List<EvaluatedMethodDeclaration> methods ;
        @DumpToString
        private final Class cls ;
        private List<EvaluatedType> instantiations = 
            new ArrayList<EvaluatedType>(0) ;
        // set to TRUE if cls is a simple class with no type parameters
        // This is important because there can be only one form of such classes,
        // so they are cacheable.
        private boolean simpleClass ;
        private boolean frozen ;

        public EvaluatedClassDeclarationImpl( final int modifiers,
            final String name, final List<EvaluatedClassDeclaration> inheritance,
            final List<EvaluatedMethodDeclaration> methods, final Class cls ) {

            this.modifiers = modifiers ;
            this.name = name ;
            this.inheritance = inheritance ;
            this.methods = methods ;
            this.cls = cls ;
            this.simpleClass = cls.getTypeParameters().length == 0 ;
            this.frozen = false ;
        }

        public void freeze() {
            frozen = true ;
        }

        public boolean simpleClass() {
            return simpleClass ;
        }

        public <T extends Annotation> T annotation(Class<T> annotationType) {
            if (cls == null) {
                throw new UnsupportedOperationException(
                    "Not supported in constructed ClassDeclaration.");
            } else {
                return (T) cls.getAnnotation( annotationType ) ;
            }
        }

        public List<Annotation> annotations() {
            if (cls == null) {
                throw new UnsupportedOperationException(
                    "Not supported in constructed ClassDeclaration.");
            } else {
                return Arrays.asList( cls.getAnnotations() ) ;
            }
        }
        
        public String name() { return name ; }

        public int modifiers() { return modifiers ; }

        public Class cls() { return cls ; }

        public List<EvaluatedMethodDeclaration> methods() { return methods ; }

        public List<EvaluatedClassDeclaration> inheritance() { return inheritance ; } ;

        private void checkFrozen() {
            if (frozen) {
                throw new IllegalStateException(
                    "Cannot modify frozen instance for " + this ) ;
            }
        }
        public void methods(List<EvaluatedMethodDeclaration> meths) { 
            checkFrozen() ;
            methods = meths ;
        }

        public void inheritance(List<EvaluatedClassDeclaration> inh) { 
            checkFrozen() ;
            inheritance = inh ;
        }

        public AnnotatedElement element() { return cls ; }

        public List<EvaluatedType> instantiations() { return this.instantiations ; }

        public void instantiations(List<EvaluatedType> arg) {
            checkFrozen() ;
            // XXX Should we add more consistency checking?
            if (simpleClass) {
                throw new IllegalStateException(
                    "Cannot add instantiations to a class with no type args" ) ;
            } else {
                this.instantiations = arg ;
            }
        }
    }
}
