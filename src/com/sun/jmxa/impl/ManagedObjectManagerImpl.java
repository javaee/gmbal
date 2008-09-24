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

import java.util.ResourceBundle ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.WeakHashMap ;
import java.util.Properties ;
import java.util.Hashtable ;
import java.util.Enumeration ;

import java.io.IOException ;

import java.lang.reflect.Type ;
import java.lang.reflect.AnnotatedElement ;

import java.lang.management.ManagementFactory ;

import javax.management.MBeanServer ;
import javax.management.JMException ;
import javax.management.ObjectName ;
import javax.management.NotificationEmitter;

import com.sun.jmxa.generic.Pair ;

import com.sun.jmxa.ManagedObjectManager ;
import com.sun.jmxa.ManagedObject ;
import com.sun.jmxa.Description ;

public class ManagedObjectManagerImpl implements ManagedObjectManagerInternal {
    private final String domain ;
    private ResourceBundle resourceBundle ;
    private MBeanServer server ; 
    private final Map<Object,ObjectName> objectMap ;
    private final Map<ObjectName,Object> objectNameMap ;
    private final Map<Class<?>,DynamicMBeanSkeleton> skeletonMap ;
    private final Map<Type,TypeConverter> typeConverterMap ;

    private static final TypeConverter recursiveTypeMarker = 
        new TypeConverterImpl.TypeConverterPlaceHolderImpl() ;

    public void close() throws IOException {
        for (Map.Entry<Object,ObjectName> entry : objectMap.entrySet()) {
            unregister( entry.getValue() ) ;
        }
        
        objectMap.clear() ;
        objectNameMap.clear() ;
        skeletonMap.clear() ;
        typeConverterMap.clear() ;
        server = null ;
        resourceBundle = null ;
    }
    public synchronized DynamicMBeanSkeleton getSkeleton( Class<?> cls ) {
	DynamicMBeanSkeleton result = skeletonMap.get( cls ) ;	

	if (result == null) {
            Pair<Class<?>,ClassAnalyzer> pair = AnnotationUtil.getClassAnalyzer( 
                cls, ManagedObject.class ) ;
            Class<?> annotatedClass = pair.first() ;
            ClassAnalyzer ca = pair.second() ;

            result = skeletonMap.get( annotatedClass ) ;
            
            if (result == null) {
                result = new DynamicMBeanSkeleton( annotatedClass, ca, this ) ;
            }

	    skeletonMap.put( cls, result ) ;
	}

	return result ;
    }

    public synchronized TypeConverter getTypeConverter( Type type ) {
	TypeConverter result = typeConverterMap.get( type ) ;	
	if (result == null) {
            // Store a TypeConverter impl that throws an exception when acessed.
            // Used to detect recursive types.
            typeConverterMap.put( type, recursiveTypeMarker ) ;

	    result = TypeConverterImpl.makeTypeConverter( type, this ) ;

            // Replace recursion marker with the constructed implementation
	    typeConverterMap.put( type, result ) ;
	}
	return result ;
    }

    public ManagedObjectManagerImpl( String domain ) {
	this.domain = domain ;
	server = ManagementFactory.getPlatformMBeanServer() ;
	objectMap = new HashMap<Object,ObjectName>() ;
	objectNameMap = new HashMap<ObjectName,Object>() ;
	skeletonMap = new WeakHashMap<Class<?>,DynamicMBeanSkeleton>() ;
	typeConverterMap = new WeakHashMap<Type,TypeConverter>() ;
    }

    /** Create another ManagedObjectManager that delegates to this one, but adds some
     * fixed properties to each ObjectName on the register call.
     * Each element in props must be in the "name=value" form.
     * @return The ManagedObjectManager delegate
     * @param mom The ManagedObjectManager to which the result delegates
     * @param extraProps The properties to be added to each object registration
     */
    public static ManagedObjectManager makeDelegate( 
        final ManagedObjectManagerInternal mom, 
	final Map<String,String> extraProps ) {

	return new ManagedObjectManagerInternal() {
            public NotificationEmitter register( Object obj, String... props ) {
                final Map<String,String> map = 
                    new HashMap<String,String>( extraProps ) ;
                addToMap( map, props ) ;
                return register( obj, map ) ;
            }

            public NotificationEmitter register( final Object obj, 
                final Properties props ) {

                final Map<String,String> map = 
                    new HashMap<String,String>( extraProps ) ;
                addToMap( map, props ) ;
                return register( obj, map ) ;
            }

	    public synchronized NotificationEmitter register( final Object obj, 
                final Map<String,String> props )  {

                final Map<String,String> map = 
                    new HashMap<String,String>( extraProps ) ;
                map.putAll( props ) ;
		return mom.register( obj, map ) ;
	    }

	    public void unregister( Object obj ) {
		try {
		    mom.unregister( obj ) ;
		} catch (Exception exc) {
		    throw new IllegalArgumentException( exc ) ;
		}
	    }

	    public ObjectName getObjectName( Object obj ) {
		return mom.getObjectName( obj ) ;
	    }

	    public String getDomain() {
		return mom.getDomain() ;
	    }

	    public Object getObject( ObjectName oname ) {
		return mom.getObject( oname ) ;
	    }

	    public TypeConverter getTypeConverter( Type type ) {
		return mom.getTypeConverter( type ) ;
	    }

	    public void setMBeanServer( MBeanServer server ) {
		mom.setMBeanServer( server ) ;
	    }

	    public MBeanServer getMBeanServer() {
		return mom.getMBeanServer() ;
	    }

            public void setResourceBundle( ResourceBundle rb ) {
                mom.setResourceBundle( rb ) ;
            }

            public ResourceBundle getResourceBundle() {
                return mom.getResourceBundle() ;
            }
            
            public String getDescription( AnnotatedElement element ) {
                return mom.getDescription( element ) ;
            }
            
            public void close() throws IOException {
                mom.close() ;
            }
	} ;
    }

