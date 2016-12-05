/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;

/**
 * Created by susanh on 9/26/15.
 */
public class ManageParticipantGroupsPage extends LabKeyPage
{
    public ManageParticipantGroupsPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public boolean isEditEnabled()
    {
        return _test.isElementPresent(Locator.linkContainingText("Edit Selected").enabled());
    }

    public boolean isDeleteEnabled()
    {
        return _test.isElementPresent(Locator.linkContainingText("Delete Selected").enabled());
    }

    public void selectGroup(String name)
    {
        Locator groupByName = Locator.xpath("//table[@role=\"presentation\"]/tbody/tr/td/div[contains(normalize-space(), '" + name + "')]");
        waitAndClick(groupByName);
    }

    public void deleteGroup(String name)
    {
        selectGroup(name);
        Locator.linkContainingText("Delete Selected").findElement(getDriver()).click();
        Locator.linkContainingText("Yes").findElement(getDriver()).click();
    }
}
