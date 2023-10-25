package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/*
    This page is linked from
 */
public class ElnSettingsPage extends LabKeyPage<ElnSettingsPage.ElementCache>
{
    public ElnSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ElnSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("notebook",  "configuration"));
        return new ElnSettingsPage(webDriverWrapper.getDriver());
    }

    public ElnSettingsPage selectCreatedByDateRowId()
    {
        elementCache().createdByDateRowIdRadioBtn.set(true);
        return this;
    }

    public ElnSettingsPage selectDateRowId()
    {
        elementCache().dateRowIdRadioBtn.set(true);
        return this;
    }

    public ElnSettingsPage selectRowId()
    {
        elementCache().rowIdRadioBtn.set(true);
        return this;
    }

    public String getSelectedPattern()
    {
        var selectedRadio = Locator.checkedRadioInGroup("nameExpression").findElement(getDriver());
        return selectedRadio.getAttribute("value");
    }

    public ShowAdminPage clickCancel()
    {
        clickAndWait(elementCache().cancelBtn);
        return new ShowAdminPage(getDriver());
    }

    public ShowAdminPage clickSave()
    {
        clickAndWait(elementCache().saveBtn);

        return new ShowAdminPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return super.elementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {

        RadioButton createdByDateRowIdRadioBtn = new RadioButton.RadioButtonFinder().withValue("CreatedByDateRowId")
                .findWhenNeeded(this);
        RadioButton dateRowIdRadioBtn = new RadioButton.RadioButtonFinder().withValue("DateRowId")
                .findWhenNeeded(this);
        RadioButton rowIdRadioBtn = new RadioButton.RadioButtonFinder().withValue("RowId")
                .findWhenNeeded(this);
        WebElement saveBtn = Locator.tagWithClass("a", "labkey-button").withText("Save")
                .findWhenNeeded(this);
        WebElement cancelBtn = Locator.tagWithClass("a", "labkey-button").withText("Cancel")
                .findWhenNeeded(this);
    }
}