    public static void addToMap( Map<String,String> base, Properties props ) {
        @SuppressWarnings("unchecked")
        Enumeration<String> enumeration = 
            (Enumeration<String>)(props.propertyNames()) ;
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement() ;
            String value = props.getProperty( key ) ;
            base.put( key, value ) ;
        }
    }

    public static void addToMap( Map<String,String> base, String... props ) {
	for (String str : props) {
	    int eqIndex = str.indexOf( "=" ) ;
	    if (eqIndex < 1) {
                // XXX I18N
		throw new IllegalArgumentException( 
		    "All properties must contain an = "
                    + "after the (non-empty) property name" ) ;
            }
	    String name = str.substring( 0, eqIndex ) ;
	    String value = str.substring( eqIndex+1 ) ;
	    base.put( name, value ) ;
	}
    }

    public NotificationEmitter register( Object obj, String... props ) {
        Map<String,String> map = new HashMap<String,String>() ;
        addToMap( map, props ) ;
	return register( obj, map ) ;
    }

    public NotificationEmitter register( final Object obj, 
	final Properties props ) {

        Map<String,String> map = new HashMap<String,String>() ;
        addToMap( map, props ) ;
	return register( obj, map ) ;
    }

    @SuppressWarnings({"unchecked"})
    public synchronized NotificationEmitter register( final Object obj, 
	final Map<String,String> props ) {

	final Class<?> cls = obj.getClass() ;
	final DynamicMBeanSkeleton skel = getSkeleton( cls ) ;
	final String type = skel.getType() ;
	final DynamicMBeanImpl mbean = new DynamicMBeanImpl( skel, obj ) ;
        
        final Map<String,String> fullProps ;
        try {
            fullProps = skel.getObjectNameProperties( obj ) ;
        } catch (Exception exc) {
            throw new RuntimeException( exc ) ;
        }
        
        fullProps.putAll( props ) ;
	fullProps.put( "type", type ) ;

        @SuppressWarnings("unchecked")
        final Hashtable onameProps = new Hashtable( (Map)fullProps ) ;

	ObjectName oname ;
	try {
	    oname = new ObjectName( domain, onameProps ) ;

            if (objectMap.containsKey( obj )) {
                // XXX I18N
                throw new IllegalArgumentException(
                    "Object " + obj + " has already been registered" ) ;
            }
            
            if (objectNameMap.containsKey( oname )) {
                throw new IllegalArgumentException(
                    // XXX I18N
                    "An Object has already been registered with ObjectName "
                    + oname ) ;
            }

            server.registerMBean( mbean, oname ) ;
            
	    objectNameMap.put( oname, obj ) ;
	    objectMap.put( obj, oname ) ;

            return mbean ;
	} catch (JMException exc) {
	    throw new IllegalArgumentException( exc ) ;
	}
    }

    public synchronized void unregister( Object obj ) {
	ObjectName oname = objectMap.get( obj ) ;
	if (oname != null) {
	    try {
		server.unregisterMBean( oname ) ;
	    } catch (Exception exc) {
		throw new IllegalArgumentException( exc ) ;
	    } finally {
		// Make sure obj is removed even if unregisterMBean fails
		objectMap.remove( obj ) ;
		objectNameMap.remove( oname ) ;
	    }
	}
    }

    public synchronized ObjectName getObjectName( Object obj ) {
	return objectMap.get( obj ) ;
    }

    public synchronized Object getObject( ObjectName oname ) {
	return objectNameMap.get( oname ) ;
    }

    public synchronized String getDomain() {
	return domain ;
    }

    public synchronized void setMBeanServer( MBeanServer server ) {
	this.server = server ;
    }

    public synchronized MBeanServer getMBeanServer() {
	return server ;
    }

    public void setResourceBundle( ResourceBundle rb ) {
        this.resourceBundle = rb ;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle ;
    }
    
    public String getDescription( AnnotatedElement element ) {
        Description desc = element.getAnnotation( Description.class ) ;
        String result ;
        if (desc == null) {
            // XXX I18N
            result = "No description available!" ;
        } else {
            result = desc.value() ;
        }
        
        if (resourceBundle != null) {
            result = resourceBundle.getString( result ) ;
        }
        
        return result ;
    }
}

