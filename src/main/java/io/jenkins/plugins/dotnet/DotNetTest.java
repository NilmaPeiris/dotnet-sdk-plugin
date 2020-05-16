package io.jenkins.plugins.dotnet;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractProject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/** A build step using the 'dotnet' executable to run unit tests for a project, using its configured runner. */
public final class DotNetTest extends DotNet {

  // TODO: Determine configuration needed.

  @DataBoundConstructor
  public DotNetTest(String sdk) {
    super(sdk, "test");
  }

  @Extension
  @Symbol("dotnetTest")
  public static class DescriptorImpl extends DotNet.DescriptorImpl {

    public DescriptorImpl() {
      load();
    }

    protected DescriptorImpl(Class<? extends DotNetTest> clazz) {
      super(clazz);
    }

    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @NonNull
    public String getDisplayName() {
      return Messages.DotNetTest_DisplayName();
    }

  }

}
