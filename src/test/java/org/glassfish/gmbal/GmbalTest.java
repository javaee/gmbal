/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2003-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.gmbal ;

import org.glassfish.pfl.basic.contain.Pair;
import org.glassfish.pfl.basic.func.UnaryPredicate;
import org.glassfish.pfl.basic.algorithm.Algorithms;
import org.glassfish.pfl.basic.func.UnaryFunction;
import java.io.IOException;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;
import java.lang.annotation.Inherited ;

import java.lang.reflect.Array;
import java.util.Iterator ;
import java.util.Map ;
import java.util.HashMap ;
import java.util.Hashtable ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.List ;
import java.util.Set ;
import java.util.HashSet ;
import java.util.Date ;


import java.math.BigInteger ;
import java.math.BigDecimal ;

import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException ;
import javax.management.ObjectName ;
import javax.management.MBeanServer ;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.openmbean.SimpleType ;
import javax.management.openmbean.OpenType ;
import javax.management.openmbean.CompositeData ;
import javax.management.openmbean.CompositeType ;
import javax.management.openmbean.TabularType ;
import javax.management.openmbean.TabularData ;

import org.glassfish.gmbal.impl.TypeConverter ;
import org.glassfish.gmbal.impl.ManagedObjectManagerInternal ;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.glassfish.external.amx.AMX;
import org.glassfish.external.statistics.BoundedRangeStatistic;
import org.glassfish.external.statistics.impl.BoundedRangeStatisticImpl;
import org.glassfish.gmbal.typelib.EvaluatedClassAnalyzer;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator;
import org.glassfish.gmbal.impl.TypeConverterImpl ;

import static org.glassfish.gmbal.typelib.EvaluatedType.* ;

public class GmbalTest extends TestCase {
    private static final boolean DEBUG = false ;
    private static boolean firstTime = true ;

    @Override
    protected void setUp() throws Exception {
        if (firstTime) {
            System.out.println( "****************** GmbalTest **********************" ) ;
            firstTime = false ;
        }
        super.setUp();
        final Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl" ) ;
        logger.setLevel(Level.OFF) ;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        final Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl" ) ;
        logger.setLevel(Level.INFO) ;
    }

    //==============================================================================================
    // Tests for Algorithms class
    //==============================================================================================
    private <A> void compareSequence( List<A> result, List<A> sdata ) {
	Iterator<A> resIter = result.iterator() ;
	Iterator<A> sdataIter = sdata.iterator() ;

	while (resIter.hasNext() && sdataIter.hasNext()) {
            assertEquals(resIter.next(), sdataIter.next());
        }

	assertEquals( resIter.hasNext(), sdataIter.hasNext() ) ;
    }
    
    // Test Algorithms
    public void testMap1() {
        System.out.println( "testMap1" ) ;
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 213 ) ;
	final List<Integer> sdata = new ArrayList<Integer>() ;

	for (Integer i : data) {
	    sdata.add( i*i ) ;
	}

	final UnaryFunction<Integer,Integer> square = new UnaryFunction<Integer,Integer>() {
	    public Integer evaluate( Integer arg ) {
		return arg*arg ;
	    }
	} ;

	List<Integer> result = Algorithms.map( data, square ) ;
    
