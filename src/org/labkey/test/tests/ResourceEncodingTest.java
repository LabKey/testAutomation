/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
package org.labkey.test.tests;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This test will only work for production builds
 */
@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class ResourceEncodingTest extends BaseWebDriverTest
{
    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    /**
     * Tests that certain files are not minified, but are gzipped (in production mode)
     */
    @Test
    public void testNonMinifiedJavascript()
    {
        List<String> baseFileNames = Arrays.asList(
                "labkey",
                "GuidedTip",
                "ext-4.2.1/ext-all-sandbox"
        );

        for (String baseFileName : baseFileNames)
        {
            log("Checking: " + baseFileName);
            try
            {
                String minFileName = baseFileName + ".min.js";
                int responseCode = WebTestHelper.getHttpResponse(WebTestHelper.getBaseURL() + "/" + minFileName).getResponseCode();
                Assert.assertEquals("Minified file should not be present: " + minFileName, HttpStatus.SC_NOT_FOUND, responseCode);

                assertGzipResource(baseFileName + ".js");
            }
            catch (IOException fail)
            {
                throw new RuntimeException(fail);
            }
        }
    }

    /**
     * Tests that certain files are minified and gzipped (in production mode)
     */
    @Test
    public void testMinifiedJavascript()
    {
        List<String> baseFileNames = Arrays.asList(
                "clientapi",
                "clientapi/labkey-api-js-core",
                "internal"
        );

        for (String baseFileName : baseFileNames)
        {
            log("Checking: " + baseFileName);
            try
            {
                String jsFileName = baseFileName + ".js";
                int responseCode = WebTestHelper.getHttpResponse(WebTestHelper.getBaseURL() + "/" + jsFileName).getResponseCode();
                Assert.assertEquals("Non-minified file should not be available: " + jsFileName, HttpStatus.SC_NOT_FOUND, responseCode);

                assertGzipResource(baseFileName + ".min.js");
            }
            catch (IOException fail)
            {
                throw new RuntimeException(fail);
            }
        }
    }

    private void assertGzipResource(String fileName) throws IOException
    {
        int responseCode = WebTestHelper.getHttpResponse(WebTestHelper.getBaseURL() + "/" + fileName).getResponseCode();
        Assert.assertEquals("File not available: " + fileName, HttpStatus.SC_OK, responseCode);
        responseCode = WebTestHelper.getHttpResponse(WebTestHelper.getBaseURL() + "/" + fileName + ".gz").getResponseCode();
        Assert.assertEquals("GZ file not available: " + fileName, HttpStatus.SC_OK, responseCode);
    }

    /**
     * Might come back to this. This is, theoretically, a better way to check that js files are Gzipped
     * I can't get response.getEntity().getContentEncoding() to return anything but null
     */
    private void assertGzipEncodedResponse(String fileName) throws IOException
    {
        String url = WebTestHelper.getBaseURL() + "/" + fileName;

        try (CloseableHttpClient client = WebTestHelper.getHttpClient())
        {
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept-Encoding", "gzip, deflate, sdch");

            try (CloseableHttpResponse response = client.execute(get, WebTestHelper.getBasicHttpContext()))
            {
                String encoding = response.getEntity().getContentEncoding();

                String expectedEncoding = TestProperties.isDevModeEnabled() ? null : "gzip";
                Assert.assertEquals("File had wrong 'Content-Encoding'", expectedEncoding, encoding);
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }
}
