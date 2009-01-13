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

package org.glassfish.gmbal.impl ;

import org.glassfish.gmbal.generic.ClassAnalyzer;
import org.glassfish.gmbal.generic.DumpToString ;

import java.lang.reflect.Array ;
import java.lang.reflect.Constructor ;
import java.lang.reflect.Type ;
import java.lang.reflect.ParameterizedType ;
import java.lang.reflect.TypeVariable ;
import java.lang.reflect.WildcardType ;
import java.lang.reflect.GenericArrayType ;

import java.util.Date ;
import java.util.Collection ;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.Iterator ;
import java.util.Enumeration ;
import java.util.Dictionary ;

import java.math.BigDecimal ;
import java.math.BigInteger ;

import javax.management.ObjectName ;

import javax.management.openmbean.ArrayType ;
import javax.management.openmbean.OpenType ;
import javax.management.openmbean.OpenDataException ;
import javax.management.openmbean.SimpleType ;
import javax.management.openmbean.TabularType ;
import javax.management.openmbean.CompositeType ;
import javax.management.openmbean.CompositeData ;
import javax.management.openmbean.CompositeDataSupport ;
import javax.management.openmbean.TabularData ;
import javax.management.openmbean.TabularDataSupport ;

import org.glassfish.gmbal.ManagedObject ;
import org.glassfish.gmbal.ManagedData ;
import org.glassfish.gmbal.generic.DprintUtil;
import org.glassfish.gmbal.generic.FacetAccessor;
import org.glassfish.gmbal.generic.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import javax.management.JMException;

/** A ManagedEntity is one of the pre-defined Open MBean types: SimpleType, 
 * ObjectName, TabularData, or CompositeData.
 */
public abstract class TypeConverterImpl implements TypeConverter {
    private static final Map<Type,OpenType> simpleTypeMap = 
        new HashMap<Type,OpenType>() ;
    private static final Map<OpenType,Type> simpleOpenTypeMap = 
        new HashMap<OpenType,Type>() ;
    private static final DprintUtil dputil = new 
        DprintUtil( TypeConverterImpl.class ) ;

    private static void initMaps( final Type type, final OpenType otype ) {
	simpleTypeMap.put( type, otype ) ;
	simpleOpenTypeMap.put( otype, type ) ;
    }

    static {
	initMaps( boolean.class, SimpleType.BOOLEAN ) ;
	initMaps( Boolean.class, SimpleType.BOOLEAN ) ;

	initMaps( char.class, SimpleType.CHARACTER ) ;
	initMaps( Character.class, SimpleType.CHARACTER ) ;

	initMaps( byte.class, SimpleType.BYTE ) ;
	initMaps( Byte.class, SimpleType.BYTE ) ;

	initMaps( short.class, SimpleType.SHORT ) ;
	initMaps( Short.class, SimpleType.SHORT ) ;

	initMaps( int.class, SimpleType.INTEGER ) ;
	initMaps( Integer.class, SimpleType.INTEGER ) ;

	initMaps( long.class, SimpleType.LONG ) ;
	initMaps( Long.class, SimpleType.LONG ) ;

	initMaps( float.class, SimpleType.FLOAT ) ;
	initMaps( Float.class, SimpleType.FLOAT ) ;

	initMaps( double.class, SimpleType.DOUBLE ) ;
	initMaps( Double.class, SimpleType.DOUBLE ) ;

	initMaps( String.class, SimpleType.STRING ) ;
	initMaps( void.class, SimpleType.VOID ) ;

	initMaps( Date.class, SimpleType.DATE ) ;
	initMaps( ObjectName.class, SimpleType.OBJECTNAME ) ;

	initMaps( BigDecimal.class, SimpleType.BIGDECIMAL ) ;
	initMaps( BigInteger.class, SimpleType.BIGINTEGER ) ;
    }

