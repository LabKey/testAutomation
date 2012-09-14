package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/12/12
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractUserHelper extends AbstractHelper
{
    public AbstractUserHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }


    public void createUser(String userName)
    {
        createUser(userName, true);
    }

    public abstract void createUser(String userName, boolean verifySuccess);
}
