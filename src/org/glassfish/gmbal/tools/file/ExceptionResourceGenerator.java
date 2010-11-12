package org.glassfish.gmbal.tools.file;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.glassfish.gmbal.generic.Algorithms;
import org.glassfish.gmbal.generic.Algorithms.Action;
import org.glassfish.gmbal.logex.ExceptionWrapper;
import org.glassfish.gmbal.logex.Log;
import org.glassfish.gmbal.logex.Message;
import org.glassfish.gmbal.tools.argparser.ArgParser;
import org.glassfish.gmbal.tools.argparser.DefaultValue;
import org.glassfish.gmbal.tools.argparser.Help;

/** Scans a directory looking for class files.  For each class file,
 * if the class file is annotated with ExceptionWrapper, extract the
 * messages and write out into a resource file.
 *
 * @author ken
 */
public class ExceptionResourceGenerator {
    private final Arguments args ;
    private final FileWrapper dest ;

    private ExceptionResourceGenerator( String[] strs ) throws IOException {
        ArgParser<Arguments> ap = new ArgParser<Arguments>( Arguments.class ) ;
        args = ap.parse( strs ) ;
        args.destination().delete() ; // ignore return: just want to start
                                      // with an empty file.
        dest = new FileWrapper( args.destination() ) ;
        dest.open(FileWrapper.OpenMode.WRITE);
        dest.writeLine( "### Resource file generated on " + new Date() ) ;
    }

    private interface Arguments {
	@Help( "Set to >0 to get information about actions taken for every "
            + "file.  Larger values give more detail." )
	@DefaultValue( "0" )
	int verbose() ;

	@Help( "Source directory for classes to scan" )
	@DefaultValue( "" )
	File source() ;

	@Help( "Destination file for resources" )
	@DefaultValue( "" )
	File destination() ;
    }

    private void msg(String string) {
        System.out.println( string ) ;
    }

    private static String getLoggerName( Class<?> cls ) {
        ExceptionWrapper ew = cls.getAnnotation( ExceptionWrapper.class ) ;
        String str = ew.loggerName() ;
        if (str.length() == 0) {
            str = cls.getPackage().getName() ;
        }
        return str ;
    }

    /** Generate a list of Strings for a resource file for the given
     * exception and log handling class.
     * @param cls The class that describes messages for logging.
     * @return A list of strings suitable for inclusion in a resource bundle.
     */
    public static List<String> getResources( final Class<?> cls ) {
        // Check that cls is annotated with @ExceptionWrapper
        // For each method of cls that is annotated with @Message
        //     add a string of the form
        //     <logger name>.<method name> = "<idPrefix><id> : <message text>"
        //     to the output.
        final List<String> result = new ArrayList<String>() ;
        final List<Method> methods = Algorithms.doPrivileged(
            new Action<List<Method>>() {
                public List<Method> run() throws Exception {
                    return Arrays.asList( cls.getDeclaredMethods() ) ;
                }
            } ) ;

        ExceptionWrapper ew = cls.getAnnotation( ExceptionWrapper.class ) ;
        if (ew != null) {
            String prefix = ew.idPrefix() ;
            String loggerName = getLoggerName( cls ) ;
            for (Method m : methods) {
                final Log log = m.getAnnotation( Log.class ) ;
                final String logId = (log == null) ? "" : "" + log.id() ;
                final Message message = m.getAnnotation( Message.class ) ;
                if (message == null) {
                    System.out.println(
                        "No @Message annotation found for method " + m ) ;
                } else {
                    final StringBuilder sb = new StringBuilder() ;
                    sb.append( loggerName ) ;
                    sb.append( "." ) ;
                    sb.append( m.getName() ) ;
                    sb.append( "=\"" ) ;
                    sb.append( prefix ) ;
                    sb.append( logId ) ;
                    sb.append( ": " ) ;
                    sb.append( message.value() ) ;
                    sb.append( "\"" ) ;
                    result.add( sb.toString() ) ;
                }
            }
        }

        return result ;
    }

    Scanner.Action action = new Scanner.Action() {
        public boolean evaluate(FileWrapper arg) {
            String fileName = arg.getAbsoluteName() ;
            if (fileName.endsWith(".class")) {
                if (args.verbose() > 0) {
                    msg( "Checking class file " + fileName ) ;
                }

                final String className = fileName.substring (
                    args.source().getAbsolutePath().length() + 1,
                    fileName.length() - ".class".length() )
                    .replace( File.separatorChar, '.' );

                try {
                    Class cls = Class.forName( className ) ;
                    List<String> resStrings = getResources(cls) ;

                    if (resStrings.size() > 0) {
                        dest.writeLine( "#" ) ;
                        dest.writeLine( "# Resources for class " + className );
                        dest.writeLine( "#" ) ;
                    }

                    for (String str : resStrings) {
                        dest.writeLine(str);
                    }
                } catch (Exception exc ) {
                    if (args.verbose() > 0) {
                        msg( "Error in processing class " + className ) ;
                        exc.printStackTrace();
                    }
                }
            }
            
            return true ;
        }

    } ;

    private void run() throws IOException {
        // set up scanner for args.source, which must be a directory
        try {
            Scanner scanner = new Scanner( args.verbose(), args.source() ) ;
            scanner.scan(action);
        } finally {
            dest.close() ;
        }
    }

    public static void main( String[] strs ) throws IOException {
        (new ExceptionResourceGenerator(strs)).run() ;
    }
}
