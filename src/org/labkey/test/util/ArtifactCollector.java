/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.teamcity.TeamCityUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.PrintsPage;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.print.PrintOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.WebTestHelper.isLocalServer;

public class ArtifactCollector
{
    private static final Map<String, Integer> _shotCounters = new HashMap<>();

    private final WebDriverWrapper _driver;
    private final String _dumpDirName;

    // Use CopyOnWriteArrayList to avoid ConcurrentModificationException
    private static List<Pair<File, FileFilter>> pipelineDirs = new CopyOnWriteArrayList<>();
    private static long _testStart;

    public ArtifactCollector(WebDriverWrapper driver, String dumpDirName)
    {
        _driver = driver;
        _dumpDirName = dumpDirName;
    }

    public ArtifactCollector(WebDriverWrapper driver, ArtifactCollector otherBrowserCollector)
    {
        this(driver, otherBrowserCollector._dumpDirName);
    }

    public ArtifactCollector(BaseWebDriverTest test)
    {
        this(test, test.getClass().getSimpleName());
    }

    public static void init()
    {
        pipelineDirs = new ArrayList<>();
        _testStart = System.currentTimeMillis();
    }

    public File ensureDumpDir()
    {
        return ensureDumpDir(_dumpDirName);
    }

    public static File ensureDumpDir(String testClassName)
    {
        File dumpDir = new File(TestProperties.getDumpDir(), testClassName);
        if ( !dumpDir.exists() )
            dumpDir.mkdirs();

        return dumpDir;
    }

    private void publishArtifact(File file, @Nullable String destination)
    {
        if (file.isDirectory() && destination == null)
        {
            destination = file.getName() + ".tar.gz";
        }
        TeamCityUtils.publishArtifact(file, destination);
    }

    public void publishArtifact(File file)
    {
        publishArtifact(file, null);
    }

    public void publishDumpedArtifacts()
    {
        File dumpDir = ensureDumpDir();
        final String artifactName = dumpDir.getName() + ".tar.gz";
        publishArtifact(dumpDir, artifactName);
    }

    public void reportTestMetadata(String artifactBaseName)
    {
        File dumpDir = ensureDumpDir();
        final String baseArtifactPath = dumpDir.getName() + ".tar.gz!" + artifactBaseName;
        final String pngPath = baseArtifactPath + ".png";
        final String htmlPath = baseArtifactPath + ".html";

        // https://www.jetbrains.com/help/teamcity/reporting-test-metadata.html#Links+to+Build+Artifacts
        TeamCityUtils.serviceMessage("testMetadata", Map.of("type", "image", "value", pngPath));
        TeamCityUtils.serviceMessage("testMetadata", Map.of("type", "artifact", "value", htmlPath));
    }

    public static void dumpThreads()
    {
        if (!isLocalServer())
            return;

        File threadDumpRequest = new File(TestFileUtils.getLabKeyRoot() + "/build/deploy", "threadDumpRequest");
        threadDumpRequest.setLastModified(System.currentTimeMillis()); // Touch file to trigger automatic thread dump.
        TestLogger.log("Threads dumped to standard labkey log file");
    }

    private String buildBaseName(@NotNull String suffix)
    {
        return getAndIncrementShotCounter() + "_" + suffix;
    }

    private int getAndIncrementShotCounter()
    {
        Integer shotCounter = _shotCounters.getOrDefault(_dumpDirName, 0);
        _shotCounters.put(_dumpDirName, shotCounter + 1);
        return shotCounter;
    }

    public String dumpPageSnapshot(String snapshotName)
    {
        return dumpPageSnapshot(snapshotName, null);
    }

    public String dumpPageSnapshot(String snapshotName, @Nullable String subdir)
    {
        File dumpDir = ensureDumpDir();
        if (!StringUtils.isBlank(subdir))
        {
            dumpDir = new File(dumpDir, subdir);
            if ( !dumpDir.exists() )
                dumpDir.mkdirs();
        }

        String baseName = buildBaseName(snapshotName);

        dumpFullScreen(dumpDir, baseName);
        dumpScreen(dumpDir, baseName);
        dumpHtml(dumpDir, baseName);
        dumpPdf(dumpDir, baseName);

        return baseName;
    }

