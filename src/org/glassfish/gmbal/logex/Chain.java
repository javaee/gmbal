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
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Chain {

}
