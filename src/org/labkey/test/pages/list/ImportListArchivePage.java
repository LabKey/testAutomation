package org.labkey.test.pages.list;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class ImportListArchivePage extends LabKeyPage<ImportListArchivePage.ElementCache>
{
    public ImportListArchivePage(WebDriver driver)
    {
        super(driver);
    }

    public static ImportListArchivePage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ImportListArchivePage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "importListArchive"));
        return new ImportListArchivePage(driver.getDriver());
    }

    public ImportListArchivePage setZipFile(File zipFile)
    {
        setFormElement(elementCache().zipInput, zipFile);
        return this;
    }

    public BeginPage clickImport()
    {
        clickAndWait(elementCache().importButton);
        return new BeginPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement zipInput = Locator.input("listZip").findWhenNeeded(this);
        WebElement importButton = Locator.lkButton("Import List Archive").findWhenNeeded(this);
    }
}