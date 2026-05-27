/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.EscapeUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

public class UpdateUserDetailsPage extends LabKeyPage<UpdateUserDetailsPage.ElementCache>
{
    public UpdateUserDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateUserDetailsPage beginAt(WebDriverWrapper webDriverWrapper, Integer userId)
    {
        Map<String, String> params = Map.of("userId", userId.toString(), "schemaName", "core", "query.queryName", "SiteUsers");
        webDriverWrapper.beginAt(WebTestHelper.buildURL("user", "showUpdate", params));
        return new UpdateUserDetailsPage(webDriverWrapper.getDriver());
    }

    public UpdateUserDetailsPage setDisplayName(String value)
    {
        return setField("DisplayName", value);
    }

    @Override
    public String getDisplayName()
    {
        return getField("DisplayName");
    }

    public UpdateUserDetailsPage setFirstName(String value)
    {
        return setField("FirstName", value);
    }

    public String getFirstName()
    {
        return getField("FirstName");
    }

    public UpdateUserDetailsPage setLastName(String value)
    {
        return setField("LastName", value);
    }

    public String getLastName()
    {
        return getField("LastName");
    }

    public UpdateUserDetailsPage setPhone(String value)
    {
        return setField("Phone", value);
    }

    public String getPhone()
    {
        return getField("Phone");
    }

    public UpdateUserDetailsPage setMobile(String value)
    {
        return setField("Mobile", value);
    }

    public String getMobile()
    {
        return getField("Mobile");
    }

    public UpdateUserDetailsPage setField(String fieldName, String value)
    {
        elementCache().findInput(fieldName).set(value);
        return this;
    }

    public String getField(String fieldName)
    {
        return elementCache().findInput(fieldName).get();
    }

    public void clickSubmit()
    {
        clickAndWait(elementCache().submitButton);
        assertNoLabKeyErrors();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement submitButton = Locator.lkButton("Submit").findWhenNeeded(this);

        Map<String, Input> formElements = new HashMap<>();

        protected Input findInput(String fieldName)
        {
            if (!formElements.containsKey(fieldName))
            {
                Input input = Input.Input(Locator.name(EscapeUtil.getFormFieldName(fieldName)), getDriver()).find();
                formElements.put(fieldName, input);
            }
            return formElements.get(fieldName);
        }
    }
}
