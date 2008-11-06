/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2007 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.jmxa.impl ;

import com.sun.jmxa.generic.ClassAnalyzer;
import java.lang.reflect.InvocationTargetException;
import java.util.List ;

import java.lang.reflect.Method ;
import java.lang.reflect.Type ;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ReflectionException ;


import com.sun.jmxa.generic.Algorithms ;

import com.sun.jmxa.ManagedAttribute ;
import com.sun.jmxa.generic.DprintUtil;
import com.sun.jmxa.generic.DumpIgnore;
import com.sun.jmxa.generic.DumpToString;
import com.sun.jmxa.generic.FacetAccessor;
import com.sun.jmxa.generic.Predicate;
import javax.management.MBeanException;
    
public class AttributeDescriptor {
    public enum AttributeType { SETTER, GETTER } ;

    private Method _method ;
    private String _id ;
    private String _description ;
    private AttributeType _atype ;
    private Type _type ;
    @DumpToString
    private TypeConverter _tc ;
    @DumpIgnore
    private DprintUtil dputil = new DprintUtil( this ) ;

    public final Method method() { return _method ; }

    public final String id() { return _id ; }

    public final String description() { return _description ; }

    public final AttributeType atype() { return _atype ; }

    public final Type type() { return _type ; }

    public final TypeConverter tc() { return _tc ; }

    // Attribute method syntax:
    // id = id as present in annotation, or as derived from method
    // Id = id with initial letter capitalized
    // patterns:
    //	Setter:
    //	    void setId( T arg ) ;
    //
    //	    void id( T arg ) ;
    //	Getter:
    //	    T getId() ;
    //	    T id() ;
    //	    boolean isId() ;
    //	    Boolean isId() ;

    // Tested by testIsSetterIsGetter
    public static boolean isSetter( final Method method, final String ident ) {
	final Class<?> returnType = method.getReturnType() ;
	if (returnType != void.class) {
	    return false ;
        }
        
	if (method.getParameterTypes().length != 1) {
	    return false ;
        }

	final String mname = method.getName() ;
	final String initCapId = ident.substring(0,1).toUpperCase() 
            + ident.substring(1) ;

	if (mname.equals( ident )) {
	    return true ;
        }

	if (mname.equals( "set" + initCapId )) {
	    return true ;
        }

	return false ;
    }

    // Tested by testIsSetterIsGetter
    public static boolean isGetter( final Method m, final String id ) {
	Class<?> rt = m.getReturnType() ;
	if (rt == void.class) {
	    return false ;
        }
	if (m.getParameterTypes().length != 0) {
	    return false ;
        }

	final String mname = m.getName() ;
	final String initCapId = id.substring(0,1).toUpperCase() 
            + id.substring(1) ;

	if (mname.equals( id )) {
	    return true ;
        }

	if (mname.equals( "get" + initCapId )) {
	    return true ;
        }

	if (rt.equals( boolean.class ) || rt.equals( Boolean.class)) {
	    if (mname.equals( "is" + initCapId )) {
		return true ;
            }
	}

	return false ;
    }

    // Check whether or not this AttributeDescriptor is applicable to obj.
    public boolean isApplicable( Object obj ) {
        return _method.getDeclaringClass().isInstance( obj ) ;
    }

    private void checkType( AttributeType at ) {
        if (at != _atype) {
            throw new RuntimeException( "Required AttributeType is " + at ) ;
        }
    }

    private void makeMBeanException( Exception exc ) throws ReflectionException,
        MBeanException {
        if ((exc instanceof IllegalAccessException) || 
            (exc instanceof IllegalArgumentException)) {
            throw new ReflectionException( exc, 
                "Exception while invoking method " + _method 
                    + " on class " + _method.getDeclaringClass().getName() ) ;
        } else {
            Exception ex = exc ;
            if (exc instanceof InvocationTargetException) {
                Throwable wrappedException = exc.getCause() ;
                if (wrappedException instanceof Exception) {
                    ex = (Exception)wrappedException ;
                }
            }
                
            throw new MBeanException( ex,
                "Exception while invoking method " + _method 
                    + " on class " + _method.getDeclaringClass().getName() ) ;  
        }
    }
    
    public Object get( FacetAccessor fa,
        boolean debug ) throws MBeanException, ReflectionException {
        
        if (debug) {
            dputil.enter( "get", "fa=", fa ) ;
        }
        
        checkType( AttributeType.GETTER ) ;
                
        Object result = null;
        
        try {
            result = _tc.toManagedEntity(fa.invoke(_method));
        } catch (Exception exc) {
            if (debug) {
                dputil.exception( "Error in get", exc ) ;
            }
            
           makeMBeanException( exc ) ;
        } finally {
            if (debug) {
                dputil.exit( result ) ;
            }
        }
        
        return result ;
    }

    public void set( FacetAccessor target, Object value, 
        boolean debug ) throws MBeanException, ReflectionException {
        
        checkType( AttributeType.SETTER ) ;
        
        if (debug) {
            dputil.enter( "set", "target=", target, "value=", value ) ;
        }
        
        try {
            target.invoke(_method, _tc.fromManagedEntity(value));
        } catch (Exception ex) {
            dputil.exception( "Exception in set ", ex ) ;
            makeMBeanException( ex ) ;
        } finally {
            dputil.exit() ;
        }
    }

