package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.property.EditDomainPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CreateSampleSetPage extends LabKeyPage<CreateSampleSetPage.ElementCache>
{
    public CreateSampleSetPage(WebDriver driver)
    {
        super(driver);
    }

    public static CreateSampleSetPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static CreateSampleSetPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "createSampleSet"));
        return new CreateSampleSetPage(driver.getDriver());
    }

    public CreateSampleSetPage setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public CreateSampleSetPage setDescription(String desc)
    {
        elementCache().descriptionInput.set(desc);
        return this;
    }

    public CreateSampleSetPage setNameExpression(String nameExp)
    {
        elementCache().nameExpressionInput.set(nameExp);
        return this;
    }

    public CreateSampleSetPage addParentColumnAlias(String importHeader, String materialInputName)
    {
        int countOfInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size();

        click(Locator.linkWithText("add parent column import alias"));
        waitFor(()-> Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size() > countOfInputs, 1000);

        List<WebElement> importAliasInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver());
        List<WebElement> importAliasSelects = Locator.tagWithName("select", "importAliasValues").findElements(getDriver());

        int index = importAliasInputs.size() - 1;
        WebElement aliasInput = importAliasInputs.get(index);
        WebElement aliasSelect = importAliasSelects.get(index);

        setFormElement(aliasInput, importHeader);
        selectOptionByTextContaining(aliasSelect, materialInputName);

        return this;
    }

    public EditDomainPage clickCreate()
    {
        clickAndWait(elementCache().createButton);
        return new EditDomainPage(getDriver());
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input nameInput = Input.Input(Locator.tagWithName("input","name"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input nameExpressionInput = Input.Input(Locator.tagWithName("input","nameExpression"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input descriptionInput = Input.Input(Locator.tagWithName("input","description"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);

        WebElement createButton = Locator.lkButton("Create").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
