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

import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.CustomModules;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.ExtHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PerlHelperWD;
import org.labkey.test.util.PipelineHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.RReportHelperWD;

import java.io.File;
import java.io.IOException;

/**
 * User: tchadick
 * Date: 2/18/13
 * Time: 2:50 PM
 */
@Category({CustomModules.class})
public class RISAssayTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private final File risXarFile = new File(getDownloadDir(), "ris.xar");
    private final File risListArchive = new File(getDownloadDir(), "ris-lists.zip");
    private final File risTransformScript = new File(getDownloadDir(), "kiem_transform.pl");
    private final File risAssayData = new File(getSampledataPath(), "kiem/RIS test.xls");
    private final File risAssayData2 = new File(getSampledataPath(), "kiem/RIS_SEQ_000_000_000_000_024.txt");
    
    private final String ASSAY_NAME = "RIS"; // Defined by risXarFile
    private final String ASSAY_ID = risAssayData.getName();

    private final int ASSAY_ROW_COUNT = 719;
    private final int UNIQUEBLATT_ROW_COUNT = 34;

    @Override
    protected String getProjectName()
    {
        return "RISAssayTest Project";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        risXarFile.delete();
        risListArchive.delete();
        risTransformScript.delete();
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProject();

        createRISAssay();

        verifyRISAssay();

        configureReportWebPart();
        verifyRISReport();

        verifyRISWobbleQC();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProject()
    {

        PerlHelperWD perlHelper = new PerlHelperWD(this);
        perlHelper.ensurePerlConfig();

        _containerHelper.createProject(getProjectName(), "Assay");
        enableModule(getProjectName(), "kiem");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Kiem RIS Dashboard");
        portalHelper.addWebPart("Kiem RIS Report");

        portalHelper.addWebPart("Lists");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void createRISAssay()
    {
        click(Locator.linkWithText("Download Assay Design"));
        click(Locator.linkWithText("Download List Archive"));
        click(Locator.linkWithText("Download Transform Script"));

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return risXarFile.exists();
            }
        }, "failed to download RIS Assay design", WAIT_FOR_JAVASCRIPT);
        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return risListArchive.exists();
            }
        }, "failed to download RIS list archive", WAIT_FOR_JAVASCRIPT);
        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return risTransformScript.exists();
            }
        }, "failed to download RIS Assay transform script", WAIT_FOR_JAVASCRIPT);

        _listHelper.importListArchive(getProjectName(), risListArchive);
        assertElementPresent(Locator.css("#lists tr"), 7);

        PipelineHelper pipelineHelper = new PipelineHelper(this);

        goToModule("FileContent");
        pipelineHelper.uploadFile(risXarFile);
        selectPipelineFileAndImportAction(risXarFile.getName(), "Import Experiment");

        waitForElementWithRefresh(Locator.linkWithText(ASSAY_NAME), WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        _assayHelper.clickEditAssayDesign();
        _assayHelper.setTransformScript(risTransformScript);
        _assayHelper.saveAssayDesign();

    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRISAssay() throws IOException, CommandException
    {
        _assayHelper.importAssay(ASSAY_NAME, risAssayData, getProjectName());

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        log("Add link to Uniqueblatts view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Uniqueblatts");
        _customizeViewsHelper.saveCustomView();

        log("Check transformed RIS data");
        clickAndWait(Locator.linkWithText(ASSAY_ID));
        waitForElement(Locator.paginationText(1, 100, ASSAY_ROW_COUNT));

        log("Check uniqueblatts RIS data");
        clickAndWait(Locator.linkWithText(ASSAY_NAME + " Runs"));
        clickAndWait(Locator.linkWithText(ASSAY_ID).index(1));
        waitForElement(Locator.paginationText(1, UNIQUEBLATT_ROW_COUNT, UNIQUEBLATT_ROW_COUNT));

        goToSchemaBrowser();
        viewQueryData("assay.General.RIS", "uniqueblatts");
        waitForElement(Locator.paginationText(1, UNIQUEBLATT_ROW_COUNT, UNIQUEBLATT_ROW_COUNT));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRISWobbleQC() throws IOException, CommandException
    {
        clickProject(getProjectName());
        _assayHelper.importAssay(ASSAY_NAME, risAssayData2, getProjectName());

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Kiem RIS Wobble QC");

        Locator.XPathLocator qcWebpart = Locator.id("ris-wobble-qc-div");
        Locator.XPathLocator thresholdCell = Locator.xpath("//span[contains(@style, 'color:#ff0000;')]"); // Threshold rows should have red text
        Locator.XPathLocator editedCell =  Locator.xpath("//span[contains(@style, 'background-color:yellow;')]"); // Edited rows should have yellow background

        waitAndClick(qcWebpart.append("//tr").withText(risAssayData2.getName()));
        clickButton("Search Run Data", 0);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.css(".x4-toolbar-text").withText("Displaying 1 - 26 of 26")); // 23 threshold rows, plus possible alternate CStart Grouping rows
        assertElementPresent(qcWebpart.append(thresholdCell), 23);

        log("Edit CStart grouping");
        _ext4Helper.checkGridRowCheckbox("27756455"); // CStart value for member of CStart Group 2775646
        _ext4Helper.checkGridRowCheckbox("41341764"); // CStart value for member of CStart Group 4134176 (non-threshold row)
        clickButton("Edit Grouping", 0);
        waitAndClick(Ext4HelperWD.Locators.window("Edit Grouping for Selections").append("//td").withText("2775645"));
        _extHelper.clickExtButton("Edit Grouping for Selections", "Save", 0);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForElement(editedCell);
        assertElementPresent(qcWebpart.append(editedCell), 2);
        assertElementNotPresent(qcWebpart.append("//tr").withPredicate(thresholdCell).withPredicate(editedCell));

        log("Reset CStart Grouping");
        _ext4Helper.checkGridRowCheckbox("27756455");
        clickButton("Reset Grouping", 0);
        _extHelper.waitForExtDialog("Reset CStart Grouping");
        _extHelper.clickExtButton("Reset CStart Grouping", "OK", 0);
        waitForElementToDisappear(qcWebpart.append(editedCell).index(1));
        assertElementPresent(qcWebpart.append(editedCell), 1);
        assertElementNotPresent(qcWebpart.append("//tr").withPredicate(thresholdCell).withPredicate(editedCell));
        _ext4Helper.checkGridRowCheckbox("41341764");
        clickButton("Reset Grouping", 0);
        _extHelper.waitForExtDialog("Reset CStart Grouping");
        _extHelper.clickExtButton("Reset CStart Grouping", "OK", 0);
        waitForElementToDisappear(qcWebpart.append(editedCell));

        log("Try increased threshold");
        Locator threshold = Locator.name("threshold");
        setFormElement(threshold, "1000");
        fireEvent(threshold, SeleniumEvent.blur);
        clickButton("Search Run Data", 0);
        waitForElement(Locator.css(".x4-toolbar-text").withText("Displaying 1 - 69 of 69"));

        log("Show all data");
        click(Locator.xpath("//input").withPredicate(Locator.xpath("../label").withText("Show all data")));
        clickButton("Search Run Data", 0);
        waitForElement(Locator.css(".x4-toolbar-text").withText("Displaying 1 - 500 of 2433"));

        portalHelper.removeWebPart("Kiem RIS Wobble QC");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRISReport()
    {
        RReportHelperWD rHelper = new RReportHelperWD(this);
        rHelper.ensureRConfig();

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("RIS Report Tool"));

        waitForElement(Locator.linkWithText(ASSAY_ID));
        click(ExtHelperWD.Locators.checkerForGridRowContainingText(ASSAY_ID));
        clickButton("Create Bar Chart", 0);
        waitForElement(Locator.linkWithText("Kiem Chart"));
        waitForElement(Locator.xpath("//img[starts-with(@id, 'resultImage')]"));

        clickAndWait(Locator.linkWithText("Kiem Chart"));
        waitForElement(Locator.xpath("//div[starts-with(@id, 'viewDiv_')]").containing("Please run this report from the RIS report webpart."));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void configureReportWebPart()
    {
        clickProject(getProjectName());

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.clickWebpartMenuItem("Kiem RIS Report", true, "Customize");
        click(Locator.tagContainingText("label", "RIS"));
        clickButton("Save");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/kiem";
    }
}
