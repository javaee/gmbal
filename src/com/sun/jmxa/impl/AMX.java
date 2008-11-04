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
    @Description( "The container that contains this MBean" )
    Container getContainer() ;

    // not sure what getDomainRoot would mean?
    
    enum GroupType { Configuration, Monitoring, Utilitiy, JSR77, Other } ;
    
    // Do we need/want this?  I'll add a JMXA annotation option if we do.
    @ManagedAttribute
    @Description( "Returns the group classification of this MBean")
    GroupType getGroup() ;

    @ManagedAttribute
    @Description( "Returns the fully-qualified type of this MBean")
    String getFullType() ;

    @ManagedAttribute
    @Description( "Return the type (or j2eeType) of this MBean" )
    String getType() ;

    @ManagedAttribute
    @Description( "Return the name of this MBean.")
    String getName() ;

    @Description( "Base interface for an AMX MBean that implements "
	+ "a container of other AMX MBeans" ) 
    public interface Container extends AMX {
        /** Attribute returned by getContaineeJ2EETypes().
         */
        public static final String	ATTR_CONTAINEE_J2EE_TYPES	= 
            "ContaineeJ2EETypes";

        /** Attribute returned by getContaineeSet().
         */
        public static final String	ATTR_CONTAINEE_OBJECT_NAME_SET	= 
            "ContaineeObjectNameSet";

        // JMXA: I can only implement this in terms of currently registered.
        // objects.  It may change over time as objects are (de)registered.
        @ManagedAttribute( id = ATTR_CONTAINEE_J2EE_TYPES )
        @Description( "The set of all types contained in this container")
        public Set<String>                          
            getContaineeJ2EETypes();

        @ManagedOperation
        @ParameterNames( { "j2eeTypes" } )
        @Description( "Returns a map from type name to a map from name to "
            + " mbean for all MBeans contained in this container" )
        public <T extends AMX> Map<String,Map<String,T>>	
            getMultiContaineeMap( final Set<String> j2eeTypes );

        @ManagedOperation
        @ParameterNames( { "j2eeType" } )
        @Description( "Returns a map from name to MBean of all containees " +
            "with the given j2eeType" )
        public <T extends AMX> Map<String,T>
            getContaineeMap( final String j2eeType );

        /** Obtain the singleton MBean having the specified type.
         *  @param j2eeType The type of the containee
         *  @return MBean of specified j2eeType or null if not present.
         *  @throws IllegalArgumentException if there is more than one item 
         *  of this type.
         */
        @ManagedOperation
        @ParameterNames( "j2eeType" ) 
        @Description( "Return the single containee of the given j2eeType")
        public <T extends AMX> T	            
            getContainee( final String j2eeType );

        @ManagedOperation
        @ParameterNames( "j2eeType" )
        @Description( ATTR_CONTAINEE_OBJECT_NAME_SET )
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
        @Description( "Return all containees with the given name, and type "
            + "in the set of types" ) 
        @ParameterNames( { "j2eeTtypes", "name" } )
        public <T extends AMX> Set<T> 	                
            getByNameContaineeSet( final Set<String> j2eeTypes, 
                final String name );

        @ManagedOperation
        @Description( "Return a single containee with the given type and " 
            + "name in this container" )
        @ParameterNames( { "j2eeTtypes", "name" } )
        public <T extends AMX> T	                        
            getContainee( final String j2eeType, final String name );
    }
}