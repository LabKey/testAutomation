package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

public class ReopenPage extends UpdatePage
{
    private Elements _elements;

    public ReopenPage(WebDriver driver)
    {
        super(driver);
    }

    public static ReopenPage beginAt(WebDriverWrapper driver, int issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ReopenPage beginAt(WebDriverWrapper driver, String containerPath, int issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "repoen", Maps.of("issueId", String.valueOf(issueId))));
        return new ReopenPage(driver.getDriver());
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