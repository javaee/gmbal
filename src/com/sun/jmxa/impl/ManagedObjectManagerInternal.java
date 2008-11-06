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

package com.sun.jmxa.impl ;

import com.sun.jmxa.generic.ClassAnalyzer;
import java.lang.reflect.Type ;

import java.lang.reflect.AnnotatedElement ;

import com.sun.jmxa.ManagedObjectManager ;
import com.sun.jmxa.generic.Pair ;
import com.sun.jmxa.InheritedAttribute ;

import com.sun.jmxa.generic.DprintUtil;
import com.sun.jmxa.generic.FacetAccessor;
import com.sun.jmxa.generic.Predicate;
import java.lang.annotation.Annotation;
import java.util.List;

public interface ManagedObjectManagerInternal extends ManagedObjectManager {
    /** Construct or lookup the TypeConverter for the given type.
     * 
     * @param type The type for which we need a TypeConverter.
     * @return The type converter.
     */
    TypeConverter getTypeConverter( Type type ) ;
    
    String getDescription( AnnotatedElement element ) ;
        
    <T extends Annotation> T getAnnotation( AnnotatedElement element,
        Class<T> type ) ;

    /** Find the superclass or superinterface of cls (which may be cls itself) 
     * that has the given annotationClass as an annotation.  If the annotated 
     * Class has an IncludeSubclass annotation, add those classes into the 
     * ClassAnalyzer for the annotated class.
     * @param cls The class for which we need a ClassAnalyzer.
     * @param annotationClass The annotation that must be present on cls or
     * a superclass or superinterface.
     * @return A Pair of the parent class of cls, and the ClassAnalyzer.
     */
    Pair<Class<?>,ClassAnalyzer> getClassAnalyzer( Class<?> cls,
        Class<? extends Annotation> annotationClass ) ;
    
    /** Get the inherited attributes from the ClassAnalyzer.
     * @param ca The ClassAnalyzer to check for InheritedAttribute(s).
     * @return The inherited attributes.
     */
    List<InheritedAttribute> getInheritedAttributes( ClassAnalyzer ca ) ;
    
    Predicate<AnnotatedElement> forAnnotation( 
        final Class<? extends Annotation> annotation ) ;
    
    FacetAccessor getFacetAccessor( Object obj ) ;
    
    MBeanImpl constructMBean( Object obj, String name ) ;
    
    boolean registrationDebug() ;
    
    boolean registrationFineDebug() ;
    
    boolean runtimeDebug() ;
}