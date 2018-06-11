package org.labkey.test;


public abstract class BaseBlueGreenTest extends BaseWebDriverTest
{
    protected boolean runValidationOnly()
    {
        if(null == System.getProperty("testValidationOnly"))
            log("System property testValidationOnly is null");
        else
            log("Value of system property testValidationOnly is '" + System.getProperty("testValidationOnly").toLowerCase().trim() + "'");

        if ((null == System.getProperty("testValidationOnly")) || (System.getProperty("testValidationOnly").toLowerCase().trim().equals("false")))
        {
            log("Going to do a full test run.");
            return false;
        }
        else
        {
            log("Only going to run the validation part of the tests.");
            return true;
        }
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        // Only run cleanup before the running a full test.
        if(!runValidationOnly() && !afterTest)
        {
            log("Doing cleanup.");
            super.doCleanup(afterTest);
        }
    }

    protected String recordError(String msg)
    {
        log("****************************");
        log(msg);
        log("****************************");
        return msg + "\n";
    }

}
