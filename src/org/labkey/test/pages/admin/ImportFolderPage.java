/*
 * Copyright (c) 2018-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath));
        ImportFolderPage page = new ImportFolderPage(driver.getDriver());
        page.setDriver(driver.getDriver());
        return page;
    }

    public ImportFolderPage selectLocalZipArchive()
    {
        elementCache().localZipRadio.check();
        return this;
    }

    public ImportFolderPage chooseFile(File file)
    {
        setFormElement(Locator.input("folderZip"), file);
        return this;
    }

    public void clickImportFolder()
    {
        clickButton("Import Folder");
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