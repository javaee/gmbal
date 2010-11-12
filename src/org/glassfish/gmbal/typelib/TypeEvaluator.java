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

import java.lang.reflect.Field;
import java.security.PrivilegedActionException;
import org.glassfish.gmbal.generic.Algorithms;
import org.glassfish.gmbal.generic.Display;
import org.glassfish.gmbal.generic.MethodMonitor;
import org.glassfish.gmbal.generic.MethodMonitorFactory;
import org.glassfish.gmbal.generic.UnaryFunction;
import org.glassfish.gmbal.generic.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.reflect.Modifier.* ;

import java.lang.reflect.Type ;
import java.lang.reflect.GenericArrayType ;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.WildcardType ;
import java.lang.reflect.ParameterizedType ;
import java.lang.reflect.TypeVariable ;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.WeakHashMap;
import javax.management.ObjectName;

/**
 *
 * @author ken
 */
public class TypeEvaluator {
    private TypeEvaluator() {}

    private static boolean debug = false ;
    private static boolean debugEvaluate = false ;
    
    private static final MethodMonitor mm = MethodMonitorFactory.makeStandard(
	TypeEvaluator.class ) ;

    private static Map<Class<?>,EvaluatedType> immutableTypes =
        new HashMap<Class<?>,EvaluatedType>() ;
    
    // Cache of representations of classes with bound type variables.
    // A class may be in many EvalMapKeys with different tvar bindings.
    // XXX EvaluatedClassDeclaration strongly references Class!
    // Design sketch: Create a custom Map implementation (or just something
    // with get/put/iterate) that has two maps: a HashMap for system classes,
    // and a WeakHashMap for non-system classes.
    private static Map<EvalMapKey,EvaluatedClassDeclaration> evalClassMap =
        new HashMap<EvalMapKey,EvaluatedClassDeclaration>() ;

    private static List<EvaluatedType> emptyETList =
            new ArrayList<EvaluatedType>(0) ;

    private static void mapPut( EvaluatedClassDeclaration ecd, 
        Class cls ) {
        mm.enter( debug, "mapPut", ecd, cls ) ;
        immutableTypes.put( cls, ecd ) ;

        try {
            EvalMapKey key = new EvalMapKey( cls, emptyETList ) ;
            evalClassMap.put( key, ecd ) ;
        } finally {
            mm.exit( debug ) ;
        }
    }
    
    // Initialize the map with a few key classes that we do NOT want to evaluate
    // (evaluating Object leads to a getClass method, which leads to all of the
    // reflection classes, which leads to, ... (450 classes later).
    // NONE of these classes are interesting for gmbal, so let's just bootstrap
    // evalClassMap with Object, Object.toString, and String (needed for the
    // return type of Object.toString).
    // Kind of like grounding a meta-object protocol...
    static {

        try {
            // Initialize all of the classes in EvaluatedType to just
            // inherit from Object, except for Object, which has no
            // inheritance, and defines a toString() method.
            // We also need to handle String separately because
            // of the toString() method signature.
            // Also note that we will mark all of these as being immutable,
            // which is only questionable for Date.
            final Class[] classes = {
                int.class, byte.class, char.class, short.class, long.class,
                boolean.class, float.class, double.class,
                void.class, Integer.class, Byte.class, Character.class,
                Short.class, Boolean.class, Float.class, Double.class,
                Long.class, BigDecimal.class, BigInteger.class,
                Date.class, ObjectName.class, Class.class,
                Number.class
            } ;

            final Class objectClass = Object.class ;
            final Class stringClass = String.class ;
            final Class voidClass = Void.class ;

            final Method toStringMethod = getDeclaredMethod( objectClass,
                "toString" ) ;

            // Introduce the EvaluatedClassDeclarations we need
            final EvaluatedClassDeclaration objectECD = getECD( objectClass ) ;
            final EvaluatedClassDeclaration voidECD = getECD( voidClass ) ;
            final EvaluatedClassDeclaration stringECD = getECD( stringClass ) ;

            final EvaluatedMethodDeclaration toStringEMD =
                DeclarationFactory.emdecl( objectECD, PUBLIC, stringECD, "toString",
                emptyETList, toStringMethod ) ;
            final List<EvaluatedMethodDeclaration> toStringList =
                Algorithms.list( toStringEMD ) ;

            final List<EvaluatedClassDeclaration> objectList =
                Algorithms.list( objectECD ) ;

            // Now finalize the definitions of the ECDs
            voidECD.inheritance( objectList ) ;
            voidECD.freeze() ;

            objectECD.methods( toStringList ) ;
            objectECD.freeze() ;

            stringECD.inheritance( objectList ) ;
            stringECD.freeze() ;

            // And store them in the evalClassMap.
            mapPut( voidECD, voidClass ) ;
            mapPut( objectECD, objectClass ) ;
            mapPut( stringECD, stringClass ) ;

            // Finally initialize all of the TypeEvaluator classes.
            for (Class cls : classes) {
                EvaluatedClassDeclaration ecd = getECD( cls ) ;
                ecd.inheritance( objectList ) ;
                ecd.freeze() ;
                mapPut( ecd, cls ) ;
            }
        } catch (Exception exc) {
            throw Exceptions.self.internalTypeEvaluatorError( exc ) ;
        }

        setDebugLevel( 
            Integer.getInteger( "org.glassfish.gmbal.TypelibDebugLevel", 0 ) ) ;
    }

