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

package com.sun.jmxa ;

import java.util.Properties ;
import java.util.ResourceBundle ;

import javax.management.ObjectName ;
import javax.management.MBeanServer ;

/** An interface used to managed Open MBeans created from annotated
 * objects.  This is mostly a facade over MBeanServer.
 */
public interface ManagedObjectManager {
    /** Construct an Open Mean for obj according to its annotations,
     * and register it with domain getDomain() and the key/value pairs
     * given by props in the form key=value.  The MBeanServer from 
     * setMBeanServer (or its default) is used.
     */
    void register( Object obj, String... props ) ;

    /** Same as register( Object, String...) except that key/value
     * pairs are given as properties.
     */
    void register( Object obj, Properties props )  ;

    /** Same as register( Object, String...) except that key/value
     * pairs are given as properties.
     */
    void register( Object obj, Map<String,String> props ) ;

    /** Unregister the Open MBean corresponding to obj from the
     * mbean server.
     */
    void unregister( Object obj ) ;

    /** Get the ObjectName for the given object (which must have
     * been registered via a register call).
     */
    ObjectName getObjectName( Object obj ) ;

    /** Get the Object that was registered with the given ObjectName.
     */
    Object getObject( ObjectName oname ) ;

    /** Return the domain name that was used when this ManagedObjectManager
     * was created.
     */
    String getDomain() ;

    /** Set the MBeanServer to which all MBeans using this interface
     * are published.  The default value is 
     * java.lang.management.ManagementFactory.getPlatformMBeanServer().
     */
    void setMBeanServer( MBeanServer server ) ;

    MBeanServer getMBeanServer() ;

    /** Set the ResourceBundle to use for getting localized descriptions.
     * If not set, the description is the value in the annotation.
     */
    void setResourceBundle( ResourceBundle rb ) ;

    ResourceBundle getResourceBundle() ;
}
