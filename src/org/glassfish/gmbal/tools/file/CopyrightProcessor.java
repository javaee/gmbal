/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.gmbal.tools.file ;

import java.util.List ;
import java.util.Map ;
import java.util.HashMap ;

import java.io.File ;
import java.io.FileInputStream;
import java.io.IOException ;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import org.glassfish.gmbal.generic.Pair;
import org.glassfish.gmbal.generic.UnaryFunction;
import org.glassfish.gmbal.tools.argparser.ArgParser;
import org.glassfish.gmbal.tools.argparser.DefaultValue;
import org.glassfish.gmbal.tools.argparser.Help;

public class CopyrightProcessor {
    private CopyrightProcessor() {} 

    private interface Arguments {
	@DefaultValue( "true" ) 
	@Help( "Set to true to validate copyright header; if false, "
            + "generate/update/insert copyright headers as needed" )
	boolean validate() ;

	@DefaultValue( "0" ) 
	@Help( "Set to >0 to get information about actions taken for "
            + "every file.  Larger values give more detail." )
	int verbose() ;

	@DefaultValue( "true" ) 
	@Help( "Set to true to avoid modifying any files" ) 
	boolean dryrun() ;

	@Help( "List of directories to process" ) 
	@DefaultValue( "" ) 
	List<File> roots() ;

	@Help( "List of directory names that should be skipped" ) 
	@DefaultValue( "" ) 
	List<String> skipdirs() ;

	@Help( "File containing text of copyright header.  This must not "
            + "include any comment characters" )
	@DefaultValue( "" ) 
	FileWrapper copyright() ;

	@DefaultValue( "1997" )
	@Help( "Default copyright start year, if not otherwise specified" ) 
	String startyear() ;

        @DefaultValue( "" )
        @Help( "File containing file suffix and name configurations for "
            + "recognizing different kinds of comment conventions")
        File configFile() ;
    }

    // File processing configuration:
    // Class:
    //  JAVA
    //  XML
    //  JAVA_LINE
    //  SCHEME
    //  SHELL
    //  SHELL_SCRIPT
    //  IGNORE
    // Each class has suffixes and file names specified in a property file:
    // cptool.<CLASS>.suffixes is a comma separated list (no spaces)
    //     of filename suffixes
    // cptool.<CLASS>.filenames is a comma separated list of specific filenames

    private static class FileProcessing {
        final String name ;
        final Scanner.Action action ;
        List<String> suffixes ;
        List<String> fileNames ;

        public FileProcessing( String name, Scanner.Action action ) {
            this.name = name ;
            this.action = action ;
            this.suffixes = new ArrayList<String>() ;
            this.fileNames = new ArrayList<String>() ;
        }

        String name() {
            return name ;
        }

        Scanner.Action action() {
            return action ;
        }

        List<String> suffixes() {
            return suffixes ;
        }

        FileProcessing suffixes( String... args ) {
            this.suffixes = Arrays.asList( args ) ;
            return this ;
        }

        List<String> fileNames() {
            return fileNames ;
        }

        FileProcessing fileNames( String... args ) {
            this.fileNames = Arrays.asList( args ) ;
            return this ;
        }
    }

    private static Map<String,FileProcessing> procMap ;

    // Block tags
    private static final String COPYRIGHT_BLOCK_TAG = "CopyrightBlock" ;
    private static final String SUN_COPYRIGHT_TAG = "SunCopyright" ;
    private static final String ORACLE_COPYRIGHT_TAG = "OracleCopyright" ;
    private static final String CORRECT_COPYRIGHT_TAG = "CorrectCopyright" ;

    private static void trace( String msg ) {
	System.out.println( msg ) ;
    }

    private static Block makeCopyrightLineCommentBlock(
        final Block copyrightText, final String prefix, final String tag ) {

	final Block result = new Block( copyrightText ) ;
	result.addPrefixToAll( prefix ) ;

	result.addTag( tag ) ;
	result.addTag( CORRECT_COPYRIGHT_TAG ) ;

	return result ;
    }

