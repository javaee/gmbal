/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.gmbal;

import org.glassfish.gmbal.generic.ObjectWriter;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

import org.glassfish.gmbal.generic.Triple ;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import static javax.management.openmbean.SimpleType.* ;

/**
 *
 * @author ken
 */
public class OpenMBeanTools {

    @SuppressWarnings("unchecked")
    public static ArrayType array( int dim, OpenType ot ) {
        try {
            return new ArrayType( dim, ot ) ;
        } catch (Exception exc) {
            throw new IllegalArgumentException( exc ) ;
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
            return new CompositeType( typeName, desc, itemNames, itemDescs, 
                itemTypes ) ;
        } catch (Exception exc) {
            throw new IllegalArgumentException( exc ) ;
        }
    }
    
    public static TabularType tab( String typeName, String desc, 
        CompositeType rowType, String... indexNames ) {

        try {
            return new TabularType( typeName, desc, rowType, indexNames ) ;
        } catch (Exception exc) {
            throw new IllegalArgumentException( exc ) ;
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
