/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.tests.filecontent;

import org.apache.hc.core5.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.pages.files.FileContentPage;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * GH Issue 1552: file timestamps must render in the server's time zone, not the viewer's.
 * <p>
 * The expectation is built from the raw instant, not from the server's pre-formatted string, so the test fails against
 * an unfixed server rather than erroring on a missing property.
 */
@Category({Daily.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class FileBrowserTimeZoneTest extends BaseWebDriverTest
{
    private static final File TEST_FILE = TestFileUtils.getSampleData("InlineImages/help.jpg");

    // The zone offset makes a timestamp rendered in the wrong zone impossible to mistake for a correct one.
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileBrowserTimeZoneTest initTest = getCurrentTest();
        initTest.doSetupSteps();
    }

    private void doSetupSteps() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        new APIContainerHelper(this).setDateAndTimeFormats(createDefaultConnection(), getProjectName(),
                null, null, DATE_TIME_FORMAT);

        new PortalHelper(this).addWebPart("Files");
        _fileBrowserHelper.uploadFile(TEST_FILE);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        _fileBrowserHelper.waitForFileGridReady();
    }

    @Test
    public void testFileTimestampsUseServerTimeZone() throws IOException
    {
        OffsetDateTime modified = OffsetDateTime.parse(getWebDavProperties(TEST_FILE.getName()).getString("lastmodified"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

        // The test JVM is assumed to share the server's time zone, as elsewhere in the framework. See
        // WebDriverWrapper.getRelativeTimeZoneOffset.
        String expected = formatter.format(modified.atZoneSameInstant(ZoneId.systemDefault()));
        String browserZoneRendering = formatter.format(modified.atZoneSameInstant(getClientTimeZone().toZoneId()));

        if (expected.equals(browserZoneRendering))
            log("Browser and server time zones agree on this run, so a timestamp rendered in the browser's zone would go unnoticed. Set 'webtest.browser.tz' to a zone with a different offset.");

        checker().verifyEquals(timeZoneError("'Last Modified' column", browserZoneRendering),
                expected, _fileBrowserHelper.getFileLastModified(TEST_FILE.getName()));

        checker().screenShotIfNewError("last_modified_column");

        // The Files web part suppresses the details panel; only the 'Manage Files' page shows it.
        FileContentPage.beginAt(this);
        _fileBrowserHelper.waitForFileGridReady();
        _fileBrowserHelper.selectFileBrowserItem(TEST_FILE.getName());
        checker().verifyEquals(timeZoneError("'Modified' in the file details panel", browserZoneRendering),
                expected, _fileBrowserHelper.getDetailsModified());

        checker().screenShotIfNewError("file_details_modified");
    }

    private static String timeZoneError(String where, String browserZoneRendering)
    {
        return "%s wasn't rendered in the server's time zone. The browser's time zone would render it as '%s'"
                .formatted(where, browserZoneRendering);
    }

    /**
     * Fetch a file's properties straight from the WebDAV listing the file browser's grid is built from.
     */
    private JSONObject getWebDavProperties(String fileName) throws IOException
    {
        String url = "%s/_webdav/%s/@files?method=JSON"
                .formatted(WebTestHelper.getBaseURL(), EscapeUtil.encodeUriPath(getProjectName()));
        SimpleHttpRequest request = new SimpleHttpRequest(url);
        request.copySession(getDriver());

        SimpleHttpResponse response = request.getResponse();
        assertEquals("WebDAV listing failed: " + url, HttpStatus.SC_OK, response.getResponseCode());

        JSONArray files = new JSONObject(response.getResponseBody()).getJSONArray("files");
        for (int i = 0; i < files.length(); i++)
        {
            JSONObject file = files.getJSONObject(i);
            if (fileName.equals(file.optString("text")))
                return file;
        }

        throw new AssertionError("'%s' is missing from the WebDAV listing: %s".formatted(fileName, url));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("filecontent");
    }

    @Override
    protected String getProjectName()
    {
        return "FileBrowserTimeZoneTest Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
