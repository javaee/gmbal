/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.generic;

import java.lang.reflect.Method;
import java.util.Collection;

/** Interface to access facets of an object.  A facet is
 * an instance of a particular class.  It may be implemented
 * in a variety of ways, including inheritance, delegation,
 * or dynamic construction on demand.
 *
 * @author ken
 */
public interface FacetAccessor {
    /** Access the Facet of Class T from the object.
     * 
     * @param <T> The Type (as a Class) of the Facet.
     * @param cls The class of the facet.
     * @return Instance of cls for this facet.  Null if no such
     * facet is available.
     */
    <T> T facet( Class<T> cls, boolean debug ) ;
    
    /** Add a facet to the object.  The type T must not already
     * be available as a facet.
     * @param <T>
     * @param obj
     */
    <T> void addFacet( T obj ) ;
    
    /** Remove the facet (if any) of the given type.
     * 
     * @param <T>
     * @param cls 
     */
    void removeFacet( Class<?> cls ) ;
    
    /** Return a list of all facets on this object.
     *
     * @return Collection of all facets.
     */
    Collection<Object> facets() ;
    
    /** Invoke method on the appropriate facet of this 
     * object, that is, on the facet corresponding to 
     * method.getDeclaringClass.
     * @param method The method to invoke.
     * @param args Arguments to the method.
     * @return restult of the invoke call.
     */
    Object invoke( Method method, boolean debug, Object... args ) ;
}
