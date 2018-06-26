/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.gmbal.util;

import java.lang.reflect.Constructor ;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class that allows any class to be instantiated via any accessible constructor.
 * Really a short hand to avoid writing a bunch of reflective code.
 */
public class GenericConstructor<T> {
    private final Object lock = new Object() ;

    private String typeName ;
    private Class<T> resultType ;
    private Class<?> type ;
    private Class<?>[] signature ;
    
    // Use the raw type of the constructor here, because
    // MethodInfo can only return a raw type for a constructor.
    // It is not possible to have MethodInfo return a 
    // Constructor<T> because T may not be known at compile time.
    private Constructor constructor ;

    /** Create a generic of type T for the untyped class cls.
     * Generally cls is a class that has been generated and loaded, so
     * no compiled code can depend on the class directly.  However, the
     * generated class probably implements some interface T, represented
     * here by Class<T>.
     * @param type The expected type of a create call.
     * @param className The name of the class to use for a constructor.
     * @param signature The signature of the desired constructor.
     * @throws IllegalArgumentException if cls is not a subclass of type.
     */
    public GenericConstructor( final Class<T> type, final String className, 
        final Class<?>... signature ) {
        this.resultType = type ;
        this.typeName = className ;
        this.signature = signature.clone() ;
    }

    @SuppressWarnings("unchecked")
    private void getConstructor() {
        synchronized( lock ) {
            if ((type == null) || (constructor == null)) {
                try {
                    type = (Class<T>)Class.forName( typeName ) ;
                    constructor = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Constructor>() {
                            public Constructor run() throws Exception {
                                synchronized( lock ) {
                                    return type.getDeclaredConstructor( signature ) ;
                                }
                            }
                        } ) ;
                } catch (Exception exc) {
                    // Catch all for several checked exceptions: ignore findbugs
                    Logger.getLogger( "org.glassfish.gmbal.util" ).log( Level.FINE,
                        "Failure in getConstructor", exc ) ;
                }
            }
        }
    }

    /** Create an instance of type T using the constructor that
     * matches the given arguments if possible.  The constructor
     * is cached, so an instance of GenericClass should always be
     * used for the same types of arguments.  If a call fails,
     * a check is made to see if a different constructor could 
     * be used.
     * @param args The constructor arguments.
     * @return A new instance of the object.
     */
    public synchronized T create( Object... args ) {
        synchronized(lock) {
            T result = null ;

            for (int ctr=0; ctr<=1; ctr++) {
                getConstructor() ;
                if (constructor == null) {
                    break ;
                }

                try {
                    result = resultType.cast( constructor.newInstance( args ) ) ;
                    break ;
                } catch (Exception exc) {
                    // There are 4 checked exceptions here with identical handling.
                    // Ignore FindBugs complaints.
                    constructor = null ;
                    Logger.getLogger("org.glassfish.gmbal.util").
                        log(Level.WARNING, "Error invoking constructor", exc );
                }
            }

            return result ;
        }
    }
}
