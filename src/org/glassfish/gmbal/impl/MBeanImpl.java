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

package org.glassfish.gmbal.impl ;

import java.lang.reflect.Field;
import org.glassfish.gmbal.generic.FacetAccessor;
import org.glassfish.gmbal.generic.FacetAccessorImpl;
import java.lang.reflect.Method;
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
import javax.management.DynamicMBean ;
import javax.management.NotificationBroadcasterSupport ;
import javax.management.MBeanNotificationInfo ;
import javax.management.AttributeChangeNotification ;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Map ;
import java.util.HashMap ;

public class MBeanImpl extends NotificationBroadcasterSupport 
    implements FacetAccessor, DynamicMBean {
    
    private boolean registered ;
    private final MBeanSkeleton skel ;
    private final String type ;
    private String name ;
    private ObjectName oname ;
    private MBeanImpl parent ;
    private Map<String,Map<String,MBeanImpl>> children ;

    private Object target ;
    private MBeanServer server ;
    
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
        addFacet( obj ) ;
        addFacet( new AMXImpl( this ) ) ;

        // Note that the construction of an MBean skeleton and
        // facet registration must stay in sync.  The code is currently separated into
        // two places (here and call to new MBeanSkeleton( skel, skel )).
        // This will also be important for dealing with multiple upper bounds.
        this.server = server ;
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
        return name.hashCode() ^ type.hashCode() ^ parent.hashCode() ;
    }
 
    @Override
    public String toString() {

        return "MBeanImpl[skel=" + skel
            + ",type=" + type + ",name=" + name
            + ",oname=" + oname ;
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
        Map<String,MBeanImpl> map = children.get( child.type() ) ;
        if (map == null) {
            map = new HashMap<String,MBeanImpl>() ;
            children.put( child.type(), map ) ;
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
 
    private void restNameHelper( StringBuilder sb, MBeanImpl mb ) {
        if (mb.parent() != null) {
            restNameHelper( sb, mb.parent() ) ;
            sb.append( '/' ) ;
        } 

        sb.append( mb.type() ) ;
	if (!name.equals("")) {
            sb.append( '[' ) ;
	    sb.append( name ) ;
            sb.append( ']' ) ;
	}
    }

    public synchronized String restName() {
        StringBuilder sb = new StringBuilder( 60 ) ;
        restNameHelper( sb, this ) ;
        return sb.toString() ;
    }
 
    public synchronized void register() throws InstanceAlreadyExistsException, 
        MBeanRegistrationException, NotCompliantMBeanException {
        
        if (!registered) {
            server.registerMBean( this, oname ) ;
            registered = true ;
        }
    }
    
    public synchronized void unregister() throws InstanceNotFoundException, 
        MBeanRegistrationException {
        
        if (registered) {
            server.unregisterMBean( oname );
            registered = false ;
        }
    }
    
    // Methods for DynamicMBean

    public Object getAttribute(String attribute) 
        throws AttributeNotFoundException, MBeanException, ReflectionException {

	return skel.getAttribute( this, attribute ) ;
    }
    
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
	InvalidAttributeValueException, MBeanException, ReflectionException  {

	skel.setAttribute( this, this, attribute ) ;
    }
        
    public AttributeList getAttributes(String[] attributes) {
	return skel.getAttributes( this, attributes ) ;
    }
        
    public AttributeList setAttributes(AttributeList attributes) {
	return skel.setAttributes( this, this, attributes ) ;
    }
    
    public Object invoke(String actionName, Object params[], String signature[])
	throws MBeanException, ReflectionException  {

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
