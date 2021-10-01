package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FolderTypePage extends LabKeyPage<FolderTypePage.ElementCache>
{
    public FolderTypePage(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    public static FolderTypePage beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", "folderTypes"));
        return new FolderTypePage(wrapper.getDriver());
    }

    public static FolderTypePage beginAt(WebDriverWrapper wrapper, String containerPath)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", containerPath, "folderTypes"));
        return new FolderTypePage(wrapper.getDriver());
    }

    public FolderTypePage clickSave()
    {
        elementCache().saveBtn.click();
        return this;
    }

    public FolderTypePage clickCancel()
    {
        elementCache().cancelBtn.click();
        return this;
    }

    public String getDefaultFolder()
    {
        return elementCache().defaultBtn.withAttribute("checked").findElement(getDriver()).getAttribute("value");
    }

    public FolderTypePage setDefaultFolder(String value)
    {
        elementCache().defaultBtn.withAttribute("value", value).findElement(getDriver()).click();
        return this;
    }

    public FolderTypePage enableFolder(String value)
    {
        elementCache().enableBtn.withAttribute("name", value).findElement(getDriver()).click();
        return this;
    }

    public FolderTypePage disableFolder(String value)
    {
        if (isEnabled(value))
            elementCache().enableBtn.withAttribute("name", value).findElement(getDriver()).click();
        return this;
    }

    public boolean isEnabled(String value)
    {
        return elementCache().enableBtn.withAttribute("name", value).withAttribute("checked").findElements(getDriver()).size() > 0;
    }

    @Override
    protected FolderTypePage.ElementCache newElementCache()
    {
        return new FolderTypePage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator.XPathLocator defaultBtn = Locator.radioButtonByName("FolderTypeDefault");
        Locator.XPathLocator enableBtn = Locator.checkbox();

        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
