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

package org.glassfish.gmbal.impl;

import org.glassfish.gmbal.generic.FacetAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.glassfish.external.amx.AMX;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.generic.MethodMonitor;
import org.glassfish.gmbal.generic.MethodMonitorFactory;

/** Represents the collection of DynamicMBeanImpls that we have registered with
 * a ManagedObjectManager.
 *
 * @author ken
 */
public class MBeanTree {
    private Object root = null ;
    private MBeanImpl rootEntity = null ;
    private Map<Object,MBeanImpl> objectMap ;
    private Map<ObjectName,Object> objectNameMap ;
    private String domain ;
    private ObjectName rootParentName ;
    private String rootParentPrefix ;
    private String nullParentsParentPath ;
    private String typeString ; // What string is used for the type of the 
                                // type name/value pair?
    private ManagedObjectManagerInternal mom ;
    private MethodMonitor mm ;
    private JMXRegistrationManager jrm ;
    private boolean suppressReport = false ;
    
    private void addToObjectMaps( MBeanImpl mbean ) {
        ObjectName oname = mbean.objectName() ;
        for (Object obj : mbean.facets() ) {
            objectMap.put( obj, mbean ) ;
        }
        objectNameMap.put( oname, mbean.target() ) ;
    }
    
    private void removeFromObjectMaps( MBeanImpl mbean ) {
        ObjectName oname = mbean.objectName() ;
        for (Object obj : mbean.facets() ) {
            objectMap.remove( obj ) ;
        }
        
        objectNameMap.remove( oname ) ;
    }
    
    public synchronized GmbalMBean setRoot( Object root, String rootName ) {
        // Now register the root MBean.
        MBeanImpl rootMB = mom.constructMBean( null, root, rootName ) ;
        
        ObjectName oname ;
        try {
            oname = objectName(null, rootMB.type(), rootMB.name());
        } catch (MalformedObjectNameException ex) {
            throw Exceptions.self.noRootObjectName(ex) ;
        }
        rootMB.objectName( oname ) ;

        addToObjectMaps( rootMB ) ;
        this.root = root ;
        rootEntity = rootMB ;
        boolean success = false ;

        try {
            jrm.setRoot( rootMB )  ;
            success = true ;
        } catch (InstanceAlreadyExistsException ex) {
            if (suppressReport)
                return null ;
            else
                throw Exceptions.self.rootRegisterFail( ex, oname ) ;
        } catch (Exception ex) {
            throw Exceptions.self.rootRegisterFail( ex, oname ) ;
        } finally {
            if (!success) {
                removeFromObjectMaps(rootMB);
                this.root = null ;
                rootEntity = null ;
            }
        }

        return rootMB ;
    }
    
    public synchronized Object getRoot() {
        return root ;
    }

