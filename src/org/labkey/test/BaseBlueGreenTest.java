package org.labkey.test;


public abstract class BaseBlueGreenTest extends BaseWebDriverTest
{
    protected boolean runValidationOnly()
    {
        if ((null == System.getProperty("testValidationOnly")) || (System.getProperty("testValidationOnly").toLowerCase().trim().equals("false")))
        {
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
