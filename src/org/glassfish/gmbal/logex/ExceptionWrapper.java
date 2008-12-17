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
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionWrapper {
    /** Return the prefix used in front of the numeric exception ID in the formatter
     * exception message.  For example, CORBA uses IIOP for this purpose.
     * @return The log messaged ID prefix
     */
    String idPrefix() ;

    /** Return the logger name to be used for all logged messages generated
     * from the class.  Default is the package in which the class is defined.
     * @return The logger name.
     */
    String loggerName() default "" ;
}
