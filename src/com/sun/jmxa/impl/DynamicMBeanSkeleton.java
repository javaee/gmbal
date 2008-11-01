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
import java.lang.reflect.Type ;

import javax.management.Attribute ;
import javax.management.AttributeList ;
import javax.management.MBeanException ;
import javax.management.InvalidAttributeValueException ;
import javax.management.AttributeNotFoundException ;
import javax.management.ReflectionException ;
import javax.management.MBeanInfo ;
import javax.management.MBeanOperationInfo ;
import javax.management.MBeanParameterInfo ;

import javax.management.openmbean.OpenMBeanAttributeInfo ;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport ;
import javax.management.openmbean.OpenMBeanOperationInfo ;
import javax.management.openmbean.OpenMBeanOperationInfoSupport ;
import javax.management.openmbean.OpenMBeanParameterInfo ;
import javax.management.openmbean.OpenMBeanParameterInfoSupport ;
import javax.management.openmbean.OpenMBeanInfoSupport ;
import javax.management.NotificationBroadcasterSupport ;
import javax.management.AttributeChangeNotification ;

import com.sun.jmxa.generic.BinaryFunction ;
import com.sun.jmxa.InheritedAttribute ;
import com.sun.jmxa.ObjectNameKey ;
import com.sun.jmxa.ManagedAttribute ;
import com.sun.jmxa.ManagedOperation ;
import com.sun.jmxa.ManagedObject ;
import com.sun.jmxa.ParameterNames ;

import com.sun.jmxa.generic.DprintUtil;
import com.sun.jmxa.generic.DumpIgnore;
import com.sun.jmxa.generic.Pair ;
import com.sun.jmxa.generic.DumpToString ;

import java.lang.reflect.InvocationTargetException;

public class DynamicMBeanSkeleton {
    // Object evaluate( Object, List<Object> ) 
    // (or Result evaluate( Target, ArgList ))
    public interface Operation 
        extends BinaryFunction<Object,List<Object>,Object> {} ;

