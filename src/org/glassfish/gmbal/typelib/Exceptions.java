/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.ParameterizedType;
import org.glassfish.gmbal.logex.Chain;
import org.glassfish.gmbal.logex.ExceptionWrapper;
import org.glassfish.gmbal.logex.Log;
import org.glassfish.gmbal.logex.Message;
import org.glassfish.gmbal.logex.WrapperGenerator;

/**
 *
 * @author ken
 */
@ExceptionWrapper( idPrefix="GMBALTLIB",
    resourceBundle = "org.glassfish.gmbal.logex.LogStrings" )
public interface Exceptions {
    static final Exceptions self = WrapperGenerator.makeWrapper(
        Exceptions.class ) ;

    // Allow 100 exceptions per class
    static final int EXCEPTIONS_PER_CLASS = 100 ;

// TypeEvaluator
    static final int TYPE_EVALUATOR_START = 1 ;

    @Message( "Internal error in TypeEvaluator" )
    @Log( id=TYPE_EVALUATOR_START + 0 )
    IllegalStateException internalTypeEvaluatorError( @Chain Exception exc ) ;

    @Message( "evaluateType should not be called with a Method ({0})" )
    @Log( id=TYPE_EVALUATOR_START + 1 )
    IllegalArgumentException evaluateTypeCalledWithMethod( Object type ) ;

    @Message( "evaluateType should not be called with an unknown type ({0})" )
    @Log( id=TYPE_EVALUATOR_START + 2 )
    IllegalArgumentException evaluateTypeCalledWithUnknownType( Object type ) ;

    @Message( "Multiple upper bounds not supported on {0}" )
    @Log( id=TYPE_EVALUATOR_START + 3 )
    UnsupportedOperationException multipleUpperBoundsNotSupported(
        Object type ) ;

    @Message( "Type list and TypeVariable list are not the same length for {0}" )
    @Log( id=TYPE_EVALUATOR_START + 4 )
    IllegalArgumentException listsNotTheSameLengthInParamType(
        ParameterizedType pt ) ;
}
