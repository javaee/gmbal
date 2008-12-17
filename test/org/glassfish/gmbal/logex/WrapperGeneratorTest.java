/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.logex;

import junit.framework.TestCase;

/**
 *
 * @author ken
 */
public class WrapperGeneratorTest extends TestCase {
    
    public WrapperGeneratorTest(String testName) {
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

    @ExceptionWrapper( idPrefix="EWT" )
    public interface TestInterface {
        @Message( "This is a test" )
        @Log( level=LogLevel.WARNING, id=1 )
        IllegalArgumentException createTestException( @Chain Throwable thr ) ;
    }

    /**
     * Test of makeWrapper method, of class WrapperGenerator.
     */
    public void testMakeWrapper() {
        System.out.println("makeWrapper");
        Class<TestInterface> cls = TestInterface.class ;
        TestInterface result = WrapperGenerator.makeWrapper(cls);
        Exception expectedCause = new Exception() ;
        Exception exc = result.createTestException( expectedCause ) ;
        assertTrue( exc instanceof IllegalArgumentException ) ;
        assertTrue( exc.getCause() == expectedCause ) ;
    }

}
