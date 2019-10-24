package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;


public class CreateSampleSetPage extends LabKeyPage<CreateSampleSetPage.ElementCache>
{
    public final static String CURRENT_SAMPLE_SET_OPTION_TEXT = "(Current Sample Set)";
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

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    public CreateSampleSetPage setDescription(String desc)
    {
        elementCache().descriptionInput.set(desc);
        return this;
    }

    public String getDescrption()
    {
        return elementCache().descriptionInput.get();
    }

    public CreateSampleSetPage setNameExpression(String nameExp)
    {
        elementCache().nameExpressionInput.set(nameExp);
        return this;
    }

    public String getNameExpression()
    {
        return elementCache().nameExpressionInput.get();
    }

    public CreateSampleSetPage addParentColumnAlias(int index, String importHeader, String materialInputName)
    {
        int countOfInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size();

        click(Locator.linkWithText("add parent column import alias"));
        waitFor(()-> Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size() > countOfInputs, 1000);

        Input aliasInput = elementCache().parentAlias(index);
        WebElement aliasSelect = elementCache().parentAliasSelect(index);

        aliasInput.setValue(importHeader);
        selectOptionByTextContaining(aliasSelect, materialInputName);

        return this;
    }

    public String getParentAlias(int index)
    {
        return elementCache().parentAlias(index).getValue();
    }

    public String getParentAliasSelectText(int index)
    {
        return new Select(elementCache().parentAliasSelect(index)).getFirstSelectedOption().getText();
    }

    public DomainFormPanel clickCreate(boolean createFailureExpected)
    {
        clickAndWait(elementCache().createButton);
        return createFailureExpected ? null : new DomainFormPanel.DomainFormPanelFinder(getDriver()).waitFor();
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
        Input nameInput = Input.Input(Locator.name("name"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input nameExpressionInput = Input.Input(Locator.name("nameExpression"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input descriptionInput = Input.Input(Locator.name("description"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);

        WebElement createButton = Locator.lkButton("Create").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);

        protected Input parentAlias(int index)
        {
            return Input.Input(Locator.name("importAliasKeys"), getDriver()).findAll(this).get(index);
        }

        protected WebElement parentAliasSelect(int index)
        {
            return Locator.tagWithName("select", "importAliasValues").findElements(getDriver()).get(index);
        }
    }
}
