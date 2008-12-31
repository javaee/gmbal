/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.typelib;

import java.io.PrintStream;
import java.lang.reflect.Type;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 *
 * @author ken
 */
public class TypeEvaluatorTest extends TestCase {
    
    public TypeEvaluatorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        Test suite = TestTypelib.suite() ;
        return suite;
    }
}