    /** Return the EvaluatedType corresponding to cls if cls represents an
     * immutable type, otherwise return null.
     * @param cls
     * @return an EvaluatedType, if cls is on the immutable list; otherwise null.
     */
    private static EvaluatedType getImmutableEvaluatedType( Class<?> cls ) {
        return immutableTypes.get( cls ) ;
    }

    public synchronized static void setDebugLevel( int level ) {
        debug = level > 1 ;
        debugEvaluate = level >= 1 ;
    }

    private static class EvalMapKey extends Pair<Class<?>,List<EvaluatedType>> {
        public EvalMapKey( Class<?> cls, List<EvaluatedType> decls ) {
            super( cls, decls ) ;
        }

        public static final EvalMapKey OBJECT_KEY = new EvalMapKey(
             Object.class, new ArrayList<EvaluatedType>(0) ) ;
    }

    private static EvaluatedClassDeclaration getECD( Class cls ) {
        return DeclarationFactory.ecdecl( PUBLIC,
            cls.getName(), cls, true ) ;
    }

    private static List<Method> getDeclaredMethods( final Class<?> cls ) {
        SecurityManager sman = System.getSecurityManager() ;
        if (sman == null) {
            return Arrays.asList( cls.getDeclaredMethods() ) ;
        } else {
            return AccessController.doPrivileged(
                new PrivilegedAction<List<Method>>() {
                    public List<Method> run() {
                        return Arrays.asList( cls.getDeclaredMethods() ) ;
                    }
                }
            ) ;

        }
    }

    private static List<Field> getDeclaredFields( final Class<?> cls ) {
        SecurityManager sman = System.getSecurityManager() ;
        if (sman == null) {
            return Arrays.asList( cls.getDeclaredFields() ) ;
        } else {
            return AccessController.doPrivileged(
                new PrivilegedAction<List<Field>>() {
                    public List<Field> run() {
                        return Arrays.asList( cls.getDeclaredFields() ) ;
                    }
                }
            ) ;

        }
    }

