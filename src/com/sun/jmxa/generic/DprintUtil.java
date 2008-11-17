/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.jmxa.generic;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ken
 */
public class DprintUtil {
    private static final boolean USE_LOGGER = false ;
    
    private String sourceClassName ;
    private String loggerName ;
    private ThreadLocal<Stack<String>> currentMethod = new ThreadLocal<Stack<String>>() {
        @Override
        public Stack<String> initialValue() {
            return new Stack<String>() ;
        }
    } ;

    public DprintUtil( Class selfClass ) {
        sourceClassName = compressClassName( selfClass.getName() ) ;  
        loggerName = selfClass.getPackage().getName() ;
    }        
    
    private static String compressClassName( String name )
    {
	// Note that this must end in . in order to be renamed correctly.
	String prefix = "com.sun.jmxa." ;
	if (name.startsWith( prefix ) ) {
	    return "(JMXA)." + name.substring( prefix.length() ) ;
	} else {
            return name;
        }
    }
 
    private synchronized void dprint(String msg) {
        String prefix = "(" + Thread.currentThread().getName() + "): " ;
  
        if (USE_LOGGER) {
            String mname = currentMethod.get().peek() ;
            Logger.getLogger( loggerName ).
                logp( Level.INFO, prefix + msg, sourceClassName, mname ) ;
        } else {
            System.out.println( prefix + sourceClassName + msg ) ;
        }
    }
    
    private synchronized void dprint(String msg, Throwable exc ) {
        String prefix = "(" + Thread.currentThread().getName() + "): " ;
        
        if (USE_LOGGER) {
            String mname = currentMethod.get().peek() ;
            Logger.getLogger( loggerName ).
                logp( Level.INFO, prefix + msg, sourceClassName, mname, exc ) ;
        } else {
            System.out.println( prefix + sourceClassName + msg + ": " + exc ) ;
            exc.printStackTrace() ;
        }
    }

    private String makeString( Object... args ) {
        if (args.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder() ;
        sb.append( '(' ) ;
        boolean first = true ;
        for (Object obj : args) {
            if (first) {
                first = false ;
            } else {
                sb.append( ' ' ) ;
            }
            if (obj == null) {
                sb.append( "<NULL>" ) ;
            } else {
                sb.append( obj.toString() ) ;
            }
        }
        sb.append( ')' ) ;

        return sb.toString() ;
    }

    public void enter( String mname, Object... args ) {
        currentMethod.get().push( mname ) ;
        String str = makeString( args ) ;
        dprint( "." + mname + "->" + str ) ;
    }

    public void info( Object... args ) {
        String mname = currentMethod.get().peek() ;
        String str = makeString( args ) ;
        dprint( "." + mname + "::" + str ) ;
    }
    
    public void exception( String msg, Throwable exc ) {
        String mname = currentMethod.get().peek() ;
        String str = makeString( "Exception: ", msg, exc ) ;
        dprint( "." + mname + "::" + str, exc ) ;
    }

    public void exit() {
        String mname = currentMethod.get().peek() ;
        dprint( "." + mname + "<-" ) ;
        currentMethod.get().pop() ;
    }

    public void exit( Object retVal ) {
        String mname = currentMethod.get().peek() ;
        dprint( "." + mname + "<-(" + retVal + ")" ) ;
        currentMethod.get().pop() ;
    }
}
