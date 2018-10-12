package org.labkey.test.util.core.webdav;

import com.github.sardine.Sardine;
import org.apache.commons.lang3.StringUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.Random;

public class WebDavUtils
{
    private static final Random random = new Random();
    /**
     * @param containerPath e.g. "Test Project/subfolder"
     * @param webDavDir e.g. "@files" or "@pipeline"
     * @return https://localhost:8080/labkey/_webdav/Test%20Project/subfolder/@files/
     */
    public static String buildBaseWebDavUrl(String containerPath, String webDavDir)
    {
        return WebTestHelper.getBaseURL() + "/_webdav/"
                + StringUtils.strip(containerPath, "/").replace(" ", "%20") + "/"
                + StringUtils.strip(webDavDir, "/");
    }

    public static String buildBaseWebDavUrl(String containerPath)
    {
        return buildBaseWebDavUrl(containerPath, "@files");
    }

    public static String buildBaseWebfilesUrl(String containerPath)
    {
        return WebTestHelper.getBaseURL() + "/_webfiles/"
                + StringUtils.strip(containerPath, "/").replace(" ", "%20") + "/";
    }

    @LogMethod
    public static void putRandomBytes(Sardine sardine, @LoggedParam String davUrlPrefix, @LoggedParam int fileCount, int fileSize) throws IOException
    {
        TestLogger.log("Uploading files to: " + davUrlPrefix);
        int progressFrequency = Math.max(10, fileCount / 10);
        for (int i = 1; i <= fileCount; i++)
        {
            String fileName = "random_" + i + ".bin";
            byte[] fileBytes = new byte[fileSize];
            random.nextBytes(fileBytes);
            if (i % progressFrequency == 0 || i == fileCount)
                TestLogger.log("Upload progress: " + i + "/" + fileCount);
            sardine.put(davUrlPrefix + fileName, fileBytes);
        }
    }

    public static class WebDavUrlFactory
    {
        protected final String baseUrl;

        protected WebDavUrlFactory(String baseUrl)
        {
            this.baseUrl = StringUtils.stripEnd(baseUrl, "/") + "/";
        }

        protected WebDavUrlFactory(String containerPath, String webDavDir)
        {
            this(buildBaseWebDavUrl(containerPath, webDavDir));
        }

        public String getPath(String relativePath)
        {
            return baseUrl + StringUtils.stripStart(relativePath, "/");
        }
    }

    public static class WebDavLocalUrlFactory extends WebDavUrlFactory
    {
        public WebDavLocalUrlFactory(String containerPath)
        {
            super(buildBaseWebDavUrl(containerPath, "@files"));
        }
    }

    public static class WebDavCloudUrlFactory extends WebDavUrlFactory
    {
        public WebDavCloudUrlFactory(String containerPath)
        {
            super(buildBaseWebDavUrl(containerPath, "@cloud/" + TestProperties.getCloudPipelineBucketName()));
        }
    }

    public static class WebFilesUrlFactory extends WebDavUrlFactory
    {
        public WebFilesUrlFactory(String containerPath)
        {
            super(buildBaseWebfilesUrl(containerPath));
        }
    }
}
