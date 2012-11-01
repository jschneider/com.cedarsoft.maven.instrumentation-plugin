package com.cedarsoft.maven.instrumentation.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.cedarsoft.maven.instrumentation.plugin.util.ClassFile;
import com.cedarsoft.maven.instrumentation.plugin.util.ClassFileLocator;
import com.google.common.base.Joiner;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 * @goal instrument
 * @phase process-classes
 * @requiresDependencyResolution compile+runtime
 */
public class InstrumentationMojo extends AbstractInstrumentationMojo {
  /**
   * @parameter expression="${project.build.outputDirectory}"
   * @read-only
   * @required
   */
  protected File outputDirectory;

  @Override
  @Nonnull
  protected File getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * Project classpath.
   *
   * @parameter default-value="${project.compileClasspathElements}"
   * @required
   * @readonly
   */
  protected List<String> classpathElements;

  @Nonnull
  @Override
  protected Iterable<? extends String> getClasspathElements() {
    return Collections.unmodifiableList( classpathElements );
  }
}
