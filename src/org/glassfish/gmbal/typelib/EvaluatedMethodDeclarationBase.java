/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright 2001-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License. You can obtain
 *  a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 *  or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  Sun designates this particular file as subject to the "Classpath" exception
 *  as provided by Sun in the GPL Version 2 section of the License file that
 *  accompanied this code.  If applicable, add the following below the License
 *  Header, with the fields enclosed by brackets [] replaced by your own
 *  identifying information: "Portions Copyrighted [year]
 *  [name of copyright owner]"
 * 
 *  Contributor(s):
 * 
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

import java.util.List;
import org.glassfish.gmbal.generic.ObjectSet;

/**
 *
 * @author ken
 */
public abstract class EvaluatedMethodDeclarationBase 
    extends EvaluatedDeclarationBase
    implements EvaluatedMethodDeclaration {
    
    @Override
    public <R> R accept( Visitor<R> visitor ) {
        return visitor.visitEvaluatedMethodDeclaration(this) ;
    }        

    public void containingClass( EvaluatedClassDeclaration cdecl ) {
        throw new IllegalArgumentException( "Operation not permitted" ) ;
    }

    void makeRepresentation( StringBuilder sb, ObjectSet set ) {
        handleModifier( sb, modifiers() ) ;
        sb.append( " " ) ;
        sb.append( returnType().toString() ) ;
        sb.append( " " ) ;
        sb.append( name() ) ;
        handleList( sb, "(", 
            castList( parameterTypes(), EvaluatedTypeBase.class ),
            ",", ")", set ) ;
    }
            
    public boolean myEquals( Object obj, ObjectSet set ) {
        EvaluatedMethodDeclaration other = (EvaluatedMethodDeclaration)obj ;
        if (!((EvaluatedTypeBase)returnType()).myEquals( other.returnType(), set )
            || !name().equals( other.name() ) )  {

            return false ;
        }
        
        return equalList( parameterTypes(), other.parameterTypes(), set ) ;
    }
    
    @Override
    public int hashCode( ObjectSet set ) {
        return returnType().hashCode() ^
            parameterTypes().hashCode() ^
            name().hashCode() ;
    }
}
