/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.lang.reflect.Type;
import javax.management.openmbean.OpenType;
import junit.framework.TestCase;
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.ManagedObjectManagerFactory;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;
import org.glassfish.gmbal.typelib.TypeEvaluator;

/**
 *
 * @author ken
 */
public class TypeConverterImplTest extends TestCase {
    ManagedObjectManagerInternal mom ;

    public TypeConverterImplTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mom = (ManagedObjectManagerInternal)ManagedObjectManagerFactory
            .createStandalone( "TestDomain" ) ;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mom.close();
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
    public void testGetJavaClass_EvaluatedType() {
        System.out.println("getJavaClass");
        EvaluatedType type = null;
        Class expResult = null;
        Class result = TypeConverterImpl.getJavaClass(type);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    private TypeConverter getTypeConverter( Object obj ) {
        Class<?> cls = obj.getClass() ;
        EvaluatedClassDeclaration ecd = 
            (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(cls) ;
        TypeConverter tc = mom.getTypeConverter(ecd) ;
        return tc ; 
    }

    private void doTest( TypeConverterTestData.TestData td ) {
        TypeConverter tc = getTypeConverter( td.data() ) ;
        assertEquals( td.otype(), tc.getManagedType() ) ;
        Object mvalue = tc.toManagedEntity(td.data()) ;
        assertEquals( td.ovalue(), mvalue ) ;
        assertEquals( td.isIdentity(), tc.isIdentity() ) ;
        Object jvalue = tc.fromManagedEntity( mvalue ) ;
        assertEquals( td.data(), jvalue ) ;
    }

    public void testData1() {
        doTest( TypeConverterTestData.Data1TestData ) ;
    }
}
