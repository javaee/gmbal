/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.gmbal ;

import java.io.IOException;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;


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

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException ;
import javax.management.ObjectName ;
import javax.management.MBeanServer ;
import javax.management.ReflectionException;
import javax.management.openmbean.SimpleType ;
import javax.management.openmbean.OpenType ;
import javax.management.openmbean.CompositeData ;
import javax.management.openmbean.CompositeType ;

import org.glassfish.gmbal.generic.UnaryFunction ;
import org.glassfish.gmbal.generic.Predicate ;
import org.glassfish.gmbal.generic.Algorithms ;


import org.glassfish.gmbal.impl.TypeConverter ;
import org.glassfish.gmbal.impl.ManagedObjectManagerInternal ;

import javax.management.MBeanInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.glassfish.gmbal.typelib.EvaluatedClassAnalyzer;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator;

public class JmxaTest extends TestCase {
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
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 213, 16, 1, 25 ) ;
	final List<Integer> sdata = new ArrayList<Integer>() ;

	for (Integer i : data) {
	    if ((i & 2) == 0) {
                sdata.add(i);
            }
	}

	final Predicate<Integer> ifEven = new Predicate<Integer>() {
	    public boolean evaluate( Integer arg ) {
		return (arg & 2) == 0 ;
	    }
	} ;

	List<Integer> result = Algorithms.filter( data, ifEven ) ;
	compareSequence( result, sdata ) ;
    }

    public void testFilter2() {
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 213, 16, 1, 25 ) ;
	final List<Integer> sdata = new ArrayList<Integer>() ;

	for (Integer i : data) {
	    if ((i & 2) == 0) {
                sdata.add(i);
            }
	}

	final Predicate<Integer> ifEven = new Predicate<Integer>() {
	    public boolean evaluate( Integer arg ) {
		return (arg & 2) == 0 ;
	    }
	} ;

	List<Integer> result = new ArrayList<Integer>() ;
	Algorithms.filter( data, result, ifEven ) ;
	compareSequence( result, sdata ) ;
    }

    public void testFind() {
	final List<Integer> data = Arrays.asList( 12, 23, 4, 9, 17, 42, 213, 16, 1, 25 ) ;

	final Predicate<Integer> is42 = new Predicate<Integer>() {
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
    public class H implements F {} 
    public class I extends H implements G {}

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
        EvaluatedClassAnalyzer ca = getCA( I.class ) ;
        List<EvaluatedClassDeclaration> res = ca.findClasses(
            Algorithms.TRUE( EvaluatedClassDeclaration.class ) ) ;
	System.out.println( "Inheritance chain for class " + I.class.getName() 
	    + " is " + res ) ;

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
        final EvaluatedClassAnalyzer ca = getCA( DD.class ) ;
	final Predicate predicate = 
	    new Predicate() {
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
        List<EvaluatedClassDeclaration> expectedResult =
            new ArrayList<EvaluatedClassDeclaration>() ;
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

    public void testPrimitiveTypeConverter() {
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
                assertTrue( tc.isIdentity() ) ;

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
    public static final String MDE_ATTR_ID_NAME = "name" ;
    public static final String MDE_ATTR_ID_DATE = "currentDate" ;
    public static final String MDE_ATTR_ID_GET_STRING = "String" ;

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
	public String name() {
	    return name ;
	}

	@ManagedAttribute( id=MDE_ATTR_ID_DATE )
        @Description( MDE_ATTR_DESC_DATE )
	public Date date() {
	    return date ;
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
	public String getName() {
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

    public class ManagedObjectExampleDerived extends ManagedObjectExample {
        public ManagedObjectExampleDerived( int num, String name ) {
            super( num, name ) ;
        }
    }
    
    public void testManagedObjectExample() {
        final int num = 12 ;
	final String name = "Liskov" ;
	final ManagedObjectExample rootObject = 
            new ManagedObjectExample( num, name ) ;
        managedObjectExampleHelper( rootObject ) ;
    }
    
    public void testManagedObjectExampleDerived() {
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
	    expectedProperties.put( "name", "na" ) ;
	    expectedProperties.put( "type", ManagedObjectExample.class.getName() ) ;
	    
	    assertEquals( expectedProperties, moeName.getKeyPropertyList() ) ;

	    MBeanServer mbs = mom.getMBeanServer() ;

	    // Validate attributes
	    assertEquals( mbs.getAttribute( moeName, "Num" ),
                Integer.valueOf( num ) ) ;
	    assertEquals( mbs.getAttribute( moeName, "Name" ), name ) ;
	    Object obj = mbs.getAttribute( moeName, "Mde" ) ;
	    assertTrue( obj instanceof CompositeData ) ;
	    CompositeData compData = (CompositeData)obj ;
	    assertEquals( root.getMde().name(), compData.get(
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

            Set<String> keys = new HashSet() ;
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
            assertEquals( value.name(), (String)compData.get( MDE_ATTR_ID_NAME ) ) ;
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
    private static final String ROOT_PARENT_NAME = ROOT_DOMAIN + ":type=FruitBat,name=Sam,foo=bar" ;
    
    private static final String ROOT_TYPE = "RootType" ;
    
    @ManagedObject
    @AMXMetadata( pathPart=ROOT_TYPE, isLeaf=false )
    public static class RootObject {
        private int value ;
        
        @ManagedAttribute
        int num() { return value ; }
        
        public RootObject( int num ) {
            this.value = num ;
        }
    }
    
    @ManagedObject
    @AMXMetadata( pathPart=ROOT_TYPE, isLeaf=false )
    public static class NamedRootObject extends RootObject{
        String name ;
        
        @ObjectNameKey
        public String getMyName() { return name ; }
        
        public NamedRootObject( String name, int num ) {
            super( num ) ;
            this.name = name;
        }
    }
        
    public void testRootMBean1() throws MalformedObjectNameException {
        final int value = 42 ;
        final String rootName = "MyRoot" ;
        final Object rootObject = new RootObject( value ) ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createFederated(
            new ObjectName( ROOT_PARENT_NAME ) ) ;
        mom.createRoot( rootObject, rootName ) ;
        mom.addTypePrefix("org.glassfish.gmbal");
        
        try {
            ObjectName rootObjectName = mom.getObjectName( rootObject ) ;
            String expectedName = "this.test:type=RootType,name=MyRoot,FruitBat=Sam,foo=bar" ;
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
        final int value = 42 ;
        final String rootName = "MyRoot" ;
        final Object rootObject = new NamedRootObject( rootName, value ) ;
        ManagedObjectManager mom = ManagedObjectManagerFactory.createFederated(
            new ObjectName( ROOT_PARENT_NAME ) ) ;
        mom.createRoot( rootObject ) ;
        mom.addTypePrefix("org.glassfish.gmbal");
        
        try {
            ObjectName rootObjectName = mom.getObjectName( rootObject ) ;
            String expectedName = "this.test:type=RootType,name=MyRoot,FruitBat=Sam,foo=bar" ;
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
    @AMXMetadata( pathPart=NMD_TYPE )
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
        
        ManagedObjectManager mom = ManagedObjectManagerFactory.createStandalone(
            ROOT_DOMAIN ) ;
        mom.createRoot() ;
        mom.addTypePrefix("org.glassfish.gmbal");
        
        try {
            mom.registerAtRoot( nmdt ) ;
            ObjectName oname = mom.getObjectName( nmdt ) ;
            String expectedName = 
                "this.test:GMBALROOT=na,type=NestedManagedDataTest,name=na" ;
            ObjectName expectedObjectName = null ;
            try {
                expectedObjectName = new ObjectName(expectedName);
            } catch (MalformedObjectNameException ex) {
                fail( "Could not create ObjectName: ex=" + ex ) ;
            } 
            
            assertEquals( expectedObjectName, oname ) ;
            
            MBeanServer server = mom.getMBeanServer() ;
            MBeanInfo mbi = server.getMBeanInfo( oname ) ;
            System.out.println( mbi ) ;
            
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
    
    public static Test suite() {
        return new TestSuite( JmxaTest.class ) ;
    }
}
