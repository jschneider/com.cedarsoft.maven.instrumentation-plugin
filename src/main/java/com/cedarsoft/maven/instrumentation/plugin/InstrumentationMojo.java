package com.cedarsoft.maven.instrumentation.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.cedarsoft.maven.instrumentation.plugin.util.ClassFile;
import com.cedarsoft.maven.instrumentation.plugin.util.ClassFileLocator;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 * @goal instrument
 * @phase process-classes
 */
public class InstrumentationMojo extends AbstractMojo {
  /**
   * The fully qualified class names of the transformers to apply.
   *
   * @parameter
   * @required
   */
  private List<String> classTransformers;

  /**
   * @parameter expression="${project.build.outputDirectory}"
   * @read-only
   * @required
   */
  private String outputDirectory;


  /**
   * The maven session
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject mavenProject;

  protected MavenProject getProject() {
    return mavenProject;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (getProject().getPackaging().equals("pom")) {
      return;
    }

    getLog().info("Starting InstrumentationMojo");

    final Collection<ClassFileTransformer> agents = getAgents();
    final File outputDirectoryDir = new File(outputDirectory);
    final Collection<? extends ClassFile> classFiles = createLocator().findClasses(outputDirectoryDir);

    performClassTransformation(classFiles, agents);
  }

  private static void performClassTransformation( @Nonnull final Iterable<? extends ClassFile> classFiles, @Nonnull final Iterable<? extends ClassFileTransformer> agents ) throws MojoExecutionException {
    for ( final ClassFile classFile : classFiles ) {
      for ( final ClassFileTransformer agent : agents ) {
        transformClass( classFile, agent );
      }
    }
  }

  private static void transformClass( @Nonnull final ClassFile classFile, @Nonnull final ClassFileTransformer agent ) throws MojoExecutionException {
    try {
      classFile.transform( agent );
    } catch ( final ClassTransformationException e ) {
      final String message = MessageFormat.format( "Failed to transform class: {0}, using ClassFileTransformer, {1}", classFile, agent.getClass() );
      throw new MojoExecutionException( message, e );
    }
  }

  private Collection<ClassFileTransformer> getAgents() throws MojoExecutionException {
    final Collection<ClassFileTransformer> agents = new ArrayList<ClassFileTransformer>();
    for ( final String className : classTransformers ) {
      final ClassFileTransformer instance = createAgentInstance( className );
      agents.add( instance );
    }
    return agents;
  }

  private static ClassFileTransformer createAgentInstance( final String className ) throws MojoExecutionException {
    final Class<?> agentClass = resolveClass( className );
    if ( !ClassFileTransformer.class.isAssignableFrom( agentClass ) ) {
      final String message = className + "is not an instance of " + ClassFileTransformer.class;
      throw new MojoExecutionException( message );
    }
    return toClassFileTransformerInstance( agentClass );
  }

  private static ClassFileTransformer toClassFileTransformerInstance(
    final Class<?> agentClass ) throws MojoExecutionException {
    try {
      return ( ClassFileTransformer ) agentClass.getConstructor().newInstance();
    } catch ( final InstantiationException e ) {
      throw new MojoExecutionException( "Failed to instantiate class: " + agentClass + ". Does it have a no-arg constructor?", e );
    } catch ( final IllegalAccessException e ) {
      throw new MojoExecutionException( agentClass + ". Does not have a public no-arg constructor?", e );
    } catch ( NoSuchMethodException e ) {
      throw new MojoExecutionException( "Failed to instantiate class: " + agentClass + ". Does it have a no-arg constructor", e );
    } catch ( InvocationTargetException e ) {
      throw new MojoExecutionException( "Failed to instantiate class: " + agentClass + ". Could not invoke constructor", e );
    }
  }

  private static Class<?> resolveClass( @Nonnull final String className ) throws MojoExecutionException {
    try {
      return Class.forName( className );
    } catch ( final ClassNotFoundException e ) {
      final String message = MessageFormat.format( "Could not find class: {0}. Is it a registered dependency of the project or the plugin?", className );
      throw new MojoExecutionException( message, e );
    }
  }

  @Nonnull
  private ClassFileLocator createLocator() {
    return new ClassFileLocator( getLog(), getClass().getClassLoader() );
  }
}
