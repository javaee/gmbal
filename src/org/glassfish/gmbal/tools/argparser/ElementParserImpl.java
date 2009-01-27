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

package org.glassfish.gmbal.tools.argparser ;

import java.util.List ;
import java.util.ArrayList ;

import java.lang.reflect.Array ;
import java.lang.reflect.Method ;
import java.lang.reflect.Constructor ;
import java.lang.reflect.Type ;
import java.lang.reflect.ParameterizedType ;
import java.lang.reflect.InvocationTargetException ;
import org.glassfish.gmbal.generic.Pair;
import org.glassfish.gmbal.generic.UnaryFunction;

public class ElementParserImpl implements ElementParser {
    private UnaryFunction<String,Object> func ;
    private String[] description ;

    public Object evaluate( String str ) {
	return func.evaluate( str ) ;
    }

    public String[] describe() {
	return description.clone() ;
    }

    private static class ResultData extends
        Pair<UnaryFunction<String,Object>,String[]> {

	public ResultData( UnaryFunction<String,Object> func, String[] desc ) {
	    super( func, desc ) ;
	}
    }

    public ElementParserImpl( Method m ) {
	ResultData result = getData( m ) ;

	func = result.first() ;
	description = result.second() ;
    }

    String[] append( String str, String[] strs ) {
	String[] result = new String[ strs.length + 1 ] ;
	int rctr = 0 ;
	result[rctr++] = str ;
	for (String s : strs) {
	    result[rctr++] = s ;
	}
	return result ;
    }

    // Used for complex data types like List and arrays.
    private ResultData getData( Method meth ) {
	UnaryFunction<String,Object> func ;
	String[] description ;
	Class type = meth.getReturnType() ;

	if (type.isArray()) {
	    final String sep = getSeparator( meth ) ;
	    final Class elementClass = type.getComponentType() ;
	    final ResultData elementResultData = getSimpleData( elementClass ) ;
	    description = append( "A " + sep + "-separated list of ",
		elementResultData.second() ) ;
	    
	    func = new UnaryFunction<String,Object>() {
		public Object evaluate( String value ) {
		    String[] elements = value.split( sep ) ;
		    Object result = Array.newInstance( elementClass, 
			elements.length ) ;
		    int ctr = 0 ;	
		    for (String str : elements) {
			Object val = elementResultData.first().evaluate( str ) ;
			Array.set( result, ctr++, val ) ;
		    }

		    return result ;
		} 
	    } ;
	} else if (type.equals( List.class )) {
	    final String sep = getSeparator( meth ) ;
	    Class elementClass = getListElementClass( meth ) ;
	    final ResultData elementResultData = getSimpleData( elementClass ) ;
	    description = append( "A " + sep + "-separated list of ",
		elementResultData.second() ) ;

	    func = new UnaryFunction<String,Object>() {
		public Object evaluate( String value ) {
		    String[] elements = value.split( sep ) ;
		    List result = new ArrayList( elements.length ) ;
		    for (String str : elements) {
			Object val = elementResultData.first().evaluate( str ) ;
			result.add( val ) ;
		    }

		    return result ;
		} 
	    } ;
	} else {
	    return getSimpleData( type ) ;    
	}

	return new ResultData( func,
	    description ) ;
    }

    // Used for all types that take a single element.  This does 
    // not include List<Type> or Type[].  
    private ResultData getSimpleData( final Class type ) {
	UnaryFunction<String,Object> lfunc ;
	String[] ldescription ;

	if (type.isPrimitive()) {
	    ldescription = new String[] { "A valid " + type.getName() } ;

	    lfunc = getPrimitiveParser( type ) ;
	} else if (type == String.class){
	    ldescription = new String[] { "A String" } ;

	    lfunc = new UnaryFunction<String,Object>() {
		public Object evaluate( String str ) {
		    return str ;
		} 
	    } ;
	} else if (type.isEnum()) {
	    ldescription = new String[] {
		"One of: " + getEnumElements( type ) } ;

	    lfunc = new UnaryFunction<String,Object>() {
		public Object evaluate( String str ) {
		    try {
			return Enum.valueOf( type, str ) ;
		    } catch (IllegalArgumentException exc) {
			throw new RuntimeException( str 
			    + " is not in enum " + type.getName(), exc ) ;
		    }
		} 
	    };
	} else { // Anything else: must be a class that supports <init>(String)
	    ldescription = new String[] {
		"A string that can create a " + type.getName() } ;

	    lfunc = makeClassConverter( type ) ;
	} 
	
	return new ResultData( lfunc,
	    ldescription ) ;
    }

