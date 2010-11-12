/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *  
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *  
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *  
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *  
 *  Contributor(s):
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

package org.glassfish.gmbal.typelib ;

import java.util.Collections ;
import java.util.List ;
import java.util.ArrayList ;

import org.glassfish.gmbal.generic.Predicate ;
import org.glassfish.gmbal.generic.Graph ;

    
/** Analyzes class inheritance hiearchy and provides methods for searching for
 * classes and methods.
 */
public class EvaluatedClassAnalyzer {
    // General purpose class analyzer
    //
    // The basic problem is to determine for any class its linearized inheritance
    // sequence.  This is an old problem in OOP.  For my purpose, I want the following
    // to be true:
    //
    // Let C be a class, let C.super be C's superclass, and let C.inter be the list of
    // C's implemented interfaces (C may be an interface, abstract, or concrete class).
    // Define ILIST(C) to be a sequence that satisfies the following properties:
    //
    // 1. ILIST(C) starts with C.
    // 2. If X is in ILIST(C), then so is X.super and each element of X.inter.
    // 3. For any class X in ILIST(C):
    //    2a. X appears before X.super in ILIST(C)
    //    2b. X appears before any X.inter in ILIST(C)
    // 4. No class appears more than once in ILIST(C)
    //
    // Note that the order can change when new classes are analyzed, so each class must be 
    // analyzed independently
    //
    // We need to elaborate on this idea to handle several issues:
    //
    // 1. We start with needing to determine whether a particular class C is ManagedData (mapped
    //    to composite data, and used for attribute and operation values in an Open MBean) or
    //    ManagedObject (mapped to an MBean with an ObjectName).  We will require that the super
    //    class graph of any object contain at most one class annotated with @ManagedObject or
    //    @ManagedData (and not both).  This means that for any class C, there is a class MC
    //    (which may be C) which is the unique class that is a superclass of C and is annotated
    //    with either @ManagedData or @ManagedObject.
    // 2. The MC class may also contain InheritedAttribute and IncludeSubclass annotations.
    //    InheritedAttribute is handled by searching in the superclasses for getter and setters
    //    conforming to the InheritedAttribute id.  IncludeSubclass extends the set of classes
    //    to scan for @ManagedAttribute and @ManagedOperation by the union of MC's superclasses,
    //    and the superclasses of all classes specified by IncludeSubclass.
    // 3. What we require here is that ALL classes that share the same MC class translate to the
    //    SAME kind of MBean or CompositeData.
    //
    // An additional complexity is that we need to properly handle generic Types.
    // The basic problem is that a reference to Map<K,V> tells us nothing about how
    // to map keys and values until we know to what K and V are bound.  Some cases
    // are pretty simple: Map<String,Foo> where Foo is @ManagedData is easy.  The
    // complexity arises in things like
    // @ManagedData
    // class Manager<T> {
    //     @ManagedAttribute
    //     Map<String,T> info() ;
    // }
    // 
    // Then later we see a reference to Manager<Foo>, from which we need to 
    // determine that T is bound to Foo (from the ParameterizedType.getActualTypeAguments() 
    // method).  
    // 
    // To do this, we need to evaluate all types as follows (in terms of typelib):
    // 1. A ClassDeclaration is evaluated to a ParameterizedType with base=ClassDeclaration
    //    (but with all types reachable from the ClassDecl evaluated) and empty
    //    typeAssignment.
    // 2. A TypeVariable evaluates to whatever it is bound to, or to the union
    //    of its upper bounds, or to Object (as a ParameterizedType).
    // 3. A GenericArrayType evaluates to a GenericArrayType of the evaluation
    //    of the component type.
    // 4. A WildcardType evaluates to the union of its upper bounds, or to 
    //    Object if it has no upper bounds (alt. could evaluate to intersection
    //    of lower bounds, but that's probably object anyway).
    // 5. A ParameterizedType evaluates to a ParameterizedType in which the 
    //    base type and all of the Types in the TypeAssignment are fully evaluated.
    //
    // In the end, the only types that remain are types in the set EvalType defined
    // as follows:
    // 1. ParameterizedType in which base and all elements of typeAssignment are
    //    in EvalType.
    // 2. GenericArrayType in which componentType is in EvalType.
    // 3. ClassDeclaration in which all elements of inheritance() and all types
    //    in methods.returnType and methods.parameterTypes where method is from 
    //    methods() are in EvalType.
    // 
    // Organization.
    // 
    // While we probably could do the construction and analysis in one pass,
    // it's probably better NOT to do this, at least initially.  Instead, we will:
    // 
    // 1. Construct a simple Graph just as we have been.
    // 2. Use a visitor to walk the Graph, converting native-mode (DeclarationFactory)
    //    typelib objects into fully-evaluated constructed-mode (DeclarationConstructor)
    //    typelib objects.
    // 3. Cache all of this.
    // 
    // Construction of the visitor
    //
    // The visitor needs to maintain a Display<TypeVariable,Type> to use in 
    // constructing the evaluated graph.  We need to enter a new scope whenever
    // the visitor references a ParameterizedType, and exit the scope whenever
    // the visitor completes processing of the ParameterizedType.  The cache is
    // essential too, as otherwise we could get cycles in the complex recursion.
    // This also means that all types must be settable after construction.
    private static final Graph.Finder<EvaluatedClassDeclaration> finder = 
        new Graph.Finder<EvaluatedClassDeclaration>() {
	
        public List<EvaluatedClassDeclaration> evaluate( 
            EvaluatedClassDeclaration arg ) {
            
            return ((EvaluatedClassDeclaration)arg).inheritance() ;
	}
    } ;

