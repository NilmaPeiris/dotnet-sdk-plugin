package io.jenkins.plugins.dotnet.console;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StatusScanner extends LineTransformationOutputStream {

  private final OutputStream out;
  private final Charset charset;

  public StatusScanner(@NonNull OutputStream out, @NonNull Charset charset) {
    this.out = out;
    this.charset = charset;
  }

  @Override
  public void close() throws IOException {
    super.close();
    out.close();
    System.err.printf("SCANNER CLOSE -> %d warnings, %d errors%n", this.warnings, this.errors);
  }

  // FIXME: This will fail for non-English environments.
  // FIXME: A better approach might be to create a custom MSBuild logger which would emit special marker lines, which we then
  // FIXME: replace by the appropriate note.
  // FIXME: That could even allow marking all targets being executed, adding an overview at the end, including links to their
  // FIXME: output (would only happen at an appropriate verbosity level).

  // FIXME: In the shorter term, it may be enough to have a few well-known alternatives for 'error' and 'warning', but assumptions
  // FIXME: about message format and word order will still cause problems.

  /** Regular expression pattern for the MSBuild error count line. */
  private static final Pattern RE_ERROR_COUNT = Pattern.compile("^ *(\\d+) Error\\(s\\)$");

  /** Regular expression pattern for the MSBuild warning count line. */
  private static final Pattern RE_WARNING_COUNT = Pattern.compile("^ *(\\d+) Warning\\(s\\)$");

  @Override
  protected void eol(byte[] lineBytes, int lineLength) throws IOException {
    final String line = trimEOL(charset.decode(ByteBuffer.wrap(lineBytes, 0, lineLength)).toString());
    {
      Matcher m = RE_ERROR_COUNT.matcher(line);
      if (m.matches())
        this.errors = Integer.parseInt(m.group(1));
      else {
        m = RE_WARNING_COUNT.matcher(line);
        if (m.matches())
          this.warnings = Integer.parseInt(m.group(1));
      }
    }
    out.write(lineBytes, 0, lineLength);
  }

  private int errors = 0;

  public int getErrors() {
    return this.errors;
  }

  private int warnings = 0;

  public int getWarnings() {
    return this.warnings;
  }

}