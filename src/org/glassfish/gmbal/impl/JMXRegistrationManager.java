/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
 *  
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *  
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *  
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *  
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */ 
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.util.LinkedHashSet;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.glassfish.external.amx.MBeanListener;
import org.glassfish.gmbal.GmbalException;
import org.glassfish.gmbal.generic.UnaryVoidFunction;

/** A simple class that implements deferred registration.
 * When registration is suspended, mbean registrations are
 * queued until registration is resumed, at which time the
 * registration are processed in order.
 *
 * @author ken
 */
public class JMXRegistrationManager {
    private int suspendCount ;
    private final ManagedObjectManagerInternal mom ;
    private final ObjectName rootParentName ;

    // Lock used to protect several data members.
    final Object lock = new Object() ;

    // Protected by lock.
    private final LinkedHashSet<MBeanImpl> deferredRegistrations ;

    // Used in inner classes.  Protected by lock.
    MBeanImpl root ;
    boolean isJMXRegistrationEnabled ;

    // Used if rootParentName is not null.
    private RootParentListener callback ;
    private MBeanListener rpListener ;

    public JMXRegistrationManager(ManagedObjectManagerInternal mom,
        ObjectName rootParentName) {

        this.suspendCount = 0 ;
        this.mom = mom ;
        this.rootParentName = rootParentName ;
        this.deferredRegistrations = new LinkedHashSet<MBeanImpl>() ;
        this.root = null ;
        this.isJMXRegistrationEnabled = false ;
        this.callback = null ;
        this.rpListener = null ;
    }

    /** Set the MBeanImpl that is the root of this MBean tree.
     * Must be set before other methods are called (but this is not
     * enforced).
     * 
     * @param root The root of the tree.
     */
    public void setRoot( MBeanImpl root ) 
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
        NotCompliantMBeanException {

        synchronized( lock ) {
            this.root = root ;
            if (rootParentName == null) {
                isJMXRegistrationEnabled = true ;
                register( root ) ;
            } else {
                // Need to handle the suspended case here.  The non-suspended
                // case is handled in the Listener below.
                if (suspendCount > 0) {
                    deferredRegistrations.add( root ) ;
                    root.suspended( true ) ;
                }

                // Set up an MBeanListener so that we don't register MBeans unless
                // rootParentName actually refers to a registered MBean.
                // Note that the listener will register the root either now,
                // or once the root parent is available.
                callback = new RootParentListener() ;
                rpListener = new MBeanListener( mom.getMBeanServer(),
                    rootParentName, callback ) ;
                rpListener.startListening() ;
            }
        }
    }

    // This should undo everything that setRoot does.
    void clear() {
        synchronized (lock) {
            root = null ;
            isJMXRegistrationEnabled = false ;

            if (rpListener != null) {
                rpListener.stopListening() ;
            }
            rpListener = null ;
            callback = null ;
        }
    }

    /** Increment the suspended registration count.
     * All registrations with JMX are suspended while suspendCount > 0.
     */
    public void suspendRegistration() {
        synchronized (lock) {
            suspendCount++ ;
        }
    }

    /** Decrement the suspended registration count.
     * If the count goes to zero. all registrations that occurred while
     * suspendCount > 0 are registered with the JMX server, UNLESS
     * isJMXRegistrationEnabled is false, in which case we simply clear the
     * deferredRegistrations list, because all MBean will be registered once the
     * root is available.
     */
    public void resumeRegistration() {
        synchronized (lock) {
            suspendCount-- ;
            if (suspendCount == 0) {
                for (MBeanImpl mb : deferredRegistrations) {
                    try {
                        if (isJMXRegistrationEnabled) {
                            mb.register();
                        }
                        mb.suspended( false ) ;
                    } catch (JMException ex) {
                        Exceptions.self.deferredRegistrationException( ex, mb ) ;
                    }
                }

                deferredRegistrations.clear() ;
            }
        }
    }

    /** Handle registration of this MBean.  If we are suspended, 
     * simply add to the deferredRegistrationList and mark the MBean as
     * suspended.  If we are not suspended, then register if JMX
     * registration is enabled.
     * 
     * @param mb The MBeanImpl to register
     * @throws InstanceAlreadyExistsException
     * @throws MBeanRegistrationException
     * @throws NotCompliantMBeanException
     */
    public void register( MBeanImpl mb )
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
        NotCompliantMBeanException {

        synchronized (lock) {
            if (suspendCount>0) {
                deferredRegistrations.add( mb ) ;
                mb.suspended( true ) ;
            } else {
                if (isJMXRegistrationEnabled) {
                    mb.register() ;
                }
            }
        }
    }

    /** Unregister the MBean.  If we are suspended, remove from the
     * deferredRegistrations list and mark suspended false.  In any case,
     * we unregister from JMX if JMX registration is enabled.
     * Note that we may call unregister on an unregistered object if 
     * suspendCount > 0, but that's OK, because MBean.unregister does
     * nothing if mb is not registered.
     * @param mb The MBean to unregister.
     * @throws InstanceNotFoundException
     * @throws MBeanRegistrationException
     */
    public void unregister( MBeanImpl mb )
        throws InstanceNotFoundException, MBeanRegistrationException {

        synchronized (lock) {
            boolean wasSuspended = mb.suspended() ;

            if (wasSuspended) {
                deferredRegistrations.remove(mb) ;
                mb.suspended( false ) ;
            } else {
                if (isJMXRegistrationEnabled) {
                    mb.unregister() ;
                }
            }
        }
    }

    // Class used to listen for the registration and deregistration of the rootParent
    // (if a rootParent is used).
    private class RootParentListener implements MBeanListener.Callback {
        private void traverse( MBeanImpl mb, UnaryVoidFunction<MBeanImpl> pre,
            UnaryVoidFunction<MBeanImpl> post ) {

            if (pre != null) {
                pre.evaluate( mb ) ;
            }

            for (Map<String,MBeanImpl> nameToMBean : mb.children().values() ) {
                for (MBeanImpl child : nameToMBean.values() ) {
                    traverse( child, pre, post ) ;
                }
            }

            if (post != null) {
                post.evaluate( mb ) ;
            }
        }

        private final UnaryVoidFunction<MBeanImpl> REGISTER_FUNC =
            new UnaryVoidFunction<MBeanImpl>() {
                public void evaluate( MBeanImpl arg ) {
                    if (!arg.suspended()) {
                        try {
                            arg.register();
                        } catch (Exception ex) {
                            throw new GmbalException("Registration exception", ex ) ;
                        }
                    }
                }
            } ;

        public void mbeanRegistered(ObjectName arg0, MBeanListener arg1) {
            synchronized (lock) {
                if (!isJMXRegistrationEnabled) {
                    isJMXRegistrationEnabled = true ;

                    if (root != null) {
                        traverse( root, REGISTER_FUNC, null );
                    }
                }
            }
        }

        private final UnaryVoidFunction<MBeanImpl> UNREGISTER_FUNC =
            new UnaryVoidFunction<MBeanImpl>() {
                public void evaluate( MBeanImpl arg ) {
                    if (!arg.suspended()) {
                        try {
                            arg.unregister();
                        } catch (Exception ex) {
                            throw new GmbalException("Registration exception", ex ) ;
                        }
                    }
                }
            } ;

        public void mbeanUnregistered(ObjectName arg0, MBeanListener arg1) {
            synchronized (lock) {
                if (isJMXRegistrationEnabled) {
                    isJMXRegistrationEnabled = false ;

                    if (root != null) {
                        traverse( root, null, UNREGISTER_FUNC );
                    }
                }
            }
        }
    }
}
