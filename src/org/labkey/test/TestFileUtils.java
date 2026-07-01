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

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
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
import org.labkey.api.util.FileUtil;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.StringUtilsLabKey;
import org.openqa.selenium.NotFoundException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
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
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.labkey.api.util.DebugInfoDumper.dumpHeap;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.util.TestDataGenerator.CHARSET_STRING;
import static org.labkey.test.util.TestDataGenerator.randomInt;
import static org.labkey.test.util.TestDataGenerator.randomName;

/**
 * Static methods for finding and reading test-related files
 */
public abstract class TestFileUtils
{
    private static final Logger LOG = LogManager.getLogger(TestFileUtils.class);

    private static final char _chQuote = '"';
    // here we quote for both tab and comma, even though
    private static final String _escapedCharsString = "\r\n\\" + _chQuote;

    private static File _labkeyRoot = null;
    private static File _buildDir = null;
    private static File _testRoot = null;
    private static File _modulesDir = null;
    private static Set<File> _sampledataDirs = null;

    public static String getFileContents(String rootRelativePath)
    {
        return getFileContents(getLabKeyRoot().toPath().resolve(rootRelativePath));
    }

    public static String getFileContents(final File file)
    {
        Path path = Paths.get(file.toURI());

        return getFileContents(path);
    }

    /**
     * Get text content of a file. Will throw an error for non-text files (e.g. PDF).
     */
    public static String getFileContents(Path path)
    {
        try
        {
            return Files.readString(path);
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }
    }

