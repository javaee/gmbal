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

import org.glassfish.gmbal.AMXMetadata;
import java.util.List ;
import java.util.Arrays ;
import java.util.ArrayList ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.Set ;
import java.util.HashSet ;
import java.util.Iterator ;
import java.util.concurrent.atomic.AtomicLong ;

import java.lang.reflect.Method ;
import java.lang.reflect.ReflectPermission;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import javax.management.Attribute ;
import javax.management.AttributeList ;
import javax.management.MBeanException ;
import javax.management.InvalidAttributeValueException ;
import javax.management.AttributeNotFoundException ;
import javax.management.ReflectionException ;
import javax.management.MBeanParameterInfo ;

import javax.management.NotificationBroadcasterSupport ;
import javax.management.AttributeChangeNotification ;

import org.glassfish.gmbal.generic.BinaryFunction ;
import org.glassfish.gmbal.NameValue ;
import org.glassfish.gmbal.ManagedOperation ;
import org.glassfish.gmbal.ParameterNames ;

import org.glassfish.gmbal.generic.DprintUtil;
import org.glassfish.gmbal.generic.DumpIgnore;
import org.glassfish.gmbal.generic.Pair ;
import org.glassfish.gmbal.generic.DumpToString ;

import org.glassfish.gmbal.generic.FacetAccessor;
import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import org.glassfish.gmbal.typelib.EvaluatedClassAnalyzer;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;

public class MBeanSkeleton {
    // Object evaluate( Object, List<Object> ) 
    // (or Result evaluate( Target, ArgList ))
    public interface Operation
        extends BinaryFunction<FacetAccessor,List<Object>,Object> {} ;

    private Descriptor descriptor ;
    private final String type ;
    private AMXMetadata mbeanType ;
    @DumpToString
    private final AtomicLong sequenceNumber ;
    private final ModelMBeanInfoSupport mbInfo ;
    @DumpToString
    private final ManagedObjectManagerInternal mom ;
    @DumpIgnore
    private final DprintUtil dputil ;
    private final Map<String,AttributeDescriptor> setters ;
    private final Map<String,AttributeDescriptor> getters ;
    private AttributeDescriptor nameAttributeDescriptor ;
    private final Map<String,Map<List<String>,Operation>> operations ;
    private final List<ModelMBeanAttributeInfo> mbeanAttributeInfoList ;
    private final List<ModelMBeanOperationInfo> mbeanOperationInfoList ;
 
    private <K,L,V> void addToCompoundMap( Map<K,Map<L,V>> source, Map<K,Map<L,V>> dest ) {
        for (Map.Entry<K,Map<L,V>> entry : source.entrySet()) {
            Map<L,V> map = entry.getValue() ;
            if (map == null) {
                map = new HashMap<L,V>() ;
                dest.put( entry.getKey(), map ) ;
            }
            map.putAll( source.get( entry.getKey() ) ) ;
        }
    }

    private MBeanSkeleton( MBeanSkeleton first, MBeanSkeleton second ) {
        dputil = new DprintUtil( getClass() ) ;
        this.mom = first.mom ;

        type = first.type ;

        sequenceNumber = new AtomicLong() ;
        setters = new HashMap<String,AttributeDescriptor>() ;
            setters.putAll( second.setters ) ;
            setters.putAll( first.setters ) ;

        getters = new HashMap<String,AttributeDescriptor>() ;
            getters.putAll( second.getters ) ;
            getters.putAll( first.getters ) ;

        operations = new HashMap<String,Map<List<String>,Operation>>() ;
            addToCompoundMap( second.operations, operations ) ;
            addToCompoundMap( first.operations, operations ) ;

        mbeanAttributeInfoList = new ArrayList<ModelMBeanAttributeInfo>() ;
            mbeanAttributeInfoList.addAll( second.mbeanAttributeInfoList ) ;
            mbeanAttributeInfoList.addAll( first.mbeanAttributeInfoList ) ;

        mbeanOperationInfoList = new ArrayList<ModelMBeanOperationInfo>() ;
            mbeanOperationInfoList.addAll( second.mbeanOperationInfoList ) ;
            mbeanOperationInfoList.addAll( first.mbeanOperationInfoList ) ;

        descriptor = DescriptorUtility.union( first.descriptor,
            second.descriptor ) ;

        mbInfo = makeMbInfo( first.mbInfo.getDescription() ) ;
    }

