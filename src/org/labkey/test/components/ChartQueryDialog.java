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
package org.labkey.test.components;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.ext4.ComboBox.ComboBox;

public class ChartQueryDialog extends ChartWizardDialog<ChartQueryDialog.ElementCache>
{
    public ChartQueryDialog(WebDriver driver)
    {
        super("Select a query", driver);
    }

    public ChartQueryDialog selectSchema(String schemaName)
    {
        elementCache().schemaCombo.selectComboBoxItem(schemaName);
        WebDriverWrapper.waitFor(() -> elementCache().queryCombo.isEnabled() && elementCache().queryCombo.getValue().isEmpty(), "Query combo not enabled after selecting a schema", 5000);
        return this;
    }

    public ChartQueryDialog selectQuery(String queryName)
    {
        elementCache().queryCombo.selectComboBoxItem(queryName);
        if (!WebDriverWrapper.waitFor(this::isOkButtonEnabled, 500))
        {
            elementCache().queryCombo.selectComboBoxItem(queryName); // Retry
            WebDriverWrapper.waitFor(this::isOkButtonEnabled, "'OK' button not enabled after selecting a query", 1000);
        }
        return this;
    }

    private boolean isOkButtonEnabled()
    {
        return !elementCache().okButton.getAttribute("class").contains("disabled");
    }

    public void clickCancel()
    {
        getWrapper().clickAndWait(elementCache().cancelButton);
    }

    public ChartTypeDialog clickOk()
    {
        elementCache().okButton.click();
        waitForClose();
        return new ChartTypeDialog(getWrapper().getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends ChartWizardDialog.ElementCache
    {
        protected ComboBox schemaCombo = ComboBox(getDriver()).withLabel("Schema:").findWhenNeeded(this);
        protected ComboBox queryCombo = ComboBox(getDriver()).withLabel("Query:").findWhenNeeded(this);
        protected final WebElement cancelButton = Ext4Helper.Locators.ext4Button("Cancel").findWhenNeeded(this);
        protected final WebElement okButton = Ext4Helper.Locators.ext4Button("OK").findWhenNeeded(this);
    }
}
