package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/12/12
 * Time: 12:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class UIUserHelper extends AbstractUserHelper
{
    public UIUserHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @Override
    public void createUser(String userName, boolean verifySuccess)
    {
            _test.goToHome();
            _test.ensureAdminMode();
            _test.goToSiteUsers();
            _test.clickButton("Add Users");

            _test.setFormElement("newUsers", userName);
            _test.uncheckCheckbox("sendMail");
//            if (cloneUserName != null)
//            {
//                checkCheckbox("cloneUserCheck");
//                setFormElement("cloneUser", cloneUserName);
//            }
            _test.clickButton("Add Users");

            if (verifySuccess)
                Assert.assertTrue("Failed to add user " + userName, _test.isTextPresent(userName + " added as a new user to the system"));

    }
}
