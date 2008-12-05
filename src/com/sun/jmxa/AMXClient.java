/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2007 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
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
 */

package com.sun.jmxa;

import com.sun.jmxa.AMX;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBeanInfo;

/** This class implements a generic AMX MBean which is connected to a possibly
 * remote MBeanServerConnection (note that MBeanServer isA MBeanServerConnection,
 * so we can actually create an AMXClientImpl simply by using the MBeanServer
 * from the mom: this is useful for testing).
 *
 * XXX This points to a weakness in the AMX interface: it would be far more
 * convenient to provide a generic set/get/invoke API that does NOT throw checked
 * exceptions, rather than inheriting them from the DynamicMBean API.  Note also
 * that DynamicMBean is intended for local use, and so is missing some required
 * exceptions when proxied by MBeanServerConnection.
 *
 * @author ken
 */
public class AMXClient implements AMX, DynamicMBean {
    private MBeanServerConnection server ;
    private ObjectName oname ;

    private <T> T fetchAttribute( String name, Class<T> type ) {
        try {
            return type.cast( server.getAttribute( oname, name ) ) ;
        } catch (JMException exc) {
            throw new RuntimeException( exc ) ;
        } catch (IOException exc) {
            throw new RuntimeException( exc ) ;
        }
    }

    public AMXClient( MBeanServerConnection server,
        ObjectName oname ) {
        this.server = server ;
        this.oname = oname ;
    }

    private AMXClient makeAMX( ObjectName on ) {
        return new AMXClient( this.server, on ) ;
    }

    public String getName() {
        return fetchAttribute( "getName", String.class )  ;
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
            throw new RuntimeException( ex ) ;
        } catch (RuntimeOperationsException ex) {
            throw new RuntimeException( ex ) ;
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IntrospectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (ReflectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public AMX getContainer() {
        ObjectName res  = fetchAttribute( "getContainer", ObjectName.class ) ;
        return makeAMX( res ) ;
    }

    public AMX[] getContained() {
        ObjectName[] onames = fetchAttribute( "getContained", 
            ObjectName[].class ) ;
        return makeAMXArray( onames ) ;
    }

    private AMX[] makeAMXArray( ObjectName[] onames ) {
        AMX[] result = new AMX[onames.length] ;
        int ctr=0 ;
        for (ObjectName on : onames ) {
            result[ctr++] = makeAMX( on ) ;
        }

        return result ;
    }

    public AMX[] getContained(String type) {
        String[] sig = { String.class.getName() } ;
        Object[] args = { type } ;
        try {
            ObjectName[] onames = (ObjectName[]) server.invoke( oname,
                "getContainer", args, sig );
            return makeAMXArray( onames ) ;
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (MBeanException ex) {
            throw new RuntimeException( ex ) ;
        } catch (ReflectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public Object getAttribute(String attribute)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        try {
            return server.getAttribute(oname, attribute);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException,
        MBeanException, ReflectionException {
        try {
            server.setAttribute(oname, attribute);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public AttributeList getAttributes(String[] attributes) {
        try {
            return server.getAttributes(oname, attributes);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (ReflectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public AttributeList setAttributes(AttributeList attributes) {
        try {
            return server.setAttributes(oname, attributes);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (ReflectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
        throws MBeanException, ReflectionException {
        try {
            return server.invoke(oname, actionName, params, signature);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    public MBeanInfo getMBeanInfo() {
        try {
            return server.getMBeanInfo(oname);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IntrospectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (ReflectionException ex) {
            throw new RuntimeException( ex ) ;
        } catch (IOException ex) {
            throw new RuntimeException( ex ) ;
        }
    }

    private ObjectName[] fetchAttribute(String string, ObjectName[] objectName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
