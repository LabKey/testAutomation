package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.WebPartPanel;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Table;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginConfigurePage extends LabKeyPage<LoginConfigurePage.ElementCache>
{
    public LoginConfigurePage(WebDriver driver)
    {
        super(driver);
    }

    public static LoginConfigurePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static LoginConfigurePage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("login", containerPath, "configure"));
        return new LoginConfigurePage(webDriverWrapper.getDriver());
    }

    public LdapConfigurePage addLdapConfiguration()
    {
        elementCache().addMenu.
                clickSubMenu(true,"LDAP - Uses the LDAP protocol to authenticate against an institution's directory server");
        return new LdapConfigurePage(getDriver());
    }

    public LdapConfigurePage removeLdapConfiguration(String description)
    {
        int targetIndex = elementCache().authConfigurationsTable.getRowIndex("Description", description);
        Locator.linkWithText("remove").index(targetIndex).findElement(elementCache().authConfigurationsTable.getComponentElement()).click();
        // handle dialog

        return new LdapConfigurePage(getDriver());
    }

    public LdapConfigurePage clickEditLdapConfiguration(String description)
    {
        Table table = elementCache().authConfigurationsTable;
        WebElement cell = table.getDataAsElement(table.getRowIndex("Description", description), 3);
        clickAndWait(Locator.linkWithText("edit").findElement(cell));

        return new LdapConfigurePage(getDriver());
    }

    public ShowAdminPage clickDone()
    {
        clickAndWait(elementCache().doneBtn);
        return new ShowAdminPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebPartPanel primaryConfigsPanel = WebPartPanel.WebPart(getDriver()).timeout(WAIT_FOR_JAVASCRIPT)
                .withTitle("Primary authentication configurations").findWhenNeeded();

        BootstrapMenu addMenu = new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div", "lk-menu-drop")
                .withChild(Locator.tagWithAttribute("a", "data-toggle", "dropdown").withText("Add..."))
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));

        WebElement addAuthButton = Locator.linkWithSpan("Add...").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

       Table authConfigurationsTable = new Table(getDriver(), primaryConfigsPanel.getComponentElement());

       WebElement doneBtn = Locator.button("Done").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}
