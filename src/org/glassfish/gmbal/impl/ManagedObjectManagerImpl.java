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

import java.util.ResourceBundle ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.WeakHashMap ;
import java.util.List ;
import java.util.ArrayList ;

import java.io.IOException ;

import java.lang.annotation.Annotation ;

import java.lang.management.ManagementFactory ;

import java.lang.reflect.AnnotatedElement;
import javax.management.MBeanServer ;
import javax.management.JMException ;
import javax.management.ObjectName ;
import javax.management.NotificationEmitter;

import org.glassfish.gmbal.generic.Pair ;
import org.glassfish.gmbal.generic.Algorithms ;

import org.glassfish.gmbal.ManagedObject ;
import org.glassfish.gmbal.Description ;
import org.glassfish.gmbal.IncludeSubclass ;
import org.glassfish.gmbal.InheritedAttribute ;
import org.glassfish.gmbal.InheritedAttributes ;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.generic.DprintUtil;
import org.glassfish.gmbal.generic.DumpIgnore;
import org.glassfish.gmbal.generic.ObjectUtility;
import org.glassfish.gmbal.generic.Predicate;
import org.glassfish.gmbal.generic.UnaryFunction;
import org.glassfish.gmbal.generic.FacetAccessor ;
import org.glassfish.gmbal.generic.FacetAccessorImpl;
import org.glassfish.gmbal.generic.Holder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.glassfish.gmbal.typelib.EvaluatedClassAnalyzer;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator;

/* Implementation notes:
 * XXX Test attribute change notification.
 */
public class ManagedObjectManagerImpl implements ManagedObjectManagerInternal {
    private static ObjectUtility myObjectUtil =
        new ObjectUtility(true, 0, 4)
            .useToString( EvaluatedType.class )
            .useToString( ManagedObjectManager.class ) ;

    private String domain ;
    private ResourceBundle resourceBundle ;
    private MBeanServer server ; 
    private MBeanTree tree ;
    private final Map<EvaluatedClassDeclaration,MBeanSkeleton> skeletonMap ;
    private final Map<EvaluatedType,TypeConverter> typeConverterMap ;
    private final Map<AnnotatedElement, Map<Class, Annotation>> addedAnnotations ;
    
    @DumpIgnore
    private DprintUtil dputil = null ;
    private ManagedObjectManager.RegistrationDebugLevel regDebugLevel = 
        ManagedObjectManager.RegistrationDebugLevel.NONE ;
    private boolean runDebugFlag = false ;

