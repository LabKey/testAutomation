/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category(DailyB.class)
public class FileBrowserIconsTest extends BaseWebDriverTest
{
    protected final static String SAMPLE_DATA_LOC =  "/sampledata/fileTypes/";

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
        FileBrowserIconsTest init = (FileBrowserIconsTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {

        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);

        log("Use the pipeline to poulate the file browser.");  // It's quicker than uploading each file individually.
        portalHelper.addWebPart("Pipeline Files");
        setPipelineRoot(TestFileUtils.getLabKeyRoot() + SAMPLE_DATA_LOC);

    }

    @Test
    public final void testSteps()
    {
        boolean pass = true;

        try
        {
            log("Validate the pipeline job has completed.");
            waitForElementToDisappear(Locator.css("div.x4-grid-empty"), 5000);
        }
        catch(org.openqa.selenium.TimeoutException te)
        {
            assertTrue("Grid was never populated, check for pipeline error.", false);
        }

        log("Validate number of text icons is correct.");
        pass = validateCount("span.fa-file-text-o", 8) & pass;

        log("Validate number of code icons is correct.");
        pass = validateCount("span.fa-file-code-o", 4) & pass;

        log("Validate number of rtf\\word icons is correct.");
        pass = validateCount("span.fa-file-word-o", 5) & pass;

        log("Validate number of image icons is correct.");
        pass = validateCount("span.fa-file-image-o", 6) & pass;

        log("Validate number of archive icons is correct.");
        pass = validateCount("span.fa-file-archive-o", 3) & pass;

        log("Validate number of video icons is correct.");
        pass = validateCount("span.fa-file-video-o", 1) & pass;

        log("Validate number of pdf icons is correct.");
        pass = validateCount("span.fa-file-pdf-o", 1) & pass;

        log("Validate number of excel icons is correct.");
        pass = validateCount("span.fa-file-excel-o", 4) & pass;

        log("Validate number of list icons is correct.");
        pass = validateCount("span.fa-list-alt", 1) & pass;

        log("Validate number of powerpoint icons is correct.");
        pass = validateCount("span.fa-file-powerpoint-o", 2) & pass;

        assertTrue("Count(s) for icons were not as expected. Review log to find the counts that were wrong.", pass);
    }

    private boolean validateCount(String cssIcon, int expCount)
    {
        int count = getElementCount(Locator.css(cssIcon));

        if (getElementCount(Locator.css(cssIcon)) != expCount)
        {
            log("!!!!!Number of " + cssIcon + " is not as expected. Expected: " + expCount + " Actual: " + count);
            return false;
        }
        else
        {
            return true;
        }
    }

}
