/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.logex;

/**
 *
 * @author ken
 */
import java.lang.annotation.Documented ;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;

/** This annotation is applied to an interface or abstract class that is used
 * to define methods for logging and/or constructing exceptions.
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Message {
    /** Define the Log message to be generated for this exception.
     * The default format is "name: arg1={1} ..." where name is the annotated
     * Method name and each arg is listed after the name.
     * The default format is also used if this annotation is not present.
     * @return
     */
    String value() default "" ;
}
