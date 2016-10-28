/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ChartQueryDialog extends ChartWizardDialog<ChartQueryDialog.ElementCache>
{
    public ChartQueryDialog(WebDriver driver)
    {
        super("Select a query", driver);
    }

    @Deprecated
    public ChartQueryDialog(BaseWebDriverTest test)
    {
        this(test.getDriver());
    }

    @Deprecated // TODO: Remove
    public void waitForDialog()
    {
    }

    public ChartQueryDialog selectSchema(String schemaName)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Schema:", schemaName);
        return this;
    }

    public ChartQueryDialog selectQuery(String queryName)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Query:", queryName);
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
    }
}
