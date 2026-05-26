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
package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class UserDetailsPanel extends WebDriverComponent<Component<?>.ElementCache>
{
    protected static final Locator LOC = Locator.byClass("user-details-panel");

    private final WebElement _el;
    private final WebDriver _driver;

    protected UserDetailsPanel(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public String getSelectedUser()
    {
        return Locator.byClass("panel-heading")
                .findOptionalElement(this)
                .map(WebElement::getText).orElse(null);
    }

    public List<String> getGroups()
    {
        var membersList = Locator.tagWithClass("div", "principal-detail-label").withText("Groups")
                .parent().descendant("ul").findElement(this);
        return getWrapper().getTexts(Locator.tag("li").findElements(membersList));
    }

}
