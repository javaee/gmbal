/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InvalidObjectException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import org.glassfish.gmbal.GmbalException;
import org.glassfish.gmbal.impl.AttributeDescriptor.AttributeType;
import org.glassfish.gmbal.logex.Chain;
import org.glassfish.gmbal.logex.ExceptionWrapper;
import org.glassfish.gmbal.logex.Log;
import org.glassfish.gmbal.logex.LogLevel;
import org.glassfish.gmbal.logex.Message;
import org.glassfish.gmbal.logex.StackTrace;
import org.glassfish.gmbal.logex.WrapperGenerator;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedFieldDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;

/** Exception wrapper class.  The logex WrapperGenerator uses this interface
 * to generate an implementation which returns the appropriate exception, and
 * generates a log report when the method is called.  This is used for all
 * implementation classes in this package.
 *
 * The exception IDs are allocated in blocks of EXCEPTIONS_PER_CLASS, which is
 * a lot more than is needed, but we have 32 bits for IDs, and multiples of
 * a suitably chosen EXCEPTIONS_PER_CLASS (like 100 here) are easy to read in
 * error messages.
 *
 * @author ken
 */
@ExceptionWrapper( idPrefix="GMBAL",
    resourceBundle = "org.glassfish.gmbal.logex.LogStrings" )
public interface Exceptions {
    static final Exceptions self = WrapperGenerator.makeWrapper(
        Exceptions.class ) ;

    // Allow 100 exceptions per class
    static final int EXCEPTIONS_PER_CLASS = 100 ;

// AMXImpl
    static final int AMX_IMPL_START = 1 ;


    @Message( "Exception in getMeta" ) 
    @Log( id = AMX_IMPL_START + 0 )
    GmbalException excForGetMeta( @Chain MBeanException ex ) ;

// AttributeDescriptor
    static final int ATTRIBUTE_DESCRIPTOR_START = AMX_IMPL_START + 
        EXCEPTIONS_PER_CLASS ;

    @Message( "Required type is {0}" )
    @Log( id=ATTRIBUTE_DESCRIPTOR_START + 0 )
    GmbalException excForCheckType( AttributeType at ) ;

    @Message( "methodName and id must not both be null" )
    @Log( id=ATTRIBUTE_DESCRIPTOR_START + 1 )
    IllegalArgumentException excForMakeFromInherited( ) ;

    @Message( "{0} is not a valid attribute method" )
    @Log( id=ATTRIBUTE_DESCRIPTOR_START + 2 )
    IllegalArgumentException excForMakeFromAnnotated( EvaluatedDeclaration m ) ;

    @Message( "Unknown EvaluatedDeclaration type {0}")
    @Log( id=ATTRIBUTE_DESCRIPTOR_START + 4 )
    IllegalArgumentException unknownDeclarationType( EvaluatedDeclaration decl ) ;

    @Message( "Attribute id {0} in method {1} in class {2} is illegal becase "
        + "it is a reserved Attribute id for AMX" )
    @Log( id=ATTRIBUTE_DESCRIPTOR_START + 5 )
    IllegalArgumentException duplicateAMXFieldName(String actualId,
        String methodName, String className );

// DescriptorIntrospector
    static final int DESCRIPTOR_INTROSPECTOR_START =
        ATTRIBUTE_DESCRIPTOR_START + EXCEPTIONS_PER_CLASS ;

    @Message( "@DescriptorFields must contain '=' : {0}" )
    @Log( id=DESCRIPTOR_INTROSPECTOR_START + 0 )
    IllegalArgumentException excForAddDescriptorFieldsToMap( String field ) ;

    @Log( id=DESCRIPTOR_INTROSPECTOR_START + 1 )
    @Message( "Exception in addAnnotationFieldsToMap")
    UndeclaredThrowableException excForAddAnnotationFieldsToMap(
        @Chain Exception ex ) ;

    @Message( "Inconcistent values for descriptor field {0} from annotations: {1} :: {2}" )
    @Log( id=DESCRIPTOR_INTROSPECTOR_START + 2 )
    IllegalArgumentException excForAddToMap( String name, Object value, Object oldValue ) ;

