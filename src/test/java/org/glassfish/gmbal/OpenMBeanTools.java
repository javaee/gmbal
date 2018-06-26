/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gmbal;

import org.glassfish.pfl.basic.func.UnaryFunction;
import org.glassfish.pfl.basic.algorithm.Algorithms;
import org.glassfish.pfl.basic.algorithm.ObjectWriter;
import org.glassfish.pfl.basic.contain.Triple;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import static javax.management.openmbean.SimpleType.* ;

/** Tools for constructing open MBean types and values, and comparing them.
 *
 * @author ken
 */
public class OpenMBeanTools {

    @SuppressWarnings("unchecked")
    public static ArrayType array( int dim, OpenType ot ) {
        try {
            return new ArrayType(dim, ot);
        } catch (OpenDataException ex) {
            throw new IllegalArgumentException( ex ) ;
        }
    }

    public static class Item extends Triple<String,String,OpenType> {

        public Item( String name, String desc, OpenType type ) {
            super( name, desc, type ) ;
        }

        public String name() { return first() ; }
        public String desc() { return second() ; }
        public OpenType type() { return third() ; }
    }

    public static Item item( final String name, final String desc, 
        final OpenType type ) {
        
        return new Item( name, desc, type ) ;
    }

    public static CompositeType comp( final String typeName, final String desc,
        final Item... items ) {
        final int size = items.length ;
        final String[] itemNames = new String[size] ;
        final String[] itemDescs = new String[size] ;
        final OpenType[] itemTypes = new OpenType[size] ;

        int ctr = 0 ;
        for (Item item : items) {
            itemNames[ctr] = item.name() ;
            itemDescs[ctr] = item.desc() ;
            itemTypes[ctr] = item.type() ;
            ctr++ ;
        }

        try {
            return new CompositeType(typeName, desc, itemNames, itemDescs, itemTypes);
        } catch (OpenDataException ex) {
            throw new IllegalArgumentException( ex ) ;
        }
    }
    
    public static TabularType tab( String typeName, String desc, 
        CompositeType rowType, String... indexNames ) {
        try {
            return new TabularType(typeName, desc, rowType, indexNames);
        } catch (OpenDataException ex) {
            throw new IllegalArgumentException( ex ) ;
        }
    }
    
    public static TabularData tabV( final TabularType tt, 
        final CompositeData... data ) {
        
        TabularData result = new TabularDataSupport(tt) ;
        result.putAll( data ) ;
        return result ;
    }
    
    public static String displayOpenType( OpenType ot ) {
        ObjectWriter writer = ObjectWriter.make( true, 0, 4 ) ;
        displayOpenTypeHelper( writer, ot ) ;
        return writer.toString() ;
    }
    
    public static String displayOpenValue( Object obj ) {
        ObjectWriter writer = ObjectWriter.make( true, 0, 4 ) ;
        displayOpenValueHelper( writer, obj ) ;
        return writer.toString() ;
    }
    
    private static void handleElement( final ObjectWriter writer, 
        final String str, final Object value ) {
        writer.startElement() ;
        writer.append( str ) ;
        writer.append( '=' ) ;
        writer.append( value ) ;
        writer.endElement() ;
    }
    
    private static void handleNestedType( final ObjectWriter writer,
        final String str, final OpenType ot ) {
        writer.startElement() ;
        writer.append( str ) ;
        writer.append( '=' ) ;
        displayOpenTypeHelper( writer, ot ) ;
        writer.endElement() ;
    }

    private static void handleNestedValue( final ObjectWriter writer,
        final String str, final Object obj ) {
        writer.startElement() ;
        writer.append( str ) ;
        writer.append( '=' ) ;
        displayOpenValueHelper( writer, obj ) ;
        writer.endElement() ;
    }
    
