package org.labkey.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Static methods for finding finding and reading test-related files
 */
public abstract class TestFileUtils
{
    private static String _labkeyRootCache = null;

    public static String getFileContents(String rootRelativePath)
    {
        return getFileContents(new File(getLabKeyRoot(), rootRelativePath));
    }

    public static String getFileContents(final File file)
    {
        try
        {
            return new String(Files.readAllBytes(Paths.get(file.toURI())));
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }
    }

    public static String getStreamContentsAsString(InputStream is) throws IOException
    {
        StringBuilder contents = new StringBuilder();
        try(BufferedReader input = new BufferedReader(new InputStreamReader(is)))
        {
            String line;
            while ((line = input.readLine()) != null)
            {
                contents.append(line);
                contents.append("\n");
            }
        }
        return contents.toString();
    }

    public static String getLabKeyRoot()
    {
        if (_labkeyRootCache == null)
        {
            _labkeyRootCache = System.getProperty("labkey.root", "..");

            File labkeyRoot = new File(_labkeyRootCache);

            if (!labkeyRoot.exists())
            {
                throw new IllegalStateException("Specified LabKey root does not exist [" + _labkeyRootCache + "]. Configure this by passing VM arg '-Dlabkey.root=[yourroot]'.");
            }

            try
            {
                _labkeyRootCache = labkeyRoot.getCanonicalPath();
            }
            catch (IOException badPath)
            {
                throw new IllegalStateException("Unable to canonicalize specified LabKey root [" + _labkeyRootCache + "]. Configure this by passing VM arg '-Dlabkey.root=[yourroot]'.");
            }

            System.out.println("Using labkey root '" + _labkeyRootCache + "', as provided by system property 'labkey.root'.");
        }
        return _labkeyRootCache;
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

    public static File getSampleData(String relativePath)
    {
        String path;
        File sampledataDirsFile = new File(getLabKeyRoot(), "server/test/build/sampledata.dirs");

        if (sampledataDirsFile.exists())
        {
            path = getFileContents(sampledataDirsFile);
        }
        else
        {
            path = getSampledataPath();
        }

        List<String> splitPath = Arrays.asList(path.split(";"));

        File foundFile = null;
        for (String sampledataDir : splitPath)
        {
            File checkFile = new File(sampledataDir, relativePath);
            if (checkFile.exists())
            {
                if (foundFile != null)
                    throw new IllegalArgumentException("Ambiguous file specified: " + relativePath + "\n" +
                            "Found:\n" +
                            foundFile + "\n" +
                            checkFile);
                else
                    foundFile = checkFile;
            }
        }

        assertNotNull("Sample data not found: " + relativePath + "\n" +
                "In: " + path, foundFile);
        return foundFile;
    }

    public static String getSampledataPath()
    {
        File path = new File(getLabKeyRoot(), "sampledata");
        return path.toString();
    }

    public static File getApiScriptFolder()
    {
        return new File(getLabKeyRoot(), "server/test/data/api");
    }
}
