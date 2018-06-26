/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.gmbal ;

import java.util.ResourceBundle ;

import java.io.Closeable ;

import java.lang.reflect.AnnotatedElement ;
import java.lang.annotation.Annotation ;

import javax.management.ObjectName ;
import javax.management.MBeanServer ;

import org.glassfish.pfl.tf.timer.spi.ObjectRegistrationManager ;

/** An interface used to managed Open MBeans created from annotated
 * objects.  This is mostly a facade over MBeanServer.
 * Note that certain methods must be called in the correct order:
 * <ol>
 * <li> Methods suspendJMXRegistration, resumeJMXRegistration,
 * getDomain, getMBeanServer, getResourceBundle, setRuntimeDebug, 
 * setRegistrationDebugLevel, setTypelibDebug, and close may be 
 * called at any time.
 * <li> All calls to addAnnotation, stripPrefix, and
 * stripPackageName must occur before any call to a createRoot method.
 * <li>All of the register and registerAtRoot methods and unregister, getObject,
 * getObjectName, and dumpSkeleton may only be called after
 * a createRoot method is called.
 * <li>Only one call to a createRoot method is permitted on any 
 * ManagedObjectManager.
 * <li>A call to close returns the MOM to the pre-createRoot state.
 * </ol>
 * If these constraints are violated, an IllegalStateException is thrown.
 */

public interface ManagedObjectManager extends Closeable {
    /** If called, no MBeans created after this call will be registered with
     * the JMX MBeanServer until resumeJMXRegistration is called.  Each call
     * increments a counter, so that nested and overlapping calls from multiple
     * threads work correctly.
	 * May be called at any time.
     */
    void suspendJMXRegistration() ;

    /** Decrements the suspend counter, if the counter is greater than 0.
     * When the counter goes to zero, it causes all MBeans created since
     * a previous call to suspendJMXRegistration incremented the counter from 0 to 1
     * to be registered with the JMX MBeanServer.  After this call, all new
     * MBean registration calls to the JMX MBeanServer happen within the
     * register call.
     * May be called at any time.
     */
    void resumeJMXRegistration() ;

    /** Return true if object is assignment compatible with a class or interface
     * that has an @ManagedObject annotation, otherwise false.  Only such objects
     * may be registered to create MBeans.
     * May be called at any time.
     */
    boolean isManagedObject( Object obj ) ;

    /** Create a default root MBean.
     * One of the createRoot methods must be called before any of the registration
     * methods may be called.
     * Only one call to a createRoot method is permitted after an
     * ManagedObjectManager is created.
     * @exception IllegalStateException if called after a call to any
     * createRoot method.
     * @return A default root MBean which supports only the AMX attributes.
     */
    GmbalMBean createRoot() ;
    
    /** Create a root MBean from root, which much have a method with the
     * @NameValue annotation.
     * One of the createRoot methods must be called before any of the registration
     * methods may be called.
     * Only one call to createRoot is permitted after an ManagedObjectManager
     * is created. 
     * @param root The Java object to be used to construct the root.
     * @exception IllegalStateException if called after a call to any
     * createRoot method.
     * @return The newly constructed MBean.
     */
    GmbalMBean createRoot( Object root ) ;
    
    /** Create a root MBean from root with the given name.
     * One of the createRoot methods must be called before any of the registration
     * methods may be called.
     * Only one call to createRoot is permitted after an ManagedObjectManager
     * is created.
     * @param root The Java object to be used to construct the root.
     * @param name The ObjectName name field to be used in the ObjectName of
     * the MBean constructed from root.
     * @exception IllegalStateException if called after a call to any
     * createRoot method.
     * @return The newly constructed MBean.
     */
    GmbalMBean createRoot( Object root, String name ) ;
    
    /** Return the root of this ManagedObjectManager.
     * May be called at any time.
     * @return the root constructed in a createRoot operation, or null if called
     * before a createRoot call.
     */
    Object getRoot() ;
    
