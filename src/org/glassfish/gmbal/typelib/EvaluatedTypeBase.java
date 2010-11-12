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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.glassfish.gmbal.generic.ObjectSet;

/**
 *
 * @author ken
 */
public abstract class EvaluatedTypeBase implements EvaluatedType {
    
    public <R> R accept( Visitor<R> visitor ) {
        return visitor.visitEvaluatedType( this ) ;
    }

    private String rep = null ;

    public static void handleModifier( StringBuilder sb, int modifiers ) {
        if (Modifier.isPublic( modifiers )) {
            sb.append( "public " ) ;
        } else if (Modifier.isPrivate( modifiers )) {
            sb.append( "private " ) ;
        } else if (Modifier.isProtected( modifiers )) {
            sb.append( "protected " ) ;
        } else if (Modifier.isAbstract( modifiers )) {
            sb.append( "abstract " ) ;
        } else if (Modifier.isNative( modifiers )) {
            sb.append( "native " ) ;
        } else if (Modifier.isStatic( modifiers )) {
            sb.append( "static " ) ;
        } else if (Modifier.isStrict( modifiers )) {
            sb.append( "strictfp " ) ;
        } else if (Modifier.isSynchronized( modifiers )) {
            sb.append( "synchronized " ) ;
        } else if (Modifier.isTransient( modifiers )) {
            sb.append( "transient " ) ;
        } else if (Modifier.isVolatile( modifiers )) {
            sb.append( "volatile " ) ;
        } else if (Modifier.isFinal( modifiers )) {
            sb.append( "Final " ) ;
        }
    }
    /*
    public static <T> void handleList( StringBuilder sb, List<T> list ) {
        handleList( sb, null, list, " ", null ) ;
    }

    public static <T> void handleList( StringBuilder sb, String start, 
        List<T> list ) {
        
        handleList( sb, start, list, " ", null ) ;
    }
     */

    <S,T extends S> List<T> castList( List<S> list, Class<T> cls ) {
        List<T> result = new ArrayList<T>() ;
        for (S s : list) {
            result.add( cls.cast(s) ) ;
        }
        return result ;
    }

    public static <T extends EvaluatedTypeBase> void handleList(
        StringBuilder sb, String start,
        List<T> list, String sep, String end, ObjectSet set ) {
        
        if (list.size() > 0) {
            if (start != null) {
                sb.append( start ) ;
            }

            boolean first = true ;
            for (T t : list) {
                if (first) {
                    first = false ;
                } else {
                    sb.append( sep ) ;
                }
                t.makeRepresentation( sb, set ) ;
            }

            if (end != null) {
                sb.append( end ) ;
            }
        }
    }

    @Override
    public synchronized String toString() {
        if (rep == null) {
            ObjectSet set = new ObjectSet() ;
            StringBuilder sb = new StringBuilder() ;
            makeRepresentation( sb, set ) ;
            rep = sb.toString() ;
        }

        return rep ;
    }

    abstract void makeRepresentation( StringBuilder sb, ObjectSet set ) ;

    @Override
    // Note that findbugs flags this as a "strange equals method".
    // But that is intentional, because this equals method must deal
    // with circular structures.
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals( Object obj ) {
        ObjectSet set = new ObjectSet() ;
        return equals( obj, set ) ;
    }

    public boolean equals( Object obj, ObjectSet set ) {
        if (this == obj) {
            return true ;
        }
        
        if (this.getClass().isAssignableFrom( obj.getClass() )) {
            if (set.contains( obj )) {
                return true ;
            } else {
                set.add( obj ) ;
                return myEquals( obj, set ) ;
            }
        } else {
            return false ;
        }
    }

    boolean equalList( List<EvaluatedType> list1, List<EvaluatedType> list2,
        ObjectSet set ) {
        if (list1 == null) {
            return list2 == null ;
        } else {
            Iterator<EvaluatedType> iter1 = list1.iterator() ;
            Iterator<EvaluatedType> iter2 = list2.iterator() ;
            while (iter1.hasNext() && iter2.hasNext()) {
                EvaluatedTypeBase obj1 = (EvaluatedTypeBase)iter1.next() ;
                EvaluatedTypeBase obj2 = (EvaluatedTypeBase)iter2.next() ;
                if (!(set.contains( obj1 ))) {
                    if (!obj1.equals( obj2, set )) {
                        return false ;
                    }
                }
            }

            if (iter1.hasNext() != iter2.hasNext()) {
                return false ;
            }
        }

        return true ;
    }

    abstract boolean myEquals( Object obj, ObjectSet set ) ;

    @Override
    public int hashCode() {
        ObjectSet set = new ObjectSet() ;
        return hashCode( set ) ;
    }

    abstract int hashCode( ObjectSet map ) ;

    public boolean isImmutable() {
        return false ;
    }
}
