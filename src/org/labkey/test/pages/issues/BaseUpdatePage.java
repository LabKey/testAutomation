package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.Select;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class BaseUpdatePage<EC extends BaseUpdatePage.ElementCache> extends BaseIssuePage<EC>
{
    public BaseUpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public Input title()
    {
        return elementCache().titleInput;
    }

    public Input comment()
    {
        return elementCache().commentInput;
    }

    /**
     * Find any type of field with the given name. Sufficient for simple get/set
     */
    public FormItem fieldWithName(String fieldName)
    {
        return elementCache().formItemNamed(fieldName);
    }

    /**
     * Find a named field you know to be a <select>
     */
    public Select selectWithName(String fieldName)
    {
        return elementCache().getSelect(fieldName);
    }

    @Override
    public Select related()
    {
        return (Select) super.related();
    }

    @Override
    public Select priority()
    {
        return (Select) super.priority();
    }

    public EmailPrefsPage clickEmailPrefs()
    {
        clickAndWait(elementCache().emailPrefsLink);
        return new EmailPrefsPage(getDriver());
    }

    public abstract LabKeyPage save();

    public UpdatePage saveFail()
    {
        clickAndWait(elementCache().saveButton);
        return new UpdatePage(getDriver());
    }

    public DetailsPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new DetailsPage(getDriver());
    }

    public DetailsPage cancelDirty()
    {
        doAndWaitForPageToLoad(() ->
        {
            elementCache().cancelButton.click();
            assertAlertContains("Confirm Navigation");
        });
        return new DetailsPage(getDriver());
    }

    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    protected class ElementCache extends BaseIssuePage.ElementCache
    {
        public ElementCache()
        {
            assignedTo = getSelect("assignedTo");
            priority = getSelect("priority");
            related = getInput("related");
            notifyList = getInput("notifyList");
        }

        protected Input titleInput = getInput("title");
        protected Input commentInput = getInput("comment");

        protected WebElement emailPrefsLink = Locator.linkWithText("email prefs").findWhenNeeded(this);
        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        protected WebElement bottomSaveButton = Locator.lkButton("Save").index(1).findWhenNeeded(this);
        protected WebElement bottomCancelButton = Locator.lkButton("Cancel").index(1).findWhenNeeded(this);
    }
}
