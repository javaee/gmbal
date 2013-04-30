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

import java.lang.annotation.Documented ;
import java.lang.annotation.Inherited ;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;

import org.glassfish.external.amx.AMX ;

/** Annotation to contol exactly how the type value in the ObjectName 
 * is extracted from a class when registering an instance of that class.
 * The absence of this annotation is the same as the default values.
 * Note that this is simply an application of the general @DescriptorKey
 * mechanism, but these particular metadata attributes control some of the
 * behavior of the AMXMBeanInterface API.
 * <p>Note that supportsAdoption is not included here, because that attribute
 * is always false for gmbal.
 *
 * @author ken
 */
@Documented 
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AMXMetadata {
    /** True if only one MBean of this type may be created inside the same
     * parent container
     *
     * @return whether or not this MBean must be the only one of its type.
     */
    @DescriptorKey( AMX.DESC_IS_SINGLETON )
    boolean isSingleton() default false ;

    /** String denoting classification of MBean.  Predefined values are
     * configuration, monitoring, jsr77, utility, and other.
     * @return The group type.
     */
    @DescriptorKey( AMX.DESC_GROUP )
    String group() default "other" ;

    /** Return the list of types that are legal as types of children of this
     * type.  If unknown, must be an empty array.
     * @return Array of child types
     */
    @DescriptorKey( AMX.DESC_SUB_TYPES )
    String[] subTypes() default {} ;

    /** Return the generic AMXMBeanInterface interface to be used.
     * @return name of interface to use.
     */
    @DescriptorKey( AMX.DESC_GENERIC_INTERFACE_NAME )
    String genericInterfaceName() default "org.glassfish.admin.amx.core.AMXProxy" ;

    /** True if the MBeanInfo is invariant, that is, has the same
     * value for the lifetime of the MBean.  This may be used as a hint
     * to clients that the MBeanInfo can be cached.
     *
     * @return True if the MBeanInfo is invariant
     */
    @DescriptorKey( AMX.DESC_STD_IMMUTABLE_INFO )
    boolean immutableInfo() default true ;

    /** Defines the name of the interface to use when generating a proxy
     * for this class.  Defaults to a generic interface.
     * @return The interface class name for a proxy.
     */
    @DescriptorKey( AMX.DESC_STD_INTERFACE_NAME )
    String interfaceClassName() default "" ;

    /** An explicit type to use for the MBean.  
     * <p>
     * Note that this is NOT part of the AMXMBeanInterface-defined metadata, but gmbal
     * needs it here to have a place to override the type.
     * <p>
     * Gmbal determines the type name as follows:
     * <ol>
     * <li>If the class has a final static field of type String with the
     * name "AMX_TYPE", the value of the field is the type name.
     * <li>Otherwise, if the class has an @AMXMetadata annotations, and the
     * value of the type is not "", the value of the type is the type name.
     * <li>Otherwise, if the package prefix of the class name matches one of
     * the type prefixes added by an stripPrefix call to the ManagedObjectManager,
     * the type name is the full class name with the matching prefix removed.
     * <li>Otherwise, if the stripPackagePrefix method was called on the
     * ManagedObjectManager, the type name is the class name without any
     * package prefixes.
     * <li>Otherwise, the type name is the class name.
     * </ol>
     * @return The type for this MBean.
     */
    @DescriptorKey( "type" )
    String type() default "" ;
} 
