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
package org.glassfish.gmbal.generic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.gmbal.GmbalException;

/** 
 *
 * @author ken
 */
public class FacetAccessorImpl implements FacetAccessor {

    private Object delegate ;
    private Map<Class<?>,Object> facetMap =
        new HashMap<Class<?>,Object>() ;

    // This class spends a bit too much time in mm because it is called
    // very frequently, so use if (debug) in front of each mm call.
    // This means that there is no operation tracing for this class in
    // the standard MethodMonitor, but that should be fine.
    private MethodMonitor mm ;
    
    public FacetAccessorImpl( Object delegate ) {
        this.delegate = delegate ;
        this.mm = MethodMonitorFactory.makeStandard( getClass() ) ;
    }
    
    public <T> T facet(Class<T> cls, boolean debug ) {
        Object result = null ;
        if (debug) mm.enter( debug, "facet", cls ) ;
        
        try {
            if (cls.isInstance(delegate)) {
                result = delegate ;
                if (debug) mm.info( debug, "result is delegate" ) ;
            } else {
                result = facetMap.get( cls ) ;
                if (debug) mm.info( debug, "result=", result ) ;
            }
                    
            if (result == null) {
                return null ;
            } else {
                return cls.cast( result ) ;
            }
        } finally {
            if (debug) mm.exit( debug, result ) ;
        }
    }
    
    public Collection<Object> facets() {
        Collection<Object> result = new ArrayList<Object>() ;
        result.addAll( facetMap.values() ) ;
        result.add( this ) ;
        return result ;
    }
    
    public <T> void addFacet( final T obj) {
        if (obj.getClass().isInstance(delegate)) {
            throw new IllegalArgumentException( 
                "Cannot add facet of supertype of this object" ) ;
        }
                
        ClassAnalyzer ca = ClassAnalyzer.getClassAnalyzer( obj.getClass() ) ;
        ca.findClasses( 
            new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    facetMap.put( arg, obj ) ;
                    return false ;
                } } ) ;
    }

    public Object invoke(Method method, boolean debug, Object... args) {
        if (debug) mm.enter( debug, "invoke", method, args ) ;
        
        Object result = null ;
        try {
            Object target = facet( method.getDeclaringClass(), debug ) ;
            if (target == null) {
                throw new IllegalArgumentException( 
                    "No facet available for method " + method ) ;
            }
            
            try {
                result = method.invoke(target, args);
            } catch (IllegalAccessException ex) {
                throw new GmbalException( "Exception on invocation", ex ) ;
            } catch (IllegalArgumentException ex) {
                throw new GmbalException( "Exception on invocation", ex ) ;
            } catch (InvocationTargetException ex) {
                throw new GmbalException( "Exception on invocation", ex ) ;
            }
        } finally {
            if (debug) mm.exit( debug, result ) ;
        }
        
        return result ;
    }

    public Object get(Field field, boolean debug) {
        if (debug) mm.enter( debug, "get", field ) ;

        Object result = null ;

        try {
            Object target = facet( field.getDeclaringClass(), debug ) ;

            try {
                result = field.get(target);
            } catch (IllegalArgumentException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            } catch (IllegalAccessException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            }
        } finally {
            if (debug) mm.exit( debug, result ) ;
        }

        return result ;
    }

    public void set(Field field, Object value, boolean debug) {
        if (debug) mm.enter( debug, "set", field, value ) ;

        try {
            Object target = facet( field.getDeclaringClass(), debug ) ;

            try {
                field.set(target, value);
            } catch (IllegalArgumentException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            } catch (IllegalAccessException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            }
        } finally {
            if (debug) mm.exit( debug ) ;
        }
    }

    public void removeFacet( Class<?> cls ) {
        if (cls.isInstance(delegate)) {
            throw new IllegalArgumentException( 
                "Cannot add facet of supertype of this object" ) ;
        }
        
        ClassAnalyzer ca = ClassAnalyzer.getClassAnalyzer( cls ) ;
        ca.findClasses( 
            new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    facetMap.remove( arg ) ;
                    return false ;
                } } ) ;
    }

}
