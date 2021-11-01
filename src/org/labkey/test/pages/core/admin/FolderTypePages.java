package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FolderTypePages extends LabKeyPage<FolderTypePages.ElementCache>
{
    public FolderTypePages(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    public static FolderTypePages beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", "folderTypes"));
        return new FolderTypePages(wrapper.getDriver());
    }

    public static FolderTypePages beginAt(WebDriverWrapper wrapper, String containerPath)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", containerPath, "folderTypes"));
        return new FolderTypePages(wrapper.getDriver());
    }

    public FolderTypePages clickSave()
    {
        elementCache().saveBtn.click();
        return this;
    }

    public FolderTypePages clickCancel()
    {
        elementCache().cancelBtn.click();
        return this;
    }

    public String getDefaultFolderType()
    {
        return elementCache().defaultTypeRadio.withAttribute("checked").findElement(getDriver()).getAttribute("value");
    }

    public FolderTypePages setDefaultFolderType(String value)
    {
        elementCache().defaultTypeRadio.withAttribute("value", value).findElement(getDriver()).click();
        return this;
    }

    public FolderTypePages enableFolderType(String name)
    {
        findFolderTypeCheckbox(name).check();
        return this;
    }

    public FolderTypePages disableFolderType(String name)
    {
        findFolderTypeCheckbox(name).uncheck();
        return this;
    }

    private Checkbox findFolderTypeCheckbox(String name)
    {
        return Checkbox.Checkbox(Locator.checkboxByName(name)).find(getDriver());
    }

    public boolean isEnabled(String value)
    {
        return elementCache().enableTypeCheckbox.withAttribute("name", value).withAttribute("checked").findElements(getDriver()).size() > 0;
    }

    @Override
    protected FolderTypePages.ElementCache newElementCache()
    {
        return new FolderTypePages.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator.XPathLocator defaultTypeRadio = Locator.radioButtonByName("FolderTypeDefault");
        Locator.XPathLocator enableTypeCheckbox = Locator.checkbox();

        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
