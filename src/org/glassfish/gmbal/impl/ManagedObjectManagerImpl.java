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

import java.util.ResourceBundle ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.WeakHashMap ;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.io.IOException ;
import java.io.Serializable;

import java.lang.annotation.Annotation ;

import java.lang.management.ManagementFactory ;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import javax.management.MBeanServer ;
import javax.management.JMException ;
import javax.management.ObjectName ;
import javax.management.MBeanAttributeInfo;

import org.glassfish.gmbal.AMXMBeanInterface;
import org.glassfish.gmbal.AMXClient;
import org.glassfish.gmbal.GmbalMBean ;
import org.glassfish.gmbal.ManagedObject ;
import org.glassfish.gmbal.Description ;
import org.glassfish.gmbal.IncludeSubclass ;
import org.glassfish.gmbal.InheritedAttribute ;
import org.glassfish.gmbal.InheritedAttributes ;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.ManagedData;

import org.glassfish.gmbal.generic.Pair ;
import org.glassfish.gmbal.generic.Algorithms ;
import org.glassfish.gmbal.generic.MethodMonitor;
import org.glassfish.gmbal.generic.MethodMonitorFactory;
import org.glassfish.gmbal.generic.DumpIgnore;
import org.glassfish.gmbal.generic.ObjectUtility;
import org.glassfish.gmbal.generic.Predicate;
import org.glassfish.gmbal.generic.UnaryFunction;
import org.glassfish.gmbal.generic.FacetAccessor ;
import org.glassfish.gmbal.generic.FacetAccessorImpl;
import org.glassfish.gmbal.generic.DelayedObjectToString;

import org.glassfish.gmbal.typelib.EvaluatedClassAnalyzer;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedFieldDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator;

import org.glassfish.external.amx.AMX;

import org.glassfish.external.statistics.AverageRangeStatistic ;
import org.glassfish.external.statistics.BoundaryStatistic;
import org.glassfish.external.statistics.BoundedRangeStatistic;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.TimeStatistic;
import org.glassfish.external.statistics.StringStatistic;

import org.glassfish.gmbal.generic.ClassAnalyzer;
import static org.glassfish.gmbal.generic.Algorithms.* ;

/* Implementation notes:
 * XXX Test attribute change notification.
 */
public class ManagedObjectManagerImpl implements ManagedObjectManagerInternal {

    // Used in MBeanSkeleton
    @AMXMetadata
    static class DefaultAMXMetadataHolder { }

    private static final AMXMetadata DEFAULT_AMX_METADATA =
	DefaultAMXMetadataHolder.class.getAnnotation(AMXMetadata.class);

    private static ObjectUtility myObjectUtil =
        new ObjectUtility(true, 0, 4)
            .useToString( EvaluatedType.class )
            .useToString( ManagedObjectManager.class ) ;

    private static final class StringComparator implements Serializable,
        Comparator<String> {
        private static final long serialVersionUID = 8274851916877850245L;
        public int compare(String o1, String o2) {
            return - o1.compareTo( o2 ) ;
        }
    } ;
    private static Comparator<String> REV_COMP = new StringComparator() ;

    // All finals should be initialized in this order in the private constructor
    @DumpIgnore
    private final MethodMonitor mm ;
    private final String domain ;
    private final MBeanTree tree ;
    private final Map<EvaluatedClassDeclaration,MBeanSkeleton> skeletonMap ;
    private final Map<EvaluatedType,TypeConverter> typeConverterMap ;
    private final Map<AnnotatedElement, Map<Class, Annotation>> addedAnnotations ;
    private final MBeanSkeleton amxSkeleton ;
    private final Set<String> amxAttributeNames ;

    // All non-finals should be initialized in this order in the init() method.
    private boolean rootCreated ;
    private ResourceBundle resourceBundle ;
    private MBeanServer server ;
    private ManagedObjectManager.RegistrationDebugLevel regDebugLevel ;
    private boolean runDebugFlag ;
    private boolean jmxRegistrationDebugFlag ;

    // Maintain the list of typePrefixes in reversed sorted order, so that
    // we strip the longest prefix first.
    private final SortedSet<String> typePrefixes = new TreeSet<String>(
        REV_COMP ) ;
    private boolean stripPackagePrefix = false ;

