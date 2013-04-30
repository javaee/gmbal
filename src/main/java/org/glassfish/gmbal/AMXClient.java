/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *  
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *  
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *  
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *  
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */ 

package org.glassfish.gmbal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBeanInfo;


/** This class implements a generic AMXMBeanInterface MBean which is connected to a possibly
 * remote MBeanServerConnection (note that MBeanServer isA MBeanServerConnection,
 * so we can actually create an AMXClientImpl simply by using the MBeanServer
 * from the mom: this is useful for testing).
 * <P>
 * Note that this version of the AMXMBeanInterface API provides a generic get/set API that
 * is identical to DynamicMBean, except that it only throws unchecked exceptions.
 * This is far more convenient in practice than the JMX-standard checked exceptions.
 *
 * @author ken
 */
public class AMXClient implements AMXMBeanInterface {
    private static ObjectName makeObjectName( String str ) {
        try {
            return new ObjectName(str);
        } catch (MalformedObjectNameException ex) {
            return null ;
        }
    }

    /** Special object name used to represent a NULL objectName result.
     */
    public static final ObjectName NULL_OBJECTNAME = makeObjectName(
        "null:type=Null,name=Null" ) ;

    private MBeanServerConnection server ;
    private ObjectName oname ;

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true ;
        }

        if (!(obj instanceof AMXClient)) {
            return false ;
        }

        AMXClient other = (AMXClient)obj ;

        return oname.equals( other.oname ) ;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.oname != null ? this.oname.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "AMXClient[" + oname + "]" ;
    }

    private <T> T fetchAttribute( String name, Class<T> type ) {
        try {
            Object obj = server.getAttribute( oname, name ) ;
            if (NULL_OBJECTNAME.equals( obj )) {
                return null ;
            } else {
                return type.cast( obj ) ;
            }
        } catch (JMException exc) {
            throw new GmbalException( "Exception in fetchAttribute", exc ) ;
        } catch (IOException exc) {
            throw new GmbalException( "Exception in fetchAttribute", exc ) ;
        }
    }

    public AMXClient( MBeanServerConnection server,
        ObjectName oname ) {
        this.server = server ;
        this.oname = oname ;
    }

    private AMXClient makeAMX( ObjectName on ) {
        if (on == null) {
            return null ;
        }
        return new AMXClient( this.server, on ) ;
    }

    public String getName() {
        return fetchAttribute( "Name", String.class )  ;
    }

    public Map<String,?> getMeta() {
        try {
            ModelMBeanInfo mbi = (ModelMBeanInfo) server.getMBeanInfo( oname );
            Descriptor desc = mbi.getMBeanDescriptor() ;
            Map<String,Object> result = new HashMap<String,Object>() ;
            for (String str : desc.getFieldNames()) {
                result.put( str, desc.getFieldValue( str )) ;
            }
            return result ;
        } catch (MBeanException ex) {
            throw new GmbalException( "Exception in getMeta", ex ) ;
        } catch (RuntimeOperationsException ex) {
            throw new GmbalException( "Exception in getMeta", ex ) ;
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in getMeta", ex ) ;
        } catch (IntrospectionException ex) {
            throw new GmbalException( "Exception in getMeta", ex ) ;
        } catch (ReflectionException ex) {
            throw new GmbalException( "Exception in getMeta", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in getMeta", ex ) ;
        }
    }

    public AMXClient getParent() {
        ObjectName res  = fetchAttribute( "Parent", ObjectName.class ) ;
        return makeAMX( res ) ;
    }

    public AMXClient[] getChildren() {
        ObjectName[] onames = fetchAttribute( "Children",
            ObjectName[].class ) ;
        return makeAMXArray( onames ) ;
    }

    private AMXClient[] makeAMXArray( ObjectName[] onames ) {
        AMXClient[] result = new AMXClient[onames.length] ;
        int ctr=0 ;
        for (ObjectName on : onames ) {
            result[ctr++] = makeAMX( on ) ;
        }

        return result ;
    }

    public Object getAttribute(String attribute) {
        try {
            return server.getAttribute(oname, attribute);
        } catch (MBeanException ex) {
            throw new GmbalException( "Exception in getAttribute", ex ) ;
        } catch (AttributeNotFoundException ex) {
            throw new GmbalException( "Exception in getAttribute", ex ) ;
        } catch (ReflectionException ex) {
            throw new GmbalException( "Exception in getAttribute", ex ) ;
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in getAttribute", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in getAttribute", ex ) ;
        }
    }

    public void setAttribute(String name, Object value ) {
        Attribute attr = new Attribute( name, value ) ;
        setAttribute( attr ) ;
    }

    public void setAttribute(Attribute attribute) {
        try {
            server.setAttribute(oname, attribute);
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in setAttribute", ex ) ;
        } catch (AttributeNotFoundException ex) {
            throw new GmbalException( "Exception in setAttribute", ex ) ;
        } catch (InvalidAttributeValueException ex) {
            throw new GmbalException( "Exception in setAttribute", ex ) ;
        } catch (MBeanException ex) {
            throw new GmbalException( "Exception in setAttribute", ex ) ;
        } catch (ReflectionException ex) {
            throw new GmbalException( "Exception in setAttribute", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in setAttribute", ex ) ;
        }
    }

    public AttributeList getAttributes(String[] attributes) {
        try {
            return server.getAttributes(oname, attributes);
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in getAttributes", ex ) ;
        } catch (ReflectionException ex) {
            throw new GmbalException( "Exception in getAttributes", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in getAttributes", ex ) ;
        }
    }

    public AttributeList setAttributes(AttributeList attributes) {
        try {
            return server.setAttributes(oname, attributes);
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in setAttributes", ex ) ;
        } catch (ReflectionException ex) {
            throw new GmbalException( "Exception in setAttributes", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in setAttributes", ex ) ;
        }
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
        throws MBeanException, ReflectionException {
        try {
            return server.invoke(oname, actionName, params, signature);
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in invoke", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in invoke", ex ) ;
        }
    }

    public MBeanInfo getMBeanInfo() {
        try {
            return server.getMBeanInfo(oname);
        } catch (InstanceNotFoundException ex) {
            throw new GmbalException( "Exception in invoke", ex ) ;
        } catch (IntrospectionException ex) {
            throw new GmbalException( "Exception in invoke", ex ) ;
        } catch (ReflectionException ex) {
            throw new GmbalException( "Exception in invoke", ex ) ;
        } catch (IOException ex) {
            throw new GmbalException( "Exception in invoke", ex ) ;
        }
    }

    public ObjectName objectName() {
        return oname ;
    }
}
