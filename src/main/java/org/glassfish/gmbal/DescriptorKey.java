/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2005-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;

/** This is taken directly from JDK 7 in order to support this feature in
 * JDK 5.
 *
 * <p>Meta-annotation that describes how an annotation element relates
 * to a field in a Descriptor.  This can be the Descriptor for
 * an MBean, or for an attribute, operation, or constructor in an
 * MBean, or for a parameter of an operation or constructor.</p>
 *
 * <p>(The DescriptorFields annotation
 * provides another way to add fields to a {@code Descriptor}.  See
 * the documentation for that annotation for a comparison of the
 * two possibilities.)</p>
 *
 * <p>Consider this annotation for example:</p>
 *
 * <pre>
 * &#64;Documented
 * &#64;Target(ElementType.METHOD)
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public &#64;interface Units {
 *     <b>&#64;DescriptorKey("units")</b>
 *     String value();
 * }
 * </pre>
 *
 * <p>and this use of the annotation:</p>
 *
 * <pre>
 * public interface CacheControlMBean {
 *     <b>&#64;Units("bytes")</b>
 *     public long getCacheSize();
 * }
 * </pre>
 *
 * <p>When a Standard MBean is made from the {@code CacheControlMBean},
 * the usual rules mean that it will have an attribute called
 * {@code CacheSize} of type {@code long}.  The {@code @Units}
 * annotation, given the above definition, will ensure that the
 * MBeanAttributeInfo for this attribute will have a
 * {@code Descriptor} that has a field called {@code units} with
 * corresponding value {@code bytes}.</p>
 *
 * <p>Similarly, if the annotation looks like this:</p>
 *
 * <pre>
 * &#64;Documented
 * &#64;Target(ElementType.METHOD)
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public &#64;interface Units {
 *     <b>&#64;DescriptorKey("units")</b>
 *     String value();
 *
 *     <b>&#64;DescriptorKey("descriptionResourceKey")</b>
 *     String resourceKey() default "";
 *
 *     <b>&#64;DescriptorKey("descriptionResourceBundleBaseName")</b>
 *     String resourceBundleBaseName() default "";
 * }
 * </pre>
 *
 * <p>and it is used like this:</p>
 *
 * <pre>
 * public interface CacheControlMBean {
 *     <b>&#64;Units("bytes",
 *            resourceKey="bytes.key",
 *            resourceBundleBaseName="com.example.foo.MBeanResources")</b>
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
 * <tr><td>descriptionResourceKey</td><td>"bytes.key"</td></tr>
 * <tr><td>descriptionResourceBundleBaseName</td>
 *     <td>"com.example.foo.MBeanResources"</td></tr>
 * </table>
 *
 * <p>An annotation such as {@code @Units} can be applied to:</p>
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
 * <p>Other uses of the annotation are ignored.</p>
 *
 * <p>Interface annotations are checked only on the exact interface
 * that defines the management interface of a Standard MBean or an
 * MXBean, not on its parent interfaces.  Method annotations are
 * checked only in the most specific interface in which the method
 * appears; in other words, if a child interface overrides a method
 * from a parent interface, only {@code @DescriptorKey} annotations in
 * the method in the child interface are considered.
 *
 * <p>The Descriptor fields contributed in this way by different
 * annotations on the same program element must be consistent with
 * each other and with any fields contributed by a 
 * DescriptorFields annotation.  That is, two
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
 * <p>An annotation element to be converted into a descriptor field
 * can be of any type allowed by the Java language, except an annotation
 * or an array of annotations.  The value of the field is derived from
 * the value of the annotation element as follows:</p>
 *
 * <table border="2">
 * <tr><th>Annotation element</th><th>Descriptor field</th></tr>
 * <tr><td>Primitive value ({@code 5}, {@code false}, etc)</td>
 *     <td>Wrapped value ({@code Integer.valueOf(5)},
 *         {@code Boolean.FALSE}, etc)</td></tr>
 * <tr><td>Class constant (e.g. {@code Thread.class})</td>
 *     <td>Class name from Class.getName()
 *         (e.g. {@code "java.lang.Thread"})</td></tr>
 * <tr><td>Enum constant (e.g. ElementType.FIELD)</td>
 *     <td>Constant name from Enum.name()
 *         (e.g. {@code "FIELD"})</td></tr>
 * <tr><td>Array of class constants or enum constants</td>
 *     <td>String array derived by applying these rules to each
 *         element</td></tr>
 * <tr><td>Value of any other type<br>
 *         ({@code String}, {@code String[]}, {@code int[]}, etc)</td>
 *     <td>The same value</td></tr>
 * </table>
 *
 * @since 1.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface DescriptorKey {
    String value();

    /**
     * <p>Do not include this field in the Descriptor if the annotation
     * element has its default value.  For example, suppose {@code @Units} is
     * defined like this:</p>
     *
     * <pre>
     * &#64;Documented
     * &#64;Target(ElementType.METHOD)
     * &#64;Retention(RetentionPolicy.RUNTIME)
     * public &#64;interface Units {
     *     &#64;DescriptorKey("units")
     *     String value();
     *
     *     <b>&#64;DescriptorKey(value = "descriptionResourceKey",
     *                    omitIfDefault = true)</b>
     *     String resourceKey() default "";
     *
     *     <b>&#64;DescriptorKey(value = "descriptionResourceBundleBaseName",
     *                    omitIfDefault = true)</b>
     *     String resourceBundleBaseName() default "";
     * }
     * </pre>
     *
     * <p>Then consider a usage such as {@code @Units("bytes")} or
     * {@code @Units(value = "bytes", resourceKey = "")}, where the
     * {@code resourceKey} and {@code resourceBundleBaseNames} elements
     * have their default values.  In this case the Descriptor resulting
     * from these annotations will not include a {@code descriptionResourceKey}
     * or {@code descriptionResourceBundleBaseName} field.</p>
     */
    boolean omitIfDefault() default false;
}
