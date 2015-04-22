package org.labkey.test.pages;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;

/**
 * Created by susanh on 4/21/15.
 */
public class Permissions
{
    private BaseWebDriverTest _test;
    private String _projectName;

    /**
     * @param test the test driver in which the page is being used
     * @param projectName name of the project in which permissions are being set or null for site-wide permissions
     */
    public Permissions(BaseWebDriverTest test, @Nullable String projectName)
    {
        this._test = test;
        this._projectName = projectName;
    }
}
