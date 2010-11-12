/* 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
