package org.labkey.test.pages.wiki;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageWikiConfigurationPage extends LabKeyPage<ManageWikiConfigurationPage.ElementCache>
{
    public ManageWikiConfigurationPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected ManageWikiConfigurationPage.ElementCache newElementCache()
    {
        return new ManageWikiConfigurationPage.ElementCache();
    }

    public String getNameInput()
    {
        return elementCache().nameInput.get();
    }

    public ManageWikiConfigurationPage setNameInput(String value)
    {
        elementCache().nameInput.set(value);
        return this;
    }

    public ManageWikiConfigurationPage rename(String newName, boolean addAlias)
    {
        elementCache().renameButton.click();
        waitFor(() -> elementCache().addAlias.isDisplayed(), WAIT_FOR_PAGE);
        elementCache().newNameInput.set(newName);
        elementCache().addAlias.set(addAlias);

        return this;
    }

    public ManageWikiConfigurationPage setAliases(String value)
    {
        elementCache().aliases.sendKeys(value);
        return this;
    }

    public String getAliases()
    {
        return elementCache().aliases.getText();
    }

    public ManageWikiConfigurationPage save()
    {
        elementCache().saveButton.click();
        return this;
    }

    public String saveExpectingErrors()
    {
        elementCache().saveButton.click();
        return elementCache().errorMsg.getText();
    }

    public EditPage editContent()
    {
        elementCache().editContentButton.click();
        return new EditPage(getDriver());
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input nameInput = Input.Input(Locator.name("name"), getDriver()).findWhenNeeded(this);
        WebElement renameButton = Locator.lkButton("Rename").findWhenNeeded(this);
        Input newNameInput = Input.Input(Locator.name("newName"), getDriver()).findWhenNeeded(this);
        Checkbox addAlias = Checkbox.Checkbox(Locator.checkboxByName("addAlias")).findWhenNeeded(this);

        Input titleInput = Input.Input(Locator.name("title"), getDriver()).findWhenNeeded(this);
        Checkbox shouldIndexCheckbox = Checkbox.Checkbox(Locator.checkboxByName("shouldIndex")).findWhenNeeded(this);

        WebElement siblingOrder = Locator.name("siblings").findWhenNeeded(this);
        WebElement moveUpButton = Locator.lkButton("Move Up").findWhenNeeded(this);
        WebElement moveDownButton = Locator.lkButton("Move Down").findWhenNeeded(this);

        WebElement aliases = Locator.textarea("aliases").findWhenNeeded(this);

        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement deleteButton = Locator.lkButton("Delete").findWhenNeeded(this);
        WebElement editContentButton = Locator.lkButton("Edit Content").findWhenNeeded(this);
        WebElement errorMsg = Locator.tagWithClass("span", "labkey-error").findWhenNeeded(this);
    }

}
