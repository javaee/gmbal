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

package com.sun.jmxa.impl;

import com.sun.jmxa.generic.DprintUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

/** Represents the collection of DynamicMBeanImpls that we have registered with
 * a ManagedObjectManager.
 *
 * @author ken
 */
public class MBeanTree {
    private MBeanImpl rootEntity ;
    private Map<Object,MBeanImpl> objectMap ;
    private Map<ObjectName,Object> objectNameMap ;
    private String domain ;
    private String rootParentName ;
    private String typeString ; // What string is used for the type of the 
                                // type name/value pair?
    private ManagedObjectManagerInternal mom ;
    private DprintUtil dputil ;
    
    public MBeanTree( final ManagedObjectManagerInternal mom,
        final String domain, 
        final String rootParentName,
        final String typeString, 
        final Object root,
        final String rootName ) {

        this.mom = mom ;
        this.domain = domain ;
        this.rootParentName = rootParentName ;
        this.typeString = typeString ;
        objectMap = new HashMap<Object,MBeanImpl>() ;
        objectNameMap = new HashMap<ObjectName,Object>() ;
        dputil = new DprintUtil( this ) ;

        // Now register the root MBean.
        MBeanImpl rootMB = mom.constructMBean( root, rootName ) ;
        
        ObjectName oname ;
        try {
            oname = objectName(null, rootMB.type(), rootMB.name());
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException( 
                "Could not construct ObjectName for root", 
                ex ) ;            
        }
        rootMB.objectName( oname ) ;
        
        objectMap.put( root, rootMB ) ;
        objectNameMap.put( oname, root ) ;
        
        try {
            rootMB.register();
        } catch (JMException ex) {
            throw new IllegalArgumentException( "Could not register root", 
                ex ) ;
        }
    }
    
    private boolean notEmpty( String str ) {
        return str != null && str.length() > 0 ;
    }
    
    private void checkCorrectRoot( MBeanImpl entity ) {
        MBeanImpl parent = entity.parent() ;
        while (parent != null) {
            if (parent == rootEntity) {
                return ;
            }
        }
        
        throw new IllegalArgumentException( "Entity " + entity 
            + " is not part of this EntityTree" ) ;
    }
    
    public ObjectName objectName( MBeanImpl parent,
        String type, String name ) 
        throws MalformedObjectNameException {
        
        // XXX Should we cache the ObjectName in the entity itself?
        checkCorrectRoot( parent ) ;
        
        StringBuilder result = new StringBuilder() ;
        result.append( domain ) ;
        result.append( ":" ) ;
        if (notEmpty( rootParentName )) {
            result.append( rootParentName ) ;
            result.append( "," ) ;
        }
        
        if (parent != null) {
            String restName = parent.restName() ;
            if (notEmpty( restName )) {
                result.append( restName ) ;
                result.append( "," ) ;
            }
        }
        
        result.append( typeString ) ;
        result.append( "=" ) ;
        result.append( type ) ;
        
        result.append( "name" ) ;
        result.append( "=" ) ;
        result.append( name ) ;
        
        return new ObjectName( result.toString() ) ; 
    }
    
    public NotificationEmitter register( 
        final Object parent, 
        final Object obj, 
        final MBeanImpl mb ) throws InstanceAlreadyExistsException, 
        MBeanRegistrationException, NotCompliantMBeanException, 
        MalformedObjectNameException {
        
        if (mom.registrationDebug()) {
            dputil.enter( "register", 
                "parent=", parent,
                "obj=", obj,
                "mb=", mb ) ;
        }
        
        try { 
            MBeanImpl oldMB = objectMap.get( obj ) ;
            if (oldMB != null) {
                String msg = "Object " + obj + " is already registered as " 
                    + oldMB ;
                
                if (mom.registrationDebug()) {
                    dputil.info( msg ) ;
                }
                
                // XXX I18N
                throw new IllegalArgumentException( msg ) ;
            }
            
            MBeanImpl parentEntity = objectMap.get( parent ) ;
            if (parentEntity == null) {
                String msg = "parent object " + parent + " not found" ;
                if (mom.registrationDebug()) {
                    dputil.info( msg ) ;
                }
                
                throw new IllegalArgumentException( msg ) ;
            }
            
            ObjectName oname = objectName( parentEntity, mb.type(), 
                mb.name() ) ;
            mb.objectName( oname ) ;
        
            objectMap.put( obj, mb ) ;
	    objectNameMap.put( oname, obj ) ;

            parentEntity.addChild( mb ) ; 

            mb.register() ;

            return mb ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public void unregister( Object obj ) {
    }
    
    public ObjectName getObjectName( Object obj ) {
        MBeanImpl result = objectMap.get(obj);
        return result.objectName() ;
    }
    
    public Object getObject( ObjectName oname ) {
        return objectNameMap.get( oname ) ;
    }
    
    public MBeanImpl getMBeanImpl( Object obj ) {
        return objectMap.get( obj ) ;
    }
    public void clear(){
        for (MBeanImpl entity : objectMap.values()) {
            try {
                entity.deregister();
            } catch (JMException ex) {
                // XXX log this, but we are cleaning up, so
                // no other action.
            }
        }
        
        objectMap.clear() ;
        objectNameMap.clear() ;
        rootEntity = null ;
    }
}
