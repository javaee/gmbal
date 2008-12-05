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
package org.glassfish.gmbal;

import java.lang.annotation.Documented ;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;

/** Annotation to contol exactly how the type value in the ObjectName 
 * is extracted from a class when registering an instance of that class.
 * The absence of this annotation is the same as the default values.
 * Note that this is simply an application of the general @DescriptorKey
 * mechanism, but these particular metadata attributes control some of the
 * behavior of the AMX API.
 *
 * @author ken
 */
@Documented 
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface MBeanType {
    /** An explicit type name to be used for a ManagedObject.
     * @return The optional type value.
     */
    @DescriptorKey( AMX.META_TYPE )
    String type() default "" ;
    
    /** Return true if this MBean may contain other MBeans, otherwise false.
     * 
     * @return whether or not this MBean is a container.
     */
    @DescriptorKey( AMX.META_CONTAINER )
    boolean isContainer() default false ;
    
    /** True if only one MBean of this type may be created inside the same
     * parent container
     * 
     * @return
     */
    @DescriptorKey( AMX.META_SINGLETON )
    boolean isSingleton() default false ;
        
    /** True if all MBean attributes are invariant, that is, have the same
     * value for the lifetime of the MBean.  This may be used as a hint
     * to clients that the contents of the MBean can be cached.
     * 
     * @return True if all attributes of the MBean are invariant.
     */
    @DescriptorKey( AMX.META_INVARIANT_MBEANINFO )
    boolean isInvariantMBeanInfo() default true ;
    
    /** Value to use in AMX CLI pathnames that include this MBean.
     * Defaults to same value as type (whether type is obtained explicitly
     * or implicitly).
     * 
     * @return The optional pathName component.
     */
    @DescriptorKey( AMX.META_PATH_PART )
    String pathPart() default "" ;
} 
