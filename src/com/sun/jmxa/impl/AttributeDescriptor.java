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
import com.sun.jmxa.generic.Pair;
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

    private AttributeDescriptor( final ManagedObjectManagerInternal mom, 
        final Method method, final String id, 
        final String description, final AttributeType atype, final Type type ) {
    
        this._method = method ;
        this._id = id ;
        this._description = description ;
        this._atype = atype ;
        this._type = type ;
        this._tc = mom.getTypeConverter( type ) ;
    }

    public final Method method() { return _method ; }

    public final String id() { return _id ; }

    public final String description() { return _description ; }

    public final AttributeType atype() { return _atype ; }

    public final Type type() { return _type ; }

    public final TypeConverter tc() { return _tc ; }
    
    public boolean isApplicable( Object obj ) {
        return _method.getDeclaringClass().isInstance( obj ) ;
    }

    private void checkType( AttributeType at ) {
        if (at != _atype) {
            throw new RuntimeException( "Required AttributeType is " + at ) ;
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
        } finally {
            dputil.exit() ;
        }
    }
    
/**************************************************************************
 * Factory methods and supporting code:
 *
 **************************************************************************/

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

    private static String getDerivedId( String methodName, 
        Pair<AttributeType,Type> ainfo ) {
        String result = methodName ;
        
        if (ainfo.first() == AttributeType.GETTER) {
            if (startsWithNotEquals( methodName, "get" )) {
                result = stripPrefix( methodName, "get" ) ;
            } else if (ainfo.second().equals( boolean.class ) &&
                startsWithNotEquals( methodName, "is" )) {
                result = stripPrefix( methodName, "is" ) ;
            }
        } else {
            if (startsWithNotEquals( methodName, "set" )) {
                result = stripPrefix( methodName, "set" ) ;
            }
        }
        
        return result ;
    }

    private static Pair<AttributeType,Type> getTypeInfo( Method method ) {
        final Type rtype = method.getGenericReturnType() ;
        final Type[] atypes = method.getGenericParameterTypes() ;
        AttributeType atype ;
        Type attrType ;

        if (rtype.equals( void.class )) {
            if (atypes.length != 1) {
                return null ;
            }

            atype = AttributeType.SETTER ;
            attrType = atypes[0] ;
        } else {
            if (atypes.length != 0) {
                return null ;
            }

            atype = AttributeType.GETTER ;
            attrType = rtype ;
        }

        return new Pair<AttributeType,Type>( atype, attrType ) ;
    }

    private static boolean empty( String arg ) {
        return (arg==null) || (arg.length() == 0) ;
    }
   
    // See if method is an attribute according to its type, and the id and methodName arguments.
    // If it is, returns its AttributeDescriptor, otherwise return null.  Fails if
    // both id and methodName are empty.
    public static AttributeDescriptor makeFromInherited( final ManagedObjectManagerInternal mom,
        final Method method, final String id, final String methodName, final String description ) {

        if (empty(methodName) && empty(id)) {
            throw new IllegalArgumentException( "methodName and id must not both be null" ) ;
        }

        Pair<AttributeType,Type> ainfo = getTypeInfo( method ) ;
        if (ainfo == null) {
            return null ;
        }

        final String derivedId = getDerivedId( method.getName(), ainfo ) ; 

        if (empty( methodName )) { // We know !empty(id) at this point
            if (!derivedId.equals( id )) {
                return null ;
            }
        } else if (!methodName.equals( method.getName() )) {
            return null ;
        }

        String actualId = empty(id) ? derivedId : id ;

        return new AttributeDescriptor( mom, method, actualId, description,
            ainfo.first(), ainfo.second() ) ;
    }

    // Create an AttributeDescriptor from method.  This case always return an
    // AttributeDescriptor (unless it fails, for example because an annotated
    // method had an invalid signature as an Attribute).
    // Note that extId and description may be empty strings.
    public static AttributeDescriptor makeFromAnnotated( 
        final ManagedObjectManagerInternal mom, 
        final Method m, final String extId, final String description ) {

        Pair<AttributeType,Type> ainfo = getTypeInfo( m ) ;
        if (ainfo == null) {
            throw new IllegalArgumentException( 
                m + " is not a valid attribute method" ) ;
        }

        String actualId = empty(extId) ? 
            getDerivedId( m.getName(), ainfo ) : extId ;

        return new AttributeDescriptor( mom, m, actualId, description,
            ainfo.first(), ainfo.second() ) ;
    }

}
