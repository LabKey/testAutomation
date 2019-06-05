package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
                .click();               // there is only ever on
        Locator lastLabelLoc = Locator.xpath("(//input[@name='newLabels'])[last()]");
        Locator lastDescLoc = Locator.xpath("(//input[@name='newDescriptions'])[last()]");
        Locator lastCheckbox = Locator.xpath("(//input[@type='checkbox' and @name='newPublicData'])[last()]");

        setFormElement(lastLabelLoc, state);
        setFormElement(lastDescLoc, description);
        new Checkbox(lastCheckbox.findElement(elementCache().stateForm)).set(publicData);

        return this;
    }

    public ManageDatasetQCStatesPage setStatePublic(String id, boolean publicData)
    {
        new Checkbox(Locator.checkboxById(id).findElement(elementCache().stateForm)).set(publicData);
        return this;
    }

    public ManageDatasetQCStatesPage setDefaultAssayQCState(String state)
    {
        selectOptionByText(Locator.name("defaultAssayQCState"), state);
        return this;
    }

    public ManageDatasetQCStatesPage setDefaultDirectEntryQCState(String state)
    {
        selectOptionByText(Locator.name("defaultDirectEntryQCState"), state);
        return this;
    }

    public ManageDatasetQCStatesPage setDefaultPipelineQCState(String state)
    {
        selectOptionByText(Locator.name("defaultPipelineQCState"), state);
        return this;
    }

    public ManageDatasetQCStatesPage showPrivateDataByDefault(String state)
    {
        selectOptionByText(Locator.name("showPrivateDataByDefault"), state);
        return this;
    }

    public ManageStudyPage clickSave()
    {
        clickButton("Save");
        return new ManageStudyPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement stateForm = Locator.tagWithAttribute("form", "name", "manageQCStates")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}
