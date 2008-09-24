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

import java.util.List ;
import java.util.ArrayList ;


import java.lang.annotation.Annotation ;

import com.sun.jmxa.generic.Algorithms ;
import com.sun.jmxa.generic.Pair ;

import com.sun.jmxa.InheritedAttribute ;
import com.sun.jmxa.InheritedAttributes ;
import com.sun.jmxa.IncludeSubclass ;
    
public class AnnotationUtil {
    private AnnotationUtil() {}

    /* Find the superclass or superinterface of cls (which may be cls itself) 
     * that has the given annotationClass as an annotation.  If the annotated 
     * Class has an IncludeSubclass annotation, add those classes into the 
     * ClassAnalyzer for the annotated class.
     */
    public static Pair<Class<?>,ClassAnalyzer> getClassAnalyzer( 
        final Class<?> cls, 
        final Class<? extends Annotation> annotationClass ) {

        ClassAnalyzer ca = new ClassAnalyzer( cls ) ;
        /* This is the versions that expects EXACTLY ONE annotation
        Class<?> annotatedClass = Algorithms.getOne( 
            ca.findClasses( ca.forAnnotation( annotationClass ) ),
            "No " + annotationClass.getName() + " annotation found",
            "More than one " + annotationClass.getName() 
            + " annotation found" ) ;
        */
        
        final Class<?> annotatedClass = Algorithms.getFirst( 
            ca.findClasses( ca.forAnnotation( annotationClass ) ),
            "No " + annotationClass.getName() + " annotation found" ) ;
        
        final List<Class<?>> classes = new ArrayList<Class<?>>() ;
        classes.add( annotatedClass ) ;
	final IncludeSubclass incsub = annotatedClass.getAnnotation( 
            IncludeSubclass.class ) ;
	if (incsub != null) {
            for (Class<?> klass : incsub.cls()) {
                classes.add( klass ) ;
            }
	}

        if (classes.size() > 1) {
            ca = new ClassAnalyzer( classes ) ;
        }
        
        return new Pair<Class<?>,ClassAnalyzer>( annotatedClass, ca ) ;
    }

    public static InheritedAttribute[] getInheritedAttributes( Class<?> cls ) {
	// Check for @InheritedAttribute(s) annotation.  
	// Find methods for these attributes in superclasses. 
	final InheritedAttribute ia = cls.getAnnotation( 
            InheritedAttribute.class ) ;
	final InheritedAttributes ias = cls.getAnnotation( 
            InheritedAttributes.class ) ;
	if ((ia != null) && (ias != null)) {
	    throw new IllegalArgumentException( 
		"Only one of the annotations InheritedAttribute or "
		+ "InheritedAttributes may appear on a class" ) ;
        }

	InheritedAttribute[] iaa = null ;
	if (ia != null)	{
	    iaa = new InheritedAttribute[] { ia } ;
        } else if (ias != null) {
	    iaa = ias.attributes() ;
        }

	return iaa ;
    }
}
