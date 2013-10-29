package com.cedarsoft.maven.instrumentation.plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

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

  @Nonnull
  @Override
  protected String getGoal() {
    return "instrument";
  }
}
