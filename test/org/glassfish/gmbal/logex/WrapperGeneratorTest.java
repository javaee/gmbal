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

        @Message( "first argument {0} is followed by {1}")
        @Log( id=2 )
        String makeMessage( int arg1, String arg2 ) ;
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

        String msg = result.makeMessage( 10, "hello" ) ;
        assertEquals( "INFO: EWT2: first argument 10 is followed by hello",
            msg ) ;
    }

}
