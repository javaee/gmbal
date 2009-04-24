/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2009 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at legal/LICENSE.TXT.
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
 * 
 */ 

package org.glassfish.gmbal ;

import java.lang.reflect.Method ;

import org.glassfish.gmbal.util.GenericConstructor ;

import javax.management.ObjectName;

/** Factory used to create ManagedObjectManager instances.
 */
public final class ManagedObjectManagerFactory {
    private ManagedObjectManagerFactory() {}
  
    // XXX How do we make this work properly with OSGi?
    private static GenericConstructor<ManagedObjectManager> objectNameCons =
        new GenericConstructor<ManagedObjectManager>( 
            ManagedObjectManager.class, 
            "org.glassfish.gmbal.impl.ManagedObjectManagerImpl",
                ObjectName.class ) ;

    
    private static GenericConstructor<ManagedObjectManager> stringCons =
        new GenericConstructor<ManagedObjectManager>( 
            ManagedObjectManager.class, 
            "org.glassfish.gmbal.impl.ManagedObjectManagerImpl",
                String.class ) ;

    /** Convenience method for getting access to a method through reflection.
     * Same as Class.getDeclaredMethod, but only throws RuntimeExceptions.
     * @param cls The class to 
     * @param name
     * @param types
     * @return
     */
    public static Method getMethod( final Class<?> cls, String name, 
        Class<?>... types ) {        
        
        try {
            return cls.getDeclaredMethod( name, types ) ;
        } catch(NoSuchMethodException exc) {
            throw new GmbalException( "Unexpected exception", exc ) ;
        } catch (SecurityException exc) {
            throw new GmbalException( "Unexpected exception", exc ) ;
        }
    }
    
    /** Create a new ManagedObjectManager.  All objectnames created will share
     * the domain value passed on this call.  This ManagedObjectManager is
     * at the top of the containment hierarchy: the parent of the root is null.
     * @param domain The domain to use for all ObjectNames created when
     * MBeans are registered.
     * @return A new ManagedObjectManager.
     */
    public static ManagedObjectManager createStandalone(
        final String domain ) {
	
        ManagedObjectManager result = stringCons.create( domain ) ;
	if (result == null) {
	    return ManagedObjectManagerNOPImpl.self ;
	} else {
	    return result ;
	}
    }
    
    /** Alternative form of the create method to be used when the
     * rootName is not needed explicitly.  If the root name is available
     * from an @ObjectNameKey annotation, it is used; otherwise the
     * type is used as the name, since the root is a singleton.
     * 
     * @param rootParentName The JMX ObjectName of the parent of the root.
     * The parent is outside of the control of this ManagedObjectManager.  
     * The ManagedObjectManager root is a child of the MBean identified
     * by the rootParentName.
     * @return The ManagedObjectManager.
     */
    public static ManagedObjectManager createFederated(
        final ObjectName rootParentName ) {
	
        ManagedObjectManager result = objectNameCons.create( rootParentName ) ;
	if (result == null) {
	    return ManagedObjectManagerNOPImpl.self ;
	} else {
	    return result ;
	}
    }
}

