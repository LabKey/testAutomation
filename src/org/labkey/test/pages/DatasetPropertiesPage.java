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
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.ext4.Window.Window;

public class DatasetPropertiesPage extends LabKeyPage<DatasetPropertiesPage.ElementCache>
{
    public DatasetPropertiesPage(WebDriver driver){super(driver);}

    protected DatasetPropertiesPage.ElementCache newElementCache()
    {
        return new DatasetPropertiesPage.ElementCache();
    }

    public EditDatasetDefinitionPage clickEditDefinition()
    {
        clickAndWait(elementCache().editDefinitionButton);
        return new EditDatasetDefinitionPage(getDriver());
    }

    public ViewDatasetDataPage clickViewData()
    {
        clickAndWait(elementCache().viewDataButton);
        return new ViewDatasetDataPage(getDriver());
    }

    public ManageDatasetsPage clickManageDatasets()
    {
        clickAndWait(elementCache().manageDatasetsButton);
        return new ManageDatasetsPage(getDriver());
    }

    public LabKeyPage deleteDataset()
    {
        click(elementCache().deleteDatasetButton);
        return null;
    }

    public ResultWindow deleteAllRows()
    {
        click(elementCache().deleteAllRowsButton);
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
            return new ResultWindow();
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
        WebElement viewDataButton = Locator.lkButton("View Data").findWhenNeeded(this);
        WebElement manageDatasetsButton = Locator.lkButton("Manage Datasets").findWhenNeeded(this);
        WebElement deleteDatasetButton = Locator.lkButton("Delete Dataset").findWhenNeeded(this);
        WebElement deleteAllRowsButton = Locator.lkButton("Delete All Rows").findWhenNeeded(this);
        WebElement showImportHistoryButton = Locator.lkButton("Show Import History").findWhenNeeded(this);
        WebElement editDefinitionButton = Locator.lkButton("Edit Definition").findWhenNeeded(this);
    }
}