    private static void displayOpenTypeHelper( final ObjectWriter writer, 
        final OpenType ot ) {
        
        try {
            if (ot instanceof SimpleType) {
                writer.startObject( "SimpleType:" + ot.getClassName() ) ;
            } else if (ot instanceof ArrayType) {
                ArrayType at = (ArrayType)ot ;
                writer.startObject( "ArrayType" ) ;

                handleElement( writer, "dim", at.getDimension() ) ;
                handleElement( writer, "description", at.getDescription() ) ;
                handleNestedType( writer, "componentType", at.getElementOpenType() ) ;
            } else if (ot instanceof CompositeType) {
                CompositeType ct = (CompositeType)ot ;
                writer.startObject( "CompositeType" ) ;

                handleElement( writer, "description", ct.getDescription() ) ;
                for (Object key : ct.keySet() ) {
                    handleNestedType( writer, (String)key,
                        ct.getType((String)key) ) ;
                }
            } else if (ot instanceof TabularType) {
                TabularType tt = (TabularType)ot ;
                writer.startObject( "TabularType" ) ;

                handleElement( writer, "description", tt.getDescription() ) ;
                handleElement( writer, "indexNames", tt.getIndexNames() ) ;
                handleNestedType( writer, "rowType", tt.getRowType() ) ;
            } else {
                writer.startObject( "*UNKNOWN*" ) ;
            }
        } finally {
            writer.endObject() ;
        }
    }

    private static void displayOpenValueHelper( final ObjectWriter writer, 
        final Object obj ) {
        
        try {
            if (obj.getClass().isArray()) {
                Class cls = obj.getClass() ;
                writer.startObject( cls.getName() + "[]=" ) ;
                for (int ctr=0; ctr<Array.getLength(obj); ctr++) {
                    handleNestedValue( writer, "[" + ctr + "]", Array.get( obj, ctr ) ) ;
                }
            } else if (obj instanceof CompositeData) {
                CompositeData cd = (CompositeData)obj ;
                writer.startObject( "CompositeData" ) ;
                handleNestedType( writer, "type", cd.getCompositeType() ) ;
                for (Object key : cd.getCompositeType().keySet()) {
                    handleNestedValue( writer, (String)key, 
                        cd.get((String)key) ) ;
                }
            } else if (obj instanceof TabularData) {
                TabularData td = (TabularData)obj ;
                TabularType tt = td.getTabularType() ;
                writer.startObject( "TabularData:" ) ;
                writer.startElement() ;
                displayOpenTypeHelper( writer, tt ) ;
                writer.endElement() ;
                for (Object row : td.values()) {
                    handleNestedValue( writer, "", row ) ;
                }
            } else {
                writer.startObject( obj.getClass().getName()) ;
                writer.append( obj.toString() ) ;
            }
        } finally {
            writer.endObject() ;
        }
    }

    public static <K,V> Map<K,V> mkmap( List<K> keys, List<V> values ) {
        Iterator<K> ikey = keys.iterator() ;
        Iterator<V> ivalue = values.iterator() ;
        Map<K,V> result = new HashMap<K,V>() ;
        while (ikey.hasNext() && ivalue.hasNext()) {
            result.put( ikey.next(), ivalue.next() ) ;
        }
        if (ikey.hasNext() != ivalue.hasNext()) {
            throw new RuntimeException( "key and value lists have different lengths") ;
        }
        return result ;
    }

    public static <E> List<E> list( E... args ) {
        return Arrays.asList( args ) ;
    }

    public static List<Object> listO( Object... args ) {
        return Arrays.asList( args ) ;
    }
    
    public static CompositeData compV( CompositeType ct,
        Map<String,Object> map ) {
        
        try {
            return new CompositeDataSupport(ct, map);
        } catch (OpenDataException ex) {
            throw new IllegalArgumentException(ex) ;
        }
    }

    private static boolean compare( boolean res, String info, Object first, Object second ) {
        if (!res) {
            System.out.println( "Comparison failed on " + info + ":\n"
                + "first arg:\n\t" + first
                + "\nsecond arg:\n\t" + second ) ;
        }

        return res ;
    }

