package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

public class ClosePage extends UpdatePage
{
    private Elements _elements;

    public ClosePage(WebDriver driver)
    {
        super(driver);
    }

    public static ClosePage beginAt(WebDriverWrapper driver, int issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ClosePage beginAt(WebDriverWrapper driver, String containerPath, int issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "close", Maps.of("issueId", String.valueOf(issueId))));
        return new ClosePage(driver.getDriver());
    }

    protected Elements elements()
    {
        return (Elements)super.elements();
    }

    @Override
    protected Elements newElements()
    {
        return new Elements();
    }

    protected class Elements extends UpdatePage.Elements
    {
        protected Elements()
        {
            assignedTo = ReadOnlyFormItem(getDriver()).withLabel("Assigned To").findWhenNeeded();
            status = ReadOnlyFormItem(getDriver()).withLabel("Status").findWhenNeeded();
        }
    }
}