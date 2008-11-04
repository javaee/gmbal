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

package com.sun.jmxa.impl ;

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

public class MBeanImpl extends NotificationBroadcasterSupport implements DynamicMBean {
    private MBeanSkeleton skel ;
    private String type ;
    private String name ;
    private ObjectName oname ;
    private MBeanImpl parent ;
    private Map<String,Map<String,MBeanImpl>> children ;

    private Object target ;
    private MBeanServer server ;
    
    public MBeanImpl( final MBeanSkeleton skel, 
        final Object obj, final MBeanServer server,
        final String type, final String name ) {

	this.skel = skel ;
        this.type = type ;
        this.name = name ;
        this.oname = null ;
        this.parent = null ;
        this.children = new HashMap<String,Map<String,MBeanImpl>>() ;
	this.target = obj ;
        this.server = server ;
    }
        
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true ;
        }
        
        if (!(obj instanceof MBeanImpl)) {
            return false ;
        }
        
        MBeanImpl other = (MBeanImpl)obj ;
        
        return parent == other.parent &&
            name.equals( other.name ) &&
            type.equals( other.type ) ;
    }
    
    public int hashCode() {
        return name.hashCode() ^ type.hashCode() ^ parent.hashCode() ;
    }
 
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
    
    public String name() {
        return name ;
    }

    public synchronized ObjectName objectName() {
        return oname ;
    }
    
    public synchronized void objectName( ObjectName oname ) {
        this.oname = oname ;
    }

    public MBeanImpl parent() {
        return parent ;
    }
   
    public void parent( MBeanImpl entity ) {
        if (parent == null) {
            parent = entity ;
        } else {
            throw new IllegalArgumentException(
                "Cannot set parent to " + entity 
                + " this node already has a parent" ) ;
        }
    }

    Map<String,Map<String,MBeanImpl>> children() {
        return children ;
    }
   
    void addChild( MBeanImpl child ) {
        child.parent( this ) ;
        Map<String,MBeanImpl> map = children.get( child.type() ) ;
        if (map == null) {
            map = new HashMap<String,MBeanImpl>() ;
            children.put( child.type(), map ) ;
        }
        
        map.put( child.name(), child) ;
    }
   
    void removeChild( MBeanImpl child ) {
        Map<String,MBeanImpl> map = children.get( child.type() ) ;
        if (map != null) {
            map.remove( child.name() ) ;
            if (map.size() == 0) {
                children.remove( child.type() ) ;
            }
        }
    }
 
    private void restNameHelper( StringBuilder sb, MBeanImpl mb ) {
        if (mb != null) {
            restNameHelper( sb, mb.parent ) ;
            if (sb.length() > 0) {
                sb.append( ',' ) ;
            }
            
            sb.append( mb.type() ) ;
            sb.append( '=' ) ;
            sb.append( mb.name() ) ;
        } 
    }

    public String restName() {
        StringBuilder sb = new StringBuilder( 60 ) ;
        restNameHelper( sb, this ) ;
        return sb.toString() ;
    }
 
    public void register() throws InstanceAlreadyExistsException, 
        MBeanRegistrationException, NotCompliantMBeanException {
        
        server.registerMBean( this, oname ) ;
    }
    
    public void deregister() throws InstanceNotFoundException, 
        MBeanRegistrationException {
        
        server.unregisterMBean( oname );
    }
    
    // Methods for DynamicMBean

    public Object getAttribute(String attribute) 
        throws AttributeNotFoundException, MBeanException, ReflectionException {

	return skel.getAttribute( target, attribute ) ;
    }
    
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
	InvalidAttributeValueException, MBeanException, ReflectionException  {

	skel.setAttribute( this, target, attribute ) ;
    }
        
    public AttributeList getAttributes(String[] attributes) {
	return skel.getAttributes( target, attributes ) ;
    }
        
    public AttributeList setAttributes(AttributeList attributes) {
	return skel.setAttributes( this, target, attributes ) ;
    }
    
    public Object invoke(String actionName, Object params[], String signature[])
	throws MBeanException, ReflectionException  {

	return skel.invoke( target, actionName, params, signature ) ;
    }
    
    private static final MBeanNotificationInfo[] 
        ATTRIBUTE_CHANGE_NOTIFICATION_INFO = { new MBeanNotificationInfo(
            new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "An Attribute of this MBean has changed" ) 
    } ;
    
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return ATTRIBUTE_CHANGE_NOTIFICATION_INFO ;
    }

    public MBeanInfo getMBeanInfo() {
        return skel.getMBeanInfo();
    }
}
