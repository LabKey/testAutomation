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
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.ext4.Window.Window;

public class DatasetPropertiesPage extends LabKeyPage<DatasetPropertiesPage.ElementCache>
{
    public DatasetPropertiesPage(WebDriver driver)
    {
        super(driver);
        // TODO: Need to figure out a way to wait for buttons to be responsive so that we don't need to use clickUntilStale
    }

    protected DatasetPropertiesPage.ElementCache newElementCache()
    {
        return new DatasetPropertiesPage.ElementCache();
    }

    public EditDatasetDefinitionPage clickEditDefinition()
    {
        doAndWaitForPageToLoad(() -> shortWait().until(LabKeyExpectedConditions.clickUntilStale(elementCache().editDefinitionButton)));
        return new EditDatasetDefinitionPage(getDriver());
    }

    public ViewDatasetDataPage clickViewData()
    {
        doAndWaitForPageToLoad(() -> shortWait().until(LabKeyExpectedConditions.clickUntilStale(elementCache().viewDataButton)));
        return new ViewDatasetDataPage(getDriver());
    }

    public ManageDatasetsPage clickManageDatasets()
    {
        doAndWaitForPageToLoad(() -> shortWait().until(LabKeyExpectedConditions.clickUntilStale(elementCache().manageDatasetsButton)));
        return new ManageDatasetsPage(getDriver());
    }

    public LabKeyPage deleteDataset()
    {
        elementCache().deleteDatasetButton.click();
        return null;
    }

    public ResultWindow deleteAllRows()
    {
        elementCache().deleteAllRowsButton.click();
        return new ConfirmationWindow().confirm();
    }

    public class ConfirmationWindow
    {
        private final Window _window;

        private ConfirmationWindow()
        {
            _window = Window(DatasetPropertiesPage.this.getDriver()).withTitle("Confirm Deletion").waitFor();
        }

        public ResultWindow confirm()
        {
            _window.clickButton("Yes", false);
            ResultWindow resultWindow = new ResultWindow();
            WebDriverWrapper.waitFor(() -> resultWindow.getResult().equals("Success"), WAIT_FOR_JAVASCRIPT);
            return resultWindow;
        }

        public DatasetPropertiesPage reject()
        {
            _window.clickButton("No", true);
            return DatasetPropertiesPage.this;
        }
    }

    public class ResultWindow
    {
        private final Window _window;

        protected ResultWindow()
        {
            _window = Window(DatasetPropertiesPage.this.getDriver()).waitFor();
        }

        public String getResult()
        {
            return _window.getTitle();
        }

        public String getMessage()
        {
            return _window.getBody();
        }

        public DatasetPropertiesPage accept()
        {
            _window.clickButton("OK", true);
            return DatasetPropertiesPage.this;
        }
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        final WebElement viewDataButton = Locator.lkButton("View Data").findWhenNeeded(this);
        final WebElement manageDatasetsButton = Locator.lkButton("Manage Datasets").findWhenNeeded(this);
        final WebElement deleteDatasetButton = Locator.lkButton("Delete Dataset").findWhenNeeded(this);
        final WebElement deleteAllRowsButton = Locator.lkButton("Delete All Rows").findWhenNeeded(this);
        final WebElement showImportHistoryButton = Locator.lkButton("Show Import History").findWhenNeeded(this);
        final WebElement editDefinitionButton = Locator.lkButton("Edit Definition").findWhenNeeded(this);
    }
}