    private static Block makeCopyrightBlockCommentBlock( Block copyrightText, 
	String start, String prefix, String end, String tag ) {

	final Block result = new Block( copyrightText ) ;
	result.addPrefixToAll( prefix ) ;
	result.addBeforeFirst( start ) ;
	result.addAfterLast( end ) ;
	
	result.addTag( tag ) ;
	result.addTag( CORRECT_COPYRIGHT_TAG ) ;

	return result ;
    }

    private static final String COPYRIGHT = "Copyright" ;

    // Search for COPYRIGHT followed by white space, then [0-9]*-[0-9]*
    private static Pair<String,String> getCopyrightPair( String str ) {
        StringParser sp = new StringParser( str ) ;
        if (!sp.skipToString( COPYRIGHT )) {
            return null;
        }

        if (!sp.skipString( COPYRIGHT )) {
            return null;
        }

        if (!sp.skipWhitespace()) {
            return null;
        }

        String start = sp.parseInt() ;
        if (start == null) {
            return null;
        }

        if (!sp.skipString( "-" )) {
            return null;
        }

        String end = sp.parseInt() ;
        if (end == null) {
            return null;
        }

        return new Pair<String,String>( start, end ) ;
    }


    // These strings are found in the copyright text file.
    private static final String START_YEAR = "StartYear" ;
    private static final String LAST_YEAR = "LastYear" ;

    private static Block makeCopyrightBlock( Pair<String,String> years, 
	Block copyrightText) throws IOException {

	if (args.verbose() > 1) {
	    trace( "makeCopyrightBlock: years = " + years ) ;
	    trace( "makeCopyrightBlock: copyrightText = " + copyrightText ) ;

	    trace( "Contents of copyrightText block:" ) ;
	    for (String str : copyrightText.contents()) {
		trace( "\t" + str ) ;
	    }
	}

	Map<String,String> map = new HashMap<String,String>() ;
	map.put( START_YEAR, years.first() ) ;
	map.put( LAST_YEAR, years.second() ) ;
	Block blk = copyrightText.instantiateTemplate( map ) ;

	if (args.verbose() > 1) {
	    trace( "Contents of copyrightText block with start and end dates:" ) ;
	    for (String str : blk.contents()) {
		trace( "\t" + str ) ;
	    }
	}   

	return blk ;
    }

    private interface BlockParserCall
        extends UnaryFunction<FileWrapper,List<Block>> {}

    private static BlockParserCall makeBlockCommentParser( final String start, 
	final String end ) {

	return new BlockParserCall() {
            @Override
	    public String toString() {
		return "BlockCommentBlockParserCall[start=," + start
		    + ",end=" + end + "]" ;
	    }

	    public List<Block> evaluate( FileWrapper fw ) {
		try {
		    return BlockParser.parseBlocks( fw, start, end ) ;
		} catch (IOException exc) {
		    throw new RuntimeException( exc ) ;
		}
	    }
	} ;
    }

    private static BlockParserCall makeLineCommentParser(
        final String prefix ) {

	return new BlockParserCall() {
            @Override
	    public String toString() {
		return "LineCommentBlockParserCall[prefix=," + prefix + "]" ;
	    }

	    public List<Block> evaluate( FileWrapper fw ) {
		try {
		    return BlockParser.parseBlocks( fw, prefix ) ;
		} catch (IOException exc) {
		    throw new RuntimeException( exc ) ;
		}
	    } 
	} ;
    }

    private static void validationError( Block block, String msg, 
        FileWrapper fw ) {

	trace( "Copyright validation error: " + msg + " for " + fw ) ;
	if ((args.verbose() > 0) && (block != null)) {
	    trace( "Block=" + block ) ;
	    trace( "Block contents:" ) ;
	    for (String str : block.contents()) {
		trace( "\"" + str + "\"" ) ;
	    }
	}
    }

