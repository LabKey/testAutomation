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

    public DomainDesignerPage clickSave()
    {
        elementCache().saveButton.click();
        return this;
    }
    public WebElement saveButton()
    {
        return elementCache().saveButton;
    }

    public DomainDesignerPage clickSaveAndFinish()
    {
        elementCache().saveAndFinishButton.click();

        String currentURL = getDriver().getCurrentUrl();
        waitFor(()-> Locator.tagWithClass("div", "alert-success").existsIn(getDriver()) ||
                getDriver().getCurrentUrl() != currentURL,
                "domain save did not notify success", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public WebElement saveAndFinishButton()
    {
        return elementCache().saveAndFinishButton;
    }

    public DomainFormPanel fieldProperties()
    {
        return elementCache().firstDomainFormPanel;
    }
    public DomainFormPanel fieldProperties(String queryName)
    {
        return elementCache().domainFormPanel(queryName);
    }

    public String waitForError()
    {
        waitFor(()-> Locators.dangerAlertLoc.existsIn(getDriver()),
                "the error alert did not appear as expected", 1000);
        return  errorAlert().getText();
    }
    public WebElement errorAlert()
    {
        return Locators.dangerAlertLoc.existsIn(getDriver()) ? Locators.dangerAlertLoc.findElement(getDriver()) : null;
    }

    public String waitForWarning()
    {
        waitFor(()-> Locators.warningAlertLoc.existsIn(getDriver()),
                "the warning alert did not appear as expected", 1000);
        return  warningAlert().getText();
    }
    public WebElement warningAlert()
    {
        return Locators.warningAlertLoc.existsIn(getDriver()) ? Locators.warningAlertLoc.findElement(getDriver()) : null;
    }

    public String waitForInfo()
    {
        waitFor(()-> Locators.infoAlertLoc.existsIn(getDriver()),
                "the info alert did not appear as expected", 1000);
        return  infoAlert().getText();
    }
    public WebElement infoAlert()
    {
        return Locators.infoAlertLoc.existsIn(getDriver()) ? Locators.infoAlertLoc.findElement(getDriver()) : null;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DomainFormPanel firstDomainFormPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())   // for situations where there's only one on the page
                .findWhenNeeded(this);                                                          // and the caller is too lazy to specify which one they want

        DomainFormPanel domainFormPanel(String domainName)                                              // for situations with multiple domainformpanels on the same page
        {
            return new DomainFormPanel.DomainFormPanelFinder(getDriver())
                    .withTitle("Field Properties - " + domainName).findWhenNeeded(this);
        }
        WebElement saveAndFinishButton = Locators.domainDesignerButton("Save And Finish")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement saveButton = Locator.button("Save")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelBtn = Locators.domainDesignerButton("Cancel")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

    public static class Locators
    {
        static public Locator.XPathLocator domainDesignerButton(String text)
        {
            return Locator.tagWithClass("button", "btn-success-default").withText(text);
        }

        static public Locator infoAlertLoc = Locator.tagWithClass("div", "alert-info");
        static public Locator successAlertLoc = Locator.tagWithClass("div", "alert-success");
        static public Locator dangerAlertLoc = Locator.tagWithClass("div", "alert-danger");
        static public Locator warningAlertLoc = Locator.tagWithClass("div", "alert-warning");
    }
}
