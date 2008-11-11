/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.impl;

/** An interface representing some sort of managed object.  A ManagedEntity
 * always has a name and a type, and normally exists in some sort of scope of
 * related ManagedEntities.
 *
 * @author ken
 */
public interface ManagedEntity {
    /** Return the name of this ManagedEntity, which must uniquely identify
     * this ManagedEntity within some scope.
     * @return The name of this ManagedEntity.
     */
    String name() ;
    
    /** Return a string representing the Type of this ManagedEntity.  There may
     * be multiple ManagedEntities within the same scope that share the same 
     * type, but have different names.
     * @return The string representing the type.
     */
    String type() ;
}
