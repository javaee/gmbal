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

import java.util.Arrays;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;

import org.glassfish.gmbal.ManagedData;
import static org.glassfish.gmbal.OpenMBeanTools.* ;

/**
 *
 * @author ken
 */
public class TypeConverterTestData {
    private TypeConverterTestData() {}

    /* For each test:
     *      Java type
     *      Expected OpenType
     *      Java value
     *      Expected OpenType value
     *      value of isIdentity
     Needed test cases:
    Parameterized type
        SortedSet (MXBean semantics)
        Collection
        Iterable
        Iterator
        Enumeration
        Map
        SortedMap (MXBean semantics)
        Dictionary
        Other
    Array Type
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

    public static class TestData {
        private Object data ;
        private OpenType otype ;
        private Object ovalue ;
        private boolean isIdentity ;

        public TestData( final Object data, final OpenType otype,
            final Object ovalue, final boolean isIdentity ) {
            this.data = data ;
            this.otype = otype ;
            this.ovalue = ovalue ;
            this.isIdentity = isIdentity ;
        }

        public TestData( final Object data, final OpenType otype,
            final Object ovalue ) {
            this( data, otype, ovalue, false ) ;
        }

        Object data() { return data ; }
        OpenType otype() { return otype ; }
        Object ovalue() { return ovalue ; }
        boolean isIdentity() { return isIdentity ; }
    }

    private static final String LIST_DESC = "Description of the list attribute" ;

    public interface TestBase<T> {
        @ManagedAttribute
        @Description( LIST_DESC )
        List<T> getList() ;
    }

    private static final String VALUE_DESC = "Description of the value attribute" ;
    private static final String DATA1_NAME = "DATA1" ;
    private static final String DATA1_DESC = "Description of Data1 type" ;

    @ManagedData( name=DATA1_NAME )
    @Description( DATA1_DESC )
    public static class Data1 implements TestBase<String> {
        private final int value ;
        private final List<String> list ;

        @ManagedAttribute 
        @Description( VALUE_DESC )
        public int value() { return value ; }

        @ManagedAttribute
        @Description( LIST_DESC )
        public List<String> getList() { return list ; }

        public Data1( int value, String... args ) {
            this.value = value ;
            this.list = Arrays.asList( args ) ;
        }
    }

    private static final CompositeType DATA1_OTYPE =
        comp( DATA1_NAME, DATA1_DESC,
            item( "list", LIST_DESC, array( 1, SimpleType.STRING )),
            item( "value", VALUE_DESC, SimpleType.INTEGER )
        ) ;

    private static String[] data1List = { "One", "Two", "Three" } ;

    private static final Data1 data1 = new Data1( 21, data1List ) ;

    private static final CompositeData data1Open =
        compV( DATA1_OTYPE,
            mkmap(
                list( "list", "value" ),
                listO( data1List, 21 ) ) ) ;

    public static final TestData Data1TestData = new TestData( data1,
        DATA1_OTYPE, data1Open ) ;

}
