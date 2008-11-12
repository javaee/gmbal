/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.generic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
    
    public FacetAccessorImpl( Object delegate ) {
        this.delegate = delegate ;
    }
    
    public <T> T facet(Class<T> cls) {
        Object result ;
        if (cls.isInstance(delegate)) {
            result = delegate ;
        } else {
            result = facetMap.get( cls ) ;
        }
        
        if (result == null) {
            return null ;
        } else {
            return cls.cast( result ) ;
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

    public Object invoke(Method method, Object... args) {
        Object target = facet( method.getDeclaringClass() ) ;
        if (target == null) {
            throw new IllegalArgumentException( 
                "No facet available for method " + method ) ;
        }
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException( "Exception on invocation", ex ) ;
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException( "Exception on invocation", ex ) ;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException( "Exception on invocation", ex ) ;
        }
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
