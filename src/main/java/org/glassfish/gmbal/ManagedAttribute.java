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
package org.glassfish.gmbal ;

import java.lang.annotation.Documented ;
import java.lang.annotation.Target ;
import java.lang.annotation.ElementType ;
import java.lang.annotation.Retention ;
import java.lang.annotation.RetentionPolicy ;

/** This annotation defines an attribute in either CompositeData (ManagedData) or 
 * an open MBean (ManagedObject).  An attribute may be read/write (has a setter
 * and a getter), read only (only has a getter),
 * or write only (only has a setter) depending on the declared methods in the class.  
 * <p> 
 * A method defines a getter if it returns a non-void type and takes no argument types.
 * Likewise a method defines a setter if it return void and takes exactly one
 * argument.  
 * <p>An id is derived from a method name as follows:
 * <ol>
 * <li>If the method is a getter, and has a name of the form getXXX, the derived
 * id is xXX (note the initial lower case change).
 * <li>If the method is a getter with a boolean return type, and has a name of 
 * the form isXXX, the derived id is xXX
 * <li>If the method is a setter, and has a name of the form setXXX, the
 * detived id is xXX.
 * <li>Otherwise the derived ID is the method name.
 * </ol>
 * <p>
 * In certain cases, a field annotated with @ManagedAttribute 
 * may also represent a read-only attribute.
 * The field must be final, and its type must be one of:
 * <ol>
 * <li>A primitive type (boolean, byte, short, char, int, long, float, double)
 * <li>A primitive type wrapper (Boolean, Byte, Short, Character, Integer, 
 * Long, Float, Double)
 * <li>A String
 * <li>A BigDecimal or BigInteger
 * <li>A java.util.Date
 * <li>An ObjectName
 * <li>An enum (which is translated to its ordinal name)
 * </ol>
 * Any such field can be accessed safely by multiple threads, because its value
 * cannot change after an instance of the containing class has completed its
 * constructor.  Note that Date is not truly immutable (it should be!), but it's
 * one of the Open MBean simple types, so it is included here.
 */
@Documented 
@Target({ElementType.METHOD,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ManagedAttribute {
    /** The id of the attribute.  Defaults to value derived from method name.
     * @return The id (default "").
     */
    String id() default "" ;
}
