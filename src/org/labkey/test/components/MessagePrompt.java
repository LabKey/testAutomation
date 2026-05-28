/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.html.Input.Input;

/**
 * Interact with an Ext4.Msg.prompt() dialog box.
 */
public class MessagePrompt extends Window<MessagePrompt.ElementCache>
{
    public MessagePrompt(String title, WebDriver driver)
    {
        super(title, driver);
    }

    public String getValue()
    {
        return elementCache().input.getValue();
    }

    public MessagePrompt setValue(String value)
    {
        elementCache().input.setValue(value);
        return this;
    }

    public void clickOK()
    {
        clickButton("OK", true);
    }

    public void clickCancel()
    {
        clickButton("Cancel", true);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends Window.ElementCache
    {
        protected Input input = Input(Locator.tagWithAttribute("input", "type", "text"), getDriver()).findWhenNeeded(this);
    }
}