    @Message( "Illegal type for annotation element using @DescriptorKey: {0}" )
    @Log( id=DESCRIPTOR_INTROSPECTOR_START + 3 )
    IllegalArgumentException excForAnnotationToField( String name ) ;

// ImmutableDescriptor
    static final int IMMUTABLE_DESCRIPTOR_START =
        DESCRIPTOR_INTROSPECTOR_START + EXCEPTIONS_PER_CLASS ;

    @Message( "Null Map" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 0 )
    IllegalArgumentException nullMap() ;

    @Message( "Empty or null field name" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 1 )
    IllegalArgumentException badFieldName() ;

    @Message( "Duplicate field name: {0}" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 2 )
    IllegalArgumentException duplicateFieldName( String name ) ;

    @Message( "Bad names or values" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 3 )
    InvalidObjectException excForReadResolveImmutableDescriptor() ;

    @Message( "Null array parameter")
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 4 )
    IllegalArgumentException nullArrayParameter() ;

    @Message( "Different size arrays")
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 5 )
    IllegalArgumentException differentSizeArrays() ;

    @Message( "Null fields parameter")
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 6 )
    IllegalArgumentException nullFieldsParameter() ;

    @Message( "Missing = character: {0}")
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 7 )
    IllegalArgumentException badFieldFormat( String field ) ;

    @Message( "Inconsistent values for descriptor field {0}: {1} :: {2}" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 8 )
    IllegalArgumentException excForUnion( String name, Object oldValue, Object value ) ;

    @Message( "Null argument" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 9 )
    IllegalArgumentException nullArgument() ;

    @Message( "Descriptor is read-only" )
    @Log( id=IMMUTABLE_DESCRIPTOR_START + 10 )
    UnsupportedOperationException unsupportedOperation() ;

// MBeanImpl
    static final int MBEAN_IMPL_START =
        IMMUTABLE_DESCRIPTOR_START + EXCEPTIONS_PER_CLASS ;

    @Message( "Cannot set parent to {0}: this node already has a parent" )
    @Log( id=MBEAN_IMPL_START + 0 )
    IllegalArgumentException nodeAlreadyHasParent( MBeanImpl entity ) ;

    @Message( "Parent object {0} only allows subtypes {1}: "
        + " cannot add child {2} of type {3}" )
    @Log( id=MBEAN_IMPL_START + 1 )
    public IllegalArgumentException invalidSubtypeOfParent(ObjectName oname,
	Set<String> subTypes, ObjectName objectName, String type);

    @Message( "Parent object {0} cannot contain more than one object" +
        " of type {1}: cannot add child {2}")
    @Log( id=MBEAN_IMPL_START + 2 )
    public IllegalArgumentException childMustBeSingleton(ObjectName pname, String ctype,
	ObjectName cname);

    @Message( "tried to register MBean {0} that is already registered" )
    @Log( id = MBEAN_IMPL_START + 3 )
    void registerMBeanRegistered(ObjectName oname);

    @Message( "tried to unregister MBean {0} that is not registered" )
    @Log( id = MBEAN_IMPL_START + 4 )
    void unregisterMBeanNotRegistered(ObjectName oname);

    @StackTrace
    @Message( "registering MBean {0}")
    @Log( id = MBEAN_IMPL_START + 5, level=LogLevel.INFO )
    public void registeringMBean(ObjectName oname);

    @StackTrace
    @Message( "unregistering MBean {0}")
    @Log( id = MBEAN_IMPL_START + 6, level=LogLevel.INFO )
    public void unregisteringMBean(ObjectName oname);

    @Message( "Got an unexpected exception from method {0}" ) 
    @Log( id = MBEAN_IMPL_START + 7 ) 
    public void unexpectedException( String method, @Chain Throwable exc ) ;

