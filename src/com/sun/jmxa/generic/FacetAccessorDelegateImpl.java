/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.generic;

import java.lang.reflect.Method;
import java.util.Collection;

/** Implementation of FacetAccessor that delegates to another FacetAccessor,
 * typically a FacetAccessorImpl.  The purpose of this call is to provide
 * a convenient template of methods that may be copied into a class that
 * implements FacetAccessor.  Typically such a class implements that
 * FacetAccessor interface and defines a data member initialized as:
 * 
 * FacetAccessor facetAccessorDelegate = new FacetAccessorImpl( this ) ;
 * 
 * and then simply copies the other methods directly.
 * 
 * This is all a workaround for the fact that Java does not
 * support dynamic inheritance, or more than one superclass.  
 * 
 * Because this is a template, I have commented out all of the code.
 * It is not used at runtime or compiletime.
 *
 * @author ken
 */
abstract class FacetAccessorDelegateImpl implements FacetAccessor {
    /*
    private FacetAccessor facetAccessorDelegate ;
    
    public FacetAccessorDelegateImpl( FacetAccessor fa ) {
        facetAccessorDelegate = fa ;
    }
    
    public <T> T facet(Class<T> cls, boolean debug ) {
        return facetAccessorDelegate.facet( cls, debug ) ;
    }

    public <T> void addFacet(T obj) {
        facetAccessorDelegate.addFacet( obj ) ;
    }

    public void removeFacet( Class<?> cls ) {
        facetAccessorDelegate.removeFacet( cls ) ;
    }

    public Object invoke(Method method, boolean debug, Object... args) {
        return facetAccessorDelegate.invoke( method, debug, args ) ;
    }

    public Collection<Object> facets() {
        return facetAccessorDelegate.facets() ;
    }
    */
}
