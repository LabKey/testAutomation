/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.tests.issues;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.issues.ClosePage;
import org.labkey.test.pages.issues.DetailsListPage;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.ReopenPage;
import org.labkey.test.pages.issues.ResolvePage;
import org.labkey.test.pages.issues.UpdatePage;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.NonWindowsTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;

@Category({DailyC.class})
public class IssuesAttachmentTest extends BaseWebDriverTest implements NonWindowsTest
{
    private static final File FILES_ARCHIVE = TestFileUtils.getSampleData("filenames/illegal_chars.tar.gz");
    private static final File EXTRACTION_DIR = new File(FILES_ARCHIVE.getParentFile(), "extracted");

    // hard-code these filenames to make sure they don't get corrupted by compression or extraction
    public static final File ALERT_FILE = new File(EXTRACTION_DIR, "<img src='invalid' onerror='alert(0);'>");
    public static final File ESCAPE_FILE = new File(EXTRACTION_DIR, "hello&nbsp;&lt;world");

    public static final String LIST_DEF_NAME = "Issues";

    private IssuesHelper issuesHelper = new IssuesHelper(this);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        try
        {
            FileUtils.deleteDirectory(EXTRACTION_DIR);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        IssuesAttachmentTest init = (IssuesAttachmentTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        assertFalse("Do not run this test on Windows. It uses files with illegal characters", System.getProperty("os.name").toLowerCase().contains("windows"));

        TestFileUtils.extractTarGz(FILES_ARCHIVE, EXTRACTION_DIR);

        _containerHelper.createProject(getProjectName(), null);
        issuesHelper.createNewIssuesList(LIST_DEF_NAME, _containerHelper);
    }

    @Test
    public void testAttachmentInjection() throws Exception
    {
        goToProjectHome();
        Map<String, String> issue = Maps.of("assignedTo", getDisplayName(), "title", "Attempt Issue Attachment Injection");
        final DetailsPage detailsPage = issuesHelper.addIssue(issue, ALERT_FILE, ESCAPE_FILE);
        final String issueId = detailsPage.getIssueId();

        final Locator.XPathLocator alertFile = Locator.linkWithText(" " + ALERT_FILE.getName());
        final Locator.XPathLocator escapeFile = Locator.linkWithText(" " + ESCAPE_FILE.getName());

        // details page
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);

        detailsPage.clickPrint();
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);

        UpdatePage.beginAt(this, issueId);
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);

        ResolvePage.beginAt(this, issueId);
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);

        ClosePage.beginAt(this, issueId);
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);

        ReopenPage.beginAt(this, issueId);
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);

        DetailsListPage.beginAt(this, LIST_DEF_NAME);
        assertElementPresent(alertFile);
        assertElementPresent(escapeFile);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssuesAttachmentTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues");
    }
}