// MBeanSkeleton
    static final int MBEAN_SKELETON_START =
        MBEAN_IMPL_START + EXCEPTIONS_PER_CLASS ;

    @Message( "At least one of getter and setter must not be null")
    @Log( id=MBEAN_SKELETON_START + 0 )
    IllegalArgumentException notBothNull() ;

    @Message( "Getter and setter type must match")
    @Log( id=MBEAN_SKELETON_START + 1 )
    IllegalArgumentException typesMustMatch() ;

    @Message( "Methods {0} and {1} are both annotated "
        + "with @ObjectNameKey in class {2}")
    @Log( id=MBEAN_SKELETON_START + 2 )
    IllegalArgumentException duplicateObjectNameKeyAttributes(
        EvaluatedMethodDeclaration first, EvaluatedMethodDeclaration second,
        String className ) ;

    @Message( "ParameterNams annotation must have the same number "
        + "of arguments as the length of the method parameter list" )
    @Log( id=MBEAN_SKELETON_START + 3 )
    IllegalArgumentException parameterNamesLengthBad() ;

    @Message( "Could not find attribute {0}" )
    @Log( id=MBEAN_SKELETON_START + 4 )
    AttributeNotFoundException couldNotFindAttribute( String name ) ;

    @Message( "Could not find writable attribute {0}" )
    @Log( id=MBEAN_SKELETON_START + 5 )
    AttributeNotFoundException couldNotFindWritableAttribute( String name ) ;

    @Message( "Could not find operation named {0}" )
    @Log( id=MBEAN_SKELETON_START + 6 )
    IllegalArgumentException couldNotFindOperation( String name ) ;

    @Message( "Could not find operation named {0} with signature {1}" )
    @Log( id=MBEAN_SKELETON_START + 7 )
    IllegalArgumentException couldNotFindOperationAndSignature( String name,
        List<String> signature) ;

    @Message( "Name of this ManagedObject")
    String nameOfManagedObject() ;

    @Message( "Error in setting attribute {0}" )
    @Log( id=MBEAN_SKELETON_START + 8 )
    void attributeSettingError( @Chain Exception ex, String name ) ;

    @Message( "Error in getting attribute {0}" )
    @Log( id=MBEAN_SKELETON_START + 9 )
    void attributeGettingError( @Chain Exception ex, String name ) ;

    @Message( "OpenDataException trying to create "
        + "OpenMBEanParameterInfoSupport for parameter {0} on method {1}" )
    @Log( id = MBEAN_SKELETON_START + 10 )
    IllegalStateException excInOpenParameterInfo(
        @Chain IllegalArgumentException exc,
        String paramName, EvaluatedMethodDeclaration meth ) ;

    @Message( "Exception on invoking annotation method {0}")
    @Log( id = MBEAN_SKELETON_START + 11, level=LogLevel.SEVERE )
    public RuntimeException annotationMethodException(Method m,
        @Chain Exception exc);

// MBeanTree
    static final int MBEAN_TREE_START =
        MBEAN_SKELETON_START + EXCEPTIONS_PER_CLASS ;

    @Message( "Root has already been set: cannot set it again" )
    @Log( id=MBEAN_TREE_START + 0 )
    IllegalStateException rootAlreadySet() ;

    @Message( "Could not construct ObjectName for root" )
    @Log( id=MBEAN_TREE_START + 1 )
    IllegalArgumentException noRootObjectName( @Chain Exception ex ) ;

    @Message( "Could not register root with ObjectName {0}" )
    @Log( id=MBEAN_TREE_START + 2 )
    IllegalArgumentException rootRegisterFail(
        @Chain Exception ex, ObjectName rootName);

    @Message( "Root has not been set" )
    @Log( id=MBEAN_TREE_START + 3 )
    IllegalStateException rootNotSet() ;

    @Message( "rootParentName {0} is invalid: missing type or name" )
    @Log( id=MBEAN_TREE_START + 4 )
    GmbalException invalidRootParentName( ObjectName oname ) ;

    @Message( "Entity {0} is not part of this EntityTree" )
    @Log( id=MBEAN_TREE_START + 5 )
    IllegalArgumentException notPartOfThisTree( MBeanImpl mbi ) ;

    @Message( "Parent cannot be null" )
    @Log( id=MBEAN_TREE_START + 6 )
    IllegalArgumentException parentCannotBeNull() ;

    @Message( "Parent object {0} not found" )
    @Log( id=MBEAN_TREE_START + 7 )
    IllegalArgumentException parentNotFound( Object parent ) ;

    @Message( "Object {0} is already registered as {1}")
    @Log( id=MBEAN_TREE_START + 8 )
    IllegalArgumentException objectAlreadyRegistered( Object obj, MBeanImpl oldMbi ) ;

    @Message( "Should not happen" )
    @Log( id=MBEAN_TREE_START + 9, level=LogLevel.FINE )
    void shouldNotHappen( @Chain Exception ex ) ;

    @Message( "Object {0} not found")
    @Log( id=MBEAN_TREE_START + 10 )
    IllegalArgumentException objectNotFound( Object obj ) ;

    @Message( "The ObjectName of the root parent MUST contain pp key")
    @Log( id=MBEAN_TREE_START + 11 )
    IllegalArgumentException ppNullInRootParent() ;

    @Message( "The ObjectName of the root parent MUST contain pp key")
    @Log( id=MBEAN_TREE_START + 12 )
    IllegalArgumentException typeNullInRootParent() ;

    @Message( "A MalformedObjectNameException occured on {0}" )
    @Log( id=MBEAN_TREE_START + 12 )
    IllegalArgumentException malformedObjectName( @Chain Exception exc,
        String str ) ;