    // Strip out old Sun or Oracle copyright block.  Prepend new copyrightText.
    //
    // copyrightText is a Block containing a copyright template in the correct
    //    comment format.
    // parseCall is the correct block parser for splitting the file into Blocks.
    // defaultStartYear is the default year to use in copyright comments if not
    //     otherwise specified in an old copyright block.
    // afterFirstBlock is true if the copyright needs to start after the 
    //     first block in the file.
    private static Scanner.Action makeCopyrightBlockAction( 
        final Block copyrightText, final BlockParserCall parserCall,
        final String defaultStartYear, final boolean afterFirstBlock ) {

	if (args.verbose() > 0) {
	    trace( "makeCopyrightBlockAction: copyrightText = "
                + copyrightText ) ;
	    trace( "makeCopyrightBlockAction: parserCall = "
                + parserCall ) ;
	    trace( "makeCopyrightBlockAction: defaultStartYear = "
                + defaultStartYear ) ;
	    trace( "makeCopyrightBlockAction: afterFirstBlock = "
                + afterFirstBlock ) ;
	}

	return new Scanner.Action() {
            @Override
	    public String toString() {
		return "CopyrightBlockAction[copyrightText=" + copyrightText
		    + ",parserCall=" + parserCall 
		    + ",defaultStartYear=" + defaultStartYear 
		    + ",afterFirstBlock=" + afterFirstBlock + "]" ;
	    }

            private void validateBlock( final Block copyrightBlock, 
                final Block block, final int count, final FileWrapper fw ) {
                if (block.hasTags( ORACLE_COPYRIGHT_TAG,
                    COPYRIGHT_BLOCK_TAG,
                    BlockParser.COMMENT_BLOCK_TAG)) {
                    if (!copyrightBlock.equals( block )) {
                        validationError( block,
                            "block " + count + " has incorrect "
                            + "copyright text", fw ) ;
                    }
                } else {
                    validationError( block,
                        "Block " + count + " should be copyright "
                        + "but isn't", fw ) ;
                }
            }

            private boolean validateFile( final Block copyrightBlock,
                final List<Block> fileBlocks, final FileWrapper fw ) {

                // There should be an Oracle copyright block in the
                // first block (if afterFirstBlock is false), otherwise
                // in the second block.  It should entirely match
                // copyrightText
                int count = 0 ;
                for (Block block : fileBlocks) {
                    // Generally always return true, because we want
                    // to see ALL validation errors.

                    if (block.hasTags( COPYRIGHT_BLOCK_TAG,
                        SUN_COPYRIGHT_TAG )) {
                        validationError( block,
                            "File contains an old Sun Copyright block", fw ) ;
                        return true ;
                    }

                    final boolean checkBlock0 = !afterFirstBlock && count==0 ;
                    final boolean checkBlock1 = afterFirstBlock && count==1 ;

                    if (checkBlock0 || checkBlock1) {
                        validateBlock( copyrightBlock, block, count, fw ) ;
                        return true ;
                    }

                    if (count > 1) {
                        // should not get here!  Return false only
                        // in this case, because this is
                        // an internal error in the validator.
                        validationError( null,
                            "Neither first nor second block checked",
                            fw ) ;
                        return false ;
                    }

                    count++ ;
                }

                return true ;
            }

            private boolean rewriteFile( final boolean hadAnOldOracleCopyright,
                final Block copyrightBlock,
                final List<Block> fileBlocks, final FileWrapper fw ) throws IOException {

                // Re-write file, replacing the first block tagged
                // ORACLE_COPYRIGHT_TAG, COPYRIGHT_BLOCK_TAG, and
                // commentBlock with the copyrightText block.
                // Any old SUN_COPYRIGHT_TAG blocks are skipped.

                if (fw.canWrite()) {
                    trace( "Updating copyright/license header on file " + fw ) ;

                    // Note: this is dangerous: a crash before close
                    // will destroy the file!
                    boolean res = fw.delete() ;
                    if (args.verbose() > 1 && !res) {
                        trace( "Failed to delete file " + fw ) ;
                    }
                    fw.open( FileWrapper.OpenMode.WRITE ) ;

                    int count = 0 ;
                    for (Block block : fileBlocks) {
                        if (count==0 && !afterFirstBlock) {
                            copyrightBlock.write( fw ) ;
                        }

                        if (block.hasTags( COPYRIGHT_BLOCK_TAG,
                            BlockParser.COMMENT_BLOCK_TAG)) {
                            if (block.hasNoTags( ORACLE_COPYRIGHT_TAG,
                                SUN_COPYRIGHT_TAG )) {
                                block.write( fw ) ;
                            }
                        } else {
                            block.write( fw ) ;
                        }

                        if (count==0 && afterFirstBlock) {
                            copyrightBlock.write( fw ) ;
                        }

                        count++ ;
                    }
                } else {
                    if (args.verbose() > 1) {
                        trace( "Skipping file " + fw
                            + " because is is not writable" ) ;
                    }
                }

                return true ;
            }

	    public boolean evaluate( FileWrapper fw ) {
		try {
                    int cy = (new GregorianCalendar()).get( Calendar.YEAR ) ;
                    String currentYear = "" + cy ;
                    Pair<String,String> years = 
                        new Pair<String,String>( defaultStartYear, currentYear ) ;
		    boolean hadAnOldOracleCopyright = false ;
		    boolean hadAnOldSunCopyright = false ;
		    
		    // Convert file into blocks
		    final List<Block> fileBlocks = parserCall.evaluate( fw ) ;

		    // Tag blocks
		    for (Block block : fileBlocks) {
			String str = block.find( COPYRIGHT ) ;
			if (str != null) {
			    block.addTag( COPYRIGHT_BLOCK_TAG ) ;
			    if (str.contains( "Sun" )) {
				Pair<String,String> scp =
                                    getCopyrightPair( str ) ;
                                if (scp != null) {
                                    years = scp ;
                                }
				block.addTag( SUN_COPYRIGHT_TAG ) ;
				hadAnOldSunCopyright = true ;
			    }
			    if (str.contains( "Oracle" )) {
				Pair<String,String> scp =
                                    getCopyrightPair( str ) ;
                                if (scp != null && !hadAnOldSunCopyright) {
                                    years = scp ;
                                }
				block.addTag( ORACLE_COPYRIGHT_TAG ) ;
				hadAnOldOracleCopyright = true ;
			    }
			}
		    }

		    if (args.verbose() > 1) {
			trace( "copyrightBlockAction: blocks in file " + fw ) ;
			for (Block block : fileBlocks) {
			    trace( "\t" + block ) ;
			    for (String str : block.contents()) {
				trace( "\t\t" + str ) ;
			    }
			}
		    }

		    Block cb = makeCopyrightBlock( years, copyrightText ) ;

		    if (args.validate()) {
                        validateFile( cb, fileBlocks, fw ) ;
		    } else {
                        rewriteFile( hadAnOldOracleCopyright, cb, fileBlocks,
                            fw ) ;
		    }
		} catch (IOException exc ) {
		    trace( "Exception while processing file " + fw + ": "
                        + exc ) ;
		    exc.printStackTrace() ;
		    return false ;
		} finally {
		    fw.close() ;
		}

		return true ;
	    }
	} ;
    }

