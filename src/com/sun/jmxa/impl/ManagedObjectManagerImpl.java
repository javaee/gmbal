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
import java.util.List ;
import java.util.ArrayList ;

import java.io.IOException ;

import java.lang.reflect.Type ;
import java.lang.reflect.AnnotatedElement ;

import java.lang.annotation.Annotation ;

import java.lang.management.ManagementFactory ;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer ;
import javax.management.JMException ;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName ;
import javax.management.NotificationEmitter;

import com.sun.jmxa.generic.Pair ;
import com.sun.jmxa.generic.Algorithms ;

import com.sun.jmxa.ManagedObject ;
import com.sun.jmxa.Description ;
import com.sun.jmxa.IncludeSubclass ;
import com.sun.jmxa.InheritedAttribute ;
import com.sun.jmxa.InheritedAttributes ;
import com.sun.jmxa.generic.DprintUtil;
import com.sun.jmxa.generic.ObjectUtility;
import com.sun.jmxa.generic.Predicate;
import com.sun.jmxa.generic.UnaryFunction;
import java.util.Arrays;

public class ManagedObjectManagerImpl implements ManagedObjectManagerInternal {
    private final String domain ;
    private ResourceBundle resourceBundle ;
    private MBeanServer server ; 
    private final Map<Object,ObjectName> objectMap ;
    private final Map<ObjectName,Object> objectNameMap ;
    private final Map<Object,DynamicMBeanImpl> objectMBeanMap ;
    private final Map<Class<?>,DynamicMBeanSkeleton> skeletonMap ;
    private final Map<Type,TypeConverter> typeConverterMap ;
    private final Map<AnnotatedElement, Map<Class, Annotation>> addedAnnotations ;
    private final List<String> defaultObjectNameProps ;
    private DprintUtil dputil = null ;

    private static final TypeConverter recursiveTypeMarker = 
        new TypeConverterImpl.TypeConverterPlaceHolderImpl() ;