// ManagedObjectManagerImpl
    static final int MANAGED_OBJECT_MANAGER_IMPL_START =
        MBEAN_TREE_START + EXCEPTIONS_PER_CLASS ;

    @Message( "obj argument is a String: {0} : was a call to "
        + "registerAtRoot intended here?" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 0 )
    IllegalArgumentException objStringWrongRegisterCall( String str ) ;

    @Message( "Exception in register" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 1 )
    IllegalArgumentException exceptionInRegister( @Chain Exception ex ) ;

    @Message( "Exception in unregister" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 2 )
    IllegalArgumentException exceptionInUnregister( @Chain Exception ex ) ;

    @Message( "Cannot add annotation to element {0}: "
        + "an Annotation of type {1} is already present")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 3 )
    IllegalArgumentException duplicateAnnotation( AnnotatedElement element,
        String name ) ;

    @Message( "Class {0} contains both the InheritedAttribute and "
        + "the InheritedAttributes annotations" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 4 )
    IllegalArgumentException badInheritedAttributeAnnotation(
        EvaluatedClassDeclaration cls ) ;

    @Message( "Field {0} must be final and have an immutable type "
        + "to be used as an attribute" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 4 )
    IllegalArgumentException illegalAttributeField(
        EvaluatedFieldDeclaration cls ) ;

    @Message( "No description available!" )
    String noDescriptionAvailable() ;

    @Message( "Method {0} cannot be called before a successful createRoot call")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 5 )
    IllegalStateException createRootNotCalled( String methodName ) ;

    @Message( "Method {0} cannot be called after a successful createRoot call")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 6 )
    IllegalStateException createRootCalled( String methodName ) ;

    @Message( "Could not construct MBean {0}")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 7 )
    public IllegalArgumentException errorInConstructingMBean(String objName,
        @Chain JMException exc);

    @Message( "Attempt made to register non-singleton object of type {1}"
        + " without a name as a child of {0}" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 8 )
    public IllegalArgumentException nonSingletonRequiresName(
        MBeanImpl parentEntity, String type);

    @Message( "Attempt made to register singleton object of type {1}"
        + " with name {2} as a child of {0}")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 9 )
    public IllegalArgumentException singletonCannotSpecifyName(
        MBeanImpl parentEntity, String type, String name);

    @Message( "No {0} annotation found on {1}" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 10 )
    public IllegalArgumentException noAnnotationFound(String name, String cls );

    @Message( "Cannot add null annotation to {0}" )
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 11 )
    public IllegalArgumentException cannotAddNullAnnotation(AnnotatedElement element);

    @Message( "ManagedObject annotation not found on class {0}")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 12 )
    public IllegalArgumentException managedObjectAnnotationNotFound(String cname);

    @Message( "Cannot call getAnnotations on {0}")
    @Log( id=MANAGED_OBJECT_MANAGER_IMPL_START + 13 )
    public IllegalArgumentException annotationsNotSupported(AnnotatedElement elem);