    public static Class getJavaClass( final OpenType ot ) {
	if (ot instanceof SimpleType) {
	    final SimpleType st = (SimpleType)ot ;
	    return (Class)simpleOpenTypeMap.get( st ) ;
	} else if (ot instanceof ArrayType) {
	    // This code is rather odd.  We need to get the opentype of the 
            // array components, convert that to a java type, and then 
            // construct a Java type (Class) that has that java type as its 
            // component (java) type.  I think the only way to do this 
            // is to call Array.newInstance, and then take the class from the 
            // resulting array instance.
	    final ArrayType at = (ArrayType)ot ;
	    final OpenType cot = at.getElementOpenType() ;
	    final Class cjt = getJavaClass( cot ) ;
	    final Object temp = Array.newInstance( cjt, 0 ) ;
	    return temp.getClass() ;
	} else if (ot instanceof TabularType) {
	    return TabularData.class ;
	} else if (ot instanceof CompositeType) {
	    return CompositeData.class ;
	} else {
            throw Exceptions.self.unsupportedOpenType( ot ) ;
	}
    }

    public static Class getJavaClass( final Type type ) {
	if (type instanceof Class) {
	    return (Class)type ;
	} else if (type instanceof GenericArrayType) {
	    // Same trick as above.
	    final GenericArrayType gat = (GenericArrayType)type ;
	    final Type ctype = gat.getGenericComponentType() ;
	    final Class cclass = getJavaClass( ctype ) ;
	    final Object temp = Array.newInstance( cclass, 0 ) ;
	    return temp.getClass() ;
	} else if (type instanceof ParameterizedType) {
	    final ParameterizedType pt = (ParameterizedType)type ;
	    final Type rpt = pt.getRawType() ;
	    return (Class)rpt ;
	} else {
            throw Exceptions.self.cannotConvertToJavaType(type) ;
	}
    }

