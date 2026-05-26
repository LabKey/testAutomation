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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RoleRow extends PermissionsRowBase<RoleRow>
{
    protected RoleRow(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected WebElement getAssignmentContainer()
    {
        return Locator.byClass("permissions-assignments-row").waitForElement(getComponentElement(), 2000);
    }

    @Override
    protected RoleRow getThis()
    {
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends PermissionsRowBase<RoleRow>.ElementCache
    {
    }

    public static class RoleRowFinder extends PermissionsRowFinder<RoleRow, RoleRowFinder>
    {

        public RoleRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public RoleRowFinder forRole(String roleTitle)
        {
            super.withTitle(roleTitle);
            return this;
        }

        @Override
        protected RoleRow construct(WebElement el, WebDriver driver)
        {
            return new RoleRow(el, driver);
        }
    }
}