    // Note: we could also make the block and line comment processors 
    // configurable, but that seems like overkill.
    private static final String JAVA_COMMENT_START = "/*" ;
    private static final String JAVA_COMMENT_PREFIX = " *" ;
    private static final String JAVA_COMMENT_END = "*/" ; 

    private static final boolean JAVA_AFTER_FIRST_BLOCK = false ;
    private static final String JAVA_FORMAT_TAG = "JavaFormat" ;

    private static final String XML_COMMENT_START = "<!--" ;
    private static final String XML_COMMENT_PREFIX = " " ;
    private static final String XML_COMMENT_END = "-->" ;
    private static final boolean XML_AFTER_FIRST_BLOCK = true ;
    private static final String XML_FORMAT_TAG = "XmlFormat" ;

    private static final String JAVA_LINE_PREFIX = "// " ;
    private static final boolean JAVA_LINE_AFTER_FIRST_BLOCK = false ;
    private static final String JAVA_LINE_FORMAT_TAG = "JavaLineFormat" ;

    private static final String SCHEME_PREFIX = "; " ;
    private static final boolean SCHEME_AFTER_FIRST_BLOCK = false ;
    private static final String SCHEME_FORMAT_TAG = "SchemeFormat" ;

    private static final String SHELL_PREFIX = "# " ;
    private static final boolean SHELL_AFTER_FIRST_BLOCK = true ;
    private static final boolean SHELL_SCRIPT_AFTER_FIRST_BLOCK = true ;
    private static final String SHELL_FORMAT_TAG = "ShellFormat" ;
    // Note that there are actually 2 SHELL parsers for SHELL and SHELL_SCRIPT.
    // The difference is that the SHELL_SCRIPT always starts with #!.

