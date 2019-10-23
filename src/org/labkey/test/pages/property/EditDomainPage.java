package org.labkey.test.pages.property;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EditDomainPage extends LabKeyPage<EditDomainPage.ElementCache>
{
    public EditDomainPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditDomainPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static EditDomainPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("property", containerPath, "editDomain"));
        return new EditDomainPage(driver.getDriver());
    }

    public PropertiesEditor getFieldEditor()
    {
        return elementCache().propertiesEditor;
    }

    public void clickSave()
    {
        waitForElement(Locator.lkButton("Save"));
        clickAndWait(elementCache().saveButton);
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final PropertiesEditor propertiesEditor = PropertiesEditor.PropertiesEditor(getDriver()).findWhenNeeded();

        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
