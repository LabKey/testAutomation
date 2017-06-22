/*
 * Copyright (c) 2017 LabKey Corporation
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