    private static void addToProcMap( FileProcessing fp ) {
        procMap.put( fp.name(), fp ) ;
    }

    private static void initializeScanners( ActionFactory af ) 
        throws IOException {

        procMap = new HashMap<String,FileProcessing>() ;

        // Create the blocks needed for different forms of the
        // copyright comment template
        final Block crtext = BlockParser.getBlock( args.copyright() ) ;

        final Block javaCopyrightText =
            makeCopyrightBlockCommentBlock( crtext,
                JAVA_COMMENT_START + " ", JAVA_COMMENT_PREFIX + " ",
                " " + JAVA_COMMENT_END + " ", JAVA_FORMAT_TAG ) ;

        final Block xmlCopyrightText =
            makeCopyrightBlockCommentBlock( crtext,
                XML_COMMENT_START, XML_COMMENT_PREFIX, XML_COMMENT_END,
                XML_FORMAT_TAG ) ;

        final Block javaLineCopyrightText =
            makeCopyrightLineCommentBlock( crtext, JAVA_LINE_PREFIX,
            JAVA_LINE_FORMAT_TAG ) ;

        final Block schemeCopyrightText =
            makeCopyrightLineCommentBlock( crtext, SCHEME_PREFIX,
            SCHEME_FORMAT_TAG ) ;

        final Block shellCopyrightText =
            makeCopyrightLineCommentBlock( crtext, SHELL_PREFIX,
            SHELL_FORMAT_TAG ) ;

        if (args.verbose() > 0) {
            trace( "Main: copyrightText = " + crtext ) ;
            trace( "Main: javaCopyrightText = " + javaCopyrightText ) ;
            trace( "Main: xmlCopyrightText = " + xmlCopyrightText ) ;
            trace( "Main: javaLineCopyrightText = " + javaLineCopyrightText ) ;
            trace( "Main: schemeCopyrightText = " + schemeCopyrightText ) ;
            trace( "Main: shellCopyrightText = " + shellCopyrightText ) ;
        }

        // Create the BlockParserCalls needed for the actions
        BlockParserCall javaBlockParserCall = makeBlockCommentParser(
            JAVA_COMMENT_START, JAVA_COMMENT_END ) ;

        BlockParserCall xmlBlockParserCall = makeBlockCommentParser(
            XML_COMMENT_START, XML_COMMENT_END ) ;

        BlockParserCall javaLineParserCall =
            makeLineCommentParser( JAVA_LINE_PREFIX ) ;

        BlockParserCall schemeLineParserCall =
            makeLineCommentParser( SCHEME_PREFIX ) ;

        BlockParserCall shellLineParserCall =
            makeLineCommentParser( SHELL_PREFIX ) ;

        // Create the default mappings from suffixes and file names to 
        // actions.
        addToProcMap( new FileProcessing( "JAVA",
            makeCopyrightBlockAction( javaCopyrightText,
                javaBlockParserCall,
                args.startyear(), JAVA_AFTER_FIRST_BLOCK ) )
            .suffixes( "c", "h", "java", "sjava", "idl" ) ) ;

        addToProcMap( new FileProcessing( "XML",
            makeCopyrightBlockAction( xmlCopyrightText,
                xmlBlockParserCall,
                args.startyear(), XML_AFTER_FIRST_BLOCK ) )
            .suffixes( "htm", "html", "xml", "dtd" ) ) ;

        addToProcMap( new FileProcessing( "SCHEME",
            makeCopyrightBlockAction( schemeCopyrightText,
                schemeLineParserCall,
                args.startyear(), SCHEME_AFTER_FIRST_BLOCK ) )
            .suffixes( "mc", "mcd", "scm", "vthought" ) ) ;

        addToProcMap( new FileProcessing( "JAVA_LINE",
            makeCopyrightBlockAction( javaLineCopyrightText,
                javaLineParserCall,
                args.startyear(), JAVA_LINE_AFTER_FIRST_BLOCK ) )
            .suffixes( "tdesc", "policy", "secure" ) ) ;

        addToProcMap( new FileProcessing( "SHELL",
            makeCopyrightBlockAction( shellCopyrightText,
                shellLineParserCall,
                args.startyear(), SHELL_AFTER_FIRST_BLOCK ) )
            .suffixes( "config", "properties", "prp", "data", "txt", "text" )
            .fileNames( "Makefile" ) ) ;

        addToProcMap( new FileProcessing( "SHELL_SCRIPT",
            makeCopyrightBlockAction( shellCopyrightText,
                shellLineParserCall,
                args.startyear(), SHELL_SCRIPT_AFTER_FIRST_BLOCK ) )
            .suffixes( "ksh", "sh" ) ) ;

        addToProcMap( new FileProcessing( "IGNORE", af.getSkipAction() )
            .suffixes( "bnd", "sxc", "sxi", "sxw", "odp", "gif", "png", "jar", 
                "zip", "jpg", "pom", "pdf", "doc", "mif", "fm", "book",
                "cvsignore", "hgignore", "hgtags", "list", "old", "orig", "rej",
                "swp", "swo", "class", "o", "css" )
            .fileNames( "NORENAME", "errorfile" ) ) ;
    }