    private static Method getDeclaredMethod( final Class<?> cls,
        final String name, final Class<?>... sig )
        throws NoSuchMethodException, PrivilegedActionException {

        SecurityManager sman = System.getSecurityManager() ;
        if (sman == null) {
            return cls.getDeclaredMethod( name, sig ) ;
        } else {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<Method>() {
                    public Method run() throws Exception {
                        return cls.getDeclaredMethod( name, sig ) ;
                    }
                }
            ) ;
        }
    }

    public synchronized static int evalClassMapSize() {
        return evalClassMap.size() ;
    }

    public synchronized static void dumpEvalClassMap() {
        System.out.println( "TypeEvaluator: dumping eval class map") ;
        int numSystem = 0 ;
        int total = 0 ;

        for (Map.Entry<EvalMapKey,EvaluatedClassDeclaration> entry
            : evalClassMap.entrySet() ) {

            System.out.println( "\tKey:" + entry.getKey() + "=>" ) ;
            System.out.println( "\t\t" + entry.getValue() ) ;

            String name = entry.getKey().first().getName() ;
            if (!name.startsWith("org.glassfish.gmbal" )) {
                numSystem++ ;
            }
            total ++ ;
        }

        System.out.printf( 
            "\nEvalClassMap contains %d entries, %d of which are system classes\n",
            total, numSystem ) ;

        // System.out.println( "Complete dump of eval class map") ;
        // System.out.println( ObjectUtility.defaultObjectToString(evalClassMap) ) ;
    }

    // XXX This is another weak hashmap that strongly references its key!
    private static Map<Class,EvaluatedType> classMap =
	new WeakHashMap<Class, EvaluatedType>() ;

    /** Given any generic java type, evaluate all of its type bounds and
     * return an evaluated type.
     * 
     * @param cls The java type to evaluate
     * @return The evaluated type
     */
    public static synchronized EvaluatedType getEvaluatedType( Class cls ) {
        EvaluatedType etype = classMap.get( cls ) ;
	if (etype == null) {
            TypeEvaluationVisitor visitor = new TypeEvaluationVisitor() ;
            etype = visitor.evaluateType( cls ) ;
	    classMap.put( cls, etype ) ;
	}

        return etype ;
    }

    // Getting PartialDefinitions right is a bit tricky.
    // We need both the Class and the List<Type> in the key, because otherwise
    // we cannot tell the difference between List<List<String>> and
    // Enum<E extends Enum<E>>: the first case is not recursive (so each
    // instance of List evaluated to a different ECD) while the second is
    // (and each instance of Enum MUST evaluate to the same ECD, or we get
    // infinite recursion).
    private static class PartialDefinitions {
        private Map<Pair<Class<?>,List<Type>>,EvaluatedType> table =
            new HashMap<Pair<Class<?>,List<Type>>,EvaluatedType>() ;

        private Pair<Class<?>,List<Type>> getKey( Class cls ) {
            List<Type> list = new ArrayList<Type>() ;
            for (TypeVariable tv : cls.getTypeParameters()) {
                Type type ;
                Type[] bounds = tv.getBounds() ;
                if (bounds.length > 0) {
                    if (bounds.length > 1) {
                        throw Exceptions.self
                            .multipleUpperBoundsNotSupported( tv ) ;
                    } else {
                        type = bounds[0] ;
                    }
                } else {
                    type = Object.class ;
                }

                list.add(type) ;
            }

            return new Pair<Class<?>,List<Type>>( cls, list ) ;
        }

        private Pair<Class<?>,List<Type>> getKey( ParameterizedType pt ) {
            List<Type> list = new ArrayList<Type>() ;
            for (Type type : pt.getActualTypeArguments()) {
                list.add(type) ;
            }

            return new Pair<Class<?>,List<Type>>( (Class<?>)pt.getRawType(),
                list ) ;
        }

        public EvaluatedType get( Class cls ) {
            return table.get( getKey( cls ) ) ;
        }

        public EvaluatedType get( ParameterizedType pt ) {
            return table.get( getKey( pt ) ) ;
        }

        public void put( Class cls, EvaluatedType et ) {
            table.put( getKey( cls ), et ) ;
        }

        public void put( ParameterizedType pt, EvaluatedType et ) {
            table.put( getKey( pt ), et ) ;
        }

        public void remove( Class cls ) {
            table.remove( getKey( cls ) ) ;
        }

        public void remove( ParameterizedType pt ) {
            table.remove( getKey( pt ) ) ;
        }
    }

    // Visits the various java.lang.reflect Types to generate an EvaluatedType
    private static class TypeEvaluationVisitor  {
        private final Display<String,EvaluatedType> display ;
        private final PartialDefinitions partialDefinitions ;
        
        public TypeEvaluationVisitor( ) {
            display = new Display<String,EvaluatedType>() ;

            partialDefinitions = new PartialDefinitions() ;
        }

        // External entry point into the Visitor.
	public EvaluatedType evaluateType( Object type ) {
            mm.enter( debugEvaluate, "evaluateType", type ) ;

            EvaluatedType result = null ;

            try {
                if (type == null) {
                    result = null ;
                } else if (type instanceof Class) {
                    Class cls = (Class)type ;
                    result = visitClassDeclaration( cls ) ;
                } else if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)type ;
                    result = visitParameterizedType( pt ) ;
                } else if (type instanceof TypeVariable) {
                    TypeVariable tvar = (TypeVariable)type ;
                    result = visitTypeVariable( tvar ) ;
                } else if (type instanceof GenericArrayType) {
                    GenericArrayType gat = (GenericArrayType)type ;
                    result = visitGenericArrayType( gat ) ;
                } else if (type instanceof WildcardType) {
                    WildcardType wt = (WildcardType)type ;
                    result = visitWildcardType( wt ) ;
                } else if (type instanceof Method) {
                    throw Exceptions.self.evaluateTypeCalledWithMethod(type) ;
                } else {
                    throw Exceptions.self.evaluateTypeCalledWithUnknownType(type) ;
                }
            } finally {
                mm.exit( debugEvaluate, result ) ;
            }

            return result ;
	}

        // The kind-specific visitXXX methods

        private EvaluatedType visitClassDeclaration( Class decl ) {
            mm.enter( debug, "visitClassDeclaration", decl ) ;

            EvaluatedType result = null ;

            try {
                if (decl.isArray()) {
                    mm.info( debug, "decl is an array" ) ;

                    return DeclarationFactory.egat( evaluateType(
                        decl.getComponentType() ) ) ;
                } else {
                    result = partialDefinitions.get( decl ) ;
                    if (result == null) {
                        // Create the classdecl as early as possible, because it
                        // may be needed on methods or type bounds.
                        EvaluatedClassDeclaration newDecl = DeclarationFactory.ecdecl(
                            decl.getModifiers(), decl.getName(), decl ) ;

                        partialDefinitions.put( decl, newDecl ) ;

                        try {
                            OrderedResult<String,EvaluatedType> bindings =
                                getBindings( decl ) ;

                            result = getCorrectDeclaration( bindings, decl, newDecl ) ;
                        } finally {
                            partialDefinitions.remove( decl ) ;
                        }
                    } else {
                        mm.info( debug, "found result:" + result ) ;
                    }
                }
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedType visitParameterizedType( ParameterizedType pt ) {
            mm.enter( debug, "visitParameterizedType", pt ) ;

            Class<?> decl = (Class<?>)pt.getRawType() ;

            EvaluatedType result = null ;
            try {
                result = partialDefinitions.get( pt ) ;
                if (result == null) {
                    // Create the classdecl as early as possible, because it
                    // may be needed on methods or type bounds.
                    EvaluatedClassDeclaration newDecl = DeclarationFactory.ecdecl(
                        decl.getModifiers(), decl.getName(), decl ) ;

                    partialDefinitions.put( pt, newDecl ) ;

                    try {
                        OrderedResult<String,EvaluatedType> bindings =
                            getBindings( pt ) ;

                        result = getCorrectDeclaration( bindings, decl, newDecl ) ;
                    } finally {
                        partialDefinitions.remove( pt ) ;
                    }
                }
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedFieldDeclaration visitFieldDeclaration(
            final EvaluatedClassDeclaration cdecl, final Field fld ) {
            mm.enter( debug, "visitFieldDeclaration", cdecl, fld ) ;

            EvaluatedFieldDeclaration result = null ;

            try {
                // Only looking at final fields
                if (!Modifier.isFinal(fld.getModifiers())) {
                    return null ;
                }

                // Only looking at immutable types.
                // Note: do NOT use EvaluatedType.isImmutable here, as that
                // requires first evaluating the type, which defeats the
                // purpose of checking for immutable types in the first place!
                final Class fieldType = fld.getType() ;
                EvaluatedType ftype = getImmutableEvaluatedType( fieldType ) ;
                if (ftype == null) {
                    return null ;
                }

                result = DeclarationFactory.efdecl(cdecl, fld.getModifiers(),
                    ftype, fld.getName(), fld ) ;
            } catch (Exception exc) {
                mm.info( debug, "Caught exception ", exc, " for field ", fld ) ;
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedMethodDeclaration visitMethodDeclaration(
            final EvaluatedClassDeclaration cdecl, final Method mdecl ) {
            mm.enter( debug, "visitMethodDeclaration", cdecl, mdecl ) ;

            EvaluatedMethodDeclaration result = null ;

            try {
                final List<EvaluatedType> eptypes =
                    Algorithms.map( Arrays.asList( mdecl.getGenericParameterTypes() ),
                        new UnaryFunction<Type,EvaluatedType>() {
                            public EvaluatedType evaluate( Type type ) {
                                return evaluateType( type ) ;
                            } } ) ;

                mm.info( debug, "eptypes" + eptypes ) ;

                // Convenience for the test: all processing is done on a method
                // named getThing, and this is where we need to debug, out of the
                // many hundreds of other method calls.
                if (mdecl.getName().equals( "getThing" )) {
                    mm.info( debug, "processing getThing method from test" ) ;
                }

                result = DeclarationFactory.emdecl( cdecl, mdecl.getModifiers(),
                    evaluateType( mdecl.getGenericReturnType() ),
                    mdecl.getName(), eptypes, mdecl ) ;
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedType visitTypeVariable( TypeVariable tvar ) {
            mm.enter( debug, "visitTypeVariable" ) ;

            EvaluatedType result = null ;

            try {
                result = lookup( tvar ) ;
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedType visitGenericArrayType( GenericArrayType at ) {
            mm.enter( debug, "visitGenericArrayType" ) ;

            EvaluatedType result = null ;

            try {
                result = DeclarationFactory.egat(
                    evaluateType( at.getGenericComponentType() ) ) ;
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedType visitWildcardType( WildcardType wt ) {
            mm.enter( debug, "visitWilcardType" ) ;

            EvaluatedType result = null ;

            try {
                // ignore lower bounds
                // Only support 1 upper bound
                List<Type> ub = Arrays.asList( wt.getUpperBounds() ) ;
                if (ub.size() > 0) {
                    if (ub.size() > 1) {
                        throw Exceptions.self.multipleUpperBoundsNotSupported(
                            wt) ;
                    }

                    result = evaluateType( ub.get(0) ) ;
                } else {
                    result = EvaluatedType.EOBJECT ;
                }
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedType lookup( TypeVariable tvar ) {
            mm.enter( debug, "lookup", tvar ) ;

            EvaluatedType result = null ;

            try {
                result = display.lookup( tvar.getName() ) ;

                if (result == null) {
                    mm.info( debug, "tvar not found in display" ) ;

                    Type[] bounds = tvar.getBounds() ;
                    if (bounds.length > 0) {
                        if (bounds.length > 1) {
                            throw Exceptions.self
                                .multipleUpperBoundsNotSupported( tvar ) ;
                        }

                        result = evaluateType( bounds[0] ) ;
                    } else {
                        result = EvaluatedType.EOBJECT ;
                    }
                }
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private EvaluatedType getCorrectDeclaration( 
            OrderedResult<String,EvaluatedType> bindings,
            Class decl, EvaluatedClassDeclaration newDecl ) {
            mm.enter( debug, "getCorrectDeclaration", decl ) ;

            EvaluatedType result = null ;

            try {
                List<EvaluatedType> blist = bindings.getList() ;
                EvalMapKey key = new EvalMapKey( decl, blist ) ;
                if (blist.size() > 0) {
                    newDecl.instantiations( blist ) ;
                }

                result = evalClassMap.get( key ) ;
                if (result == null) {
                    mm.info( debug, "No result in evalClassMap" ) ;

                    evalClassMap.put( key, newDecl ) ;

                    processClass( newDecl, bindings.getMap(), decl ) ;

                    result = newDecl ;
                } else {
                    mm.info( debug, "Found result in evalClassMap" ) ;
                }
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private void processClass( final EvaluatedClassDeclaration newDecl,
            final Map<String,EvaluatedType> bindings, final Class decl ) {

            mm.enter( debug, "processClass", bindings, decl ) ;

            display.enterScope() ;
            display.bind( bindings ) ;

            try {
                List<EvaluatedClassDeclaration> inheritance =
                    Algorithms.map( getInheritance( decl ),
                    new UnaryFunction<Type,EvaluatedClassDeclaration>() {
                        public EvaluatedClassDeclaration evaluate( Type pt ) {
                            return (EvaluatedClassDeclaration)evaluateType( pt ) ;
                        } } ) ;

                mm.info( debug, "inheritance", inheritance ) ;

                newDecl.inheritance( inheritance ) ;

                List<EvaluatedFieldDeclaration> newFields = Algorithms.map(
                    getDeclaredFields( decl ),
                    new UnaryFunction<Field,EvaluatedFieldDeclaration>() {
                        public EvaluatedFieldDeclaration evaluate( Field fld ) {
                            return visitFieldDeclaration( newDecl, fld ) ;
                        } } ) ;

                newDecl.fields( newFields ) ;

                List<EvaluatedMethodDeclaration> newMethods = Algorithms.map(
                    getDeclaredMethods( decl ),
                    new UnaryFunction<Method,EvaluatedMethodDeclaration>() {
                        public EvaluatedMethodDeclaration evaluate(
                            Method md ) {

                            return visitMethodDeclaration( newDecl, md ) ;
                        } } ) ;

                newDecl.methods( newMethods ) ;
                newDecl.freeze() ;

                mm.info( debug, "newDecl" + newDecl ) ;
            } finally {
                display.exitScope() ;

                mm.exit( debug ) ;
            }
        }

        private List<Type> getInheritance( Class cls ) {
            mm.enter( debug, "getInheritance", cls ) ;

            List<Type> result = null ;

            try {
                result = new ArrayList<Type>(0) ;
                result.add( cls.getGenericSuperclass() ) ;
                result.addAll( Arrays.asList( cls.getGenericInterfaces() ) ) ;
            } finally {
                mm.exit( debug, result ) ;
            }

            return result ;
        }

        private OrderedResult<String,EvaluatedType> getBindings( Class decl ) {
            OrderedResult<String,EvaluatedType> result = new
                OrderedResult<String,EvaluatedType>() ;

            for (TypeVariable tv : decl.getTypeParameters()) {
                EvaluatedType res = lookup( tv ) ;
                result.add( tv.getName(), res ) ;
            }

            return result ;
        }

        private OrderedResult<String,EvaluatedType> getBindings( ParameterizedType pt ) {
            OrderedResult<String,EvaluatedType> result = new
                OrderedResult<String,EvaluatedType>() ;

            Iterator<Type> types =
                Arrays.asList(pt.getActualTypeArguments()).iterator() ;
            Iterator<TypeVariable> tvars =
                Arrays.asList(((Class)pt.getRawType()).getTypeParameters()).iterator() ;

            while (types.hasNext() && tvars.hasNext()) {
                Type type = types.next() ;
                TypeVariable tvar = tvars.next() ;
                result.add( tvar.getName(), evaluateType( type ) ) ;
            }

            if (types.hasNext() != tvars.hasNext()) {
                throw Exceptions.self.listsNotTheSameLengthInParamType(pt) ;
            }

            return result ;
        }

        public static class OrderedResult<K,V> {
            private List<V> list = new ArrayList<V>(0) ;
            private Map<K,V> map = new HashMap<K,V>() ;

            public List<V> getList() { return list ; }
            public Map<K,V> getMap() { return map ; }

            public void add( K key, V value ) {
                list.add( value ) ;
                map.put( key, value ) ;
            }
        }
    }
}
