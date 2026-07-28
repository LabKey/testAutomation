/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CspLogUtil
{
    private static final List<String> ignoredViolations = List.of(
        "/_rstudio/",
        "/_rstudioReport/"
    );
    private static final Set<String> ignoredDirectives = Collections.emptySet();

    private static final String logName = "csp-report.log";
    private static final File logFile = new File(TestFileUtils.getServerLogDir(), logName);

    private static long lastSize = 0;
    private static long lastModified = 0;
    private static boolean missingLog = false;

    private CspLogUtil() { }

    public static void init()
    {
        if (lastModified == 0)
        {
            try
            {
                BasicFileAttributes logFileAttributes;
                logFileAttributes = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);

                lastSize = logFileAttributes.size();
                lastModified = logFileAttributes.lastModifiedTime().toMillis();
            }
            catch (IOException e)
            {
                lastModified = System.currentTimeMillis();
            }
        }
    }

    public static void checkNewCspWarnings(ArtifactCollector artifactCollector)
    {
        if (TestProperties.isServerRemote() || missingLog)
            return;

        BasicFileAttributes logFileAttributes;
        try
        {
            logFileAttributes = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
            if (!logFileAttributes.isRegularFile())
            {
                throw new IOException(logFile.getAbsolutePath() + " is not a file");
            }
        }
        catch (IOException e)
        {
            missingLog = true; // Only fail one test if CSP check is enabled but log is missing.
            if (TestProperties.isCspCheckSkipped())
            {
                TestLogger.warn(e.getMessage());
                return;
            }
            throw new RuntimeException("Unable to read CSP log", e);
        }

        long logSize = logFileAttributes.size();
        long modified = logFileAttributes.lastModifiedTime().toMillis();
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

                boolean foundVioloation = false;
                MultiValuedMap<Crawler.ControllerActionId, String> violations = new HashSetValuedHashMap<>();
                List<CspReport> cspReports = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (String line : warningLines)
                {
                    if (!sb.isEmpty() || line.equals("{"))
                    {
                        sb.append(line);
                    }
                    if (line.equals("}"))
                    {
                        cspReports.add(new CspReport(sb.toString()));
                        sb = new StringBuilder();
                        foundVioloation = true;
                    }
                }

                if (!foundVioloation)
                {
                    throw new AssertionError("Detected CSP violations but unable to parse log file: " + recentWarningsFile.getAbsolutePath());
                }

                for (CspReport cspReport : cspReports)
                {
                    String url = cspReport.getDocumentUrl();
                    String violatedDirective = cspReport.getViolatedDirective();
                    if (ignoredViolations.stream().anyMatch(url::contains) || ignoredDirectives.contains(violatedDirective))
                    {
                        TestLogger.warn("Ignoring %s CSP warning on page: %s".formatted(violatedDirective, url));
                    }
                    else
                    {
                        Crawler.ControllerActionId actionId = new Crawler.ControllerActionId(url);
                        violations.put(actionId, cspReport.toString());
                    }
                }

                if (!violations.isEmpty())
                {
                    StringBuilder errorMessage = new StringBuilder()
                            .append("Detected CSP violations on the following actions (See log for more detail: ")
                            .append(recentWarningsFile.getAbsolutePath())
                            .append("):");
                    for (Crawler.ControllerActionId actionId : violations.keySet())
                    {
                        errorMessage.append("\n\t");
                        Collection<String> urls = violations.get(actionId);
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
                    if (TestProperties.isCspCheckSkipped())
                    {
                        TestLogger.warn(errorMessage.toString());
                    }
                    else
                    {
                        throw new CspWarningDetectedException(errorMessage);
                    }
                }
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
        if (TestProperties.isServerRemote() || !logFile.isFile())
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

class CspReport
{
    private final String _violatedDirective;
    private final String _documentUrl;

    CspReport(String reportStr)
    {
        // Support report-to reports only. GitHub Issue #900
        JSONObject report = new JSONObject(reportStr).getJSONObject("body");
        _violatedDirective = report.getString("effectiveDirective");
        _documentUrl = report.getString("documentURL");
    }

    public String getViolatedDirective()
    {
        return _violatedDirective;
    }

    public String getDocumentUrl()
    {
        return _documentUrl;
    }

    @Override
    public String toString()
    {
        return getViolatedDirective() + ": " + getDocumentUrl();
    }
}
