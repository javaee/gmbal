package com.sun.jmxa.impl;

import com.sun.jmxa.Description;
import com.sun.jmxa.ManagedAttribute;
import com.sun.jmxa.ManagedOperation;
import com.sun.jmxa.ParameterNames;
import java.util.Map;
import java.util.Set;

@Description(value = "Base interface for an AMX MBean that implements " 
    + "a container of other AMX MBeans")
public interface Container extends AMX {
    public static final String ATTR_CONTAINEE_J2EE_TYPES = 
        "ContaineeJ2EETypes";
    
    public static final String ATTR_CONTAINEE_OBJECT_NAME_SET = 
        "ContaineeObjectNameSet";

    @ManagedAttribute(id = ATTR_CONTAINEE_J2EE_TYPES)
    @Description("The set of all types contained in this container")
    public Set<String> getContaineeJ2EETypes();

    @ManagedOperation
    @ParameterNames({"j2eeTypes"})
    @Description("Returns a map from type name to a map from name to "
        + " mbean for all MBeans contained in this container")
    public Map<String, Map<String, AMX>> 
        getMultiContaineeMap(final Set<String> j2eeTypes );

    @ManagedOperation
    @ParameterNames({"j2eeType"})
    @Description("Returns a map from name to MBean of all containees " 
        + "with the given j2eeType")
    public Map<String, AMX> getContaineeMap(final String j2eeType );

    @ManagedOperation
    @ParameterNames("j2eeType")
    @Description("Return the single containee of the given j2eeType")
    public AMX getContainee(final String j2eeType);

    @ManagedOperation(id=ATTR_CONTAINEE_OBJECT_NAME_SET)
    @ParameterNames("j2eeType")
    @Description(ATTR_CONTAINEE_OBJECT_NAME_SET)
    public Set<AMX> getContaineeSet(final String j2eeType);

    @ManagedAttribute
    @Description("Return all containees contained in this container")
    public Set<AMX> getContaineeSet();

    @ManagedOperation
    @Description("Return all containees having the specified j2eeTypes")
    @ParameterNames({"j2eeTypes"})
    public Set<AMX> getContaineeSet(final Set<String> j2eeTypes);

    @ManagedOperation
    @Description("Return all containees with the given name, and type " 
        + "in the set of types")
    @ParameterNames({"j2eeTtypes", "name"})
    public Set<AMX> getByNameContaineeSet(final Set<String> j2eeTypes,
        final String name);

    @ManagedOperation
    @Description("Return a single containee with the given type and " 
        + "name in this container")
    @ParameterNames({"j2eeTtypes", "name"})
    public AMX getContainee(final String j2eeType, final String name);
}
