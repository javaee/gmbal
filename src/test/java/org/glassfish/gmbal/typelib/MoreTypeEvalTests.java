/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gmbal.typelib;

import junit.framework.TestCase;

/**
 * @author ken
 */
public class MoreTypeEvalTests extends TestCase {
    private static int evalClassMapSize = 0 ;

    public static int additionalClasses() {
        //int oldSize = evalClassMapSize ;
        return TypeEvaluator.evalClassMapSize() ;
        //return evalClassMapSize - oldSize  ;
    }
    public MoreTypeEvalTests(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.out.println( "initial evalClassMap size = "
            + additionalClasses() ) ;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.out.println( "final evalClassMap size = "
            + additionalClasses() ) ;
    }

    public void testListInteger() {
        System.out.println( "testListInteger" ) ;
        EvaluatedType type = TestTypelibDecls.Prototypes.LIST_INTEGER ;
        assertEquals( "java.util.List<java.lang.Integer>", type.toString() );
    }

    public void testListObject() {
        System.out.println( "testListObject" ) ;
        EvaluatedType type = TestTypelibDecls.Prototypes.LIST_OBJECT ;
        assertEquals( "java.util.List<java.lang.Object>", type.toString() );
    }

    public void testListString() {
        System.out.println( "testListString" ) ;
        EvaluatedType type = TestTypelibDecls.Prototypes.LIST_STRING ;
        assertEquals( "java.util.List<java.lang.String>", type.toString() );
    }

    public void testListListString() {
        System.out.println( "testListListString") ;
        EvaluatedType type = TestTypelibDecls.Prototypes.LIST_LIST_STRING ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals( "java.util.List<java.util.List<java.lang.String>>",
            type.toString() ) ;
    }

    public void testListListObject() {
        System.out.println( "testListListObject") ;
        EvaluatedType type = TestTypelibDecls.Prototypes.LIST_LIST_OBJECT ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals( "java.util.List<java.util.List<java.lang.Object>>",
            type.toString() ) ;
    }

    public void testMapObjectObject() {
        System.out.println( "testMapObjectObject") ;
        EvaluatedType type = TestTypelibDecls.Prototypes.MAP_OBJECT_OBJECT ;
        assertEquals( "java.util.Map<java.lang.Object,java.lang.Object>",
            type.toString() ) ;
    }

    public void testMapStringInteger() {
        System.out.println( "testMapStringInteger") ;
        EvaluatedType type = TestTypelibDecls.Prototypes.MAP_STRING_INTEGER ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals( "java.util.Map<java.lang.String,java.lang.Integer>",
            type.toString() ) ;
    }

    public void testListMapStringListString() {
        System.out.println( "testListMapStringListString") ;
        EvaluatedType type =
            TestTypelibDecls.Prototypes.LIST_MAP_STRING_LIST_STRING ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals(
            "java.util.List<java.util.Map<java.lang.String,java.util.List<java.lang.String>>>",
            type.toString() ) ;
    }

    public void testRecursiveType() {
        System.out.println( "testRecursiveType") ;
        EvaluatedType type = TestTypelibDecls.Prototypes.RECURSIVE_TYPE ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals( "java.util.List<java.util.List>", type.toString() ) ;
    }

    public void testColor() {
        System.out.println( "testColor") ;
        EvaluatedType type = TestTypelibDecls.Prototypes.COLOR ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals(
            "org.glassfish.gmbal.typelib.TestTypelibDecls$Prototypes$Color",
            type.toString() ) ;
    }

    public void testConcurrentHashMap() {
        System.out.println( "testConcurrentHashMap") ;
        EvaluatedType type = TestTypelibDecls.getCHM() ;
        // System.out.println( "toString() = " + type.toString() ) ;
        assertEquals(
            "java.util.concurrent.ConcurrentHashMap<java.lang.String,java.lang.String>",
            type.toString() ) ;
    }

    public void testComplexType() {
        try {
            System.out.println( "testComplexType1") ;
            EvaluatedType etype = TypeEvaluator.getEvaluatedType( ComplexType1.CT21.class) ;
            fail( "Expected an exception" ) ;
        } catch (StackOverflowError ignored) {
        } catch (IllegalStateException ignored) {
        }
    }
}
