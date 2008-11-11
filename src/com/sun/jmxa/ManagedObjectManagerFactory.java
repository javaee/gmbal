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

import java.lang.reflect.Method ;

import com.sun.jmxa.util.GenericConstructor ;

/** Factory used to create ManagedObjectManager instances.
 */
public final class ManagedObjectManagerFactory {
    private ManagedObjectManagerFactory() {}
    
    private static GenericConstructor<ManagedObjectManager> cons =
        new GenericConstructor<ManagedObjectManager>( 
            ManagedObjectManager.class, 
            "com.sun.jmxa.impl.ManagedObjectManagerImpl",
                String.class, String.class, Object.class, String.class ) ;

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
            throw new IllegalArgumentException( exc ) ;
        } catch (SecurityException exc) {
            throw new IllegalArgumentException( exc ) ;
        }
    }
    
    /** Create a new ManagedObjectManager.  All objectnames created will share
     * the domain value passed on this call.
     * @param mode 
     * @param domain The domain to use for all ObjectNames created when
     * MBeans are registered.
     * @param rootParentName The comma-separated list of name=value pairs
     * that represents the parent of the root.  The parent is outside of
     * the control of this ManagedObjectManager.  This means that the root is
     * a child of this parent, as represented by the rootParentName.  This 
     * may be an empty string if the root has no parent.
     * @param rootObject The root managed object to be used as the root of
     * all MBeans created by this ManagedObjectManager.
     * @param rootName The name to be used for the root object.
     * @return A new ManagedObjectManager.
     */
    public static ManagedObjectManager create( 
        final String domain, final String rootParentName,
        final Object rootObject, final String rootName ) {
	
        return cons.create( domain, rootParentName, rootObject, rootName ) ;
    }
    
    /** Alternative form of the create method to be used when the
     * rootName is not needed explicitly.  If the root name is available
     * from an @ObjectNameKey annotation, it is used; otherwise the
     * type is used as the name, since the root is a singleton.
     * 
     * @param mode 
     * @param domain The domain to use for all ObjectNames created when
     * MBeans are registered.
     * @param rootParentName The comma-separated list of name=value pairs
     * that represents the parent of the root.  The parent is outside of
     * the control of this ManagedObjectManager.  This means that the root is
     * a child of this parent, as represented by the rootParentName.
     * @param rootObject The root managed object to be used as the root of
     * all MBeans created by this ManagedObjectManager.
     * @return The ManagedObjectManager.
     */
    public static ManagedObjectManager create( 
        final String domain, final String rootParentName,
        final Object rootObject ) {
	
        return create( domain, rootParentName, rootObject, null ) ;
    }
}

