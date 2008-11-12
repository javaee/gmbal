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
    
    boolean isContainer() default false ;
} 
