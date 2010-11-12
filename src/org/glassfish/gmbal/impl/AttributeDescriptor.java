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

package org.glassfish.gmbal.impl ;


import java.lang.reflect.AccessibleObject;
import java.lang.reflect.ReflectPermission;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.List;
import javax.management.ReflectionException ;

import org.glassfish.gmbal.generic.MethodMonitor;
import org.glassfish.gmbal.generic.MethodMonitorFactory;
import org.glassfish.gmbal.generic.DumpIgnore;
import org.glassfish.gmbal.generic.DumpToString;
import org.glassfish.gmbal.generic.FacetAccessor;
import org.glassfish.gmbal.generic.Pair;
import javax.management.MBeanException;
import org.glassfish.gmbal.typelib.EvaluatedAccessibleDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedFieldDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedMethodDeclaration;
import org.glassfish.gmbal.typelib.EvaluatedType;
    
public class AttributeDescriptor {
    public enum AttributeType { SETTER, GETTER } ;

    @DumpToString
    private EvaluatedAccessibleDeclaration _decl ;
    private String _id ;
    private String _description ;
    private AttributeType _atype ;
    @DumpToString
    private EvaluatedType _type ;
    private TypeConverter _tc ;

    @DumpIgnore
    private MethodMonitor mm = MethodMonitorFactory.makeStandard( getClass() ) ;

    private static final Permission accessControlPermission =
        new ReflectPermission( "suppressAccessChecks" ) ;

    private AttributeDescriptor( final ManagedObjectManagerInternal mom,
        final EvaluatedAccessibleDeclaration decl, final String id,
        final String description, final AttributeType atype,
        final EvaluatedType type ) {

        this._decl = AccessController.doPrivileged(
            new PrivilegedAction<EvaluatedAccessibleDeclaration>() {
                public EvaluatedAccessibleDeclaration run() {
                    decl.accessible().setAccessible(true);
                    return decl ;
                }
            }
        ) ;

        this._id = id ;
        this._description = description ;
        this._atype = atype ;
        this._type = type ;
        this._tc = mom.getTypeConverter( type ) ;
    }

    public final AccessibleObject accessible() { return _decl.accessible() ; }

    public final String id() { return _id ; }

    public final String description() { return _description ; }

    public final AttributeType atype() { return _atype ; }

    public final EvaluatedType type() { return _type ; }

    public final TypeConverter tc() { return _tc ; }
    
    public boolean isApplicable( Object obj ) {
        if (_decl instanceof EvaluatedMethodDeclaration) {
            EvaluatedMethodDeclaration em = (EvaluatedMethodDeclaration)_decl ;
            return em.method().getDeclaringClass().isInstance( obj ) ;
        } else if (_decl instanceof EvaluatedFieldDeclaration) {
            EvaluatedFieldDeclaration ef = (EvaluatedFieldDeclaration)_decl ;
            return ef.field().getDeclaringClass().isInstance( obj ) ;
        }

        return false ;
    }

    private void checkType( AttributeType at ) {
        if (at != _atype) {
            throw Exceptions.self.excForCheckType( at ) ;
        }
    }

    public Object get( FacetAccessor fa,
        boolean debug ) throws MBeanException, ReflectionException {
        
        mm.enter( debug, "get", fa ) ;
        
        checkType( AttributeType.GETTER ) ;
                
        Object result = null;
        
        try {
            if (_decl instanceof EvaluatedMethodDeclaration) {
                EvaluatedMethodDeclaration em = (EvaluatedMethodDeclaration)_decl ;
                result = _tc.toManagedEntity( fa.invoke( em.method(), debug ));
            } else if (_decl instanceof EvaluatedFieldDeclaration) {
                EvaluatedFieldDeclaration ef = (EvaluatedFieldDeclaration)_decl ;
                result = _tc.toManagedEntity( fa.get( ef.field(), debug )) ;
            } else {
                Exceptions.self.unknownDeclarationType(_decl) ;
            }
        } finally {
            mm.exit( debug, result ) ;
        }
        
        return result ;
    }

