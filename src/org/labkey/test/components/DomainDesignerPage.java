package org.labkey.test.components;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.domain.DomainPanel;
import org.labkey.test.components.domain.UnsavedChangesModalDialog;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class DomainDesignerPage extends BaseDomainDesigner<DomainDesignerPage.ElementCache>
{
    public DomainDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public static DomainDesignerPage beginAt(WebDriverWrapper driver, String containerPath, String schema, String query)
    {
        driver.beginAt(WebTestHelper.buildURL("core", containerPath, "domainDesigner", Maps.of("schemaName", schema, "queryName", query)));
        return new DomainDesignerPage(driver.getDriver());
    }

    public void clickFinish()
    {
        clickSave();
    }

    public UnsavedChangesModalDialog clickCancelWithUnsavedChanges()
    {
        elementCache().cancelButton.click();
        UnsavedChangesModalDialog unsavedChangesModal = new UnsavedChangesModalDialog(
                new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Keep unsaved changes?"));
        return unsavedChangesModal;
    }

    // this will return the first domain fields panel if there are multiple on the page
    // if you are looking for a specific one, use the fieldsPanel(title) helper
    public DomainFormPanel fieldsPanel()
    {
        getWrapper().waitForElementToDisappear(Locator.tagWithText("span", "Loading..."));
        return elementCache().firstDomainFormPanel;
    }

    public DomainFormPanel expandFieldsPanel(String title)
    {
        return elementCache().domainFormPanel(title)
                .expand();
    }

    /**
     * Get a list of the Domain Panels on this page.
     * @return List of DomainFormElement
     */
    public List<DomainPanel> getPanels()
    {
        return new DomainPanel.DomainPanelFinder(getDriver()).findAll();
    }

    /**
     * Get the titles of the panels on this page.
     * @return List of strings with the panel titles.
     */
    public List<String> getPanelTitles()
    {
        waitFor(() -> getPanels().size() > 0, "No form panels found on page.", WAIT_FOR_JAVASCRIPT);
        List<String> titles = new ArrayList<>();
        for(DomainPanel<?,?> formPanel : getPanels())
        {
            titles.add(formPanel.getPanelTitle());
        }
        return titles;
    }

    public String waitForError()
    {
        WebDriverWrapper.waitFor(()-> BootstrapLocators.errorBanner.existsIn(getDriver()),
                "the error alert did not appear as expected", 1000);
        return  errorAlert().getText();
    }

    public WebElement errorAlert()
    {
        return BootstrapLocators.errorBanner.existsIn(getDriver()) ? BootstrapLocators.errorBanner.findElement(getDriver()) : null;
    }

    public String waitForWarning()
    {
        WebDriverWrapper.waitFor(()-> BootstrapLocators.warningBanner.existsIn(getDriver()),
                "the warning alert did not appear as expected", 1000);
        return  warningAlert().getText();
    }

    public WebElement warningAlert()
    {
        return BootstrapLocators.warningBanner.existsIn(getDriver()) ? BootstrapLocators.warningBanner.findElement(getDriver()) : null;
    }

    public String waitForInfo()
    {
        WebDriverWrapper.waitFor(()-> BootstrapLocators.infoBanner.existsIn(getDriver()),
                "the info alert did not appear as expected", 1000);
        return  infoAlert().getText();
    }

    public WebElement infoAlert()
    {
        return BootstrapLocators.infoBanner.existsIn(getDriver()) ? BootstrapLocators.infoBanner.findElement(getDriver()) : null;
    }

    public String waitForAnyAlert()
    {
        WebElement alert = Locator.waitForAnyElement(getWrapper().shortWait(),
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

    protected class ElementCache extends BaseDomainDesigner<ElementCache>.ElementCache
    {
        DomainFormPanel firstDomainFormPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())   // for situations where there's only one on the page
                .timeout(WAIT_FOR_JAVASCRIPT)
                .findWhenNeeded(this);

        DomainFormPanel domainFormPanel(String title) // for situations with multiple domainformpanels on the same page
        {
            return new DomainFormPanel.DomainFormPanelFinder(getDriver()).withTitle(title)
                    .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        }

    }
}
