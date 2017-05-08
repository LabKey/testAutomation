package org.labkey.test.components.internal;

import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.WebDriver;

public class ImpersonateRoleWindow extends ImpersonateWindow
{
    public ImpersonateRoleWindow(WebDriver driver)
    {
        super("Impersonate Roles", driver);
    }

    public ImpersonateRoleWindow selectRoles(String... roles)
    {
        for (String role : roles)
            Ext4GridRef.locateExt4GridCell(role).waitForElement(this, 1000).click();

        return this;
    }
}
