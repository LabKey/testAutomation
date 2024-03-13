package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.react.Tabs;
import org.labkey.test.components.ui.files.FileUploadPanel;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
        waitFor(() -> elementCache().authTabs.getTabText().size() > 1, WAIT_FOR_PAGE);
    }


    public ReactAssayDesignerPage selectStandardAssay()
    {
        elementCache().authTabs.selectTab("Standard Assay");

        clickSelectButton();
        return new ReactAssayDesignerPage(getDriver());
    }

    public ReactAssayDesignerPage selectAssayType(String name)
    {
        if (name.equals("General") || name.equals("Standard")) {
            return selectStandardAssay();
        }

        WebElement activeTab = elementCache().authTabs.selectTab("Specialty Assays");

        WebElement specialtySelect = Locator.id("specialty-assay-type-select").findWhenNeeded(activeTab);
        shortWait().until(ExpectedConditions.visibilityOf(specialtySelect));
        selectOptionByText(specialtySelect, name);

        waitFor(()->
           elementCache().selectButton.getText().toLowerCase().contains(name.toLowerCase()),
            String.format("took too long for the select button text to contain the assay name [%s].", name), 2000);

        clickSelectButton();
        return new ReactAssayDesignerPage(getDriver());
    }

    public ChooseAssayTypePage goToImportAssayDesignTab()
    {
        elementCache().authTabs.selectTab("Import Assay Design");
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

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        public final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(getDriver());
        // selectButton's text changes depending upon which assay is selected- it can be 'Choose Standard Assay' or 'Choose x Assay'
        public final WebElement selectButton = Locator.tagWithClass("button", "pull-right").findWhenNeeded(getDriver());

        public final Tabs authTabs = new Tabs.TabsFinder(getDriver()).locatedBy(Locator.byClass("lk-tabs")).findWhenNeeded(this);

    }
}
