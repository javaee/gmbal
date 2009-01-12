/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.generic;

import java.util.IdentityHashMap;
import java.util.Iterator;


/**
 *
 * @author ken
 */
public class ObjectSet {
    private IdentityHashMap map = new IdentityHashMap();
    private static Object VALUE = new Object() ;

    public boolean contains( Object obj ) {
        return map.get( obj ) == VALUE ;
    }

    public void add( Object obj ) {
        map.put( obj, VALUE ) ;
    }

    public void remove( Object obj ) {
        map.remove( obj ) ;
    }

    public Iterator iterator() {
        return map.keySet().iterator() ;
    }
}
