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
package org.labkey.test;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.util.FileUtil;
import org.labkey.api.writer.PrintWriters;
import org.labkey.test.util.TestLogger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertNotNull;

/**
 * Static methods for finding finding and reading test-related files
 */
public abstract class TestFileUtils
{
    private static File _labkeyRoot = null;
    private static File _buildDir = null;
    public static final File DEFAULT_SAMPLEDATA_DIR = new File(getLabKeyRoot(), "sampledata");

    public static String getFileContents(String rootRelativePath)
    {
        return getFileContents(Paths.get(getLabKeyRoot(), rootRelativePath));
    }

    public static String getFileContents(final File file)
    {
        Path path = Paths.get(file.toURI());

        return getFileContents(path);
    }

    public static String getFileContents(Path path)
    {
        try
        {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }
    }

    public static String getStreamContentsAsString(InputStream is) throws IOException
    {
        return StringUtils.join(IOUtils.readLines(is).toArray(), System.lineSeparator());
    }

    public static String getLabKeyRoot()
    {
        if (_labkeyRoot == null)
        {
            final String labkeyRootProperty = System.getProperty("labkey.root");

            if (labkeyRootProperty != null)
            {
                _labkeyRoot = new File(labkeyRootProperty);

                if (!_labkeyRoot.exists())
                {
                    throw new IllegalStateException("Specified LabKey root does not exist [" + _labkeyRoot + "]. Configure this by passing VM arg labkey.root={yourroot}");
                }
                if (!new File(_labkeyRoot, "server/test").exists())
                {
                    throw new IllegalStateException("Specified LabKey root exists [" + _labkeyRoot + "] but isn't the root of a LabKey enlistment. Configure this by passing VM arg labkey.root={yourroot}");
                }

                _labkeyRoot = FileUtil.getAbsoluteCaseSensitiveFile(_labkeyRoot);

                TestLogger.log("Using labkey root '" + _labkeyRoot + "', as provided by system property 'labkey.root'.");
            }
            else
            {
                _labkeyRoot = FileUtil.getAbsoluteCaseSensitiveFile(new File(""));
                if (_labkeyRoot.getName().equals("test") && _labkeyRoot.getParentFile().getName().equals("server"))
                    _labkeyRoot = _labkeyRoot.getParentFile().getParentFile(); // Working directory is in '{labkey.root}/server'; otherwise is in enlistment root
                else if (_labkeyRoot.getName().equals("server"))
                    _labkeyRoot = _labkeyRoot.getParentFile(); // Working directory is in '{labkey.root}/server'; otherwise is in enlistment root
                else if (!new File(_labkeyRoot, "server").exists())
                {
                    throw new IllegalStateException("Unable to locate enlistment. Working directory [" + _labkeyRoot + "] isn't a recognized location. Configure manually with passing VM arg labkey.root={yourroot}");
                }
            }
        }
        return _labkeyRoot.toString();
    }

    public static File getTestBuildDir()
    {
        if (_buildDir == null)
        {
            _buildDir = new File(getLabKeyRoot(), "build/modules/test"); // Gradle
            if (!_buildDir.exists())
            {
                _buildDir = new File(getLabKeyRoot(), "server/test/build"); // Ant
            }
        }
        return _buildDir;
    }

    public static File getDefaultDeployDir()
    {
        return new File(getLabKeyRoot(), "build/deploy");
    }

    public static File getDefaultFileRoot(String containerPath)
    {
        return new File(getLabKeyRoot(), "build/deploy/files/" + containerPath + "/@files");
    }

    public static String getDefaultWebAppRoot()
    {
        File path = new File(getLabKeyRoot(), "build/deploy/labkeyWebapp");
        return path.toString();
    }

    /**
     * Searches all sampledata directories for the specified file
     * @param relativePath e.g. "lists/ListDemo.lists.zip" or "OConnor_Test.folder.zip"
     * @return File object with the full path to the specified file
     */
    @NotNull
    public static File getSampleData(String relativePath)
    {
        Set<String> sampledataDirs = new TreeSet<>();

        File sampledataDirsFile = new File(getTestBuildDir(), "sampledata.dirs");
        if (sampledataDirsFile.exists())
        {
            String path = getFileContents(sampledataDirsFile);
            sampledataDirs.addAll(Arrays.asList(path.split(";")));
        }
        else
        {
            sampledataDirs.add(DEFAULT_SAMPLEDATA_DIR.toString());
        }

        File foundFile = null;
        for (String sampledataDir : sampledataDirs)
        {
            File checkFile = new File(sampledataDir, relativePath);
            if (checkFile.exists())
            {
                if (foundFile != null && !foundFile.equals(checkFile))
                    throw new IllegalArgumentException("Ambiguous file specified: " + relativePath + "\n" +
                            "Found:\n" +
                            foundFile + "\n" +
                            checkFile);
                else
                    foundFile = checkFile;
            }
        }

        assertNotNull("Sample data not found: " + relativePath + "\n" +
                "Run `./gradlew :server:test:build :server:test:writeSampleDataFile` once to locate all sampledata" + "\n" +
                "Currently known sample data locations: " + String.join("\n", sampledataDirs), foundFile);
        return foundFile;
    }

