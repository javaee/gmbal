/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.impl;

/**
 *
 * @author ken
 */
public class AMXImpl implements AMX {
    protected MBeanImpl mbean ;
        
    public AMXImpl( final MBeanImpl mb ) {
        this.mbean = mb ;
    }
    
    public Container getContainer() {
        MBeanImpl parent = mbean.parent() ;
        if (parent != null) {
            return parent.facet( Container.class, false ) ;
        } else {
            // XXX Need to get to rootParentName
            return null ;
        }
    }

    public String getType() {
        return mbean.type() ;
    }

    public String getName() {
        return mbean.name() ;
    }

}
