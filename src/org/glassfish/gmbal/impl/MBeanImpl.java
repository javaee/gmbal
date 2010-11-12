/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gmbal.impl ;

import java.lang.reflect.Field;
import org.glassfish.gmbal.generic.FacetAccessor;
import org.glassfish.gmbal.generic.FacetAccessorImpl;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.management.Attribute ;
import javax.management.AttributeList ;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException ;
import javax.management.InvalidAttributeValueException ;
import javax.management.AttributeNotFoundException ;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException ;
import javax.management.MBeanInfo ;
import javax.management.NotificationBroadcasterSupport ;
import javax.management.MBeanNotificationInfo ;
import javax.management.AttributeChangeNotification ;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.security.AccessController ;
import java.security.PrivilegedExceptionAction ;
import java.security.PrivilegedActionException ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.external.amx.AMX;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.generic.OperationTracer;

public class MBeanImpl extends NotificationBroadcasterSupport 
    implements FacetAccessor, GmbalMBean {
    
    private boolean registered ;
    private final MBeanSkeleton skel ;
    private final String type ;
    private String name ;
    private ObjectName oname ;
    private MBeanImpl parent ;
    private final Set<String> subTypes ; // Null if not used: don't create empty
					 // sets if not used.

    // First index is type, second is name.
    private Map<String,Map<String,MBeanImpl>> children ;

    private Object target ;
    private MBeanServer server ;
    private String parentPathForObjectName;
    private boolean suspended;
    
    public MBeanImpl( final MBeanSkeleton skel, 
        final Object obj, final MBeanServer server,
        final String type ) {

        this.registered = false ;
        this.skel = skel ;
        this.type = type ;
        this.name = "" ;
        this.oname = null ;
        this.parent = null ;
        this.children = new HashMap<String,Map<String,MBeanImpl>>() ;
        this.target = obj ;
	String[] stypes = skel.getMBeanType().subTypes() ;
	if (stypes.length > 0) {
	    this.subTypes = new HashSet( Arrays.asList(stypes)) ;
	} else {
	    this.subTypes = null ;
	}

        addFacet( obj ) ;
        addFacet( new AMXImpl( this ) ) ;

        // Note that the construction of an MBean skeleton and
        // facet registration must stay in sync.  The code is currently separated into
        // two places (here and call to new MBeanSkeleton( skel, skel )).
        // This will also be important for dealing with multiple upper bounds.
        this.server = server ;
        this.parentPathForObjectName = null ;
        this.suspended = false ;
    }
        
    @Override
    public synchronized boolean equals( Object obj ) {
        if (this == obj) {
            return true ;
        }
        
        if (!(obj instanceof MBeanImpl)) {
            return false ;
        }
        
        MBeanImpl other = (MBeanImpl)obj ;
        
        return parent == other.parent() &&
            name.equals( other.name() ) &&
            type.equals( other.type() ) ;
    }
    
    @Override
    public synchronized int hashCode() {
        if (parent == null) {
            return name.hashCode() ^ type.hashCode() ;
        } else {
            return name.hashCode() ^ type.hashCode() ^ parent.hashCode() ;
        }
    }
 
    @Override
    public String toString() {

        return "MBeanImpl[type=" + type + ",name=" + name
            + ",oname=" + oname + "]" ;
    }
    
    public MBeanSkeleton skeleton() {
        return skel ;
    }

    public String type() {
        return type ;
    }
    
    public Object target() {
        return target ;
    }
    
    public synchronized String name() {
        return name ;
    }

    public synchronized void name( String str ) {
        name = str ;
    }
    
    public synchronized ObjectName objectName() {
        return oname ;
    }
    
    public synchronized void objectName( ObjectName oname ) {
        this.oname = oname ;
    }

    public synchronized MBeanImpl parent() {
        return parent ;
    }
   
    public synchronized void parent( MBeanImpl entity ) {
        if (parent == null) {
            parent = entity ;
        } else {
            throw Exceptions.self.nodeAlreadyHasParent(entity) ;
        }
    }

    public synchronized Map<String,Map<String,MBeanImpl>> children() {
        // Make a copy to avoid problems with concurrent modification.
        Map<String,Map<String,MBeanImpl>> result = new 
            HashMap<String,Map<String,MBeanImpl>>() ;
        for (Map.Entry<String,Map<String,MBeanImpl>> entry 
            : children.entrySet()) {
            
            result.put( entry.getKey(), 
                Collections.unmodifiableMap( 
                    new HashMap<String,MBeanImpl>( entry.getValue() ) ) ) ;
        }
           
        return Collections.unmodifiableMap( result ) ;
    }
   
    public synchronized void addChild( MBeanImpl child ) {
        child.parent( this ) ;

        // XXX Add test case!
        if (subTypes != null && !subTypes.contains(child.type())) {
            throw Exceptions.self.invalidSubtypeOfParent( this.oname,
                this.subTypes, child.objectName(), child.type() ) ;
        }

        Map<String,MBeanImpl> map = children.get( child.type() ) ;
        if (map == null) {
            map = new HashMap<String,MBeanImpl>() ;
            children.put( child.type(), map ) ;
        }

        // XXX add test case!
        boolean isSingleton = child.skeleton().getMBeanType().isSingleton() ;
        if (isSingleton && map.size() > 0) {
            throw Exceptions.self.childMustBeSingleton( this.oname,
                child.type(), child.objectName() ) ;
        }

        map.put( child.name(), child) ;
    }
   
    public synchronized void removeChild( MBeanImpl child ) {
        Map<String,MBeanImpl> map = children.get( child.type() ) ;
        if (map != null) {
            map.remove( child.name() ) ;
            if (map.size() == 0) {
                children.remove( child.type() ) ;
            }
        }
    }
 
    private void restNameHelper( StringBuilder sb ) {
        if (parent() != null) {
            parent().restNameHelper( sb ) ;
            sb.append( '/' ) ;
        } 

        sb.append( type() ) ;
	if (!name.equals("")) {
            sb.append( '[' ) ;
	    sb.append( name ) ;
            sb.append( ']' ) ;
	}
    }

    private synchronized String restName() {
        StringBuilder sb = new StringBuilder( 60 ) ;
        restNameHelper( sb ) ;
        return sb.toString() ;
    }

    public synchronized String getParentPathPart( String rootParentPrefix ) {
        if (parentPathForObjectName == null) {
            StringBuilder result = new StringBuilder() ;
            result.append( AMX.PARENT_PATH_KEY ) ;
            result.append( "=" ) ;

            String qname ;
            if (rootParentPrefix == null) {
                qname = "/" + restName() ;
            } else {
                qname = rootParentPrefix + "/" + restName() ;
            }

            result.append( MBeanTree.getQuotedName( qname ) ) ;

            // Note that the "/" MUST be passed to getQuotedName, or we
            // can get things like /"...", which is wrong.
            result.append( ',' ) ;

            parentPathForObjectName = result.toString() ;
        }

        return parentPathForObjectName ;
    }
 
    public synchronized boolean suspended() {
        return suspended ;
    }

    public synchronized void suspended( boolean flag ) {
        suspended = flag ;
    }

    public synchronized void register() throws InstanceAlreadyExistsException, 
        MBeanRegistrationException, NotCompliantMBeanException {
        
        if (!registered) {
            if (skeleton().mom().jmxRegistrationDebug()) {
                Exceptions.self.registeringMBean( oname ) ;
            }

            try {
                AccessController.doPrivileged( 
                    new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            server.registerMBean( MBeanImpl.this, oname ) ;
                            return null ;
                        }
                    } ) ;
            } catch (PrivilegedActionException exc) {
                final Throwable e = exc.getCause() ;
                if (e instanceof InstanceAlreadyExistsException) {
                    throw (InstanceAlreadyExistsException)e ;
                } else if (e instanceof MBeanRegistrationException) {
                    throw (MBeanRegistrationException)e ;
                } else if (e instanceof NotCompliantMBeanException) {
                    throw (NotCompliantMBeanException)e ;
                } else {
                    // got an unexpected exception: log it
                    Exceptions.self.unexpectedException( 
                        "MBeanServer.registerMBean", e ) ;
                }
            }

            registered = true ;
        } else {
            Exceptions.self.registerMBeanRegistered( oname ) ;
        }
    }
    
    public synchronized void unregister() throws InstanceNotFoundException, 
        MBeanRegistrationException {
        
        if (registered) {
            if (skeleton().mom().jmxRegistrationDebug()) {
                Exceptions.self.unregisteringMBean( oname ) ;
            }

            registered = false ;

            try {
                AccessController.doPrivileged( 
                    new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            server.unregisterMBean( oname );
                            return null ;
                        }
                    } ) ;
            } catch (PrivilegedActionException exc) {
                final Throwable e = exc.getCause() ;
                if (e instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException)e ;
                } else if (e instanceof MBeanRegistrationException) {
                    throw (MBeanRegistrationException)e ;
                } else {
                    // got an unexpected exception: log it
                    Exceptions.self.unexpectedException( 
                        "MBeanServer.unregisterMBean", e ) ;
                }
            }
        } else {
            Exceptions.self.unregisterMBeanNotRegistered( oname ) ;
        }
    }
    
    // Methods for DynamicMBean

    public Object getAttribute(String attribute) 
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        OperationTracer.clear() ;
	return skel.getAttribute( this, attribute ) ;
    }
    
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
	InvalidAttributeValueException, MBeanException, ReflectionException  {
        OperationTracer.clear() ;
	skel.setAttribute( this, this, attribute ) ;
    }
        
    public AttributeList getAttributes(String[] attributes) {
        OperationTracer.clear() ;
	return skel.getAttributes( this, attributes ) ;
    }
        
    public AttributeList setAttributes(AttributeList attributes) {
        OperationTracer.clear() ;
	return skel.setAttributes( this, this, attributes ) ;
    }
    
    public Object invoke(String actionName, Object params[], String signature[])
	throws MBeanException, ReflectionException  {
        OperationTracer.clear() ;
	return skel.invoke( this, actionName, params, signature ) ;
    }
    
    private static final MBeanNotificationInfo[] 
        ATTRIBUTE_CHANGE_NOTIFICATION_INFO = { new MBeanNotificationInfo(
            new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "An Attribute of this MBean has changed" ) 
    } ;
    
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return ATTRIBUTE_CHANGE_NOTIFICATION_INFO.clone() ;
    }

    public MBeanInfo getMBeanInfo() {
        return skel.getMBeanInfo();
    }
    
    /**********************************************************************
     * Code for dynamic inheritance support: use invoke with reflection to
     * call dynamically inherited classes.
     */
    
    private FacetAccessor facetAccessorDelegate = 
        new FacetAccessorImpl( this ) ;
    
    public <T> T facet(Class<T> cls, boolean debug ) {
        return facetAccessorDelegate.facet( cls, debug ) ;
    }

    public <T> void addFacet(T obj) {
        facetAccessorDelegate.addFacet( obj ) ;
    }

    public void removeFacet( Class<?> cls ) {
        facetAccessorDelegate.removeFacet( cls ) ;
    }

    public Object invoke(Method method, boolean debug, Object... args) {
        return facetAccessorDelegate.invoke( method, debug, args ) ;
    }

    public Collection<Object> facets() {
        return facetAccessorDelegate.facets() ;
    }

    public Object get(Field field, boolean debug) {
        return facetAccessorDelegate.get( field, debug ) ;
    }

    public void set(Field field, Object value, boolean debug) {
        facetAccessorDelegate.set( field, value, debug ) ;
    }
}