    public static File getApiScriptFolder()
    {
        return new File(getLabKeyRoot(), "server/test/data/api");
    }

    public static String getProcessOutput(File executable, String... args) throws IOException
    {
        executable = FileUtil.getAbsoluteCaseSensitiveFile(executable);

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(ArrayUtils.addAll(new String[]{executable.getAbsolutePath()}, args));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Different platforms output version info differently; just combine all std/err output
        return StringUtils.trim(getStreamContentsAsString(p.getInputStream()));
    }

    public static File getTestTempDir()
    {
        File buildDir = new File(getLabKeyRoot(), "build");
        return new File(buildDir, "testTemp");
    }

    public static void delete(File file)
    {
        TestLogger.log("Deleting from filesystem: " + file.toString());
        checkFileLocation(file);

        if (!file.exists())
            return;

        FileUtils.deleteQuietly(file);

        if (!file.exists())
            TestLogger.log("Deletion successful.");
        else
            TestLogger.log("Failed to delete : " + file.getAbsolutePath());
    }

    public static void deleteDir(File dir)
    {
        TestLogger.log("Deleting from filesystem: " + dir.toString());
        checkFileLocation(dir);
        if (!dir.exists())
            return;

        try
        {
            FileUtils.deleteDirectory(dir);
            TestLogger.log("Deletion successful.");
        }
        catch (IOException e)
        {
            TestLogger.log("WARNING: Exception deleting directory -- " + e.getMessage());
        }
    }

    private static void checkFileLocation(File file)
    {
        try
        {
            if (!FileUtils.directoryContains(new File(getLabKeyRoot()), file))
            {
                // TODO: Consider throwing IllegalArgumentException
                TestLogger.log("DEBUG: Attempting to delete a file outside of test enlistment");
            }
        }
        catch (IOException ignore) { }
    }

    public static File saveFile(File dir, String fileName, String contents)
    {
        File tsvFile = new File(dir, fileName);

        try (Writer writer = PrintWriters.getPrintWriter(tsvFile))
        {
            writer.write(contents);
            return tsvFile;
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static boolean isFileInZipArchive(File zipArchive, String fileName) throws IOException
    {
        List<String> files = getFilesInZipArchive(zipArchive);
        return files.stream().anyMatch((f)-> f.endsWith(fileName));
    }

    public static List<String> getFilesInZipArchive(File zipArchive) throws IOException
    {
        final ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipArchive)));

        ZipEntry entry;
        List<String> files = new ArrayList<>();
        while ((entry = zipInputStream.getNextEntry()) != null)
        {
            files.add(entry.getName());
        }
        return files;
    }

    /** Untar an input file into an output file.
     * The output file is created in the output folder, having the same name
     * as the input file, minus the '.tar' extension.
     */
    private static List<File> unTar(final File inputFile, final File outputDir) throws IOException, ArchiveException
    {
        final List<File> untaredFiles = new ArrayList<>();
        try (InputStream is = new FileInputStream(inputFile);
             TarArchiveInputStream inputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is))
        {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) inputStream.getNextEntry()) != null)
            {
                final File outputFile = new File(outputDir, entry.getName());
                if (entry.isDirectory())
                {
                    if (!outputFile.exists())
                    {
                        if (!outputFile.mkdirs())
                        {
                            throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                        }
                    }
                }
                else
                {
                    try (OutputStream outputFileStream = new FileOutputStream(outputFile))
                    {
                        org.apache.commons.compress.utils.IOUtils.copy(inputStream, outputFileStream);
                    }
                }
                untaredFiles.add(outputFile);
            }
        }

        return untaredFiles;
    }

    /**
     * Ungzip an input file into an output file.
     */
    private static File unGzip(final File inputFile, final File outputDir) throws IOException
    {
        final File outputFile = new File(outputDir, inputFile.getName().substring(0, inputFile.getName().length() - 3));

        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(inputFile));
             FileOutputStream out = new FileOutputStream(outputFile))
        {
            IOUtils.copy(in, out);
        }

        return outputFile;
    }

    public static List<File> extractTarGz(File archive, File destDir) throws IOException, ArchiveException
    {
        destDir.mkdirs();
        return unTar(unGzip(archive, destDir), destDir);
    }
}
