/*
 * Copyright (c) 2016-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.compliance;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BaseComplianceSettingsTab extends LabKeyPage
{
    Elements _elements;

    public BaseComplianceSettingsTab(WebDriver driver)
    {
        super(driver);
    }

    public void clickSave()
    {
        clickAndWait(elements().saveButton);
    }

    public void clickCancel()
    {
        clickAndWait(elements().cancelButton);
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        WebElement saveButton = new LazyWebElement(Locators.saveButton, this);
        WebElement cancelButton = new LazyWebElement(Locators.cancelButton, this);
    }

    public static class Locators extends org.labkey.test.Locators
    {
        protected static final Locator saveButton = Locator.lkButton("Save");
        protected static final Locator cancelButton = Locator.lkButton("Cancel");
    }
}
