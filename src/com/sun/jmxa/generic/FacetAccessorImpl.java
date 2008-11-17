/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.generic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 
 *
 * @author ken
 */
public class FacetAccessorImpl implements FacetAccessor {
    private Object delegate ;
    private Map<Class<?>,Object> facetMap =
        new HashMap<Class<?>,Object>() ;
    private DprintUtil dputil ;
    
    public FacetAccessorImpl( Object delegate ) {
        this.delegate = delegate ;
        this.dputil = new DprintUtil( getClass() ) ;
    }
    
    public <T> T facet(Class<T> cls, boolean debug ) {
        Object result = null ;
        if (debug) {
            dputil.enter( "facet", "cls=", cls ) ;
        }
        
        try {
            if (cls.isInstance(delegate)) {
                result = delegate ;
                if (debug) {
                    dputil.info( "result is delegate" ) ;
                }
            } else {
                result = facetMap.get( cls ) ;
                if (debug) {
                    dputil.info( "result=", result ) ;
                }
            }
                    
            if (result == null) {
                return null ;
            } else {
                return cls.cast( result ) ;
            }
        } finally {
            if (debug) {
                dputil.exit( result ) ;
            }
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
                
        ClassAnalyzer ca = new ClassAnalyzer( obj.getClass() ) ;
        ca.findClasses( 
            new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    facetMap.put( arg, obj ) ;
                    return false ;
                } } ) ;
    }

    public Object invoke(Method method, boolean debug, Object... args) {
        if (debug) {
            dputil.enter( "invoke", "method=", method, "args=",
                Arrays.asList( args ) ) ;
        }
        
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
                throw new RuntimeException( "Exception on invocation", ex ) ;
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException( "Exception on invocation", ex ) ;
            } catch (InvocationTargetException ex) {
                throw new RuntimeException( "Exception on invocation", ex ) ;
            }
        } catch (RuntimeException exc) {
            if (debug) {
                dputil.exception( "Exception in method invoke call", exc ) ;
            }
        } finally {
            if (debug) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    public void removeFacet( Class<?> cls ) {
        if (cls.isInstance(delegate)) {
            throw new IllegalArgumentException( 
                "Cannot add facet of supertype of this object" ) ;
        }
        
        ClassAnalyzer ca = new ClassAnalyzer( cls ) ;
        ca.findClasses( 
            new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    facetMap.remove( arg ) ;
                    return false ;
                } } ) ;
    }


}