    private ManagedObjectManagerImpl( final String domain,
        final ObjectName rootParentName ) {

	this.mm = MethodMonitorFactory.makeStandard( getClass() ) ;
        this.domain = domain ;
        this.tree = new MBeanTree( this, domain, rootParentName, AMX.TYPE_KEY ) ;
        this.skeletonMap = 
            new WeakHashMap<EvaluatedClassDeclaration,MBeanSkeleton>() ;
        this.typeConverterMap = new WeakHashMap<EvaluatedType,TypeConverter>() ;
        this.addedAnnotations = 
            new HashMap<AnnotatedElement, Map<Class, Annotation>>() ;

        final EvaluatedClassDeclaration ecd =
            (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(
                AMXMBeanInterface.class ) ;
        this.amxAttributeNames = new HashSet<String>() ;
        this.amxSkeleton = getSkeleton( ecd ) ;
        for (MBeanAttributeInfo mbi : amxSkeleton.getMBeanInfo().getAttributes()) {
            amxAttributeNames.add( mbi.getName() ) ;
        }
    }

    @ManagedData
    @Description( "The Statistic model and its sub-models specify the data"
        + " models which are requried to be used to provide the performance data"
        + " described by the specific attributes in the Stats models" )
    @InheritedAttributes( {
        @InheritedAttribute( methodName="getName",
            description = "The name of this Statistic" ),
        @InheritedAttribute( methodName="getUnit",
            description = "The unit of measurement for this Statistic" ),
        @InheritedAttribute( methodName="getDescription",
            description = "A human-readable description of the Statistic" ),
        @InheritedAttribute( methodName="getStartTime",
            description = "The time of the first measurement represented as a long" ),
        @InheritedAttribute( methodName="getLastSampleTime",
            description = "The time of the first measurement represented as a long")
    } )
    public interface DummyStatistic { }

    @ManagedData
    @Description( "Specifies standard timing measurements")
    @InheritedAttributes( {
        @InheritedAttribute( methodName="getCount",
            description = "Number of times the operation was invoked since "
                 + "the beginning of this measurement"  ),
        @InheritedAttribute( methodName="getMaxTime",
            description = "The maximum amount of time taken to complete one invocation "
                + "of this operation since the beginning of this measurement" ),
        @InheritedAttribute( methodName="getMinTime",
            description = "The minimum amount of time taken to complete one invocation "
                + "of this operation since the beginning of this measurement" ),
        @InheritedAttribute( methodName="getTotalTime",
            description = "The total amount of time taken to complete every invocation "
                + "of this operation since the beginning of this measurement" )
    } )
    public interface DummyTimeStatistic extends DummyStatistic { }

    @ManagedData
    @Description( "Specifies standard measurements of the upper and lower "
        + "limits of the value of an attribute" )
    @InheritedAttributes( {
        @InheritedAttribute( methodName = "getUpperBound",
            description = "The upper limit of the value of this attribute" ),
        @InheritedAttribute( methodName = "getLowerBound",
            description = "The lower limit of the value of this attribute" )
    } )
    public interface DummyBoundaryStatistic extends DummyStatistic {}

    @ManagedData
    @Description( "Specifies standard count measurements" )
    @InheritedAttributes( {
        @InheritedAttribute( methodName = "getCount",
            description = "The count since the last reset" )
    } )
    public interface DummyCountStatistic {}

    @ManagedData
    @Description( "Specifies standard measurements of the lowest and highest values"
        + " an attribute has held as well as its current value" ) 
    @InheritedAttributes( {
        @InheritedAttribute( methodName = "getHighWaterMark",
            description = "The highest value this attribute has held since"
                + " the beginninYg of the measurement" ),
        @InheritedAttribute( methodName = "getLowWaterMark",
            description = "The lowest value this attribute has held since"
                + " the beginninYg of the measurement" ),
        @InheritedAttribute( methodName = "getCurrent",
            description = "The current value of this attribute" )
    } ) 
    public interface DummyRangeStatistic {}

    @ManagedData
    @Description( "Adds an average to the range statistic")
    @InheritedAttributes( {
        @InheritedAttribute( methodName = "getAverage",
            description = 
                "The average value of this attribute since its last reset")
    })
    public interface DummyAverageRangeStatistic {}

    @ManagedData
    @Description( "Provides standard measurements of a range that has fixed limits" ) 
    public interface DummyBoundedRangeStatistic extends
        DummyBoundaryStatistic, DummyRangeStatistic {}

    @ManagedData
    @Description( "Custom statistic type whose value is a string")
    @InheritedAttributes( {
        @InheritedAttribute(
            methodName="getCurrent",
            description="Returns the String value of the statistic" )
    } )
    public interface DummyStringStatistic extends DummyStatistic { }

    List<Pair<Class,Class>> statsData = list(
        pair( (Class)DummyStringStatistic.class,
            (Class)StringStatistic.class ),

        pair( (Class)DummyTimeStatistic.class,
            (Class)TimeStatistic.class ),

        pair( (Class)DummyStatistic.class,
            (Class)Statistic.class ),

        pair( (Class)DummyBoundaryStatistic.class,
            (Class)BoundaryStatistic.class ),

        pair( (Class)DummyBoundedRangeStatistic.class,
            (Class)BoundedRangeStatistic.class ),

        pair( (Class)DummyCountStatistic.class,
            (Class)CountStatistic.class ),

        pair( (Class)DummyRangeStatistic.class,
            (Class)RangeStatistic.class ),

        pair( (Class)DummyAverageRangeStatistic.class,
            (Class)AverageRangeStatistic.class )
    ) ;

    private void addAnnotationIfNotNull( AnnotatedElement elemement,
        Annotation annotation ) {
        if (annotation != null) {
            addAnnotation(elemement, annotation);
        }
    }

    private void initializeStatisticsSupport() {
        for (Pair<Class,Class> pair : statsData) {
            Class dummy = pair.first() ;
            Class real = pair.second() ;
            addAnnotationIfNotNull( real, dummy.getAnnotation( ManagedData.class ) ) ;
            addAnnotationIfNotNull( real, dummy.getAnnotation( Description.class ) ) ;
            addAnnotationIfNotNull( real, dummy.getAnnotation( InheritedAttributes.class ) ) ;
        }
    }

    private void init() {
        this.server = AccessController.doPrivileged( 
            new PrivilegedAction<MBeanServer>() {
                public MBeanServer run() {
                    return ManagementFactory.getPlatformMBeanServer() ;
                } 
            } ) ;

        rootCreated = false ;
        resourceBundle = null ;
        regDebugLevel = ManagedObjectManager.RegistrationDebugLevel.NONE ;
        runDebugFlag = false ;
        jmxRegistrationDebugFlag = false ;

        tree.clear() ;
        skeletonMap.clear() ;
        typeConverterMap.clear() ;
        addedAnnotations.clear() ;
        mm.clear() ;

        initializeStatisticsSupport() ;
    }
    
    public ManagedObjectManagerImpl( final String domain ) {
        this( domain, null ) ;
        init() ;
    }

    public ManagedObjectManagerImpl( final ObjectName rootParentName ) {
        this( rootParentName.getDomain(), rootParentName ) ;
        init() ;
    }

    public void close() throws IOException {
        // Can be called anytime

        mm.enter( registrationDebug(), "close" ) ;
        
        try {
            init() ;
        } finally {
	    mm.exit( registrationDebug() ) ;
        }
    }

    private synchronized void checkRootNotCreated( String methodName ) {
        if (rootCreated) {
            throw Exceptions.self.createRootCalled(methodName) ;
        }
    }

    private synchronized void checkRootCreated( String methodName ) {
        if (!rootCreated) {
            throw Exceptions.self.createRootNotCalled(methodName) ;
        }
    }

    public synchronized void suspendJMXRegistration() {
        mm.clear() ;
        // Can be called anytime
        tree.suspendRegistration() ;
    }

    public synchronized void resumeJMXRegistration() {
        mm.clear() ;
        // Can be called anytime
        tree.resumeRegistration();
    }

    public synchronized void stripPackagePrefix() {
        mm.clear() ;
        checkRootNotCreated("stripPackagePrefix");
        stripPackagePrefix = true ;
    }

    @Override
    public String toString( ) {
        // Can be called anytime
        return "ManagedObjectManagerImpl[domain=" + domain + "]" ;
    }

    public synchronized ObjectName getRootParentName() {
        checkRootCreated("getRootParentName");
        return tree.getRootParentName() ;
    }

    @ManagedObject
    @AMXMetadata( type="gmbal-root", isSingleton=true)
    @Description( "Dummy class used when no root is specified" ) 
    private static class Root {
        // No methods: will simply implement an AMXMBeanInterface container
        @Override
        public String toString() {
            return "GmbalDefaultRoot" ;
        }
    }
    
    public synchronized GmbalMBean createRoot() {
        return createRoot( new Root() ) ;
    }

    public synchronized GmbalMBean createRoot(Object root) {
        return createRoot( root, null ) ;
    }

    public synchronized GmbalMBean createRoot(Object root, String name) {
        mm.clear() ;
        checkRootNotCreated( "createRoot" ) ;


        GmbalMBean result ;

        try {
            // Assume successful create, so that AMXMBeanInterface checks that
            // back through getRootParentName will succeed.
            rootCreated = true ;
            result = tree.setRoot( root, name ) ;
            if (result == null) {
                rootCreated = false ;
            }
        } catch (RuntimeException exc) {
            rootCreated = false ;
            throw exc ;
        }

        return result ;
    }

    public synchronized Object getRoot() {
        mm.clear() ;
        // Can be called anytime.
        return tree.getRoot() ;
    }
    
    private synchronized MBeanSkeleton getSkeleton( EvaluatedClassDeclaration cls ) {
        // can be called anytime, otherwise we can't create the root itself!
        mm.enter( registrationDebug(), "getSkeleton", cls ) ;
        
        try {
            MBeanSkeleton result = skeletonMap.get( cls ) ;

            boolean newSkeleton = result == null ;
            if (newSkeleton) {
                mm.info( registrationDebug(), "Skeleton not found" ) ;
                
                Pair<EvaluatedClassDeclaration,EvaluatedClassAnalyzer> pair = 
                    getClassAnalyzer( cls, ManagedObject.class ) ;
                EvaluatedClassAnalyzer ca = pair.second() ;

                EvaluatedClassDeclaration annotatedClass = pair.first() ;
                mm.info( registrationFineDebug(), "Annotated class for skeleton is",
                    annotatedClass ) ;
                if (annotatedClass == null) {
                    throw Exceptions.self.managedObjectAnnotationNotFound(
                        cls.name() ) ;
                }

                MBeanSkeleton skel = new MBeanSkeleton( cls, ca, this ) ;

                if (amxSkeleton == null) {
                    // Can't compose amxSkeleton with itself!
                    result = skel ;
                } else {
                    result = amxSkeleton.compose( skel ) ;
                }

                skeletonMap.put( cls, result ) ;
            }
            
            mm.info(registrationFineDebug() || (registrationDebug() && newSkeleton),
                "Skeleton", new DelayedObjectToString( result, myObjectUtil ) ) ;
            
            return result ;
        } finally {
            mm.exit( registrationDebug() ) ;
        }
    }

    public synchronized TypeConverter getTypeConverter( EvaluatedType type ) {
        // Can be called anytime
        mm.enter( registrationFineDebug(), "getTypeConverter", type ) ;
        
        TypeConverter result = null;
        
        try {
            boolean newTypeConverter = false ;
            result = typeConverterMap.get( type ) ;	
            if (result == null) {
                mm.info( registrationFineDebug(), "Creating new TypeConverter" ) ;
            
                // Store a TypeConverter impl that throws an exception when 
                // acessed.  Used to detect recursive types.
                typeConverterMap.put( type, 
                    new TypeConverterImpl.TypeConverterPlaceHolderImpl( type ) ) ;

                result = TypeConverterImpl.makeTypeConverter( type, this ) ;

                // Replace recursion marker with the constructed implementation
                typeConverterMap.put( type, result ) ;
                newTypeConverter = true ;
            }
            
            mm.info(registrationFineDebug() ||
		(registrationDebug() && newTypeConverter), "result",
		myObjectUtil.objectToString( result ) ) ;
        } finally {
	    mm.exit( registrationFineDebug(), result ) ;
        }
        
        return result ;
    }

    private static Field getDeclaredField( final Class<?> cls,
        final String name )
        throws PrivilegedActionException, NoSuchFieldException {

        SecurityManager sman = System.getSecurityManager() ;
        if (sman == null) {
            return cls.getDeclaredField( name ) ;
        } else {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<Field>() {
                    public Field run() throws Exception {
                        return cls.getDeclaredField( name ) ;
                    }
                }
            ) ;
        }
    }

