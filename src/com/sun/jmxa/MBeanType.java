/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa;

import java.lang.annotation.Documented ;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;

/** Annotation to contol exactly how the type value in the ObjectName 
 * is extracted from a class when registering an instance of that class.
 * The absence of this annotation is the same as the default values.
 *
 * @author ken
 */
@Documented 
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface MBeanType {
    /** An explicit type name to be used for a ManagedObject.
     * @return The optional type value.
     */
    String type() default "" ;
    
    /** Return true if this MBean may contain other MBeans, otherwise false.
     * 
     * @return whether or not this MBean is a container.
     */
    boolean isContainer() default false ;
    
    /** True if only one MBean of this type may be created inside the same
     * parent container.
     * 
     * @return
     */
    boolean isSingleton() default false ;
        
    /** True if all MBean attributes are invariant, that is, have the same
     * value for the lifetime of the MBean.  This may be used as a hint
     * to clients that the contents of the MBean can be cached.
     * 
     * @return True if all attributes of the MBean are invariant.
     */
    boolean mbeanInfoIsInvariant() default true ;
    
    /** Value to use in AMX CLI pathnames that include this MBean.
     * Defaults to same value as type (whether type is obtained explicitly
     * or implicitly).
     * 
     * @return The optional pathName component.
     */
    String pathName() default "" ;
} 
