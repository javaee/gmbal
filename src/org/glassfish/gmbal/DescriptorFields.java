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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This is taken directly from JDK 7 in order to support this feature in
 * JDK 5.
 *
 * <p>Annotation that adds fields to a Descriptor.  This can be the
 * Descriptor for an MBean, or for an attribute, operation, or constructor
 * in an MBean, or for a parameter of an operation or constructor.</p>
 *
 * <p>Consider this Standard MBean interface, for example:</p>
 *
 * <pre>
 * public interface CacheControlMBean {
 *     <b>&#64;DescriptorFields("units=bytes")</b>
 *     public long getCacheSize();
 * }
 * </pre>
 *
 * <p>When a Standard MBean is made using this interface, the usual rules
 * mean that it will have an attribute called {@code CacheSize} of type
 * {@code long}.  The {@code DescriptorFields} annotation will ensure
 * that the MBeanAttributeInfo for this attribute will have a
 * {@code Descriptor} that has a field called {@code units} with
 * corresponding value {@code bytes}.</p>
 *
 * <p>Similarly, if the interface looks like this:</p>
 *
 * <pre>
 * public interface CacheControlMBean {
 *     <b>&#64;DescriptorFields({"units=bytes", "since=1.5"})</b>
 *     public long getCacheSize();
 * }
 * </pre>
 *
 * <p>then the resulting {@code Descriptor} will contain the following
 * fields:</p>
 *
 * <table border="2">
 * <tr><th>Name</th><th>Value</th></tr>
 * <tr><td>units</td><td>"bytes"</td></tr>
 * <tr><td>since</td><td>"1.5"</td></tr>
 * </table>
 *
 * <p>The {@code @DescriptorFields} annotation can be applied to:</p>
 *
 * <ul>
 * <li>a Standard MBean or MXBean interface;
 * <li>a method in such an interface;
 * <li>a parameter of a method in a Standard MBean or MXBean interface
 * when that method is an operation (not a getter or setter for an attribute);
 * <li>a public constructor in the class that implements a Standard MBean
 * or MXBean;
 * <li>a parameter in such a constructor.
 * </ul>
 *
 * <p>Other uses of the annotation will either fail to compile or be
 * ignored.</p>
 *
 * <p>Interface annotations are checked only on the exact interface
 * that defines the management interface of a Standard MBean or an
 * MXBean, not on its parent interfaces.  Method annotations are
 * checked only in the most specific interface in which the method
 * appears; in other words, if a child interface overrides a method
 * from a parent interface, only {@code @DescriptorFields} annotations in
 * the method in the child interface are considered.
 *
 * <p>The Descriptor fields contributed in this way must be consistent
 * with each other and with any fields contributed by
 * DescriptorKey annotations.  That is, two
 * different annotations, or two members of the same annotation, must
 * not define a different value for the same Descriptor field.  Fields
 * from annotations on a getter method must also be consistent with
 * fields from annotations on the corresponding setter method.</p>
 *
 * <p>The Descriptor resulting from these annotations will be merged
 * with any Descriptor fields provided by the implementation, such as
 * the <a href="Descriptor.html#immutableInfo">{@code
 * immutableInfo}</a> field for an MBean.  The fields from the annotations
 * must be consistent with these fields provided by the implementation.</p>
 *
 * <h4>{@literal @DescriptorFields and @DescriptorKey}</h4>
 *
 * <p>The DescriptorKey annotation provides
 * another way to use annotations to define Descriptor fields.
 * <code>&#64;DescriptorKey</code> requires more work but is also more
 * robust, because there is less risk of mistakes such as misspelling
 * the name of the field or giving an invalid value.
 * <code>&#64;DescriptorFields</code> is more convenient but includes
 * those risks.  <code>&#64;DescriptorFields</code> is more
 * appropriate for occasional use, but for a Descriptor field that you
 * add in many places, you should consider a purpose-built annotation
 * using <code>&#64;DescriptorKey</code>.
 *
 * @since 1.7
 */
@Documented
@Inherited  // for @MBean and @MXBean classes
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD,
         ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DescriptorFields {
    /**
     * <p>The descriptor fields.  Each element of the string looks like
     * {@code "name=value"}.</p>
     */
    public String[] value();
}
