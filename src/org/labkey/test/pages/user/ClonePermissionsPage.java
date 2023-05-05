package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ClonePermissionsPage extends LabKeyPage<ClonePermissionsPage.ElementCache>
{
    public ClonePermissionsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ClonePermissionsPage beginAt(WebDriverWrapper webDriverWrapper, Integer userId)
    {
        Map<String, String> params = Map.of("userId", userId.toString(), "schemaName", "core", "query.queryName", "Users", "targetUser", userId.toString());
        webDriverWrapper.beginAt(WebTestHelper.buildURL("security", "clonePermissions", params));
        return new ClonePermissionsPage(webDriverWrapper.getDriver());
    }

    public String getWarningMessage()
    {
        return elementCache().warningMsg.getText();
    }

    public ClonePermissionsPage setCloneUser(String value)
    {
        elementCache().cloneUser.set(value);
        return this;
    }

    public ShowUsersPage clonePermission()
    {
        clickAndWait(elementCache().clonePermissionBtn);
        return new ShowUsersPage(getDriver());
    }

    public String clonePermissionExpectingError()
    {
        elementCache().clonePermissionBtn.click();
        return Locator.tagWithClass("div", "labkey-error").findElement(getDriver()).getText();
    }

    @Override
    protected ClonePermissionsPage.ElementCache newElementCache()
    {
        return new ClonePermissionsPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement warningMsg = Locator.xpath("//table/tbody/tr[1]/td").findWhenNeeded(this);
        Input cloneUser = new Input(Locator.name("cloneUser").findWhenNeeded(this), getDriver());
        WebElement clonePermissionBtn = Locator.lkButton("Clone Permissions").findWhenNeeded(this);
    }
}
