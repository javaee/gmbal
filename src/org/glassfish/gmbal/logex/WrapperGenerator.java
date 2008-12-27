/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.logex;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Given an annotated interface, return a Proxy that implements that interface.
 * Interface must be annotated with @ExceptionWrapper( String idPrefix, String loggerName ).
 * id prefix defaults to empty, loggerName defaults to the package name of the annotated
 * class.
 *
 * The behavior of the implementation of each method on the interface is determined
 * in part by its return type as follows:
 * <ul>
 * <li>void.  Such a method can only log a message.</li>
 * <li>String. Such a method may log a message, and also returns the message.</li>
 * <li>A subclass of Exception.  Such a method may log a message, and also returns
 * an exception containing the message.
 * </ul>
 *
 * Each method may be annotated as follows:
 *
 * <ul>
 * <li>@Message( String value ).  This defines the message to be placed in a resource
 * bundle (generated at build time by a separate tool).  The key to the resource
 * bundle is <loggerName>.<methodName>.  The message is prepended with the 
 * idPrefix and the id from the @Log annotation (if @Log is present, otherwise nothing
 * is prepended to the message).  If this annotation is not present, a default message
 * is created from the method name and the arguments.
 * <li>@Log( LogLevel level, int id ).  The presence of this annotation indicates that
 * a log record must be generated, and logger IF the appropriate logger is enabled at
 * the given level (note that LogLevel is an enum used for the annotation, each member
 * of which returns the java.util.logging.Level from a getLevel() method).
 * </ul>
 * 
 * In addition, the @Chain annotation may be used on a method parameter (whose type
 * must be a subclass of Throwable) of a method that returns an exception
 * to indicate that the parameter should be the cause of the returned exception.
 * All other method parameters are used as arguments in formatting the message.
 *
 * @author ken
 */
public class WrapperGenerator {
    private WrapperGenerator() {}

    // Find the outer index in pannos for which the element array
    // contains an annotation of type cls.
    private static int findAnnotatedParameter( Annotation[][] pannos,
        Class<? extends Annotation> cls ) {
        for (int ctr1=0; ctr1<pannos.length; ctr1++ ) {
            final Annotation[] annos = pannos[ctr1] ;
            for (int ctr2=0; ctr2< annos.length; ctr2++ ) {
                Annotation anno = annos[ctr2] ;
                if (cls.isInstance(anno)) {
                    return ctr1 ;
                }
            }
        }

        return -1 ;
    }

    private static Object[] getWithSkip( Object[] args, int skip ) {
        if (skip >= 0) {
            Object[] result = new Object[args.length-1] ;
            int rindex = 0 ;
            for (int ctr=0; ctr<args.length; ctr++) {
                if (ctr != skip) {
                    result[rindex++] = args[ctr] ;
                }
            }
            return result ;
        } else {
            return args ;
        }
    }

    private static String getMessage( Method method, int numParams, 
        String idPrefix, int logId ) {

        final Message message = method.getAnnotation( Message.class ) ;
        final StringBuilder sb = new StringBuilder() ;
        sb.append( idPrefix ) ;
        sb.append( logId ) ;
        sb.append( ": " ) ;
                    
        if (message == null) {
            sb.append( method.getName() ) ;
            sb.append( ' ' ) ;
            for (int ctr=0; ctr<numParams; ctr++) {
                if (ctr>0) {
                    sb.append( ", " ) ;
                }

                sb.append( "arg" ) ;
                sb.append( ctr ) ;
                sb.append( "={" + ctr + "}" ) ;
            }
        } else {
            sb.append( message.value() ) ;
        }

        return sb.toString() ;
    }

    private static void inferCaller( LogRecord lrec ) {
	// Private method to infer the caller's class and method names

	// Get the stack trace.
	StackTraceElement stack[] = (new Throwable()).getStackTrace();
	StackTraceElement frame = null ;
	String wcname = "$Proxy" ; // Is this right?  Do we always have Proxy$n here?
	String baseName = WrapperGenerator.class.getName() ;
	String nestedName = WrapperGenerator.class.getName() + "$1" ;

	// The top of the stack should always be a method in the wrapper class,
	// or in this base class.
	// Search back to the first method not in the wrapper class or this class.
	int ix = 0;
	while (ix < stack.length) {
	    frame = stack[ix];
	    String cname = frame.getClassName();
	    if (!cname.contains(wcname) && !cname.equals(baseName)
                && !cname.equals(nestedName))  {
		break;
	    }

	    ix++;
	}

	// Set the class and method if we are not past the end of the stack
	// trace
	if (ix < stack.length) {
	    lrec.setSourceClassName(frame.getClassName());
	    lrec.setSourceMethodName(frame.getMethodName());
	}
    }

