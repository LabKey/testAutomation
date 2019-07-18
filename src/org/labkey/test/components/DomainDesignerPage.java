package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DomainDesignerPage extends LabKeyPage<DomainDesignerPage.ElementCache>
{
    public DomainDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public static DomainDesignerPage beginAt(WebDriverWrapper driver, String containerPath, String schema, String query)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "domainDesigner", Maps.of("schemaName", schema, "queryName", query)));
        return new DomainDesignerPage(driver.getDriver());
    }

    public DomainDesignerPage clickSaveChanges()
    {
        elementCache().saveChangesBtn.click();

        waitFor(()-> Locator.tagWithClass("div", "alert-success").existsIn(getDriver()),
                "domain save did not notify success", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public DomainFormPanel fieldProperties(String queryName)
    {
        return elementCache().domainFormPanel(queryName);
    }

    public WebElement errorAlert()
    {
        Locator alertLoc = Locator.tagWithClass("div", "alert-danger");
        return alertLoc.existsIn(getDriver()) ? null : alertLoc.findElement(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DomainFormPanel domainFormPanel(String domainName)
        {
            return new DomainFormPanel.DomainFormPanelFinder(getDriver())
                    .withTitle("Field Properties - " + domainName).findWhenNeeded(this);
        }
        WebElement saveChangesBtn = Locators.domainDesignerButton("Save Changes")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelBtn = Locators.domainDesignerButton("Cancel")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

    public static class Locators
    {
        static public Locator.XPathLocator domainDesignerButton(String text)
        {
            return Locator.tagWithClass("button", "domain-designer-button").withText(text);
        }
    }
}