    private ModelMBeanInfoSupport makeMbInfo( String description ) {
        ModelMBeanAttributeInfo[] attrInfos = mbeanAttributeInfoList.toArray(
            new ModelMBeanAttributeInfo[mbeanAttributeInfoList.size()] ) ;
        ModelMBeanOperationInfo[] operInfos = mbeanOperationInfoList.toArray(
            new ModelMBeanOperationInfo[mbeanOperationInfoList.size() ] ) ;

        return new ModelMBeanInfoSupport(
            type, description, attrInfos, null,
                operInfos, null, descriptor ) ;
    }

    /** Create a new MBeanSkeleton that is the composition of this one
     * and skel.  Note that, if this and skel contain the same attribute,
     * the version from skel will appear in the composition.
     */
    public MBeanSkeleton compose( MBeanSkeleton skel ) {
        return new MBeanSkeleton( skel, this ) ;
    }

    private enum DescriptorType { mbean, attribute, operation }

    // Create a valid descriptor so that ModelMBinfoSupport won't throw
    // an exception.
    Descriptor makeValidDescriptor( Descriptor desc, DescriptorType dtype,
        String dname ) {

	Map<String,Object> map = new HashMap<String,Object>() ;
	String[] names = desc.getFieldNames() ;
	Object[] values = desc.getFieldValues( (String[])null ) ;
	for (int ctr=0; ctr<names.length; ctr++ ) {
	    map.put( names[ctr], values[ctr] ) ;
	}

        map.put( "descriptorType", dtype.toString() ) ;
        if (dtype == DescriptorType.operation) {
            map.put( "role", "operation" ) ;
            map.put( "targetType", "ObjectReference" ) ;
        } else if (dtype == DescriptorType.mbean) {
            map.put( "persistPolicy", "never" ) ;
            map.put( "log", "F" ) ;
            map.put( "visibility", "1" ) ;
	}

        map.put( "name", dname ) ;
	map.put( "displayName", dname ) ;

        return DescriptorUtility.makeDescriptor( map ) ;
    }

    @Override
    public String toString() {
        return "DynamicMBeanSkeleton[type=" + type + "]" ;
    }
    
