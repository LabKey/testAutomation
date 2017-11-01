/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.components.labkey;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.security.Credentials;

/**
 * Component for bootstrap modal created by LABKEY.Utils.alert()
 * clientapi/dom/Utils.js
 */
public class LabKeyAlert extends ModalDialog implements Alert
{
    public LabKeyAlert(WebDriver driver)
    {
        this(driver, 0);
    }

    public LabKeyAlert(WebDriver driver, long timeout)
    {
        super(Locator.id("lk-utils-modal").findWhenNeeded(driver).withTimeout(timeout), driver);
    }

    @Override
    public void dismiss()
    {
        close();
    }

    @Override
    public void accept()
    {
        close();
    }

    @Override
    public String getText()
    {
        return getTitle() + " : " + getBodyText();
    }

    @Override
    public void sendKeys(String keysToSend) { }

    @Override
    public void setCredentials(Credentials credentials) { }

    @Override
    public void authenticateUsing(Credentials credentials) { }
}