    public static long getFileRowCount(final File file) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            long lines = 0;
            while (reader.readLine() != null) lines++;
            return lines;
        }
    }

    public static String getStreamContentsAsString(InputStream is) throws IOException
    {
        return StringUtils.join(IOUtils.readLines(is, Charset.defaultCharset()).toArray(), System.lineSeparator());
    }

    public static File getLabKeyRoot()
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
                if (!FileUtil.appendName(_labkeyRoot, "server").exists())
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
                else if (!FileUtil.appendName(_labkeyRoot, "server").exists())
                {
                    throw new IllegalStateException("Unable to locate enlistment. Working directory [" + _labkeyRoot + "] isn't a recognized location. Configure manually with passing VM arg labkey.root={yourroot}");
                }
            }
        }
        return _labkeyRoot;
    }

    public static File getServerLogDir()
    {
        return FileUtil.appendName(FileUtil.appendName(getDefaultDeployDir(), "embedded"), "logs");
    }

    public static File getTestRoot()
    {
        if (_testRoot == null)
        {
            _testRoot = FileUtil.appendName(FileUtil.appendName(getLabKeyRoot(), "server"), "testAutomation");
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
            _buildDir = FileUtil.appendPath(getLabKeyRoot(), org.labkey.api.util.Path.parse("build/modules/" + getTestProjectName())); // Gradle
        }
        return _buildDir;
    }

    public static File getBaseFileRoot()
    {
        // Files are a sibling of the modules directory
        return FileUtil.appendName(getModulesDir().getParentFile(), "files");
    }

    public static File getGradleReportDir()
    {
        return FileUtil.appendPath(getTestBuildDir(), org.labkey.api.util.Path.parse("test/logs/reports"));
    }

    /**
     * Access is restricted because deployment structure varies between Non-embedded, locally built embedded, and
     * deployed embedded distribution.
     */
    static File getDefaultDeployDir()
    {
        return FileUtil.appendPath(getLabKeyRoot(), org.labkey.api.util.Path.parse("build/deploy"));
    }

    public static File getModulesDir()
    {
        if (_modulesDir == null)
        {
            // Module root when deploying from embedded distribution
            _modulesDir =  FileUtil.appendPath(getDefaultDeployDir(), org.labkey.api.util.Path.parse("embedded/modules"));
            if (!_modulesDir.isDirectory())
            {
                _modulesDir = FileUtil.appendName(getDefaultDeployDir(), "modules");
            }
        }
        return _modulesDir;
    }

    public static File getExternalModulesDir()
    {
        return FileUtil.appendName(getModulesDir().getParentFile(), "externalModules");
    }

    public static File getDefaultFileRoot(String containerPath)
    {
        return FileUtil.appendPath(getBaseFileRoot(), org.labkey.api.util.Path.parse(containerPath + "/@files"));
    }

    public static String getDefaultWebAppRoot()
    {
        File path = FileUtil.appendName(getModulesDir().getParentFile(), "labkeyWebapp");
        if (!path.isDirectory())
        {
            // Casing is different when deployed from an embedded distribution
            path = FileUtil.appendName(getModulesDir().getParentFile(), "labkeywebapp");
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
            File checkFile = FileUtil.appendPath(sampledataDir, org.labkey.api.util.Path.parse(relativePath));
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

            File sampledataDirsFile = FileUtil.appendName(getTestBuildDir(), "sampledata.dirs");
            if (sampledataDirsFile.exists())
            {
                String path = getFileContents(sampledataDirsFile);
                _sampledataDirs.addAll(Arrays.stream(path.split(";")).map(File::new).toList());
            }
            else
            {
                _sampledataDirs.add(FileUtil.appendName(getTestRoot(), "data"));
                Path modulesDir = FileUtil.appendPath(getLabKeyRoot(), org.labkey.api.util.Path.parse("server/modules")).toPath();
                try
                {
                    // We know where the modules live; no reason to insist that sampledata.dirs exists.
                    Files.walkFileTree(modulesDir, Collections.emptySet(), 2, new SimpleFileVisitor<>(){
                        @Override
                        public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs)
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
        File buildDir = FileUtil.appendName(getLabKeyRoot(), "build");
        return FileUtil.appendName(buildDir, "testTemp");
    }

    /**
     * Creates a directory under the 'testTemp' directory: 'build/testTemp/[children]'
     * @param children will be appended to the testTemp path
     * @return A file pointer to the specified directory. The directory will exist
     * @throws IOException if the directories were not created
     */
    public static File ensureTestTempDir(String... children) throws IOException
    {
        File file = getTestTempDir();
        for (String child : children)
        {
            file = FileUtil.appendName(file, child);
        }

        FileUtils.forceMkdir(file);

        return file;
    }

    /**
     * Creates a directory under the 'testTemp' directory to contain the specified file. 'build/testTemp[/children]/lastChild'
     * @param children will be appended to the testTemp path to construct the desired file's path
     * @return A file pointer to the specified file. The file's parents will exist but the file might not
     * @throws IOException if the parent directories were not created
     */
    public static File ensureTestTempFile(String... children) throws IOException
    {
        File file = getTestTempDir();

        for (String child : children)
        {
            file = FileUtil.appendName(file, child);
        }

        if (file.toString().length() == getTestTempDir().toString().length())
        {
            throw new IllegalArgumentException("No valid children were provided: " + Arrays.toString(children));
        }
        FileUtils.forceMkdirParent(file);

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

    /**
     * Deletes a directory and all its contents, retrying up to 10 times with a 10-second delay between attempts.
     * <p>
     * Before each attempt, the directory and all its children are marked writable to handle read-only files or
     * directories. This is primarily intended to work around Windows file-locking issues where an external process
     * may hold a lock on the directory or its contents.
     * <p>
     * On the final failed attempt, a heap dump is captured for diagnostics if running on TeamCity. The list of running
     * processes is also logged to help identify what may be holding the lock.
     *
     * @param dir the directory to delete
     * @throws Exception if an unexpected error occurs
     */
    public static void deleteDirWithRetry(File dir) throws Exception
    {
        // Sometimes on Windows the directory could be locked, maybe by an external process, or the child directory is
        // readonly. Use a retry mechanism to set the writeable flag and then try to delete the parent directory.
        for (int attempt = 1; attempt <= 10; attempt++) {
            try
            {
                dir.setWritable(true, false);

                // Wrap in a try to close the stream.
                try (Stream<Path> files = Files.walk(dir.toPath()))
                {
                    files.forEach(p -> p.toFile().setWritable(true, false));
                }

                FileUtils.deleteDirectory(dir);
                LOG.info(String.format("Deletion of directory %s was successful.", dir));
                break;
            } catch (AccessDeniedException e)
            {
                throw e;
            } catch (IOException | UncheckedIOException ioException) {
                LOG.warn(String.format("IOException trying to delete directory %s. Error: %s. Waiting 10s and retrying. Attempt %d of 10.",
                        dir, ioException.getMessage(), attempt));
                if (attempt == 10) {

                    if (TestProperties.isTestRunningOnTeamCity()) {
                        LOG.info("Dump the heap.");
                        dumpHeap();
                    }

                    ProcessBuilder pb;
                    if (SystemUtils.IS_OS_WINDOWS) {
                        pb = new ProcessBuilder("tasklist");
                    }
                    else {
                        pb = new ProcessBuilder("ps", "-ef");
                    }

                    try {
                        LOG.info("Lock diagnostic...");
                        pb.redirectErrorStream(true);

                        Process p = pb.start();
                        try (InputStream is = p.getInputStream()) {
                            String output = new String(is.readAllBytes(), StringUtilsLabKey.DEFAULT_CHARSET);
                            LOG.info("Running processes:\n" + output);
                        }
                        finally {
                            // Don't leak the process resource.
                            p.destroy();
                        }

                    } catch (IOException diagnosticException) {
                        LOG.warn("Failed to run lock diagnostic: " + diagnosticException.getMessage(), diagnosticException);
                    }
                    throw ioException;
                }
                sleep(10_000);
            }
        }

    }

    private static void checkFileLocation(File file)
    {
        try
        {
            if (!FileUtils.directoryContains(getLabKeyRoot(), file))
            {
                // TODO: Consider throwing IllegalArgumentException
                LOG.info("DEBUG: Attempting to delete a file outside of test enlistment: " + getLabKeyRoot());
            }
        }
        catch (IOException ignore) { }
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
        File file = FileUtil.appendPath(getTestTempDir(), org.labkey.api.util.Path.parse(name));
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
        File file = FileUtil.appendPath(getTestTempDir(), org.labkey.api.util.Path.parse(name));
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
        return writeFile(file, contents, false);
    }

    public static File writeFile(File file, String contents, boolean append) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8))
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
        try (PDDocument document = Loader.loadPDF(pdf, password))
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
                File destFile = FileUtil.appendPath(unzipDir, org.labkey.api.util.Path.parse(entry.getName()));

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
    private static List<File> unTar(final File inputFile, final File outputDir) throws IOException
    {
        final List<File> untaredFiles = new ArrayList<>();
        try (InputStream is = new FileInputStream(inputFile);
             TarArchiveInputStream inputStream = new ArchiveStreamFactory().createArchiveInputStream("tar", is))
        {
            TarArchiveEntry entry;
            Path normalizedOutputPath = outputDir.toPath().normalize();

            while ((entry = inputStream.getNextEntry()) != null)
            {
                final File outputFile = FileUtil.appendPath(outputDir, org.labkey.api.util.Path.parse(entry.getName()));

                if (!outputFile.toPath().normalize().startsWith(normalizedOutputPath))
                    throw new IOException("Bad zip entry (" + entry.getName() + ") in " + inputFile.getAbsolutePath());

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
                        IOUtils.copy(inputStream, outputFileStream);
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
        final File outputFile = FileUtil.appendName(outputDir, inputFile.getName().substring(0, inputFile.getName().length() - 3));

        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(inputFile));
             FileOutputStream out = new FileOutputStream(outputFile))
        {
            IOUtils.copy(in, out);
        }

        return outputFile;
    }

    public static List<File> extractTarGz(File archive, File destDir) throws IOException
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

    // NOTE: These constants are copied from FileUtil.java and should be kept in sync.
    private static final char[] ILLEGAL_CHARS = {'/','\\',':','?','<','>','*','|','"','^', '\n', '\r', '\''};
    public static final String ILLEGAL_CHARS_STRING = new String(ILLEGAL_CHARS);

    /**
     * Determining expected file names for downloaded files that are named according to some
     * value that might include characters that are not legal for files.
     * NOTE: This implementation is expected to exactly match FileUtil.makeLegalName(String name) defined on the server.
     */
    public static String makeLegalFileName(String name)
    {
        if (name == null)
        {
            return "__null__";
        }

        if (name.isEmpty())
        {
            return "__empty__";
        }

        //limit to 255 chars (FAT and OS X)
        //replace illegal chars
        char[] ret = new char[Math.min(255, name.length())];
        for(int idx = 0; idx < ret.length; ++idx)
        {
            char ch = name.charAt(idx);
            // Reject characters that are illegal anywhere
            if (StringUtils.contains(ILLEGAL_CHARS_STRING, ch) ||
                    // Or characters that are illegal starts to a file name
                    (idx == 0 && (ch == '-' || ch == '$')))
            {
                ch = '_';
            }
            else if (ch == '-' &&
                    idx > 0 &&
                    name.charAt(idx - 1) == ' ')
            {
                int i = idx + 1;
                // Skip through as many consecutive '-' as there might be
                while (i < name.length() && name.charAt(i) == '-')
                {
                    i++;
                }
                // If the next character after the '-' isn't a space, transform the leading '-' in the sequence
                if (i < name.length() && name.charAt(i) != ' ')
                {
                    ch = '_';
                }
            }

            ret[idx] = ch;
        }

        //can't end with space (windows)
        //can't end with period (windows)
        int lastIndex = ret.length - 1;
        char ch = ret[lastIndex];
        if (ch == ' ' || ch == '.')
            ret[lastIndex] = '_';

        return new String(ret);
    }

    public static String randomFileName(@NotNull String part, @Nullable String extension)
    {
        return randomFileName(part, extension, null, null);
    }

    public static String randomFileName(@NotNull String part, @Nullable String extension, @Nullable Integer numStartChars, @Nullable Integer numEndChars)
    {
        String baseName = makeLegalFileName(randomName(part, numStartChars == null ? randomInt(0, 5) : numStartChars, numEndChars == null ? randomInt(0, 5) : numEndChars, CHARSET_STRING, null).name());
        if (extension != null)
            return baseName + extension;
        return baseName;
    }
}
