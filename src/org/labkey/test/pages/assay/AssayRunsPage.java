/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.assay;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentRunTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class AssayRunsPage extends LabKeyPage<AssayRunsPage.ElementCache>
{
    public AssayRunsPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayRunsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AssayRunsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayRuns"));
        return new AssayRunsPage(driver.getDriver());
    }

    public static AssayRunsPage beginAt(WebDriverWrapper driver, String containerPath, Integer protocolId)
    {
        driver.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayRuns", Map.of("rowId", protocolId)));
        return new AssayRunsPage(driver.getDriver());
    }

    public DataRegionTable getTable()
    {
        return new ExperimentRunTable("Runs", this);
    }

    public AssayDataPage clickAssayIdLink(String assayId)
    {
        int rowIndex = getTable().getRowIndex("Assay ID", assayId);
        WebElement cell =  getTable().findCell(rowIndex, "Assay ID");
        WebElement link = Locator.linkWithText(assayId).waitForElement(cell, WAIT_FOR_JAVASCRIPT);
        clickAndWait(link);
        return new AssayDataPage(getDriver());
    }

    public AssayDataPage clickViewResults()
    {
        clickAndWait(Locator.linkWithText("view results"));
        return new AssayDataPage(getDriver());
    }

    public UpdateQCStatePage updateSelectedQcStatus()
    {
        getTable().clickHeaderMenu("QC State", "Update state of selected rows");
        return new UpdateQCStatePage(getDriver());
    }

    public ManageAssayQCStatesPage manageQCStates()
    {
        getTable().clickHeaderMenu("QC State", "Manage states");
        return new ManageAssayQCStatesPage(getDriver());
    }

    public AssayRunsPage setRowQcStatus(String state, String comment, boolean expectError, int... rowIndices)
    {
        for (int rowIndex : rowIndices)
        {
            getTable().checkCheckbox(rowIndex);
        }
        UpdateQCStatePage updatePage = updateSelectedQcStatus()
                .selectState(state)
                .setComment(comment);

        if (expectError)
        {
            clickButton("update", 0);
            ModalDialog dialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Error").find();
            Assert.assertEquals("A comment is required when changing a QC State for the selected run(s).", dialog.getBodyText());
            dialog.dismiss();
            return updatePage.clickCancel();
        }

        return updatePage.clickUpdate();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

    }
}
