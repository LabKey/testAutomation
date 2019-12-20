package org.labkey.test.components;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.domain.UnsavedChangesModalDialog;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

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

    public void clickFinish()
    {
        scrollIntoView(elementCache().finishButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().finishButton()));
        clickAndWait(elementCache().finishButton());
    }

    public DomainDesignerPage clickFinishExpectingError()
    {
        scrollIntoView(elementCache().finishButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().finishButton()));
        elementCache().finishButton().click();
        waitForError();
        return this;
    }

    public UnsavedChangesModalDialog clickCancel()
    {
        elementCache().cancelBtn().click();
        UnsavedChangesModalDialog unsavedChangesModal = new UnsavedChangesModalDialog(
                new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Keep unsaved changes?"));
        return unsavedChangesModal;
    }

    public DomainDesignerPage clickCancelAndDiscardChanges()
    {
        clickCancel().discardChanges();
        return new DomainDesignerPage(getDriver());
    }

    public boolean isAlertVisible()
    {
        return Locators.alert.findOptionalElement(getDriver()).map(WebElement::isDisplayed).orElse(false);
    }

    public WebElement finishButton()
    {
        return elementCache().finishButton();
    }

    // this will return the first domain fields panel if there are multiple on the page
    // if you are looking for a specific one, use the fieldsPanel(title) helper
    public DomainFormPanel fieldsPanel()
    {
        return elementCache().firstDomainFormPanel;
    }

    public DomainFormPanel fieldsPanel(String title)
    {
        return elementCache().domainFormPanel(title);
    }

    /**
     * Get a list of the Domain Panels on this page.
     * @return List of DomainFormElement
     */
    public List<DomainFormPanel> getPanels()
    {
        return new DomainFormPanel.DomainFormPanelFinder(getDriver()).findAll();
    }

    /**
     * Get the titles of the panels on this page.
     * @return List of strings with the panel titles.
     */
    public List<String> getPanelTitles()
    {
        List<String> titles = new ArrayList<>();
        for(DomainFormPanel formPanel : getPanels())
        {
            titles.add(formPanel.getPanelTitle());
        }
        return titles;
    }

    public String waitForError()
    {
        waitFor(()-> BootstrapLocators.errorBanner.existsIn(getDriver()),
                "the error alert did not appear as expected", 1000);
        return  errorAlert().getText();
    }

    public WebElement errorAlert()
    {
        return BootstrapLocators.errorBanner.existsIn(getDriver()) ? BootstrapLocators.errorBanner.findElement(getDriver()) : null;
    }

    public String waitForWarning()
    {
        waitFor(()-> BootstrapLocators.warningBanner.existsIn(getDriver()),
                "the warning alert did not appear as expected", 1000);
        return  warningAlert().getText();
    }

    public WebElement warningAlert()
    {
        return BootstrapLocators.warningBanner.existsIn(getDriver()) ? BootstrapLocators.warningBanner.findElement(getDriver()) : null;
    }

    public String waitForInfo()
    {
        waitFor(()-> BootstrapLocators.infoBanner.existsIn(getDriver()),
                "the info alert did not appear as expected", 1000);
        return  infoAlert().getText();
    }

    public WebElement infoAlert()
    {
        return BootstrapLocators.infoBanner.existsIn(getDriver()) ? BootstrapLocators.infoBanner.findElement(getDriver()) : null;
    }

    public String waitForAnyAlert()
    {
        WebElement alert = Locator.waitForAnyElement(shortWait(),
                BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
        return alert.getText();
    }

    public String anyAlert()
    {
        WebElement alert = Locator.findAnyElementOrNull(getDriver(),
                BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
        if (alert !=null)
            return alert.getText();
        else
            return null;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DomainFormPanel firstDomainFormPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())   // for situations where there's only one on the page
                .timeout(WAIT_FOR_JAVASCRIPT)
                .findWhenNeeded(this);                                                          // and the caller is too lazy to specify which one they want

        DomainFormPanel domainFormPanel(String title) // for situations with multiple domainformpanels on the same page
        {
            return new DomainFormPanel.DomainFormPanelFinder(getDriver()).withTitle(title).findWhenNeeded(this);
        }                                                     // and the caller is too lazy to specify which one they want

        WebElement finishButton()
        {
            return Locator.button("Save")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }

        WebElement cancelBtn()
        {
            return Locator.button("Cancel")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }
    }

    public static class Locators
    {
        static public Locator.XPathLocator domainDesignerButton(String text)
        {
            return Locator.tagWithClass("button", "btn-success-default").withText(text);
        }

        static public Locator alert = Locator.tagWithClass("div" , "alert");
    }
}