    private String parentPath( final ObjectName rootParentName ) {
        final String pp = rootParentName.getKeyProperty( AMX.PARENT_PATH_KEY ) ;
        final String type = rootParentName.getKeyProperty( AMX.TYPE_KEY ) ;
        final String name = rootParentName.getKeyProperty( AMX.NAME_KEY ) ;

        if (pp == null) {
            throw Exceptions.self.ppNullInRootParent() ;
        }

        if (type == null) {
            throw Exceptions.self.typeNullInRootParent() ;
        }

        String prefix ;
        if (pp.equals( "/" )) {
            prefix = pp ;
        } else {
            if (pp.endsWith("/" )) {
                prefix = pp ;
            } else {
                prefix = pp + "/" ;
            }
        }

        if (name == null) {
            return prefix + type ;
        } else {
            return prefix + type + '[' + name + ']' ;
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
            nullParentsParentPath =  AMX.PARENT_PATH_KEY + "=/," ;
        } else {
            rootParentPrefix = parentPath( rootParentName ) ;
            nullParentsParentPath = AMX.PARENT_PATH_KEY + "="
                + rootParentPrefix + "," ;
        }

        this.typeString = typeString ;
        objectMap = new HashMap<Object,MBeanImpl>() ;
        objectNameMap = new HashMap<ObjectName,Object>() ;
        mm = MethodMonitorFactory.makeStandard( getClass() ) ;
        jrm = new JMXRegistrationManager( mom, rootParentName ) ;
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

    static String getQuotedName( String name ) {
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

                case ':' :
                case '=' :
                case ',' :
                    needsQuotes = true ;
                    break ;
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

    private Map<String,String> typePartMap = new WeakHashMap<String,String>() ;

    private synchronized String getTypePart( String type ) {
        String result = typePartMap.get( type ) ;
        if (result == null) {
            StringBuilder sb = new StringBuilder() ;
            sb.append( typeString ) ;
            sb.append( "=" ) ;
            sb.append( getQuotedName( type ) ) ;
            result = sb.toString() ;

            typePartMap.put( type, result ) ;
        }

        return result ;
    }

    public synchronized ObjectName objectName( MBeanImpl parent,
        String type, String name ) 
        throws MalformedObjectNameException {
        mm.enter( mom.registrationDebug(), "objectName", parent,
            type, name ) ;

        ObjectName oname = null ;

        try {
            if (parent != null) {
                checkCorrectRoot( parent ) ;
            }

            StringBuilder result = new StringBuilder() ;

            result.append( domain ) ;
            result.append( ":" ) ;

            // pp
            String ppPart ;
            if (parent == null) {
                ppPart = nullParentsParentPath ;
            } else {
                ppPart = parent.getParentPathPart( rootParentPrefix ) ;
            }

            mm.info( mom.registrationDebug(), "ppPart", ppPart ) ;
            result.append( ppPart ) ;

            // type
            String typePart = getTypePart( type ) ;
            mm.info( mom.registrationDebug(), "typePart", typePart ) ;
            result.append( typePart ) ;

            // name: this is not a good candidate for caching
            if (name.length() > 0) {
                result.append( ',') ;
                result.append( AMX.NAME_KEY ) ;
                result.append( "=" ) ;
                result.append( getQuotedName( name ) ) ;
            }

            String on = result.toString() ;
            try {
                oname = new ObjectName( on ) ;
            } catch (MalformedObjectNameException exc) {
                throw Exceptions.self.malformedObjectName(exc, on) ;
            }
        } finally {
            mm.exit( mom.registrationDebug(), oname ) ;
        }

        return oname ;
    }

    public MBeanImpl getParentEntity( Object parent ) {
        if (parent == null) {
            throw Exceptions.self.parentCannotBeNull() ;
        }

        MBeanImpl parentEntity ;

        parentEntity = objectMap.get( parent ) ;
        if (parentEntity == null) {
            throw Exceptions.self.parentNotFound(parent) ;
        }

        return parentEntity ;
    }

    public synchronized GmbalMBean register(
        final MBeanImpl parentEntity,
        final Object obj, 
        final MBeanImpl mb ) throws InstanceAlreadyExistsException, 
        MBeanRegistrationException, NotCompliantMBeanException, 
        MalformedObjectNameException {
        
        mm.enter( mom.registrationDebug(), "register", parentEntity, obj, mb ) ;
        
        try { 
            MBeanImpl oldMB = objectMap.get( obj ) ;
            if (oldMB != null) {
                throw Exceptions.self.objectAlreadyRegistered(obj, oldMB) ;
            }
            
            ObjectName oname = objectName( parentEntity, mb.type(), 
                mb.name() ) ;
            mb.objectName( oname ) ;

            Object oldObj = objectNameMap.get( oname ) ;
            if (oldObj != null) {
                throw Exceptions.self.objectAlreadyRegistered( obj,
                    objectMap.get( oldObj ) ) ;
            }
        
            addToObjectMaps( mb ) ;

            parentEntity.addChild( mb ) ; 

            jrm.register( mb ) ;

            return mb ;
        } finally {
            mm.exit( mom.registrationDebug() ) ;
        }
    }
    
    public synchronized void unregister( Object obj ) 
        throws InstanceNotFoundException, MBeanRegistrationException {
        if (obj == root) {
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
        // A user may be looking for the ObjectName of a GmbalMBean that
        // was returned from a register call.  If that is the case,
        // obj should be an instance of MBeanImpl, and we can go directly to
        // the ObjectName.
        if (obj instanceof MBeanImpl) {
            return ((MBeanImpl)obj).objectName() ;
        }

        // obj might be a POJO that was used to create an MBean: the normal case.
        MBeanImpl result = objectMap.get(obj);
        if (result != null) {
            return result.objectName() ;
        } else {
            return null ;
        }
    }
    
    public synchronized Object getObject( ObjectName oname ) {
        return objectNameMap.get( oname ) ;
    }
    
    public synchronized MBeanImpl getMBeanImpl( Object obj ) {
        return objectMap.get( obj ) ;
    }
    
    public synchronized void clear(){
        if (root != null) {
            try {
                unregister(root);
            } catch (InstanceNotFoundException ex) {
                Exceptions.self.shouldNotHappen( ex ) ;
            } catch (MBeanRegistrationException ex) {
                Exceptions.self.shouldNotHappen( ex ) ;
            }
        }
        
        objectMap.clear() ;
        objectNameMap.clear() ;
        rootEntity = null ;
        jrm.clear() ;
    }

    public ObjectName getRootParentName() {
        return rootParentName ;
    }

    synchronized void setSuppressDuplicateSetRootReport(boolean suppressReport) {
        this.suppressReport = suppressReport ;
    }
}