    private final String type ;
    @DumpToString
    private final AtomicLong sequenceNumber ;
    private final MBeanInfo mbInfo ;
    @DumpToString
    private final ManagedObjectManagerInternal mom ;
    @DumpIgnore
    private final DprintUtil dputil ;
    private final Map<String,AttributeDescriptor> setters ;
    private final Map<String,AttributeDescriptor> getters ;
    private final Map<String,AttributeDescriptor> objectNameKeys ;
    private final Map<String,Map<List<String>,Operation>> operations ;
    private final List<OpenMBeanAttributeInfo> mbeanAttributeInfoList ; 
    private final List<OpenMBeanOperationInfo> mbeanOperationInfoList ; 
 
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
                throw new IllegalArgumentException(
                    "At least one of getter and setter must not be null" ) ;
            }

            if ((setter != null) && (getter != null) 
                && !setter.type().equals( getter.type() )) {
                    throw new IllegalArgumentException( 
                        "Getter and setter types do not match" ) ;
            }

            AttributeDescriptor nonNullDescriptor = 
                (getter != null) ? getter : setter ;

            String name = nonNullDescriptor.id() ;
            String description = nonNullDescriptor.description() ;
            if (mom.registrationFineDebug()) {
                dputil.info( "name=", name, "description=", description ) ;
            }
            
            TypeConverter tc = mom.getTypeConverter( nonNullDescriptor.type() ) ;

            OpenMBeanAttributeInfo ainfo = new OpenMBeanAttributeInfoSupport( name, 
                description, tc.getManagedType(), 
                getter != null, setter != null, false ) ;
            
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

    private void analyzeInheritedAttributes( final Class<?> annotatedClass, 
        final ClassAnalyzer ca ) {
	// Check for @InheritedAttribute(s) annotation.  
        // Find methods for these attributes in superclasses. 
        
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeInheritedAttributes", "annotatedClass=", 
                annotatedClass, "ca=", ca ) ;
        }
        
        try {
            final List<InheritedAttribute> iaa = mom.getInheritedAttributes(  ca ) ;
            for (InheritedAttribute attr : iaa) {
                AttributeDescriptor setterInfo =
                    AttributeDescriptor.findAttribute(mom, ca, attr.id(),
                    attr.description(),
                    AttributeDescriptor.AttributeType.SETTER);

                AttributeDescriptor getterInfo =
                    AttributeDescriptor.findAttribute(mom, ca, attr.id(),
                    attr.description(),
                    AttributeDescriptor.AttributeType.GETTER);

                processAttribute(getterInfo, setterInfo);
            } 
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private <K,V> void putIfNotPresent( Map<K,V> map,
        K key, V value ) {
    
        if (mom.registrationFineDebug()) {
            dputil.enter( "putIfNotPresent", "key=", key,
                "value=", value ) ;
        }
        
        try {
            if (!map.containsKey( key )) {
                if (mom.registrationFineDebug()) {
                    dputil.info( "Adding key, value to map" ) ;
                }
                map.put( key, value ) ;
            } else {
                if (mom.registrationFineDebug()) {
                    dputil.info( "Key,value already in map" ) ;
                }
            }
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private void analyzeAnnotatedAttributes( ClassAnalyzer ca ) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeAnnotatedAttributes", "ca=", ca ) ;
        }
        
        try {
            final List<Method> attributes = ca.findMethods( 
                ca.forAnnotation( mom, ManagedAttribute.class ) ) ;
            
            if (mom.registrationFineDebug()) {
                dputil.info( "attributes=", attributes ) ;
            }

            for (Method m : attributes) {
                AttributeDescriptor minfo = new AttributeDescriptor( mom, m ) ;

                if (minfo.atype() == AttributeDescriptor.AttributeType.GETTER) {
                    putIfNotPresent( getters, minfo.id(), minfo ) ;
                } else {
                    putIfNotPresent( setters, minfo.id(), minfo ) ;
                }
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

    private void analyzeObjectNameKeys( ClassAnalyzer ca) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeObjectNameKeys", "ca=", ca ) ;
        }
        
        try {
            final List<Method> onkMethods = ca.findMethods(
                ca.forAnnotation( mom, ObjectNameKey.class )) ;

            if (mom.registrationFineDebug()) {
                dputil.info( "onkMethods=", onkMethods ) ;
            }
            
            for (Method m : onkMethods ){
                ObjectNameKey onk = m.getAnnotation( ObjectNameKey.class ) ;
                String id = onk.value() ;
                AttributeDescriptor ad = new AttributeDescriptor( mom, m,
                    id, "" ) ;
                putIfNotPresent( objectNameKeys, ad.id(), ad ) ;
            }
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    private Pair<Operation,OpenMBeanOperationInfo> makeOperation( 
        final Method m ) {
	
        if (mom.registrationFineDebug()) {
            dputil.enter( "makeOperation", "m=", m ) ;
        }
        
        try {
            final String desc = mom.getDescription( m ) ;
            final Type rtype = m.getGenericReturnType() ;
            final TypeConverter rtc = rtype == null ? null : mom.getTypeConverter( 
                rtype ) ;
            final Type[] atypes = m.getGenericParameterTypes() ;
            final List<TypeConverter> atcs = new ArrayList<TypeConverter>() ;
            for (Type ltype : atypes) {
                atcs.add( mom.getTypeConverter( ltype ) ) ;
            }

            if (mom.registrationFineDebug()) {
                dputil.info( "desc=", desc ) ;
                dputil.info( "rtype=", rtype ) ;
                dputil.info( "rtc=", rtc ) ;
                dputil.info( "atcs=", atcs ) ;
                dputil.info( "atypes=", atypes ) ;
            }
            
            final Operation oper = new Operation() {
                public Object evaluate( Object target, List<Object> args ) {
                    if (mom.runtimeDebug()) {
                        dputil.enter( "Operation:evaluate", "taget=", target, 
                            "args=", args ) ;
                        
                    }
                    try {
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
                        
                        Object result = m.invoke( target, margs ) ;

                        if (mom.runtimeDebug()) {
                            dputil.info( "After invoke: result=", result ) ;
                        }
                        
                        if (rtc == null) {
                            return null ;
                        } else {
                            return rtc.toManagedEntity( result ) ;
                        }
                    } catch (IllegalAccessException exc) {
                        throw new RuntimeException( exc ) ;
                    } catch (InvocationTargetException exc) {
                        throw new RuntimeException( exc ) ;
                    }
                }
            } ;

            final ParameterNames pna = m.getAnnotation( ParameterNames.class ) ;
            if (mom.registrationFineDebug()) {
                dputil.info( "pna=", pna.value() ) ;
            }
            
            if (pna != null && pna.value().length != atcs.size()) {
                // XXX I18N
                throw new IllegalArgumentException( 
                    "ParametersNames annotation must have the same number" 
                    + " of arguments as the length of the method parameter list" );
            }

            final OpenMBeanParameterInfo[] paramInfo = 
                new OpenMBeanParameterInfo[ atcs.size() ] ;
            int ctr = 0 ;
            for (TypeConverter tc : atcs) {
                paramInfo[ctr] = new OpenMBeanParameterInfoSupport( 
                    (pna == null) ? "arg" + ctr : pna.value()[ctr], 
                    desc, tc.getManagedType() ) ;
                ctr++ ;
            }

            // XXX Note that impact is always set to ACTION_INFO here.  If this is 
            // useful to set in general, we need to add impact to the 
            // ManagedOperation annotation.
            // This is basically what JSR 255 does.
            final OpenMBeanOperationInfo operInfo = 
                new OpenMBeanOperationInfoSupport( m.getName(),
                desc, paramInfo, rtc.getManagedType(), 
                MBeanOperationInfo.ACTION_INFO ) ;

            if (mom.registrationFineDebug()) {
                dputil.info( "operInfo=", operInfo ) ;
            }
            
            return new Pair<Operation,OpenMBeanOperationInfo>( oper, operInfo ) ;
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    private void analyzeOperations( ClassAnalyzer ca ) {
        if (mom.registrationFineDebug()) {
            dputil.enter( "analyzeOperations", "ca=", ca ) ;
        }
        
        try {
            // Scan for all methods annotation with @ManagedOperation, 
            // including inherited methods.
            final List<Method> ops = ca.findMethods( ca.forAnnotation( mom,
                ManagedOperation.class ) ) ;
            for (Method m : ops) {             
                final Pair<Operation,OpenMBeanOperationInfo> data = 
                    makeOperation( m ) ;
                final OpenMBeanOperationInfo info = data.second() ;

                final List<String> dataTypes = new ArrayList<String>() ;
                for (MBeanParameterInfo pi : info.getSignature()) {
                    // Replace recursion marker with the constructed implementation
                    dataTypes.add( pi.getType() ) ;
                }

                Map<List<String>,Operation> map = operations.get( m.getName() ) ;
                if (map == null) {
                    map = new HashMap<List<String>,Operation>() ;
                    operations.put( m.getName(), map ) ;
                }

                // Note that the first occurrence of any method will be the most
                // derived, so if there is already an entry, don't overwrite it.
                putIfNotPresent( map, dataTypes, data.first() ) ;

                mbeanOperationInfoList.add( info ) ;
            }
        } finally {
            if (mom.registrationFineDebug()) {
                dputil.exit() ;
            }
        }
    }

    public DynamicMBeanSkeleton( final Class<?> annotatedClass, 
        final ClassAnalyzer ca, final ManagedObjectManagerInternal mom ) {

        dputil = new DprintUtil( this ) ;
	this.mom = mom ;

        final ManagedObject mo = annotatedClass.getAnnotation( 
            ManagedObject.class ) ;
        
        if (mo.type().equals("")) {
	    type = annotatedClass.getName() ;
        } else {
            type = mo.type() ;
        }

        sequenceNumber = new AtomicLong() ;
	setters = new HashMap<String,AttributeDescriptor>() ;
	getters = new HashMap<String,AttributeDescriptor>() ; 
        objectNameKeys = new HashMap<String,AttributeDescriptor>() ;
	operations = new HashMap<String,Map<List<String>,Operation>>() ;
	mbeanAttributeInfoList = new ArrayList<OpenMBeanAttributeInfo>() ;
	mbeanOperationInfoList = new ArrayList<OpenMBeanOperationInfo>() ;

        analyzeInheritedAttributes( annotatedClass, ca ) ;
        analyzeAnnotatedAttributes( ca ) ;
        analyzeOperations( ca ) ;
        analyzeObjectNameKeys( ca ) ;
        
	OpenMBeanAttributeInfo[] attrInfos = mbeanAttributeInfoList.toArray( 
	    new OpenMBeanAttributeInfo[mbeanAttributeInfoList.size()] ) ;
	OpenMBeanOperationInfo[] operInfos = mbeanOperationInfoList.toArray(
	    new OpenMBeanOperationInfo[mbeanOperationInfoList.size() ] ) ;
        // XXX Do we want to use the class analyzer to handle an inherited 
        // @Description annotation?
	mbInfo = new OpenMBeanInfoSupport( 
	    type, mom.getDescription( annotatedClass ), attrInfos, null, 
            operInfos, null ) ;
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

    public Object getAttribute( Object obj, String name) 
        throws AttributeNotFoundException, MBeanException, ReflectionException {

        if (mom.runtimeDebug()) {
            dputil.enter( "getAttribute", "obj=", obj, "name=", name ) ;
        }
        
        Object result = null ;
        try {
            AttributeDescriptor getter = getters.get( name ) ;
            if (getter == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Error in finding getter ", name ) ;
                }
                throw new AttributeNotFoundException( "Could not find attribute " 
                    + name ) ;
            }
            result = getter.get( obj, mom.runtimeDebug() ) ;
            return result ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( result ) ; 
            }
        }
    }
    
    public void setAttribute( final NotificationBroadcasterSupport emitter, 
        final Object obj, final Attribute attribute) 
        throws AttributeNotFoundException, InvalidAttributeValueException, 
        MBeanException, ReflectionException  {
        
        if (mom.runtimeDebug()) {
            dputil.enter( "setAttribute", "emitter=", emitter,
                "obj=", obj, "attribute=", attribute ) ;
        }

        try {
            final String name = attribute.getName() ;
            final Object value = attribute.getValue() ;
            final AttributeDescriptor getter = getters.get( name ) ;
            final Object oldValue = (getter == null) ?
                null :
                getter.get( obj, mom.runtimeDebug() ) ;

            if (mom.runtimeDebug()) {
                dputil.info( "oldValue=", oldValue ) ;
            }
            
            final AttributeDescriptor setter = setters.get( name ) ;
            if (setter == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Could not find setter" ) ;
                }
                throw new AttributeNotFoundException( 
                    "Could not find writable attribute " + name ) ;

            }

            setter.set( obj, value, mom.runtimeDebug() ) ;

            // Note that this code assumes that emitter is also the MBean,
            // because the MBean extends NotificationBroadcasterSupport!
            AttributeChangeNotification notification =
                new AttributeChangeNotification( emitter,
                    sequenceNumber.incrementAndGet(),
                    System.currentTimeMillis(),
                    "Changed attribute " + type,
                    type,
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
        
    public AttributeList getAttributes( Object obj, String[] attributes) {
        if (mom.runtimeDebug()) {
            dputil.enter( "getAttributes", "attributes=",
                Arrays.asList( attributes ) ) ;
        }
        
        try {
            AttributeList result = new AttributeList() ;
            for (String str : attributes) {
                Object value = null ;
                Exception exception = null ;
                
                try {
                    value = getAttribute(obj, str);
                } catch (Exception exc) {
                    exception = exc ;
                }

                // If value == null, we had a problem in trying to fetch it,
                // so just ignore that attribute.  Returning null simply leads to
                // a blank entry in jconsole.  Do not let an error in fetching
                // one attribute prevent fetching the others.
                
                if (exception != null) {
                    if (mom.runtimeDebug()) {
                        dputil.exception( "getAttribute: ", exception ) ;
                    }
                }
                
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
        final Object obj, final AttributeList attributes) {
	
        if (mom.runtimeDebug()) {
            dputil.enter( "setAttributes", "emitter=", emitter,
                "obj=", obj, "attributes=", attributes ) ;
        }
        
        AttributeList result = new AttributeList() ;

        try {
            for (Object elem : attributes) {
                Attribute attr = (Attribute)elem ;
                Exception exception = null ;
                try {
                    setAttribute(emitter, obj, attr);
                } catch (Exception ex) {
                    exception = ex ;
                }
                
                if (exception == null) {
                    result.add( attr ) ;
                } else {
                    if (mom.runtimeDebug()) {
                        dputil.exception( "Error in setting attribute" 
                            + attr.getName(), exception ) ;
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
    
    public Object invoke( Object obj, String actionName, Object params[], 
        String sig[]) throws MBeanException, ReflectionException  {
        final List<String> signature = Arrays.asList( sig ) ;
        final List<Object> parameters = Arrays.asList( params ) ;
        Object result = null ;
        
        if (mom.runtimeDebug()) {
            dputil.enter( "invoke", "obj=", obj, "actionName", actionName,
                "params=", parameters, "signature=", signature ) ;
        }
        
        try {
            final Map<List<String>,Operation> opMap = operations.get( actionName ) ;
            if (opMap == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Operation not found" ) ;
                }
                
                throw new IllegalArgumentException( 
                    "Could not find operation named " + actionName ) ;
            }

            final Operation op = opMap.get( signature ) ;
            if (op == null) {
                if (mom.runtimeDebug()) {
                    dputil.info( "Cound not find signature" ) ;
                }
                
                throw new IllegalArgumentException( 
                    "Could not find operation named " + actionName 
                    + " with signature " + sig ) ;
            }

            result = op.evaluate( obj, parameters ) ;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }
    
    public List<String> getObjectNameProperties( final Object obj ) throws
        MBeanException, ReflectionException {
        
        if (mom.runtimeDebug()) {
            dputil.enter( "getObjectNameProperties", "obj=", obj ) ;
        }
        
        try {
            final List<String> result = new ArrayList<String>();
            for (Map.Entry<String, AttributeDescriptor> entry : 
                objectNameKeys.entrySet()) {

                final String key = entry.getKey();
                final AttributeDescriptor ad = entry.getValue();
                
                if (mom.runtimeDebug()) {
                    // XXX Should this use a type converter?  If not, check that 
                    // type is reasonable.
                    dputil.info( "key=", key, "ad=", ad ) ;
                }
                
                String value = null ;
                try {
                    value = ad.get(obj, mom.runtimeDebug()).toString();
                } catch (Exception exc) {
                    if (mom.runtimeDebug()) {
                        dputil.exception( "Error in getting value", exc ) ;
                    }
                }
                
                if (value != null) {
                    result.add(key + "=" + value);
                }
            }
                
            return result;
        } finally {
            if (mom.runtimeDebug()) {
                dputil.exit() ;
            }
        }
    }
    
    public MBeanInfo getMBeanInfo() {
	return mbInfo ;
    }
}

