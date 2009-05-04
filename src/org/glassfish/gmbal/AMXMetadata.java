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
 * <p>Note that supportsAdoption is not included here, because that attribute
 * is always false for gmbal.
 *
 * @author ken
 */
@Documented 
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface AMXMetadata {
    /** True if only one MBean of this type may be created inside the same
     * parent container
     *
     * @return
     */
    @DescriptorKey( AMX.META_SINGLETON )
    boolean isSingleton() default false ;

    /** String denoting classification of MBean.  Predefined values are
     * configuration, monitoring, jsr77, utility, and other.
     * @return The group type.
     */
    @DescriptorKey( AMX.META_GROUP_TYPE )
    String group() default "other" ;

    /** Return the list of types that are legal as types of children of this
     * type.  If unknown, must be an empty array.  Not used is isLeaf is true.
     * @return Array of child types
     */
    @DescriptorKey( AMX.META_SUB_TYPES )
    String[] subTypes() default {} ;

    /** Return the generic AMX interface to be used.
     */
    @DescriptorKey( AMX.META_GENERIC_INTERFACE_NAME )
    String genericInterfaceName() default "" ;

    /** True if the MBeanInfo is invariant, that is, has the same
     * value for thehttp://jpgserv.red.iplanet.com/webcasts/corba/internals-vol1/CorbaTraining.html lifetime of the MBean.  This may be used as a hint
     * to clients that the MBeanInfo can be cached.
     *
     * @return True if the MBeanInfo is invarianthttp://jpgserv.red.iplanet.com/webcasts/corba/internals-vol1/CorbaTraining.htmlhttp://jpgserv.red.iplanet.com/webcasts/corba/internals-vol1/CorbaTraining.html.
     */
    @DescriptorKey( AMX.META_MBEANINFO_INVARIANT )
    boolean immutableInfo() default true ;

    /** Defines the name of the interface to use when generating a proxy
     * for this class.  Defaults to a generic interface.
     * @return
     */
    @DescriptorKey( AMX.META_INTERFACE_NAME )
    String interfaceClassName() default "" ;

    /** An explicit type to use for the MBean.  The default is derived from
     * the class name.
     * <p>Note that this is NOT part of the AMX-defined metadata, but gmbal
     * needs it here to have a place to override the type.  This could also
     * be specified on the @ManagedObject annotation, but the metadata seems
     * a better choice.
     * @return The type for this MBean.
     */
    @DescriptorKey( AMX.META_TYPE )
    String type() default "" ;
} 
