/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;

/** A simple class that implements deferred registration.
 * When registration is suspended, mbean registrations are
 * queued until registration is resumed, at which time the
 * registration are processed in order.
 *
 * @author ken
 */
public class JMXRegistrationManager {
    private boolean isSuspended = false ;
    private LinkedHashSet<MBeanImpl> deferredRegistrations =
        new LinkedHashSet<MBeanImpl>() ;

    public synchronized void suspendRegistration() {
        isSuspended = true ;
    }

    public synchronized void resumeRegistration() {
        isSuspended = false ;
        for (MBeanImpl mb : deferredRegistrations) {
            try {
                mb.register();
            } catch (InstanceAlreadyExistsException ex) {
                Logger.getLogger(JMXRegistrationManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MBeanRegistrationException ex) {
                Logger.getLogger(JMXRegistrationManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotCompliantMBeanException ex) {
                Logger.getLogger(JMXRegistrationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        deferredRegistrations.clear() ;
    }

    public synchronized void register( MBeanImpl mb )
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
        NotCompliantMBeanException {

        if (isSuspended) {
            deferredRegistrations.add( mb ) ;
        } else {
            mb.register() ;
        }
    }

    public synchronized void unregister( MBeanImpl mb )
        throws InstanceNotFoundException, MBeanRegistrationException {

        if (isSuspended) {
            deferredRegistrations.remove(mb) ;
        } else {
            mb.unregister() ;
        }
    }
}
