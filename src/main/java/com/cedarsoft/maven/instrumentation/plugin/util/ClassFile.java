package com.cedarsoft.maven.instrumentation.plugin.util;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;

import com.cedarsoft.maven.instrumentation.plugin.ClassTransformationException;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public class ClassFile {
  @Nonnull
  private final File classFile;
  @Nonnull
  private final ClassLoader dependenciesClassLoader;
  @Nonnull
  private final CtClass compiledClass;

  public ClassFile( @Nonnull final File classFile, @Nonnull ClassLoader dependenciesClassLoader) throws IOException {
    this.classFile = classFile;
    this.dependenciesClassLoader = dependenciesClassLoader;
    final ClassPool classPool = new ClassPool(true);
    classPool.appendClassPath(new LoaderClassPath(dependenciesClassLoader));

    final InputStream inputStream = new BufferedInputStream( new FileInputStream( classFile ) );
    try {
      compiledClass = classPool.makeClass( inputStream );
    } finally {
      inputStream.close();
    }
  }

  @Nonnull
  public String getClassName() {
    return compiledClass.getName();
  }

  public void transform( @Nonnull final ClassFileTransformer agent ) throws ClassTransformationException {
    try {
      doTransform( agent );
    } catch ( final MalformedURLException e ) {
      throw new RuntimeException( "Should not happen.  Could not create URL from file.", e );
    } catch ( final IOException e ) {
      throw new ClassTransformationException( "Failed to write transformed class file contents.", e );
    } catch ( final CannotCompileException e ) {
      throw new RuntimeException( "Should not happen. Failed to convert a valid compiled class into bytecode", e );
    } catch ( final IllegalClassFormatException e ) {
      throw new RuntimeException( "Should not happen.  The class file does not contain valid content", e );
    }
  }

  private void doTransform( @Nonnull final ClassFileTransformer agent ) throws MalformedURLException, CannotCompileException, IOException, IllegalClassFormatException {
    final ClassLoader loader = getClassLoader();
    final String className = getInternalClassName();
    final Class<?> classBeingRedefined = compiledClass.toClass( loader, loader.getClass().getProtectionDomain() );
    final byte[] classBytes = compiledClass.toBytecode();
    final ProtectionDomain protectionDomain = classBeingRedefined.getProtectionDomain();

    final byte[] transformedBytes = agent.transform( loader, className, classBeingRedefined, protectionDomain, classBytes );

    replaceClassContents( transformedBytes );
  }

  @Nonnull
  private ClassLoader getClassLoader() throws MalformedURLException {
    final File parentDir = getClassParentDir();
    return new URLClassLoader( new URL[]{parentDir.toURI().toURL()}, dependenciesClassLoader);
  }

  @Nonnull
  public File getClassParentDir() {
    final String className = compiledClass.getName();
    final int packageCount = className.split( "\\." ).length;
    File parentDir = classFile;
    for ( int i = 0; i < packageCount; i++ ) {
      parentDir = parentDir.getParentFile();
    }
    return parentDir;
  }

  /**
   * @return The internal class name as specified in the JVM Specification
   *
   * @see http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html
   */
  @Nonnull
  private String getInternalClassName() {
    return compiledClass.getName().replace( '.', '/' );
  }

  private void replaceClassContents( @Nonnull final byte[] replacementBytes ) throws IOException {
    final BufferedOutputStream bufferedStream = new BufferedOutputStream( new FileOutputStream( classFile ) );
    try {
      bufferedStream.write( replacementBytes );
      bufferedStream.flush();
    } finally {
      bufferedStream.close();
    }
  }

  @Override
  public String toString() {
    return "ClassFile [classFile=" + classFile + "]";
  }
}