    private static List<String> getProp( final Properties props,
        final String name, final List<String> defaultValue ) {
        String str = props.getProperty(name) ;
        if (str == null) {
            return defaultValue ;
        } else {
            return Arrays.asList( str.split( "," ) ) ;
        }
    }

    private static Arguments args ;

    public static void main(String[] strs) {
	ArgParser<Arguments> ap = new ArgParser<Arguments>( Arguments.class ) ;
	args = ap.parse( strs ) ;

	if (args.verbose() > 0) {
	    trace( "Main: args:\n" + args ) ;
	}

	try {
            ActionFactory af = new ActionFactory( args.verbose(),
                args.dryrun() ) ;
            initializeScanners( af ) ;

            // override any defaults from the config file
            Properties props = new Properties() ;
            if ((args.configFile() != null) && args.configFile().exists()) {
                try {
                    InputStream is = new FileInputStream( args.configFile()) ;
                    props.load(is) ;
                } catch (IOException exc) {
                    System.out.println(
                        "Exception " + exc
                            + " while attempting to read configFile "
                            + args.configFile() ) ;
                }
            }

	    // Configure the recognizer
            Recognizer recognizer = af.getRecognizerAction() ;

            for (Map.Entry<String,FileProcessing> entry : procMap.entrySet()) {
                String name = entry.getKey() ;

                FileProcessing fp = entry.getValue() ;
                Scanner.Action action = fp.action() ;

                // Install the actions in the recognizer, overriding the
                // defaults from the fp instance with data from the
                // properties file, if present.
                List<String> suffixes = getProp( props,
                    "cptool." + name + ".suffixes", fp.suffixes() ) ;
                List<String> fileNames = getProp( props, 
                    "cptool." + name + ".filenames", fp.fileNames() ) ;

                for (String str : suffixes) {
                    recognizer.addKnownSuffix( str, action ) ;
                }

                for (String str : fileNames) {
                    recognizer.addKnownName( str, action ) ;
                }
            }

	    if (args.verbose() > 0) {
		trace( "Main: contents of recognizer:" ) ;
		recognizer.dump() ;
	    }

	    Scanner scanner = new Scanner( args.verbose(), args.roots() ) ;
	    for (String str : args.skipdirs() ) {
                scanner.addDirectoryToSkip(str);
            }

	    // Finally, we process all files
	    scanner.scan( recognizer ) ;
	} catch (IOException exc) {
	    System.out.println( "Exception while processing: " + exc ) ;
	    exc.printStackTrace() ;
	}
    }
}

