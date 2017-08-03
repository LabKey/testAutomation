/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.labkey.test.TestProperties.isHeapDumpCollectionEnabled;
import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.WebTestHelper.isLocalServer;

public class ArtifactCollector
{
    private final BaseWebDriverTest _test;
    private final WebDriverWrapper _driver;

    // Use CopyOnWriteArrayList to avoid ConcurrentModificationException
    private static List<Pair<File, FileFilter>> pipelineDirs = new CopyOnWriteArrayList<>();
    private static long _testStart;

    public ArtifactCollector(BaseWebDriverTest test)
    {
        this(test, test);
    }

    public ArtifactCollector(BaseWebDriverTest test, WebDriverWrapper driver)
    {
        _test = test;
        _driver = driver;
    }

    public static void init()
    {
        pipelineDirs = new ArrayList<>();
        _testStart = System.currentTimeMillis();
    }

    public File ensureDumpDir()
    {
        String currentTestClassName;
        try
        {
            currentTestClassName = BaseWebDriverTest.getCurrentTestClass().getSimpleName();
        }
        catch (NullPointerException e)
        {
            currentTestClassName = "UnknownTest";
        }
        return ensureDumpDir(currentTestClassName);
    }

    public File ensureDumpDir(String testClassName)
    {
        File dumpDir = new File(TestProperties.getDumpDir(), testClassName);
        if ( !dumpDir.exists() )
            dumpDir.mkdirs();

        return dumpDir;
    }

    // Publish artifacts while the build is still in progress:
    // http://www.jetbrains.net/confluence/display/TCD4/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-PublishingArtifactswhiletheBuildisStillinProgress
    public void publishArtifact(File file)
    {
        if (file != null && isTestRunningOnTeamCity())
        {
            // relativize path to labkey project root
            String labkeyRoot = TestFileUtils.getLabKeyRoot();
            labkeyRoot = new File(labkeyRoot).getAbsolutePath();
            String strFile = file.getAbsolutePath();
            if (strFile.toLowerCase().startsWith(labkeyRoot.toLowerCase()))
            {
                String path = strFile.substring(labkeyRoot.length());
                if (path.startsWith(File.separator))
                    path = path.substring(1);
                System.out.println("##teamcity[publishArtifacts '" + path + "']");
            }
        }
    }

    public void dumpThreads()
    {
        if (!isLocalServer())
            return;

        File threadDumpRequest = new File(TestFileUtils.getLabKeyRoot() + "/build/deploy", "threadDumpRequest");
        threadDumpRequest.setLastModified(System.currentTimeMillis()); // Touch file to trigger automatic thread dump.
        TestLogger.log("Threads dumped to standard labkey log file");
    }

    private String screenshotBaseName(@NotNull String suffix)
    {
        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
        String baseName = dateFormat.format(new Date()) + _test.getClass().getSimpleName();
        return baseName + "#" + suffix;
    }

    public void dumpPageSnapshot(String testName, @Nullable String subdir)
    {
        File dumpDir = ensureDumpDir();
        if (subdir != null && subdir.length() > 0)
        {
            dumpDir = new File(dumpDir, subdir);
            if ( !dumpDir.exists() )
                dumpDir.mkdirs();
        }

        String baseName = screenshotBaseName(testName);

        dumpFullScreen(dumpDir, baseName);
        dumpScreen(dumpDir, baseName);
        dumpHtml(dumpDir, baseName);
    }

    public void dumpHeap()
    {
        if (!isLocalServer())
            return;
        if ( _test.isGuestModeTest() )
            return;
        if (!isHeapDumpCollectionEnabled())
            return;

        _driver.pushLocation();

        // Use dumpHeapAction rather that touching file so that we can get file name and publish artifact.
        _driver.beginAt("/admin/dumpHeap.view");
        File destDir = ensureDumpDir();
        String dumpMsg = Locators.bodyPanel().childTag("div").findElement(_test.getDriver()).getText();
        String filename = dumpMsg.substring(dumpMsg.indexOf("HeapDump_"));
        File heapDump = new File(TestFileUtils.getLabKeyRoot() + "/build/deploy", filename);
        File destFile = new File(destDir, filename);

        if ( heapDump.renameTo(destFile) )
            publishArtifact(destFile);
        else
            TestLogger.log("Unable to move HeapDump file to test logs directory.");

        _driver.popLocation(); // go back to get screenshot if needed.
    }

    public File dumpScreen(@NotNull String suffix)
    {
        File dumpDir = ensureDumpDir();
        String baseName = screenshotBaseName(suffix);
        return dumpScreen(dumpDir, baseName);
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
        if (_test == _driver)
            pageHtml = _test.getLastPageText();
        else
            pageHtml = _driver.getHtmlSource();

        if (pageHtml == null)
            return null;

        String docType = (String)_driver.executeScript("" +
                "if (document.doctype == null)\n" +
                "  return 'txt';\n" +
                "else\n" +
                "  return document.doctype.name;");

        if (_driver.getDriver() instanceof ChromeDriver && docType.equals("txt"))
            docType = "html"; // Chrome wraps displayed text files in HTML

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
}
