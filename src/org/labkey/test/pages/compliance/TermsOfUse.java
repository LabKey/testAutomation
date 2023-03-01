/*
 * Copyright (c) 2017-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class TermsOfUse extends LabKeyPage
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
