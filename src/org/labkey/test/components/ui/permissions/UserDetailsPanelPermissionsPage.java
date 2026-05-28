/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class UserDetailsPanelPermissionsPage extends UserDetailsPanel
{
    protected UserDetailsPanelPermissionsPage(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static SimpleWebDriverComponentFinder<UserDetailsPanelPermissionsPage> finder(WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, LOC, UserDetailsPanelPermissionsPage::new);
    }

    public List<String> getEffectiveRoles()
    {
        var listContainer=  Locator.tagWithClass("div", "principal-detail-label").withText("Effective Roles")
                .parent().descendant("ul").waitForElement(this, 2000);
        return Locator.tag("li")
                .findElements(listContainer)
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }
}