    public File dumpScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + ".png");
        try
        {
            File tempScreen = ((TakesScreenshot) _driver.getDriver()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(tempScreen, screenFile);
            return screenFile;
        }
        catch (IOException ioe)
        {
            TestLogger.log("Failed to copy screenshot file: " + ioe.getMessage());
        }

        return null;
    }

    public File dumpPdf(File dir, String baseName)
    {
        File pdfFile = new File(dir, baseName + ".pdf");
        try
        {
            Pdf pdf = ((PrintsPage) _driver.getDriver()).print(new PrintOptions());
            byte[] pdfData = Base64.getDecoder().decode(pdf.getContent());
            try (OutputStream stream = new FileOutputStream(pdfFile)) {
                stream.write(pdfData);
            }
            return pdfFile;
        }
        catch (Exception ioe)
        {
            TestLogger.log("Failed dump page pdf: " + ioe.getMessage());
        }

        return null;
    }

    public File dumpFullScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + "Fullscreen.png");

        // Windows doesn't support OS level screenshots for headless environment
        if (!isTestRunningOnTeamCity() || !System.getProperty("os.name").toLowerCase().contains("win"))
        {
            try
            {
                // capture entire screen
                BufferedImage fullscreen = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                ImageIO.write(fullscreen, "png", screenFile);

                return screenFile;
            }
            catch (AWTException | IOException e)
            {
                TestLogger.log("Failed to take full screenshot: " + e.getMessage());
            }
        }

        return null;
    }

    public File dumpHtml(File dir, String baseName)
    {
        String pageHtml;
        if (_driver instanceof BaseWebDriverTest test)
            pageHtml = test.getLastPageText();
        else
            pageHtml = _driver.getHtmlSource();

        if (pageHtml == null)
            return null;

        String docType = (String)_driver.executeScript("" +
                "if (document.doctype == null)\n" +
                "  return 'html';\n" + // Chrome and Firefox wrap text files in html
                "else\n" +
                "  return document.doctype.name;");

        File htmlFile = new File(dir, baseName + "." + docType);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_16)))
        {
            writer.write(pageHtml);
            return htmlFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void addArtifactLocation(File path, FileFilter fileFilter)
    {
        pipelineDirs.add(new ImmutablePair<>(path, fileFilter));
    }

    public void addArtifactLocation(File path)
    {
        addArtifactLocation(path, pathname -> true);
    }

    public void dumpPipelineFiles()
    {
        for (Pair<File, FileFilter> artifactLocation : pipelineDirs)
        {
            dumpPipelineFiles(artifactLocation);
        }
    }

    private void dumpPipelineFiles(Pair<File, FileFilter> artifactLocation)
    {
        File dumpDir = ensureDumpDir();
        File artifactDir = artifactLocation.getLeft();
        FileFilter artifactFilter = artifactLocation.getRight();

        // moves all files created by the test under @artifactDir to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(artifactDir, artifactFilter);
        for (File file : files)
        {
            if ( file.isFile() )
            {
                File dest = new File(dumpDir, file.getParent().substring(artifactDir.toString().length()));
                if (!dest.exists())
                    dest.mkdirs();
                try
                {
                    FileUtils.copyFile(file, new File(dest, file.getName()));
                }
                catch (IOException log)
                {
                    TestLogger.log("Failed to collect test artifact: " + file.toString().substring(artifactDir.toString().length()));
                }
            }
        }
    }

    private ArrayList<File> listFilesRecursive(File path, final FileFilter filter)
    {
        FileFilter directoryOrArtifactFilter = pathname ->
                pathname.isDirectory() && !pathname.isHidden() ||
               pathname.lastModified() > _testStart && filter.accept(pathname);

        File[] files = path.listFiles(directoryOrArtifactFilter);
        ArrayList<File> allFiles = new ArrayList<>();
        if (files != null)
        {
            for (File file : files)
            {
                if ( file.isDirectory() )
                    allFiles.addAll(listFilesRecursive(file, filter));
                else // file.isFile()
                    allFiles.add(file);
            }
        }
        return allFiles;
    }

    public File exportDiagnostics()
    {
        _driver.beginAt(WebTestHelper.buildURL("diagnostics", "exportDiagnostics"));
        File diagnostics = _driver.clickAndWaitForDownload(Locator.id("downloadBtn"));
        TestLogger.log("Server diagnostics exported to: " + diagnostics.getAbsolutePath());
        return diagnostics;
    }
}