    public void close() throws IOException {
        if (debug()) {
            dputil.enter( "close" ) ;
        }
        
        try {
            for (Map.Entry<Object,ObjectName> entry : objectMap.entrySet()) {
                unregister( entry.getValue() ) ;
            }

            objectMap.clear() ;
            objectNameMap.clear() ;
            objectMBeanMap.clear() ;
            skeletonMap.clear() ;
            typeConverterMap.clear() ;
            server = null ;
            resourceBundle = null ;
            defaultObjectNameProps.clear() ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }
    
    public synchronized DynamicMBeanSkeleton getSkeleton( Class<?> cls ) {
        if (debug()) {
            dputil.enter( "getSkeleton", cls ) ;
        }
        
        try {
            DynamicMBeanSkeleton result = skeletonMap.get( cls ) ;

            if (result == null) {
                if (debug()) {
                    dputil.dprint( "creating new Skeleton" ) ;
                }
                
                Pair<Class<?>,ClassAnalyzer> pair = getClassAnalyzer( 
                    cls, ManagedObject.class ) ;
                Class<?> annotatedClass = pair.first() ;
                ClassAnalyzer ca = pair.second() ;

                result = skeletonMap.get( annotatedClass ) ;

                if (result == null) {
                    result = new DynamicMBeanSkeleton( annotatedClass, ca, this ) ;
                }

                skeletonMap.put( cls, result ) ;
            }
            
            if (debug()) {
                dputil.dprint( "Skeleton=" + dumpSkeleton( result ) ) ;
            }
            
            return result ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized TypeConverter getTypeConverter( Type type ) {
        if (debug()) {
            dputil.enter( "getTypeConverter", type ) ;
        }
        
        try {
            TypeConverter result = typeConverterMap.get( type ) ;	
            if (result == null) {
                if (debug()) {
                    dputil.dprint( "Creating new TypeConverter" ) ;
                }
            
                // Store a TypeConverter impl that throws an exception when acessed.
                // Used to detect recursive types.
                typeConverterMap.put( type, recursiveTypeMarker ) ;

                result = TypeConverterImpl.makeTypeConverter( type, this ) ;

                // Replace recursion marker with the constructed implementation
                typeConverterMap.put( type, result ) ;
            }
            return result ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }

    public ManagedObjectManagerImpl( String domain, List<String> defProps ) {
	this.domain = domain ;
	server = ManagementFactory.getPlatformMBeanServer() ;
	objectMap = new HashMap<Object,ObjectName>() ;
	objectNameMap = new HashMap<ObjectName,Object>() ;
        objectMBeanMap = new HashMap<Object,DynamicMBeanImpl>() ;
	skeletonMap = new WeakHashMap<Class<?>,DynamicMBeanSkeleton>() ;
	typeConverterMap = new WeakHashMap<Type,TypeConverter>() ;
        addedAnnotations = 
            new HashMap<AnnotatedElement, Map<Class, Annotation>>() ;
        defaultObjectNameProps = new ArrayList<String>( defProps ) ;
    }

    public NotificationEmitter register( Object obj, String... props ) {
        Map<String,String> map = new HashMap<String,String>() ;
	return register( obj, Arrays.asList( props ) ) ;
    }

    private ObjectName makeObjectName( final Object obj, 
        final DynamicMBeanSkeleton skel, 
        final List<String> props ) throws MalformedObjectNameException {
        
        if (debug()) {
            dputil.enter( "makeObjectName" ) ;
        }
        
        // Construct the key/value pairs for the ObjectName
        // ObjectName syntax:
        // domain:key=value,...
        // where domain is any string not containing * ? or :
        // where key is any string not containing : = ? * " or ,
        // where value has been processed by ObjectName.quote
        // Confusingly, the spec for ObjectName says that order does not
        // matter, but order greatly affects the behavior of jconsole.
        // However, it appears that order is presered if new ObjectName(String)
        // is used: the toString() method should provide all properties in
        // the same order.
        try {
            List<String> oknProps = skel.getObjectNameProperties(obj) ;
            if (debug()) {
                dputil.dprint( "oknProps=" + oknProps ) ;
            }
            
            List<String> fullProps = new ArrayList<String>() ;
            StringBuilder objname = new StringBuilder() ;
            objname.append( domain ) ;
            objname.append( ':' ) ;

            objname.append( "type=" ) ;
            objname.append( skel.getType() ) ;

            for (String str : defaultObjectNameProps) {
                objname.append( ',' ) ;
                objname.append( str ) ;
            }

            for (String str : props) {
                objname.append( ',' ) ;
                objname.append( str ) ;
            }        

            for (String str : oknProps ) {
                objname.append( ',' ) ;
                objname.append( str ) ;
            }

            if (debug()) {
                dputil.dprint( "objname=" + objname ) ;
            }
            
            return new ObjectName( objname.toString() ) ;
        } finally {
            dputil.exit() ;
        }
    }
   
    @SuppressWarnings("unchecked")
    public synchronized NotificationEmitter register( final Object obj, 
	final List<String> props ) {

        if (debug()) {
            dputil.enter( "register", "obj=", obj, "props=", props ) ;
        }
        
        // Construct the MBean
	try {
            final Class<?> cls = obj.getClass() ;
            final DynamicMBeanSkeleton skel = getSkeleton( cls ) ;
            final DynamicMBeanImpl mbean = new DynamicMBeanImpl( skel, obj ) ;

            final ObjectName oname = makeObjectName( obj, skel, props ) ;

            if (objectMap.containsKey( obj )) {
                if (debug()) {
                    dputil.dprint( "Object is already registered" ) ;
                }
                
                // XXX I18N
                throw new IllegalArgumentException(
                    "Object " + obj + " has already been registered" ) ;
            }

            if (objectNameMap.containsKey( oname )) {
                if (debug()) {
                    dputil.dprint( "ObjectName has already been registered" ) ;
                }
                
                throw new IllegalArgumentException(
                    // XXX I18N
                    "An Object has already been registered with ObjectName "
                    + oname ) ;
            }
        
            server.registerMBean( mbean, oname ) ;
            
	    objectNameMap.put( oname, obj ) ;
	    objectMap.put( obj, oname ) ;
            objectMBeanMap.put( obj, mbean ) ;

            return mbean ;
	} catch (JMException exc) {
	    throw new IllegalArgumentException( exc ) ;
	} finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized void unregister( Object obj ) {
        if (debug()) {
            dputil.enter( "unregister", "obj=", obj ) ;
        }
        
        try {
            ObjectName oname = objectMap.get( obj ) ;
            if (oname != null) {
                try {
                    server.unregisterMBean(oname);
                } catch (InstanceNotFoundException ex) {
                    throw new IllegalArgumentException( 
                        "Could not unregister " + obj, ex ) ;
                } catch (MBeanRegistrationException ex) {
                    throw new IllegalArgumentException( 
                        "Could not unregister " + obj, ex ) ;
                } finally {
                    // Make sure obj is removed even if unregisterMBean fails
                    objectMap.remove( obj ) ;
                    objectNameMap.remove( oname ) ;
                    objectMBeanMap.remove( obj ) ;
                }
            } else if (debug()) {
                dputil.dprint( obj + " not found" ) ;
            }
        } finally {
            dputil.exit() ;
        }
    }

    public synchronized ObjectName getObjectName( Object obj ) {
        if (debug()) {
            dputil.enter( "getObjectName", obj ) ;
        }
        
        try {
            ObjectName result = objectMap.get( obj ) ;
            if (debug()) {
                dputil.dprint( "result is " + result ) ;
            }
            return result ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized Object getObject( ObjectName oname ) {
        if (debug()) {
            dputil.enter( "getObject", oname ) ;
        }
        
        try {
            Object result = objectNameMap.get( oname ) ;
            if (debug()) {
                dputil.dprint( "result is " + result ) ;
            }
                
            return result ;
	} finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
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
    
    
    public void addAnnotation( AnnotatedElement element,
        Annotation annotation ) {
        
        if (debug()) {
            dputil.enter( "addAnnotation", "element = ", element,
                "annotation = ", annotation ) ;
        }
        
        try {
            Map<Class, Annotation> map = addedAnnotations.get( element ) ;
            if (map == null) {
                if (debug()) {
                    dputil.dprint( "Creating new Map<Class,Annotation>" ) ;
                }
                
                map = new HashMap<Class, Annotation>() ;
                addedAnnotations.put( element, map ) ;
            }

            Annotation  ann = map.get( annotation.getClass() ) ;
            if (ann != null) {
                if (debug()) {
                    dputil.dprint( "Duplicate annotation") ;
                }
                
                throw new IllegalArgumentException( "Cannot add annotation " 
                    + " to element " + element 
                    + ": an Annotation of type " 
                    + annotation.getClass().getName() 
                    + " is already present" ) ;
            }

            map.put( annotation.getClass(), annotation ) ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }
       
    @SuppressWarnings({"unchecked"})
    public <T extends Annotation> T getAnnotation( AnnotatedElement element,
        Class<T> type ) {
        
        if (debug()) {
            dputil.enter( "getAnnotation", "element = ", element,
                "type = ", type.getName() ) ;
        }
        
        try {
            T result = element.getAnnotation( type ) ;
            if (result == null) {
                if (debug()) {
                    dputil.dprint( "getting annotation from addedAnnotations map" ) ;
                }

                Map<Class, Annotation> map = addedAnnotations.get( element );
                if (map != null) {
                    result = (T)map.get( type ) ;
                } 
            }

            if (debug()) {
                dputil.dprint( "result = " + result ) ;
            }
            
            return result ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }
    
    public Pair<Class<?>,ClassAnalyzer> getClassAnalyzer( 
        final Class<?> cls, 
        final Class<? extends Annotation> annotationClass ) {

        if (debug()) {
            dputil.enter( "getClassAnalyzer", "cls = ", cls,
                "annotationClass = ", annotationClass ) ;
        }
        
        try {
            ClassAnalyzer ca = new ClassAnalyzer( cls ) ;

            final Class<?> annotatedClass = Algorithms.getFirst( 
                ca.findClasses( ca.forAnnotation( this, annotationClass ) ),
                "No " + annotationClass.getName() + " annotation found" ) ;

            if (debug()) {
                dputil.dprint( "annotatedClass = " + annotatedClass ) ;
            }
    
            final List<Class<?>> classes = new ArrayList<Class<?>>() ;
            classes.add( annotatedClass ) ;
            final IncludeSubclass incsub = annotatedClass.getAnnotation( 
                IncludeSubclass.class ) ;
            if (incsub != null) {
                for (Class<?> klass : incsub.cls()) {
                    classes.add( klass ) ;
                    if (debug()) {
                        dputil.dprint( "included subclass: " + klass ) ;
                    }
                }
            }

            if (classes.size() > 1) {
                if (debug()) {
                    dputil.dprint( 
                        "Getting new ClassAnalyzer for included subclasses" ) ;
                }
                ca = new ClassAnalyzer( classes ) ;
            }

            return new Pair<Class<?>,ClassAnalyzer>( annotatedClass, ca ) ;
        } finally {
            if (debug()) {
                dputil.exit() ;
            }
        }
    }
    
    public List<InheritedAttribute> getInheritedAttributes( 
        final ClassAnalyzer ca ) {        
        
        if (debug()) {
            dputil.enter( "getInheritedAttributes", "ca=", ca ) ;
        }
        
        try {
            final Predicate<AnnotatedElement> pred = Algorithms.or( 
                ca.forAnnotation( this, InheritedAttribute.class ),
                ca.forAnnotation( this, InheritedAttributes.class ) ) ;

            // Construct list of classes annotated with InheritedAttribute or
            // InheritedAttributes.
            final List<Class<?>> iaClasses = ca.findClasses( pred ) ;

            List<InheritedAttribute> isList = Algorithms.flatten( iaClasses,
                new UnaryFunction<Class<?>,List<InheritedAttribute>>() {
                    public List<InheritedAttribute> evaluate( Class<?> cls ) {
                        final InheritedAttribute ia = getAnnotation(cls,
                            InheritedAttribute.class);
                        final InheritedAttributes ias = getAnnotation(cls,
                            InheritedAttributes.class);
                        if ((ia != null) && (ias != null)) {
                            throw new IllegalArgumentException( "class " + cls
                                + " contains both the InheritedAttribute and " 
                                + " the InheritedAttributes annotations" ) ;
                        }

                        final List<InheritedAttribute> result = 
                            new ArrayList<InheritedAttribute>() ;

                        if (ia != null) {
                            result.add( ia ) ;
                        } else if (ias != null) {
                            result.addAll( Arrays.asList( ias.attributes() )) ;
                        }

                        return result ;
                    }
            } ) ;

            return isList ;
        } finally {
            if (debug())
                dputil.exit() ;
        }
    }
    
    public void setDebug( boolean flag ) {
        if (flag) {
            dputil = new DprintUtil( this ) ;
        } else {
            dputil = null ;
        }
    }
    
    public String dumpSkeleton( Object obj ) {
        DynamicMBeanImpl impl = objectMBeanMap.get( obj ) ;
        if (impl == null) {
            return obj + " is not currently registered with mom " + this ;
        } else {
            DynamicMBeanSkeleton skel = impl.skeleton() ;
            String skelString = ObjectUtility.defaultObjectToString( skel ) ;
            return "Skeleton for MBean for object " + obj + ":\n"
                + skelString ;
        }
    }
    
    public boolean debug() {
        return dputil != null ;
    }
}

