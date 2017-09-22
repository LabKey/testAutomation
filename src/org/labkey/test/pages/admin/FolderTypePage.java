package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.RadioButton.RadioButton;

public class FolderTypePage extends FolderManagementPage
{
    public FolderTypePage(WebDriver driver)
    {
        super(driver);
    }

    public static FolderTypePage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static FolderTypePage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "folderType"));
        return new FolderTypePage(driver.getDriver());
    }

    public FolderTypePage setFolderType(String folderType)
    {
        elementCache().findFolderTypeRadioButton(folderType).check();
        return this;
    }

    public FolderTypePage enableModule(String module)
    {
        elementCache().findActiveModuleCheckbox(module).check();
        return this;
    }

    public FolderTypePage disableModule(String module)
    {
        elementCache().findActiveModuleCheckbox(module).uncheck();
        // TODO: Handle disabled checkboxes and warning for dependent modules
        return this;
    }

    public void save()
    {
        clickAndWait(elementCache().updateFolderButton);
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

    protected class ElementCache extends FolderManagementPage.ElementCache
    {
        protected RadioButton findFolderTypeRadioButton(String folderType)
        {
            return RadioButton(Locator.radioButtonByNameAndValue("folderType", folderType)).find(this);
        }

        protected Checkbox findActiveModuleCheckbox(String module)
        {
            return Checkbox(Locator.checkboxByNameAndValue("activeModules", module)).find(this);
        }

        protected final WebElement updateFolderButton = Locator.id("UpdateFolderButtonDiv").childTag("a").findWhenNeeded(this);
    }
}