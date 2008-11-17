/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.impl;

import com.sun.jmxa.generic.Algorithms;
import com.sun.jmxa.generic.UnaryFunction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ken
 */
public class ContainerImpl extends AMXImpl implements Container {

    public ContainerImpl( MBeanImpl mb ) {
        super( mb ) ;
    }
    
    public Set<String> getContaineeJ2EETypes() {
        return Collections.unmodifiableSet( mbean.children().keySet() ) ;
    }
    
    private static UnaryFunction<MBeanImpl,AMX> extract = 
        new UnaryFunction<MBeanImpl,AMX>() {
            @SuppressWarnings("unchecked")
            public AMX evaluate( MBeanImpl mb ) {
                return mb.facet( AMX.class, false ) ;
            }
        } ;

    public Map<String, Map<String, AMX>> 
        getMultiContaineeMap(Set<String> j2eeTypes) {
        
        Map<String,Map<String,AMX>> result = 
            new HashMap<String,Map<String,AMX>>() ;
                
        for (String str : j2eeTypes) {
            Map<String,MBeanImpl> value = mbean.children().get(str) ;
            result.put( str, Algorithms.map( value, extract ) ) ;
        }
        
        return result ;
    }

    public Map<String, AMX> getContaineeMap(String j2eeType) {
        Map<String,MBeanImpl> value = mbean.children().get( j2eeType ) ;
        return Algorithms.map( value, extract ) ;
    }

    public AMX getContainee(String j2eeType) {
        Map<String,MBeanImpl> contents = mbean.children().get( j2eeType ) ;
        return Algorithms.getOne( contents.values(), "No MBeans found", 
            "More than one MBean of type " + j2eeType + " found" )
            .facet( AMX.class, false ) ;
    }

    public Set<AMX> getContaineeSet(String j2eeType) {
        return new HashSet<AMX>( 
            Algorithms.map(  mbean.children().get( j2eeType ), 
                extract ).values() ) ;
    }

    public Set<AMX> getContaineeSet() {
        return getContaineeSet( mbean.children().keySet() ) ;
    }

    public Set<AMX> getContaineeSet(Set<String> j2eeTypes) {
        Set<AMX> result = new HashSet<AMX>() ;
        for (String str : j2eeTypes ) {
            result.addAll( getContaineeSet( str ) ) ;
        }
        return result ;
    }

    public Set<AMX> getByNameContaineeSet(Set<String> j2eeTypes,
        String name) {
        Set<AMX> result = new HashSet<AMX>() ;
        for (String str : j2eeTypes ) {
            MBeanImpl mb = mbean.children().get( str ).get( name ) ;
            if (mb != null) {
                result.add( mb.facet( AMX.class, false ) ) ;
            }
        }
        return result ;    }

    public AMX getContainee(String j2eeType, String name) {
        return mbean.children().get( j2eeType ).get( name ).facet( AMX.class,
            false ) ;
    }

}
