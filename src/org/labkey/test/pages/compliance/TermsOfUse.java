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

import java.util.ArrayList;
import java.util.List;

public class TermsOfUse extends LabKeyPage<LabKeyPage<?>.ElementCache>
{
    public TermsOfUse(WebDriver driver)
    {
        super(driver);
    }

    public void agreeToTerms(boolean agree)
    {
        Checkbox.Ext4Checkbox().locatedBy(Locator.id("AgreeToTermsCheckbox-inputEl"))
                .find(getDriver())
                .set(agree);
    }

    public void agreeToTermsAndOk()
    {
        agreeToTerms(true);
        clickOK();
    }

    public List<String> getTerms()
    {
        List<String> terms = new ArrayList<>();
        List<WebElement> termEls =  Locator.xpath("//table[@class='term']//td[not(@class='termnumber')]").waitForElements(getDriver(), WAIT_FOR_JAVASCRIPT);
        for(WebElement el : termEls)
        {
            terms.add(el.getText());
        }
        return terms;
    }

    public boolean isTermsDialogPresent()
    {
        return isTextPresent("Terms of Use");
    }

    public void clickOK()
    {
        clickAndWait(Locator.tagWithText("span", "OK"));
    }
}
