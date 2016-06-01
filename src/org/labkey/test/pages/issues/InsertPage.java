package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

public class InsertPage extends UpdatePage
{
    public InsertPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static InsertPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "insert"));
        return new InsertPage(driver.getDriver());
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
            status = ReadOnlyFormItem(getDriver()).withLabel("Status").findWhenNeeded();
        }
    }
}