    private static LogRecord makeLogRecord( Level level, String key,
        Object[] args, Throwable cause, Logger logger ) {
        LogRecord result = new LogRecord( level, key ) ;
        if (args != null && args.length > 0) {
            result.setParameters( args ) ;
        }
        if (level != Level.INFO) {
            if (cause != null) {
                result.setThrown( cause ) ;
            }
            inferCaller( result ) ;
        }

        result.setLoggerName( logger.getName() ) ;
        result.setResourceBundle( logger.getResourceBundle() ) ;
        return result ;
    }

    private static Exception makeException( Class<?> rtype, String msg ) {
        try {
            Constructor cons = rtype.getConstructor(String.class) ;
            return (Exception)cons.newInstance(msg) ;
        } catch (Exception exc) {
            throw new RuntimeException( exc ) ;
        }
    }

    private static class ShortFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder() ;
            sb.append(record.getLevel().getLocalizedName());
            sb.append(": ");
            String message = formatMessage( record ) ;
            sb.append(message);
            return sb.toString();
        }
    }

    private final static Formatter formatter = new ShortFormatter() ;

    private static String handleMessageOnly( Method method, Logger logger,
        Object[] messageParams ) {

        // Just format the message: no exception ID or log level
        // This code is adapted from java.util.logging.Formatter.formatMessage
        String msg = (String)method.getAnnotation( Message.class ).value() ;
        String transMsg ;
        ResourceBundle catalog = logger.getResourceBundle() ;
        try {
            transMsg = catalog.getString( msg ) ;
        } catch (Exception exc) {
            transMsg = msg ;
        }
        if (transMsg.indexOf( "{0" ) > 0 ) {
            return java.text.MessageFormat.format( transMsg, messageParams ) ;
        } else {
            return transMsg ;
        }
    }

    private static Object handleFullLogging( Log log, Method method, Logger logger,
        String idPrefix, Object[] messageParams, Throwable cause )  {

        int logId = log.id() ;
        Level level = log.level().getLevel() ;
        Class<?> rtype = method.getReturnType() ;

        final String msgString = getMessage( method, messageParams.length,
            idPrefix, logId ) ;
        LogRecord lrec = makeLogRecord( level, msgString,
            messageParams, cause, logger ) ;

        if (logger.isLoggable(level)) {
            logger.log( lrec ) ;
        }

        if (rtype.equals( void.class ))
            return null ;

        String fullMessage = formatter.format( lrec ) ;

        if (Exception.class.isAssignableFrom(rtype)) {
            Exception exc = makeException( rtype, fullMessage ) ;
            exc.initCause( cause ) ;
            return exc ;
        }  else if (String.class.isAssignableFrom(rtype)) {
            return fullMessage ;
        } else {
            throw new RuntimeException( "Method " + method
                + " has an illegal return type" ) ;
        }
    }

    public static <T> T makeWrapper( final Class<T> cls ) {
        // Must have an interface to use a Proxy.
        if (!cls.isInterface()) {
            throw new IllegalArgumentException( "Class " + cls +
                "is not an interface" ) ;
        }

        ExceptionWrapper ew = cls.getAnnotation( ExceptionWrapper.class ) ;
        final String idPrefix = ew.idPrefix() ;
        String str = ew.loggerName() ;
        if (str.length() == 0) {
            str = cls.getPackage().getName() ;
        }
        final String name = str ;

        InvocationHandler inh = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {

                final Annotation[][] pannos = method.getParameterAnnotations() ;
                final int chainIndex = findAnnotatedParameter( pannos,
                    Chain.class ) ;
                Throwable cause = null ;
                final Object[] messageParams = getWithSkip( args, chainIndex ) ;
                if (chainIndex >= 0) {
                    cause = (Throwable)args[chainIndex] ;
                }

                final Logger logger = Logger.getLogger( name ) ;
                final Class<?> rtype = method.getReturnType() ;
                final Log log = method.getAnnotation( Log.class ) ;

                if (log == null) {
                    if (!rtype.equals( String.class ) ) {
                        throw new IllegalArgumentException(
                            "No @Log annotation present on "
                            + cls.getName() + "." + method.getName() ) ;
                    }

                    return handleMessageOnly( method, logger, messageParams ) ;
                } else {
                    return handleFullLogging( log, method, logger, idPrefix,
                        messageParams, cause ) ;
                }
            }
        } ;

        // Load the Proxy using the same ClassLoader that loaded the interface
        ClassLoader loader = cls.getClassLoader() ;
        Class[] classes = { cls  } ;
        return (T)Proxy.newProxyInstance(loader, classes, inh ) ;
    }
}