    public void suspendJMXRegistration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void resumeJMXRegistration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private static final class StringComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            return - o1.compareTo( o2 ) ;
        }
    } ;
    private Comparator<String> revComp = new StringComparator() ;
    
    // Maintain the list of typePrefixes in reversed sorted order, so that
    // we strip the longest prefix first.
    private final SortedSet<String> typePrefixes = new TreeSet<String>( 
        revComp ) ;

    @Override
    public String toString( ) {
        return "ManagedObjectManagerImpl[domain=" + domain + "]" ;
    }
    
    private ManagedObjectManagerImpl() {
        this.resourceBundle = null ;
        this.server = ManagementFactory.getPlatformMBeanServer() ;
        this.skeletonMap = 
            new WeakHashMap<EvaluatedClassDeclaration,MBeanSkeleton>() ;
        this.typeConverterMap = new WeakHashMap<EvaluatedType,TypeConverter>() ;
        this.addedAnnotations = 
            new HashMap<AnnotatedElement, Map<Class, Annotation>>() ;
    }
    
    public ManagedObjectManagerImpl( final String domain ) {
        this() ;
        this.domain = domain ;

        // set actualRoot, rootName later
        // MBeanTree need mom, domain, rootParentName
        this.tree = new MBeanTree( this, domain, null, "type" ) ;
    }

    public ManagedObjectManagerImpl( final ObjectName rootParentName ) {
        this() ;
        this.domain = rootParentName.getDomain() ;

        // set actualRoot, rootName later
        // MBeanTree need mom, domain, rootParentName
        this.tree = new MBeanTree( this, domain, rootParentName, "type" ) ;
    }


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

    public synchronized ObjectName getRootParentName() {
        return tree.getRootParentName() ;
    }

    @ManagedObject
    @AMXMetadata( pathPart="GMBALROOT")
    @Description( "Dummy class used when no root is specified" ) 
    private static class Root {
        // No methods: will simply implement an AMX container
    }
    
    public synchronized NotificationEmitter createRoot() {
        return tree.setRoot( new Root(), null ) ;
    }

    public synchronized NotificationEmitter createRoot(Object root) {
        return tree.setRoot( root, null ) ;
    }

    public synchronized NotificationEmitter createRoot(Object root, String name) {
        return tree.setRoot( root, name ) ;
    }

    public synchronized Object getRoot() {
        return tree.getRoot() ;
    }
    
    public synchronized MBeanSkeleton getSkeleton( EvaluatedClassDeclaration cls ) {
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
                
                Pair<EvaluatedClassDeclaration,EvaluatedClassAnalyzer> pair = 
                    getClassAnalyzer( cls, ManagedObject.class ) ;
                EvaluatedClassDeclaration annotatedClass = pair.first() ;
                EvaluatedClassAnalyzer ca = pair.second() ;

                result = skeletonMap.get( annotatedClass ) ;

                if (result == null) {
                    result = new MBeanSkeleton( annotatedClass, ca, this ) ;
                }

                skeletonMap.put( cls, result ) ;
            }
            
            if (registrationFineDebug() || (registrationDebug() && newSkeleton)) {
                dputil.info( "Skeleton=" 
                    + myObjectUtil.objectToString(result) ) ;
            }
            
            return result ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized TypeConverter getTypeConverter( EvaluatedType type ) {
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
                typeConverterMap.put( type, 
                    new TypeConverterImpl.TypeConverterPlaceHolderImpl( type ) ) ;

                result = TypeConverterImpl.makeTypeConverter( type, this ) ;

                // Replace recursion marker with the constructed implementation
                typeConverterMap.put( type, result ) ;
                newTypeConverter = true ;
            }
            
            if (registrationFineDebug() || 
                (registrationDebug() && newTypeConverter)) {
                
                if (registrationFineDebug()) {
                    dputil.info( "result=" 
                        + myObjectUtil.objectToString( result ) ) ;
                }
            }
        } finally {
            if (registrationFineDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    public String getStrippedName( Class<?> cls ) {
        String arg = cls.getName() ;
        for (String str : typePrefixes ) {
            if (arg.startsWith( str ) ) {
                return arg.substring( str.length() + 1 ) ;
            }
        }
        
        return arg ;
    }
    
    public synchronized MBeanImpl constructMBean( Object obj, String name ) {
        MBeanImpl result = null ;
        
        if (registrationDebug()) {
            dputil.enter( "constructMean", 
                "obj=", obj,
                "name=", name ) ;
        }
        
        try {
            final Class<?> cls = obj.getClass() ;
            final EvaluatedClassDeclaration cdecl = 
                (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(cls) ;
            final MBeanSkeleton skel = getSkeleton( cdecl ) ;

            String type = skel.getType() ;
            if (registrationDebug()) {
                dputil.info( "Stripped type =", type ) ;
            }

            result = new MBeanImpl( skel, obj, server, type ) ;
            
            String objName = name ;
            if (objName == null) {
                objName = skel.getNameValue( result ) ;
                if (objName == null) {
                    objName = "na" ;
                }
            }  
           
            if (registrationDebug()) {
                dputil.info( "Name value =", objName ) ;
            }
            
            result.name( objName ) ;
        } catch (JMException exc) {
            if (registrationDebug()) {
                dputil.exception( "Problem in fetching value of name", exc) ;
            }
        } finally {
            if (registrationDebug()) {
                dputil.exit( result ) ;
            }
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
        
        if (obj instanceof String) {
            throw Exceptions.self.objStringWrongRegisterCall( (String)obj ) ;
        }
        
        // Construct the MBean
        try {
            final MBeanImpl mb = constructMBean( obj, name ) ;
            
            return tree.register( parent, obj, mb) ;
    	} catch (JMException exc) {
            throw Exceptions.self.exceptionInRegister(exc) ;
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

    
    public synchronized NotificationEmitter registerAtRoot(Object obj, String name) {
        return register( tree.getRoot(), obj, name ) ;
    }

    public synchronized NotificationEmitter registerAtRoot(Object obj) {
        return register( tree.getRoot(), obj, null ) ;
    }
    
    public synchronized void unregister( Object obj ) {
        if (registrationDebug()) {
            dputil.enter( "unregister", "obj=", obj ) ;
        }
        
        try {
            tree.unregister( obj ) ;
        } catch (JMException exc) {
            throw Exceptions.self.exceptionInUnregister(exc) ;
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
    
    public synchronized FacetAccessor getFacetAccessor( Object obj ) {
        MBeanImpl mb = tree.getMBeanImpl( obj ) ;
        if (mb != null) {
            return tree.getFacetAccessor( obj ) ;
        } else {
            return new FacetAccessorImpl( obj ) ;
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

    public synchronized void setResourceBundle( ResourceBundle rb ) {
        this.resourceBundle = rb ;
    }

    public synchronized ResourceBundle getResourceBundle() {
        return resourceBundle ;
    }
    
    public synchronized String getDescription( EvaluatedDeclaration element ) {
        Description desc = element.annotation( Description.class ) ;
        String result ;
        if (desc == null) {
            result = Exceptions.self.noDescriptionAvailable() ;
        } else {
            result = desc.value() ;
        }
        
        if (resourceBundle != null) {
            result = resourceBundle.getString( result ) ;
        }
        
        return result ;
    }
    
    
    public synchronized void addAnnotation( AnnotatedElement element,
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
                
                throw Exceptions.self.duplicateAnnotation( element, 
                    annotation.getClass().getName()) ;
            }

            map.put( annotation.getClass(), annotation ) ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public synchronized <T extends Annotation> T getAnnotation( 
        EvaluatedDeclaration element, Class<T> type ) {
        
        if (registrationFineDebug()) {
            dputil.enter( "getAnnotation", "element=", element,
                "type=", type.getName() ) ;
        }
        
        try {
            T result = element.annotation( type ) ;
            if (result == null) {
                if (registrationFineDebug()) {
                    dputil.info( 
                        "No annotation on element: trying addedAnnotations map" ) ;
                }

                Map<Class, Annotation> map = addedAnnotations.get( 
                    element.element() );
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
    
    public synchronized Pair<EvaluatedClassDeclaration,EvaluatedClassAnalyzer>
        getClassAnalyzer( final EvaluatedClassDeclaration cls,
        final Class<? extends Annotation> annotationClass ) {

        if (registrationDebug()) {
            dputil.enter( "getClassAnalyzer", "cls = ", cls,
                "annotationClass = ", annotationClass ) ;
        }
        
        try {
            EvaluatedClassAnalyzer ca = new EvaluatedClassAnalyzer( cls ) ;

            final EvaluatedClassDeclaration annotatedClass = Algorithms.getFirst(
                ca.findClasses( forAnnotation( annotationClass, 
                    EvaluatedClassDeclaration.class ) ),
                "No " + annotationClass.getName() + " annotation found on" 
                    + "EvaluatedClassAnalyzer " + ca ) ;

            if (registrationDebug()) {
                dputil.info( "annotatedClass = " + annotatedClass ) ;
            }
    
            final List<EvaluatedClassDeclaration> classes =
                new ArrayList<EvaluatedClassDeclaration>() ;
            classes.add( annotatedClass ) ;
            final IncludeSubclass incsub = annotatedClass.annotation(
                IncludeSubclass.class ) ;
            if (incsub != null) {
                for (Class<?> klass : incsub.value()) {
                    EvaluatedClassDeclaration ecd = 
                        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(klass) ;
                    classes.add( ecd ) ;
                    if (registrationDebug()) {
                        dputil.info( "included subclass: " + klass ) ;
                    }
                }
            }

            if (classes.size() > 1) {
                if (registrationDebug()) {
                    dputil.info( 
                        "Getting new EvaluatedClassAnalyzer for included subclasses" ) ;
                }
                ca = new EvaluatedClassAnalyzer( classes ) ;
            }

            return new Pair<EvaluatedClassDeclaration,
                 EvaluatedClassAnalyzer>( annotatedClass, ca ) ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public synchronized List<InheritedAttribute> getInheritedAttributes( 
        final EvaluatedClassAnalyzer ca ) {
        
        if (registrationDebug()) {
            dputil.enter( "getInheritedAttributes", "ca=", ca ) ;
        }
        
        try {
            final Predicate<EvaluatedClassDeclaration> pred = Algorithms.or(
                forAnnotation( InheritedAttribute.class, 
                    EvaluatedClassDeclaration.class ),
                forAnnotation( InheritedAttributes.class, 
                    EvaluatedClassDeclaration.class ) ) ;

            // Construct list of classes annotated with InheritedAttribute or
            // InheritedAttributes.
            final List<EvaluatedClassDeclaration> iaClasses =
                ca.findClasses( pred ) ;

            List<InheritedAttribute> isList = Algorithms.flatten( iaClasses,
                new UnaryFunction<EvaluatedClassDeclaration,List<InheritedAttribute>>() {
                    public List<InheritedAttribute> evaluate( EvaluatedClassDeclaration cls ) {
                        final InheritedAttribute ia = getAnnotation(cls,
                            InheritedAttribute.class);
                        final InheritedAttributes ias = getAnnotation(cls,
                            InheritedAttributes.class);
                        if ((ia != null) && (ias != null)) {
                            throw Exceptions.self.badInheritedAttributeAnnotation(cls) ;
                        }

                        final List<InheritedAttribute> result = 
                            new ArrayList<InheritedAttribute>() ;

                        if (ia != null) {
                            result.add( ia ) ;
                        } else if (ias != null) {
                            result.addAll( Arrays.asList( ias.value() )) ;
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
    
    private class ADHolder implements Predicate<InheritedAttribute> {
        
        private final EvaluatedMethodDeclaration method ;
        private final ManagedObjectManagerInternal.AttributeDescriptorType adt ;
        private AttributeDescriptor content ;

        public ADHolder(  final EvaluatedMethodDeclaration method,
            ManagedObjectManagerInternal.AttributeDescriptorType adt ) {
            this.method = method ;
            this.adt = adt ;
        }
        
        public boolean evaluate( InheritedAttribute ia ) {
            AttributeDescriptor ad = AttributeDescriptor.makeFromInherited( 
                ManagedObjectManagerImpl.this, method,
                ia.id(), ia.methodName(), ia.description(), adt ) ;
            boolean result = ad != null ;
            if (result) {
                content = ad ;
            }
            
            return result ;
        }

        public AttributeDescriptor content() {
            return content ;
        }
    }
    
    private AttributeDescriptor getAttributeDescriptorIfInherited( 
        final EvaluatedMethodDeclaration method, 
        final List<InheritedAttribute> ias,
        final ManagedObjectManagerInternal.AttributeDescriptorType adt ) {
        
        ADHolder adh = new ADHolder( method, adt ) ;
        Algorithms.find( ias, adh ) ;
        return adh.content() ;
    }

    public synchronized <K,V> void putIfNotPresent( final Map<K,V> map,
        final K key, final V value ) {
    
        if (registrationFineDebug()) {
            dputil.enter( "putIfNotPresent", "key=", key,
                "value=", value ) ;
        }
        
        try {
            if (!map.containsKey( key )) {
                if (registrationFineDebug()) {
                    dputil.info( "Adding key, value to map" ) ;
                }
                map.put( key, value ) ;
            } else {
                if (registrationFineDebug()) {
                    dputil.info( "Key,value already in map" ) ;
                }
            }
        } finally {
            if (registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    // Returns a pair of maps defining all managed attributes in the ca.  The first map
    // is all setters, and the second is all getters.  Only the most derived version is present.
    public synchronized Pair<Map<String,AttributeDescriptor>,
        Map<String,AttributeDescriptor>>
        getAttributes( 
            final EvaluatedClassAnalyzer ca,
            final ManagedObjectManagerInternal.AttributeDescriptorType adt ) {

        if (registrationDebug()) {
            dputil.enter( "getAttributes" ) ;
        }

        try {
            final Map<String,AttributeDescriptor> getters = 
                new HashMap<String,AttributeDescriptor>() ; 
            final Map<String,AttributeDescriptor> setters = 
                new HashMap<String,AttributeDescriptor>() ; 
            final Pair<Map<String,AttributeDescriptor>,
                Map<String,AttributeDescriptor>> result =  
                    new Pair<Map<String,AttributeDescriptor>,
                        Map<String,AttributeDescriptor>>( getters, setters ) ;
            
            final List<InheritedAttribute> ias = getInheritedAttributes( ca ) ;
            
            ca.findMethods( new Predicate<EvaluatedMethodDeclaration>() {
                public boolean evaluate( EvaluatedMethodDeclaration method ) {
                    ManagedAttribute ma = method.annotation(
                        ManagedAttribute.class ) ;
                    AttributeDescriptor ad ;
                    if (ma == null) {
                        ad = getAttributeDescriptorIfInherited( method, ias,
                            adt ) ;
                    } else {
                        Description desc = getAnnotation( method, Description.class ) ;
                        String description ;
                        if (desc == null) {
                            description = "No description available for " + method.name() ;
                        } else {
                            description = desc.value() ;
                        }

                        ad = AttributeDescriptor.makeFromAnnotated( ManagedObjectManagerImpl.this,
                            method, ma.id(), description, adt ) ;
                    }
                    
                    if (ad != null) {
                        if (ad.atype()==AttributeDescriptor.AttributeType.GETTER) {
                            putIfNotPresent( getters, ad.id(), ad ) ;
                        } else {
                            putIfNotPresent( setters, ad.id(), ad ) ;
                        }
                    }
                    
                    return false ;
                } } ) ;
         
            return result ;
        } finally {
            if (registrationDebug()) {
                dputil.exit() ;
            }
        }
    }

    public synchronized void setRegistrationDebug( 
        ManagedObjectManager.RegistrationDebugLevel level ) {
        
        regDebugLevel = level ;
        if (level != ManagedObjectManager.RegistrationDebugLevel.NONE ) {
            dputil = new DprintUtil( getClass() ) ;
        } else {
            dputil = null ;
        }
    }
    
    public synchronized void setRuntimeDebug( boolean flag ) {
        runDebugFlag = flag ;
    }
    
    public synchronized String dumpSkeleton( Object obj ) {
        MBeanImpl impl = tree.getMBeanImpl( obj ) ;
        if (impl == null) {
            return obj + " is not currently registered with mom " + this ;
        } else {
            MBeanSkeleton skel = impl.skeleton() ;
            String skelString = myObjectUtil.objectToString( skel ) ;
            return "Skeleton for MBean for object " + obj + ":\n"
                + skelString ;
        }
    }
    
    public synchronized boolean registrationDebug() {
        return regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.NORMAL 
            || regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.FINE ;
    }
    
    public synchronized boolean registrationFineDebug() {
        return regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.FINE ;
    }
    
    public synchronized boolean runtimeDebug() {
        return runDebugFlag ;
    }
    
    public synchronized void stripPrefix( String... args ) {
        for (String str : args) {
            typePrefixes.add( str ) ;
        }
    }
    
    public synchronized <T extends EvaluatedDeclaration> Predicate<T> forAnnotation(
        final Class<? extends Annotation> annotation,
        final Class<T> cls ) {

        return new Predicate<T>() {
            public boolean evaluate( T elem ) {
                return getAnnotation( elem, annotation ) != null ;
            }
        } ;
    }

}
