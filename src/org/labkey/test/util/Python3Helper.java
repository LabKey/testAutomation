package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;

public class Python3Helper extends PythonHelper
{
    public Python3Helper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    protected String getVersionPrefix()
    {
        return "3.";
    }

    @Override
    protected String getPythonHome()
    {
        return "PYTHON3_HOME";
    }

    @Override
    protected String getPythonExeName()
    {
        return "python3";
    }
}