    public void set( FacetAccessor target, Object value, 
        boolean debug ) throws MBeanException, ReflectionException {
        
        checkType( AttributeType.SETTER ) ;
        
        mm.enter( debug, "set", target, value ) ;
        
        try {
            if (_decl instanceof EvaluatedMethodDeclaration) {
                EvaluatedMethodDeclaration em =
                    (EvaluatedMethodDeclaration)_decl ;
                target.invoke( em.method(), debug, _tc.fromManagedEntity(value)) ;
            } else if (_decl instanceof EvaluatedFieldDeclaration) {
                EvaluatedFieldDeclaration ef = (EvaluatedFieldDeclaration)_decl ;
                target.set( ef.field(), _tc.fromManagedEntity( value ), debug ) ;
            } else {
                Exceptions.self.unknownDeclarationType(_decl) ;
            }
        } finally {
            mm.exit( debug ) ;
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
        return str.substring( prefix.length() ) ;

        /* old implementation that converted first char after prefix to lower case:
	int prefixLength = prefix.length() ;
	String first = str.substring( prefixLength, 
            prefixLength+1 ).toLowerCase() ;
	if (str.length() == prefixLength + 1) {
	    return first ;
	} else {
	    return first + str.substring( prefixLength + 1 ) ;
	}
        */
    }

    private static String lowerInitialCharacter( final String arg ) {
        if (arg == null || arg.length() == 0) {
            return arg ;
        }

        char initChar = Character.toLowerCase( arg.charAt(0) ) ;
        String rest = arg.substring(1) ;
        return initChar + rest ;
    }

    private static String getDerivedId( String methodName, 
        final Pair<AttributeType,EvaluatedType> ainfo,
        final ManagedObjectManagerInternal.AttributeDescriptorType adt) {

        String result = methodName ;
        boolean needLowerCase = adt ==
            ManagedObjectManagerInternal.AttributeDescriptorType
                .COMPOSITE_DATA_ATTR ;

        if (ainfo.first() == AttributeType.GETTER) {
            if (startsWithNotEquals( methodName, "get" )) {
                result = stripPrefix( methodName, "get" ) ;
                if (needLowerCase) {
                    result = lowerInitialCharacter( result ) ;
                }
            } else if (ainfo.second().equals( EvaluatedType.EBOOLEAN ) &&
                startsWithNotEquals( methodName, "is" )) {
                result = stripPrefix( methodName, "is" ) ;
                if (needLowerCase) {
                    result = lowerInitialCharacter( result ) ;
                }
            }
        } else {
            if (startsWithNotEquals( methodName, "set" )) {
                result = stripPrefix( methodName, "set" ) ;
                if (needLowerCase) {
                    result = lowerInitialCharacter( result ) ;
                }
            }
        }
        
        return result ;
    }

    private static Pair<AttributeType,EvaluatedType> getTypeInfo(
        EvaluatedDeclaration decl ) {

        EvaluatedType evalType = null ;
        AttributeType atype = null ;
        if (decl instanceof EvaluatedMethodDeclaration) {
            EvaluatedMethodDeclaration method =
                (EvaluatedMethodDeclaration)decl ;

            final List<EvaluatedType> atypes = method.parameterTypes() ;

            if (method.returnType().equals( EvaluatedType.EVOID )) {
                if (atypes.size() != 1) {
                    return null ;
                }

                atype = AttributeType.SETTER ;
                evalType = atypes.get(0) ;
            } else {
                if (atypes.size() != 0) {
                    return null ;
                }

                atype = AttributeType.GETTER ;
                evalType = method.returnType() ;
            }
        } else if (decl instanceof EvaluatedFieldDeclaration) {
            EvaluatedFieldDeclaration field = (EvaluatedFieldDeclaration)decl ;

            evalType = field.fieldType() ;
            atype = AttributeType.GETTER ;
        } else {
            Exceptions.self.unknownDeclarationType( decl ) ;
        }

        return new Pair<AttributeType,EvaluatedType>( atype, evalType ) ;
    }

    private static boolean empty( String arg ) {
        return (arg==null) || (arg.length() == 0) ;
    }
   
    // See if method is an attribute according to its type, and the id and methodName arguments.
    // If it is, returns its AttributeDescriptor, otherwise return null.  Fails if
    // both id and methodName are empty.
    public static AttributeDescriptor makeFromInherited(
        final ManagedObjectManagerInternal mom,
        final EvaluatedMethodDeclaration method, final String id,
        final String methodName, final String description,
        final ManagedObjectManagerInternal.AttributeDescriptorType adt ) {

        if (empty(methodName) && empty(id)) {
            throw Exceptions.self.excForMakeFromInherited() ;
        }

        Pair<AttributeType,EvaluatedType> ainfo = getTypeInfo( method ) ;
        if (ainfo == null) {
            return null ;
        }

        final String derivedId = getDerivedId( method.name(), ainfo, adt ) ;

        if (empty( methodName )) { // We know !empty(id) at this point
            if (!derivedId.equals( id )) {
                return null ;
            }
        } else if (!methodName.equals( method.name() )) {
            return null ;
        }

        String actualId = empty(id) ? derivedId : id ;

        return new AttributeDescriptor( mom, method, actualId, description,
            ainfo.first(), ainfo.second() ) ;
    }

    // Create an AttributeDescriptor from a field or method.  Fields always
    // correspond to getters.  This case always returns an
    // AttributeDescriptor (unless it fails, for example because an annotated
    // method had an invalid signature as an Attribute).
    // Note that extId and description may be empty strings.
    public static AttributeDescriptor makeFromAnnotated( 
        final ManagedObjectManagerInternal mom, 
        final EvaluatedAccessibleDeclaration decl, final String extId,
        final String description,
        final ManagedObjectManagerInternal.AttributeDescriptorType adt ) {

        Pair<AttributeType,EvaluatedType> ainfo = getTypeInfo( decl ) ;
        if (ainfo == null) {
            throw Exceptions.self.excForMakeFromAnnotated( decl ) ;
        }

        String actualId = empty(extId) ? 
            getDerivedId( decl.name(), ainfo, adt ) : extId ;

        if (mom.isAMXAttributeName(actualId)) {
            throw Exceptions.self.duplicateAMXFieldName(
                actualId, decl.name(), decl.containingClass().name() ) ;
        }

        return new AttributeDescriptor( mom, decl, actualId, description,
            ainfo.first(), ainfo.second() ) ;
    }
}
