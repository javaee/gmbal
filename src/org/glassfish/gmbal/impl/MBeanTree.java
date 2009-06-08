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

package org.glassfish.gmbal.impl;

import org.glassfish.gmbal.generic.DprintUtil;
import org.glassfish.gmbal.generic.FacetAccessor;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import org.glassfish.gmbal.GmbalMBean;

/** Represents the collection of DynamicMBeanImpls that we have registered with
 * a ManagedObjectManager.
 *
 * XXX Need to get some benchmarks for registration cost.  This should help
 * to determine whether we need to enable/disable MBean registration with the
 * MBeanServer.
 *
 * @author ken
 */
public class MBeanTree {
    private boolean rootIsSet = false ;
    private Object root ;
    private MBeanImpl rootEntity ;
    private Map<Object,MBeanImpl> objectMap ;
    private Map<ObjectName,Object> objectNameMap ;
    private String domain ;
    private ObjectName rootParentName ;
    private String rootParentPrefix ;
    private String typeString ; // What string is used for the type of the 
                                // type name/value pair?
    private ManagedObjectManagerInternal mom ;
    private DprintUtil dputil ;
    private JMXRegistrationManager jrm ;
    
    private void addToObjectMaps( MBeanImpl mbean ) {
        ObjectName oname = mbean.objectName() ;
        for (Object obj : mbean.facets() ) {
            objectMap.put( obj, mbean ) ;
        }
        objectNameMap.put( oname, mbean ) ;
    }
    
    private void removeFromObjectMaps( MBeanImpl mbean ) {
        ObjectName oname = mbean.objectName() ;
        for (Object obj : mbean.facets() ) {
            objectMap.remove( obj ) ;
        }
        
        objectNameMap.remove( oname ) ;
    }
    
    public synchronized GmbalMBean setRoot( Object root, String rootName ) {
        if (rootIsSet) {
            throw Exceptions.self.rootAlreadySet() ;
        } else {
            rootIsSet = true ;
        }
        
        // Now register the root MBean.
        MBeanImpl rootMB = mom.constructMBean( root, rootName ) ;
        
        ObjectName oname ;
        try {
            oname = objectName(null, rootMB.type(), rootMB.name());
        } catch (MalformedObjectNameException ex) {
            throw Exceptions.self.noRootObjectName(ex) ;
        }
        rootMB.objectName( oname ) ;
        
        addToObjectMaps( rootMB ) ;
        
        try {
            rootMB.register();
        } catch (JMException ex) {
            throw Exceptions.self.rootRegisterFail( ex ) ;
        }
        
        this.root = root ;
        rootEntity = rootMB ;
        return rootMB ;
    }
    
    public synchronized Object getRoot() {
        if (rootIsSet) {
            return root ;
        } else {
            throw Exceptions.self.rootNotSet() ;
        }   
    }

    private String parentPath( final ObjectName rootParentName ) {
        final String pp = rootParentName.getKeyProperty("pp") ;
        final String type = rootParentName.getKeyProperty("type") ;
        final String name = rootParentName.getKeyProperty("name") ;

        if (pp == null) {
            Exceptions.self.ppNullInRootParent() ;
        }

        if (type == null) {
            Exceptions.self.typeNullInRootParent() ;
        }

        if (name == null) {
            return pp + '/' + type ;
        } else {
            return pp + '/' + type + '[' + name + ']' ;
        }
    }

    public MBeanTree( final ManagedObjectManagerInternal mom,
        final String domain, 
        final ObjectName rootParentName,
        final String typeString ) {
        
        this.mom = mom ;
        this.domain = domain ;
        this.rootParentName = rootParentName ;
        if (rootParentName == null) {
            rootParentPrefix = null ;
        } else {
            rootParentPrefix = parentPath( rootParentName ) ;
        }

        this.typeString = typeString ;
        objectMap = new HashMap<Object,MBeanImpl>() ;
        objectNameMap = new HashMap<ObjectName,Object>() ;
        dputil = new DprintUtil( getClass() ) ;
        jrm = new JMXRegistrationManager() ;
    }

    synchronized void suspendRegistration() {
        jrm.suspendRegistration();
    }

    synchronized void resumeRegistration() {
        jrm.resumeRegistration() ;
    }

    public synchronized FacetAccessor getFacetAccessor(Object obj) {
        return objectMap.get( obj ) ;
    }
    
    private void checkCorrectRoot( MBeanImpl entity ) {
        MBeanImpl current = entity ;
        do {
            if (current == rootEntity) {
                return ;
            }
            
            current = current.parent() ;
        } while (current != null) ;
        
        throw Exceptions.self.notPartOfThisTree(entity) ;
    }

