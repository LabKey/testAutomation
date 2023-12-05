package org.labkey.test.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

public class CspLogUtil
{
    private static final String logName = "csp-report.log";
    private static final File logFile = new File(TestFileUtils.getServerLogDir(), logName);

    private static long lastSize = 0;
    private static long lastModified = 0;

    private CspLogUtil() { }

    public static void checkNewCspWarnings(ArtifactCollector artifactCollector)
    {
        if (TestProperties.isCspCheckSkipped())
            return;

        assertFalse("Cannot check CSP log on remote server", TestProperties.isServerRemote());
        assertThat(logFile).as("CSP log file").isFile();

        long logSize = logFile.length();
        long modified = logFile.lastModified();
        if (logSize > lastSize || modified > lastModified)
        {
            try
            {
                // Modified but got smaller? Log file probably rotated.
                Assert.assertTrue("CSP log file seems to have rotated. Check manually.", logSize > lastSize);
                List<String> warningLines;
                File recentWarningsFile = new File(artifactCollector.ensureDumpDir(), logName);

                try (FileInputStream fIn = new FileInputStream(logFile))
                {
                    //noinspection ResultOfMethodCallIgnored
                    fIn.skip(lastSize);
                    warningLines = IOUtils.readLines(fIn, Charset.defaultCharset());
                    TestFileUtils.writeFile(recentWarningsFile, StringUtils.join(warningLines.toArray(), System.lineSeparator()));
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Failed to read recent CSP violations.", e);
                }

                List<String> violations = new ArrayList<>();
                for (String line : warningLines)
                {
                    String[] split = line.split("ContentSecurityPolicy warning on page: ");
                    if (split.length > 1)
                    {
                        violations.add(split[1]);
                    }
                }

                throw new AssertionError("Detected CSP violations on the following pages (See log for more detail: %s):\n%s"
                        .formatted(recentWarningsFile.getAbsolutePath(), String.join("\n", violations)));
            }
            finally
            {
                lastSize = logSize;
                lastModified = modified;
            }
        }
    }
}
