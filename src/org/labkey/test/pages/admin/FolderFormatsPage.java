package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FolderFormatsPage extends BaseSettingsPage
{

    public FolderFormatsPage(WebDriver driver)
    {
        super(driver);
    }

    public static FolderFormatsPage beginAt(WebDriverWrapper wrapper)
    {
        return beginAt(wrapper, wrapper.getCurrentContainerPath());
    }

    public static FolderFormatsPage beginAt(WebDriverWrapper wrapper, String containerPath)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", containerPath, "folderSettings"));
        return new FolderFormatsPage(wrapper.getDriver());
    }

    private Boolean getInherited(String name)
    {
        return elementCache().inheritedChk(name).isSelected();
    }

    private void setInherited(String name, boolean enable)
    {
        if (enable)
            checkCheckbox(elementCache().inheritedChk(name));
        else
            uncheckCheckbox(elementCache().inheritedChk(name));
    }

    public boolean getDefaultDateDisplayInherited()
    {
        return getInherited("defaultDateFormatInherited");
    }

    public void setDefaultDateDisplayInherited(boolean enable)
    {
        setInherited("defaultDateFormatInherited", enable);
    }

    public boolean getDefaultTimeDisplayInherited()
    {
        return getInherited("defaultTimeFormatInherited");
    }

    public void setDefaultTimeDisplayInherited(boolean enable)
    {
        setInherited("defaultTimeFormatInherited", enable);
    }

    public boolean getDefaultDateTimeDisplayInherited()
    {
        return getInherited("defaultDateTimeFormatInherited");
    }

    public void setDefaultDateTimeDisplayInherited(boolean enable)
    {
        setInherited("defaultDateTimeFormatInherited", enable);
    }

    public boolean getDefaultNumberDisplayInherited()
    {
        return getInherited("defaultNumberFormatInherited");
    }

    public void setDefaultNumberDisplayInherited(boolean enable)
    {
        setInherited("defaultNumberFormatInherited", enable);
    }

    public boolean getRestrictChartingColsInherited()
    {
        return getInherited("restrictedColumnsEnabledInherited");
    }

    public void setRestrictChartingColsInherited(boolean enable)
    {
        setInherited("restrictedColumnsEnabledInherited", enable);
    }

    public FolderFormatsPage clickSave()
    {
        super.save();
        clearCache();
        return this;
    }

    public FolderFormatsPage clickInheritAll()
    {
        clickAndWait(elementCache().inheritAll);
        clearCache();
        return this;
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseSettingsPage.ElementCache
    {
        WebElement inheritedChk(String name)
        {
            return Locator.checkboxByName(name).findWhenNeeded(this);
        }

        WebElement inheritAll = Locator.lkButton("Inherit All").findWhenNeeded(this);

    }

}
