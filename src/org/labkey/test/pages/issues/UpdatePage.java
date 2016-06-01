package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.labkey.TextFormItem;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.labkey.SelectFormItem.SelectFormItem;

public class UpdatePage extends BaseIssuePage
{
    private Elements _elements;

    public UpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public Input title()
    {
        return elements().titleInput;
    }

    public Input comment()
    {
        return elements().commentInput;
    }

    public LabKeyPage clickEmailPrefs()
    {
        clickAndWait(elements().emailPrefsLink);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage save()
    {
        clickAndWait(elements().saveButton);
        return new LabKeyPage(getDriver());
    }

    public UpdatePage saveFail()
    {
        clickAndWait(elements().saveButton);
        return new UpdatePage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elements().cancelButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancelDirty()
    {
        doAndWaitForPageToLoad(() ->
        {
            elements().cancelButton.click();
            assertAlertContains("Confirm Navigation");
        });
        return new LabKeyPage(getDriver());
    }

    protected Elements elements()
    {
        if (_elements == null)
            _elements = newElements();
        return _elements;
    }

    protected Elements newElements()
    {
        return new Elements();
    }

    protected class Elements extends BaseIssuePage.Elements
    {
        public Elements()
        {
            status = SelectFormItem(getDriver()).withName("status").findWhenNeeded();
            assignedTo = SelectFormItem(getDriver()).withName("assignedTo").findWhenNeeded();
            priority = SelectFormItem(getDriver()).withName("priority").findWhenNeeded();
            related = TextFormItem.TextFormItem(getDriver()).withName("related").findWhenNeeded();
            notifyList = TextFormItem.TextFormItem(getDriver()).withName("notifyList").findWhenNeeded();
        }

        protected Input titleInput = new Input(Locator.name("title").findWhenNeeded(this), getDriver());
        protected Input commentInput = new Input(Locator.name("comment").findWhenNeeded(this), getDriver());

        protected WebElement emailPrefsLink = Locator.linkWithText("email prefs").findWhenNeeded(this);
        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        protected WebElement bottomSaveButton = Locator.lkButton("Save").index(1).findWhenNeeded(this);
        protected WebElement bottomCancelButton = Locator.lkButton("Cancel").index(1).findWhenNeeded(this);
    }
}
