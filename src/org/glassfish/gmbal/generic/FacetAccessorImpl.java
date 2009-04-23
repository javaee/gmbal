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
package org.glassfish.gmbal.generic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.gmbal.GmbalException;

/** 
 *
 * @author ken
 */
public class FacetAccessorImpl implements FacetAccessor {
    private Object delegate ;
    private Map<Class<?>,Object> facetMap =
        new HashMap<Class<?>,Object>() ;
    private DprintUtil dputil ;
    
    public FacetAccessorImpl( Object delegate ) {
        this.delegate = delegate ;
        this.dputil = new DprintUtil( getClass() ) ;
    }
    
    public <T> T facet(Class<T> cls, boolean debug ) {
        Object result = null ;
        if (debug) {
            dputil.enter( "facet", "cls=", cls ) ;
        }
        
        try {
            if (cls.isInstance(delegate)) {
                result = delegate ;
                if (debug) {
                    dputil.info( "result is delegate" ) ;
                }
            } else {
                result = facetMap.get( cls ) ;
                if (debug) {
                    dputil.info( "result=", result ) ;
                }
            }
                    
            if (result == null) {
                return null ;
            } else {
                return cls.cast( result ) ;
            }
        } finally {
            if (debug) {
                dputil.exit( result ) ;
            }
        }
    }
    
    public Collection<Object> facets() {
        Collection<Object> result = new ArrayList<Object>() ;
        result.addAll( facetMap.values() ) ;
        result.add( this ) ;
        return result ;
    }
    
    public <T> void addFacet( final T obj) {
        if (obj.getClass().isInstance(delegate)) {
            throw new IllegalArgumentException( 
                "Cannot add facet of supertype of this object" ) ;
        }
                
        ClassAnalyzer ca = new ClassAnalyzer( obj.getClass() ) ;
        ca.findClasses( 
            new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    facetMap.put( arg, obj ) ;
                    return false ;
                } } ) ;
    }

    public Object invoke(Method method, boolean debug, Object... args) {
        if (debug) {
            dputil.enter( "invoke", "method=", method, "args=",
                Arrays.asList( args ) ) ;
        }
        
        Object result = null ;
        try {
            Object target = facet( method.getDeclaringClass(), debug ) ;
            if (target == null) {
                throw new IllegalArgumentException( 
                    "No facet available for method " + method ) ;
            }
            
            try {
                result = method.invoke(target, args);
            } catch (IllegalAccessException ex) {
                throw new GmbalException( "Exception on invocation", ex ) ;
            } catch (IllegalArgumentException ex) {
                throw new GmbalException( "Exception on invocation", ex ) ;
            } catch (InvocationTargetException ex) {
                throw new GmbalException( "Exception on invocation", ex ) ;
            }
        } catch (RuntimeException exc) {
            if (debug) {
                dputil.exception( "Exception in method invoke call", exc ) ;
            }

            throw exc ;
        } finally {
            if (debug) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    public Object get(Field field, boolean debug) {
        if (debug) {
            dputil.enter( "get", "field=", field ) ;
        }

        Object result = null ;

        try {
            Object target = facet( field.getDeclaringClass(), debug ) ;

            try {
                result = field.get(target);
            } catch (IllegalArgumentException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            } catch (IllegalAccessException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            }
        } catch (RuntimeException exc) {
            if (debug) {
                dputil.exception( "Exception in method invoke call", exc ) ;
            }

            throw exc ;
        } finally {
            if (debug) {
                dputil.exit( result ) ;
            }
        }

        return result ;
    }

    public void set(Field field, Object value, boolean debug) {
        if (debug) {
            dputil.enter( "set", "field=", field, "value", value ) ;
        }

        try {
            Object target = facet( field.getDeclaringClass(), debug ) ;

            try {
                field.set(target, value);
            } catch (IllegalArgumentException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            } catch (IllegalAccessException ex) {
                throw new GmbalException( "Exception on field get", ex ) ;
            }
        } catch (RuntimeException exc) {
            if (debug) {
                dputil.exception( "Exception in method invoke call", exc ) ;
            }

            throw exc ;
        } finally {
            if (debug) {
                dputil.exit() ;
            }
        }
    }

    public void removeFacet( Class<?> cls ) {
        if (cls.isInstance(delegate)) {
            throw new IllegalArgumentException( 
                "Cannot add facet of supertype of this object" ) ;
        }
        
        ClassAnalyzer ca = new ClassAnalyzer( cls ) ;
        ca.findClasses( 
            new Predicate<Class>() {
                public boolean evaluate(Class arg) {
                    facetMap.remove( arg ) ;
                    return false ;
                } } ) ;
    }

}
