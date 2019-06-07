package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.study.QCStateTableRow;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class ManageAssayQCStatesPage extends LabKeyPage<ManageAssayQCStatesPage.ElementCache>
{
    public ManageAssayQCStatesPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageAssayQCStatesPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageAssayQCStatesPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new ManageAssayQCStatesPage(driver.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitForText("Manage Assay QC States");
    }


    public ManageAssayQCStatesPage addStateRow(String state, String description, boolean publicData)
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

    public ManageAssayQCStatesPage setDefaultQCState(String state)
    {
        selectOptionByText(elementCache().defaultQCState, state);
        return this;
    }

    public String getDefaultQCState()
    {
        return getSelectedOptionText(elementCache().defaultQCState);
    }

    public void clickSave()
    {
        clickButton("Save");
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement stateForm = Locator.tagWithAttribute("form", "name", "manageQCStates")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        Locator.XPathLocator defaultQCState = Locator.name("defaultQCState");
    }
}
