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
