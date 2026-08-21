/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