    /* Type mapping rules for OT : Type -> OpenType:
     *  <pre>
     *  Java Type			Open Type
     *  -----------------------------------------
     *  boolean, Boolean		BOOLEAN
     *  char, Character			CHARACTER
     *  byte, Byte			BYTE
     *  short, Short			SHORT
     *  int, Integer			INTEGER
     *  long, LONG			LONG
     *  float, Float			FLOAT	
     *  double, Double			DOUBLE
     *  String				STRING
     *  void				VOID
     *  java.util.Date			DATE
     *  javax.management.ObjectName	OBJECTNAME
     *  java.math.BigDecimal		BIGDECIMAL
     *  java.math.BigInteger		BIGINTEGER
     *  (all of the above types have the same JT and OT representation.
     *  
     *  Enumeration type		String (only valid values are keywords)
     *
     *  @ManagedObject			ObjectName
     *
     *  @ManagedData			CompositeType( 
     *					    @ManagedData.name, 
     *					    @ManagedData.description, 
     *					    ALL @ManagedAttribute ID, (extracted from the method 
     *					                               name)
     *					    ALL @ManagedAttribute.description, 
     *					    ALL @ManagedAttribute OT(Type) ) (extracted from the 
     *						                              method attribute 
     *						                              TYPE)
     *					We also need to include @IncludeSubclass and 
     *					@InheritedAttribute(s) attributes
     *					Also note that @InheritTable adds an attribute to the 
     *					class.  
     *					The inherited table is mapped to an array.  
     *					The InheritTable annotation specifies a class X, which must
     *					be part of the Type C<X>, where C is a subclass of 
     *					Collection, Iterable, Iterator, or Enumerable.  
     *					JT -> OT: invoke attribute methods, collect results, 
     *					    construct CompositeDataSupport
     *					OT -> JT: NOT SUPPORTED (for now)
     *					(can only be supported in Collection case)
     *
     *	C<X>, C a subtype of Collection, Iterator, Iterable, or Enumeration
     *	        			Mapped to array data containing OT(X) 
     *					JT -> OT: Construct ArrayType of type OT(X), 
     *					    fill it with all JT elements as mapped to their OT type
     *					OT -> JT: NOT SUPPORTED 
     *					    (unless CompositeData -> Java type is supported)
     *					What about SortedSet?
     *
     *	M<K,V>, M a subtype of Map or Dictionary
     *	                                Mapped to tabular data containing id="key" OT(K) as 
     *	                                the key and id="value" OT(V) 
     *	                                XXX What about SortedMap?
     *	    
     *	X[]				OT(X)[]
     *					    As for CompositeData, we will mostly treat this as 
     *					    readonly.  Mappings same as C<X> case. 
     *
     *	TypeVariable			
     *	WildCardType			Both need to be supported.  The basic idea is that 
     *	                                the upper bounds give us information on what interfaces 
     *	                                must be implemented by by an instance of the type 
     *	                                variable.  This in turn tells us what
     *					subclasses identified by @IncludeSubclass should be 
     *					used when mapping the actual data to a CompositeData 
     *					instance.
     *					Not supported: lower bounds (what would that mean?  
     *					               Do I need it?)
     *						       multiple upper bounds (I don't think I 
     *						       need this?)
     *					Multiple upper bounds PROBABLY means the intersection of 
     *					the included subclasses for all of the bounds
     *
     *	Other				String
     *					    JT -> OT: use toString
     *					    OT -> JT: requires a <init>(String) constructor
     *
     * XXX How much do we support the OpenType -> JavaType mapping?  This is mainly a question for
     * CompositeData.  Note that CompositeData is immutable, so all @ManagedAttributes in 
     * CompositeData must be getters.  I think this would mainly apply to a setter in an MBean 
     * whose type maps to CompositeData.
     * For now, we will ignore this, because I think all CompositeData types in the ORB will be 
     * read only.  If this is NOT the case, we can adopt a solution similar to the MXBean 
     * @ConstructorProperties.
     */
    public static TypeConverter makeTypeConverter( final Type type, 
        final ManagedObjectManagerInternal mom ) {
        
        if (mom.registrationDebug()) {
            dputil.enter( "makeTypeConverter", "type=", type,
                "mom=", mom ) ;
        }
        
        TypeConverter result = null ;
        try {
            final OpenType stype = simpleTypeMap.get( type ) ;
            if (stype != null) {
                result = handleSimpleType( (Class)type, stype ) ;
            } else if (type instanceof Class) {
                result = handleClass( (Class<?>)type, mom ) ;
            } else if (type instanceof ParameterizedType) {
                result = handleParameterizedType( (ParameterizedType)type, mom ) ;
            } else if (type instanceof GenericArrayType) {
                result = handleArrayType( (GenericArrayType)type, mom ) ;
            } else if (type instanceof TypeVariable) {
                // XXX Evaluate this TypeVariable to its bound type.
                result = handleAsString( Object.class ) ;
            } else if (type instanceof WildcardType) {
                // XXX Usually we will want to evaluate this WildCard to its bound type.
                // Just treat this the same as its bound type
                final WildcardType wt = (WildcardType)type ;
                final Type[] upperBounds = wt.getUpperBounds() ;
                final Type[] lowerBounds = wt.getLowerBounds() ;
                if (lowerBounds.length > 0) {
                    result = handleAsString( Object.class ) ;
                } else if (upperBounds.length == 0) {
                    result = handleAsString( Object.class ) ;
                } else if (upperBounds.length > 1) {
                    result = handleAsString( Object.class ) ;
                } else {
                    result = makeTypeConverter( upperBounds[0], mom ) ;
                }
            } else {
                result = handleAsString( Object.class ) ;
            }
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }
    
    private static TypeConverter handleClass( final Class<?> cls, 
        final ManagedObjectManagerInternal mom ) {
        
        if (mom.registrationDebug()) {
            dputil.enter( "handleClass" ) ;
        }
        
        TypeConverter result = null ;
        
        try {
            final ManagedObject mo = cls.getAnnotation( ManagedObject.class ) ;
            final ManagedData md = cls.getAnnotation( ManagedData.class ) ;
            if (mom.registrationDebug()) {
                dputil.info( "mo=", mo, "md=", md ) ;
            }
            
            if (mo != null) {
                result = handleManagedObject( cls, mom, mo ) ;
            } else if (md != null) {
                result = handleManagedData( cls, mom, md ) ;
            } else if (cls.isEnum()) {
                result = handleEnum( cls ) ;
            } else {
                // map to string
                // XXX Perhaps changes this to act as if MXBean?
                result = handleAsString( cls ) ;
            }
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    } 
	
    private static TypeConverter handleManagedObject( final Class type, 
	final ManagedObjectManagerInternal mom, final ManagedObject mo ) {

        TypeConverter result = null ;
        if (mom.registrationDebug()) {
            dputil.enter( "handleManagedObject", "type=", type,
                "mom=", mom, "mo=", mo ) ;
        }
        
        try {
            result = new TypeConverterImpl( type, SimpleType.OBJECTNAME ) {
                public Object toManagedEntity( Object obj ) {
                    return mom.getObjectName( obj ) ;
                }

                @Override
                public Object fromManagedEntity( final Object entity ) {
                    if (!(entity instanceof ObjectName)) {
                        throw Exceptions.self.entityNotObjectName( entity ) ;
                    }

                    final ObjectName oname = (ObjectName)entity ;
                    return mom.getObject( oname ) ;
                }
            } ;
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    @SuppressWarnings("unchecked")
    private static TypeConverter handleArrayType( final GenericArrayType type, 
	final ManagedObjectManagerInternal mom ) {

        if (mom.registrationDebug()) {
            dputil.enter( "handleArrayType" ) ;
        }
        
        TypeConverter result = null ;
        try {
            final Type ctype = type.getGenericComponentType() ;
            final TypeConverter ctypeTc = mom.getTypeConverter( ctype ) ;
            final OpenType cotype = ctypeTc.getManagedType() ;
            final OpenType ot ;

            try {
                ot = new ArrayType( 1, cotype ) ;
            } catch (OpenDataException exc) {
                throw Exceptions.self.noArrayOfArray() ;
            }

            final OpenType myManagedType = ot ;
            
            if (mom.registrationDebug()) {
                dputil.info( "ctype=", ctype, "ctypeTc=", ctypeTc,
                    "cotype=", cotype, "ot=", ot ) ;
            }

            result = new TypeConverterImpl( type, myManagedType ) {
                public Object toManagedEntity( final Object obj ) {
                    if (isIdentity()) {
                        return obj ;
                    } else {
                        final Class cclass = getJavaClass( ctype ) ;
                        final int length = Array.getLength( obj ) ;
                        final Object result = Array.newInstance( cclass, length ) ;
                        for (int ctr=0; ctr<length; ctr++) {
                            final Object elem = Array.get( obj, ctr ) ;
                            final Object relem =  ctypeTc.toManagedEntity( elem ) ;
                            Array.set( result, ctr, relem ) ;
                        }

                        return result ;
                    }
                }

                @Override
                public Object fromManagedEntity( final Object entity ) {
                    if (isIdentity()) {
                        return entity ;
                    } else {
                        final Class cclass = getJavaClass( cotype ) ;

                        final int length = Array.getLength( entity ) ;
                        final Object result = Array.newInstance( cclass, length ) ;
                        for (int ctr=0; ctr<length; ctr++) {
                            final Object elem = Array.get( entity, ctr ) ;
                            final Object relem =  
                                ctypeTc.fromManagedEntity( elem ) ;
                            Array.set( result, ctr, relem ) ;
                        }

                        return result ;
                    }
                }

                @Override
                public boolean isIdentity() {
                    return ctypeTc.isIdentity() ; 
                }
            } ;
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    private static TypeConverter handleEnum( final Class cls ) {

	return new TypeConverterImpl( cls, SimpleType.STRING ) {
	    public Object toManagedEntity( final Object obj ) {
		return obj.toString() ;
	    }

            @Override
            @SuppressWarnings("unchecked")
	    public Object fromManagedEntity( final Object entity ) {
		if (!(entity instanceof String)) {
                    throw Exceptions.self.notAString(entity) ;
                }

		return Enum.valueOf( cls, (String)entity ) ;
	    }
	} ;
    }

    @SuppressWarnings({"unchecked"})
    private static TypeConverter handleAsString( final Class cls ) {

        Constructor cs = null ;
        try {
            cs = cls.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException ex) {
            // log message
        } catch (SecurityException ex) {
            // log message
        }

	final Constructor cons = cs ;

	return new TypeConverterImpl( cls, SimpleType.STRING ) {
	    public Object toManagedEntity( Object obj ) {
                if (obj == null) {
                    return "*NULL*" ;
                } else {
                    return obj.toString() ;
                }
	    }

            @Override
	    public Object fromManagedEntity( final Object entity ) {
		if (cons == null) {
                    throw Exceptions.self.noStringConstructor(cls);
                }
                
		try {
                    final String str = (String) entity;
                    return cons.newInstance(str);
                } catch (InstantiationException exc) {
                    throw Exceptions.self.stringConversionError(cls, exc ) ;
                } catch (IllegalAccessException exc) {
                    throw Exceptions.self.stringConversionError(cls, exc ) ;
                } catch (InvocationTargetException exc) {
                    throw Exceptions.self.stringConversionError(cls, exc ) ;
                }
	    }
	} ;
    }

    private static TypeConverter handleSimpleType( final Class cls, 
	final OpenType stype ) {

	return new TypeConverterImpl( cls, stype ) {
	    public Object toManagedEntity( final Object obj ) {
		return obj ;
	    }

            @Override
	    public Object fromManagedEntity( final Object entity ) {
		return entity ;
	    }

            @Override
	    public boolean isIdentity() {
		return true ; 
	    }
	} ;
    }
    
    private static Collection<AttributeDescriptor> analyzeManagedData( 
        final Class<?> cls, final ManagedObjectManagerInternal mom ) {
       
        if (mom.registrationDebug()) {
            dputil.enter( "analyzeManagedData", "cls=", cls, "mom=", mom ) ;
        }
        
        Collection<AttributeDescriptor> result = null ;
        
        try {
            final ClassAnalyzer ca = mom.getClassAnalyzer( cls, 
                ManagedData.class ).second() ;

            final Pair<Map<String,AttributeDescriptor>,
                Map<String,AttributeDescriptor>> ainfos =
                    mom.getAttributes( ca ) ;

            result = ainfos.first().values() ;
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    private static CompositeType makeCompositeType( final Class cls, 
        final ManagedObjectManagerInternal mom, final ManagedData md, 
        Collection<AttributeDescriptor> minfos ) {

        if (mom.registrationDebug()) {
            dputil.enter( "makeCompositeType",
                "cls=", cls, "mom=", mom, "md=", md, "minfos=", minfos ) ;
        }
        
        CompositeType result = null ;
        
        try {
            String name = md.name() ;
            if (name.equals( "" )) {
                name = mom.getStrippedName( cls ) ;
            }
            
            if (mom.registrationDebug()) {
                dputil.info( "name=", name ) ;
            }
            
            final String mdDescription = mom.getDescription( cls ) ;
            if (mom.registrationDebug()) {
                dputil.info( "mdDescription=", mdDescription ) ;
            }
            
            final int length = minfos.size() ;
            final String[] attrNames = new String[ length ] ;
            final String[] attrDescriptions = new String[ length ] ;
            final OpenType[] attrOTypes = new OpenType[ length ] ;

            int ctr = 0 ;
            for (AttributeDescriptor minfo : minfos) {
                attrNames[ctr] = minfo.id() ;
                attrDescriptions[ctr] = minfo.description() ;
                attrOTypes[ctr] = minfo.tc().getManagedType() ;
                ctr++ ;
            }
            
            if (mom.registrationDebug()) {
                dputil.info( "attrNames=", Arrays.asList(attrNames),
                    "attrDescriptions=", Arrays.asList(attrDescriptions),
                    "attrOTypes=", Arrays.asList(attrOTypes) ) ;
            }

            try {
                result = new CompositeType( 
                    name, mdDescription, attrNames, attrDescriptions, attrOTypes ) ;
            } catch (OpenDataException exc) {
                throw Exceptions.self.exceptionInMakeCompositeType(exc) ;
            }
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    private static TypeConverter handleManagedData( final Class cls, 
	final ManagedObjectManagerInternal mom, final ManagedData md ) {

        if (mom.registrationDebug()) {
            dputil.enter( "handleManagedData", "cls=", cls,
                "mom=", mom, "md=", md ) ;
        }
        
        TypeConverter result = null ;
        try {
            final Collection<AttributeDescriptor> minfos = analyzeManagedData(
                cls, mom ) ;
            final CompositeType myType = makeCompositeType( cls, mom, md, minfos ) ;
            if (mom.registrationDebug()) {
                dputil.info( "minfos=", minfos, "myType=", myType ) ;
            }

            result = new TypeConverterImpl( cls, myType ) {
                public Object toManagedEntity( Object obj ) {
                    if (mom.runtimeDebug()) {
                        dputil.enter( "(ManagedData):toManagedEntity", "obj=", obj ) ;
                    }
                    
                    Object runResult = null ;
                    try {
                        Map<String,Object> data = new HashMap<String,Object>() ;
                        for (AttributeDescriptor minfo : minfos) {
                            if (mom.runtimeDebug()) {
                                dputil.info( "Fetching attribute " + minfo.id() ) ;
                            }
                            
                            Object value = null ;
                            if (minfo.isApplicable( obj )) {
                                try {
                                    FacetAccessor fa = mom.getFacetAccessor( obj ) ;
                                    value = minfo.get(fa, mom.runtimeDebug());
                                } catch (JMException ex) {
                                    if (mom.runtimeDebug()) {
                                        dputil.exception( "Error", ex) ;
                                    }
                                }
                            }

                            data.put( minfo.id(), value ) ;
                        }

                        try {
                            runResult = new CompositeDataSupport( myType, data ) ;
                        } catch (OpenDataException exc) {
                            throw Exceptions.self.exceptionInHandleManagedData(exc) ;
                        }
                    } finally {
                        if (mom.runtimeDebug()) {
                            dputil.exit( runResult ) ;
                        }
                    }
                    
                    return runResult ;
                }
            } ;
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    private static class EnumerationAdapter<T> implements Iterator<T> {
        final private Enumeration<T> enumeration ;

        public EnumerationAdapter( final Enumeration<T> en ) {
            this.enumeration = en ;
        }

        public boolean hasNext() {
            return enumeration.hasMoreElements() ;
        }

        public T next() {
            return enumeration.nextElement() ;
        }

        public void remove() {
            throw Exceptions.self.removeNotSupported() ;
        }
    }

    // TypeConverter that throws exceptions for its methods.  Used as a 
    // place holder to detect recursive types.
    public static class TypeConverterPlaceHolderImpl implements TypeConverter {
        public Type getDataType() {
            throw Exceptions.self.recursiveTypesNotSupported() ;
        }

        public OpenType getManagedType() {
            throw Exceptions.self.recursiveTypesNotSupported() ;
        }

        public Object toManagedEntity( Object obj ) {
            throw Exceptions.self.recursiveTypesNotSupported() ;
        }

        public Object fromManagedEntity( Object entity ) {
            throw Exceptions.self.recursiveTypesNotSupported() ;
        }

        public boolean isIdentity() {
            throw Exceptions.self.recursiveTypesNotSupported() ;
        }
    }

    private abstract static class TypeConverterListBase 
        extends TypeConverterImpl {
        
        final TypeConverter memberTc ;

        public TypeConverterListBase( final Type dataType, 
            final TypeConverter memberTc ) {
            
            super( dataType, makeArrayType( memberTc.getManagedType() ) ) ;
            this.memberTc = memberTc ;
        }

        @SuppressWarnings("unchecked")
        private static ArrayType makeArrayType( final OpenType ot ) {
            try {
                return new ArrayType( 1, ot ) ;
            } catch (OpenDataException exc) {
                throw Exceptions.self.openTypeInArrayTypeException( ot, exc) ;
            }
        }

        protected abstract Iterator getIterator( Object obj ) ;

        public Object toManagedEntity( final Object obj ) {
            final Iterator iter = getIterator( obj ) ;
            final List<Object> list = new ArrayList<Object>() ;
            while (iter.hasNext()) {
                list.add( iter.next() ) ;
            }

            final Class cclass = getJavaClass( memberTc.getManagedType() ) ;
            final Object result = Array.newInstance( cclass, list.size() ) ;
            int ctr = 0 ;
            for (Object elem : list) {
                final Object mappedElem = memberTc.toManagedEntity( elem ) ;
                Array.set( result, ctr++, mappedElem ) ;
            }

            return result ;
        }
    }

    private interface Table<K,V> extends Iterable<K> {
        V get( K key ) ;
    }

    private static class TableMapImpl<K,V> implements Table<K,V> {
        final private Map<K,V> map ;

        public TableMapImpl( final Map<K,V> map ) {
            this.map = map ;
        }

        public Iterator<K> iterator() {
            return map.keySet().iterator() ;
        }

        public V get( final K key ) {
            return map.get( key ) ;
        }
    }

    private static class TableDictionaryImpl<K,V> implements Table<K,V> {
        final private Dictionary<K,V> dict ;

        public TableDictionaryImpl( final Dictionary<K,V> dict ) {
            this.dict = dict ;
        }

        @SuppressWarnings("unchecked")
        public Iterator<K> iterator() {
            return new EnumerationAdapter( dict.elements() ) ;
        }

        public V get( final K key ) {
            return dict.get( key ) ;
        }
    }

    private abstract static class TypeConverterMapBase 
        extends TypeConverterImpl {
        
        final private TypeConverter keyTypeConverter ;
        final private TypeConverter valueTypeConverter ;

        public TypeConverterMapBase( Type dataType, 
            TypeConverter keyTypeConverter, TypeConverter valueTypeConverter ) {
            
            super( dataType, makeMapTabularType( keyTypeConverter, 
                valueTypeConverter ) ) ;
            this.keyTypeConverter = keyTypeConverter ;
            this.valueTypeConverter = valueTypeConverter ;
        }

        private static TabularType makeMapTabularType( 
            final TypeConverter firstTc, final TypeConverter secondTc ) {
            
            final String mapType = firstTc + "->" + secondTc ;

            final String[] itemNames = new String[] { "key", "value" } ;

            final String description = Exceptions.self.rowTypeDescription(
                mapType) ;

            final String[] itemDescriptions = new String[] {
                Exceptions.self.keyFieldDescription(mapType),
                Exceptions.self.valueFieldDescription(mapType)
            } ;

            final OpenType[] itemTypes = new OpenType[] {
                firstTc.getManagedType(), secondTc.getManagedType() 
            } ;

            try {
                final CompositeType rowType = new CompositeType( mapType,
                    description, itemNames, itemDescriptions, itemTypes ) ;

                final String[] keys = new String[] { "key" } ;
                final String tableName =
                    Exceptions.self.tableName( mapType ) ;
                final String tableDescription = 
                    Exceptions.self.tableDescription( mapType ) ;

                final TabularType result = new TabularType( tableName,
                    tableDescription, rowType, keys ) ;

                return result ;
            } catch (OpenDataException exc) {
                throw Exceptions.self.exceptionInMakeMapTabularType(exc) ;
            }
        }

        protected abstract Table getTable( Object obj ) ;

        @SuppressWarnings("unchecked")
        public Object toManagedEntity( Object obj ) {
            try {
                final Table table = getTable( obj ) ;
                final TabularType ttype = (TabularType)getManagedType() ;
                final CompositeType ctype = ttype.getRowType() ;
                final TabularData result = new TabularDataSupport( ttype ) ;
                for (Object key : table) {
                    @SuppressWarnings("unchecked")
                    final Object value = table.get( key ) ;
                    final Object mappedKey = 
                        keyTypeConverter.toManagedEntity( key ) ;
                    final Object mappedValue = 
                        valueTypeConverter.toManagedEntity( value ) ;
                    final Map items = new HashMap() ;
                    items.put( "key", mappedKey ) ;
                    items.put( "value", mappedValue ) ;
                    CompositeDataSupport cdata = new CompositeDataSupport( 
                        ctype, items ) ;
                    result.put( cdata ) ;
                }

                return result ;
            } catch (OpenDataException exc) {
                throw Exceptions.self.excInMakeMapTabularDataToManagedEntity( 
                    exc ) ;
            }
        }
    }

    // Type must be a T<X,...> :
    // 1. T is a collection type: Collection, Iterator, Iterable, Enumeration, 
    // so the type is T<X> for some type X.  This maps to an array of the 
    // mapping of X.
    // 2. T is a mapping type: Map or Dictionary
    //    type is T<K,V>.  This maps to a TabularType, with key field named 
    //    "key" of type mapping of K, and value field "value" of type mapping 
    //    of V.
    // 3. Otherwise return null.
    private static TypeConverter handleParameterizedType( 
        final ParameterizedType type, 
	final ManagedObjectManagerInternal mom ) {

        if (mom.registrationDebug()) {
            dputil.enter( "handleParameterizedType", "type=", type,
                "mom=", mom ) ;
        }
        
        TypeConverter result = null ;
        
        try {
            final Class cls = (Class)(type.getRawType()) ; 
            final Type[] args = type.getActualTypeArguments() ;
            if (mom.registrationDebug()) {
                dputil.info( "cls=", cls, "args=", Arrays.asList( args ) ) ;
            }
            
            if (args.length < 1) {
                throw Exceptions.self.paramTypeNeedsArgument(type) ;
            }

            final Type firstType = args[0] ;
            final TypeConverter firstTc = mom.getTypeConverter( firstType ) ;

            if (mom.registrationDebug()) {
                dputil.info( "firstType=", firstType, "firstTc=", firstTc ) ;
            }
            
            // Case 1: Some kind of collection. Must have 1 type parameter.
            if (Iterable.class.isAssignableFrom(cls)) {
                result = new TypeConverterListBase( type, firstTc ) {
                    protected Iterator getIterator( Object obj ) {
                        return ((Collection)obj).iterator() ;
                    }
                } ;
            } else if (Iterator.class.isAssignableFrom(cls)) {
                result = new TypeConverterListBase( type, firstTc ) {
                    protected Iterator getIterator( Object obj ) {
                        return (Iterator)obj ;
                    }
                } ;
            } else if (Enumeration.class.isAssignableFrom(cls)) {
                result = new TypeConverterListBase( type, firstTc ) {
                    @SuppressWarnings("unchecked")
                    protected Iterator getIterator( Object obj ) {
                        return new EnumerationAdapter( (Enumeration)obj ) ;
                    }
                } ;
            } else if (args.length != 2) {
                result = handleClass( (Class<?>)type.getRawType(), mom ) ;
            } else {
                final Type secondType = args[0] ;
                final TypeConverter secondTc = mom.getTypeConverter( secondType ) ;
           
                if (mom.registrationDebug()) {
                    dputil.info( "secondType=", secondType, 
                        "secondTc=", secondTc ) ;
                }
                
                if (Map.class.isAssignableFrom(cls)) {
                    result = new TypeConverterMapBase( type, firstTc, secondTc ) {
                        @SuppressWarnings("unchecked")
                        protected Table getTable( Object obj ) {
                            return new TableMapImpl( (Map)obj ) ;
                        }
                    } ;
                } else if (Dictionary.class.isAssignableFrom(cls)) {
                    result = new TypeConverterMapBase( type, firstTc, secondTc ) {
                        @SuppressWarnings("unchecked")
                        protected Table getTable( Object obj ) {
                            return new TableDictionaryImpl( (Dictionary)obj ) ;
                        }
                    } ;
                } else {
                    // XXX This loses all information about type evaluation:
                    // instead, we want the class WITH the evaluate types.
                    result = handleClass( (Class<?>)type.getRawType(), mom ) ;
                }
            }
        } catch (RuntimeException exc) {
            if (mom.registrationDebug()) {
                dputil.exception( "Error", exc ) ;
            }
            throw exc ;
        } finally {
            if (mom.registrationDebug()) {
                dputil.exit( result ) ;
            }
        }

        return result ;
    }
    
    // Basic support for all TypeConverters.
    @DumpToString
    protected final Type dataType ;
    @DumpToString
    protected final OpenType managedType ;

    protected TypeConverterImpl( final Type dataType, 
        final OpenType managedType ) {
        
        this.dataType = dataType ;
        this.managedType = managedType ;
    }

    /* Java generic type of attribute in problem-domain Object.
     */
    public final Type getDataType() {
        return dataType ;
    }

    /* Open MBeans Open Type for management domain object.
     */
    public final OpenType getManagedType() {
        return managedType ;
    }

    /* Convert from a problem-domain Object obj to a ManagedEntity.
     */
    public abstract Object toManagedEntity( Object obj ) ;

    /* Convert from a ManagedEntity to a problem-domain Object.
     */
    public Object fromManagedEntity( Object entity ) {
        throw Exceptions.self.openToJavaNotSupported(managedType, dataType) ;
    }

    /* Returns true if this TypeConverter is an identity transformation.
     */
    public boolean isIdentity() {
        return false ;
    }
    
    private String displayOpenType( OpenType otype ) {
        if (otype instanceof SimpleType) {
            SimpleType stype = (SimpleType)otype ;
            return "SimpleType(" + otype.getTypeName() + ")" ;
        } else if (otype instanceof ArrayType) {
            ArrayType atype = (ArrayType)otype ;
            return "ArrayType(" + displayOpenType( atype.getElementOpenType() )
                + "," + atype.getDimension() + ")" ;
        } else if (otype instanceof CompositeType) {
            CompositeType ctype = (CompositeType)otype ;
            return "CompositeType(" + ctype.getTypeName() + ")" ;
        } else if (otype instanceof TabularType) {
            TabularType ttype = (TabularType)otype ;
            return "TabularType(" + ttype.getTypeName() + ","
                + "rowType=" + ttype.getRowType()
                + "indexNames=" + ttype.getIndexNames() + ")" ;
        } else {
            return "UNKNOWN(" + otype + ")" ;
        }
    }

    public String toString() {
        return "TypeConverter[dataType=" + dataType 
            + ",managedType=" + displayOpenType( managedType ) + "]" ;
    }
}

