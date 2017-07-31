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
package org.labkey.test.tests.announcements;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.announcements.InsertPage;
import org.labkey.test.pages.announcements.RespondPage;
import org.labkey.test.util.NonWindowsTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;

@Category({DailyC.class})
public class MessagesAttachmentTest extends BaseWebDriverTest implements NonWindowsTest
{
    private static final File FILES_ARCHIVE = TestFileUtils.getSampleData("filenames/illegal_chars.tar.gz");
    private static final File EXTRACTION_DIR = new File(FILES_ARCHIVE.getParentFile(), "extracted");

    // hard-code these filenames to make sure they don't get corrupted by compression or extraction
    public static final File ALERT_FILE = new File(EXTRACTION_DIR, "<img src='invalid' onerror='alert(0);'>");
    public static final File ESCAPE_FILE = new File(EXTRACTION_DIR, "hello&nbsp;&lt;world");

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
        assertFalse("Do not run this test on Windows. It uses files with illegal characters", System.getProperty("os.name").toLowerCase().contains("windows"));

        MessagesAttachmentTest init = (MessagesAttachmentTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);

        TestFileUtils.extractTarGz(FILES_ARCHIVE, EXTRACTION_DIR);
    }

    @Test
    public void testMessageAttachmentInjection() throws Exception
    {
        final String messageTitle = "Attempt Message Attachment Injection";

        InsertPage.beginAt(this)
                .setTitle(messageTitle)
                .addAttachments(ALERT_FILE, ESCAPE_FILE)
                .submit();

        final String id = getUrlParam("entityId");

        // thread page
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        RespondPage.beginAt(this, id);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));
    }

    @Test
    public void testResponseAttachmentInjection() throws Exception
    {
        final String messageTitle = "Attempt Response Attachment Injection";

        InsertPage.beginAt(this)
                .setTitle(messageTitle)
                .submit()
                .clickRespond()
                .addAttachments(ALERT_FILE, ESCAPE_FILE)
                .submit();

        final String id = getUrlParam("entityId");

        // thread page
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        RespondPage.beginAt(this, id);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));
    }

    @Test
    public void testMessageAttachmentDeletion() throws Exception
    {
        final String messageTitle = "Attempt Message Attachment Deletion";

        InsertPage.beginAt(this)
                .setTitle(messageTitle)
                .addAttachments(ALERT_FILE, ESCAPE_FILE)
                .submit()
                .clickEdit()
                .removeAttachment(0)
                .removeAttachment(0)
                .submit();

        final String id = getUrlParam("entityId");

        // thread page
        assertElementNotPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementNotPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "MessagesAttachmentTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("announcements");
    }
}