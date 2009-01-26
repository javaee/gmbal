/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.io.InvalidObjectException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import org.glassfish.gmbal.GmbalException;
import org.glassfish.gmbal.impl.AttributeDescriptor.AttributeType;
import org.glassfish.gmbal.logex.Chain;
import org.glassfish.gmbal.logex.ExceptionWrapper;
import org.glassfish.gmbal.logex.Log;
import org.glassfish.gmbal.logex.Message;
import org.glassfish.gmbal.logex.WrapperGenerator;

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
@ExceptionWrapper( idPrefix="GMBAL" )
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
    IllegalArgumentException excForMakeFromAnnotated( Method m ) ;

// DescriptorIntrospector
    static final int DESCRIPTOR_INTROSPECTOR_START =
        ATTRIBUTE_DESCRIPTOR_START + EXCEPTIONS_PER_CLASS ;

    @Message( "@DescriptorFields must contain '=' : {0}" )
    @Log( id=DESCRIPTOR_INTROSPECTOR_START + 0 )
    IllegalArgumentException excForAddDescriptorFieldsToMap( String field ) ;

    @Log( id=DESCRIPTOR_INTROSPECTOR_START + 1 )
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
        Method first, Method second, String className ) ;

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

// MBeanTree
    static final int MBEAN_TREE_START =
        MBEAN_SKELETON_START + EXCEPTIONS_PER_CLASS ;

    @Message( "Root has already been set: cannot set it again" )
    @Log( id=MBEAN_TREE_START + 0 )
    IllegalStateException rootAlreadySet() ;

    @Message( "Could not construct ObjectName for root" )
    @Log( id=MBEAN_TREE_START + 1 )
    IllegalArgumentException noRootObjectName( @Chain Exception ex ) ;

    @Message( "Could not register root" )
    @Log( id=MBEAN_TREE_START + 2 )
    IllegalArgumentException rootRegisterFail( @Chain Exception ex ) ;

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
    String parentNotFound( Object parent ) ;

    @Message( "Object {0} is alreadt registered as {1}")
    @Log( id=MBEAN_TREE_START + 8 )
    String objectAlreadyRegistered( Object obj, MBeanImpl oldMbi ) ;

    @Message( "Should not happen" )
    @Log( id=MBEAN_TREE_START + 9 )
    IllegalStateException shouldNotHappen( @Chain Exception ex ) ;

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
    IllegalArgumentException badInheritedAttributeAnnotation( Class cls ) ;

    @Message( "No description available!" )
    String noDescriptionAvailable() ;

// TypeConverterImpl
    static final int TYPE_CONVERTER_IMPL_START =
        MANAGED_OBJECT_MANAGER_IMPL_START + EXCEPTIONS_PER_CLASS ;

    @Message( "Unsupported OpenType {0}")
    @Log( id=TYPE_CONVERTER_IMPL_START + 0 )
    IllegalArgumentException unsupportedOpenType( OpenType ot ) ;

    @Message( "{0} cannot be converted into a Java class")
    @Log( id=TYPE_CONVERTER_IMPL_START + 1 )
    IllegalArgumentException cannotConvertToJavaType( Type type ) ;

    @Message( "Management entity {0} is not an ObjectName")
    @Log( id=TYPE_CONVERTER_IMPL_START + 2 )
    IllegalArgumentException entityNotObjectName( Object entity ) ;

    @Message( "Arrays of arrays not supported")
    @Log( id=TYPE_CONVERTER_IMPL_START + 3 )
    IllegalArgumentException noArrayOfArray( ) ;

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

    @Message( "Recursive types are not supported")
    @Log( id=TYPE_CONVERTER_IMPL_START + 10 )
    UnsupportedOperationException recursiveTypesNotSupported( ) ;

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
    UnsupportedOperationException openToJavaNotSupported( OpenType otype, Type javaType ) ;
}