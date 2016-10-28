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

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.WebDriver;

public abstract class ChartWizardDialog<EC extends ChartWizardDialog.ElementCache> extends Window<EC>
{
    public ChartWizardDialog(String title, WebDriver driver)
    {
        super(Locator.tagWithClass("div", "chart-wizard-dialog")
                .withDescendant(Locator.tagWithClass("div", "title-panel")
                        .withText(title))
                .waitForElement(driver, 10000), driver);
    }

    @Override
    public void close()
    {
        clickCancel();
    }

    public void clickCancel()
    {
        clickButton("Cancel", 0);
        waitForClose();
    }

    class ElementCache extends Window.Elements
    {
        public ElementCache()
        {
            title = Locator.css("div.title-panel").findWhenNeeded(this);
            body = Locator.css("div.chart-wizard-panel").findWhenNeeded(this);
        }
    }
}