    private final List<EvaluatedClassDeclaration> classInheritance ;
    private String contents = null ;

    private EvaluatedClassAnalyzer( Graph<EvaluatedClassDeclaration> gr ) {
	List<EvaluatedClassDeclaration> result = 
            new ArrayList<EvaluatedClassDeclaration>( gr.getPostorderList() ) ;
	Collections.reverse( result ) ;
        classInheritance = result ;
    }

    public EvaluatedClassAnalyzer( final EvaluatedClassDeclaration cls ) {
	this( new Graph<EvaluatedClassDeclaration>( 
            cls, finder ) ) ;
    }

    public EvaluatedClassAnalyzer( final List<EvaluatedClassDeclaration> decls ) {
        this( new Graph<EvaluatedClassDeclaration>( decls, finder ) ) ;
    }

    public List<EvaluatedClassDeclaration> findClasses(
        Predicate<EvaluatedClassDeclaration> pred ) {
	
        final List<EvaluatedClassDeclaration> result =
            new ArrayList<EvaluatedClassDeclaration>() ;
	
        for (EvaluatedClassDeclaration c : classInheritance) {
            if (pred.evaluate( c )) {
                result.add( c ) ;
            }
        }

        return result ;
    }

    // Tested by testFindMethod
    // Tested by testGetAnnotatedMethods
    public List<EvaluatedMethodDeclaration> findMethods(
        Predicate<EvaluatedMethodDeclaration> pred ) {

        final List<EvaluatedMethodDeclaration> result =
            new ArrayList<EvaluatedMethodDeclaration>() ;

        for (EvaluatedClassDeclaration c : classInheritance) {
            for (EvaluatedMethodDeclaration m : c.methods()) {
                if (pred.evaluate( m )) {
                    result.add( m ) ;
                }
	    }
	}

	return result ;
    }

    public List<EvaluatedFieldDeclaration> findFields(
        Predicate<EvaluatedFieldDeclaration> pred ) {
	
        final List<EvaluatedFieldDeclaration> result =
            new ArrayList<EvaluatedFieldDeclaration>() ;
	
        for (EvaluatedClassDeclaration c : classInheritance) {
            for (EvaluatedFieldDeclaration f : c.fields()) {
                if (pred.evaluate( f )) {
                    result.add( f ) ;
                }
	    }
	}

	return result ;
    }

    @Override
    public synchronized String toString() {
        if (contents == null) {
            StringBuilder sb = new StringBuilder() ;

            boolean first = true ;
            sb.append( "ClassAnalyzer[" ) ;
            for (EvaluatedClassDeclaration cls : classInheritance) {
                if (first) {
                    first = false ;
                } else {
                    sb.append( " " ) ;
                }
                sb.append( cls.name() ) ;
            }
            sb.append( "]" ) ;
            contents = sb.toString() ;
        }

        return contents ;
    }
}
