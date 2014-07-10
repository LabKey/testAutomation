/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.WebTestHelper.DEFAULT_TARGET_SERVER;
import static org.labkey.test.WebTestHelper.getTargetServer;

public class ArtifactCollector
{
    public static final String DEFAULT_TEST_NAME = "testSteps";
    private final BaseWebDriverTest _test;
    private static List<Pair<File, FileFilter>> pipelineDirs = new ArrayList<>();

    public ArtifactCollector(BaseWebDriverTest test)
    {
        _test = test;
    }

    public static void forgetArtifactDirs()
    {
        pipelineDirs = new ArrayList<>();
    }

    public File ensureDumpDir()
    {
        String currentTestClassName;
        try
        {
            currentTestClassName = BaseWebDriverTest.getCurrentTestClass().getSimpleName();
        }
        catch (ClassNotFoundException e)
        {
            currentTestClassName = "UnknownTest";
        }

        File dumpDir = new File(TestProperties.getDumpDir(), currentTestClassName);
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
            String labkeyRoot = WebTestHelper.getLabKeyRoot();
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

    public void dumpThreads(BaseWebDriverTest baseWebDriverTest)
    {
        if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
            return;

        try
        {
            File threadDumpRequest = new File(BaseWebDriverTest.getLabKeyRoot() + "/build/deploy", "threadDumpRequest");
            threadDumpRequest.setLastModified(System.currentTimeMillis()); // Touch file to trigger automatic thread dump.
        }
        catch (Exception e)
        {
            baseWebDriverTest.log("Error dumping threads: " + e.getMessage());
        }

        baseWebDriverTest.log("Threads dumped to standard labkey log file");
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

        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
        String baseName = dateFormat.format(new Date()) + _test.getClass().getSimpleName();
        if (!DEFAULT_TEST_NAME.equals(testName))
            baseName += "#" + testName;

        dumpFullScreen(dumpDir, baseName);
        dumpScreen(dumpDir, baseName);
        dumpHtml(dumpDir, baseName);
    }

    public void dumpHeap()
    {
        if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
            return;
        if ( _test.isGuestModeTest() )
            return;
        _test.pushLocation();
        try
        {
            // Use dumpHeapAction rather that touching file so that we can get file name and publish artifact.
            _test.beginAt("/admin/dumpHeap.view");
            File destDir = ensureDumpDir();
            String dumpMsg = Locator.css("#bodypanel > div").findElement(_test.getDriver()).getText();
            String filename = dumpMsg.substring(dumpMsg.indexOf("HeapDump_"));
            File heapDump = new File(BaseWebDriverTest.getLabKeyRoot() + "/build/deploy", filename);
            File destFile = new File(destDir, filename);

            if ( heapDump.renameTo(destFile) )
                publishArtifact(destFile);
            else
                _test.log("Unable to move HeapDump file to test logs directory.");
        }
        catch (Exception e)
        {
            _test.log("Error dumping heap: " + e.getMessage());
        }
        _test.popLocation(); // go back to get screenshot if needed.
    }

    public File dumpScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + ".png");
        try
        {
            File tempScreen = ((TakesScreenshot) _test.getDriver()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(tempScreen, screenFile);
            return screenFile;
        }
        catch (IOException ioe)
        {
            _test.log("Failed to copy screenshot file: " + ioe.getMessage());
        }

        return null;
    }

    public void dumpPageSnapshot(@Nullable String subdir)
    {
        dumpPageSnapshot(DEFAULT_TEST_NAME, subdir);
    }

    public void dumpPageSnapshot()
    {
        dumpPageSnapshot(null);
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
            catch (Exception e)
            {
                _test.log("Failed to take full screenshot: " + e.getMessage());
            }
        }

        return null;
    }

    public File dumpHtml(File dir, String baseName)
    {
        if (_test.getLastPageText() == null)
            return null;

        File htmlFile = new File(dir, baseName + ".html");
        try(FileWriter writer = new FileWriter(htmlFile))
        {
            writer.write(_test.getLastPageText());
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
        addArtifactLocation(path, new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return true;
            }
        });
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
                file.renameTo(new File(dest, file.getName()));
            }
        }
    }

    private ArrayList<File> listFilesRecursive(File path, final FileFilter filter)
    {
        FileFilter directoryOrArtifactFilter = new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.isDirectory() && !pathname.isHidden() ||
                       pathname.lastModified() > BaseWebDriverTest.getStartTime() && filter.accept(pathname);
            }
        };

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
