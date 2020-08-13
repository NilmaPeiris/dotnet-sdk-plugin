package io.jenkins.plugins.dotnet.commands.nuget;

import io.jenkins.plugins.dotnet.commands.CommandTests;
import org.junit.Test;

public class DeleteTests extends CommandTests {

  @Test
  public void simpleExecutionWorks() throws Exception {
    super.runCommandAndValidateProcessExecution(Delete::new, check -> check.expectCommand().withArguments("nuget", "delete"));
  }

}