    // This method should only be called when getter.id.equals( setter.id ) 
    private void processAttribute( AttributeDescriptor getter, 
        AttributeDescriptor setter ) {

        if (mom.registrationFineDebug()) {
            dputil.enter( "processAttribute", "getter=", getter,
                "setter=", setter ) ;
        }
        
        try {
            if ((setter == null) && (getter == null)) {
                throw Exceptions.self.notBothNull() ;
            }

            if ((setter != null) && (getter != null) 
                && !setter.type().equals( getter.type() )) {

                throw Exceptions.self.typesMustMatch() ;
            }

            AttributeDescriptor nonNullDescriptor =
                (getter != null) ? getter : setter ;

            String name = nonNullDescriptor.id() ;
            String description = nonNullDescriptor.description() ;
            Descriptor desc = DescriptorUtility.EMPTY_DESCRIPTOR ;
            if (getter != null) {
                desc = DescriptorUtility.union( desc,
                    DescriptorIntrospector.descriptorForElement(
                        getter.method() ) );
            }

            if (setter != null) {
                desc = DescriptorUtility.union( desc,
                    DescriptorIntrospector.descriptorForElement(
                        setter.method() ) );
            }

            desc = makeValidDescriptor( desc, DescriptorType.attribute, name ) ;

            if (mom.registrationFineDebug()) {
                dputil.info( "name=", name, "description=", description,
                    "desc=", desc ) ;
            }
            
            TypeConverter tc = mom.getTypeConverter( nonNullDescriptor.type() ) ;

            ModelMBeanAttributeInfo ainfo = new ModelMBeanAttributeInfo( name,
                description, tc.getManagedType().toString(),
                getter != null, setter != null, false, desc ) ;
            
            if (mom.registrationFineDebug()) {
                dputil.info("ainfo=", ainfo ) ;
            }

            mbeanAttributeInfoList.add( ainfo ) ;
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private void analyzeFields( EvaluatedClassAnalyzer ca ) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeFields", "ca=", ca ) ;
        }

        try {

        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private void analyzeAttributes( EvaluatedClassAnalyzer ca ) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeAttributes", "ca=", ca ) ;
        }
        
        try {
            Pair<Map<String,AttributeDescriptor>,
                Map<String,AttributeDescriptor>> amap =
                mom.getAttributes( ca,
                    ManagedObjectManagerInternal.AttributeDescriptorType.MBEAN_ATTR ) ;

            getters.putAll( amap.first() ) ;
            setters.putAll( amap.second() ) ;
            
            if (mom.registrationFineDebug()) {
                dputil.info( "attributes=", amap ) ;
            }

            final Set<String> setterNames = new HashSet<String>(setters.keySet()) ;
            if (mom.registrationFineDebug()) {
                dputil.info( "(Before removing getters):setterNames=", setterNames ) ;
            }
            
            for (String str : getters.keySet()) {
                processAttribute( getters.get( str ), setters.get( str ) ) ;
                setterNames.remove( str ) ;
            }
            
            if (mom.registrationFineDebug()) {
                dputil.info( "(After removing getters):setterNames=", setterNames ) ;
            }
   
            // Handle setters without getters
            for (String str : setterNames) {
                processAttribute( null, setters.get( str ) ) ;
            }
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private void analyzeObjectNameKeys( EvaluatedClassAnalyzer ca) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeObjectNameKeys", "ca=", ca ) ;
        }
        
        try {
            final List<EvaluatedMethodDeclaration> annotatedMethods =
                ca.findMethods( mom.forAnnotation( NameValue.class,
                    EvaluatedMethodDeclaration.class )) ;
            
            if (annotatedMethods.size() == 0) {
                return ;
            }
            
            // If there are two methods with @NameValue in the same
            // class, we have an error.
            EvaluatedMethodDeclaration annotatedMethod = annotatedMethods.get(0) ;
            if (annotatedMethods.size() > 1) {
                EvaluatedMethodDeclaration second = annotatedMethods.get(1) ;
                
                if (annotatedMethod.containingClass().equals(
                    second.containingClass())) {

                    throw Exceptions.self.duplicateObjectNameKeyAttributes(
                        annotatedMethod, second,
                        annotatedMethod.containingClass().name() ) ;
                }
            } 

            if (mom.registrationFineDebug()) {
                dputil.info( "annotatedMethod=", annotatedMethod ) ;
            }
            
            nameAttributeDescriptor = AttributeDescriptor.makeFromAnnotated(
                mom, annotatedMethod, "Name",
                Exceptions.self.nameOfManagedObject(),
                ManagedObjectManagerInternal.AttributeDescriptorType.MBEAN_ATTR ) ;
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }
    private static final Permission accessControlPermission =
        new ReflectPermission( "suppressAccessChecks" ) ;

    private Pair<Operation,ModelMBeanOperationInfo> makeOperation(
        final EvaluatedMethodDeclaration m ) {
	
        if (mom.registrationFineDebug()) {
            dputil.enter( "makeOperation", "m=", m ) ;
        }

        SecurityManager sman = System.getSecurityManager() ;
        if (sman != null) {
            sman.checkPermission( accessControlPermission ) ;
        }

        AccessController.doPrivileged(
            new PrivilegedAction<Method>() {
                public Method run() {
                    m.method().setAccessible(true);
                    return m.method() ;
                }
            }
        ) ;

        try {
            final String desc = mom.getDescription( m ) ;
            final EvaluatedType rtype = m.returnType() ;
            final TypeConverter rtc = rtype == null ? null : mom.getTypeConverter( 
                rtype ) ;
            final List<EvaluatedType> atypes = m.parameterTypes() ;
            final List<TypeConverter> atcs = new ArrayList<TypeConverter>() ;
            final ManagedOperation mo = mom.getAnnotation( m,
                ManagedOperation.class ) ;
            
            Descriptor modelDescriptor = makeValidDescriptor(
                DescriptorIntrospector.descriptorForElement( m.element() ),
                DescriptorType.operation, m.name() ) ;

            for (EvaluatedType ltype : atypes) {
                atcs.add( mom.getTypeConverter( ltype ) ) ;
            }

            if (mom.registrationFineDebug()) {
                dputil.info( "desc=", desc ) ;
                dputil.info( "rtype=", rtype ) ;
                dputil.info( "rtc=", rtc ) ;
                dputil.info( "atcs=", atcs ) ;
                dputil.info( "atypes=", atypes ) ;
                dputil.info( "descriptor=", modelDescriptor ) ;
            }
            
            final Operation oper = new Operation() {
                public Object evaluate( FacetAccessor target, List<Object> args ) {
                    if (mom.runtimeDebug()) {
                        dputil.enter( "Operation:evaluate", "taget=", target, 
                            "args=", args ) ;
                    }

                    Object[] margs = new Object[args.size()] ;
                    Iterator<Object> argsIterator = args.iterator() ;
                    Iterator<TypeConverter> tcIterator = atcs.iterator() ;
                    int ctr = 0 ;
                    while (argsIterator.hasNext() && tcIterator.hasNext()) {
                        final Object arg = argsIterator.next() ;
                        final TypeConverter tc = tcIterator.next() ;
                        margs[ctr++] = tc.fromManagedEntity( arg ) ;
                    }

                    if (mom.runtimeDebug()) {
                        dputil.info( "Before invoke: margs=", Arrays.asList( margs ) ) ;
                    }

                    Object result = target.invoke( m.method(), mom.runtimeDebug(),
                        margs ) ;

                    if (mom.runtimeDebug()) {
                        dputil.info( "After invoke: result=", result ) ;
                    }

                    if (rtc == null) {
                        return null ;
                    } else {
                        return rtc.toManagedEntity( result ) ;
                    }
                }
            } ;

            final ParameterNames pna = m.annotation( ParameterNames.class ) ;
            if (mom.registrationFineDebug()) {
                dputil.info( "pna=", pna.value() ) ;
            }
            
            if (pna != null && pna.value().length != atcs.size()) {
                throw Exceptions.self.parameterNamesLengthBad() ;
            }

            final MBeanParameterInfo[] paramInfo = 
                new MBeanParameterInfo[ atcs.size() ] ;
            int ctr = 0 ;
            for (TypeConverter tc : atcs) {
                paramInfo[ctr] = new MBeanParameterInfo(
                    (pna == null) ? "arg" + ctr : pna.value()[ctr], 
                    tc.getManagedType().toString(), desc ) ;
                ctr++ ;
            }

            final ModelMBeanOperationInfo operInfo =
                new ModelMBeanOperationInfo( m.name(),
                desc, paramInfo, rtc.getManagedType().toString(),
                mo.impact().ordinal(), modelDescriptor ) ;

            if (mom.registrationFineDebug()) {
                dputil.info( "operInfo=", operInfo ) ;
            }
            
            return new Pair<Operation,ModelMBeanOperationInfo>( oper, operInfo ) ;
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private void analyzeOperations( EvaluatedClassAnalyzer ca ) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeOperations", "ca=", ca ) ;
        }
        
        try {
            // Scan for all methods annotation with @ManagedOperation, 
            // including inherited methods.
            final List<EvaluatedMethodDeclaration> ops = ca.findMethods( mom.forAnnotation(
                ManagedOperation.class, EvaluatedMethodDeclaration.class ) ) ;
            for (EvaluatedMethodDeclaration m : ops) {
                final Pair<Operation,ModelMBeanOperationInfo> data =
                    makeOperation( m ) ;
                final ModelMBeanOperationInfo info = data.second() ;

                final List<String> dataTypes = new ArrayList<String>() ;
                for (MBeanParameterInfo pi : info.getSignature()) {
                    // Replace recursion marker with the constructed implementation
                    dataTypes.add( pi.getType() ) ;
                }

                Map<List<String>,Operation> map = operations.get( m.name() ) ;
                if (map == null) {
                    map = new HashMap<List<String>,Operation>() ;
                    operations.put( m.name(), map ) ;
                }

                // Note that the first occurrence of any method will be the most
                // derived, so if there is already an entry, don't overwrite it.
                mom.putIfNotPresent( map, dataTypes, data.first() ) ;

                mbeanOperationInfoList.add( info ) ;
            }
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    private String getTypeName( final AMXMetadata mbt,
        final Class<?> cls ) {
        
        String result ;
        if (mbt.pathPart().length() > 0) {
            result = mbt.pathPart() ;
        } else {
            result = mom.getStrippedName( cls ) ;
        }
        
        return result ;
    }

    @AMXMetadata
    private static class DefaultMBeanTypeHolder{} 
    private static AMXMetadata defaultMBeanType =
        DefaultMBeanTypeHolder.class.getAnnotation( AMXMetadata.class ) ;

    public MBeanSkeleton( final EvaluatedClassDeclaration annotatedClass,
        final EvaluatedClassAnalyzer ca,
        final ManagedObjectManagerInternal mom ) {

        dputil = new DprintUtil( getClass() ) ;
        this.mom = mom ;
        
        mbeanType = mom.getAnnotation(annotatedClass, AMXMetadata.class ) ;
        if (mbeanType == null) {
            mbeanType = defaultMBeanType ;
        }
        
        type = getTypeName( mbeanType, annotatedClass.cls() ) ;
        sequenceNumber = new AtomicLong() ;
        setters = new HashMap<String,AttributeDescriptor>() ;
        getters = new HashMap<String,AttributeDescriptor>() ;
        operations = new HashMap<String,Map<List<String>,Operation>>() ;
        mbeanAttributeInfoList = new ArrayList<ModelMBeanAttributeInfo>() ;
        mbeanOperationInfoList = new ArrayList<ModelMBeanOperationInfo>() ;

        analyzeAttributes( ca ) ;
        analyzeFields( ca ) ;
        analyzeOperations( ca ) ;
        analyzeObjectNameKeys( ca ) ;

        descriptor = makeValidDescriptor(
            DescriptorIntrospector.descriptorForElement(
                annotatedClass.cls() ), DescriptorType.mbean, type ) ;

        mbInfo = makeMbInfo( mom.getDescription( annotatedClass ) ) ;
    }

    // The rest of the methods are used in the DynamicMBeanImpl code.
    
    public String getType() {
        if (mom.runtimeDebug()) {
            dputil.enter( "getType" ) ;
        }
        
        try {
            return type ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( type ) ;
            }
        }
    }

    public AMXMetadata getMBeanType() {
        return mbeanType ;
    }
    
    public Object getAttribute( FacetAccessor fa, String name) 
        throws AttributeNotFoundException, MBeanException, ReflectionException {

        if (mom.runtimeDebug()) {
            dputil.enter( "getAttribute", "fa=", fa, "name=", name ) ;
        }
        
        Object result = null ;
        try {
            AttributeDescriptor getter = getters.get( name ) ;
            if (getter == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Error in finding getter ", name ) ;
                }
                throw Exceptions.self.couldNotFindAttribute( name ) ;
            }
            result = getter.get( fa, mom.runtimeDebug() ) ;
            return result ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( result ) ; 
            }
        }
    }
    
