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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.ext4.ComboBox;
import org.openqa.selenium.WebDriver;

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
        return this;
    }

    public ChartQueryDialog selectQuery(String queryName)
    {
        elementCache().queryCombo.selectComboBoxItem(queryName);
        return this;
    }

    public void clickCancel()
    {
        clickButton("Cancel", 0);
        waitForClose();
    }

    public ChartTypeDialog clickOk()
    {
        clickButton("OK", 0);
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
    }
}
