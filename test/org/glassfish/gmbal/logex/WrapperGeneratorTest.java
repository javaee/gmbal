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
        TestInterface self = WrapperGenerator.makeWrapper( TestInterface.class ) ;

        @Message( "This is a test" )
        @Log( level=LogLevel.WARNING, id=1 )
        IllegalArgumentException createTestException( @Chain Throwable thr ) ;

        @Message( "first argument {0} is followed by {1}")
        @Log( id=2 )
        String makeMessage( int arg1, String arg2 ) ;

        @Log( level=LogLevel.INFO, id=3 )
        String defaultMessage( int arg1, String arg2 ) ;

        @Message( "A simple message with {0} and {1}" )
        String simpleMessage( int first, String second ) ;
    }

    /**
     * Test of makeWrapper method, of class WrapperGenerator.
     */
    public void testCreateTestException() {
        Exception expectedCause = new Exception() ;
        Exception exc = TestInterface.self.createTestException( expectedCause ) ;
        assertTrue( exc instanceof IllegalArgumentException ) ;
        assertTrue( exc.getCause() == expectedCause ) ;
    }

    public void testMakeMessage() {
        String msg = TestInterface.self.makeMessage( 10, "hello" ) ;
        assertEquals( "WARNING: EWT2: first argument 10 is followed by hello",
            msg ) ;
    }

    public void testDefaultMessage() {
        String dmsg = TestInterface.self.defaultMessage( 10, "hello" ) ;
        assertEquals( "INFO: EWT3: defaultMessage arg0=10, arg1=hello", dmsg ) ;
    }

    public void testSimpleMessage( ) {
        String smsg = TestInterface.self.simpleMessage( 10, "hello" ) ;
        assertEquals( "A simple message with 10 and hello", smsg ) ;
    }
}