    public void setAttribute( final NotificationBroadcasterSupport emitter, 
        final FacetAccessor fa, final Attribute attribute) 
        throws AttributeNotFoundException, InvalidAttributeValueException, 
        MBeanException, ReflectionException  {
        
        if (mom.runtimeDebug()) {
            dputil.enter( "setAttribute", "emitter=", emitter,
                "fa=", fa, "attribute=", attribute ) ;
        }

        try {
            final String name = attribute.getName() ;
            final Object value = attribute.getValue() ;
            final AttributeDescriptor getter = getters.get( name ) ;
            final Object oldValue = (getter == null) ?
                null :
                getter.get( fa, mom.runtimeDebug() ) ;

            if (mom.runtimeDebug()) {
                dputil.info( "oldValue=", oldValue ) ;
            }
            
            final AttributeDescriptor setter = setters.get( name ) ;
            if (setter == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Could not find setter" ) ;
                }
                throw Exceptions.self.couldNotFindWritableAttribute(name) ;
            }

            setter.set( fa, value, mom.runtimeDebug() ) ;

            // XXX Can we check if a notification is needed before constructing this?
            // Note that this code assumes that emitter is also the MBean,
            // because the MBean extends NotificationBroadcasterSupport!
            AttributeChangeNotification notification =
                new AttributeChangeNotification( emitter,
                    sequenceNumber.incrementAndGet(),
                    System.currentTimeMillis(),
                    "Changed attribute " + name,
                    name,
                    setter.tc().getManagedType().toString(),
                    oldValue,
                    value ) ;

            if (mom.runtimeDebug()) {
                dputil.info( "sending notification ", notification ) ;
            }
            
            emitter.sendNotification( notification ) ;    
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit() ;
            }
        }
    }
        
    public AttributeList getAttributes( FacetAccessor fa, String[] attributes) {
        if (mom.runtimeDebug()) {
            dputil.enter( "getAttributes", "attributes=",
                Arrays.asList( attributes ) ) ;
        }
        
        try {
            AttributeList result = new AttributeList() ;
            for (String str : attributes) {
                Object value = null ;
                
                try {
                    value = getAttribute(fa, str);
                } catch (JMException ex) {
                    Exceptions.self.attributeGettingError(ex, str ) ;
                    if (mom.runtimeDebug()) {
                        dputil.exception( "getAttributes: ", ex ) ;
                    }
                }

                // If value == null, we had a problem in trying to fetch it,
                // so just ignore that attribute.  Returning null simply leads to
                // a blank entry in jconsole.  Do not let an error in fetching
                // one attribute prevent fetching the others.
                
                Attribute attr = new Attribute( str, value ) ;
                result.add( attr ) ;
            }

            return result ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit() ;
            }
        }
    }
        
    public AttributeList setAttributes( 
        final NotificationBroadcasterSupport emitter,
        final FacetAccessor fa, final AttributeList attributes) {
	
        if (mom.runtimeDebug()) {
            dputil.enter( "setAttributes", "emitter=", emitter,
                "fa=", fa, "attributes=", attributes ) ;
        }
        
        AttributeList result = new AttributeList() ;

        try {
            for (Object elem : attributes) {
                Attribute attr = (Attribute)elem ;

                try {
                    setAttribute(emitter, fa, attr);
                    result.add( attr ) ;
                } catch (JMException ex) {
                    Exceptions.self.attributeSettingError(ex, attr.getName()) ;
                    if (mom.runtimeDebug()) {
                        dputil.exception( "Error in setting attribute"
                            + attr.getName(), ex ) ;
                    }
                }
            }
            
            return result ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( result ) ;
            }
        }
    }
    
    public Object invoke( FacetAccessor fa, String actionName, Object params[], 
        String sig[]) throws MBeanException, ReflectionException  {
        final List<String> signature = Arrays.asList( sig ) ;
        final List<Object> parameters = Arrays.asList( params ) ;
        Object result = null ;
        
        if (mom.runtimeDebug()) {
            dputil.enter( "invoke", "fa=", fa, "actionName", actionName,
                "params=", parameters, "signature=", signature ) ;
        }
        
        try {
            final Map<List<String>,Operation> opMap = operations.get( 
                actionName ) ;
            if (opMap == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Operation not found" ) ;
                }
                
                throw Exceptions.self.couldNotFindOperation(actionName) ;
            }

            final Operation op = opMap.get( signature ) ;
            if (op == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Cound not find signature" ) ;
                }
                
                throw Exceptions.self.couldNotFindOperationAndSignature(
                    actionName, signature ) ;
            }

            result = op.evaluate( fa, parameters ) ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }
    
    public String getNameValue( final FacetAccessor fa ) throws
        MBeanException, ReflectionException {
        
        if (mom.runtimeDebug()) {
            dputil.enter( "getNameValue", "fa=", fa ) ;
        }
        
        String value = null ;
        try { 
            if (nameAttributeDescriptor == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "nameAttributeDescriptor is null" ) ;
                }
            } else {
                value = nameAttributeDescriptor.get(fa, 
                    mom.runtimeDebug()).toString();
            }
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( value ) ;
            }
        }
        
        return value ;
    }
    
    public ModelMBeanInfoSupport getMBeanInfo() {
        return mbInfo ;
    }
    
    public ManagedObjectManagerInternal mom() {
        return mom ;
    }
}