    // Do we need this?  Looks like OpenType may have a well-defined equals
    // method.  But equals/toString is terrible for diagnostics: we end
    // up with extremely long (hundreds of chars!) strings that NB does not display
    // properly.  We really need a comparison that says WHY the types are not the
    // same.
    public static boolean equalTypes( OpenType ot1, OpenType ot2 )  {
        String tname1 = ot1.getTypeName() ;
        String tname2 = ot2.getTypeName() ;
        if (compare( tname1.equals( tname2 ), "type names", ot1, ot2 )) {
            if (ot1 instanceof SimpleType) {
                SimpleType st1 = (SimpleType)ot1 ;
                if (!(ot2 instanceof SimpleType)) {
                    return false ;
                }
                SimpleType st2 = (SimpleType)ot2 ;

                return compare( st1.equals( st2 ), "simple types", st1, st2 ) ;
            } else if (ot1 instanceof ArrayType) {
                ArrayType at1 = (ArrayType)ot1 ;
                if (!(ot2 instanceof ArrayType)) {
                    return false ;
                }
                ArrayType at2 = (ArrayType)ot2 ;

                int dim1 = at1.getDimension() ;
                int dim2 = at2.getDimension() ;
                if (compare( dim1==dim2, "array dimensions", dim1, dim2 ))  {
                    OpenType c1 = at1.getElementOpenType() ;
                    OpenType c2 = at2.getElementOpenType() ;
                    return compare( equalTypes( c1, c2 ), "array component types", c1, c2 ) ;
                }
            } else if (ot1 instanceof CompositeType) {
                CompositeType ct1 = (CompositeType)ot1 ;
                if (!(ot2 instanceof CompositeType)) {
                    return false ;
                }
                CompositeType ct2 = (CompositeType)ot2 ;
                String desc1 = ct1.getDescription() ;
                String desc2 = ct2.getDescription() ;
                if (compare( desc1.equals( desc2 ), 
                    "CompositeType descriptions", desc1, desc2 )) {

                    Set<String> keys1 = (Set<String>)ct1.keySet() ;
                    Set<String> keys2 = (Set<String>)ct2.keySet() ;
                    int size1 = keys1.size() ;
                    int size2 = keys2.size() ;

                    if (compare( size1==size2,
                        "size of CompositeType keySets", keys1, keys2 )) {
                        Iterator<String> iter1 = keys1.iterator() ;
                        while(iter1.hasNext()) {
                            String key1 = iter1.next() ;
                            if (compare( keys2.contains( key1 ),
                                "key not found", key1, keys2 )) {

                                OpenType cot1 = ct1.getType( key1 ) ;
                                OpenType cot2 = ct2.getType( key1 ) ;
                                if (!compare( equalTypes( cot1, cot2 ),
                                    "CompositeType field " + key1, cot1, cot2 )) {
                                    return false ;
                                }
                            } else {
                                return false ;
                            }
                        }
                        return true ;
                    }
                }
            } else if (ot1 instanceof TabularType) {
                TabularType tt1 = (TabularType)ot1 ;
                if (!(ot2 instanceof TabularType)) {
                    return false ;
                }
                TabularType tt2 = (TabularType)ot2 ;

                String desc1 = tt1.getDescription() ;
                String desc2 = tt2.getDescription() ;
                if (compare( desc1.equals(desc2), "TabularType descriptions",
                    desc1, desc2 )) {

                    CompositeType tct1 = tt1.getRowType() ;
                    CompositeType tct2 = tt2.getRowType() ;
                    if (compare( equalTypes( tct1, tct2 ), "TabularType row types",
                        tct1, tct2 )) {

                        List<String> indices1 = (List<String>)tt1.getIndexNames() ;
                        List<String> indices2 = (List<String>)tt2.getIndexNames() ;
                        return compare( indices1.equals(indices2), "TabularType index names",
                            indices1, indices2 ) ;
                    }
                }
            }
        }

        return false ;
    }

    private static Set<TabularDataKey> getKeys( TabularData td ) {
        Set<TabularDataKey> result = new HashSet<TabularDataKey>() ;

        Algorithms.map( (Set<Object[]>)td.keySet(), result,
            new UnaryFunction<Object[],TabularDataKey>() {
                public TabularDataKey evaluate( Object[] arg ) {
                    return new TabularDataKey( arg ) ;
                } } );

        return result ;
    }

    private static class TabularDataKey {
        private Object[] key ;

        public TabularDataKey( Object[] key ) {
            this.key = key ;
        }

        @Override
        public boolean equals( Object obj ) {
            if (this == obj) {
                return true ;
            }

            if (!(obj instanceof TabularDataKey)) {
                return false ;
            }

            TabularDataKey other = (TabularDataKey)obj ;

            return Arrays.equals(key, other.key);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(key);
        }

