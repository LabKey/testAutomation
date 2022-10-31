package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RenameFolderPage extends LabKeyPage<RenameFolderPage.ElementCache>
{
    public RenameFolderPage(WebDriver driver)
    {
        super(driver);
    }

    public static RenameFolderPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "renameFolder"));
        return new RenameFolderPage(driver.getDriver());
    }

    public String getProjectName()
    {
        return elementCache().projectName.getText();
    }

    public RenameFolderPage setProjectName(String value)
    {
        setFormElement(elementCache().projectName, value);
        return this;
    }

    public RenameFolderPage setTitleSameAsName(boolean value)
    {
        setCheckbox(elementCache().sameAsNameCheckbox, value);
        return this;
    }

    public String getProjectTitle()
    {
        return elementCache().projectTitle.getText();
    }

    public RenameFolderPage setProjectTitle(String value)
    {
        setFormElement(elementCache().projectTitle, value);
        return this;
    }

    public RenameFolderPage setAlias(boolean value)
    {
        setCheckbox(elementCache().aliasCheckbox, value);
        return this;
    }

    public FolderManagementPage save()
    {
        clickAndWait(elementCache().save);
        return new FolderManagementPage(getDriver());
    }

    public FolderManagementPage cancel()
    {
        clickAndWait(elementCache().cancel);
        return new FolderManagementPage(getDriver());
    }

    @Override
    protected RenameFolderPage.ElementCache newElementCache()
    {
        return new RenameFolderPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final WebElement projectName = Locator.name("name").findWhenNeeded(this);
        private final WebElement projectTitle = Locator.name("title").findWhenNeeded(this);
        private final WebElement sameAsNameCheckbox = Locator.name("titleSameAsName").findWhenNeeded(this);
        private final WebElement aliasCheckbox = Locator.name("addAlias").findWhenNeeded(this);
        private final WebElement save = Locator.tagWithText("span", "Save").findWhenNeeded(this);
        private final WebElement cancel = Locator.tagWithText("span", "Cancel").findWhenNeeded(this);
    }
}
