/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author ken
 */
public class TypelibSuite extends TestCase {
    
    public TypelibSuite(String testName) {
        super(testName);
    }            

    public static Test suite() {
        TestSuite suite = new TestSuite("TypelibSuite");
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
