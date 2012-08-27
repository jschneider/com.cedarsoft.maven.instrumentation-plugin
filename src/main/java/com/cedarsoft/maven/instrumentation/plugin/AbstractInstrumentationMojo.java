package com.cedarsoft.maven.instrumentation.plugin;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.cedarsoft.maven.instrumentation.plugin.util.ClassFile;
import com.cedarsoft.maven.instrumentation.plugin.util.ClassFileLocator;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public abstract class AbstractInstrumentationMojo extends AbstractMojo {
  /**
   * The fully qualified class names of the transformers to apply.
   *
   * @parameter
   * @required
   */
  protected List<String> classTransformers;
  /**
   * The maven session
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject mavenProject;

  private static void performClassTransformation(@Nonnull final Iterable<? extends ClassFile> classFiles, @Nonnull final Iterable<? extends ClassFileTransformer> agents) throws MojoExecutionException {
    for (final ClassFile classFile : classFiles) {
      for (final ClassFileTransformer agent : agents) {
        transformClass(classFile, agent);
      }
    }
  }

  private static void transformClass(@Nonnull final ClassFile classFile, @Nonnull final ClassFileTransformer agent) throws MojoExecutionException {
    try {
      classFile.transform(agent);
    } catch (final ClassTransformationException e) {
      final String message = MessageFormat.format("Failed to transform class: {0}, using ClassFileTransformer, {1}", classFile, agent.getClass());
      throw new MojoExecutionException(message, e);
    }
  }

  private static ClassFileTransformer createAgentInstance(final String className) throws MojoExecutionException {
    final Class<?> agentClass = resolveClass(className);
    if (!ClassFileTransformer.class.isAssignableFrom(agentClass)) {
      final String message = className + "is not an instance of " + ClassFileTransformer.class;
      throw new MojoExecutionException(message);
    }
    return toClassFileTransformerInstance(agentClass);
  }

  private static ClassFileTransformer toClassFileTransformerInstance(
    final Class<?> agentClass) throws MojoExecutionException {
    try {
      return (ClassFileTransformer) agentClass.getConstructor().newInstance();
    } catch (final InstantiationException e) {
      throw new MojoExecutionException("Failed to instantiate class: " + agentClass + ". Does it have a no-arg constructor?", e);
    } catch (final IllegalAccessException e) {
      throw new MojoExecutionException(agentClass + ". Does not have a public no-arg constructor?", e);
    } catch (NoSuchMethodException e) {
      throw new MojoExecutionException("Failed to instantiate class: " + agentClass + ". Does it have a no-arg constructor", e);
    } catch (InvocationTargetException e) {
      throw new MojoExecutionException("Failed to instantiate class: " + agentClass + ". Could not invoke constructor", e);
    }
  }

  private static Class<?> resolveClass(@Nonnull final String className) throws MojoExecutionException {
    try {
      return Class.forName(className);
    } catch (final ClassNotFoundException e) {
      final String message = MessageFormat.format("Could not find class: {0}. Is it a registered dependency of the project or the plugin?", className);
      throw new MojoExecutionException(message, e);
    }
  }

  protected MavenProject getProject() {
    return mavenProject;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (getProject().getPackaging().equals("pom")) {
      return;
    }

    getLog().info("Starting InstrumentationMojo - instrumenting <" + getOutputDirectory() + ">");

    File outputDirectory = getOutputDirectory();
    if (!outputDirectory.isDirectory()) {
      getLog().info("Canceling since " + outputDirectory + " does not exist");
      return;
    }

    final Collection<ClassFileTransformer> agents = getAgents();
    final Collection<? extends ClassFile> classFiles = createLocator().findClasses(outputDirectory);

    performClassTransformation(classFiles, agents);
  }


  @Nonnull
  protected abstract File getOutputDirectory();

  @Nonnull
  private ClassLoader createClassLoader() throws MojoExecutionException {
    List<URL> urls = new ArrayList<URL>();
    for (String classpathElement : getClasspathElements()) {
      File file = new File(classpathElement);
      if (file.equals(getOutputDirectory())) {
        continue;
      }

      try {
        urls.add(file.toURI().toURL());
      } catch (MalformedURLException e) {
        throw new MojoExecutionException("Could not convert <" + classpathElement + "> to url", e);
      }
    }

    return new URLClassLoader(urls.toArray(new URL[urls.size()]));
  }

  @Nonnull
  protected abstract Iterable<? extends String> getClasspathElements();

  private Collection<ClassFileTransformer> getAgents() throws MojoExecutionException {
    final Collection<ClassFileTransformer> agents = new ArrayList<ClassFileTransformer>();
    for (final String className : classTransformers) {
      final ClassFileTransformer instance = createAgentInstance(className);
      agents.add(instance);
    }
    return agents;
  }

  @Nonnull
  private ClassFileLocator createLocator() throws MojoExecutionException {
    return new ClassFileLocator(getLog(), createClassLoader());
  }
}