    private String getAMXTypeFromField( Class<?> cls, String fieldName ) {
        try {
            final Field fld = getDeclaredField(cls, fieldName);

            if (Modifier.isFinal(fld.getModifiers()) 
                && Modifier.isStatic(fld.getModifiers())
                && fld.getType().equals(String.class)) {

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        fld.setAccessible(true);
                        return null;
                    }
                });

                return (String) fld.get(null);
            } else {
                return "";
            }
        } catch (PrivilegedActionException ex) {
            return "" ;
        } catch (IllegalArgumentException ex) {
            return "" ;
        } catch (IllegalAccessException ex) {
            return "" ;
        } catch (NoSuchFieldException ex) {
            return "" ;
        } catch (SecurityException ex) {
            return "" ;
        }
    }

    private boolean goodResult( String str ) {
        return str!=null && str.length()>0 ;
    }

    // XXX Needs Test for the AMX_TYPE case
    public synchronized String getTypeName( Class<?> cls, String fieldName,
        String nameFromAnnotation ) {
        // Can be called anytime
        String result = getAMXTypeFromField( cls, fieldName ) ;
        if (goodResult( result)) {
            return result ;
        }

	// Next, check for annotations?
        if (goodResult( nameFromAnnotation)) {
            return nameFromAnnotation ;
        }

        String className = cls.getName() ;

	// Next, check stripPrefixes
        for (String str : typePrefixes ) {
            if (className.startsWith( str ) ) {
                return className.substring( str.length() + 1 ) ;
            }
        }
        
        // The result is either the class name, or the class name without
	// package prefix (if any) if stripPackagePrefix has been set.
        if (stripPackagePrefix) {
            int lastDot = className.lastIndexOf( '.' ) ;
            if (lastDot == -1) {
                return className ;
            } else {
                return className.substring( lastDot + 1 ) ;
            }
        } else {
            return className ;
        }
    }

    public synchronized boolean isManagedObject( Object obj ) {
        final EvaluatedClassDeclaration cdecl =
            (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(
                obj.getClass() ) ;
        final ManagedObject mo = getFirstAnnotationOnClass( cdecl,
            ManagedObject.class ) ;

        return mo != null ;
    }
    
    public synchronized MBeanImpl constructMBean( MBeanImpl parentEntity,
        Object obj, String name ) {

        // Can be called anytime
        MBeanImpl result = null ;
        
        mm.enter( registrationDebug(), "constructMean", obj, name ) ;
        
        String objName = name ;
        try {
            final Class<?> cls = obj.getClass() ;
            final EvaluatedClassDeclaration cdecl = 
                (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(cls) ;
            final MBeanSkeleton skel = getSkeleton( cdecl ) ;

            AMXMetadata amd = getFirstAnnotationOnClass( cdecl, AMXMetadata.class ) ;
            if (amd == null) {
                amd = getDefaultAMXMetadata() ;
            }

            String type = skel.getType() ;
	    mm.info( registrationDebug(), "Stripped type", type ) ;

            result = new MBeanImpl( skel, obj, server, type ) ;
            
            if (objName == null) {
                objName = skel.getNameValue( result ) ;
                if (objName == null) {
                    objName = "" ;
                }
            }  

            if (objName.length() == 0) {
                if (!amd.isSingleton()) {
                    throw Exceptions.self.nonSingletonRequiresName( 
                        parentEntity, type ) ;
                }
            } else {
                if (amd.isSingleton()) {
                    throw Exceptions.self.singletonCannotSpecifyName( 
                        parentEntity, type, name ) ;
                }
            }
           
            mm.info( registrationDebug(), "Name value =", objName ) ;
            
            result.name( objName ) ;
        } catch (JMException exc) {
            throw Exceptions.self.errorInConstructingMBean( objName, exc ) ;
        } finally {
            mm.exit( registrationDebug(), result ) ;
        }
        
        return result ;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized GmbalMBean register( final Object parent,
        final Object obj, final String name ) {

        mm.clear() ;
        checkRootCreated("register");
        mm.enter( registrationDebug(), "register", parent, obj, name ) ;
        
        if (obj instanceof String) {
            throw Exceptions.self.objStringWrongRegisterCall( (String)obj ) ;
        }

        // Construct the MBean
        try {
            MBeanImpl parentEntity = tree.getParentEntity(parent) ;

            final MBeanImpl mb = constructMBean( parentEntity, obj, name ) ;
            
            return tree.register( parentEntity, obj, mb) ;
    	} catch (JMException exc) {
            throw Exceptions.self.exceptionInRegister(exc) ;
        } finally {
            mm.exit( registrationDebug() ) ;
        }
    }
    
    public synchronized GmbalMBean register( final Object parent,
        final Object obj ) {

        return register( parent, obj, null ) ;
    }

    
    public synchronized GmbalMBean registerAtRoot(Object obj, String name) {
        return register( tree.getRoot(), obj, name ) ;
    }

    public synchronized GmbalMBean registerAtRoot(Object obj) {
        return register( tree.getRoot(), obj, null ) ;
    }
    
    public synchronized void unregister( Object obj ) {
        mm.clear() ;
        checkRootCreated("unregister");
        mm.enter( registrationDebug(), "unregister", obj ) ;
        
        try {
            tree.unregister( obj ) ;
        } catch (JMException exc) {
            throw Exceptions.self.exceptionInUnregister(exc) ;
        } finally {
            mm.exit( registrationDebug() ) ;
        }
    }

    public synchronized ObjectName getObjectName( Object obj ) {
        mm.clear() ;
        checkRootCreated("getObjectName");
        mm.enter( registrationDebug(), "getObjectName", obj ) ;

        if (obj instanceof ObjectName) {
            return (ObjectName)obj ;
        }

        if (obj instanceof AMXClient) {
            return ((AMXClient)obj).objectName() ;
        }

        ObjectName result = null;
        try {
            result = tree.getObjectName( obj ) ;
        } finally {
            mm.exit( registrationDebug(), result ) ;
        }
        
        return result ;
    }

    public AMXClient getAMXClient(Object obj) {
        ObjectName oname = getObjectName( obj ) ;
        if (oname == null) {
            return null ;
        }

        return new AMXClient( server, oname ) ;
    }

    public synchronized Object getObject( ObjectName oname ) {
        checkRootCreated("getObject");
        mm.enter( registrationDebug(), "getObject", oname ) ;
        
        Object result = null ;
        try {
            result = tree.getObject( oname ) ;
	} finally {
            mm.exit( registrationDebug(), result ) ;
        }
        
        return result ;
    }
    
    public synchronized FacetAccessor getFacetAccessor( Object obj ) {
        // Can be called anytime
        MBeanImpl mb = tree.getMBeanImpl( obj ) ;
        if (mb != null) {
            return tree.getFacetAccessor( obj ) ;
        } else {
            return new FacetAccessorImpl( obj ) ;
        }
    }   
    
    public synchronized String getDomain() {
        // Can be called anytime
	return domain ;
    }

    public synchronized void setMBeanServer( MBeanServer server ) {
        mm.clear() ;
        checkRootNotCreated("setMBeanServer");
	this.server = server ;
    }

    public synchronized MBeanServer getMBeanServer() {
        // Can be called anytime
	return server ;
    }

    public synchronized void setResourceBundle( ResourceBundle rb ) {
        mm.clear() ;
        checkRootNotCreated("setResourceBundle");
        this.resourceBundle = rb ;
    }

    public synchronized ResourceBundle getResourceBundle() {
        // Can be called anytime
        return resourceBundle ;
    }
    
    public synchronized String getDescription( EvaluatedDeclaration element ) {
        // Can be called anytime
        Description desc ;
        if (element instanceof EvaluatedClassDeclaration) {
            EvaluatedClassDeclaration ecd = (EvaluatedClassDeclaration)element;
            desc = getFirstAnnotationOnClass(ecd, Description.class ) ;
        } else {
            desc = getAnnotation( element.element(), Description.class ) ;
        }

        String result = "" ;
        if (desc != null) {
            result = desc.value() ;
        }

        if (result.length() == 0) {
            result = Exceptions.self.noDescriptionAvailable() ;
        } else {
            if (resourceBundle != null) {
                result = resourceBundle.getString( result ) ;
            }
        }

        return result ;
    }
    
    
    public synchronized void addAnnotation( AnnotatedElement element,
        Annotation annotation ) {
        mm.clear() ;
        checkRootNotCreated("addAnnotation");
        mm.enter( registrationDebug(), "addAnnotation", element, annotation ) ;
        if (annotation == null) {
            throw Exceptions.self.cannotAddNullAnnotation( element ) ;
        }

        try {
            Map<Class, Annotation> map = addedAnnotations.get( element ) ;
            if (map == null) {
	        mm.info( registrationDebug(),
		    "Creating new Map<Class,Annotation>" ) ;
                
                map = new HashMap<Class, Annotation>() ;
                addedAnnotations.put( element, map ) ;
            }

            Class<?> annotationType = annotation.annotationType() ;
            Annotation ann = map.get( annotationType ) ;
            if (ann != null) {
                mm.info( registrationDebug(), "Duplicate annotation") ;
                
                throw Exceptions.self.duplicateAnnotation( element, 
                    annotation.getClass().getName()) ;
            }

            map.put( annotationType, annotation ) ;
        } finally {
            mm.exit( registrationDebug() ) ;
        }
    }

    public <T extends Annotation> T getFirstAnnotationOnClass(
        final EvaluatedClassDeclaration element, final Class<T> type ) {

        EvaluatedClassAnalyzer eca = new EvaluatedClassAnalyzer( element ) ;
        List<EvaluatedClassDeclaration> ecds = eca.findClasses(
            forAnnotation(type, EvaluatedClassDeclaration.class) ) ;

        if (ecds.size() > 0) {
            return getAnnotation( ecds.get(0).element(), type ) ;
        }  else {
            return null ;
        }
    }

    private Map<AnnotatedElement,Map<Class,Annotation>> annotationCache =
        new WeakHashMap<AnnotatedElement,Map<Class,Annotation>>() ;

    private Map<Class,Annotation> getAllAnnotations( final Class cls ) {
        Map<Class,Annotation> result = annotationCache.get( cls ) ;

        if (result == null) {
            final Map<Class,Annotation> res =
                new HashMap<Class,Annotation>() ;

            ClassAnalyzer ca = ClassAnalyzer.getClassAnalyzer(cls) ;
            ca.findClasses( new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    // First, put in declared annotations if not already present.
                    Annotation[] annots = arg.getDeclaredAnnotations() ;
                    for (Annotation anno : annots) {
                        putIfNotPresent( res, anno.annotationType(), anno ) ;
                    }

                    // Then, put in added annotations if not already present.
                    Map<Class,Annotation> emap = addedAnnotations.get( arg ) ;
                    if (emap != null) {
                        for (Map.Entry<Class,Annotation> entry : emap.entrySet()) {
                            putIfNotPresent( res, entry.getKey(),
                                entry.getValue()) ;
                        }
                    }

                    return true ; // evaluate everything
                }
            }) ;

            annotationCache.put( cls, res ) ;
            result = res ;
        }

        return result ;
    }

    @SuppressWarnings({"unchecked"})
    public synchronized <T extends Annotation> T getAnnotation( 
        AnnotatedElement element, Class<T> type ) {

        // Can be called anytime
        mm.enter( registrationFineDebug(), "getAnnotation", element,
            type.getName() ) ;
        
        try {
            if (element instanceof Class) {
                Class cls = (Class)element ;
                Map<Class,Annotation> annos = getAllAnnotations(cls) ;
                return (T)annos.get( type ) ;
            } else {
                T result = element.getAnnotation( type ) ;
                if (result == null) {
                    mm.info( registrationFineDebug(),
                        "No annotation on element: trying addedAnnotations map" ) ;

                    Map<Class, Annotation> map = addedAnnotations.get(
                        element );
                    if (map != null) {
                        result = (T)map.get( type ) ;
                    }
                }

                mm.info( registrationFineDebug(), "result" + result ) ;

                return result ;
            }
        } finally {
            mm.exit( registrationFineDebug() ) ;
        }
    }

    public synchronized Collection<Annotation> getAnnotations(
        AnnotatedElement elem ) {

        // Can be called anytime
        mm.enter( registrationFineDebug(), "getAnnotations", elem ) ;

        try {
            if (elem instanceof Class) {
                Class cls = (Class)elem ;

                return getAllAnnotations( cls ).values() ;
            } else if (elem instanceof Method) {
                return Arrays.asList( elem.getAnnotations() ) ;
            } else if (elem instanceof Field) {
                return Arrays.asList( elem.getAnnotations() ) ;
            } else {
                // error
                throw Exceptions.self.annotationsNotSupported( elem ) ;
            }
        } finally {
            mm.exit( registrationFineDebug() ) ;
        }
    }

    public synchronized Pair<EvaluatedClassDeclaration,EvaluatedClassAnalyzer>
        getClassAnalyzer( final EvaluatedClassDeclaration cls,
        final Class<? extends Annotation> annotationClass ) {
        // Can be called anytime
        mm.enter( registrationDebug(), "getClassAnalyzer", cls,
            annotationClass ) ;
        
        try {
            final EvaluatedClassAnalyzer clsca = new EvaluatedClassAnalyzer( cls ) ;

            final EvaluatedClassDeclaration annotatedClass = Algorithms.getFirst(
                clsca.findClasses( forAnnotation( annotationClass,
                    EvaluatedClassDeclaration.class ) ),
                new Runnable() {
                    public void run() {
                        throw Exceptions.self.noAnnotationFound(
                            annotationClass.getName(), cls.name() ) ;
                    }
                } ) ;

            mm.info( registrationDebug(), "annotatedClass = " + annotatedClass ) ;
    
            final List<EvaluatedClassDeclaration> classes =
                new ArrayList<EvaluatedClassDeclaration>() ;
            classes.add( cls ) ;

            // XXX Should we construct a union of all @IncludeSubclass contents?
            final IncludeSubclass incsub = getFirstAnnotationOnClass(
                cls, IncludeSubclass.class ) ;
            if (incsub != null) {
                for (Class<?> klass : incsub.value()) {
                    EvaluatedClassDeclaration ecd = 
                        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(klass) ;
                    classes.add( ecd ) ;
                    mm.info( registrationDebug(), "included subclass", klass ) ;
                }
            }

            EvaluatedClassAnalyzer ca = new EvaluatedClassAnalyzer( classes ) ;

            return new Pair<EvaluatedClassDeclaration,
                 EvaluatedClassAnalyzer>( annotatedClass, ca ) ;
        } finally {
            mm.exit( registrationDebug() ) ;
        }
    }
    
    public synchronized List<InheritedAttribute> getInheritedAttributes( 
        final EvaluatedClassAnalyzer ca ) {
        // Can be called anytime
        
        mm.enter( registrationDebug(), "getInheritedAttributes", ca ) ;
        
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
                        final InheritedAttribute ia = getFirstAnnotationOnClass(cls,
                            InheritedAttribute.class);
                        final InheritedAttributes ias = getFirstAnnotationOnClass(cls,
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
	    mm.exit( registrationDebug() ) ;
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
        // Can be called anytime
        mm.enter( registrationFineDebug(), "putIfNotPresent", key, value ) ;
        
        try {
            if (!map.containsKey( key )) {
                mm.info( registrationFineDebug(), "Adding key, value to map" ) ;
                map.put( key, value ) ;
            } else {
                mm.info( registrationFineDebug(), "Key,value already in map" ) ;
            }
        } finally {
            mm.exit( registrationFineDebug() ) ;
        }
    }

    // Only final fields of immutable type can be used as ManagedAttributes.
    static void checkFieldType( EvaluatedFieldDeclaration field ) {
        if (!Modifier.isFinal( field.modifiers() ) ||
            !field.fieldType().isImmutable()) {
            Exceptions.self.illegalAttributeField(field) ;
        }
    }

    // Returns a pair of maps defining all managed attributes in the ca.  The first map
    // is all setters, and the second is all getters.  Only the most derived version is present.
    public synchronized Pair<Map<String,AttributeDescriptor>,
        Map<String,AttributeDescriptor>>
        getAttributes( 
            final EvaluatedClassAnalyzer ca,
            final ManagedObjectManagerInternal.AttributeDescriptorType adt ) {
        // Can be called anytime
        // mm.enter( registrationDebug(), "getAttributes" ) ;

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

            ca.findFields( new Predicate<EvaluatedFieldDeclaration>() {
                public boolean evaluate( EvaluatedFieldDeclaration field ) {
                    ManagedAttribute ma = getAnnotation( field.element(),
                        ManagedAttribute.class ) ;
                    if (ma == null) {
                        return false ;
                    } else {
                        checkFieldType( field ) ;

                        Description desc = getAnnotation( field.element(),
                            Description.class ) ;
                        String description ;
                        if (desc == null) {
                            description = "No description available for "
                                + field.name() ;
                        } else {
                            description = desc.value() ;
                        }

                        AttributeDescriptor ad =
                            AttributeDescriptor.makeFromAnnotated(
                                 ManagedObjectManagerImpl.this, field,
                                 ma.id(), description, adt ) ;

                        putIfNotPresent( getters, ad.id(), ad ) ;

                        return true ;
                    }
                } } ) ;

            ca.findMethods( new Predicate<EvaluatedMethodDeclaration>() {
                public boolean evaluate( EvaluatedMethodDeclaration method ) {
                    ManagedAttribute ma = getAnnotation( method.element(),
                        ManagedAttribute.class ) ;
                    AttributeDescriptor ad ;
                    if (ma == null) {
                        ad = getAttributeDescriptorIfInherited( method, ias,
                            adt ) ;
                    } else {
                        Description desc = getAnnotation( method.element(),
                            Description.class ) ;
                        String description ;
                        if (desc == null) {
                            description = "No description available for "
                                + method.name() ;
                        } else {
                            description = desc.value() ;
                        }

                        ad = AttributeDescriptor.makeFromAnnotated(
                            ManagedObjectManagerImpl.this,
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
	    // mm.exit( registrationDebug() ) ;
        }
    }

    public synchronized void setRegistrationDebug( 
        ManagedObjectManager.RegistrationDebugLevel level ) {
        // can be called anytime
        regDebugLevel = level ;
    }

    public synchronized void setJMXRegistrationDebug(boolean flag) {
        jmxRegistrationDebugFlag = flag ;
    }
    
    public synchronized void setRuntimeDebug( boolean flag ) {
        // can be called anytime
        runDebugFlag = flag ;
    }

    public synchronized void setTypelibDebug( int level ) {
        // can be called anytime
        TypeEvaluator.setDebugLevel(level);
    }
    
    public synchronized String dumpSkeleton( Object obj ) {
        // can be called anytime
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
        // can be called anytime
        return regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.NORMAL 
            || regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.FINE ;
    }
    
    public synchronized boolean registrationFineDebug() {
        // can be called anytime
        return regDebugLevel == ManagedObjectManager.RegistrationDebugLevel.FINE ;
    }
    
    public synchronized boolean runtimeDebug() {
        // can be called anytime
        return runDebugFlag ;
    }

    public synchronized boolean jmxRegistrationDebug() {
        return jmxRegistrationDebugFlag ;
    }
    
    public synchronized void stripPrefix( String... args ) {
        checkRootNotCreated("stripPrefix" ) ;
        for (String str : args) {
            typePrefixes.add( str ) ;
        }
    }
    
    public synchronized <T extends EvaluatedDeclaration> Predicate<T> forAnnotation(
        final Class<? extends Annotation> annotation,
        final Class<T> cls ) {
        // Can be called anytime

        return new Predicate<T>() {
            public boolean evaluate( T elem ) {
                return getAnnotation( elem.element(), annotation ) != null ;
            }
        } ;
    }

    public AMXMetadata getDefaultAMXMetadata() {
        return DEFAULT_AMX_METADATA ;
    }

    public boolean isAMXAttributeName( String name ) {
        return amxAttributeNames.contains( name ) ;
    }

    public void suppressDuplicateRootReport(boolean suppressReport) {
        checkRootNotCreated("suppressDuplicateRootReport");
        tree.setSuppressDuplicateSetRootReport( suppressReport ) ;
    }
}
