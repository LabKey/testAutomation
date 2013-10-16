package org.labkey.test;

import org.jetbrains.annotations.Nullable;

import static org.labkey.test.TestProperties.isSystemMaintenanceDisabled;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * User: tchadick
 * Date: 10/4/13
 */
public class WebDriverTestPreamble extends BaseWebDriverTest
{
    public void preamble() throws Exception
    {
        log("\n\n=============== Starting " + Runner.getCurrentTestName() + Runner.getProgress() + " =================");

        _startTime = System.currentTimeMillis();

        logToServer("=== Starting " + Runner.getCurrentTestName() + Runner.getProgress() + " ===");
        signIn();
        enableEmailRecorder();
        resetErrors();

        if (isSystemMaintenanceDisabled())
        {
            // Disable scheduled system maintenance to prevent timeouts during nightly tests.
            disableMaintenance();
        }

        if ( !isGuestModeTest() )
        {
            if (!isConfigurationSupported()) // skip this check if it returns true with no database info.
            {
                log("** Skipping " + Runner.getCurrentTestName() + " test for unsupported configurarion");
                _testFailed = false;
                return;
            }
        }

        // Only do this as part of test startup if we haven't already checked. Since we do this as the last
        // step in the test, there's no reason to bother doing it again at the beginning of the next test
        if (!_checkedLeaksAndErrors && !"DRT".equals(System.getProperty("suite")))
        {
            checkLeaksAndErrors();
        }
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        throw new RuntimeException("This is not a test.");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