    /** Construct an Open Mean for obj according to its annotations,
     * and register it with domain getDomain() and the appropriate
     * ObjectName.  The MBeanServer from setMBeanServer (or its default) is used.
     * Here parent is considered to contain obj, and this containment is
     * represented by the construction of the ObjectName following the AMX
     * specification for ObjectNames.
     * <p>
     * The MBeanInfo for the result is actually ModelMBeanInfo, and may contain
     * extra metadata as defined using annotations defined with the 
     * @DescriptorKey and @DescriptorField meta-annotations.
     * <p>
     * Must be called after a successful createRoot call.
     * <p>
     * This version of register should not be used to register singletons.
     * </ol>
     * @param parent The parent object that contains obj.
     * @param obj The managed object we are registering.
     * @param name The name to use for registering this object.
     * @return The MBean constructed from obj.
     * @exception IllegalStateException if called before a createRoot method is
     * called successfully.
     */
    GmbalMBean register( Object parent, Object obj, String name ) ;

    /** Same as register( parent, obj, name ), but here the name
     * is derived from an @NameValue annotation.
     * <p>
     * This version of register should also be used to register singletons.
     * 
     * @param parent The parent object that contains obj.
     * @param obj The managed object we are registering.
     * @return The MBean constructed from obj.
     * @exception IllegalStateException if called before a createRoot method is
     * called successfully.
     */
    GmbalMBean register( Object parent, Object obj ) ;
    
    /** Registers the MBean for obj at the root MBean for the ObjectManager,
     * using the given name.  Exactly the same as mom.register( mom.getRoot(),
     * obj, name ).
     * <p>
     * Must be called after a successful createRoot call.
     * <p>
     * This version of register should not be used to register singletons.
     * @param obj The object for which we construct and register an MBean.
     * @param name The name of the MBean.
     * @return The MBean constructed from obj.
     * @exception IllegalStateException if called before a createRoot method is
     * called successfully.
     */
    GmbalMBean registerAtRoot( Object obj, String name ) ;
    
    /** Same as registerAtRoot( Object, String ), but here the name
     * is derived from an @ObjectKeyName annotation.  Exactly the same as
     * mom.register( mom.getRoot(), obj ).
     * <p>
     * This version of register should also be used to register singletons.
     * @param obj The managed object we are registering.
     * @return The MBean constructed from obj.
     * @exception IllegalStateException if called before a createRoot method is
     * called successfully.
     */
    GmbalMBean registerAtRoot( Object obj ) ;
    

    /** Unregister the Open MBean corresponding to obj from the
     * mbean server.
     * <p>
     * Must be called after a successful createRoot call.
     * @param obj The object originally passed to a register method.
     */
    void unregister( Object obj ) ;

    /** Get the ObjectName for the given object (which must have
     * been registered via a register call).
     * <p>
     * Must be called after a successful createRoot call.
     * @param obj The object originally passed to a register call.
     * @return The ObjectName used to register the MBean.
     */
    ObjectName getObjectName( Object obj ) ;

    /** Get an AMXClient instance for the object obj, if obj is registered
     * as an MBean in this mom.
     * <p>
     * Must be called after a successful createRoot call.
     * @param obj The object corresponding to an MBean.
     * @return An AMXClient that acts as a proxy for this MBean.
     */
    AMXClient getAMXClient( Object obj ) ;

    /** Get the Object that was registered with the given ObjectName.
     * Note that getObject and getObjectName are inverse operations.
     * <p>
     * Must be called after a successful createRoot call.
     * @param oname The ObjectName used to register the object.
     * @return The Object passed to the register call.
     */
    Object getObject( ObjectName oname ) ;
    
    /** Add a type prefix to strip from type names, to shorten the names for
     * a better presentation to the user.  This may only be called before a
     * createRot method is called.
     *
     * @param str Class package name to strip from type name.
     * @exception IllegalStateException if called after createRoot method.
     */
    void stripPrefix( String... str ) ;

    /** Change the default type name algorithm so that if nothing else 
     * applies, the entire package prefix is stripped form the Class name.
     * Otherwise, the full Class name is the type.
     * 
     * @exception IllegalStateException if called after a createRoot method.
     */
    void stripPackagePrefix() ;
    
