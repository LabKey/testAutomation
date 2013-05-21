/*
 * Copyright (c) 2013 LabKey Corporation
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

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;

public class ReportThumbnailTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ReportThumbnailTest";
    private static final File TEST_STUDY = new File(getSampledataPath(), "study/LabkeyDemoStudyWithCharts.folder.zip");
    private static final File TEST_THUMBNAIL = new File(getSampledataPath(), "Microarray/test1.jpg");
    private String THUMBNAIL_DATA;

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        doSetup();
        goToDataViews();
        setThumbnailSRC("Example Box Plot");
        toggleThumbnailType("Example Box Plot", true);
        assertNewThumbnail("Example Box Plot");
        assignCustomThumbnail("Example Box Plot", TEST_THUMBNAIL);
        assertNewThumbnail("Example Box Plot");

        goToDataViews();
        setThumbnailSRC("Example Scatter Plot");
        toggleThumbnailType("Example Scatter Plot", true);
        assertNewThumbnail("Example Scatter Plot");
        assignCustomThumbnail("Example Scatter Plot", TEST_THUMBNAIL);
        assertNewThumbnail("Example Scatter Plot");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, "Study");
        importStudyFromZip(TEST_STUDY);
        goToDataViews();
    }

    protected void goToDataViews()
    {
        clickFolder(PROJECT_NAME);
        waitAndClickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT); // Lots of stuff on this page. Can take a while to load.
    }

    @LogMethod
    protected void toggleThumbnailType(String chart, boolean custom)
    {
        clickAndWait(Locator.linkWithText(chart));
        waitForElement(Locator.css("svg"));
        clickButton("Edit");
        waitForElement(Locator.css("svg"));
        waitForTextToDisappear("loading data...");
        clickButton("Save", 0);
        if(!custom)
        {
            waitAndClick(Locator.xpath("//input[@type='button' and ../label[text()='None']]"));
        }
        else
        {
            waitAndClick(Locator.xpath("//input[@type='button' and ../label[text()='Auto-generate']]"));
        }

        _extHelper.clickExtButton("", "Save", 0);
        // Timing is tight, don't step through this bit.
        _extHelper.waitForExtDialog("Saved");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void setThumbnailSRC(String chart)
    {
        waitForElement(Locator.linkWithText(chart));
        mouseOver(Locator.linkWithText(chart));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        try
        {
            THUMBNAIL_DATA = WebTestHelper.getHttpGetResponseBody(getAttribute(thumbnail, "src"));
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void assertNewThumbnail(String chart)
    {
        goToDataViews();
        waitForElement(Locator.xpath("//a[text()='"+chart+"']"));
        mouseOver(Locator.xpath("//a[text()='"+chart+"']"));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        String thumbnailData;
        try
        {
            thumbnailData = WebTestHelper.getHttpGetResponseBody(getAttribute(thumbnail, "src"));
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        Assert.assertFalse("Thumbnail was was still default", THUMBNAIL_DATA.equals(thumbnailData));
        THUMBNAIL_DATA = thumbnailData;
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void assignCustomThumbnail(String chart, File thumbnail)
    {
        goToDataViews();
        waitAndClick(Locator.xpath("//img[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.id("customThumbnail"));
        setFormElement(Locator.xpath("//input[@id='customThumbnail-fileInputEl']"), thumbnail);
        _extHelper.clickExtButton(chart, "Save", 0);
        waitForTextToDisappear("Saving...");
    }
}
