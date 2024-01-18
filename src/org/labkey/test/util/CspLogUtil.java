package org.labkey.test.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.labkey.serverapi.writer.PrintWriters;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CspLogUtil
{
    private static final String logName = "csp-report.log";
    private static final File logFile = new File(TestFileUtils.getServerLogDir(), logName);

    private static long lastSize = 0;
    private static long lastModified = 0;
    private static boolean missingLog = false;

    private CspLogUtil() { }

    public static void checkNewCspWarnings(ArtifactCollector artifactCollector)
    {
        if (TestProperties.isCspCheckSkipped() || TestProperties.isServerRemote() || missingLog)
            return;

        if (!logFile.isFile())
        {
            missingLog = true; // Only fail one test if CSP check is enabled but log is missing.
            assertThat(logFile).as("CSP log file").isFile(); // Should fail
        }

        long logSize = logFile.length();
        long modified = logFile.lastModified();
        if (logSize > 0 && (logSize > lastSize || modified > lastModified))
        {
            try
            {
                // Modified but got smaller? Log file probably rotated.
                Assert.assertTrue("CSP log file seems to have rotated. Check manually.", logSize > lastSize);
                List<String> warningLines;
                File recentWarningsFile = new File(artifactCollector.ensureDumpDir(), logName);

                try (FileInputStream fIn = new FileInputStream(logFile);
                     Writer writer = PrintWriters.getPrintWriter(new FileOutputStream(recentWarningsFile, true)))
                {
                    //noinspection ResultOfMethodCallIgnored
                    fIn.skip(lastSize);
                    warningLines = IOUtils.readLines(fIn, Charset.defaultCharset());
                    IOUtils.writeLines(warningLines, System.lineSeparator(), writer);
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Failed to read recent CSP violations.", e);
                }

                MultiValuedMap<Crawler.ControllerActionId, String> violoations = new HashSetValuedHashMap<>();
                for (String line : warningLines)
                {
                    String[] split = line.split("ContentSecurityPolicy warning on page: ");
                    if (split.length > 1)
                    {
                        String url = split[1];
                        Crawler.ControllerActionId actionId = new Crawler.ControllerActionId(url);
                        violoations.put(actionId, url);
                    }
                }

                if (violoations.isEmpty())
                {
                    throw new AssertionError("Detected CSP violations but unable to parse log file: " + recentWarningsFile.getAbsolutePath());
                }

                StringBuilder errorMessage = new StringBuilder()
                        .append("Detected CSP violations on the following actions (See log for more detail: ")
                        .append(recentWarningsFile.getAbsolutePath())
                        .append("):");
                for (Crawler.ControllerActionId actionId : violoations.keySet())
                {
                    errorMessage.append("\n\t");
                    Collection<String> urls = violoations.get(actionId);
                    errorMessage.append(actionId);
                    if (urls.size() > 1)
                    {
                        errorMessage.append("\n\t\t");
                        errorMessage.append(String.join("\n\t\t", urls));
                    }
                    else
                    {
                        errorMessage.append(": ").append(urls.iterator().next());
                    }
                }
                throw new CspWarningDetectedException(errorMessage);
            }
            finally
            {
                lastSize = logSize;
                lastModified = modified;
            }
        }
    }

    public static void resetCspLogMark()
    {
        if (TestProperties.isCspCheckSkipped() || TestProperties.isServerRemote() || !logFile.isFile())
            return;

        lastSize = logFile.length();
        lastModified = logFile.lastModified();
    }

    public static class CspWarningDetectedException extends AssertionError
    {
        public CspWarningDetectedException(Object detailMessage)
        {
            super(detailMessage);
        }
    }
}
