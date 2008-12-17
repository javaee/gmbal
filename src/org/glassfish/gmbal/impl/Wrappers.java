/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import org.glassfish.gmbal.GmbalException;
import org.glassfish.gmbal.logex.Chain;
import org.glassfish.gmbal.logex.ExceptionWrapper;
import org.glassfish.gmbal.logex.Log;
import org.glassfish.gmbal.logex.LogLevel;
import org.glassfish.gmbal.logex.Message;

/**
 *
 * @author ken
 */
@ExceptionWrapper( idPrefix="GMBAL" )
public interface Wrappers {
    @Log( id=0, level=LogLevel.WARNING  )
    @Message( "Exception in getMeta" )
    GmbalException getMetaException( @Chain Throwable exc ) ;
}
