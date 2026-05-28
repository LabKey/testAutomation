/*
 * Copyright (c) 2016-2026 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 4 )
public class FileBrowserIconsTest extends BaseWebDriverTest
{
    protected final static File SAMPLE_DATA_LOC =  TestFileUtils.getSampleData("fileTypes");

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @Override
    protected String getProjectName()
    {
        return "FileBrowserIconsTestProject";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @BeforeClass
    public static void initTest()
    {
        FileBrowserIconsTest init = getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {

        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);

        log("Use the pipeline to poulate the file browser.");  // It's quicker than uploading each file individually.
        portalHelper.addWebPart("Pipeline Files");
        setPipelineRoot(SAMPLE_DATA_LOC.getAbsolutePath());

    }

    @Test
    public final void testSteps()
    {
        _fileBrowserHelper.waitForFileGridReady();

        validateCount("text", "span.fa-file-text-o", 8);
        validateCount("code", "span.fa-file-code-o", 4);
        validateCount("rtf/word", "span.fa-file-word-o", 5);
        validateCount("image", "span.fa-file-image-o", 6);
        validateCount("archive", "span.fa-file-archive-o", 5);
        validateCount("video", "span.fa-file-video-o", 1);
        validateCount("pdf", "span.fa-file-pdf-o", 2);
        validateCount("excel", "span.fa-file-excel-o", 4);
        validateCount("list", "span.fa-list-alt", 2);
        validateCount("powerpoint", "span.fa-file-powerpoint-o", 2);
        validateCount("file", "span.fa-file-o", 1);

        checker().screenShotIfNewError("icon_counts");
    }

    private void validateCount(String description, String cssIcon, int expCount)
    {
        log("Validate number of %s icons is correct.".formatted(description));
        int count = Locator.css(cssIcon).findElements(getDriver()).size();

        checker().verifyEquals(description + " icons", expCount, count);
    }

}
