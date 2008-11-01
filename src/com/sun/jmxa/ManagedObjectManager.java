/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.jmxa ;

import java.util.ResourceBundle ;

import java.io.Closeable ;

import java.lang.reflect.AnnotatedElement ;
import java.lang.annotation.Annotation ;

import java.util.List;
import javax.management.ObjectName ;
import javax.management.MBeanServer ;
import javax.management.NotificationEmitter ;

/** An interface used to managed Open MBeans created from annotated
 * objects.  This is mostly a facade over MBeanServer.
 */

/*
 * XXX Do we need to support an @Notification annotation as in JSR 255?
 * XXX Do we need dependency injection (@Resource)?
 * XXX Test attribute change notification.
 * XXX Test @ObjectNameKey
 * XXX Should we automate handling of recursive types using @Key/@Map?
 * XXX Do we need to support @Descriptor from JSR 255?
 * XXX Do we want simplified exception wrapper scheme for I18N of exceptions?
 */
public interface ManagedObjectManager extends Closeable {
    /** Construct an Open Mean for obj according to its annotations,
     * and register it with domain getDomain() and the key/value pairs
     * given by props in the form key=value.  The MBeanServer from 
     * setMBeanServer (or its default) is used.
     * <p>
     * The ObjectName is constructed with name/value pairs in the following order:
     * <ol>
     * <li>The type
     * <li>Defaults in the ManagedObjectManager
     * <li>Arguments passed into register
     * <li>Any name/value pairs derived from ObjectNameKey annotations
     * </ol>
     * @param obj The object used to construct an OpenMbean that is registered
     * with the MBeanServer.
     * @param props The additional properties to use in constructing the 
     * ObjectNameKey for the registered MBean.  May be null, in which case
     * the ObjectNameKey annotations are used to construct the 
     * ObjectNameKey.
     * @return The NotificationEmitter that can be used to register 
     * NotificationListeners against the registered MBean.  Only
     * AttributeChangeNotifications are supported.
     */
    NotificationEmitter register( Object obj, String... props ) ;

    /** Same as register( Object, String...) except that key/value
     * pairs are given as properties.
     * @param obj The object used to construct an OpenMbean that is registered
     * with the MBeanServer.
     * @param props The additional properties to use in constructing the 
     * ObjectNameKey for the registered MBean.  May be null, in which case
     * the ObjectNameKey annotations are used to construct the 
     * ObjectNameKey.
     * @return The NotificationEmitter that can be used to register 
     * NotificationListeners against the registered MBean.  Only
     * AttributeChangeNotifications are supported.     */
    NotificationEmitter register( Object obj, List<String> props )  ;

    /** Unregister the Open MBean corresponding to obj from the
     * mbean server.
     * @param obj The object originally passed to a register method.
     */
    void unregister( Object obj ) ;

    /** Get the ObjectName for the given object (which must have
     * been registered via a register call).
     * @param obj The object originally passed to a register call.
     * @return The ObjectName used to register the MBean.
     */
    ObjectName getObjectName( Object obj ) ;

    /** Get the Object that was registered with the given ObjectName.
     * Note that getObject and getObjectName are inverse operations.
     * @param oname The ObjectName used to register the object.
     * @return The Object passed to the register call.
     */
    Object getObject( ObjectName oname ) ;

    /** Return the domain name that was used when this ManagedObjectManager
     * was created.
     * @return Get the domain name for this ManagedObjectManager.
     */
    String getDomain() ;

    /** Set the MBeanServer to which all MBeans using this interface
     * are published.  The default value is 
     * java.lang.management.ManagementFactory.getPlatformMBeanServer().
     * @param server The MBeanServer to set as the MBeanServer for this 
     * ManagedObjectManager.
     */
    void setMBeanServer( MBeanServer server ) ;

    /** Get the current MBeanServer.
     * @return The current MBeanServer, either the default, or the value passed
     * to setMBeanServer.
     */
    MBeanServer getMBeanServer() ;

    /** Set the ResourceBundle to use for getting localized descriptions.
     * If not set, the description is the value in the annotation.
     * @param rb The resource bundle to use.  May be null.
     */
    void setResourceBundle( ResourceBundle rb ) ;

    /** Get the resource bundle (if any) set by setResourceBundle.
     * @return The resource bundle set by setResourceBundle: may be null.
     */
    ResourceBundle getResourceBundle() ;
    
    /** Method to add an annotation to an element that cannot be modified.
     * This is typically needed when dealing with an implementation of an 
     * interface that is part of a standardized API, and so the interface
     * cannot be annotated by modifiying the source code.  In some cases the
     * implementation of the interface also cannot be inherited, because the
     * implementation is generated by a standardized code generator.  Another
     * possibility is that there are several different implementations of the
     * standardized interface, and it is undesirable to annotate each 
     * implementation with @InheritedAttributes.
     * @param element The annotated element (class or method for our purposes).
     * @param annotation The annotation we wish to add to the element.
     */
    void addAnnotation( AnnotatedElement element, Annotation annotation ) ;
        
    /** DebugLevel used to control how much debug info is printed for 
     * registration of objects.
     */
    public enum RegistrationDebugLevel { NONE, NORMAL, FINE } ;
    
    /** Print debug output to System.out.
     * 
     * @param level NONE is no debugging at all, NORMAL traces high-level
     * construction of skeletons and type converters, and dumps results of new
     * skeletons and type converters, FINE traces everything in great detail.
     */
    void setRegistrationDebug( RegistrationDebugLevel level ) ;
    
    /** Enable printing of debug output for runtime MBean operations
     * to System.out.
     * 
     * @param flag true to enable runtime debug, false to disable.
     */
    void setRuntimeDebug( boolean flag ) ;
    
    /** Dump the skeleton used in the implementation of the MBean for obj.
     * Obj must be currently registered.
     * 
     * @param obj The registered object whose skeleton should be displayed.
     * @return The string representation of the skeleton.
     */
    String dumpSkeleton( Object obj ) ;
    
    /** Add a type prefix to strip from type names, to shorten the names for
     * a better presentation.
     *
     * @param str Class package name to strip from type name
     */
    void addTypePrefix( String str ) ;
}
