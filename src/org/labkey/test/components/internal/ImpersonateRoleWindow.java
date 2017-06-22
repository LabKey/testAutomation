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

import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.WebDriver;

public class ImpersonateRoleWindow extends ImpersonateWindow
{
    public ImpersonateRoleWindow(WebDriver driver)
    {
        super("Impersonate Roles", driver);
    }

    public ImpersonateRoleWindow selectRoles(String... roles)
    {
        for (String role : roles)
            Ext4GridRef.locateExt4GridCell(role).waitForElement(this, 1000).click();

        return this;
    }
}
