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
package org.labkey.test.components.internal;

import org.labkey.test.components.ext4.ComboBox;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.STARTS_WITH;

public class ImpersonateGroupWindow extends ImpersonateWindow
{
    private final ComboBox groupCombo = ComboBox.ComboBox(getDriver()).withLabel("Group:").findWhenNeeded(this);

    public ImpersonateGroupWindow(WebDriver driver)
    {
        super("Impersonate Group", driver);
        groupCombo.setMatcher(STARTS_WITH);
    }

    public ImpersonateGroupWindow selectGroup(String groupName)
    {
        groupCombo.selectComboBoxItem(groupName);
        return this;
    }

    public ImpersonateGroupWindow selectSiteGroup(String groupName)
    {
        groupCombo.selectComboBoxItem("Site: " + groupName);
        return this;
    }
}
