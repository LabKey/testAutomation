/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class ManageDatasetQCStatesPage extends LabKeyPage<ManageDatasetQCStatesPage.ElementCache>
{
    public ManageDatasetQCStatesPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageDatasetQCStatesPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageDatasetQCStatesPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study", containerPath, "manageQCStates"));
        return new ManageDatasetQCStatesPage(driver.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitForText("Manage Dataset QC States");
    }

    public ManageDatasetQCStatesPage addStateRow(String state, String description, boolean publicData)
    {
        Locator.tagWithClass("span", "fa-plus-circle").waitForElement(elementCache().stateForm, 1000)
                .click();               // there is only ever one "add state" button; click it

        QCStateTableRow newRow = new QCStateTableRow(
                QCStateTableRow.Locators.lastRow.waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT), getDriver());

        newRow.setState(state)
                .setDescription(description)
                .setPublicData(publicData);

        return this;
    }

    public List<QCStateTableRow> getStateRows()
    {
        return QCStateTableRow.Locators.rowLoc.findElements(getDriver())
                .stream()
                .map(elem -> new QCStateTableRow(elem, getDriver()))
                .collect(Collectors.toList());
    }

    public QCStateTableRow getStateRow(String stateName)
    {
        return getStateRows().stream().filter(a-> a.getState().equals(stateName)).findFirst().orElse(null);
    }

    public ManageDatasetQCStatesPage setStatePublic(String id, boolean publicData)
    {
        new Checkbox(Locator.checkboxById(id).findElement(elementCache().stateForm)).set(publicData);
        return this;
    }

    public ManageDatasetQCStatesPage setDefaultPublishDataQCState(String state)
    {
        selectOptionByText(elementCache().defaultPublishDataQCState, state);
        return this;
    }

    public String getDefaultPublishDataQCState()
    {
        return getSelectedOptionText(elementCache().defaultPublishDataQCState);
    }

    public ManageDatasetQCStatesPage setDefaultDirectEntryQCState(String state)
    {
        selectOptionByText(elementCache().defaultDirectEntryQCStateLoc, state);
        return this;
    }

    public String getDefaultDirectEntryQCState()
    {
        return getSelectedOptionText(elementCache().defaultDirectEntryQCStateLoc);
    }

    public ManageDatasetQCStatesPage setDefaultPipelineQCState(String state)
    {
        selectOptionByText(elementCache().defaultPipelineQCStateLoc, state);
        return this;
    }

    public String getDefaultPipelineQCState()
    {
        return getSelectedOptionText(elementCache().defaultPipelineQCStateLoc);
    }

    public ManageDatasetQCStatesPage setDefaultVisibility(String state)  // will be either Public data or All data
    {
        selectOptionByText(elementCache().defaultVisibilityLoc, state);
        return this;
    }

    public String getDefaultVisibility()
    {
        return getSelectedOptionText(elementCache().defaultVisibilityLoc);
    }

    public ManageStudyPage clickSave()
    {
        clickButton("Save");
        return new ManageStudyPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement stateForm = Locator.tagWithAttribute("form", "name", "manageQCStates")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        Locator.XPathLocator defaultDirectEntryQCStateLoc = Locator.name("defaultDirectEntryQCState");
        Locator.XPathLocator defaultPipelineQCStateLoc = Locator.name("defaultPipelineQCState");

        Locator.XPathLocator defaultVisibilityLoc = Locator.name("showPrivateDataByDefault");
        Locator.XPathLocator defaultPublishDataQCState = Locator.name("defaultPublishDataQCState");
    }
}
