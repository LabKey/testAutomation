package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/12/12
 * Time: 11:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class APIUserHelper extends AbstractUserHelper
{
    public APIUserHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }


    public void createUser(String userName, boolean verifySuccess)
    {

            CreateUserCommand command = new CreateUserCommand(userName);
            Connection connection = _test.getDefaultConnection();
            try
            {
                command.execute(connection, "");
            }
            catch (Exception e)
            {
                if(verifySuccess)
                    Assert.fail("Error while creating user: " + e.getMessage());
            }
    }
}
