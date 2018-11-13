/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.labkey.LabKeyAlert;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.openqa.selenium.WebElement;

public class ConfigureMasterPatientIndexPage extends LabKeyPage<ConfigureMasterPatientIndexPage.ElementCache>
{

    public ConfigureMasterPatientIndexPage(WebDriverWrapper test) {
        super(test);
    }

    public ShowAdminPage clickSave() {
        elementCache().saveButton.click();
        return new ShowAdminPage(getDriver());
    }

    public ShowAdminPage clickCancel() {
        elementCache().cancelButton.click();
        return new ShowAdminPage(getDriver());
    }

    public ConfigureMasterPatientIndexPage setServerUrl(String serverUrl) {
        setFormElement(elementCache().serverUrl, serverUrl);
        return this;
    }

    public String getServerUrl() {
        return getFormElement(elementCache().serverUrl);
    }

    public ConfigureMasterPatientIndexPage setUser(String userName) {
        setFormElement(elementCache().userName, userName);
        return this;
    }

    public String getUserName() {
        return getFormElement(elementCache().userName);
    }

    public ConfigureMasterPatientIndexPage setPassword(String password) {
        setFormElement(elementCache().password, password);
        return this;
    }

    public String getPassword() {
        return getFormElement(elementCache().password);
    }

    public LabKeyAlert clickTestConnection() {
        elementCache().testConnectionButton.click();
        waitForElementToBeVisible(LabKeyAlert.Locators.dialog);
        return new LabKeyAlert(getDriver(), 5000);
    }

    public String getIndexType() {
        return getSelectedOptionText(elementCache().indexType);
    }

    public ConfigureMasterPatientIndexPage setIndexType(String optionText) {
        selectOptionByText(elementCache().indexType, optionText);
        return this;
    }

    public boolean isErrorMessagePresent() {
        Locator errorMsg = Locator.byClass("labkey-error");
        return (isElementPresent(errorMsg) && isElementVisible(errorMsg));
    }

    public String getErrorMessage() {
        if(isErrorMessagePresent())
            return elementCache().errorMsg.getText();
        else
            return "";
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement saveButton = Locator.lkButton("save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("cancel").findWhenNeeded(this);
        protected WebElement testConnectionButton = Locator.lkButton("test connection").findWhenNeeded(this);
        protected WebElement indexType = Locator.name("type").findWhenNeeded(this);

        protected WebElement serverUrl = Locator.input("url").findWhenNeeded(this);
        protected WebElement userName = Locator.input("username").findWhenNeeded(this);
        protected WebElement password = Locator.input("password").findWhenNeeded(this);

        protected WebElement errorMsg = Locator.byClass("labkey-error").findWhenNeeded(this);
    }
}
