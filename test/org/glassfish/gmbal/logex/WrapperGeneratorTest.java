/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at legal/LICENSE.TXT.
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
 * 
 */ 
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
