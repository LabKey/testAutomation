/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.util.core.webdav;

import com.github.sardine.Sardine;
import org.apache.commons.lang3.StringUtils;
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
                + StringUtils.strip(webDavDir, "/") + "/";
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
            if (i % progressFrequency == 0 || i == fileCount)
                TestLogger.log("Upload progress: " + i + "/" + fileCount);
            putRandomBytes(sardine, davUrlPrefix + fileName, fileSize);
        }
    }

    @LogMethod
    public static void putRandomBytes(Sardine sardine, @LoggedParam String fullDavUrl, int fileSize) throws IOException
    {
        byte[] fileBytes = new byte[fileSize];
        random.nextBytes(fileBytes);
        sardine.put(fullDavUrl, fileBytes);
    }

    /**
     * @deprecated Moved to {@link org.labkey.test.util.core.webdav.WebDavUrlFactory}
     * TODO: remove in 19.2
     */
    @Deprecated(forRemoval = true)
    public static class WebDavUrlFactory extends org.labkey.test.util.core.webdav.WebDavUrlFactory
    {
        protected WebDavUrlFactory(String baseUrl)
        {
            super(baseUrl);
        }
    }

    /**
     * @deprecated Moved to {@link org.labkey.test.util.core.webdav.WebDavUrlFactory#webDavUrlFactory(String)}
     * Leave in place to ease merges from panoramaweb18.3
     * TODO: remove in 19.2
     */
    @Deprecated(forRemoval = true)
    public static WebDavUrlFactory webDavUrlFactory(String containerPath)
    {
        return new WebDavUrlFactory(buildBaseWebDavUrl(containerPath, "@files"));
    }
}
