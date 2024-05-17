/*
 * Copyright (c) 2016-2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.html;

import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

public class ValidatingInput extends Input
{
    public ValidatingInput(WebElement el, WebDriver driver)
    {
        super(el, driver);
    }

    @Override
    public void set(String value)
    {
        set(value, true);
    }

    public void set(String value, boolean validateValue)
    {
        WebDriverWrapper.waitFor(()-> getComponentElement().isEnabled(), "Input is not enabled.", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);

        super.set(value);
        blur();

        if (validateValue && !strip(value).equals(strip(get())))
        {
            super.set(value); // Retry once
            blur();
            assertEquals("Set failed", strip(value), strip(get())); // Fail fast when react select gets out of sync somehow
        }
    }

    private String strip(String value) // removes newlines, tabs, other non-space, non-comma, other chars
    {
        return value.replace("\n", "").replaceAll("[^a-zA-Z0-9.,\\s+]", "");
    }
}
