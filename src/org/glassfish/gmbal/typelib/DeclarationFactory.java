/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2007 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
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
 */
package org.glassfish.gmbal.typelib ;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Utility class used to construct instances of the typelib interfaces directly from
 * factory methods, rather than from actual Java classes.  This is useful for testing:
 * we can construct the expected result, then compare with the actual result.
 */
public class DeclarationFactory {
    
    private DeclarationFactory() {}

    public static EvaluatedArrayType egat( final EvaluatedType compType ) {
        return new EvaluatedArrayTypeBase() {
            public EvaluatedType componentType() {
                return compType ;
            }
        } ;
    }
    
    public static EvaluatedMethodDeclaration emdecl( 
        final int modifiers, final EvaluatedType rtype, final String name,
        final List<EvaluatedType> ptypes, final Method method ) {
        
        return new EvaluatedMethodDeclarationBase() {
            private EvaluatedClassDeclaration container ;
            
            public String name() {
                return name ;
            }
            
            public int modifiers() {
                return modifiers ;
            }
            
            public List<EvaluatedType> parameterTypes() {
                return ptypes ;
            }

            public EvaluatedType returnType() {
                return rtype ;
            }
            
            public EvaluatedClassDeclaration containingClass() {
                return container ;
            }
            
            @Override
            public void containingClass( EvaluatedClassDeclaration cdecl ) {
                container = cdecl ;
            }
            
            public java.lang.reflect.Method method() {
                return method ;
            }

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

            public AnnotatedElement element() {
                return method ;
            }
        } ;
    }
    
    public static EvaluatedClassDeclaration ecdecl( final int modifiers,
        final String name, final Class cls ) {

        return ecdecl( modifiers, name, 
            new ArrayList<EvaluatedClassDeclaration>(),
            new ArrayList<EvaluatedMethodDeclaration>(), cls ) ;
    }

    public static EvaluatedClassDeclaration ecdecl( final int modifiers,
        final String name, final List<EvaluatedClassDeclaration> inheritance,
        final List<EvaluatedMethodDeclaration> methods, final Class cls ) {
    
        return new EvaluatedClassDeclarationBase() {
            private List<EvaluatedMethodDeclaration> myMethods =
                methods ;
            private List<EvaluatedClassDeclaration> myInheritance = 
                inheritance ;
            private List<EvaluatedType> instantiations = null ;
            
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
            
            public String name() {
                return name ;
            }

            public int modifiers() {
                return modifiers ;
            }

            public Class cls() {
                return cls ;
            }

            public List<EvaluatedMethodDeclaration> methods() {
                return myMethods ;
            }

            public List<EvaluatedClassDeclaration> inheritance() {
                return myInheritance ;
            } ;

            public void methods(List<EvaluatedMethodDeclaration> meths) {
                myMethods = meths ;
            }

            public void inheritance(List<EvaluatedClassDeclaration> inh) {
                myInheritance = inh ;
            }

            public AnnotatedElement element() {
                return cls ;
            }

            public List<EvaluatedType> instantiations() {
                return this.instantiations ;
            }

            public void instantiations(List<EvaluatedType> arg) {
                this.instantiations = arg ;
            }
        } ;
    }
}