    /** Return the domain name that was used when this ManagedObjectManager
     * was created.  This is the JMX domain that will be used in all ObjectNames
     * created by this ManagedObjectManager.
     * <p>
     * May be called at any time.
     * @return Get the domain name for this ManagedObjectManager.
     */
    String getDomain() ;

    /** Set the MBeanServer to which all MBeans using this interface
     * are published.  The default value is 
     * java.lang.management.ManagementFactory.getPlatformMBeanServer().
     * <p>
     * Must be called before a successful createRoot call.
     * @param server The MBeanServer to set as the MBeanServer for this 
     * ManagedObjectManager.
     */
    void setMBeanServer( MBeanServer server ) ;

    /** Get the current MBeanServer.
     * <p>
     * May be called at any time.
     * @return The current MBeanServer, either the default, or the value passed
     * to setMBeanServer.
     */
    MBeanServer getMBeanServer() ;

    /** Set the ResourceBundle to use for getting localized descriptions.
     * If not set, the description is the value in the annotation.
     * <p>
     * Must be called before a successful call to a createRoot method.
     * @param rb The resource bundle to use.  May be null.
     */
    void setResourceBundle( ResourceBundle rb ) ;

    /** Get the resource bundle (if any) set by setResourceBundle.
     * <p>
     * May be called at any time.
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
     * @exception IllegalStateException if called after a call to a createRoot
     * method.
     */
    void addAnnotation( AnnotatedElement element, Annotation annotation ) ;

    /** Add all annotations for this class as if they were declared on the
     * inheritance parent(s) of the class (immediate superclass for a class, all
     * immediate superinterfaces for an interface).  Also add all method
     * annotations for methods that override an inherited method.  This acts as
     * if all annotations on cls were actually applied to the immediate super
     * class or interface.
     *
     * @param cls Class to analyze for inherited annotations.
     */
    void addInheritedAnnotations( Class<?> cls ) ;
        
    /** DebugLevel used to control how much debug info is printed for 
     * registration of objects.
     */
    public enum RegistrationDebugLevel { NONE, NORMAL, FINE } ;
    
    /** Print debug output to System.out.
     * <p>
     * May be called at any time.
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
     * <p>
     * May be called at any time.
     * 
     * @param flag true to enable runtime debug, false to disable.
     */
    void setRuntimeDebug( boolean flag ) ;

    /** Enabled generation of debug log for type evaluator debugging.  This
     * happens as part of the registration process for the first time a particular
     * class is processed.
     * <p>
     * May be called at any time.
     *
     * @param level set to 1 to just see the results of the TypeEvaluator, >1 to
     * see lots of details.  WARNING: values >1 will result in a large amount
     * of output.
     */
    void setTypelibDebug( int level ) ;

    /** Set debugging for JMX registrations.  If true, all registrations and
     * deregistrations with the MBeanServer are traced.
     *
     * @param flag True to enalbed registration tracing.
     */
    void setJMXRegistrationDebug( boolean flag ) ;

    /** Dump the skeleton used in the implementation of the MBean for obj.
     * Obj must be currently registered.
     * <p>
     * Must be called after a successful call to a createRoot method.
     * 
     * @param obj The registered object whose skeleton should be displayed.
     * @return The string representation of the skeleton.
     */
    String dumpSkeleton( Object obj ) ;

    /** Suppress reporting of a duplicate root name.  If this option is enabled,
     * createRoot( Object ) and createRoot( Object, String ) will return null
     * for a duplicate root name, otherwise a Gmbal error will be reported.
     * Note that this applies ONLY to createRoot: the register methods are
     * unaffected.  Also note that any other errors that might occur on
     * createRoot will be reported normally.
     * <p>
     * Must be called before a successful call to a createRoot method.
     */
    void suppressDuplicateRootReport( boolean suppressReport ) ;

    /** Return an ObjectRegistrationManager as required in the pfl timer services.
     * <p>
     * Can be called at any time.
     */
    ObjectRegistrationManager getObjectRegistrationManager() ;
}