        @Override
        public String toString() {
            return Arrays.toString(key) ;
        }

        CompositeData get( TabularData td ) {
            return td.get( key ) ;
        }
    }

    // Need this, because the equals methods give us no useful information
    // about WHY there is a difference.
    public static boolean equalValues( Object v1, Object v2 )  {
        Class cls1 = v1.getClass() ;
        if (v1 instanceof CompositeData) {
            CompositeData cd1 = (CompositeData)v1 ;
            if (compare( v2 instanceof CompositeData, 
                "v1 is CompositeData, v2 is not", v1, v2 )) {

                CompositeData cd2 = (CompositeData)v2 ;

                CompositeType ct1 = cd1.getCompositeType() ;
                CompositeType ct2 = cd2.getCompositeType() ;

                if (compare( equalTypes( ct1, ct2 ), 
                    "CompositeTypes are not the same", ct1, ct2 )) {

                    Set<String> keys = (Set<String>)ct1.keySet() ;
                    for (String key : keys) {
                        Object elem1 = cd1.get(key) ;
                        Object elem2 = cd2.get(key) ;
                        if (!compare( equalValues( elem1, elem2), 
                            "Elements for key " + key + " are not the same",
                            elem1, elem2 )) {
                            return false ;
                        }
                    }
                    return true ;
                }
            }
        } else if (v1 instanceof TabularData) {
            TabularData td1 = (TabularData)v1 ;
            if (compare( v2 instanceof TabularData,
                "v1 is TabularData, v2 is not", v1, v2 )) {

                TabularData td2 = (TabularData)v2 ;

                TabularType tt1 = td1.getTabularType() ;
                TabularType tt2 = td2.getTabularType() ;

                if (compare( equalTypes( tt1, tt2 ),
                    "CompositeTypes are not the same", tt1, tt2 )) {
                    Set<TabularDataKey> keys1 = getKeys( td1 ) ;
                    Set<TabularDataKey> keys2 = getKeys( td2 ) ;
                    if (compare( keys1.equals( keys2 ), "TabularData key sets",
                        keys1, keys2 )) {
                        for (TabularDataKey key : keys1) {
                            CompositeData cd1 = key.get( td1 ) ;
                            CompositeData cd2 = key.get( td2 ) ;
                            if (!compare( equalValues(cd1, cd2), 
                                "TabularData at key " + key, cd1, cd2 )) {

                                return false ;
                            }
                        }
                        return true ;
                    }
                }
            }
        } else if (cls1.isArray()) {
            Class cls2 = v2.getClass() ;
            if (compare( cls2.isArray(), "v1 is an array, but v2 is not", 
                v1, v2 )) {

                int len1 = Array.getLength(v1) ;
                int len2 = Array.getLength(v2) ;
                if (compare( len1==len2, "array lengths", v1, v2 )) {
                    for (int ctr=0; ctr<len1; ctr++) {
                        Object elem1 = Array.get( v1, ctr ) ;
                        Object elem2 = Array.get( v2, ctr ) ;
                        if (!compare( equalValues( elem1, elem2 ), 
                            "array element at index " + ctr, elem1, elem2 )) {
                            return false ;
                        }
                    }
                    return true ;
                }
            }
        } else {
            // Must be a simple type, or invalid.  We'll just use equals here.
            return compare( v1.equals(v2), "simple values", v1, v2 ) ;
        }

        return false ;
    }

    public static void main( String[] args ) {
        // List<Object> x = list( 0, "string", list( 3, 4 ) ) ;

        OpenType ot = tab( "tab1", "a tab type", 
            comp( "comp1", "a comp type", 
                item( "fld1", "field 1", BYTE ),
                item( "fld2", "field 2", OBJECTNAME ),
                item( "fld3", "field 3",
                    array( 1, 
                        comp( "comp2", "comp type 2",
                            item( "fld4", "field 4", LONG ),
                            item( "fld5", "field 5", DATE ))))),
            "fld2" ) ;
                        
        System.out.println( ot ) ;
        System.out.println( displayOpenType( ot ) ) ;
    }

}
