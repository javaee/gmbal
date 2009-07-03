/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2002-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.gmbal.generic ;

import java.util.List ;
import java.util.ArrayList ;

public class OperationTracer {
    private static boolean enabled = true ;

    public static void enable() {
        enabled = true ;
    }

    public static void disable() {
        enabled = false ;
    }

    private OperationTracer() {}

    private static ThreadLocal<List<Pair<String,Object[]>>> state = new ThreadLocal() {
        @Override
        public List<Pair<String,Object[]>> initialValue() {
            return new ArrayList<Pair<String,Object[]>>() ;
        }
    } ;


    private static String format( final Pair<String,Object[]> arg ) {
        String name = arg.first() ;
        Object[] args = arg.second() ;
        StringBuilder sb = new StringBuilder() ;
        if (name == null) {
            sb.append( "!NULL_NAME!" ) ;
        } else {
            sb.append( name ) ;
        }

        sb.append( '(' ) ;
        boolean first = true ;
        for (Object obj : args ) {
            if (first) {
                first = false ;
            } else {
                sb.append( ',' ) ;
            }

            if (arg == null) {
                sb.append( "null" ) ;
            } else {
                sb.append( arg ) ;
            }
        }
        return sb.toString() ;
    }

    /** Return the current contents of the OperationTracer state
     * for the current thread.
     */
    public static String getAsString() {
        final StringBuilder sb = new StringBuilder() ;
        final List<Pair<String,Object[]>> elements = state.get() ;
        boolean first = true ;
        for (Pair<String,Object[]> elem : elements) {
            if (first) {
                first = false ;
            } else {
                sb.append( ':' ) ;
            }

            sb.append( format( elem ) ) ;
        }

        return sb.toString() ;
    }

    public static void clear() {
        if (enabled) {
            state.get().clear() ;
        }
    }

    public static void enter( final String name, final Object... args ) {
        if (enabled) {
            state.get().add( new Pair<String,Object[]>( name, args ) ) ;
        }
    }

    public static void exit() {
        if (enabled) {
            final List<Pair<String,Object[]>> elements = state.get() ;
            int size = elements.size() ;
            if (size > 0) {
                elements.remove( size - 1 ) ;
            }
        }
    }
}
