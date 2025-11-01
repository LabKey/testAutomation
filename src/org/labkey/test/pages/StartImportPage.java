/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class StartImportPage extends LabKeyPage<StartImportPage.ElementCache>
{
    public StartImportPage(WebDriver test)
    {
        super(test);
    }

    public static StartImportPage startImportFromFile(BaseWebDriverTest test, File zipFile, boolean validateQueries)
    {
        test.goToFolderManagement();
        test.clickAndWait(Locator.linkWithText("Import"));
        test.waitForElement(Locator.name("folderZip"));
        test.setFormElement(Locator.name("folderZip"), zipFile);

        StartImportPage sip = new StartImportPage(test.getDriver());
        sip.setValidateQueriesCheckBox(validateQueries);

        test.clickButtonContainingText("Import Folder");
        test.waitForText("Select specific objects to import");

        sip.clearCache();
        return sip;
    }

    public static StartImportPage startImportFromPipeline(BaseWebDriverTest test, File zipFile, boolean validateQueries, boolean selectSpecificImportOptions)
    {
        FileBrowserHelper fileBrowserHelper = new FileBrowserHelper(test);

        test.goToFolderManagement();
        test.clickAndWait(Locator.linkWithText("Import"));
        test.waitForElement(Locator.linkWithText("Use Pipeline"));
        test.click(Locator.linkWithText("Use Pipeline"));

        fileBrowserHelper.uploadFile(zipFile);
        fileBrowserHelper.importFile(zipFile.getName(), "Import Folder");
        test.waitForText("Import Folder from Pipeline");

        StartImportPage sip = new StartImportPage(test.getDriver());
        sip.setValidateQueriesCheckBox(validateQueries);

        return sip;
    }

    public void setValidateQueriesCheckBox(boolean check)
    {
        elementCache().validateQueriesCheckbox.set(check);
    }

    public void setFailForUndefinedVisitsCheckBox(boolean check)
    {
        elementCache().failForUndefinedVisitsCheckbox.set(check);
    }

    public void clickStartImport()
    {
        clickButton("Start Import");
    }

    public void clickStartImport(String confirmationText)
    {
        clickButton("Start Import", 0);

        Window confirmation = new Window("Confirmation", getDriver());
        assertEquals("Wrong confirmation message", confirmationText, confirmation.getBody());
        confirmation.clickButton("Yes");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        protected final Checkbox validateQueriesCheckbox = initialCheckbox("validateQueries");
        protected final Checkbox failForUndefinedVisitsCheckbox = initialCheckbox("failForUndefinedVisits");


        public ElementCache()
        {
            shortWait().until(ExpectedConditions.visibilityOf(validateQueriesCheckbox.getComponentElement()));
        }

        public Checkbox initialCheckbox(String name)
        {
            return Checkbox.Checkbox(Locator.input(name)).findWhenNeeded(this);
        }
    }
}
