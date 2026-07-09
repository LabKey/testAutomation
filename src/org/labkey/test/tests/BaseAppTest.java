package org.labkey.test.tests;

import org.jetbrains.annotations.Contract;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;

import java.util.Map;

public abstract class BaseAppTest extends BaseWebDriverTest
{
    @Contract(pure = true)
    public abstract String getAppControllerName();

    @Contract(pure = true)
    public String buildAppURL(String containerPath, Object... parts)
    {
        return WebTestHelper.buildAppURL(containerPath, getAppControllerName(), parts);
    }

    @Contract(pure = true)
    public String buildAppURL(String containerPath, Map<String, ?> params, Object... parts)
    {
        return WebTestHelper.buildAppURL(containerPath, getAppControllerName(), params, parts);
    }

}
