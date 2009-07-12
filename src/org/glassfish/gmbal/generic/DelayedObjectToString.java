/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.generic;

/**
 *
 * @author ken_admin
 */
public class DelayedObjectToString {
    private Object obj ;
    private ObjectUtility ou ;

    public DelayedObjectToString( Object obj, ObjectUtility ou ) {
	this.obj = obj ;
	this.ou = ou ;
    }

    @Override
    public String toString() {
	return ou.objectToString( obj ) ;
    }
}