    private static boolean startsWithNotEquals( String str, String prefix ) {
	return str.startsWith( prefix ) && !str.equals( prefix ) ;
    }

    private static String stripPrefix( String str, String prefix ) {
	int prefixLength = prefix.length() ;
	String first = str.substring( prefixLength, 
            prefixLength+1 ).toLowerCase() ;
	if (str.length() == prefixLength + 1) {
	    return first ;
	} else {
	    return first + str.substring( prefixLength + 1 ) ;
	}
    }
    
    /* Find the attribute corresponding to a getter or setter with the given
     * id. Returns null if no such attribute is found.
     */
    public static AttributeDescriptor findAttribute( 
        final ManagedObjectManagerInternal mom,
        final ClassAnalyzer ca, final String id, 
        final String description, final AttributeType at ) {

        final List<Method> methods = ca.findMethods( 
	    new Predicate<Method>() {
		public boolean evaluate( final Method m ) {
                    if (at == AttributeType.GETTER) {
                        return isGetter(  m ,id ) ;
                    } else {
                        return isSetter(  m ,id ) ;
                    }
		}
	    }
	) ;

        if (methods.size() == 0) {
            return null ;
        }

        return new AttributeDescriptor( mom, methods, id, description, at ) ;
    }

    private AttributeDescriptor( ManagedObjectManagerInternal mom, 
        final List<Method> methods, final String id, final String description, 
        final AttributeType at ) {

        this( mom, 
            (at == AttributeType.GETTER) ?
                Algorithms.getFirst( methods, "No getter named " + id 
                    + " found" )
            :
                Algorithms.getFirst( methods, "No setter named " + id 
                    + " found" ),
            id, description ) ;
    }

    // Handle a method that is NOT annotated with @ManagedAttribute
    public AttributeDescriptor( ManagedObjectManagerInternal mom, Method m, 
        String extId, String description ) {

        this._method = m ;
        this._id = extId ;
        this._description = description ;

        final String name = m.getName() ;
        if (startsWithNotEquals( name, "get" )) {
            if (extId.equals( "" )) {
                _id = stripPrefix( name, "get" ) ;
            }

            this._atype = AttributeType.GETTER ;

            if (m.getGenericReturnType() == void.class) {
                throw new IllegalArgumentException( m 
                    + " is an illegal getter method" ) ;
            }
            if (m.getGenericParameterTypes().length != 0) {
                throw new IllegalArgumentException( m 
                    + " is an illegal getter method" ) ;
            }
            this._type = m.getGenericReturnType() ;
        } else if (startsWithNotEquals( name, "set" )) {
            if (extId.equals( "" )) {
                _id = stripPrefix( name, "set" ) ;
            }

            this._atype = AttributeType.SETTER ;

            if (m.getGenericReturnType() != void.class) {
                throw new IllegalArgumentException( m 
                    + " is an illegal setter method" ) ;
            }
            if (m.getGenericParameterTypes().length != 1 ) {
                throw new IllegalArgumentException( m 
                    + " is an illegal setter method" ) ;
            }
            this._type = m.getGenericParameterTypes()[0] ;
        } else if (startsWithNotEquals( name, "is" )) {
            if (extId.equals( "" )) {
                _id = stripPrefix( name, "is" ) ;
            }

            this._atype = AttributeType.GETTER ;

            if (m.getGenericParameterTypes().length != 0) {
                throw new IllegalArgumentException( m 
                    + " is an illegal \"is\" method" ) ;
            }
            this._type = m.getGenericReturnType() ;
            if (!_type.equals( boolean.class ) && 
                !_type.equals( Boolean.class )) {
                
                throw new IllegalArgumentException( m 
                    + " is an illegal \"is\" method" ) ;
            }
        } else {
            if (extId.equals( "" )) {
                _id = name ;
            }
            
            Type rtype = m.getGenericReturnType() ;
            Type[] ptypes = m.getGenericParameterTypes() ;
            if (rtype.equals( void.class ) && (ptypes.length == 1)) {
                this._type = ptypes[0] ;
                this._atype = AttributeType.SETTER ;
            } else if (!rtype.equals( void.class ) && (ptypes.length == 0)) {
                this._type = rtype ;
                this._atype = AttributeType.GETTER ;
            } else {
                throw new IllegalArgumentException( m 
                    + " is not a valid attribute method" ) ;
            }
        }

        this._tc = mom.getTypeConverter( this._type ) ;
    }

    // Handle a method with an @ManagedAttribute annotation
    public AttributeDescriptor( ManagedObjectManagerInternal mom, Method m ) {
        this( mom, m,
            m.getAnnotation( ManagedAttribute.class ).id(),
            mom.getDescription( m ) ) ;
    }

    public static class WrappedException extends RuntimeException {
        private static final long serialVersionUID = -3289041604278946501L;
        
	public WrappedException( Exception exc ) {
	    super( exc ) ;
	}

        @Override
	public Exception getCause() {
	    return (Exception)super.getCause() ;
	}
    }
}
