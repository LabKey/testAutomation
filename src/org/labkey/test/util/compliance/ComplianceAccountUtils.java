/*
 * Copyright (c) 2017-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
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
        usersTable.checkAll();

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
