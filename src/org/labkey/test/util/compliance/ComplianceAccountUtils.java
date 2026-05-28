/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.util.compliance;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

public class ComplianceAccountUtils
{
    BaseWebDriverTest _test;

    public ComplianceAccountUtils(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void reactivateAllAccounts()
    {
        _test.log("Reactivate all deactivated users.");
        _test.goToHome();
        _test.goToSiteUsers();
        DataRegionTable usersTable = new DataRegionTable("Users", _test);
        int countOfActiveUsers = usersTable.getDataRowCount();
        _test.clickAndWait(Locator.linkWithText("include inactive users"));
        usersTable = new DataRegionTable("Users", _test);
        int countOfAllUsers = usersTable.getDataRowCount();
        usersTable.checkAllOnPage();

        _test.log("Number of active users: " + countOfActiveUsers + " Number of total users: " + countOfAllUsers);

        // If the count of all users is larger than active users then it means there are inactive users.
        if(countOfAllUsers > countOfActiveUsers)
        {
            _test.log("There are inactive users, going to make them active.");
            _test.clickButton("Reactivate");
            _test.waitForText("Reactivate Users");
            _test.clickButton("Reactivate");
            _test.waitForText("include inactive users");
        }
        _test.goToHome();
    }
}
