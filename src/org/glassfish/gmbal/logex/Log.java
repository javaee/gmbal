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
public @interface Log {
    /** The logging Level (encoded as an enum) to use for the log record
     * generated from the annotated method.
     * 
     * @return The log level.
     */
    LogLevel level() default LogLevel.WARNING ;

    /** The exception ID to be used.  This is used to construct the message
     * ID in the log message.
     * @return The exception id (which must include the VMCID).
     */
    int id() default 0 ;
}
