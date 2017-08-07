package org.codehaus.mojo.animal_sniffer;

/*
 * The MIT License
 *
 * Copyright (c) 2008 Kohsuke Kawaguchi and codehaus.org.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Checks the signature against classes in this list.
 *
 * @author Kohsuke Kawaguchi
 */
public class SignatureChecker
    extends ClassFileVisitor
{
    /**
     * The fully qualified name of the annotation to use to annotate methods/fields/classes that are
     * to be ignored by animal sniffer.
     */
    public static final String ANNOTATION_FQN = "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement";

    /**
     * Similar to {@link #ANNOTATION_FQN}. Kept for backward compatibility reasons
     */
    public static final String PREVIOUS_ANNOTATION_FQN = "org.jvnet.animal_sniffer.IgnoreJRERequirement";

    private final Map<String, Clazz> classes = new HashMap<String, Clazz>();

    private final Logger logger;

    /**
     * Classes in this packages are considered to be resolved elsewhere and
     * thus not a subject of the error checking when referenced.
     */
    private final List<MatchRule> ignoredPackageRules;

    private final Set<String> ignoredPackages;

    private final Set<String> ignoredOuterClassesOrMethods = new HashSet<String>();

    private boolean hadError = false;

    private List<File> sourcePath;

    private Collection<String> annotationDescriptors;

    public static void main( String[] args )
        throws Exception
    {
        Set<String> ignoredPackages = new HashSet<String>();
        ignoredPackages.add( "org.jvnet.animal_sniffer.*" );
        ignoredPackages.add( "org.codehaus.mojo.animal_sniffer.*" );
        ignoredPackages.add( "org.objectweb.*" );
        new SignatureChecker( new FileInputStream( "signature" ), ignoredPackages,
                              new PrintWriterLogger( System.out ) ).process( new File( "target/classes" ) );
    }

    public SignatureChecker( InputStream in, Set<String> ignoredPackages, Logger logger )
        throws IOException
    {
        this.ignoredPackages = new HashSet<String>();
        this.ignoredPackageRules = new LinkedList<MatchRule>();
        for(String wildcard : ignoredPackages )
        {
            if ( wildcard.indexOf( '*' ) == -1 && wildcard.indexOf( '?' ) == -1 )
            {
                this.ignoredPackages.add( wildcard.replace( '.', '/' ) );
            }
            else
            {
                this.ignoredPackageRules.add( newMatchRule( wildcard.replace( '.', '/' ) ) );
            }
        }
        this.annotationDescriptors = new HashSet<String>();
        this.annotationDescriptors.add( toAnnotationDescriptor( ANNOTATION_FQN ) );
        this.annotationDescriptors.add( toAnnotationDescriptor( PREVIOUS_ANNOTATION_FQN ) );

        this.logger = logger;
        ObjectInputStream ois = null;
        try
        {
            ois = new ObjectInputStream( new GZIPInputStream( in ) );
            while ( true )
            {
                Clazz c = (Clazz) ois.readObject();
                if ( c == null )
                {
                    return; // finished
                }
                classes.put( c.getName(), c );
            }
        }
        catch ( ClassNotFoundException e )
        {
            throw new NoClassDefFoundError( e.getMessage() );
        }
        finally
        {
            if ( ois != null )
            {
                try
                {
                    ois.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }

    /** @since 1.9 */
    public void setSourcePath( List<File> sourcePath )
    {
        this.sourcePath = sourcePath;
    }

    /**
     * Sets the annotation type(s) that this checker should consider to ignore annotated
     * methods, classes or fields.
     * <p>
     * By default, the {@link #ANNOTATION_FQN} and {@link #PREVIOUS_ANNOTATION_FQN} are
     * used.
     * <p>
     * If you want to <strong>add</strong> an extra annotation types, make sure to add
     * the standard one to the specified lists.
     *
     * @param annotationTypes a list of the fully qualified name of the annotation types
     *                        to consider for ignoring annotated method, class and field
     * @since 1.11
     */
    public void setAnnotationTypes( Collection<String> annotationTypes )
    {
        this.annotationDescriptors.clear();
        for ( String annotationType : annotationTypes )
        {
            annotationDescriptors.add( toAnnotationDescriptor( annotationType ) );
        }
    }

    protected void process( final String name, InputStream image )
        throws IOException
    {
        ClassReader cr = new ClassReader( image );

        try
        {
            cr.accept( new CheckingVisitor( name ), 0 );
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            logger.error( "Bad class file " + name );
            // MANIMALSNIFFER-9 it is a pity that ASM does not throw a nicer error on encountering a malformed
            // class file.
            IOException ioException = new IOException( "Bad class file " + name );
            ioException.initCause( e );
            throw ioException;
        }
    }

    private static interface MatchRule
    {
        boolean matches( String text );
    }

    private static class PrefixMatchRule
        implements SignatureChecker.MatchRule
    {
        private final String prefix;

        public PrefixMatchRule( String prefix )
        {
            this.prefix = prefix;
        }

        public boolean matches( String text )
        {
            return text.startsWith( prefix );
        }
    }

    private static class ExactMatchRule
        implements SignatureChecker.MatchRule
    {
        private final String match;

        public ExactMatchRule( String match )
        {
            this.match = match;
        }

        public boolean matches( String text )
        {
            return match.equals( text );
        }
    }

    private static class RegexMatchRule
        implements SignatureChecker.MatchRule
    {
        private final Pattern regex;

        public RegexMatchRule( Pattern regex )
        {
            this.regex = regex;
        }

        public boolean matches( String text )
        {
            return regex.matcher( text ).matches();
        }
    }

    private SignatureChecker.MatchRule newMatchRule( String matcher )
    {
        int i = matcher.indexOf( '*' );
        if ( i == -1 )
        {
            return new ExactMatchRule( matcher );
        }
        if ( i == matcher.length() - 1 )
        {
            return new PrefixMatchRule( matcher.substring( 0, i ) );
        }
        return new RegexMatchRule( RegexUtils.compileWildcard( matcher ) );
    }

    public boolean isSignatureBroken()
    {
        return hadError;
    }

    private class CheckingVisitor
        extends ClassVisitor
    {
        private final Set<String> ignoredPackageCache;

        private String packagePrefix;
        private int line;
        private String name;
        private String internalName;

        private boolean ignoreClass = false;

        public CheckingVisitor( String name )
        {
            super(Opcodes.ASM5);
            this.ignoredPackageCache = new HashSet<String>( 50 * ignoredPackageRules.size() );
            this.name = name;
        }

        @Override
        public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
        {
            internalName = name;
            packagePrefix = name.substring(0, name.lastIndexOf( '/' ) + 1 );
        }

        @Override
        public void visitSource( String source, String debug )
        {
            for ( File root : sourcePath )
            {
                File s = new File( root, packagePrefix + source );
                if ( s.isFile() )
                {
                    name = s.getAbsolutePath();
                }
            }
        }

        @Override
        public void visitOuterClass( String owner, String name, String desc )
        {
            if ( ignoredOuterClassesOrMethods.contains( owner ) ||
                 ( name != null && ignoredOuterClassesOrMethods.contains ( owner + "#" + name + desc ) ) )
            {
                ignoreClass = true;
            }
        }

        public boolean isIgnoreAnnotation(String desc)
        {
            for ( String annoDesc : annotationDescriptors )
            {
                if ( desc.equals( annoDesc ) )
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            if ( isIgnoreAnnotation( desc ) )
            {
                ignoreClass = true;
                ignoredOuterClassesOrMethods.add( internalName );
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod( int access, final String name, final String desc, String signature, String[] exceptions )
        {
            return new MethodVisitor(Opcodes.ASM5)
            {
                /**
                 * True if @IgnoreJRERequirement is set.
                 */
                boolean ignoreError = ignoreClass;

                @Override
                public AnnotationVisitor visitAnnotation( String annoDesc, boolean visible )
                {
                    if ( isIgnoreAnnotation(annoDesc) )
                    {
                        ignoreError = true;
                        ignoredOuterClassesOrMethods.add( internalName + "#" + name + desc );
                    }
                    return super.visitAnnotation( annoDesc, visible );
                }

                private static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";

                @Override
                public void visitInvokeDynamicInsn( String name, String desc, Handle bsm, Object... bsmArgs )
                {
                    if ( LAMBDA_METAFACTORY.equals( bsm.getOwner() ) )
                    {
                        if ( "metafactory".equals( bsm.getName() ) ||
                             "altMetafactory".equals( bsm.getName() ) )
                        {
                            // check the method reference
                            Handle methodHandle = (Handle) bsmArgs[1];
                            check( methodHandle.getOwner(), methodHandle.getName() + methodHandle.getDesc() );
                            // check the functional interface type
                            checkType( Type.getReturnType( desc ) );
                        }
                    }
                }

                @Override
                public void visitMethodInsn( int opcode, String owner, String name, String desc, boolean itf )
                {
                    checkType( Type.getReturnType( desc ) );
                    check( owner, name + desc );
                }

                @Override
                public void visitTypeInsn( int opcode, String type )
                {
                    checkType( type );
                }

                @Override
                public void visitFieldInsn( int opcode, String owner, String name, String desc )
                {
                    check( owner, name + '#' + desc );
                }

                @Override
                public void visitLineNumber( int line, Label start )
                {
                    CheckingVisitor.this.line = line;
                }

                private void checkType( Type asmType )
                {
                    if ( asmType == null )
                    {
                        return;
                    }
                    if ( asmType.getSort() == Type.OBJECT )
                    {
                        checkType( asmType.getInternalName() );
                    }
                    if ( asmType.getSort() == Type.ARRAY )
                    {
                        // recursive call
                        checkType( asmType.getElementType() );
                    }
                }

                private void checkType( String type )
                {
                    if ( shouldBeIgnored( type ) )
                    {
                        return;
                    }
                    if ( type.charAt( 0 ) == '[' )
                    {
                        return; // array
                    }
                    Clazz sigs = classes.get( type );
                    if ( sigs == null )
                    {
                        error( type, null );
                    }
                }

                private void check( String owner, String sig )
                {
                    if ( shouldBeIgnored( owner ) )
                    {
                        return;
                    }
                    if ( find( classes.get( owner ), sig, true ) )
                    {
                        return; // found it
                    }
                    error( owner, sig );
                }

                private boolean shouldBeIgnored( String type )
                {
                    if ( ignoreError )
                    {
                        return true;    // warning suppressed in this context
                    }
                    if ( type.charAt( 0 ) == '[' )
                    {
                        return true; // array
                    }

                    if ( ignoredPackages.contains( type ) || ignoredPackageCache.contains( type ) )
                    {
                        return true;
                    }
                    for ( MatchRule rule : ignoredPackageRules )
                    {
                        if ( rule.matches( type ) )
                        {
                            ignoredPackageCache.add( type );
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

        /**
         * If the given signature is found in the specified class, return true.
         * @param baseFind TODO
         */
        private boolean find( Clazz c , String sig , boolean baseFind  )
        {
            if ( c == null )
            {
                return false;
            }
            if ( c.getSignatures().contains( sig ) )
            {
                return true;
            }

            if ( sig.startsWith( "<" ) )
            // constructor and static initializer shouldn't go up the inheritance hierarchy
            {
                return false;
            }

            if ( find( (Clazz) classes.get( c.getSuperClass() ), sig, false ) )
            {
                return true;
            }

            if ( c.getSuperInterfaces() != null )
            {
                for ( int i = 0; i < c.getSuperInterfaces().length; i++ )
                {
                    if ( find( classes.get( c.getSuperInterfaces()[i] ), sig, false ) )
                    {
                        return true;
                    }
                }
            }

            // This is a rare case and quite expensive, so moving it to the end of this method and only execute it from
            // first find-call.
            if ( baseFind )
            {
                // MANIMALSNIFFER-49
                Pattern returnTypePattern = Pattern.compile( "(.+\\))L(.+);" );
                Matcher returnTypeMatcher = returnTypePattern.matcher( sig );
                if ( returnTypeMatcher.matches() )
                {
                    String method = returnTypeMatcher.group( 1 );
                    String returnType = returnTypeMatcher.group( 2 );

                    Clazz returnClass = classes.get( returnType );

                    if ( returnClass != null && returnClass.getSuperClass() != null )
                    {
                        String oldSignature = method + 'L' + returnClass.getSuperClass() + ';';
                        if ( find( c, oldSignature, false ) )
                        {
                            logger.info( name + ( line > 0 ? ":" + line : "" )
                                + ": Covariant return type change detected: "
                                + toSourceForm( c.getName(), oldSignature ) + " has been changed to "
                                + toSourceForm( c.getName(), sig ) );
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void error( String type, String sig )
        {
            hadError = true;
            logger.error(name + (line > 0 ? ":" + line : "") + ": Undefined reference: " + toSourceForm( type, sig ) );
        }
    }

    static String toSourceForm( String type, String sig )
    {
        String sourceType = toSourceType( type );
        if ( sig == null )
        {
            return sourceType;
        }
        int hash = sig.indexOf( '#' );
        if ( hash != -1 )
        {
            return toSourceType( CharBuffer.wrap( sig, hash + 1, sig.length() ) ) + " " + sourceType + "." + sig.substring( 0, hash );
        }
        int lparen = sig.indexOf( '(' );
        if ( lparen != -1 )
        {
            int rparen = sig.indexOf( ')' );
            if ( rparen != -1 )
            {
                StringBuilder b = new StringBuilder();
                String returnType = sig.substring( rparen + 1 );
                if ( returnType.equals( "V" ) )
                {
                    b.append( "void" );
                }
                else
                {
                    b.append( toSourceType( CharBuffer.wrap( returnType ) ) );
                }
                b.append( ' ' );
                b.append( sourceType );
                b.append( '.' );
                // XXX consider prettifying <init>
                b.append( sig.substring( 0, lparen ) );
                b.append( '(' );
                boolean first = true;
                CharBuffer args = CharBuffer.wrap( sig, lparen + 1, rparen );
                while ( args.hasRemaining() )
                {
                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        b.append( ", " );
                    }
                    b.append( toSourceType( args ) );
                }
                b.append( ')' );
                return b.toString();
            }
        }
        return "{" + type + ":" + sig + "}"; // ??
    }

    static String toAnnotationDescriptor( String classFqn )
    {
        return "L" + fromSourceType( classFqn ) + ";";
    }

    private static String toSourceType( CharBuffer type )
    {
        switch ( type.get() )
        {
            case 'L':
                for ( int i = type.position(); i < type.limit(); i++ )
                {
                    if ( type.get( i ) == ';' )
                    {
                        String text = type.subSequence( 0, i - type.position() ).toString();
                        type.position( i + 1 );
                        return toSourceType( text );
                    }
                }
                return "{" + type + "}"; // ??
            case '[':
                return toSourceType( type ) + "[]";
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'D':
                return "double";
            case 'F':
                return "float";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'S':
                return "short";
            case 'Z':
                return "boolean";
            default:
                return "{" + type + "}"; // ??
        }
    }

    private static String toSourceType( String text )
    {
        return text.replaceFirst( "^java/lang/([^/]+)$", "$1" ).replace( '/', '.' ).replace( '$', '.' );
    }

    private static String fromSourceType( String text )
    {
        return text.replace( '.', '/' ).replace( '.', '$' );
    }

}
