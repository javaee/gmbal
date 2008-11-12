package com.sun.jmxa.impl ;

import com.sun.jmxa.Description;
import com.sun.jmxa.ManagedAttribute;
import com.sun.jmxa.ManagedObject;

@ManagedObject
@Description( "Base interface for any MBean that works in the AMX framework" ) 
public interface AMX {
    @ManagedAttribute
    @Description( "The container that contains this MBean" )
    Container getContainer() ;

    @ManagedAttribute
    @Description( "Return the type (or j2eeType) of this MBean" )
    String getType() ;

    @ManagedAttribute
    @Description( "Return the name of this MBean.")
    String getName() ;
}