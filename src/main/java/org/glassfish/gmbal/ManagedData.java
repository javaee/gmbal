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

/** This annotation defines CompositeData.   An interface or class annotated as @ManagedData
 * has a corresponding CompositeData instance constructed according to the @ManagedAttribute 
 * annotations on its methods.  All inherited annotated methods are included.
 * In the case of conflicts, the most derived method is used (that is the method
 * declared in the method 
 * closest to the class annotated as @ManagedData).
 */
@Documented 
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface ManagedData {
    /** The name of the ManagedData.  
     * <P>
     * Gmbal determines the ManagedData name as follows:
     * <ol>
     * <li>If the class has a final static field of type String with the
     * name "GMBAL_TYPE", the value of the field is the ManagedData name.
     * <li>Otherwise, if the class has an @ManagedData annotation, and the
     * value of the name is not "", the value of the name is the ManagedData name.
     * <li>Otherwise, if the package prefix of the class name matches one of
     * the type prefixes added by an stripPrefix call to the ManagedObjectManager,
     * the ManagedData name is the full class name with the matching prefix removed.
     * <li>Otherwise, if the stripPackagePrefix method was called on the
     * ManagedObjectManager, the ManagedData name is the class name without any
     * package prefixes.
     * <li>Otherwise, the ManagedData name is the class name.
     * </ol>
     *
     */
    String name() default "" ;
}