// TypeConverterImpl
    static final int TYPE_CONVERTER_IMPL_START =
        MANAGED_OBJECT_MANAGER_IMPL_START + EXCEPTIONS_PER_CLASS ;

    @Message( "Unsupported OpenType {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 0 )
    IllegalArgumentException unsupportedOpenType( OpenType ot ) ;

    @Message( "{0} cannot be converted into a Java class")
    @Log( id=TYPE_CONVERTER_IMPL_START + 1 )
    IllegalArgumentException cannotConvertToJavaType( EvaluatedType type ) ;

    @Message( "Management entity {0} is not an ObjectName")
    @Log( id=TYPE_CONVERTER_IMPL_START + 2 )
    IllegalArgumentException entityNotObjectName( Object entity ) ;

    @Message( "Arrays of arrays not supported")
    @Log( id=TYPE_CONVERTER_IMPL_START + 3 )
    IllegalArgumentException noArrayOfArray( @Chain Exception exc ) ;

    @Message( "{0} is not a String" )
    @Log( id=TYPE_CONVERTER_IMPL_START + 4 )
    IllegalArgumentException notAString( Object obj ) ;

    @Message( "There is no <init>(String) constructor "
        + "available to convert a String into a {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 5 )
    UnsupportedOperationException noStringConstructor( Class cls ) ;

    @Message( "Error in converting from String to {0}" )
    @Log( id=TYPE_CONVERTER_IMPL_START + 6 )
    IllegalArgumentException stringConversionError( Class cls, @Chain
        Exception exc ) ;

    @Message( "Exception in makeCompositeType")
    @Log( id=TYPE_CONVERTER_IMPL_START + 7 )
    IllegalArgumentException exceptionInMakeCompositeType( @Chain Exception exc ) ;

    @Message( "Exception in handleManagedData")
    @Log( id=TYPE_CONVERTER_IMPL_START + 8 )
    IllegalArgumentException exceptionInHandleManagedData( @Chain Exception exc ) ;

    @Message( "Remove is not supported")
    @Log( id=TYPE_CONVERTER_IMPL_START + 9 )
    UnsupportedOperationException removeNotSupported( ) ;

    @Message( "Recursive types are not supported: type is {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 10 )
    UnsupportedOperationException recursiveTypesNotSupported( EvaluatedType et ) ;

    @Message( "OpenType exception in ArrayType construction caused by {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 11 )
    IllegalArgumentException openTypeInArrayTypeException( OpenType ot,
        @Chain Exception exc ) ;

    @Message( "Exception in makeMapTabularType")
    @Log( id=TYPE_CONVERTER_IMPL_START + 12 )
    IllegalArgumentException exceptionInMakeMapTabularType(
        @Chain Exception exc ) ;

    @Message( "row type for {0}")
    String rowTypeDescription( String mapType ) ;

    @Message( "Key of map {0}")
    String keyFieldDescription( String mapType ) ;

    @Message( "Value of map {0}")
    String valueFieldDescription( String mapType ) ;

    @Message( "Table:{0}")
    String tableName( String mapType ) ;

    @Message( "Table for map {0}")
    String tableDescription( String mapType ) ;

    @Message( "Exception in makeMapTabularData:toManagedEntity")
    @Log( id=TYPE_CONVERTER_IMPL_START + 13 )
    IllegalArgumentException excInMakeMapTabularDataToManagedEntity(
        @Chain Exception exc ) ;

    @Message( "{0} must have at least 1 type argument")
    @Log( id=TYPE_CONVERTER_IMPL_START + 14 )
    IllegalArgumentException paramTypeNeedsArgument( ParameterizedType type ) ;

    @Message( "Converting from OpenType {0} to Java type {1} is not supported")
    @Log( id=TYPE_CONVERTER_IMPL_START + 15 )
    UnsupportedOperationException openToJavaNotSupported( OpenType otype, 
        EvaluatedType javaType ) ;

    @Message( "iterator() method not found in subclass of Iterable {0}") 
    @Log( id=TYPE_CONVERTER_IMPL_START + 16 ) 
    IllegalStateException iteratorNotFound( EvaluatedClassDeclaration cls ) ;

    @Message( "next() method not found in type {0}") 
    @Log( id=TYPE_CONVERTER_IMPL_START + 17 )
    IllegalStateException nextNotFound( EvaluatedClassDeclaration cls ) ;

    @Message( "Could not set field {1} in CompositeData for type {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 18, level=LogLevel.FINE )
    public void errorInConstructingOpenData(String name, String id,
        @Chain JMException ex);

    @Message( "No <init>(String) constructor available for class {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 19, level=LogLevel.FINE )
    void noStringConstructorAvailable( @Chain Exception exc, String name);

// JMXRegistrationManager start
    static final int JMX_REGISTRATION_MANAGER_START =
        TYPE_CONVERTER_IMPL_START + EXCEPTIONS_PER_CLASS ;

    @Message( "JMX exception on registration of MBean {0}" )
    @Log( id=JMX_REGISTRATION_MANAGER_START + 0 )
    void deferredRegistrationException( @Chain JMException exc,
        MBeanImpl mbean ) ;
}
