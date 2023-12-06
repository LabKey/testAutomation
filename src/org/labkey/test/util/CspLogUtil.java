package org.labkey.test.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
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
        if (logSize > 0 && (logSize > lastSize || modified > lastModified))
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
                throw new AssertionError(errorMessage);
            }
            finally
            {
                lastSize = logSize;
                lastModified = modified;
            }
        }
    }
}
