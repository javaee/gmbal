/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal;

/** Unchecked exception type used for all gmbal exceptions.
 *
 * @author ken
 */
public class GmbalException extends RuntimeException {
    public GmbalException( String msg ) {
        super( msg ) ;
    }

    public GmbalException( String msg, Throwable thr ) {
        super( msg, thr ) ;
    }
}
