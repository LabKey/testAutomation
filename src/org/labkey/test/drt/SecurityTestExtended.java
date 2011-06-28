package org.labkey.test.drt;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 6/22/11
 * Time: 11:40 AM
 * To change this template use File | Settings | File Templates.
 */

/**This class is for security related tests that should be run only weekly,
 * because they are time consuming, unlikely to fail, or otherwise do not need
 */
public class SecurityTestExtended extends SecurityTest
{

    protected void doTestSteps()
    {
        cantReachAdminToolFromUserAccount(true);
    }
}
