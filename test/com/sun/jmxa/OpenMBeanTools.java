package com.sun.jmxa;

import java.util.List ;
import java.util.ArrayList ;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

import com.sun.jmxa.generic.Triple ;
import com.sun.jmxa.generic.Pair ;

import javax.management.openmbean.CompositeDataSupport;
import static javax.management.openmbean.SimpleType.* ;

/**
 *
 * @author ken
 */
public class OpenMBeanTools {
    public static <T> List<T> list( T... arg ) {
        List<T> result = new ArrayList<T>() ;
        for (T obj : arg) {
            result.add( obj ) ;
        }
        return result ;
    }

    public static <S,T> Pair<S,T> pair( S first, T second ) {
        return new Pair<S,T>( first, second ) ;
    }
    
    public static <K,V> Map<K,V> map( Pair<K,V>... pairs ) {
        Map<K,V> result = new HashMap<K,V>() ;
        for (Pair<K,V> pair : pairs ) {
            result.put( pair.first(), pair.second() ) ;
        }
        return result ;
    }
    
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

    public static Item item( String name, String desc, OpenType type ) {
        return new Item( name, desc, type ) ;
    }

    public static CompositeType comp( String typeName, String desc, List<Item> items ) {
        int size = items.size() ;
        String[] itemNames = new String[size] ;
        String[] itemDescs = new String[size] ;
        OpenType[] itemTypes = new OpenType[size] ;

        int ctr = 0 ;
        for (Item item : items) {
            itemNames[ctr] = item.name() ;
            itemDescs[ctr] = item.desc() ;
            itemTypes[ctr] = item.type() ;
            ctr++ ;
        }

        try {
            return new CompositeType( typeName, desc, itemNames, itemDescs, itemTypes ) ;
        } catch (Exception exc) {
            throw new IllegalArgumentException( exc ) ;
        }
    }
    
    private static TabularType tab( String typeName, String desc, 
        CompositeType rowType, List<String> indexNames ) {
        
        String[] inames = new String[indexNames.size()] ;
        int ctr = 0 ;
        for (String str : indexNames) {
            inames[ctr] = str ;
            ctr++ ;
        }

        try {
            return new TabularType( typeName, desc, rowType, inames ) ;
        } catch (Exception exc) {
            throw new IllegalArgumentException( exc ) ;
        }
    }
    
    public static String displayOpenType( OpenType ot ) {
        return "" ;
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
                list( 
                    item( "fld1", "field 1", BYTE ),
                    item( "fld2", "field 2", OBJECTNAME ),
                    item( "fld3", "field 3",
                        array( 1, 
                            comp( "comp2", "comp type 2",
                                list(
                                    item( "fld4", "field 4", LONG ),
                                    item( "fld5", "field 5", DATE ))))))),
            list( "fld2" )) ;
                        
        System.out.println( ot ) ;
        // System.out.println( ObjectUtility.defaultObjectToString( ot ) ) ;
    }

}
