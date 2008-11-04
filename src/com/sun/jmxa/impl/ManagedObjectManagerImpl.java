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
import com.sun.jmxa.ManagedObjectManager;
import com.sun.jmxa.ManagedObjectManagerFactory;
import com.sun.jmxa.generic.DprintUtil;
import com.sun.jmxa.generic.DumpIgnore;
import com.sun.jmxa.generic.ObjectUtility;
import com.sun.jmxa.generic.Predicate;
import com.sun.jmxa.generic.UnaryFunction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.jar.Attributes.Name;

public class ManagedObjectManagerImpl implements ManagedObjectManagerInternal {
    private final String domain ;
    private ResourceBundle resourceBundle ;
    private MBeanServer server ; 
    private MBeanTree tree ;
    private final Map<Class<?>,MBeanSkeleton> skeletonMap ;
    private final Map<Type,TypeConverter> typeConverterMap ;
    private final Map<AnnotatedElement, Map<Class, Annotation>> addedAnnotations ;
    
    @DumpIgnore
    private DprintUtil dputil = null ;
    private ManagedObjectManager.RegistrationDebugLevel regDebugLevel = 
        ManagedObjectManager.RegistrationDebugLevel.NONE ;
    private boolean runDebugFlag = false ;
    private final Set<String> typePrefixes = new HashSet<String>() ;

    @Override
    public String toString( ) {
        return "ManagedObjectManagerImpl[domain=" + domain + "]" ;
    }
    
    public ManagedObjectManagerImpl( 
        final ManagedObjectManagerFactory.Mode mode,
        final String domain, 
        final String rootParentName,
        final Object rootObject,
        final String rootName ) {
        
	this.domain = domain ;
        resourceBundle = null ;
        String typeString = mode==ManagedObjectManagerFactory.Mode.J2EE ?
            "j2eeType" :
            "type" ;
        
	server = ManagementFactory.getPlatformMBeanServer() ;
        tree = new MBeanTree( this, domain, rootParentName, typeString,
            rootObject, rootName ) ;
	skeletonMap = new WeakHashMap<Class<?>,MBeanSkeleton>() ;
	typeConverterMap = new WeakHashMap<Type,TypeConverter>() ;
        addedAnnotations = 
            new HashMap<AnnotatedElement, Map<Class, Annotation>>() ;
    }

    private static final TypeConverter recursiveTypeMarker = 
        new TypeConverterImpl.TypeConverterPlaceHolderImpl() ;

