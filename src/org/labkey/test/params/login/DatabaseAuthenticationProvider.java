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
package org.labkey.test.params.login;

import org.labkey.test.pages.core.login.DatabaseAuthConfigureDialog;
import org.labkey.test.pages.core.login.LoginConfigRow;
import org.openqa.selenium.WebDriver;

public class DatabaseAuthenticationProvider extends AuthenticationProvider<DatabaseAuthConfigureDialog>
{
    @Override
    public String getProviderName()
    {
        return "Database";
    }

    @Override
    public String getProviderDescription()
    {
        return "Standard database authentication";
    }

    @Override
    public DatabaseAuthConfigureDialog getEditDialog(LoginConfigRow row)
    {
        return new DatabaseAuthConfigureDialog(row);
    }

    @Override
    public DatabaseAuthConfigureDialog getNewDialog(WebDriver driver)
    {
        return null;    // we shouldn't be calling this it is there by default/no way to add a new one;
    }
}
