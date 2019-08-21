package org.labkey.test.components;

import org.labkey.test.BootstrapLocators;
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
        String msg = waitForAnyAlert();
        log("Clicking save.  Waited until alert with message ["+msg+"] appeared");
        return this;
    }
    public WebElement saveButton()
    {
        return elementCache().saveButton;
    }

    public void clickSaveAndFinish()
    {
        log("clicking [Save and Finish]");
        clickAndWait(elementCache().saveAndFinishButton);
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
        waitFor(()-> BootstrapLocators.dangerAlertLoc.existsIn(getDriver()),
                "the error alert did not appear as expected", 1000);
        return  errorAlert().getText();
    }
    public WebElement errorAlert()
    {
        return BootstrapLocators.dangerAlertLoc.existsIn(getDriver()) ? BootstrapLocators.dangerAlertLoc.findElement(getDriver()) : null;
    }

    public String waitForWarning()
    {
        waitFor(()-> BootstrapLocators.warningAlertLoc.existsIn(getDriver()),
                "the warning alert did not appear as expected", 1000);
        return  warningAlert().getText();
    }
    public WebElement warningAlert()
    {
        return BootstrapLocators.warningAlertLoc.existsIn(getDriver()) ? BootstrapLocators.warningAlertLoc.findElement(getDriver()) : null;
    }

    public String waitForInfo()
    {
        waitFor(()-> BootstrapLocators.infoAlertLoc.existsIn(getDriver()),
                "the info alert did not appear as expected", 1000);
        return  infoAlert().getText();
    }
    public WebElement infoAlert()
    {
        return BootstrapLocators.infoAlertLoc.existsIn(getDriver()) ? BootstrapLocators.infoAlertLoc.findElement(getDriver()) : null;
    }

    public String waitForAnyAlert()
    {
        WebElement alert = Locator.waitForAnyElement(shortWait(),
                BootstrapLocators.dangerAlertLoc, BootstrapLocators.infoAlertLoc, BootstrapLocators.warningAlertLoc, BootstrapLocators.successAlertLoc);
        return alert.getText();
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

    }
}
