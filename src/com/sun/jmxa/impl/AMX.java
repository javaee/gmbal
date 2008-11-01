package com.sun.jmxa.impl ;

import com.sun.jmxa.Description;
import com.sun.jmxa.ManagedAttribute;
import com.sun.jmxa.ManagedObject;
import com.sun.jmxa.ManagedOperation;
import com.sun.jmxa.ParameterNames;
import java.util.Map;
import java.util.Set;

@ManagedObject
@Description( "Base interface for any MBean that works in the AMX framework" ) 
public interface AMX {
    @ManagedAttribute
    @Description( "" )
    Container getContainer() ;

    // not sure what getDomainRoot would mean?
    
    enum GroupType { Configuration, Monitoring, Utilitiy, jsr77, other } ;
    
    GroupType getGroup() ;

    String getFullType() ;

    // Could be for type or j2eeType depending on mom config
    String getType() ;

    String getName() ;

    public interface Container extends AMX {
        /** Attribute returned by getContaineeJ2EETypes().
         */
        public static final String	ATTR_CONTAINEE_J2EE_TYPES	= "ContaineeJ2EETypes";

        /** Attribute returned by getContaineeSet().
         */
        public static final String	ATTR_CONTAINEE_OBJECT_NAME_SET	= "ContaineeObjectNameSet";

        /** @return Set of String of all <i>possible</i> j2eeTypes contained within this item
         *  @see com.sun.appserv.management.base.Util#getNamesSet
         */
        // JMXA: I can only implement this in terms of currently registered objects.
        // It may change over time as objects are (de)registered.
        public Set<String>                          
            getContaineeJ2EETypes();

        /** Return a Map keyed by j2eeType. The <i>value</i> corresponding to each
         *  key (j2eeType) is another Map, as returned from {@link #getContaineeMap}.
         *  <p>
         *  If the passed Set is null, then all types are obtained.  Pass
         *  the set returned from {@link #getContaineeJ2EETypes} to get all currently
         *  present containees.
         *  
         *  @param j2eeTypes	the j2eeTypes to look for, or null for all
         *  @return Map (possibly empty) of <i>j2eeType</i>=&lt;Map of <i>name</i>=<i>AMX</i>&gt;
         */
        public <T extends AMX> Map<String,Map<String,T>>	
            getMultiContaineeMap( final Set<String> j2eeTypes );

        /** Each key in the resulting Map is a String which is the value of the 
         *  AMX.NAME_KEY for that {@link AMX}, which is the value.
         *  
         *  @param j2eeType	the j2eeType to look for
         *  @return Map <i>name</i>=<i>AMX</i>
         */
        public <T extends AMX> Map<String,T>
            getContaineeMap( final String j2eeType );

        /** Obtain the singleton MBean having the specified type.
         *  @param j2eeType
         *  @return {@link AMX} of specified j2eeType or null if not present
         *  @throws IllegalArgumentException if there is more than one item of this type
         */
        public <T extends AMX> T	            
            getContainee( final String j2eeType );

        /** @return all containees having the specified j2eeType.
         */
        public <T extends AMX> Set<T> 	    
            getContaineeSet( final String j2eeType );

        @ManagedAttribute
        @Description( "Return all containees contained in this container" ) 
        public <T extends AMX> Set<T> 	    
            getContaineeSet( );

        @ManagedOperation 
        @Description( "Return all containees having the specified j2eeTypes" ) 
        @ParameterNames( { "j2eeTypes" } ) 
        public <T extends AMX> Set<T> 	     
            getContaineeSet( final Set<String> j2eeTypes );


        @ManagedOperation
        @Description( "Return all containees with the given name, and type in the set of types" ) 
        @ParameterNames( { "j2eeTtypes", "name" } )
        public <T extends AMX> Set<T> 	                
            getByNameContaineeSet( final Set<String> j2eeTypes, final String name );

        @ManagedOperation
        @Description( "Return a single containee with the given type and name in this container" )
        @ParameterNames( { "j2eeTtypes", "name" } )
        public <T extends AMX> T	                        
            getContainee( final String j2eeType, final String name );
    }
}