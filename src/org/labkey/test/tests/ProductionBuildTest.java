package org.labkey.test.tests;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This test will only work if you have built with `ant production`
 */
@Category({DailyB.class})
public class ProductionBuildTest extends BaseWebDriverTest
{
    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

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
                "ext-all-sandbox"
        );

        for (String baseFileName : baseFileNames)
        {
            log("Checking: " + baseFileName);
            try
            {
                String minFileName = baseFileName + ".min.js";
                int responseCode = WebTestHelper.getHttpGetResponse(WebTestHelper.getBaseURL() + "/" + minFileName);
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
                "clientapi_core",
                "internal"
        );

        for (String baseFileName : baseFileNames)
        {
            log("Checking: " + baseFileName);
            try
            {
                String jsFileName = baseFileName + ".js";
                int responseCode = WebTestHelper.getHttpGetResponse(WebTestHelper.getBaseURL() + "/" + jsFileName);
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
        int expectedResponse = TestProperties.isDevModeEnabled() ? HttpStatus.SC_NOT_FOUND : HttpStatus.SC_OK;
        int responseCode = WebTestHelper.getHttpGetResponse(WebTestHelper.getBaseURL() + "/" + fileName + ".gz");
        Assert.assertEquals("GZ file returned wrong response: " + fileName, expectedResponse, responseCode);

    }

    private void assertGzipEncodedResponse(String fileName) throws IOException
    {
        String url = WebTestHelper.getBaseURL() + "/" + fileName;
        HttpResponse response = null;

        try (CloseableHttpClient client = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept-Encoding", "gzip, deflate, sdch");

            response = client.execute(get, WebTestHelper.getBasicHttpContext());
            Header encodingHeader = response.getEntity().getContentEncoding();
            String encoding = encodingHeader == null ? null : encodingHeader.getValue();

            String expectedEncoding = TestProperties.isDevModeEnabled() ? null : "gzip";
            Assert.assertEquals("File had wrong 'Content-Encoding'", expectedEncoding, encoding);
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }
}
