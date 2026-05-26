/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.pages.compliance;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.RadioButton;
import org.openqa.selenium.WebDriver;

public class ComplianceSessionSettingsPage extends BaseComplianceSettingsPage<ComplianceSessionSettingsPage.ElementCache>
{
    public ComplianceSessionSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceSessionSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, SettingsTab.Session);
        return new ComplianceSessionSettingsPage(webDriverWrapper.getDriver());
    }

    public ComplianceSessionSettingsPage showBackgroundBehindLoggedOutModal()
    {
        elementCache().showBackgroundRadio.check();
        clickSave();
        return this;
    }

    public ComplianceSessionSettingsPage blurBackgroundBehindLoggedOutModal()
    {
        elementCache().blurBackgroundRadio.check();
        clickSave();
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage<ElementCache>.ElementCache
    {
        final RadioButton showBackgroundRadio = new RadioButton.RadioButtonFinder().withNameAndValue("backgroundHideEnabled", "false").findWhenNeeded(this);
        final RadioButton blurBackgroundRadio = new RadioButton.RadioButtonFinder().withNameAndValue("backgroundHideEnabled", "true").findWhenNeeded(this);
    }
}
