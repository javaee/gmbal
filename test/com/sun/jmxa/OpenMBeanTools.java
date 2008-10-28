package com.sun.jmxa;

import com.sun.jmxa.generic.ObjectWriter;
import java.util.List ;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

import com.sun.jmxa.generic.Triple ;

import java.lang.reflect.Array;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import static javax.management.openmbean.SimpleType.* ;
import static com.sun.jmxa.generic.Algorithms.* ;

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
                for (String key : ct.keySet() ) {
                    handleNestedType( writer, key, ct.getType(key) ) ;
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
                for (String key : cd.getCompositeType().keySet()) {
                    handleNestedValue( writer, key, cd.get(key) ) ;
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
    
    public static Object compV( CompositeType ct, 
        Map<String,Object> map ) {
        
        try {
            return new CompositeDataSupport(ct, map);
        } catch (OpenDataException ex) {
            throw new IllegalArgumentException(ex) ;
        }
    }
    
    public static void main( String[] args ) {
        List<Object> x = list( 0, "string", list( 3, 4 ) ) ;

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
