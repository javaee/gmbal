/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2009 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at legal/LICENSE.TXT.
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
 * 
 */ 

package org.glassfish.gmbal ;

import java.util.ResourceBundle ;

import java.io.Closeable ;

import java.lang.reflect.AnnotatedElement ;
import java.lang.annotation.Annotation ;

import javax.management.ObjectName ;
import javax.management.MBeanServer ;
import javax.management.NotificationEmitter ;

/** An interface used to managed Open MBeans created from annotated
 * objects.  This is mostly a facade over MBeanServer.
 */

public interface ManagedObjectManager extends Closeable {
    /** If called, no MBeans created after this call will be registered with
     * the JMX MBeanServer until resumeJMXRegistration is called.
     */
    void suspendJMXRegistration() ;

    /** Causes all MBeans created since a previous call to suspendJMXRegistration
     * to be registered with the JMX MBeanServer.
     */
    void resumeJMXRegistration() ;

    /** Create a default root MBean.
     * One of the createRoot methods must be called before any of the registration
     * methods may be called.
     * Only one call to createRoot is permitted after an ManagedObjectManager
     * is created.
     * @return
     */
    NotificationEmitter createRoot() ;
    
    /** Create a root MBean from root, which much have a method with the
     * @NameValue annotation.
     * One of the createRoot methods must be called before any of the registration
     * methods may be called.
     * Only one call to createRoot is permitted after an ManagedObjectManager
     * is created.
     * @param root
     * @return
     */
    NotificationEmitter createRoot( Object root ) ;
    
    /** Create a root MBean from root with the given name.
     * One of the createRoot methods must be called before any of the registration
     * methods may be called.
     * Only one call to createRoot is permitted after an ManagedObjectManager
     * is created.
     */
    NotificationEmitter createRoot( Object root, String name ) ;
    
    /** Return the root of this ManagedObjectManager.
     *
     * @return root
     */
    Object getRoot() ;
    
    /** Construct an Open Mean for obj according to its annotations,
     * and register it with domain getDomain() and the appropriate
     * ObjectName.  The MBeanServer from setMBeanServer (or its default) is used.
     * Here parent is considered to contain obj, and this containment is
     * represented by the construction of the ObjectName.
     * <p>
     * The ObjectName is constructed with name/value pairs in the following order:
     * <ol>
     * <li>The rest of the parent (that is, everything in the parent except
     * for the type and name values)
     * <li>The value "[typevalue]=[namevalue]" where typevalue is the value of
     * the type pair in the parent, and namevalue is the value of the name
     * pair of the parent (if any; parent may be null).
     * <li>"[typeword]=[type]", where typeword is either "type" or "j2eeType",
     * and type is derived from the @ManagedObject annotation of obj (note that 
     * the class of obj, or one of its superclasses or superinterfaces, must
     * be annotated with @ManagedObject or an error results).
     * <li>"name=[name]", 
     * </ol>
     * @param parent The parent object that contains obj.
     * @param obj The managed object we are registering.
     * @param name The name to use for registering this object.
     * 
     * @return The NotificationEmitter that can be used to register 
     * NotificationListeners against the registered MBean.  Only
     * AttributeChangeNotifications are supported.
     */
    NotificationEmitter register( Object parent, Object obj, String name ) ;

    /** Same as register( Object, Object, String ), but here the name
     * is derived from an @ObjectKeyName annotation.
     * 
     * @param parent The parent object that contains obj.
     * @param obj The managed object we are registering.
     * 
     * @return The NotificationEmitter that can be used to register 
     * NotificationListeners against the registered MBean.  Only
     * AttributeChangeNotifications are supported.
     */
    NotificationEmitter register( Object parent, Object obj ) ;
    
    /** Registers the MBean for obj at the root MBean for the ObjectManager,
     * using the given name.
     * @param obj The object for which we construct and register an MBean.
     * @param name The name of the MBean.
     * @return A NotificationEmitter for this MBean.
     */
    NotificationEmitter registerAtRoot( Object obj, String name ) ;
    
    /** Same as registerAtRoot( Object, String ), but here the name
     * is derived from an @ObjectKeyName annotation.
     * 
     * @param obj The managed object we are registering.
     * @return The NotificationEmitter that can be used to register 
     * NotificationListeners against the registered MBean.  Only
     * AttributeChangeNotifications are supported.
     */
    NotificationEmitter registerAtRoot( Object obj ) ;
    

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
    
    /** Add a type prefix to strip from type names, to shorten the names for
     * a better presentation to the user.
     *
     * @param str Class package name to strip from type name
     */
    void stripPrefix( String... str ) ;
    
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
     * The tracing is done with INFO-level logger calls.  The logger name is
     * that package name (org.glassfish.gmbal.impl).
     */
    void setRegistrationDebug( RegistrationDebugLevel level ) ;
    
    /** Enable generation of debug log at INFO level for runtime MBean operations
     * to the org.glassfish.gmbal.impl logger.
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

}
