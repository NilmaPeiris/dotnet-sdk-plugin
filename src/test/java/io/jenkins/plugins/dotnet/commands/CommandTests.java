package io.jenkins.plugins.dotnet.commands;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.FreeStyleProject;
import hudson.model.Slave;
import hudson.model.TaskListener;
import io.jenkins.plugins.dotnet.DotNetSDK;
import org.junit.Assert;
import org.junit.Rule;
import org.jvnet.hudson.test.FakeLauncher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class CommandTests {

  @Rule
  public final JenkinsRule rule = new JenkinsRule();

  protected static final Logger LOGGER = Logger.getLogger(CommandTests.class.getName());

  //region Command Line Checking (Expected Commands)

  protected static final class CommandLineChecker extends Launcher.DummyLauncher implements FakeLauncher {

    public CommandLineChecker(@NonNull TaskListener listener) {
      super(listener);
    }

    private final Queue<ExpectedCommand> expectedCommands = new LinkedList<>();

    public static final class ExpectedCommand {

      private final List<String> commandLine = new ArrayList<>();

      private final List<Boolean> masks = new ArrayList<>();

      public ExpectedCommand(@NonNull String executable) {
        this.commandLine.add(executable);
        this.masks.add(false);
      }

      public ExpectedCommand withArgument(@NonNull String argument) {
        return this.withArgument(argument, false);
      }

      public ExpectedCommand withArgument(@NonNull String argument, boolean masked) {
        this.commandLine.add(argument);
        this.masks.add(masked);
        return this;
      }

      public ExpectedCommand withArguments(@NonNull String... arguments) {
        if (arguments.length != 0) {
          for (final String argument : arguments)
            this.withArgument(argument);
        }
        return this;
      }

      public void verify(@NonNull ProcStarter actualCommand) {
        final List<String> actualCommandLine = actualCommand.cmds();
        final boolean[] actualMasks = actualCommand.masks();
        final int expectedSize = this.commandLine.size();
        final int actualSize = actualCommandLine.size();
        CommandTests.LOGGER.info(String.format("Expected Command: %d element(s): '%s'", expectedSize, String.join("' '", this.commandLine)));
        CommandTests.LOGGER.info(String.format("Actual   Command: %d element(s): '%s'", actualSize, String.join("' '", actualCommandLine)));
        Assert.assertEquals("Wrong command line being executed: incorrect number of elements.", expectedSize, actualSize);
        for (int i = 0; i < expectedSize; ++i) {
          final String expectedElement = this.commandLine.get(i);
          final String actualElement = actualCommandLine.get(i);
          Assert.assertEquals(String.format("Wrong command line being executed: element #%d has wrong content.", i), expectedElement, actualElement);
          final boolean expectedMask = this.masks.get(i) != null && this.masks.get(i);
          final boolean actualMask = actualMasks != null && actualMasks[i];
          Assert.assertFalse(String.format("Wrong command line being executed: element #%d is not masked.", i), !actualMask && expectedMask);
          Assert.assertFalse(String.format("Wrong command line being executed: element #%d is unexpectedly masked.", i), actualMask && !expectedMask);
        }

      }

    }

    public ExpectedCommand expectCommand() {
      return this.expectCommand(DotNetSDK.getExecutableFileName(this));
    }

    public ExpectedCommand expectCommand(@NonNull String executable) {
      final ExpectedCommand expectedCommand = new ExpectedCommand(executable);
      this.expectedCommands.add(expectedCommand);
      return expectedCommand;
    }

    public void finish() {
      if (this.expectedCommands.isEmpty())
        return;
      while (!this.expectedCommands.isEmpty()) {
        final ExpectedCommand expected = this.expectedCommands.remove();
        final int expectedSize = expected.commandLine.size();
        final String expectedCommand = String.join("' '", expected.commandLine);
        CommandTests.LOGGER.info(String.format("Expected Command: %d element(s): '%s'", expectedSize, expectedCommand));
      }
      Assert.fail("Expected command(s) not executed.");
    }

    @Override
    public Proc onLaunch(ProcStarter p) throws IOException {
      final ExpectedCommand expectedCommand = this.expectedCommands.poll();
      Assert.assertNotNull("Unexpected command execution.", expectedCommand);
      expectedCommand.verify(p);
      return new FinishedProc(0);
    }

  }

  //endregion

  protected <T extends Command> void runCommandAndValidateProcessExecution(Supplier<T> createCommand, Consumer<CommandLineChecker> manageExpectations) throws Exception {
    final FreeStyleProject project = this.rule.createFreeStyleProject();
    { // Create the command and add it as a project build step
      final T step = createCommand.get();
      project.getBuildersList().add(step);
    }
    // Set u
    final CommandLineChecker launcher = new CommandLineChecker(this.rule.createTaskListener());
    manageExpectations.accept(launcher);
    { // Set up a fake node with a command-line checker as fake launcher
      final Slave slave = this.rule.createPretendSlave(launcher);
      this.rule.jenkins.addNode(slave);
      project.setAssignedNode(slave);
    }
    this.rule.buildAndAssertSuccess(project);
    launcher.finish();
  }

  @FunctionalInterface
  protected interface Code {

    void execute() throws Exception;

  }

  protected void withCredentials(@NonNull StandardCredentials credentials, @NonNull Code code) throws IOException {
    // FIXME: Is this really the right way to do this?
    final CredentialsProvider provider = ExtensionList.lookup(CredentialsProvider.class).get(SystemCredentialsProvider.ProviderImpl.class);
    final CredentialsStore store = provider.getStore(this.rule.jenkins);
    final Domain domain = Domain.global();
    store.addCredentials(domain, credentials);
    try {
      code.execute();
    }
    catch (Throwable t) {
      store.removeCredentials(domain, credentials);
    }
  }

}
