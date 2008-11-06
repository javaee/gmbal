/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.generic;

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
    
    private static Method Aoperation ;
    private static Method add ;
    private static Method Boperation ;
    private static Method sub ;
    
    static {
        try {
            Aoperation = A.class.getDeclaredMethod( "operation", int.class ) ;
            Boperation = B.class.getDeclaredMethod( "operation", int.class ) ;
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
        
        public <T> T facet(Class<T> cls) {
            return delegate.facet( cls ) ;
        }

        public <T> void addFacet(T obj) {
            delegate.addFacet( obj ) ;
        }

        public void removeFacet( Class<?> cls ) {
            delegate.removeFacet( cls ) ;
        }

        public Object invoke(Method method, Object... args) {
            return delegate.invoke( method, args ) ;
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
    }
    
    private static class BImpl implements B {
        int factor ;
        
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

    @Override
    protected void setUp() throws Exception {
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

        assertEquals( fa, fa.facet( TestClass.class ) ) ;
        assertEquals( fa, fa.facet( A.class ) ) ;
        assertNull( fa.facet( B.class ) ) ;
        B b = new BImpl( 10 ) ;
        fa.addFacet( b ) ;
        assertEquals( b, fa.facet( B.class ) ) ;
        fa.removeFacet( B.class ) ;
        assertNull( fa.facet( B.class ) ) ;
    }

    /**
     * Test of invoke method, of class FacetAccessor.
     */
    public void testInvoke() {
        System.out.println("invoke");
        FacetAccessor fa = new TestClass() ;
        B b = new BImpl( 10 ) ;
        fa.addFacet( b ) ;
        
        assertEquals( fa.invoke( Aoperation, 2 ), 4 ) ;
        assertEquals( fa.invoke( add, 21, 17 ), 38 ) ;
        assertEquals( fa.invoke( Boperation, 2 ), 20 ) ;
        assertEquals( fa.invoke( sub, 21, 17 ), 4 ) ;
        
        b = new BImpl( 100 ) ;
        fa.addFacet( b ) ;
        assertEquals( fa.invoke( Boperation, 2 ), 200 ) ;
    }

}
