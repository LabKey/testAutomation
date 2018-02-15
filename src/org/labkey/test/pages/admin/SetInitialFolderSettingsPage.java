package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SetInitialFolderSettingsPage extends LabKeyPage<SetInitialFolderSettingsPage.ElementCache>
{
    public SetInitialFolderSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static SetInitialFolderSettingsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static SetInitialFolderSettingsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new SetInitialFolderSettingsPage(driver.getDriver());
    }

    public SetInitialFolderSettingsPage setCustomFileRoot(String fileRoot)
    {
        elementCache().customLocRadioButton.click();
        setFormElement(elementCache().folderRootPathInput, fileRoot);
        return this;
    }

    public LabKeyPage clickFinish()
    {
        clickAndWait(elementCache().finishButton);

        return new LabKeyPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        final WebElement finishButton = Locator.lkButton("Finish").findWhenNeeded(this).withTimeout(4000);
        public WebElement useDefaultRadioButton =  Locator.xpath("//td[./label[text()='Use Default']]/input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final WebElement customLocRadioButton =  Locator.xpath("//td[./label[text()='Custom Location']]/input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final WebElement folderRootPathInput = Locator.input("folderRootPath")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}