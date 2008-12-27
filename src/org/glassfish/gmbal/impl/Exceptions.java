/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import javax.management.MBeanException;
import org.glassfish.gmbal.GmbalException;
import org.glassfish.gmbal.generic.UnaryFunction;
import org.glassfish.gmbal.impl.AttributeDescriptor.AttributeType;
import org.glassfish.gmbal.logex.Chain;
import org.glassfish.gmbal.logex.ExceptionWrapper;
import org.glassfish.gmbal.logex.Log;
import org.glassfish.gmbal.logex.LogLevel;
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

// MBeanSkeleton
    static final int MBEAN_SKELETON_START =
        MBEAN_IMPL_START + EXCEPTIONS_PER_CLASS ;

// MBeanTree
    static final int MBEAN_TREE_START =
        MBEAN_SKELETON_START + EXCEPTIONS_PER_CLASS ;

// ManagedObjectManagerImpl
    static final int MANAGED_OBJECT_MANAGER_IMPL_START =
        MBEAN_TREE_START + EXCEPTIONS_PER_CLASS ;

// TypeConverterImpl
    static final int TYPE_CONVERTER_IMPL_START =
        MANAGED_OBJECT_MANAGER_IMPL_START + EXCEPTIONS_PER_CLASS ;
}
