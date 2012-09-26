package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/25/12
 * Time: 6:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class SystemMaintenanceTest extends BaseSeleniumWebTest
{
    @Override
    protected String getProjectName()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void doTestSteps() throws Exception
    {

        // Disable scheduled system maintenance
        setSystemMaintenance(false);
        // Manually start system maintenance... we'll check for completion at the end of the test (before mem check)
        startSystemMaintenance();

        checkRadioButton("usageReportingLevel", "MEDIUM");     // Force devs to report full usage info
        checkRadioButton("exceptionReportingLevel", "HIGH");   // Force devs to report full exception info
        clickButton("Save");

        // Now that the test is done, ensure that system maintenance is complete...
        waitForSystemMaintenanceCompletion();

        // Verify scheduled system maintenance is disabled.
        goToAdminConsole();
        clickLinkWithText("running threads");
        assertTextNotPresent("SystemMaintenance");

    }

    @Override
    protected void doCleanup() throws Exception
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
