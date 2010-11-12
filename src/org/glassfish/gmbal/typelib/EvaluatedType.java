/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2001-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gmbal.typelib;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import javax.management.ObjectName;

/**
 *
 * @author ken
 */
public interface EvaluatedType {
    // Constants for commonly used types.
    // Do NOT include generics like Class here, as they WILL pull in hundreds
    // of classes into the initialization of typelib.
    public static final EvaluatedClassDeclaration EVOID           = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( void.class ) ;
    public static final EvaluatedClassDeclaration EINT            = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( int.class ) ;
    public static final EvaluatedClassDeclaration EINTW           = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Integer.class ) ;
    public static final EvaluatedClassDeclaration EBYTE           = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( byte.class ) ;
    public static final EvaluatedClassDeclaration EBYTEW          = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Byte.class ) ;
    public static final EvaluatedClassDeclaration ECHAR           = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( char.class ) ;
    public static final EvaluatedClassDeclaration ECHARW          = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Character.class ) ;
    public static final EvaluatedClassDeclaration ESHORT          = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( short.class ) ;
    public static final EvaluatedClassDeclaration ESHORTW         = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Short.class ) ;
    public static final EvaluatedClassDeclaration EBOOLEAN        = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( boolean.class ) ;
    public static final EvaluatedClassDeclaration EBOOLEANW       = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Boolean.class ) ;
    public static final EvaluatedClassDeclaration EFLOAT          = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( float.class ) ;
    public static final EvaluatedClassDeclaration EFLOATW         = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Float.class ) ;
    public static final EvaluatedClassDeclaration EDOUBLE         = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( double.class ) ;
    public static final EvaluatedClassDeclaration EDOUBLEW        = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Double.class ) ;
    public static final EvaluatedClassDeclaration ELONG           = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( long.class ) ;
    public static final EvaluatedClassDeclaration ELONGW          = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Long.class ) ;
    public static final EvaluatedClassDeclaration EBIG_DECIMAL    = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( BigDecimal.class ) ;
    public static final EvaluatedClassDeclaration EBIG_INTEGER    = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( BigInteger.class ) ;
    public static final EvaluatedClassDeclaration EDATE           = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Date.class ) ;
    public static final EvaluatedClassDeclaration EOBJECT_NAME    = 
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( ObjectName.class ) ;
    public static final EvaluatedClassDeclaration ESTRING         =
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( String.class ) ;
    public static final EvaluatedClassDeclaration EOBJECT         =
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Object.class ) ;
    public static final EvaluatedClassDeclaration ENUMBER          =
        (EvaluatedClassDeclaration)TypeEvaluator.getEvaluatedType( Number.class ) ;

    <R> R accept( Visitor<R> visitor ) ;

    /** Returns true if the type is immutable.
     *
     */
    boolean isImmutable() ;

    String name();
}
