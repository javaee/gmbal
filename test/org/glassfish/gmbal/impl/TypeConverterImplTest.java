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

package org.glassfish.gmbal.impl;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenType;
import junit.framework.TestCase;
import org.glassfish.gmbal.ManagedObjectManagerFactory;
import org.glassfish.gmbal.typelib.EvaluatedClassDeclaration;
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

    private TypeConverter getTypeConverter( Object obj ) {
        Class<?> cls = obj.getClass() ;
        EvaluatedClassDeclaration ecd = 
            (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType(cls) ;
        TypeConverter tc = mom.getTypeConverter(ecd) ;
        return tc ; 
    }

    private void doTest( TypeConverterTestData.TestData td ) {
        TypeConverter tc = getTypeConverter( td.data() ) ;
        System.out.println( "tc.getManagedType() = " + tc.getManagedType() ) ;
        System.out.println( "td.otype()          = " + td.otype() ) ;
        assertEquals( td.otype().toString(), tc.getManagedType().toString() ) ;
        Object mvalue = tc.toManagedEntity(td.data()) ;
        // XXX Need to handle String[]: bring back deep Equals code? assertEquals( td.ovalue(), mvalue ) ;
        assertEquals( td.isIdentity(), tc.isIdentity() ) ;
        try {
            Object jvalue = tc.fromManagedEntity( mvalue ) ;
            assertEquals( td.data(), jvalue ) ;
        } catch (UnsupportedOperationException exc) {
            System.out.println( "Conversion to Java type not currently supported for " 
                + tc.getManagedType() ) ;
        }
    }

    public void testData1() {
        doTest( TypeConverterTestData.Data1TestData ) ;
    }

    public void testDoubleIndexData() {
        TypeConverterTestData.DoubleIndexData did = 
            new TypeConverterTestData.DoubleIndexData() ;
        TypeConverter tc = getTypeConverter( did ) ;
        Object obj = tc.toManagedEntity(did) ;
        assertTrue( obj instanceof CompositeData ) ;
        System.out.println( obj ) ;
    }
}
