package org.labkey.test.pages.admin;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class ImportFolderPage extends LabKeyPage<ImportFolderPage.ElementCache> implements FolderManagementTab
{
    private WebDriver _driver;

    public ImportFolderPage() { }

    public ImportFolderPage(WebDriver driver)
    {
        super(driver);
    }

    public static ImportFolderPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ImportFolderPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "importFolder", Maps.of("tabId", "import")));
        return new ImportFolderPage(driver.getDriver());
    }

    @NotNull
    @Override
    public WebDriver getWrappedDriver()
    {
        if (_driver == null)
            throw new IllegalStateException("Page object not initialized. Call setDriver() before use.");
        return _driver;
    }

    @Override
    public void setDriver(WebDriver driver)
    {
        _driver = driver;
    }

    @Override
    public String getTabId()
    {
        return "import";
    }

    public void importFolderFromZip(File zipFile)
    {
        // TODO
    }

    public void importFromTemplateFolder(String containerPath)
    {
        if (!containerPath.startsWith("/"))
            containerPath = "/" + containerPath;
        elementCache().existingFolderRadio.check();
        elementCache().sourceFolderCombo.selectComboBoxItem(containerPath);
        clickAndWait(elementCache().importFolderButton);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        RadioButton localZipRadio = new RadioButton.RadioButtonFinder().withLabel("Local zip archive").findWhenNeeded(this);
        RadioButton existingFolderRadio = new RadioButton.RadioButtonFinder().withLabel("Existing folder").findWhenNeeded(this);
        Checkbox validateQueriesCheckbox = Checkbox.Checkbox(Locator.input("validateQueries")).findWhenNeeded(this);
        Checkbox advancedOptionsCheckbox = Checkbox.Checkbox(Locator.input("advancedImportOptions")).findWhenNeeded(this);
        Checkbox createSharedDatasetsCheckbox = Checkbox.Checkbox(Locator.input("createSharedDatasets")).findWhenNeeded(this);
        ComboBox sourceFolderCombo = new ComboBox.ComboBoxFinder(getDriver()).withInputNamed("sourceTemplateFolder").findWhenNeeded(this);

        WebElement importFolderButton = Locator.lkButton("Import Folder").findWhenNeeded(this);
        WebElement usePipelineButton = Locator.lkButton("Use Pipeline").findWhenNeeded(this);
    }
}