    public void close() throws IOException {
        if (registrationDebug()) {
            dputil.enter( "close" ) ;
        }
        
        try {
            tree.clear() ;
            skeletonMap.clear() ;
            typeConverterMap.clear() ;
            addedAnnotations.clear() ;
            server = null ;
            resourceBundle = null ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public synchronized MBeanSkeleton getSkeleton( Class<?> cls ) {
        if (registrationDebug()) {
            dputil.enter( "getSkeleton", cls ) ;
        }
        
        try {
            MBeanSkeleton result = skeletonMap.get( cls ) ;

            boolean newSkeleton = false ;
            if (result == null) {
                newSkeleton = true ;
                if (registrationDebug()) {
                    dputil.info( "creating new Skeleton" ) ;
                }
                
                Pair<Class<?>,ClassAnalyzer> pair = getClassAnalyzer( 
                    cls, ManagedObject.class ) ;
                Class<?> annotatedClass = pair.first() ;
                ClassAnalyzer ca = pair.second() ;

                result = skeletonMap.get( annotatedClass ) ;

                if (result == null) {
                    result = new MBeanSkeleton( annotatedClass, ca, this ) ;
                }

                skeletonMap.put( cls, result ) ;
            }
            
            if (registrationFineDebug() || (registrationDebug() && newSkeleton)) {
                dputil.info( "Skeleton=" 
                    + ObjectUtility.defaultObjectToString( result ) ) ;
            }
            
            return result ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized TypeConverter getTypeConverter( Type type ) {
        if (registrationFineDebug()) {
            dputil.enter( "getTypeConverter", type ) ;
        }
        
        TypeConverter result = null;
        
        try {
            boolean newTypeConverter = false ;
            result = typeConverterMap.get( type ) ;	
            if (result == null) {
                if (registrationFineDebug()) {
                    dputil.info( "Creating new TypeConverter" ) ;
                }
            
                // Store a TypeConverter impl that throws an exception when 
                // acessed.  Used to detect recursive types.
                typeConverterMap.put( type, recursiveTypeMarker ) ;

                result = TypeConverterImpl.makeTypeConverter( type, this ) ;

                // Replace recursion marker with the constructed implementation
                typeConverterMap.put( type, result ) ;
                newTypeConverter = true ;
            }
            
            if (registrationFineDebug() || 
                (registrationDebug() && newTypeConverter)) {
                
                if (registrationFineDebug()) {
                    dputil.info( "result=" 
                        + ObjectUtility.defaultObjectToString( result ) ) ;
                }
            }
        } finally {
            if (registrationFineDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    private String stripType( String arg ) {
        for (String str : typePrefixes ) {
            if (arg.startsWith( str ) ) {
                return arg.substring( str.length() + 1 ) ;
            }
        }
        
        return arg ;
    }
    
    public MBeanImpl constructMBean( Object obj, String name ) {
        
        MBeanImpl result = null ;
        
        if (registrationDebug()) {
            dputil.enter( "constructMean", 
                "obj=", obj,
                "name=", name ) ;
        }
        
        try {
            final Class<?> cls = obj.getClass() ;
            final MBeanSkeleton skel = getSkeleton( cls ) ;

            String type = stripType( skel.getType() ) ;
            if (registrationDebug()) {
                dputil.info( "Stripped type =", type ) ;
            }

            String objName = name ;
            if (objName == null) {
                objName = skel.getNameValue( obj ) ;
                if (objName == null) {
                    objName = skel.getType() ;
                }
            }  
            
            if (registrationDebug()) {
                dputil.info( "Name value =", objName ) ;
            }

            result = new MBeanImpl( skel, obj, server, type, objName ) ;
        } catch (JMException exc) {
            dputil.exception( "Problem in fetching value of name", exc) ;
        } finally {
            dputil.exit() ;
        }
        
        return result ;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized NotificationEmitter register( final Object parent,
        final Object obj, final String name ) {

        if (registrationDebug()) {
            dputil.enter( "register", 
                "parent=", parent, 
                "obj=", obj,
                "name=", name ) ;
        }
        
        // Construct the MBean
	try {
            final MBeanImpl mb = constructMBean( obj, name ) ;
            
            return tree.register( parent, obj, mb) ;
	} catch (JMException exc) {
	    throw new IllegalArgumentException( exc ) ;
	} finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
    
   public synchronized NotificationEmitter register( final Object parent,
        final Object obj ) {

        return register( parent, obj, null ) ;
    }

    public synchronized void unregister( Object obj ) {
        if (registrationDebug()) {
            dputil.enter( "unregister", "obj=", obj ) ;
        }
        
        try {
            tree.unregister( obj ) ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized ObjectName getObjectName( Object obj ) {
        if (registrationDebug()) {
            dputil.enter( "getObjectName", obj ) ;
        }
        
        ObjectName result = null;
        try {
            result = tree.getObjectName( obj ) ;
        } finally {
            if (registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    public synchronized Object getObject( ObjectName oname ) {
        if (registrationDebug()) {
            dputil.enter( "getObject", oname ) ;
        }
        
        Object result = null ;
        try {
            result = tree.getObject( oname ) ;
	} finally {
            if (registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
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
        
        if (registrationDebug()) {
            dputil.enter( "addAnnotation", "element = ", element,
                "annotation = ", annotation ) ;
        }
        
        try {
            Map<Class, Annotation> map = addedAnnotations.get( element ) ;
            if (map == null) {
                if (registrationDebug()) {
                    dputil.info( "Creating new Map<Class,Annotation>" ) ;
                }
                
                map = new HashMap<Class, Annotation>() ;
                addedAnnotations.put( element, map ) ;
            }

            Annotation  ann = map.get( annotation.getClass() ) ;
            if (ann != null) {
                if (registrationDebug()) {
                    dputil.info( "Duplicate annotation") ;
                }
                
                throw new IllegalArgumentException( "Cannot add annotation " 
                    + " to element " + element 
                    + ": an Annotation of type " 
                    + annotation.getClass().getName() 
                    + " is already present" ) ;
            }

            map.put( annotation.getClass(), annotation ) ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
       
    @SuppressWarnings({"unchecked"})
    public <T extends Annotation> T getAnnotation( AnnotatedElement element,
        Class<T> type ) {
        
        if (registrationFineDebug()) {
            dputil.enter( "getAnnotation", "element=", element,
                "type=", type.getName() ) ;
        }
        
        try {
            T result = element.getAnnotation( type ) ;
            if (result == null) {
                if (registrationFineDebug()) {
                    dputil.info( 
                        "No annotation on element: trying addedAnnotations map" ) ;
                }

                Map<Class, Annotation> map = addedAnnotations.get( element );
                if (map != null) {
                    result = (T)map.get( type ) ;
                } 
            }

            if (registrationFineDebug()) {
                dputil.info( "result = " + result ) ;
            }
            
            return result ;
        } finally {
            if (registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public Pair<Class<?>,ClassAnalyzer> getClassAnalyzer( 
        final Class<?> cls, 
        final Class<? extends Annotation> annotationClass ) {

        if (registrationDebug()) {
            dputil.enter( "getClassAnalyzer", "cls = ", cls,
                "annotationClass = ", annotationClass ) ;
        }
        
        try {
            ClassAnalyzer ca = new ClassAnalyzer( cls ) ;

            final Class<?> annotatedClass = Algorithms.getFirst( 
                ca.findClasses( ca.forAnnotation( this, annotationClass ) ),
                "No " + annotationClass.getName() + " annotation found" ) ;

            if (registrationDebug()) {
                dputil.info( "annotatedClass = " + annotatedClass ) ;
            }
    
            final List<Class<?>> classes = new ArrayList<Class<?>>() ;
            classes.add( annotatedClass ) ;
            final IncludeSubclass incsub = annotatedClass.getAnnotation( 
                IncludeSubclass.class ) ;
            if (incsub != null) {
                for (Class<?> klass : incsub.cls()) {
                    classes.add( klass ) ;
                    if (registrationDebug()) {
                        dputil.info( "included subclass: " + klass ) ;
                    }
                }
            }

            if (classes.size() > 1) {
                if (registrationDebug()) {
                    dputil.info( 
                        "Getting new ClassAnalyzer for included subclasses" ) ;
                }
                ca = new ClassAnalyzer( classes ) ;
            }

            return new Pair<Class<?>,ClassAnalyzer>( annotatedClass, ca ) ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public List<InheritedAttribute> getInheritedAttributes( 
        final ClassAnalyzer ca ) {        
        
        if (registrationDebug()) {
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
            if (registrationDebug())
                dputil.exit() ;
        }
    }
    
    public void setRegistrationDebug( 
        ManagedObjectManager.RegistrationDebugLevel level ) {
        
        regDebugLevel = level ;
        if (level != ManagedObjectManager.RegistrationDebugLevel.NONE ) {
            dputil = new DprintUtil( this ) ;
        } else {
            dputil = null ;
        }
    }
    
    public void setRuntimeDebug( boolean flag ) {
        runDebugFlag = flag ;
    }
    
    public String dumpSkeleton( Object obj ) {
        MBeanImpl impl = tree.getMBeanImpl( obj ) ;
        if (impl == null) {
            return obj + " is not currently registered with mom " + this ;
        } else {
            MBeanSkeleton skel = impl.skeleton() ;
            String skelString = ObjectUtility.defaultObjectToString( skel ) ;
            return "Skeleton for MBean for object " + obj + ":\n"
                + skelString ;
        }
    }
    
    public boolean registrationDebug() {
        return regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.NORMAL 
            || regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.FINE ;
    }
    
    public boolean registrationFineDebug() {
        return regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.FINE ;
    }
    
    public boolean runtimeDebug() {
        return runDebugFlag ;
    }
    
    public void addTypePrefix( String arg ) {
        typePrefixes.add( arg ) ;
    }
}