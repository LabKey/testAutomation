/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Component for bootstrap modal created by LABKEY.Utils.alert() and LABKEY.Utils.modal()
 * clientapi/dom/Utils.js
 */
public class LabKeyAlert extends ModalDialog implements Alert
{
    public LabKeyAlert(WebDriver driver)
    {
        this(driver, 0);
    }

    /**
     * @deprecated Use {@link #getFinder(WebDriver)}
     */
    @Deprecated
    public LabKeyAlert(WebDriver driver, int timeout)
    {
        this(getFinder(driver).timeout(timeout).findWhenNeeded(driver).getComponentElement(), driver);
    }

    private LabKeyAlert(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static SimpleWebDriverComponentFinder<LabKeyAlert> getFinder(WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, Locator.id("lk-utils-modal"), LabKeyAlert::new);
    }

    public WebElement getFunctionBody()
    {
        return ExtraLocators.functionBody.findWhenNeeded(getComponentElement()).withTimeout(WAIT_FOR_JAVASCRIPT);
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

    public void clickButton(String buttonText)
    {
        getWrapper().clickAndWait(Locator.linkWithText(buttonText).findElement(this));
    }

    public static class ExtraLocators {
        static public Locator.XPathLocator functionBody = Locator.tagWithClass("div", "modal-fn-body");
    }
}
