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
package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class GroupRow extends PermissionsRowBase<GroupRow>
{
    protected GroupRow(WebElement el, WebDriver driver)
    {
        super(el, driver);
    }

    @Override
    protected WebElement getAssignmentContainer()
    {
        return Locator.tagWithClass("div", "expandable-container__member-buttons")
                .waitForElement(getComponentElement(), 2000);
    }

    @Override
    protected GroupRow getThis()
    {
        return this;
    }

    protected boolean isDeleteEmptyButtonDisabled()
    {
        return !elementCache().deleteEmptyGroupBtn().isEnabled();
    }

    public void clickDeleteEmptyGroup()
    {
        expand();
        var btn = elementCache().deleteEmptyGroupBtn();
        btn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(btn));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends PermissionsRowBase<GroupRow>.ElementCache
    {
        final WebElement actionContainer = Locator.tagWithClass("div", "expandable-container__action-container")
                .findWhenNeeded(this);

        final WebElement deleteEmptyGroupBtn()
        {
            return Locator.tagWithText("button", "Delete Empty Group")
                    .findElement(actionContainer);
        }
    }

    public static class GroupRowFinder extends PermissionsRowFinder<GroupRow, GroupRowFinder>
    {
        public GroupRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public GroupRowFinder forGroup(String groupName)
        {
            super.withTitle(groupName);
            return this;
        }

        @Override
        protected GroupRow construct(WebElement el, WebDriver driver)
        {
            return new GroupRow(el, driver);
        }

    }

}