	compareSequence( result, sdata ) ;
    }

    public void testMap2() {
        System.out.println( "testMap2" ) ;
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 213 ) ;
	final List<Integer> sdata = new ArrayList<Integer>() ;

	for (Integer i : data) {
	    sdata.add( i*i ) ;
	}

	final UnaryFunction<Integer,Integer> square = new UnaryFunction<Integer,Integer>() {
	    public Integer evaluate( Integer arg ) {
		return arg*arg ;
	    }
	} ;

	List<Integer> result = new ArrayList<Integer>() ;
	Algorithms.map( data, result, square ) ;

	compareSequence( result, sdata ) ;
    }

    public void testFilter1() {
        System.out.println( "testFilter1" ) ;
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 213, 16, 1, 25 ) ;
	final List<Integer> sdata = new ArrayList<Integer>() ;

	for (Integer i : data) {
	    if ((i & 2) == 0) {
                sdata.add(i);
            }
	}

	final UnaryPredicate<Integer> ifEven = new UnaryPredicate<Integer>() {
	    public boolean evaluate( Integer arg ) {
		return (arg & 2) == 0 ;
	    }
	} ;

	List<Integer> result = Algorithms.filter( data, ifEven ) ;
	compareSequence( result, sdata ) ;
    }

    public void testFilter2() {
        System.out.println( "testFilter2" ) ;
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 213, 16, 1, 25 ) ;
	final List<Integer> sdata = new ArrayList<Integer>() ;

	for (Integer i : data) {
	    if ((i & 2) == 0) {
                sdata.add(i);
            }
	}

	final UnaryPredicate<Integer> ifEven = new UnaryPredicate<Integer>() {
	    public boolean evaluate( Integer arg ) {
		return (arg & 2) == 0 ;
	    }
	} ;

	List<Integer> result = new ArrayList<Integer>() ;
	Algorithms.filter( data, result, ifEven ) ;
	compareSequence( result, sdata ) ;
    }

    public void testFind() {
        System.out.println( "testFind" ) ;
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 42, 213, 16, 1, 25 ) ;

	final UnaryPredicate<Integer> is42 = new UnaryPredicate<Integer>() {
	    public boolean evaluate( Integer arg ) {
		return arg == 42 ;
	    }
	} ;

	Integer result = Algorithms.find( data, is42 ) ;
	assertTrue( result != null ) ;
	assertTrue( result == 42 ) ;
    }

    //==============================================================================================
    // Tests for AnnotationUtil class
    //==============================================================================================

    private static EvaluatedClassAnalyzer getCA( Class<?> cls ) {
        final EvaluatedClassDeclaration cdecl =
            (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(cls) ;
        return new EvaluatedClassAnalyzer( cdecl ) ;
    }

    public interface A {} 
    public interface B extends A {}
    public interface C extends A {}
    public interface D extends B, C {}
    public interface E {}
    public interface F extends A, E {}
    public interface G extends F, D {}
    public static class H implements F {}
    public static class I extends H implements G {}

    private static final Class[] cdata =  {
	Object.class, 
	A.class, B.class, C.class, D.class, E.class,
	F.class, G.class, H.class, I.class } ;

    // Invariants to test on makeInheritanceChain:
    // For any class C:
    // Let R = makeInheritanceChain(C). Then:
    //
    // C precedes C.super, all C.implements in R
    // C is the first element of R.


    public void testGetInheritanceChain() {
        System.out.println( "testGetInheritanceChain" ) ;
        EvaluatedClassAnalyzer ca = getCA( I.class ) ;
        List<EvaluatedClassDeclaration> res = ca.findClasses(
            Algorithms.TRUE( EvaluatedClassDeclaration.class ) ) ;
	// System.out.println( "Inheritance chain for class " + I.class.getName() 
	    // + " is " + res ) ;

	Map<EvaluatedClassDeclaration,Integer> positions =
            new HashMap<EvaluatedClassDeclaration,Integer>() ;
	int position = 0 ;
	for (EvaluatedClassDeclaration cls : res) {
	    positions.put( cls, position++ ) ;
	}

        EvaluatedClassDeclaration idecl = getECD( I.class ) ;
	Integer firstIndex = positions.get( idecl ) ;
	assertNotNull( "Index for top-level class " + idecl.name()
	    + " is null", firstIndex ) ;
	assertTrue( "Index of top-level class " + idecl.name()
	    + " is " + firstIndex + " but should be 0",
            firstIndex == 0 ) ;

	for (Class cls : cdata) {
            EvaluatedClassDeclaration ecd = getECD( cls ) ;
	    Integer cindex = positions.get( ecd ) ;
	    assertNotNull(  "Index for class " + ecd.name() + " is null",
                cindex ) ;

            for (EvaluatedClassDeclaration sdecl : ecd.inheritance()) {
                Integer iindex = positions.get( sdecl ) ;
		assertNotNull( "Index of interface " + sdecl.name() + " should not be null",
                    iindex ) ;
		assertTrue( "Index of class " + ecd.name() + " is " + cindex
		    + ": should be less than index of interface " 
		    + sdecl.name() + " which is " + iindex,
                    cindex < iindex ) ;
            }
	}
    }

    @Target(ElementType.METHOD) 
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Test1{} 

    @Target(ElementType.METHOD) 
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Test2{}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Test3{}

    @Test3
    private interface AA {
	@Test1
	int getFooA() ;

	@Test1
	void setFooA( int arg ) ;


	@Test2
	int barA() ;

	@Test2
	void barA( int arg ) ;
    }

    private interface BB extends AA {
	@Test1
	boolean isSomething() ;
    }

    @Test3
    private interface CC extends AA {
	@Test2
	int getFooC() ;
    }

    private interface DD extends CC, BB {
	@Test2
	void setFooD( int arg ) ;
    }

    private static EvaluatedMethodDeclaration getMethod( Class<?> cls,
        String name, Class... args) {

        List<EvaluatedType> argDecls = Algorithms.map(
            Arrays.asList( args ),
            new UnaryFunction<Class,EvaluatedType>() {
                public EvaluatedType evaluate( Class cls ) {
                    return TypeEvaluator.getEvaluatedType( cls ) ;
        } } ) ;

	try {
            EvaluatedClassDeclaration ecd =
                (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(cls) ;

            for (EvaluatedMethodDeclaration emd : ecd.methods()) {
                if (emd.name().equals( name )
                    && emd.parameterTypes().equals( argDecls )) {
                    return emd ;
                }
            }

            return null ;
	} catch (Exception exc) {
	    fail( "getMethod() caught exception " + exc ) ;
	    return null ;
	}
    }

    public void testFindMethod() {
        System.out.println( "testFindMethod" ) ;
        final EvaluatedClassAnalyzer ca = getCA( DD.class ) ;
	final UnaryPredicate predicate =
	    new UnaryPredicate() {
		public boolean evaluate( Object obj ) {
                    EvaluatedMethodDeclaration method =
                        (EvaluatedMethodDeclaration)obj ;

		    return method.name().equals("barA") &&
			method.returnType() == EvaluatedType.EINT ;
		}
	    } ;

	final EvaluatedMethodDeclaration expectedResult =
            getMethod( AA.class, "barA" ) ;

        @SuppressWarnings("unchecked")
	final List<EvaluatedMethodDeclaration> result =
            ca.findMethods( predicate ) ;
        assertEquals( result.size(), 1 ) ;
        EvaluatedMethodDeclaration resultMethod = result.get(0) ;
	assertEquals( expectedResult, resultMethod ) ;
    }

    public void testGetAnnotatedMethods() throws IOException {
        System.out.println( "testGetAnnotatedMethods" ) ;
        ManagedObjectManagerInternal mom = (ManagedObjectManagerInternal)
            ManagedObjectManagerFactory.createStandalone("master" ) ;
        try {
            EvaluatedClassAnalyzer ca = getCA( DD.class ) ;
            List<EvaluatedMethodDeclaration> methods =
                ca.findMethods( mom.forAnnotation( Test2.class,
                EvaluatedMethodDeclaration.class) ) ;
            Set<EvaluatedMethodDeclaration> methodSet =
                new HashSet<EvaluatedMethodDeclaration>( methods ) ;

            EvaluatedMethodDeclaration[] expectedMethods = {
                getMethod( DD.class, "setFooD", int.class ),
                getMethod( CC.class, "getFooC" ),
                getMethod( AA.class, "barA" ),
                getMethod( AA.class, "barA", int.class ) } ;

            List<EvaluatedMethodDeclaration> expectedMethodList =
                Arrays.asList( expectedMethods ) ;
            Set<EvaluatedMethodDeclaration> expectedMethodSet =
                new HashSet<EvaluatedMethodDeclaration>( expectedMethodList ) ;

            assertEquals( expectedMethodSet, methodSet ) ;
        } finally {
            mom.close() ;
        }
    }

    private EvaluatedClassDeclaration getECD( Class<?> cls ) {
        return (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( cls ) ;
    }

    public void testGetClassAnnotations() throws IOException {
        System.out.println( "testGetClassAnnotations" ) ;
        List<EvaluatedClassDeclaration> expectedResult =
            new ArrayList<EvaluatedClassDeclaration>() ;
        expectedResult.add( getECD( DD.class ) ) ;
        expectedResult.add( getECD( BB.class ) ) ;
        expectedResult.add( getECD( CC.class ) ) ;
        expectedResult.add( getECD( AA.class ) ) ;
        ManagedObjectManagerInternal mom = (ManagedObjectManagerInternal)
            ManagedObjectManagerFactory.createStandalone("master" ) ;
        try {
            EvaluatedClassAnalyzer ca = getCA( DD.class ) ;
            List<EvaluatedClassDeclaration> classes =
                ca.findClasses( mom.forAnnotation( Test3.class,
                EvaluatedClassDeclaration.class ) ) ;

            assertEquals( classes, expectedResult ) ;
        } finally {
            mom.close() ;
        }
    }

    private static EvaluatedMethodDeclaration getter_fooA =
        getMethod( AA.class, "getFooA" ) ;
    private static EvaluatedMethodDeclaration setter_fooA =
        getMethod( AA.class, "setFooA", int.class ) ;
    private static EvaluatedMethodDeclaration getter_barA =
        getMethod( AA.class, "barA" ) ;
    private static EvaluatedMethodDeclaration setter_barA =
        getMethod( AA.class, "barA", int.class ) ;
    private static EvaluatedMethodDeclaration getter_something =
        getMethod( BB.class, "isSomething" ) ;
    private static EvaluatedMethodDeclaration getter_fooC =
        getMethod( CC.class, "getFooC" ) ;
    private static EvaluatedMethodDeclaration setter_fooD =
        getMethod( DD.class, "setFooD", int.class ) ;

    /*
    public void testIsSetterIsGetter() {
	Method m ;
        ClassAnalyzer ca = new ClassAnalyzer( DD.class ) ;
	ManagedObjectManagerInternal mom = 
            (ManagedObjectManagerInternal)ManagedObjectManagerFactory
                .createStandalone( 
                    "ORBTest", "", null ) ;
                        
        try {
            AttributeDescriptor ad ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "fooA", "null description", 
                AttributeDescriptor.AttributeType.GETTER ) ;
            assertEquals( getter_fooA, ad.method() ) ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "fooA", "null description", 
                AttributeDescriptor.AttributeType.SETTER ) ;
            assertEquals( setter_fooA, ad.method() ) ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "barA", "null description", 
                AttributeDescriptor.AttributeType.GETTER ) ;
            assertEquals( getter_barA, ad.method() ) ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "barA", "null description", 
                AttributeDescriptor.AttributeType.SETTER ) ;
            assertEquals( setter_barA, ad.method() ) ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "something", "null description", 
                AttributeDescriptor.AttributeType.GETTER ) ;
            assertEquals( getter_something, ad.method() ) ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "fooC", "null description", 
                AttributeDescriptor.AttributeType.GETTER ) ;
            assertEquals( getter_fooC, ad.method() ) ;

            ad = AttributeDescriptor.findAttribute( mom, ca, "fooD", "null description", 
                AttributeDescriptor.AttributeType.SETTER ) ;
            assertEquals( setter_fooD, ad.method() ) ;
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                // Don't care
            }
        }
    }
    */
    
    //==============================================================================================
    // Tests for TypeConverter class
    //==============================================================================================
    
    // For each kind of type, test:
    // getDataType
    // getManagedType
    // toManagedEntity
    // fromManagedEntity
    // isIdentity
    //
    // to/from round trip should result in equal objects (java -> managed -> java -> managed)
    //
    // Types to test:
    //
    // primitives (DONE)
    // Date, ObjectName, String, BigDecimal, BigInteger (DONE)
    // enum
    // ManagedObject
    // ManagedData, simple case (DONE)
    // ManagedData, @IncludeSubclass
    // ManagedData, @InheritTable
    // C<X> for a subtype of Collection
    // M<K,V> for a subtype of map (map, sorted map)
    // X[]
    // Main cases of interest for collections, maps, and arrays are ManagedObject, ManagedData, and something 
    //	    simple like String
    // Defer testing of TypeVariable and WildCardType
    // Also test an arbitrary class not of the above (uses toString, may or may not have a <init>( String )
    // constructor

    private static ObjectName makeObjectName( String str ) {
	try {
	    return new ObjectName( str ) ;
	} catch (MalformedObjectNameException exc) {
	    throw new RuntimeException( exc ) ;
	}
    }

    private static final Object[][] primitiveTCTestData = new Object[][] {
	{ BigDecimal.class, SimpleType.BIGDECIMAL, new BigDecimal( "1.234556677888" ) },
	{ BigInteger.class, SimpleType.BIGINTEGER, new BigInteger( "1234566789012334566576790" ) },
	{ boolean.class, SimpleType.BOOLEAN, Boolean.TRUE },
	{ Boolean.class, SimpleType.BOOLEAN, Boolean.TRUE },
	{ byte.class, SimpleType.BYTE, 5 },
	{ Byte.class, SimpleType.BYTE, -23 },
	{ char.class, SimpleType.CHARACTER, 'a' },
	{ Character.class, SimpleType.CHARACTER, 'A' },
	{ Date.class, SimpleType.DATE, new Date() },
	{ double.class, SimpleType.DOUBLE, Double.valueOf( 1.2345D ) },
	{ Double.class, SimpleType.DOUBLE, Double.valueOf( 1.2345D ) },
	{ float.class, SimpleType.FLOAT, Float.valueOf( 1.2345F ) },
	{ Float.class, SimpleType.FLOAT, Float.valueOf( 1.2345F ) },
	{ short.class, SimpleType.SHORT, Short.valueOf( (short)24 ) },
	{ Short.class, SimpleType.SHORT, Short.valueOf( (short)26 ) },
	{ int.class, SimpleType.INTEGER, Integer.valueOf( 2345323 ) },
	{ Integer.class, SimpleType.INTEGER, Integer.valueOf( 2345323 ) },
	{ long.class, SimpleType.LONG, Long.valueOf( 743743743743743743L ) },
	{ Long.class, SimpleType.LONG, Long.valueOf( 743743743743743743L ) },
	{ ObjectName.class, SimpleType.OBJECTNAME, makeObjectName( "foo: bar1=red, bar2=blue" ) },
	{ String.class, SimpleType.STRING, "foo" } 
    } ;

    private static final Set<EvaluatedType> notIdentity = new HashSet<EvaluatedType>(
        Arrays.asList( EBYTE, EBOOLEAN, ECHAR, EINT, ESHORT, ELONG,
            EFLOAT, EDOUBLE ) ) ;

    public void testPrimitiveTypeConverter() {
        System.out.println( "testPrimitiveTypeConverter" ) ;
	ManagedObjectManagerInternal mom = 
            (ManagedObjectManagerInternal)ManagedObjectManagerFactory
                .createStandalone( "ORBTest" ) ;
        try {
            for (Object[] data : primitiveTCTestData) {
                Class cls = (Class)data[0] ;
                EvaluatedClassDeclaration ecd = getECD( cls ) ;
                SimpleType st = (SimpleType)data[1] ;
                Object value = data[2] ;

                TypeConverter tc = mom.getTypeConverter( ecd ) ;

                assertTrue( tc.getDataType() == ecd ) ;
                assertTrue( tc.getManagedType() == st ) ;
                assertTrue( tc.isIdentity() ==
                    !notIdentity.contains( tc.getDataType() ) ) ;

                Object managed = tc.toManagedEntity( value ) ;
                Object value2 = tc.fromManagedEntity( managed ) ;
                assertEquals( value, value2 ) ;

                Object managed2 = tc.toManagedEntity( value2 ) ;
                assertEquals( managed, managed2 ) ;
            }
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                // don't care
            }
        }
    }

    public static final String MDE_DESCRIPTION = "Description of ManagedDataExample" ;
    public static final String MDE_ATTR_DESC_NAME = "Description of ManagedDataExample name attribute" ;
    public static final String MDE_ATTR_DESC_DATE = "Description of ManagedDataExample date attribute" ;
    public static final String MDE_ATTR_DESC_GET_STRING = "Description of ManagedDataExample string attribute" ;
    public static final String MDE_ATTR_ID_NAME = "theName" ;
    public static final String MDE_ATTR_ID_DATE = "currentDate" ;
    public static final String MDE_ATTR_ID_GET_STRING = "string" ;

    @ManagedData
    @Description( MDE_DESCRIPTION )
    public static class ManagedDataExample {
	private String name ;
	private Date date ;

	public ManagedDataExample( String name ) {
	    this.name = name ;
	    date = new Date() ;
	}

	@ManagedAttribute( id=MDE_ATTR_ID_NAME )
        @Description( MDE_ATTR_DESC_NAME ) 
	public String theName() {
	    return name ;
	}

	@ManagedAttribute( id=MDE_ATTR_ID_DATE )
        @Description( MDE_ATTR_DESC_DATE )
	public Date date() {
	    return new Date( date.getTime() ) ;
	}

        @ManagedAttribute
        @Description( MDE_ATTR_DESC_GET_STRING ) 
        public String getString() {
            return name ;
        }

        @Override
	public boolean equals( Object obj ) {
	    if (this == obj) {
                return true;
            }

	    if (!(obj instanceof ManagedDataExample)) {
                return false;
            }

	    ManagedDataExample mde = (ManagedDataExample)obj ;
	    return mde.name.equals( name ) && mde.date.equals( date ) ;
	}

        @Override
	public int hashCode() {
	    return name.hashCode() ^ date.hashCode() ;
	}
    }

    public static final String MOE_DESCRIPTION = "Description of ManagedObject" ;
    public static final String MOE_ATTR_DESC_NAME = "Description of ManagedAttribute name" ;
    public static final String MOE_ATTR_DESC_NUM = "Description of ManagedAttribute num" ;
    public static final String MOE_ATTR_DESC_MDE = "Description of ManagedAttribute mde" ;
    public static final String MOE_OP_DESC_INCREMENT = "Description of ManagedOperation increment" ;
    public static final String MOE_OP_DESC_DECREMENT = "Description of ManagedOperation decrement" ;

    @ManagedObject
    @Description( MOE_DESCRIPTION ) 
    @AMXMetadata( isSingleton=true )
    public static class ManagedObjectExample {
	private int num ;
	private String name ;
	private ManagedDataExample mde ;

	public ManagedObjectExample( int num, String name ) {
	    this.num = num ;
	    this.name = name ;
	    this.mde = new ManagedDataExample( name ) ;
	}

	@ManagedAttribute
        @Description( MOE_ATTR_DESC_NAME )
	public String getTheName() {
	    return name ;
	}

	@ManagedAttribute
        @Description( MOE_ATTR_DESC_NUM ) 
	public int getNum() {
	    return num ;
	}

	@ManagedAttribute
        @Description( MOE_ATTR_DESC_MDE )
	public ManagedDataExample getMde() {
	    return mde ;
	}

	@ManagedOperation
        @Description( MOE_OP_DESC_INCREMENT ) 
	public int increment( int value ) {
	    return num+=value ;
	}

	@ManagedOperation
        @Description( MOE_OP_DESC_DECREMENT ) 
	public int decrement( int value ) {
	    return num-=value ;
	}

	public void nop() {
	    // This is not a managed operation.
	}
    }

    public static class ManagedObjectExampleDerived extends ManagedObjectExample {
        public ManagedObjectExampleDerived( int num, String name ) {
            super( num, name ) ;
        }
    }
    
    public void testManagedObjectExample() {
        System.out.println( "testManagedObjectExample" ) ;
        final int num = 12 ;
	final String name = "Liskov" ;
	final ManagedObjectExample rootObject = 
            new ManagedObjectExample( num, name ) ;
        managedObjectExampleHelper( rootObject ) ;
    }
    
    public void testManagedObjectExampleDerived() {
        System.out.println( "testManagedObjectExampleDerived" ) ;
        final int num = 12 ;
	final String name = "Liskov" ;
	final ManagedObjectExample rootObject = 
            new ManagedObjectExampleDerived( num, name ) ;
        managedObjectExampleHelper( rootObject ) ; 
    }
    
    @SuppressWarnings({"unchecked"})
    public void managedObjectExampleHelper( ManagedObjectExample root ) {
	final String domain = "ORBTest" ;
	final int num = 12 ;
	final String name = "Liskov" ;

	final String propName = "ObjectNumber" ;
	final int onum = 1 ;

	final ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            domain ) ;
        mom.createRoot( root ) ;

	try {
            // mom.setRegistrationDebug(
                // ManagedObjectManager.RegistrationDebugLevel.NONE ) ;
            //System.out.println( mom.dumpSkeleton( root ) ) ;

	    ObjectName moeName = mom.getObjectName( root ) ;
	    assertEquals( domain, moeName.getDomain() ) ;
	    
	    Hashtable expectedProperties = new Hashtable() ;
            expectedProperties.put( "pp", "/" ) ;
	    expectedProperties.put( "type", root.getClass().getName() ) ;
	    
	    assertEquals( expectedProperties, moeName.getKeyPropertyList() ) ;

	    MBeanServer mbs = mom.getMBeanServer() ;

	    // Validate attributes
	    assertEquals( mbs.getAttribute( moeName, "Num" ),
                Integer.valueOf( num ) ) ;
	    assertEquals( mbs.getAttribute( moeName, "TheName" ), name ) ;
	    Object obj = mbs.getAttribute( moeName, "Mde" ) ;
	    assertTrue( obj instanceof CompositeData ) ;
	    CompositeData compData = (CompositeData)obj ;
	    assertEquals( root.getMde().theName(), compData.get(
                MDE_ATTR_ID_NAME ) ) ;
	    assertEquals( root.getMde().date(), compData.get( 
                MDE_ATTR_ID_DATE ) ) ;
            assertEquals( root.getMde().getString(), compData.get( 
                MDE_ATTR_ID_GET_STRING ) ) ;

	    // Validate operations
	} catch (Exception exc) {
	    exc.printStackTrace() ;
	    fail( "Caught exception on register " + exc ) ;
	} finally {
	    try {
                mom.close() ;
	    } catch (Exception exc) {
		exc.printStackTrace() ;
		fail( "Caught exception on unregister " + exc ) ;
	    }
	}

	// make sure object is really gone
    }

    @SuppressWarnings("unchecked")
    public void testManagedDataTypeConverter() {
        System.out.println( "testManagedDataTypeConverter" ) ;
	ManagedObjectManagerInternal mom = 
            (ManagedObjectManagerInternal)ManagedObjectManagerFactory
                .createStandalone( "ORBTest" ) ;
    
        try {
            EvaluatedClassDeclaration cdecl = 
                getECD( ManagedDataExample.class ) ;
            TypeConverter tc = mom.getTypeConverter( cdecl ) ;
            assertTrue( tc.getDataType() == cdecl ) ;

            OpenType otype = tc.getManagedType() ;
            assertTrue( otype instanceof CompositeType ) ;
            CompositeType ctype = (CompositeType)otype ;
            assertEquals( MDE_DESCRIPTION, ctype.getDescription() ) ;
            assertEquals( MDE_ATTR_DESC_NAME, ctype.getDescription( MDE_ATTR_ID_NAME ) ) ;
            assertEquals( MDE_ATTR_DESC_DATE, ctype.getDescription( MDE_ATTR_ID_DATE ) ) ;
            assertEquals( SimpleType.STRING, ctype.getType( MDE_ATTR_ID_NAME ) ) ;
            assertEquals( SimpleType.DATE, ctype.getType( MDE_ATTR_ID_DATE ) ) ;

            Set<String> keys = new HashSet<String>() ;
            keys.add( MDE_ATTR_ID_NAME ) ;
            keys.add( MDE_ATTR_ID_DATE ) ;
            keys.add( MDE_ATTR_ID_GET_STRING ) ;
            assertEquals( keys, ctype.keySet() ) ;

            assertFalse( tc.isIdentity() ) ;

            ManagedDataExample value = new ManagedDataExample( "test" ) ;

            Object managed = tc.toManagedEntity( value ) ;

            assertTrue( managed instanceof CompositeData ) ;
            CompositeData compData = (CompositeData)managed ;
            assertEquals( compData.getCompositeType(), ctype ) ;
            assertEquals( value.theName(),
                (String)compData.get( MDE_ATTR_ID_NAME ) ) ;
            assertEquals( value.date(), (Date)compData.get( MDE_ATTR_ID_DATE ) ) ;
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                // Don't care
            }
        }
    }

    private static final String ROOT_DOMAIN = "this.test" ;
    private static final String ROOT_PARENT_NAME = ROOT_DOMAIN 
        + ":pp=/Test/Root/Name,type=FruitBat,name=Sam" ;
    
    private static final String ROOT_TYPE = "RootType" ;
    
    @ManagedObject
    @AMXMetadata( type=ROOT_TYPE)
    public static class RootObject {
        private int value ;
        
        @ManagedAttribute
        int num() { return value ; }
        
        public RootObject( int num ) {
            this.value = num ;
        }
    }
    
    @ManagedObject
    @AMXMetadata( type=ROOT_TYPE)
    public static class NamedRootObject extends RootObject{
        String name ;
        
        @NameValue
        public String getMyName() { return name ; }
        
        public NamedRootObject( String name, int num ) {
            super( num ) ;
            this.name = name;
        }
    }
        
    public void testRootMBean1() throws MalformedObjectNameException {
        System.out.println( "testRootMBean1" ) ;
        final int value = 42 ;
        final String rootName = "MyRoot" ;
        final Object rootObject = new RootObject( value ) ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createFederated(
            new ObjectName( ROOT_PARENT_NAME ) ) ;
        // mom.setRegistrationDebug(
            // ManagedObjectManager.RegistrationDebugLevel.NORMAL ) ;
        mom.stripPrefix("org.glassfish.gmbal");
        mom.createRoot( rootObject, rootName ) ;

        try {
            ObjectName rootObjectName = mom.getObjectName( rootObject ) ;
            String expectedName =
                "this.test:pp=/Test/Root/Name/FruitBat[Sam],"
                + "type=RootType,name=MyRoot" ;
            ObjectName expectedObjectName = null ;
            try {
                expectedObjectName = new ObjectName(expectedName);
            } catch (MalformedObjectNameException ex) {
                fail( "Could not create ObjectName: ex="  + ex ) ;
            }

            assertEquals( expectedObjectName, rootObjectName ) ;
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                fail( "Exception on close: " + ex ) ;
            }
        }
    }

    public void testRootMBean3() throws MalformedObjectNameException {
        System.out.println( "testRootMBean3" ) ;
        final int value = 42 ;
        final String rootName = "MyRoot" ;
        final Object rootObject = new RootObject( value ) ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createFederated(
            new ObjectName( ROOT_PARENT_NAME ) ) ;
        // mom.setRegistrationDebug(
            // ManagedObjectManager.RegistrationDebugLevel.NORMAL ) ;
        mom.stripPackagePrefix() ;
        mom.createRoot( rootObject, rootName ) ;
        
        try {
            ObjectName rootObjectName = mom.getObjectName( rootObject ) ;
            String expectedName = 
                "this.test:pp=/Test/Root/Name/FruitBat[Sam],"
                + "type=RootType,name=MyRoot" ;
            ObjectName expectedObjectName = null ;
            try {
                expectedObjectName = new ObjectName(expectedName);
            } catch (MalformedObjectNameException ex) {
                fail( "Could not create ObjectName: ex="  + ex ) ;
            } 
            
            assertEquals( expectedObjectName, rootObjectName ) ;
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                fail( "Exception on close: " + ex ) ;
            }
        }
    }
    
    public void testRootMBean2() throws MalformedObjectNameException {
        System.out.println( "testRootMBean2" ) ;
        final int value = 42 ;
        final String rootName = "MyRoot" ;
        final Object rootObject = new NamedRootObject( rootName, value ) ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createFederated(
            new ObjectName( ROOT_PARENT_NAME ) ) ;
        // mom.setRegistrationDebug(
            // ManagedObjectManager.RegistrationDebugLevel.NORMAL ) ;
        mom.stripPrefix("org.glassfish.gmbal");
        mom.createRoot( rootObject ) ;
        
        try {
            ObjectName rootObjectName = mom.getObjectName( rootObject ) ;
            String expectedName = 
                "this.test:pp=/Test/Root/Name/FruitBat[Sam],"
                + "type=RootType,name=MyRoot" ;
            ObjectName expectedObjectName = null ;
            try {
                expectedObjectName = new ObjectName(expectedName);
            } catch (MalformedObjectNameException ex) {
                fail( "Could not create ObjectName: ex=" + ex ) ;
            } 
            
            assertEquals( expectedObjectName, rootObjectName ) ;
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                fail( "Exception on close: " + ex ) ;
            }
        }
    }
    
    @ManagedData
    @Description( "A simple representation of ab address in America" ) 
    public static class Address {
        private int number ;
        private String street ;
        private String city ;
        private String state ;
        private int zipCode ;
        
        public Address( int number, String street, String city, String state,
            int zipCode ) {
            this.number = number ;
            this.street = street ;
            this.city = city ;
            this.state = state ;
            this.zipCode = zipCode ;
        }
        
        @ManagedAttribute
        @Description( "street number" )
        public int number() {
            return number ;
        }
        
        @ManagedAttribute
        @Description( "street nzme" )
        public String street() {
            return street ;
        }

        @ManagedAttribute
        @Description( "city nzme" )
        public String city() {
            return city ;
        }
        
        @ManagedAttribute
        @Description( "state nzme" )
        public String state() {
            return state ;
        }
        
        @ManagedAttribute
        @Description( "zip code")
        public int zipCode() {
            return zipCode ;
        }
    }
    
    private static final Address testAddress = new Address(
        1234, "maple street", "anywhere", "anystate", 99999 ) ;
    
    @ManagedData
    @Description( "A simple representation of a person in America" ) 
    public static class Person {
        private String fn ;
        private String ln ;
        private Address addr ;
        
        public Person( String firstName, String lastName, Address address ) {
            fn = firstName ;
            ln = lastName ;
            addr = address ;
        }
        
        @ManagedAttribute
        @Description( "first name" ) 
        public String firstName() {
            return fn ;
        }
        
        @ManagedAttribute
        @Description( "last name" )
        public String lastName() {
            return ln ;
        }
        
        @ManagedAttribute
        @Description( "address" )
        public Address address() {
            return addr ;
        }
    }
    
    private static final Person testPerson = new Person( "some", "one", 
        testAddress ) ;
    
    private static final String NMD_TYPE = "NestedManagedDataTest" ;
    
    @ManagedObject
    @AMXMetadata( type=NMD_TYPE, isSingleton=true )
    @Description( "Nested Managed Data test")
    public static class NestedManagedDataTest {
        Person person ;
        
        public NestedManagedDataTest( Person person ) {
            this.person = person ;
        }
        
        @ManagedAttribute
        @Description( "a person" )
        public Person getPerson() {
            return person ;
        }
    }
    
    private static final NestedManagedDataTest nmdt = new NestedManagedDataTest(
        testPerson ) ;
    
    public void testNestedManagedData() throws AttributeNotFoundException, 
        MBeanException, ReflectionException, InstanceNotFoundException, IntrospectionException {
        System.out.println( "testNestedManagedData" ) ;
        
        ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            ROOT_DOMAIN ) ;
        mom.stripPrefix("org.glassfish.gmbal");
        mom.createRoot() ;
        
        try {
            mom.registerAtRoot( nmdt ) ;
            ObjectName oname = mom.getObjectName( nmdt ) ;
            String expectedName = 
                "this.test:pp=/gmbal-root,type=NestedManagedDataTest" ;
            ObjectName expectedObjectName = null ;
            try {
                expectedObjectName = new ObjectName(expectedName);
            } catch (MalformedObjectNameException ex) {
                fail( "Could not create ObjectName: ex=" + ex ) ;
            } 
            
            assertEquals( expectedObjectName, oname ) ;
            
            MBeanServer server = mom.getMBeanServer() ;
            // MBeanInfo mbi = server.getMBeanInfo( oname ) ;
            // System.out.println( mbi ) ;
            
            CompositeData person = (CompositeData) server.getAttribute( oname, 
                "Person" ) ;
            assertEquals( person.get( "firstName" ), testPerson.firstName()) ;
            assertEquals( person.get( "lastName" ), testPerson.lastName() ) ;
            CompositeData address = (CompositeData) person.get("address" ) ;
            assertEquals( address.get( "number"), testAddress.number() ) ;
            assertEquals( address.get( "street"), testAddress.street() ) ;
            assertEquals( address.get( "city"), testAddress.city() ) ;
            assertEquals( address.get( "state"), testAddress.state() ) ;
            assertEquals( address.get( "zipCode"), testAddress.zipCode() ) ;
        } finally {
            try {
                mom.close();
            } catch (IOException ex) {
                fail( "Exception on close: " + ex ) ;
            }
        }    
    }

    public static class MOMSequenceTester {
        final ManagedObjectManager mom =
            ManagedObjectManagerFactory.createStandalone( "test" ) ;
        final MBeanServer mbs =
            mom.getMBeanServer() ;

        public abstract static class MethodTest implements Runnable {
            private final String name ;
            private final boolean before ;
            private final boolean after ;

            public MethodTest( String name, boolean before, boolean after ) {
                this.name = name ;
                this.before = before ;
                this.after = after ;
            }

            public MethodTest( String name ) {
                this( name, false, false ) ;
            }

            @Override
            public String toString() {
                return name ;
            }

            public boolean normalBefore() {
                return before ;
            }

            public boolean normalAfter() {
                return after ;
            }
        }

        private boolean expectedNormalCompletion( final MethodTest mt ) {
            if (mom.getRoot() == null)
                return mt.normalBefore();
            else
                return mt.normalAfter() ;
        }

        private void error( final MethodTest mt, final Throwable thr ) {
            String location ;
            if (mom.getRoot() == null) {
                location = "BEFORE" ;
            } else {
                location = "AFTER" ;
            }

            if (thr == null) {
                fail( "got unexpected normal completion for " + mt + " "
                    + location + " mom.createRoot was called." ) ;
            } else {
                fail( "got unexpected exception " + thr + " for " + mt + " "
                    + location + " mom.createRoot was called." ) ;
            }
        }

        private void testMethod( final MethodTest mt ) {
            final boolean shouldComplete = expectedNormalCompletion( mt ) ;
            try {
                mt.run() ;
                if (!shouldComplete) {
                    error( mt, null ) ;
                }
            } catch (IllegalStateException exc) {
                if (shouldComplete) {
                    error( mt, exc ) ;
                }
            } catch (Throwable thr) {
                error( mt, thr ) ;
            }
        }

        @ManagedObject 
        public static class Bean {
            @ManagedAttribute
            private final int id ;

            public Bean( int id ) {
                this.id = id ;
            }
        }

        private Bean bean1 = new Bean(1) ;
        private Bean bean2 = new Bean(2) ;
        private ObjectName oname1 = null ;

        // This contains every MOM method except for some of the overloaded 
        // forms of the registration methods.
        private final MethodTest[] methods = new MethodTest[] {
            new MethodTest( "suspendJMXRegistration", true, true ) {
                public void run() {
                    mom.suspendJMXRegistration() ;
                }
            },
            new MethodTest( "resumeJMXRegistration", true, true ) {
                public void run() {
                    mom.resumeJMXRegistration() ;
                }
            },
            new MethodTest( "getRoot", true, true ) {
                public void run() {
                    mom.getRoot() ;
                }
            },
            new MethodTest( "registetAtRoot", false, true ) {
                public void run() {
                    mom.registerAtRoot( bean1, "Bean-1" ) ;
                }
            },
            new MethodTest( "register", false, true ) {
                public void run() {
                    mom.register( bean1, bean2, "Bean-1" ) ;
                }
            },
            new MethodTest( "dumpSkeleton", true, true ) {
                public void run() {
                    mom.dumpSkeleton( bean1 ) ;
                }
            },
            new MethodTest( "getObjectName", false, true ) {
                public void run() {
                    oname1 = mom.getObjectName( bean1 ) ;
                }
            },
            new MethodTest( "getObject", false, true ) {
                public void run() {
                    assertEquals( bean1, mom.getObject( oname1 ) ) ;
                }
            },
            new MethodTest( "stripPrefix", true, false ) {
                public void run() {
                    mom.stripPrefix( "org.glassfish.gmbal" ) ;
                }
            },
            new MethodTest( "stripPackagePrefix", true, false ) {
                public void run() {
                    mom.stripPackagePrefix( ) ;
                }
            },
            new MethodTest( "unregister", false, true ) {
                public void run() {
                    mom.unregister( bean2 ) ;
                    mom.unregister( bean1 ) ;
                }
            },
            new MethodTest( "getMBeanServer", true, true ) {
                public void run() {
                    mom.getMBeanServer() ;
                }
            },
            new MethodTest( "setMBeanServer", true, false ) {
                public void run() {
                    mom.setMBeanServer( mbs ) ;
                }
            },
            new MethodTest( "getResourceBundle", true, true ) {
                public void run() {
                    mom.getResourceBundle() ;
                }
            },
            new MethodTest( "setResourceBundle", true, false ) {
                public void run() {
                    mom.setResourceBundle( null ) ;
                }
            },
            new MethodTest( "addAnnotation", true, false ) {
                public void run() {
                    mom.addAnnotation( this.getClass(), 
                        Bean.class.getAnnotation( ManagedObject.class ) ) ;
                }
            },
            new MethodTest( "setRegistrationDebug", true, true ) {
                public void run() {
                    mom.setRegistrationDebug(
                        ManagedObjectManager.RegistrationDebugLevel.NONE);
                }
            },
            new MethodTest( "setRuntimeDebug", true, true ) {
                public void run() {
                    mom.setRuntimeDebug( false ) ;
                }
            },
            new MethodTest( "setTypelibDebug", true, true ) {
                public void run() {
                    mom.setTypelibDebug( 0 ) ;
                }
            },

            // Must be last in the list!
            new MethodTest( "createRoot", true, false ) {
                public void run() {
                    mom.createRoot() ;
                }
            }
        } ;


        public void doTest() {
            try {
                for (MethodTest mt : methods) {
                    testMethod( mt ) ;
                }

                // Call again, because now the root has been created.
                for (MethodTest mt : methods) {
                    testMethod( mt ) ;
                }
            } finally {
                try {
                    mom.close();
                } catch (IOException ex) {
                    Logger.getLogger(GmbalTest.class.getName()).log(
                        Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void testMethodSequence() {
        System.out.println( "testMethodSequence" ) ;
        Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl" ) ;
        try {
            // Turn off logging for this test, since we expect to see
            // many exceptions.
            // logger.setLevel(Level.OFF) ;
            (new MOMSequenceTester()).doTest() ;
        } finally {
            // logger.setLevel(Level.INFO) ;
        }
    }

    @ManagedObject
    @Description( "" )
    public static class TestClass {
        private final String name ;
        public TestClass( String name ) {
            this.name = name ;
        }

        @ManagedAttribute
        @Description( "" )
        public int id() {
            return 42 ;
        }

        @NameValue
        String theName() {
            return name ;
        }
    }

    private void tryName( ManagedObjectManager mom, String str ) {
        Object obj = new TestClass( str ) ;
        mom.registerAtRoot(obj) ;
        ObjectName oname = mom.getObjectName(obj) ;
        // System.out.println( "\tObjectName is " + oname ) ;
    }

    public void testQuotedName() throws IOException {
        System.out.println( "testQuotedName") ;
	final ManagedObjectManager mom =
           ManagedObjectManagerFactory.createStandalone( "test" ) ;
        mom.stripPackagePrefix() ;
        mom.createRoot() ;

        try {
            tryName( mom, "This:Contains-Some,Inter\"esting=Characters*?" );
            tryName( mom, "A:Simple case" ) ;
            tryName( mom, "{http://example.org}AddNumbersService{http://example.org}AddNumbersPort-2" ) ;

            // mom.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.NORMAL) ;
            Object parent = new TestClass( "This:String=Needs?Quotes" ) ;
            mom.registerAtRoot( parent ) ;
            Object child = new TestClass( "Another[Annoying:String]") ;
            mom.register( parent, child ) ;
            ObjectName oname = mom.getObjectName(child) ;
            // System.out.println( "\tObjectName is " + oname);
            // mom.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.NONE) ;
        } finally {
            mom.close() ;
        }
    }

    public static Test suite() {
        return new TestSuite( GmbalTest.class ) ;
    }

    private static final String[][] DATA1 = new String[][] {
        { "red", "green", "blue", "yellow" },
        { "spring", "fall" }
    } ;

    private static final List<List<String>> DATA2 = 
        new ArrayList<List<String>>() ;

    static {
        for (String[] sa : DATA1) {
            DATA2.add( Arrays.asList( sa )) ;
        }
    }

    @ManagedObject
    @Description( "" ) 
    public static class TestDataTypes {
        @ManagedAttribute
        @Description( "" )
        String[][] foo() {
            return DATA1 ;
        }

        @ManagedAttribute
        @Description( "" )
        List<List<String>> bar() {
            return DATA2 ;
        }
    }

    public void testDataTypes() throws IOException {
        System.out.println( "testDataTypes") ;
        ManagedObjectManager mom =
            ManagedObjectManagerFactory.createStandalone( "test" ) ;
        mom.stripPackagePrefix() ;
        mom.createRoot() ;

        try {
            Object obj = new TestDataTypes() ;
            mom.registerAtRoot( obj, "simple" ) ;
        } finally {
            mom.close() ;
        }
    }

    // From the metro project
    @ManagedData
    public static class WebServiceFeature {
        private String id ;
        private boolean enabled = false;

        public WebServiceFeature( String id, boolean enabled ) {
            this.id = id ;
            this.enabled = enabled ;
        }

        @ManagedAttribute
        public String getID() { return id ; }

        @ManagedAttribute
        public boolean isEnabled() {
            return enabled;
        }
    }

    @ManagedData
    public interface WSFeatureList extends Iterable<WebServiceFeature> {
        boolean isEnabled(Class<? extends WebServiceFeature> feature);

        @ManagedAttribute
        WebServiceFeature[] toArray();
    }

    public static class ListIteratorBase<T> implements Iterable<T> {
        private List<T> contents ;

        public ListIteratorBase( List<T> contents ) {
            this.contents = contents ;
        }

        public Iterator<T> iterator() {
            return contents.iterator() ;
        }
    }

    public static class WSFeatureListImpl 
        extends ListIteratorBase<WebServiceFeature>
        implements WSFeatureList {

        public WSFeatureListImpl( String... ids ) {
            super( Algorithms.map( Arrays.asList(ids),
                new UnaryFunction<String,WebServiceFeature>() {
                    public WebServiceFeature evaluate( String str ) {
                        return new WebServiceFeature( str, true ) ;
                    } } ) ) ;


            ArrayList<WebServiceFeature> features =
                new ArrayList<WebServiceFeature>() ;
            for (String str : ids) {
                features.add( new WebServiceFeature( str, true ) ) ;
            }
        }

        public boolean isEnabled(Class<? extends WebServiceFeature> feature) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public WebServiceFeature[] toArray() {
            int size = 0 ;
            for (WebServiceFeature wsf : this ) {
                size++ ;
            }

            WebServiceFeature[] result = new WebServiceFeature[ size ] ;

            int ctr = 0 ;
            for (WebServiceFeature wsf : this ) {
                result[ ctr++ ] = wsf ;
            }

            return result ;
        }
    }

    private static String[] features = { "First", "Second", "Third" } ;
    private static WSFeatureListImpl wsf = new WSFeatureListImpl( features ) ;

    @ManagedObject
    public static class WSTest {
        @ManagedAttribute
        WSFeatureListImpl features() {
            return wsf ;
        }
    }

    public void testWSTest() throws IOException, AttributeNotFoundException,
        MBeanException, ReflectionException {

        System.out.println( "testWSTest" ) ;
        ManagedObjectManager mom =
            ManagedObjectManagerFactory.createStandalone( "test" ) ;
        mom.stripPackagePrefix() ;
        mom.createRoot() ;

        GmbalMBean mb ;

        try {
            // mom.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.NORMAL) ;
            // mom.setRuntimeDebug( true ) ;

            Object obj = new WSTest() ;
            mb = mom.registerAtRoot( obj, "simple" ) ;
            Object result = mb.getAttribute("features") ;

            // mom.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.NONE) ;
            // mom.setRuntimeDebug( false ) ;

            assertTrue( result instanceof CompositeData ) ;
            CompositeData wsfl = (CompositeData) result ;

            Object o2 = wsfl.get( "toArray" ) ;
            assertTrue( o2 instanceof CompositeData[] ) ;
            CompositeData[] cds = (CompositeData[])o2 ;

            assertEquals( features.length, cds.length ) ;

            Set<String> strresults = new HashSet<String>() ;
            for (CompositeData cd : cds ) {
                Object bres = cd.get( "enabled" ) ;
                assertTrue( bres instanceof Boolean ) ;
                assertTrue( (Boolean)bres ) ;

                Object sres = cd.get( "iD" ) ;
                assertTrue( sres instanceof String ) ;
                strresults.add( (String)sres ) ;
            }

            Set<String> exp = new HashSet<String>( Arrays.asList( features ) ) ;
            assertEquals( exp, strresults ) ;
        } finally {
            mom.close() ;
        }
    }
    
// Lloyd's example
    @ManagedObject
    @Description( "A test MBean for Gmbal" )
    public final static class GmbalMOM {
        private final ManagedObjectManager mMOM;
        private ObjectName childName ;

        public GmbalMOM( /*final MBeanServer server,*/ final ObjectName parent) {
            mMOM = ManagedObjectManagerFactory.createFederated( parent );
            // mMOM.setMBeanServer(server);
            mMOM.stripPackagePrefix();
            mMOM.createRoot();
        }

        public void registerChildren() {
            final Gmbal1 test = new Gmbal1();
            mMOM.registerAtRoot( test );
            childName = mMOM.getObjectName(test);
        }

        public ManagedObjectManager getMOM() {
            return mMOM;
        }

        public ObjectName getChildName() {
            return childName ;
        }
    }

    @ManagedObject
    @Description( "A test MBean for Gmbal" )
    @AMXMetadata( isSingleton=true )
    public final static class Gmbal1
    {
        private volatile String mTest;
        private volatile int    mCount;

        public Gmbal1() {
            mTest = "hello world";
        }

        @ManagedAttribute
        @Description( "test field" )
        public String getTest() {
            return mTest;
        }

        public void setTest(final String test) {
            mTest = test;
        }

        @ManagedAttribute
        @Description( "test field" )
        public int getCount() {
            return mCount;
        }

        public void setCount(final int count) {
            mCount = count;
        }
    }

    public void testLloydExample() throws MalformedObjectNameException {
        System.out.println( "TestLloydExample" ) ;
        ManagedObjectManager rmom = 
            ManagedObjectManagerFactory.createStandalone("root") ;
        rmom.createRoot() ;
        ObjectName rname = rmom.getObjectName(rmom.getRoot()) ;

        GmbalMOM mom = new GmbalMOM(rname) ;
        mom.registerChildren() ;
        ObjectName childName = mom.getChildName() ;
        System.out.println( "\tchildName = " + childName ) ;
        AMXClient client = new AMXClient( mom.getMOM().getMBeanServer(),
            childName ) ;
        System.out.println( "\tclient = " + client ) ;
        AMXMBeanInterface parent = client.getParent() ;
        System.out.println( "\tparent = " + parent ) ;
        AMXMBeanInterface[] children = parent.getChildren() ;
        System.out.println( "\tchildren = " + Arrays.asList( children )) ;
    }

    // An illegal definition
    @ManagedData
    public final static class Foo {
        private String name = "foo" ;

        @Override
        public String toString() {
            return "FooObject" ;
        }

        @ManagedAttribute
        String getName() { return name ; }

        @ManagedAttribute
        String illegalAttribute( String arg ) { return "I'm Illegal!" ; }
    }

    @ManagedObject
    public static class Bar {
        private String name = "bar" ;

        @Override
        public String toString() {
            return "BarObject" ;
        }

        @NameValue
        @ManagedAttribute
        String getName() { return name ; }

        @ManagedAttribute
        Foo getFoo() { return new Foo() ; }
    }

    public void testIllegalAttribute() throws IOException {
        System.out.println( "testIllegalAttribute") ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            "test" ) ;
        Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl" ) ;
        try {
            logger.setLevel(Level.OFF) ;
            mom.createRoot() ;
            Object obj = new Bar() ;
            mom.registerAtRoot( obj ) ;
            fail( "Expected exception not seen") ;
        } catch (IllegalArgumentException exp) {
            // This is expected
        } catch (Throwable thr) {
            fail( "Unexpected exception " + thr ) ;
        } finally {
            logger.setLevel(Level.INFO) ;

            mom.close() ;
        }
    }

    public static class MyDataObject {
        private String name ;
        private int value ;

        public MyDataObject( String name, int value ) {
            this.name = name ;
            this.value = value ;
        }

        public String theName() {
            return name ;
        }

        public int value() {
            return value ;
        }
    }

    @ManagedData
    @InheritedAttributes( {
        @InheritedAttribute( methodName="theName" , description="The name" ),
        @InheritedAttribute( methodName="value" , description="The value" )
    } )
    public interface MyDataObjectDummy {}

    @ManagedObject
    @Description( "Test for inherited attributes on ManagedData")
    public static class MyDataObjectBean {
        private MyDataObject md = new MyDataObject( "Montara", 42 ) ;

        @ManagedAttribute
        MyDataObject getMDO() { return md ; }

        @NameValue
        String theName() {
            return "MyDataObjectBeanTest" ;
        }
    }

    public void testMyDataObject() throws IOException,
        AttributeNotFoundException, MBeanException, ReflectionException {
        System.out.println( "testMyDataObject") ;

        MyDataObjectBean mdob = new MyDataObjectBean() ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            "test" );

        try {
            mom.addAnnotation( MyDataObject.class,
                MyDataObjectDummy.class.getAnnotation(ManagedData.class )) ;
            mom.addAnnotation( MyDataObject.class,
                MyDataObjectDummy.class.getAnnotation(InheritedAttributes.class )) ;
            mom.stripPackagePrefix();
            mom.createRoot() ;

            GmbalMBean res = mom.registerAtRoot( mdob ) ;

            Object obj = res.getAttribute("MDO") ;

            // ObjectName on = mom.getObjectName(mdob) ;
            // System.out.println( "Contents of " + on + ": " + obj ) ;
            assertTrue( obj instanceof CompositeData ) ;

            CompositeData cd = (CompositeData)obj ;
            assertEquals( cd.get( "theName" ), mdob.getMDO().theName() ) ;
            assertEquals( cd.get( "value" ), mdob.getMDO().value() ) ;
        } finally {
            mom.close() ;
        }
    }

    @ManagedObject
    public static class MBeanBase {
        @ManagedAttribute
        public int value() { return 42 ; }
    }

    @AMXMetadata( isSingleton=true )
    public static class SingletonMBean extends MBeanBase {
    }

    public static class NonSingletonMBean extends MBeanBase {
        @NameValue
        String theName() { return "me" ; }
    }

    public static class UnnamedNonSingletonMBean extends MBeanBase {
    }

    private void doRegisterAtRoot( ManagedObjectManager mom, Object obj,
        boolean exceptionExpected ) {

        try {
            mom.registerAtRoot(obj) ;
            mom.unregister(obj);

            assertTrue( "Unexpected successful completion", !exceptionExpected ) ;
        } catch (IllegalArgumentException exc) {
            assertTrue( "Unexpected exception " + exc, exceptionExpected ) ;
        } catch (Throwable thr) {
            fail( "Unexpected exception " + thr ) ;
        }
    }

    private void doRegisterAtRoot( ManagedObjectManager mom, Object obj,
        String name, boolean exceptionExpected ) {

        try {
            mom.registerAtRoot(obj, name) ;
            mom.unregister(obj);

            assertTrue( "Unexpected successful completion", !exceptionExpected ) ;
        } catch (IllegalArgumentException exc) {
            assertTrue( "Unexpected exception " + exc, exceptionExpected ) ;
        } catch (Throwable thr) {
            fail( "Unexpected exception " + thr ) ;
        }
    }

    public void testSingletonMBean() throws IOException {
        System.out.println( "testSingletonMBean") ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            "test" );
        Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl" ) ;

        SingletonMBean smb = new SingletonMBean() ;
        NonSingletonMBean nsmb = new NonSingletonMBean() ;
        UnnamedNonSingletonMBean unsmb = new UnnamedNonSingletonMBean() ;

        try {
            logger.setLevel(Level.OFF );
            mom.stripPackagePrefix();
            mom.createRoot() ;

            // just test registerAtRoot variants
            doRegisterAtRoot( mom, smb, false ) ;
            doRegisterAtRoot( mom, nsmb, false ) ;
            doRegisterAtRoot( mom, unsmb, true ) ;
            doRegisterAtRoot( mom, smb, "foo", true ) ;
            doRegisterAtRoot( mom, nsmb, "foo", false ) ;
            doRegisterAtRoot( mom, unsmb, "foo", false ) ;
        } finally {
            logger.setLevel(Level.INFO );
            mom.close() ;
        }
    }

    @ManagedObject 
    @Description( "Test for @ManagedAttribute field")
    public static class TestFieldAttribute {
        @NameValue String theName() { return "name" ; }

        @ManagedAttribute
        private final int value ;

        TestFieldAttribute() {
            value = 42 ;
        }
    }

    public void testFieldAttribute() throws IOException,
        AttributeNotFoundException, MBeanException, ReflectionException {
        System.out.println( "testFieldAttribute") ;

        TestFieldAttribute tobj = new TestFieldAttribute() ;

        ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            "test" );

        try {
            mom.stripPackagePrefix();
            mom.createRoot() ;
            GmbalMBean mb = mom.registerAtRoot( tobj ) ;

            assertEquals( tobj.value, mb.getAttribute( "value" )) ;
        } finally {
            mom.close() ;
        }
    }

    @ManagedObject
    @Description( "Test MBean for quotation problems") 
    @AMXMetadata( type="test:bean")
    public static class QuoteTestBean {
        private String name ;

        public QuoteTestBean( String name ) {
            this.name = name ;
        }

        @ManagedAttribute
        @Description( "simple value")
        int getNumber() { return 42 ; }

        @NameValue
        String theName() {
            return name ;
        }
    }

    public void testQuotes() throws IOException {
        System.out.println( "testQuotes" ) ;
        ManagedObjectManager mom = null ;

        QuoteTestBean root = new QuoteTestBean("needs=quote") ;
        QuoteTestBean child1 = new QuoteTestBean("noquote") ;
        QuoteTestBean child2 = new QuoteTestBean("child=needs:quote") ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot( root ) ;
            ObjectName rootName = mom.getObjectName(root) ;
            // System.out.println( "\trootName=" + rootName ) ;

            mom.registerAtRoot( child1 ) ;
            ObjectName child1Name = mom.getObjectName(child1) ;
            // System.out.println( "\tchild1Name=" + child1Name ) ;

            mom.registerAtRoot( child2 ) ;
            ObjectName child2Name = mom.getObjectName(child2) ;
            // System.out.println( "\tchild2Name=" + child2Name ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    private static final long BRTEST_CURRENT    = 27 ;
    private static final long BRTEST_HWM        = 36 ;
    private static final long BRTEST_LWM        = 11 ;
    private static final long BRTEST_TOP        = 100 ;
    private static final long BRTEST_BOTTOM     = 0 ;
    private static final String BRTEST_NAME     = "TestAttr" ;
    private static final String BRTEST_UNITS    = "Barns" ;
    private static final String BRTEST_DESC     = "A silly stat" ;
    private static final long BRTEST_START_TIME = System.currentTimeMillis() ;
    private static final long BRTEST_SAMPLE_TIME= BRTEST_START_TIME + 237 ;

    private static final List<Pair<String,Object>> BRTEST_DATA = Algorithms.list(
        Algorithms.pair( "current", (Object)BRTEST_CURRENT ),
        Algorithms.pair( "highWaterMark", (Object)BRTEST_HWM ),
        Algorithms.pair( "lowWaterMark", (Object)BRTEST_LWM ),
        Algorithms.pair( "upperBound", (Object)BRTEST_TOP ),
        Algorithms.pair( "lowerBound", (Object)BRTEST_BOTTOM ),
        Algorithms.pair( "name", (Object)BRTEST_NAME ),
        Algorithms.pair( "unit", (Object)BRTEST_UNITS ),
        Algorithms.pair( "description", (Object)BRTEST_DESC ),
        Algorithms.pair( "startTime", (Object)BRTEST_START_TIME ),
        Algorithms.pair( "lastSampleTime", (Object)BRTEST_SAMPLE_TIME ) )  ;

    // Test the BoundedRangeStatistic
    @ManagedObject
    @Description( "BoundedRangeStatistic test")
    public static class BRMBean {
        private final BoundedRangeStatistic brstat =
            new BoundedRangeStatisticImpl( BRTEST_CURRENT, BRTEST_HWM,
                BRTEST_LWM, BRTEST_TOP, BRTEST_BOTTOM, BRTEST_NAME,
                BRTEST_UNITS, BRTEST_DESC, BRTEST_START_TIME,
                BRTEST_SAMPLE_TIME ) ;

        @ManagedAttribute
        @Description( "The stat attr")
        BoundedRangeStatistic getStat() {
            return brstat ;
        }

        @NameValue
        String theName() { return "TestBean" ; }
    }

    public void testBoundedRangeStatistic() throws IOException,
        AttributeNotFoundException, MBeanException, ReflectionException {
        System.out.println( "testBoundedRangeStatistic" ) ;
        ManagedObjectManager mom = null ;

        BRMBean root = new BRMBean() ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            GmbalMBean gmb = mom.createRoot( root ) ;
            ObjectName rootName = mom.getObjectName(root) ;
            // System.out.println( "\trootName=" + rootName ) ;

            Object data = gmb.getAttribute( "Stat" ) ;
            assertTrue( data instanceof CompositeData) ;
            CompositeData brdata = (CompositeData)data ;

            for (Pair<String,Object> elem : BRTEST_DATA) {
                String aname = elem.first() ;
                Object exp = elem.second() ;
                Object act = brdata.get( aname ) ;
                assertEquals( aname + " has unexpected value", act, exp ) ;
            }
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }

    }

    public void testSuppressDuplicateRoot() {
        System.out.println( "testSuppressDuplicateRoot" ) ;

        final ManagedObjectManager mom1 = 
            ManagedObjectManagerFactory.createStandalone("test") ;
        final Object root1 = new RootObject(1) ;
        mom1.stripPackagePrefix();

        final ManagedObjectManager mom2 = 
            ManagedObjectManagerFactory.createStandalone("test") ;
        final Object root2 = new RootObject(2) ;
        mom2.stripPackagePrefix();

        mom1.createRoot( root1, "A" ) ;

        // suppress true
        mom2.suppressDuplicateRootReport(true) ;
        GmbalMBean gmb = mom2.createRoot( root2, "A" ) ;
        assertEquals( null, gmb ) ;

        // suppress false 
        mom2.suppressDuplicateRootReport(false);
        try {
            mom2.createRoot( root2, "A" ) ;
            fail( "Unexpected successful completion") ;
        } catch (IllegalArgumentException exc) {
            // this is the expected result
        } catch (Exception exc) {
           fail( "Unexpected exception: " + exc ) ;
        }
    }

    @ManagedObject
    @Description( "This is a boolean attribute test")
    private static class BooleanAttributeTest {
        private String name ;

        @NameValue
        public String name() { return name ; }

        private boolean flag ;

        public BooleanAttributeTest( final String name ) {
            this.name = name ;
            flag = false ;
        }

        @ManagedAttribute
        public boolean getFlag() { return flag ; }

        @ManagedAttribute
        public void setFlag( final boolean flag ) { this.flag = flag ; }
    }

    public void testBooleanAttribute() throws IOException {
        System.out.println( "testBooleanAttribute" ) ;

        BooleanAttributeTest bat = new BooleanAttributeTest( "FOO" ) ;
        ManagedObjectManager mom = null ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            GmbalMBean gmb = mom.createRoot( bat ) ;
            ObjectName rootName = mom.getObjectName(bat) ;
            // System.out.println( "\trootName=" + rootName ) ;
            AMXClient amxc = mom.getAMXClient( bat ) ;

            assertEquals( Boolean.FALSE, amxc.getAttribute( "Flag") ) ;
            amxc.setAttribute( "Flag", Boolean.TRUE );
            assertEquals( Boolean.TRUE, amxc.getAttribute( "Flag") ) ;
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    @ManagedData
    @Description( "Some test data for arrays of primitives")
    private static class ContainsPrimitives {
        private static final boolean[] booleanArray = { true, false, true, false } ;
        private static final byte[] byteArray = { 1, 2, 3, 4 } ;
        private static final short[] shortArray = { 1, 2, 3, 4 } ;
        private static final int[] intArray = { 1, 2, 3, 4 } ;
        private static final char[] charArray = { 'A', 'B', 'C', 'D' } ;
        private static final long[] longArray = { 1, 2, 3, 4 } ;
        private static final float[] floatArray = { 1.0F, 2.0F, 3.0F, 4.0F } ;
        private static final double[] doubleArray = { 1.0, 2.0, 3.0, 4.0 } ;

        @ManagedAttribute
        boolean[] getBooleanArray() { return booleanArray ; }

        @ManagedAttribute
        byte[] getByteArray() { return byteArray ; }

        @ManagedAttribute
        short[] getShortArray() { return shortArray ; }

        @ManagedAttribute
        int[] getIntArray() { return intArray ; }

        @ManagedAttribute
        char[] getCharArray() { return charArray ; }

        @ManagedAttribute
        long[] getLongArray() { return longArray ; }

        @ManagedAttribute
        float[] getFloatArray() { return floatArray ; }

        @ManagedAttribute
        double[] getDoubleArray() { return doubleArray ; }

        public static final Set<String> ATTRIBUTES = new HashSet<String>(
            Arrays.asList( "booleanArray", "byteArray", "shortArray",
                "intArray", "charArray", "longArray", "floatArray",
                "doubleArray" ) ) ;

        public static final Map<String,Object[]> RESULTS = new
            HashMap<String,Object[]>() ;

        static {
            RESULTS.put( "booleanArray", convert( booleanArray )) ;
            RESULTS.put( "byteArray", convert( byteArray )) ;
            RESULTS.put( "charArray", convert( charArray )) ;
            RESULTS.put( "shortArray", convert( shortArray )) ;
            RESULTS.put( "intArray", convert( intArray )) ;
            RESULTS.put( "longArray", convert( longArray )) ;
            RESULTS.put( "floatArray", convert( floatArray )) ;
            RESULTS.put( "doubleArray", convert( doubleArray )) ;
        }
    }

    static Object[] convert( Object obj ) {
        Class<?> cls = obj.getClass() ;
        if (cls.isArray()) {
            Class<?> ccls = cls.getComponentType() ;
            if (ccls.isPrimitive()) {
                int len = Array.getLength(obj) ;
                Object[] result = new Object[len] ;
                for (int ctr=0; ctr<len; ctr++) {
                    Object elem = Array.get( obj, ctr ) ;
                    Array.set( result, ctr, elem ) ;
                }
                return result ;
            } else {
                return (Object[])obj ;
            }
        } else {
            return new Object[] { obj } ;
        }
    }

    @ManagedObject
    @Description( "Test for ContainsPrimtives")
    private static class ContainsPrimitivesBean {
        ContainsPrimitives cp = new ContainsPrimitives() ;

        @NameValue
        public String myName() { return "ContainsPrimitiveBean" ; }

        @ManagedAttribute
        public ContainsPrimitives getArrays() { return cp ; }
    }

    public void testContainsPrimitives() throws IOException {
        System.out.println( "testContainsPrimitives" ) ;

        ContainsPrimitivesBean cpb = new ContainsPrimitivesBean() ;
        ManagedObjectManager mom = null ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            GmbalMBean gmb = mom.createRoot( cpb ) ;
            ObjectName rootName = mom.getObjectName(cpb) ;
            // System.out.println( "\trootName=" + rootName ) ;
            AMXClient amxc = mom.getAMXClient( cpb ) ;

            Object result = amxc.getAttribute( "Arrays") ;
            assertTrue("Result is not a CompositeData, it is " + result,
                result instanceof CompositeData ) ;
            CompositeData compres = (CompositeData)result ;
            CompositeType ctype = compres.getCompositeType() ;
            Set<String> keys = ctype.keySet() ;
            assertEquals( ContainsPrimitives.ATTRIBUTES, keys ) ;

            for (String key : keys) {
                Object attr = compres.get(key) ;
                Object[] expected = ContainsPrimitives.RESULTS.get( key ) ;
                assertTrue( Arrays.deepEquals( expected, (Object[])attr )) ;
            }
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    @ManagedObject
    @Description( "Test for TabularData" )
    public static class TabularDataBean {
        public Set<String> keys ;
        public Map<String,Integer> map ;
        public Dictionary<String,Integer> dictionary ;

        @NameValue String myName() { return "TabularDataBean" ; }

        public TabularDataBean() {
            keys = new HashSet<String>( Arrays.asList( "red",
                "orange", "yellow", "green", "blue" , "indigo", "violet" ) ) ;
            map = new HashMap<String,Integer>() ;
            for (String key : keys) {
                map.put( key, key.length()) ;
            }
            dictionary = new Hashtable<String,Integer>( map ) ;
        }

        @ManagedAttribute
        @Description( "Value as a Map")
        Map<String,Integer> getMap() {
            return map ;
        }

        @ManagedAttribute
        @Description( "Value as a Dictionary")
        Dictionary<String,Integer> getDictionary() {
            return dictionary ;
        }
    }

    private static final List<String> ttNames = Arrays.asList( "key" ) ;
    private static final Set<String> ttAll = new HashSet<String>(
            Arrays.asList("key", "value") ) ;

    public void testTabularDataBean() throws IOException {
        System.out.println( "testTabularDataBean" ) ;

        TabularDataBean tdb = new TabularDataBean() ;
        ManagedObjectManager mom = null ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot( tdb ) ;
            ObjectName rootName = mom.getObjectName(tdb) ;
            System.out.println( "\trootName=" + rootName ) ;
            AMXClient amxc = mom.getAMXClient( tdb ) ;

            checkTabularContents( amxc, tdb, "Map" ) ;
            checkTabularContents( amxc, tdb, "Dictionary" ) ;
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    private void checkTabularContents( AMXClient amxc, 
        TabularDataBean tdb, String attrName ) {
        Object result = amxc.getAttribute( attrName ) ;
        assertTrue("Result is not a TabularData, it is " + result,
            result instanceof TabularData ) ;
        TabularData tres = (TabularData)result ;
        TabularType ttype = tres.getTabularType() ;
        List<String> indexNames = ttype.getIndexNames();
        assertEquals( ttNames, indexNames ) ;

        CompositeType ctype = ttype.getRowType() ;
        assertEquals( ttAll, ctype.keySet() ) ;

        Map<String,Integer> contents = new HashMap<String, Integer>() ;
        for (Object row : tres.keySet()) {
            List<?> lrow = (List<?>)row ;
            Object[] key = lrow.toArray(new Object[lrow.size()]) ;
            CompositeData cd = tres.get( key ) ;

            Object val = cd.get( "key" ) ;
            assertTrue( val instanceof String ) ;
            String cdkey = (String)val ;

            val = cd.get( "value" ) ;
            assertTrue( val instanceof Integer ) ;
            Integer cdval = (Integer)val ;

            contents.put( cdkey, cdval ) ;
        }

        assertEquals( tdb.map, contents ) ;
    }

    enum Color { RED, BLUE, GREEN }

    @ManagedObject
    @Description( "Test for Enums" )
    public static class EnumBean {
        private Color color = null ;

        @NameValue String myName() { return "EnumBean" ; }

        @ManagedAttribute
        @Description( "Enum value")
        Color getColor() { return color ; }

        void setColor( Color color ) { this.color = color ; }
    }

    public void testEnumBean() throws IOException {
        System.out.println( "testEnumBean" ) ;

        EnumBean tdb = new EnumBean() ;
        ManagedObjectManager mom = null ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot( tdb ) ;
            ObjectName rootName = mom.getObjectName(tdb) ;
            System.out.println( "\trootName=" + rootName ) ;
            AMXClient amxc = mom.getAMXClient( tdb ) ;

            Object value = amxc.getAttribute( "Color" ) ;
            assertEquals( TypeConverterImpl.NULL_STRING, value ) ;

            Color color = Color.BLUE ;
            tdb.setColor( color ) ;
            value = amxc.getAttribute( "Color" ) ;
            assertEquals( color.toString(), value ) ;
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    @ManagedObject
    public interface hasMO{}

    @ManagedObject
    public static class AMO {}

    public static class InhMO implements hasMO {}

    public void testIsManagedObject() throws IOException {
        System.out.println( "testEnumBean" ) ;

        ManagedObjectManager mom = null ;
        Object obj = new Object() ;
        Object mo = new AMO() ;
        Object imo = new InhMO() ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();

            assertTrue( !mom.isManagedObject(obj)) ;
            assertTrue( mom.isManagedObject(mo));
            assertTrue( mom.isManagedObject(imo));

            mom.createRoot() ;
            
            assertTrue( !mom.isManagedObject(obj)) ;
            assertTrue( mom.isManagedObject(mo));
            assertTrue( mom.isManagedObject(imo));
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    @ManagedObject
    public static class MultiTestClass {
        private ManagedObjectManager mom ;
        private String myName ;

        public MultiTestClass( final ManagedObjectManager mom,
            final String name ) {
            
            this.mom = mom ;
            myName = name ;
        }

        public String toString() {
            return "MultiTestClass[" + myName + "]" ;
        }

        @NameValue
        public String myString() { return myName ; }

        public void testRegister( boolean shouldComplete ) {
            try {
                mom.registerAtRoot( this );
                if (!shouldComplete) {
                    fail( "Register should not succeed for " + this ) ;
                }
            } catch (IllegalArgumentException exc) {
                if (shouldComplete) {
                    fail( "Register should not throw exception "
                        + exc + " for " + this ) ;
                }
            } catch (Throwable thr) {
                fail( "Unexpected exception " + thr
                    + " for register of " + this ) ;
            }
        }

        public void testUnregister( boolean shouldComplete ) {
            try {
                mom.unregister( this );
                if (!shouldComplete) {
                    fail( "Unregister should not succeed for " + this ) ;
                }
            } catch (IllegalArgumentException exc) {
                if (shouldComplete) {
                    fail( "Unregister should not throw exception "
                        + exc + " for " + this ) ;
                }
            } catch (Throwable thr) {
                fail( "Unexpected exception " + thr
                    + " for unregister of " + this ) ;
            }
        }
    }

    public void testMultiRegistration() throws IOException {
        System.out.println( "testMultiRegistration" ) ;

        ManagedObjectManager mom = null ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot() ;
            MultiTestClass obj1 = new MultiTestClass( mom, "1" ) ;
            MultiTestClass obj1too = new MultiTestClass( mom, "1" ) ;
            MultiTestClass obj2 = new MultiTestClass( mom, "2" ) ;

            obj1.testRegister( true ) ;
            obj1.testRegister( false ) ;
            obj1too.testRegister( false ) ;
            obj2.testRegister( true ) ;

            obj1.testUnregister( true ) ;
            obj1.testUnregister( false ) ;
            obj1too.testUnregister( false ) ;
            obj2.testUnregister( true ) ;

            obj1.testRegister( true ) ;
            obj1.testRegister( false ) ;
            obj1too.testRegister( false ) ;
            obj2.testRegister( true ) ;
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    // Note that we require RUNTIME retentation, or Gmbal can't see the
    // annotation.  Also, if the interface is not public, we get access
    // errors.
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE } )
    public @interface Climate {
        @DescriptorKey("rainfall")
        double rainfall() default 0.0 ;
        
        @DescriptorKey("highTemp")
        int highTemp() default 40 ;
        
        @DescriptorKey("lowTemp")
        int lowTemp() default 0 ;
    }

    @Climate( rainfall=10.0, highTemp = 30, lowTemp = 0 )
    @AMXMetadata( group="other", isSingleton=true, type="TestTheMetaData")
    @ManagedObject
    public static class TestMDBean {
        @ManagedAttribute
        String data() { return "blue" ; }
    }

    String getString( Object obj ) {
        if (obj instanceof byte[])
            return Arrays.toString( (byte[])obj ) ;
        else if (obj instanceof boolean[])
            return Arrays.toString( (boolean[])obj ) ;
        else if (obj instanceof short[])
            return Arrays.toString( (short[])obj ) ;
        else if (obj instanceof char[])
            return Arrays.toString( (char[])obj ) ;
        else if (obj instanceof int[])
            return Arrays.toString( (int[])obj ) ;
        else if (obj instanceof long[])
            return Arrays.toString( (long[])obj ) ;
        else if (obj instanceof float[])
            return Arrays.toString( (float[])obj ) ;
        else if (obj instanceof double[])
            return Arrays.toString( (double[])obj ) ;
        else if (obj instanceof Object[])
            return Arrays.toString( (Object[])obj ) ;
        else
            return obj.toString() ;
    }

    public void msg( String str ) {
        if (DEBUG) {
            System.out.println( str ) ;
        }
    }

    public void dumpMBeanInfo( MBeanInfo mbi ) {
        msg( "\tclassName:     " + mbi.getClassName() ) ;
        msg( "\tattributes:    " + getString( mbi.getAttributes() ) ) ;
        msg( "\tconstructors:  " + getString( mbi.getConstructors() ) ) ;
        msg( "\tdescription:   " + getString( mbi.getDescription() ) ) ;
        msg( "\tdescriptor:    " ) ;
        /* this only works on JDK 6!
        Descriptor desc = mbi.getDescriptor() ;
        for (String str : desc.getFieldNames()) {
            Object value = desc.getFieldValue(str) ;
            msg( "\t\tdesc[" + str + "]=" + getString( value ) ) ;
        }
         */
        msg( "\tnotifications: " + getString( mbi.getNotifications() ) ) ;
        msg( "\toperations:    " + getString( mbi.getOperations() ) ) ;
    
        if (mbi instanceof ModelMBeanInfo) {
            ModelMBeanInfo mmbi = (ModelMBeanInfo)mbi ;
            try {
                Descriptor[] mdesc = mmbi.getDescriptors("mbean");
                msg( "\tDescriptors from ModelMBeanInfo:") ;
                int ctr = 0 ;
                for (Descriptor desc : mdesc) {
                    msg( "\tDescriptor " + ctr++ ) ;
                    String[] keys = desc.getFieldNames() ;
                    for (String key : keys) {
                        Object value = desc.getFieldValue(key) ;
                        msg( "\t\t" + key + " -> " + value ) ;
                    }
                }
            } catch (MBeanException ex) {
                Logger.getLogger(GmbalTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RuntimeOperationsException ex) {
                Logger.getLogger(GmbalTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void testDumpMetadata() throws IOException {
        System.out.println( "testDumpMetadata" ) ;

        ManagedObjectManager mom = null ;
        Object obj = new TestMDBean() ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot() ;
            GmbalMBean mb = mom.registerAtRoot( obj ) ;
            AMXClient amxc = mom.getAMXClient( obj ) ;


            msg( "MBeanInfo: " ) ;
            dumpMBeanInfo( amxc.getMBeanInfo() ) ;
            msg( "getMeta: " ) ;
            for (Map.Entry<String,?> entry : amxc.getMeta().entrySet()) {
                msg( "\t" + entry.getKey()
                    + " => " + getString( entry.getValue() ) ) ;
            }
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    @ManagedObject
    @AMXMetadata( isSingleton=true )
    public interface InhMeta {
        @ManagedAttribute
        String myAttribute() ;
    }

    public static class InhMetaImpl implements InhMeta {
        private String myAttr ;

        public InhMetaImpl( String str ) {
            myAttr = str ;
        }

        public String myAttribute() {
            return myAttr ;
        }
    }

    public void testInhMeta() throws IOException, MBeanException {
        System.out.println( "testInhMeta" ) ;

        final String data = "AttributeValue" ;

        ManagedObjectManager mom = null ;
        Object obj = new InhMetaImpl( data ) ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot() ;

            GmbalMBean mb = mom.registerAtRoot( obj ) ;
            AMXClient amxc = mom.getAMXClient( obj ) ;

            msg( "MBeanInfo: " ) ;
            dumpMBeanInfo( amxc.getMBeanInfo() ) ;
            msg( "getMeta: " ) ;
            for (Map.Entry<String,?> entry : amxc.getMeta().entrySet()) {
                msg( "\t" + entry.getKey()
                    + " => " + getString( entry.getValue() ) ) ;
            }

            ModelMBeanInfo mmbi = (ModelMBeanInfo)amxc.getMBeanInfo() ;
            Descriptor desc = mmbi.getMBeanDescriptor() ;
            boolean isSingleton = (Boolean)(desc.getFieldValue(
                AMX.DESC_IS_SINGLETON )) ;
            assertTrue( isSingleton ) ;
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    // test for invalid attributes
    private static final String BAD_ATTR_1 = "b234567" ;
    private static final String BAD_ATTR_2 = "b123456" ;

    @ManagedObject
    @AMXMetadata( isSingleton=true )
    private static class BadAttrBean {
        private int id ;

        public BadAttrBean( int id ) {
            this.id = id ;
        }

        @ManagedAttribute
        int id() { return id ; }
    }

    public void testBadAttr() throws IOException, MBeanException {
        System.out.println( "testBadAttr" ) ;

        final int data = 27 ;

        ManagedObjectManager mom = null ;
        Object obj = new BadAttrBean( data ) ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot() ;

            GmbalMBean mb = mom.registerAtRoot( obj ) ;
            AMXClient amxc = mom.getAMXClient( obj ) ;

            try {
                amxc.getAttribute( BAD_ATTR_1 ) ;
                fail( "Exception expected" ) ;
            } catch (GmbalException exc) {
                msg( "caught exception " + exc ) ;
            } catch (Exception exc) {
                fail( "caught unexpected exception " + exc ) ;
            }

            try {
                amxc.getAttribute( BAD_ATTR_1 ) ;
                fail( "Exception expected" ) ;
            } catch (GmbalException exc) {
                msg( "caught exception " + exc ) ;
            } catch (Exception exc) {
                fail( "caught unexpected exception " + exc ) ;
            }
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }

    public void testBadAttrs() throws IOException, MBeanException {
        System.out.println( "testBadAttrs" ) ;

        final int data = 27 ;

        ManagedObjectManager mom = null ;
        Object obj = new BadAttrBean( data ) ;

        try {
            mom = ManagedObjectManagerFactory.createStandalone("test") ;
            mom.stripPackagePrefix();
            mom.createRoot() ;

            GmbalMBean mb = mom.registerAtRoot( obj ) ;
            AMXClient amxc = mom.getAMXClient( obj ) ;

            try {
                String[] attrs = { BAD_ATTR_1, BAD_ATTR_2, "id" } ;

                AttributeList alist = amxc.getAttributes( attrs ) ;

                AttributeList expList = new AttributeList() ;
                Attribute attr = new Attribute( "id", data ) ;
                expList.add( attr ) ;

                assertEquals( expList, alist ) ;
            } catch (Exception exc) {
                fail( "caught unexpected exception " + exc ) ;
            }

            try {
                amxc.getAttribute( BAD_ATTR_1 ) ;
                fail( "Exception expected" ) ;
            } catch (GmbalException exc) {
                msg( "caught exception " + exc ) ;
            } catch (Exception exc) {
                fail( "caught unexpected exception " + exc ) ;
            }
        } catch (GmbalException exc) {
            fail( "Exception: " + exc ) ;
        } finally {
            if (mom != null) {
                mom.close() ;
            }
        }
    }
}
