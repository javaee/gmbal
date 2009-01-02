/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.lang.reflect.Type;
import javax.management.openmbean.OpenType;
import junit.framework.TestCase;

/**
 *
 * @author ken
 */
public class TypeConverterImplTest extends TestCase {
    
    public TypeConverterImplTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getJavaClass method, of class TypeConverterImpl.
     */
    public void testGetJavaClass_OpenType() {
        System.out.println("getJavaClass");
        OpenType ot = null;
        Class expResult = null;
        Class result = TypeConverterImpl.getJavaClass(ot);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getJavaClass method, of class TypeConverterImpl.
     */
    public void testGetJavaClass_Type() {
        System.out.println("getJavaClass");
        Type type = null;
        Class expResult = null;
        Class result = TypeConverterImpl.getJavaClass(type);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of makeTypeConverter method, of class TypeConverterImpl.
     */
    public void testMakeTypeConverter() {
        System.out.println("makeTypeConverter");
        Type type = null;
        ManagedObjectManagerInternal mom = null;
        TypeConverter expResult = null;
        TypeConverter result = TypeConverterImpl.makeTypeConverter(type, mom);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDataType method, of class TypeConverterImpl.
     */
    public void testGetDataType() {
        System.out.println("getDataType");
        TypeConverterImpl instance = null;
        Type expResult = null;
        Type result = instance.getDataType();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getManagedType method, of class TypeConverterImpl.
     */
    public void testGetManagedType() {
        System.out.println("getManagedType");
        TypeConverterImpl instance = null;
        OpenType expResult = null;
        OpenType result = instance.getManagedType();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toManagedEntity method, of class TypeConverterImpl.
     */
    public void testToManagedEntity() {
        System.out.println("toManagedEntity");
        Object obj = null;
        TypeConverterImpl instance = null;
        Object expResult = null;
        Object result = instance.toManagedEntity(obj);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fromManagedEntity method, of class TypeConverterImpl.
     */
    public void testFromManagedEntity() {
        System.out.println("fromManagedEntity");
        Object entity = null;
        TypeConverterImpl instance = null;
        Object expResult = null;
        Object result = instance.fromManagedEntity(entity);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isIdentity method, of class TypeConverterImpl.
     */
    public void testIsIdentity() {
        System.out.println("isIdentity");
        TypeConverterImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isIdentity();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class TypeConverterImpl.
     */
    public void testToString() {
        System.out.println("toString");
        TypeConverterImpl instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /* Needed test cases:
    Parameterized type
        Collection
        Iterable
        Iterator
        Enumeration
        Map
        Dictionary
        Other
    Array Type
    Ignore TypeVariable and WildcardType: they will be remove after typelib integration
    Default case (currently handleAsString: move to handle as in MXBean)
    @ManagedObject
    enum
    MXBean behavior
        OpenType(J) -> Java class J
            CompositeData -> Java:
                1. J has method public static J from( CompositeData cd )
                2. public @ConstructorProperties constructor
                3. J is a class with public no-arg constructor, and every getter
                   has a setter
                4. J is an interface with only getters
        Need to fix the name of ManagedData attributes! (lower initial case)

     */
}
