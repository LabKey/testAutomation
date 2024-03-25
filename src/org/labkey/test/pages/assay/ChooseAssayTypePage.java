package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ui.files.FileUploadPanel;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class ChooseAssayTypePage extends LabKeyPage<ChooseAssayTypePage.ElementCache>
{
    public ChooseAssayTypePage(WebDriver driver)
    {
        super(driver);
    }

    public static ChooseAssayTypePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ChooseAssayTypePage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "chooseAssayType"));
        return new ChooseAssayTypePage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitFor(() -> elementCache().stdAssayTabLocator().existsIn(getDriver()) &&
                elementCache().specialtyAssayTabLocator().existsIn(getDriver()), WAIT_FOR_PAGE);
    }


    public ReactAssayDesignerPage selectStandardAssay()
    {
        elementCache().stdAssayTab.click();

        WebElement standardPanel = elementCache().stdAssayPaneLocator().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        waitFor(()-> standardPanel.getAttribute("class") != null &&
                standardPanel.getAttribute("class").contains("active"),
                "took too long to select the standard assay panel", 2000);

        clickSelectButton();
        return new ReactAssayDesignerPage(getDriver());
    }

    public ReactAssayDesignerPage selectAssayType(String name)
    {
        if (name.equals("General") || name.equals("Standard")) {
            return selectStandardAssay();
        }

        elementCache().specialtyAssayTab.click();
        WebElement specialtyPanel = elementCache().specialtyAssayPaneLocator().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        waitFor(()-> specialtyPanel.getAttribute("class") != null &&
                        specialtyPanel.getAttribute("class").contains("active"),
                "took too long to select the specialty assay panel", 2000);

        waitForElementToBeVisible(elementCache().specialtySelectLocator);
        selectOptionByText(elementCache().specialtySelect, name);

        waitFor(()->
           elementCache().selectButton.getText().toLowerCase().contains(name.toLowerCase()),
            String.format("took too long for the select button text to contain the assay name [%s].", name), 2000);

        clickSelectButton();
        return new ReactAssayDesignerPage(getDriver());
    }

    public ChooseAssayTypePage selectAssayLocation(String value)
    {
        elementCache().stdAssayTab.click();
        selectOptionByText(elementCache().containerSelect, value);
        return this;
    }

    public ChooseAssayTypePage goToImportAssayDesignTab()
    {
        elementCache().importAssayTab.click();
        return this;
    }

    public void uploadXarFile(File xar)
    {
        goToImportAssayDesignTab();
        var uploadPanel = new FileUploadPanel.FileUploadPanelFinder(getDriver()).waitFor();
        uploadPanel.uploadFile(xar);
        clickButton("Import");
    }

    protected void clickSelectButton()
    {
        clickAndWait(elementCache().selectButton);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        public final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(getDriver());
        // selectButton's text changes depending upon which assay is selected- it can be 'Choose Standard Assay' or 'Choose x Assay'
        public final WebElement selectButton = Locator.tagWithClass("button", "pull-right").findWhenNeeded(getDriver());

        public final WebElement specialtyPanel = Locator.tagWithId("div", "assay-picker-tabs-pane-specialty").findWhenNeeded(this);

        public final WebElement stdAssayTab = stdAssayTabLocator().findWhenNeeded(this);
        public final WebElement specialtyAssayTab = specialtyAssayTabLocator().findWhenNeeded(this);
        public final WebElement importAssayTab = Locator.tagContainingText("a", "Import Assay Design").findWhenNeeded(this);

        public final WebElement containerSelect = Locator.tagWithId("select", "assay-type-select-container").findWhenNeeded(this);
        public final Locator specialtySelectLocator = Locator.tagWithId("select", "specialty-assay-type-select");
        public final WebElement specialtySelect = specialtySelectLocator.findWhenNeeded(specialtyPanel);

        protected Locator.XPathLocator buttonPanelLocator()
        {
            return Locator.byClass("assay-designer-section");
        }
        public Locator.XPathLocator stdAssayTabLocator()
        {
            return Locator.id("assay-picker-tabs-tab-standard");
        }
        public Locator.XPathLocator stdAssayPaneLocator()
        {
            return Locator.id("assay-picker-tabs-pane-standard");
        }
        public Locator.XPathLocator specialtyAssayTabLocator()
        {
            return Locator.id("assay-picker-tabs-tab-specialty");
        }
        public Locator.XPathLocator specialtyAssayPaneLocator()
        {
            return Locator.id("assay-picker-tabs-pane-specialty");
        }
    }
}
