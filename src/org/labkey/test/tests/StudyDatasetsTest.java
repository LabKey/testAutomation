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
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

public class StudyDatasetsTest extends StudyBaseTest
{
    private static final String CATEGORY1 = "Category1";
    private static final String GROUP1A = "Group1A";
    private static final String GROUP1B = "Group1B";
    private static final String CATEGORY2 = "Category2";
    private static final String GROUP2A = "Group2A";
    private static final String GROUP2B = "Group2B";
    private static final String EXTRA_GROUP = "Extra Group";
    private static final String[] PTIDS = {"999320016","999320518","999320529","999320533","999320557","999320565"};

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        importStudy();
        // wait for study (but not specimens) to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);

        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP1A, "Mouse", CATEGORY1, true, null, PTIDS[0], PTIDS[1]);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP1B, "Mouse", CATEGORY1, false, null, PTIDS[2], PTIDS[3]);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP2A, "Mouse", CATEGORY2, true, null, PTIDS[1], PTIDS[3]);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP2B, "Mouse", CATEGORY2, false, null, PTIDS[2], PTIDS[4]);
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        createDataset("A");
        renameDataset("A", "Original A");
        createDataset("A");
        deleteFields("A");

        checkFieldsPresent("Original A", "YTest", "ZTest");

        verifySideFilter();
    }

    protected void createDataset(String name)
    {
        goToManageDatasets();

        waitForText("Create New Dataset");
        click(Locator.xpath("//a[text()='Create New Dataset']"));
        waitForElement(Locator.xpath("//input[@name='typeName']"));
        setFormElement(Locator.xpath("//input[@name='typeName']"), name);
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='name0-input']"));
        assertTextNotPresent("XTest");
        setFormElement(Locator.xpath("//input[@id='name0-input']"), "XTest");
        clickButtonContainingText("Add Field", 0);
        waitForElement(Locator.xpath("//input[@id='name1-input']"));
        assertTextNotPresent("YTest");
        setFormElement(Locator.xpath("//input[@id='name1-input']"), "YTest");
        clickButtonContainingText("Add Field", 0);
        waitForElement(Locator.xpath("//input[@id='name2-input']"));
        assertTextNotPresent("ZTest");
        setFormElement(Locator.xpath("//input[@id='name2-input']"), "ZTest");
        clickButton("Save");
 }

    protected void renameDataset(String orgName, String newName)
    {
        goToManageDatasets();

        waitForElement(Locator.xpath("//a[text()='" + orgName + "']"));
        click(Locator.xpath("//a[text()='" + orgName + "']"));
        waitForText("Edit Definition");
        clickButton("Edit Definition");

        waitForElement(Locator.xpath("//input[@name='dsName']"));
        setFormElement(Locator.xpath("//input[@name='dsName']"), newName);
        setFormElement(Locator.xpath("//input[@name='dsLabel']"), newName);

        assertTextPresent("XTest");
        assertTextPresent("YTest");
        assertTextPresent("ZTest");
        clickButton("Save");
}

    protected void deleteFields(String name)
    {
        goToManageDatasets();

        waitForElement(Locator.xpath("//a[text()='" + name + "']"));
        click(Locator.xpath("//a[text()='" + name + "']"));
        waitForText("Edit Definition");
        clickButton("Edit Definition");

        waitForElement(Locator.xpath("//div[@id='partdelete_2']"));
        mouseClick(Locator.id("partdelete_2").toString());
        clickButtonContainingText("OK", 0);
        waitForElement(Locator.xpath("//div[@id='partdelete_1']"));
        mouseClick(Locator.id("partdelete_1").toString());

        assertTextPresent("XTest");
        assertElementNotPresent(Locator.xpath("//input[@id='name1-input']"));
        assertElementNotPresent(Locator.xpath("//input[@id='name2-input']"));
        clickButton("Save");
}

    protected void checkFieldsPresent(String name, String... items)
    {
        goToManageDatasets();

        waitForElement(Locator.xpath("//a[text()='" + name + "']"));
        click(Locator.xpath("//a[text()='" + name + "']"));
        waitForText("Edit Definition");
        clickButton("Edit Definition");

        for(String item : items)
        {
            waitForText(item);
        }
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifySideFilter()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("DEM-1: Demographics"));
        DataRegionTable dataregion = new DataRegionTable("Dataset", this);
        verifyFilterPanelOnDemographics(dataregion);

        _studyHelper.deleteCustomParticipantGroup(EXTRA_GROUP, "Mouse");

        clickProject(getProjectName());
        clickFolder(getFolderName());
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addQueryWebPart("Demographics", "study", "DEM-1: Demographics", null);
        dataregion = new DataRegionTable("qwp6", this);
        verifyFilterPanelOnDemographics(dataregion);
    }

    private void verifyFilterPanelOnDemographics(DataRegionTable dataset)
    {
        dataset.openSideFilterPanel();

        waitForElement(Locator.paginationText(24));
        dataset.clickFacetLabel(CATEGORY1, GROUP1A); // Select only GROUP1A
        waitForElementToDisappear(Locator.css(".labkey-pagination"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.linkWithText(PTIDS[0]));
        assertElementPresent(Locator.linkWithText(PTIDS[1]));
        Assert.assertEquals("Wrong number of rows after filter", 2, dataset.getDataRowCount());

        dataset.clickFacetCheckbox(CATEGORY1, GROUP1B); // GROUP1A OR GROU1B
        waitForElement(Locator.linkWithText(PTIDS[2]));
        assertElementPresent(Locator.linkWithText(PTIDS[0]));
        assertElementPresent(Locator.linkWithText(PTIDS[1]));
        assertElementPresent(Locator.linkWithText(PTIDS[3]));
        Assert.assertEquals("Wrong number of rows after filter", 4, dataset.getDataRowCount());

        dataset.clickFacetLabel(CATEGORY2, GROUP2A); // (GROUP1A OR GROU1B) AND GROUP2A
        waitForElementToDisappear(Locator.linkWithText(PTIDS[2]));
        waitForElement(Locator.linkWithText(PTIDS[1]));
        assertElementPresent(Locator.linkWithText(PTIDS[3]));
        Assert.assertEquals("Wrong number of rows after filter", 2, dataset.getDataRowCount());

        dataset.clickFacetLabel(CATEGORY2, "Not in any group"); // (GROUP1A OR GROUP1B) AND (CATEGORY2 = NULL)
        waitForElementToDisappear(Locator.linkWithText(PTIDS[1]));
        waitForElement(Locator.linkWithText(PTIDS[0]));
        Assert.assertEquals("Wrong number of rows after filter", 1, dataset.getDataRowCount());

        dataset.clickFacetCheckbox(CATEGORY2); // (GROUP1A OR GROUP1B)
        waitForElement(Locator.linkWithText(PTIDS[2]));
        assertElementPresent(Locator.linkWithText(PTIDS[0]));
        assertElementPresent(Locator.linkWithText(PTIDS[1]));
        assertElementPresent(Locator.linkWithText(PTIDS[3]));
        Assert.assertEquals("Wrong number of rows after filter", 4, dataset.getDataRowCount());

        dataset.clickFacetCheckbox("Cohorts", "Group 1"); // (GROUP1A OR GROUP1B) AND (NOT(COHORT 1))
        waitForElementToDisappear(Locator.linkWithText(PTIDS[0]));
        waitForElement(Locator.linkWithText(PTIDS[1]));
        assertElementPresent(Locator.linkWithText(PTIDS[2]));
        Assert.assertEquals("Wrong number of rows after filter", 2, dataset.getDataRowCount());

        dataset.toggleAllFacetsCheckbox();
        waitForElement(Locator.linkWithText(PTIDS[5]));
        Assert.assertEquals("Wrong number of rows after filter", 24, dataset.getDataRowCount());

        _extHelper.clickMenuButton(false, "Mouse Groups", "Create Mouse Group", "From All Mice");
        _extHelper.waitForExtDialog("Define Mouse Group");
        setFormElement(Locator.id("groupLabel-inputEl"), EXTRA_GROUP);
        _extHelper.clickExtButton("Define Mouse Group", "Save", 0);
        waitForElement(DataRegionTable.Locators.facetRow(EXTRA_GROUP, EXTRA_GROUP));
    }

    public void goToManageDatasets()
    {
        goToManageStudy();
        waitForText("Manage Datasets");
        click(Locator.xpath("//a[text()='Manage Datasets']"));
    }

}
