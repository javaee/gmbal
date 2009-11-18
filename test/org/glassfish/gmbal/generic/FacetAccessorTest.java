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
import java.lang.reflect.Method;
import java.util.Collection;
import junit.framework.TestCase;

/**
 *
 * @author ken
 */
public class FacetAccessorTest extends TestCase {
    private interface A {
        int operation( int arg ) ;
        
        int add( int arg1, int arg2 ) ;
    }
    
    private interface B {
        int operation( int arg ) ;
        
        int sub( int arg1, int arg2 ) ;
    }
    
    private static Method a_operation ;
    private static Method add ;
    private static Method b_operation ;
    private static Method sub ;
    
    static {
        try {
            a_operation = A.class.getDeclaredMethod( "operation", int.class ) ;
            b_operation = B.class.getDeclaredMethod( "operation", int.class ) ;
            add = A.class.getDeclaredMethod( "add", int.class, int.class ) ;
            sub = B.class.getDeclaredMethod( "sub", int.class, int.class ) ;
        } catch (Exception exc) {
            throw new RuntimeException( exc ) ;
        }
    }
    
    private static class TestClass implements A, FacetAccessor {
        private FacetAccessor delegate ;
        
        public TestClass() {
            this.delegate = new FacetAccessorImpl( this ) ;
        }
        
        public <T> T facet(Class<T> cls, boolean debug) {
            return delegate.facet( cls, debug ) ;
        }

        public <T> void addFacet(T obj) {
            delegate.addFacet( obj ) ;
        }

        public void removeFacet( Class<?> cls ) {
            delegate.removeFacet( cls ) ;
        }

        public Object invoke(Method method, boolean debug, Object... args) {
            return delegate.invoke( method, debug, args ) ;
        }
        
        public int operation( int arg ) {
            return 2*arg ;
        }
        
        public int add( int arg1, int arg2 ) {
            return arg1+arg2 ;
        }

        public Collection<Object> facets() {
            return delegate.facets() ;
        }

        public Object get(Field field, boolean debug) {
            return delegate.get( field, debug ) ;
        }

        public void set(Field field, Object value, boolean debug) {
            delegate.set( field, value, debug ) ;
        }
    }
    
    private static class BImpl implements B {
        public int factor ;
        
        public BImpl( int factor ) {
            this.factor = factor ;
        }
        
        public int operation( int arg ) {
            return factor*arg ;
        }
        
        public int sub( int arg1, int arg2 ) {
            return arg1-arg2 ;

        }
    }
        
    public FacetAccessorTest(String testName) {
        super(testName);
    }            

    private static boolean firstTime = true ;

    @Override
    protected void setUp() throws Exception {
        if (firstTime) {
            System.out.println( "****************** FacetAccessorTest **********************" ) ;
            firstTime = false ;
        }

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of facet method, of class FacetAccessor.
     */
    public void testFacet() {
        System.out.println("facet");
        FacetAccessor fa = new TestClass() ;

        assertEquals( fa, fa.facet( TestClass.class, false ) ) ;
        assertEquals( fa, fa.facet( A.class, false ) ) ;
        assertNull( fa.facet( B.class, false ) ) ;
        B b = new BImpl( 10 ) ;
        fa.addFacet( b ) ;
        assertEquals( b, fa.facet( B.class, false ) ) ;
        fa.removeFacet( B.class ) ;
        assertNull( fa.facet( B.class, false ) ) ;
    }

    /**
     * Test of invoke method, of class FacetAccessor.
     */
    public void testInvoke() {
        System.out.println("invoke");
        FacetAccessor fa = new TestClass() ;
        B b = new BImpl( 10 ) ;
        fa.addFacet( b ) ;
        
        assertEquals( fa.invoke( a_operation, false, 2  ), 4 ) ;
        assertEquals( fa.invoke( add, false, 21, 17 ), 38 ) ;
        assertEquals( fa.invoke( b_operation, false, 2 ), 20 ) ;
        assertEquals( fa.invoke( sub, false, 21, 17 ), 4 ) ;
        
        b = new BImpl( 100 ) ;
        fa.addFacet( b ) ;
        assertEquals( fa.invoke( b_operation, false, 2 ), 200 ) ;
    }

    public void testGet() throws NoSuchFieldException {
        System.out.println( "get" ) ;
        FacetAccessor fa = new TestClass() ;
        B b = new BImpl( 10 ) ;
        fa.addFacet( b ) ;

        Field factorField = BImpl.class.getDeclaredField("factor") ;
        assertEquals( fa.get( factorField, false ), 10 ) ;
    }
}
