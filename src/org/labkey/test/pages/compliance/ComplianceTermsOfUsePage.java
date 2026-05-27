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

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ComplianceTermsOfUsePage extends LabKeyPage<ComplianceTermsOfUsePage.ElementCache>
{
    public ComplianceTermsOfUsePage(WebDriver test)
    {
        super(test);
        waitForText("Terms of Use");
    }

    public ComplianceTermsOfUsePage checkAgree()
    {
        elementCache().agreeCheckbox.check();
        return this;
    }

    public ComplianceTermsOfUsePage uncheckAgree()
    {
        elementCache().agreeCheckbox.uncheck();
        return this;
    }

    public void clickOk()
    {
        clickButton("OK");
    }

    public String getLabelText()
    {
        return elementCache().agreeLabel.getText();
    }

    @Override
    protected ComplianceTermsOfUsePage.ElementCache newElementCache()
    {
        return new ComplianceTermsOfUsePage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        final Checkbox agreeCheckbox = Checkbox.Ext4Checkbox().locatedBy(Locator.id("AgreeToTermsCheckbox-inputEl")).findWhenNeeded(this);
        final WebElement agreeLabel = Locator.xpath("//label[@id='AgreeToTermsCheckbox-boxLabelEl']").findWhenNeeded(this);
    }

}