    private String getQuotedName( String name ) {
        // Adapted from the ObjectName.quote method.
        // Here we only quote if needed, and save a lot of
        // extra processing for String.equals or regex.

        // Allow a little extra space for quoting.  buf will re-size
        // if necessary.
        final StringBuilder buf = new StringBuilder( name.length() + 10 );
        buf.append( '"' ) ;
        final int len = name.length();
        boolean needsQuotes = false ;
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            switch (c) {
                case '\n':
                    c = 'n';
                    buf.append('\\');
                    needsQuotes = true ;
                    break;

                case '\\':
                case '\"':
                case '*':
                case '?':
                    buf.append('\\');
                    needsQuotes = true ;
                    break;
            }
            buf.append(c);
        }

        if (needsQuotes) {
            buf.append('"');
            return buf.toString();
        } else {
            return name ;
        }
    }

    public synchronized ObjectName objectName( MBeanImpl parent,
        String type, String name ) 
        throws MalformedObjectNameException {
        if (mom.registrationDebug()) {
            dputil.enter( "objectName", "parent=", parent, 
                "type=", type, "name=", name ) ;
        }

        ObjectName oname = null ;

        try {
            if (parent != null) {
                checkCorrectRoot( parent ) ;
            }

            StringBuilder result = new StringBuilder() ;

            result.append( domain ) ;
            result.append( ":" ) ;

            if (mom.registrationDebug()) {
                dputil.info( "rootParentPrefix=", rootParentPrefix ) ;
                if (parent != null) {
                    dputil.info( "parent.restName()=", parent.restName() ) ;
                } else {
                    dputil.info( "parent is null" ) ;
                }
            }
            
            // pp
            result.append( "pp" ) ;
            result.append( "=" ) ;

            if ((rootParentPrefix == null) && (parent == null)) {
                result.append( '/' ) ;
            } else {
                if (rootParentPrefix != null) {
                    result.append( rootParentPrefix ) ;
                }

                if (parent != null) {
                    result.append( '/' ) ;
                    result.append( getQuotedName( parent.restName() ) ) ;
                }
            }

            result.append( ',' ) ;

            // type
            result.append( typeString ) ;
            result.append( "=" ) ;
            result.append( getQuotedName( type ) ) ;

            // name
            if (name.length() > 0) {
                result.append( ',') ;
                result.append( "name" ) ;
                result.append( "=" ) ;
                result.append( getQuotedName( name ) ) ;
            }

            oname =  new ObjectName( result.toString() ) ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( oname ) ;
            }
        }

        return oname ;
    }
    
    public synchronized GmbalMBean register(
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
            if (parent == null) {
                throw Exceptions.self.parentCannotBeNull() ;
            }
            
            MBeanImpl oldMB = objectMap.get( obj ) ;
            if (oldMB != null) {
                String msg = Exceptions.self.objectAlreadyRegistered(obj, oldMB) ;
                
                if (mom.registrationDebug()) {
                    dputil.info( msg ) ;
                }
                
                throw new IllegalArgumentException( msg ) ;
            }
            
            MBeanImpl parentEntity ;

            parentEntity = objectMap.get( parent ) ;
            if (parentEntity == null) {
                String msg = Exceptions.self.parentNotFound(parent) ;
                if (mom.registrationDebug()) {
                    dputil.info( msg ) ;
                }
                throw new IllegalArgumentException( msg ) ;
            }
            
            ObjectName oname = objectName( parentEntity, mb.type(), 
                mb.name() ) ;
            mb.objectName( oname ) ;
        
            addToObjectMaps( mb ) ;

            parentEntity.addChild( mb ) ; 

            jrm.register( mb ) ;

            return mb ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public synchronized void unregister( Object obj ) 
        throws InstanceNotFoundException, MBeanRegistrationException {
        if (obj == root) {
            rootIsSet = false ;
            root = null ;
            rootEntity = null ;
        }
        
        MBeanImpl mb = objectMap.get( obj ) ;
        if (mb == null) {
            throw Exceptions.self.objectNotFound( obj ) ;
        }

        for (Map<String,MBeanImpl> nameToMBean : mb.children().values() ) {
            for (MBeanImpl child : nameToMBean.values() ) {
                unregister( child.target()) ;
            }
        }

        removeFromObjectMaps( mb ) ;
        jrm.unregister( mb ) ;
        
        if (mb.parent() != null) {
            mb.parent().removeChild( mb ) ;
        }
    }
    
    public synchronized ObjectName getObjectName( Object obj ) {
        MBeanImpl result = objectMap.get(obj);
        return result.objectName() ;
    }
    
    public synchronized Object getObject( ObjectName oname ) {
        return objectNameMap.get( oname ) ;
    }
    
    public synchronized MBeanImpl getMBeanImpl( Object obj ) {
        return objectMap.get( obj ) ;
    }
    
    public synchronized void clear(){
        if (rootIsSet) {
            try {
                unregister(root);
            } catch (InstanceNotFoundException ex) {
                throw Exceptions.self.shouldNotHappen( ex ) ;
            } catch (MBeanRegistrationException ex) {
                throw Exceptions.self.shouldNotHappen( ex ) ;
            }
        }
        
        objectMap.clear() ;
        objectNameMap.clear() ;
        rootEntity = null ;
    }

    public ObjectName getRootParentName() {
        return rootParentName ;
    }
}
