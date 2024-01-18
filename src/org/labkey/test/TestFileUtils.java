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
package org.labkey.test;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.bouncycastle.util.io.Streams;
import org.jetbrains.annotations.NotNull;
import org.labkey.serverapi.writer.PrintWriters;
import org.openqa.selenium.NotFoundException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Static methods for finding and reading test-related files
 */
public abstract class TestFileUtils
{
    private static final Logger LOG = LogManager.getLogger(TestFileUtils.class);

    private static File _labkeyRoot = null;
    private static File _buildDir = null;
    private static File _testRoot = null;
    private static File _modulesDir = null;
    private static Set<File> _sampledataDirs = null;

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
        return StringUtils.join(IOUtils.readLines(is, Charset.defaultCharset()).toArray(), System.lineSeparator());
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
                if (!new File(_labkeyRoot, "server").exists())
                {
                    throw new IllegalStateException("Specified LabKey root exists [" + _labkeyRoot + "] but isn't the root of a LabKey enlistment. Configure this by passing VM arg labkey.root={yourroot}");
                }

                _labkeyRoot = _labkeyRoot.getAbsoluteFile().toPath().normalize().toFile();

                LOG.info("Using labkey root '" + _labkeyRoot + "', as provided by system property 'labkey.root'.");
            }
            else
            {
                _labkeyRoot = new File("").getAbsoluteFile();
                if (_labkeyRoot.getParentFile().getName().equals("server"))
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

    public static File getServerLogDir()
    {
        if (TestProperties.isEmbeddedTomcat())
        {
            return new File(getDefaultDeployDir(), "embedded/logs");
        }
        else
        {
            return new File(TestProperties.getTomcatHome(), "logs");
        }
    }

    public static File getTestRoot()
    {
        if (_testRoot == null)
        {
            _testRoot = new File(getLabKeyRoot(), "server/testAutomation");
        }
        return _testRoot;
    }

    private static String getTestProjectName()
    {
        return getTestRoot().getName();
    }

    public static File getTestBuildDir()
    {
        if (_buildDir == null)
        {
            _buildDir = new File(getLabKeyRoot(), "build/modules/" + getTestProjectName()); // Gradle
        }
        return _buildDir;
    }

    private static File getBaseFileRoot()
    {
        // Files are a sibling of the modules directory
        return new File(getModulesDir().getParentFile(), "files");
    }

    public static File getGradleReportDir()
    {
        return new File(getTestBuildDir(), "test/logs/reports");
    }

    /**
     * Private because deployment structure varies between Non-embedded, locally built embedded, and deployed embedded
     * distribution.
     */
    private static File getDefaultDeployDir()
    {
        return new File(getLabKeyRoot(), "build/deploy");
    }

    public static File getModulesDir()
    {
        if (_modulesDir == null)
        {
            _modulesDir = new File(getDefaultDeployDir(), "modules");
            if (TestProperties.isEmbeddedTomcat() && !_modulesDir.isDirectory())
            {
                // Module root when deploying from embedded distribution
                _modulesDir = new File(getDefaultDeployDir(), "embedded/server/modules");
            }
        }
        return _modulesDir;
    }

    public static File getDefaultFileRoot(String containerPath)
    {
        return new File(getBaseFileRoot(), containerPath + "/@files");
    }

    public static String getDefaultWebAppRoot()
    {
        File path = new File(getModulesDir().getParentFile(), "labkeyWebapp");
        if (!path.isDirectory())
        {
            // Casing is different when deployed from an embedded distribution
            path = new File(getModulesDir().getParentFile(), "labkeywebapp");
        }
        return path.toString();
    }

    /**
     * Searches all sampledata directories for the specified file.
     *
     * @param relativePath e.g. "lists/ListDemo.lists.zip" or "OConnor_Test.folder.zip"
     * @return File object with the full path to the specified file
     *
     * @see #getSampleDataDirs()
     */
    @NotNull
    public static File getSampleData(String relativePath)
    {
        List<File> sampleDatas = getSampleDatas(relativePath);

        if (sampleDatas.isEmpty())
        {
            throw new NotFoundException("Sample data not found: " + relativePath + "\n" +
                    "Run `./gradlew :server:test:build :server:test:writeSampleDataFile` once to locate all sampledata" + "\n" +
                    "Currently known sample data locations: " + getSampleDataDirs().stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")));
        }
        if (sampleDatas.size() > 1)
        {
            throw new IllegalArgumentException(
                "Ambiguous file specified: " + relativePath + "\n" +
                    "Found:\n" +
                    sampleDatas.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")));
        }

        return sampleDatas.get(0);
    }

    /**
     * Searches all sampledata directories for the specified relative path.
     *
     * @param relativePath e.g. "lists/ListDemo.lists.zip" or "OConnor_Test.folder.zip"
     * @return files with the relative path in all sampledata directories
     *
     * @see #getSampleDataDirs()
     */
    @NotNull
    public static List<File> getSampleDatas(String relativePath)
    {
        Set<File> sampledataDirs = getSampleDataDirs();
        List<File> foundFiles = new ArrayList<>();

        for (File sampledataDir : sampledataDirs)
        {
            File checkFile = new File(sampledataDir, relativePath);
            if (checkFile.exists())
            {
                foundFiles.add(checkFile);
            }
        }

        return foundFiles;
    }

    @NotNull
    public static Set<File> getSampleDataDirs()
    {
        if (_sampledataDirs == null)
        {
            _sampledataDirs = new TreeSet<>();

            File sampledataDirsFile = new File(getTestBuildDir(), "sampledata.dirs");
            if (sampledataDirsFile.exists())
            {
                String path = getFileContents(sampledataDirsFile);
                _sampledataDirs.addAll(Arrays.stream(path.split(";")).map(File::new).collect(Collectors.toList()));
            }
            else
            {
                _sampledataDirs.add(new File(getTestRoot(), "data"));
                Path modulesDir = new File(getLabKeyRoot(), "server/modules").toPath();
                try
                {
                    // We know where the modules live; no reason to insist that sampledata.dirs exists.
                    Files.walkFileTree(modulesDir, Collections.emptySet(), 2, new SimpleFileVisitor<>(){
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                        {
                            if (dir.equals(modulesDir))
                            {
                                return FileVisitResult.CONTINUE;
                            }
                            if (dir.resolve("module.properties").toFile().exists()) // In a module directory?
                            {
                                final File sampledataDir = dir.resolve("test/sampledata").toFile();
                                if (sampledataDir.exists())
                                {
                                    _sampledataDirs.add(sampledataDir);
                                }
                                return FileVisitResult.SKIP_SUBTREE; // No nested modules, stop digging.
                            }
                            return FileVisitResult.CONTINUE; // In a module container, walk the modules.
                        }
                    });
                }
                catch (IOException e)
                {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        return _sampledataDirs;
    }

    public static File getTestTempDir()
    {
        File buildDir = new File(getLabKeyRoot(), "build");
        return new File(buildDir, "testTemp");
    }

    public static File ensureTestTempDir() throws IOException
    {
        File file = getTestTempDir();
        FileUtils.forceMkdir(file);
        return file;
    }

    public static void delete(File file)
    {
        LOG.info("Deleting from filesystem: " + file.toString());
        checkFileLocation(file);

        if (!file.exists())
            return;

        FileUtils.deleteQuietly(file);

        if (!file.exists())
            LOG.info("Deletion successful.");
        else
            LOG.info("Failed to delete : " + file.getAbsolutePath());
    }

    public static void deleteDir(File dir)
    {
        LOG.info("Deleting from filesystem: " + dir.toString());
        checkFileLocation(dir);
        if (!dir.exists())
            return;

        try
        {
            FileUtils.deleteDirectory(dir);
            LOG.info("Deletion successful.");
        }
        catch (IOException e)
        {
            LOG.info("WARNING: Exception deleting directory -- " + e.getMessage());
        }
    }

    private static void checkFileLocation(File file)
    {
        try
        {
            if (!FileUtils.directoryContains(new File(getLabKeyRoot()), file))
            {
                // TODO: Consider throwing IllegalArgumentException
                LOG.info("DEBUG: Attempting to delete a file outside of test enlistment: " + getLabKeyRoot());
            }
        }
        catch (IOException ignore) { }
    }

    /**
     *
     * @param dir Location to create new file
     * @param fileName Name of file to be created
     * @param contents Text contents of file
     * @return File object pointing to new file
     * @deprecated Use {@link #writeFile(File, String)} or {@link #writeTempFile(String, String)}
     */
    @Deprecated
    public static File saveFile(File dir, String fileName, String contents)
    {
        File tsvFile = new File(dir, fileName);

        try
        {
            return writeFile(tsvFile, contents);
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Write text to a file in the test temp directory. Temp directory will be created if it does not exist.
     * @param name Name of the file to be created. An existing file will be overwritten
     * @param contents text to write to the file
     * @return File object pointing to the new file
     * @throws IOException If an I/O error occurs when opening or writing to the file
     */
    public static File writeTempFile(String name, InputStream contents) throws IOException
    {
        File file = new File(getTestTempDir(), name);
        FileUtils.forceMkdirParent(file);

        FileUtils.copyInputStreamToFile(contents, file);
        return file;
    }

    /**
     * Write text to a file in the test temp directory. Temp directory will be created if it does not exist.
     * @param name Name of the file to be created. An existing file will be overwritten
     * @param contents text to write to the file
     * @return File object pointing to the new file
     * @throws IOException If an I/O error occurs when opening or writing to the file
     */
    public static File writeTempFile(String name, String contents) throws IOException
    {
        File file = new File(getTestTempDir(), name);
        FileUtils.forceMkdirParent(file);

        return writeFile(file, contents);
    }

    /**
     * Write text to a file
     * @param file target file. Parent directory should exist. Existing file will be overwritten.
     * @param contents text to write to the file
     * @return the initially provided file
     * @throws IOException If an I/O error occurs when opening or writing to the file
     */
    public static File writeFile(File file, String contents) throws IOException
    {
        try (Writer writer = PrintWriters.getPrintWriter(file))
        {
            writer.write(contents);
            return file;
        }
    }

    public static String readPdfText(File pdf)
    {
        return readPdfText(pdf, null);
    }

    public static String readPdfText(File pdf, String password)
    {
        try (PDDocument document = PDDocument.load(pdf, password))
        {
            if (document.isEncrypted())
            {
                document.setAllSecurityToBeRemoved(true);
            }

            return new PDFTextStripper().getText(document);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean isFileInZipArchive(File zipArchive, String fileName) throws IOException
    {
        List<String> files = getFilesInZipArchive(zipArchive);
        return files.stream().anyMatch((f)-> f.endsWith(fileName));
    }

    public static List<String> getFilesInZipArchive(File zipArchive) throws IOException
    {
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipArchive))))
        {
            ZipEntry entry;
            List<String> files = new ArrayList<>();
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                files.add(entry.getName());
            }
            return files;
        }
    }

    @SuppressWarnings("Duplicates")
    public static List<File> unzipToDirectory(File sourceZip, File unzipDir) throws IOException
    {
        List<File> files = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceZip));
             BufferedInputStream is = new BufferedInputStream(zis))
        {
            ZipEntry entry;

            while (null != (entry = zis.getNextEntry()))
            {
                File destFile = new File(unzipDir, entry.getName());

                if (!destFile.getCanonicalPath().startsWith(unzipDir.getCanonicalPath() + File.separator)) {
                    throw new IOException("Zip entry is outside of the target dir: " + entry.getName());
                }

                if (entry.isDirectory())
                {
                    destFile.mkdirs();
                    if (!destFile.isDirectory())
                    {
                        throw new IOException("Failed to create directory: " + destFile.getName());
                    }
                    continue;
                }

                destFile.getParentFile().mkdirs();
                if (destFile.exists())
                {
                    throw new IOException("File already exists: " + destFile.getName());
                }
                if (!destFile.createNewFile())
                {
                    throw new IOException("Failed to extract file: " + destFile.getName());
                }

                try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(destFile)))
                {
                    IOUtils.copy(is, os);
                }

                files.add(destFile);
                zis.closeEntry();
            }
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

    public static byte[] decrypt(byte[] encrypted, char[] passPhrase) throws IOException, PGPException
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        InputStream in = new ByteArrayInputStream(encrypted);
        in = PGPUtil.getDecoderStream(in);

        JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        if (o instanceof PGPEncryptedDataList)
        {
            enc = (PGPEncryptedDataList) o;
        }
        else
        {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData) enc.get(0);
        InputStream clear = pbe.getDataStream(new JcePBEDataDecryptorFactoryBuilder(new JcaPGPDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build())
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passPhrase));

        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(clear);
        PGPCompressedData cData = (PGPCompressedData) pgpFact.nextObject();
        pgpFact = new JcaPGPObjectFactory(cData.getDataStream());
        PGPLiteralData ld = (PGPLiteralData) pgpFact.nextObject();
        return Streams.readAll(ld.getInputStream());
    }

}