    private String getEnumElements( Class cls ) {
	boolean isFirst = true ;
	StringBuilder sb = new StringBuilder() ;
	for (Object obj : cls.getEnumConstants()) {
	    if (isFirst) {
		isFirst = false ;
	    } else {
		sb.append( ' ' ) ;
	    }
	    sb.append( obj.toString() ) ;
	}
	return sb.toString() ;
    }

    private static class BooleanParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Boolean.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> booleanParser =
        new BooleanParser() ;

    private static class ByteParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Byte.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> byteParser =
        new ByteParser() ;

    private static class CharParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            if (str.length() != 1)
                throw new RuntimeException( "String \"" + str
                    + "\" cannot be converted to a Character" ) ;
            return Character.valueOf( str.charAt(0) ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> charParser =
        new CharParser() ;

    private static class ShortParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Short.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> shortParser =
        new ShortParser() ;

    private static class IntegerParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Integer.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> integerParser =
        new IntegerParser() ;

    private static class LongParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Long.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> longParser =
        new LongParser() ;

    private static class FloatParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Float.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> floatParser =
        new FloatParser() ;

    private static class DoubleParser implements UnaryFunction<String,Object> {
        public Object evaluate( String str ) {
            return Double.valueOf( str ) ;
        }
    } ;
    private static final UnaryFunction<String,Object> doubleParser =
        new DoubleParser() ;

    private UnaryFunction<String,Object> getPrimitiveParser( Class type ) {
	UnaryFunction<String,Object> lfunc = null ;

	if (type == boolean.class) {
            lfunc = booleanParser ;
	} else if (type == byte.class) {
	    lfunc = byteParser ;
	} else if (type == char.class) {
	    lfunc = charParser ;
	} else if (type == short.class) {
            lfunc = shortParser ;
	} else if (type == int.class) {
            lfunc = integerParser ;
	} else if (type == long.class) {
            lfunc = longParser ;
	} else if (type == float.class) {
            lfunc = floatParser ;
	} else if (type == double.class) {
            lfunc = doubleParser ;
	} 

	return lfunc ;
    }

    private UnaryFunction<String,Object> makeClassConverter( 
	final Class type ) {

	Constructor cons ;

	try {
	    cons = type.getConstructor( String.class ) ;
	} catch (NoSuchMethodException e1) {
	    throw new RuntimeException( type.getName() 
		+ " does not have a constructor (String)", e1 ) ;
	} catch (SecurityException e2) {
	    throw new RuntimeException( type.getName() 
		+ " constructor (String) is not accessible", e2 ) ;
	}
    
	final Constructor fcons = cons ;

	return new UnaryFunction<String,Object>() {
	    public Object evaluate( String str ) {
		try {
		    return fcons.newInstance( str ) ;
		} catch (InvocationTargetException e1) {
		    throw new RuntimeException( type.getName() 
			+ "(String) constructor threw exception: " 
			+ e1.getTargetException(), e1 ) ;
		} catch (Exception e2) {
                    // Catch all for several checked exceptions: ignore findbugs
		    throw new RuntimeException( "Exception " + e2 
			+ " occured in calling constructor " 
			+ type.getName() + "(String)", e2 ) ;
		}
	    }
	} ;
    }

    private Class getListElementClass( Method meth ) { 
	Type rtype = meth.getGenericReturnType() ;
	if (rtype instanceof ParameterizedType) {
	    ParameterizedType ptype = (ParameterizedType)rtype ;
	    Type[] typeArgs = ptype.getActualTypeArguments() ;
	    assert typeArgs.length == 1 ;
	    Type etype = typeArgs[0] ;
	    if (etype instanceof Class) {
		return (Class)etype ;
	    } else {
		throw new RuntimeException( "Method " + meth 
		    + " has a List<> return type " 
		    + " that is not parameterized by a class" ) ;
	    }
	} else {
	    throw new RuntimeException( "Method " + meth 
		+ " does not have a parameterized List return type" ) ;
	}
    }

    private String getSeparator( Method meth ) {
	Separator sep = meth.getAnnotation( Separator.class ) ;
	String result = "," ;
	if (sep != null)
	    result = (String)sep.value() ;

	return result ;
    }
}
