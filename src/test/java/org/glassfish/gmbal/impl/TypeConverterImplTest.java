/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2018 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.glassfish.gmbal.ManagedObjectManagerFactory;

/**
 *
 * @author ken
 */
public class TypeConverterImplTest extends TestCase {
    ManagedObjectManagerInternal mom ;

    public TypeConverterImplTest(String testName) {
        super(testName);
    }

    private static boolean firstTime = true ;

    @Override
    protected void setUp() throws Exception {
        if (firstTime) {
            System.out.println( "****************** TypeConverterImplTest **********************" ) ;
            firstTime = false ;
        }
        super.setUp();
        mom = (ManagedObjectManagerInternal)ManagedObjectManagerFactory
            .createStandalone( "TestDomain" ) ;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mom.close();
    }

    /*
    public void testGetJavaClass_OpenType() {
        System.out.println("getJavaClass");
        OpenType ot = null;
        Class expResult = null;
        Class result = TypeConverterImpl.getJavaClass(ot);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    public void testGetJavaClass_EvaluatedType() {
        System.out.println("getJavaClass");
        EvaluatedType type = null;
        Class expResult = null;
        Class result = TypeConverterImpl.getJavaClass(type);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }
     */

    public void testData1() {
        Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl") ;
        logger.setLevel(Level.OFF ) ;
        try {
            TypeConverterTestData.Data1TestData.test( mom ) ;
        } finally {
            logger.setLevel(Level.INFO ) ;
        }
    }

    public void testData2() {
        TypeConverterTestData.Data2TestData.test( mom ) ;
    }

    public void testDoubleIndexData() {
        Logger logger = Logger.getLogger( "org.glassfish.gmbal.impl") ;
        logger.setLevel(Level.OFF ) ;
        try {
            TypeConverterTestData.Data3TestData.test( mom ) ;
        } finally {
            logger.setLevel(Level.INFO ) ;
        }
    }
}
