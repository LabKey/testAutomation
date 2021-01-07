package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

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

    public ReactAssayDesignerPage selectStandardAssay()
    {
        click(elementCache().stdAssayTab);
        // TODO: Verify this clickAndWait works in app environment
        clickAndWait(elementCache().selectButton);
        return new ReactAssayDesignerPage(getDriver());
    }

    public ReactAssayDesignerPage selectAssayType(String name)
    {
        if (name.equals("General") || name.equals("Standard")) {
            return selectStandardAssay();
        }

        click(elementCache().specialtyAssayTab);
        waitForElementToBeVisible(elementCache().specialtySelectLocator);
        selectOptionByText(elementCache().specialtySelect, name);
        // TODO: Verify this clickAndWait works in app environment
        clickAndWait(elementCache().selectButton);
        return new ReactAssayDesignerPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final WebElement buttonPanel = buttonPanelLocator().findWhenNeeded(this);
        public final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(buttonPanel);
        public final WebElement selectButton = Locator.byClass("pull-right").findWhenNeeded(buttonPanel);

        public final WebElement specialtyPanel = Locator.tagWithId("div", "assay-picker-tabs-pane-specialty").findWhenNeeded(this);

        public final WebElement stdAssayTab = Locator.tagContainingText("a", "Standard Assay").findWhenNeeded(this);
        public final WebElement specialtyAssayTab = Locator.tagContainingText("a", "Specialty Assays").findWhenNeeded(this);
        public final WebElement importAssayTab = Locator.tagContainingText("a", "Import Assay Design").findWhenNeeded(this);

        public final WebElement containerSelect = Locator.tagWithId("select", "assay-type-select-container").findWhenNeeded(this);

        public final Locator specialtySelectLocator = Locator.tagWithId("select", "specialty-assay-type-select");
        public final WebElement specialtySelect = specialtySelectLocator.findWhenNeeded(specialtyPanel);

        protected Locator.XPathLocator buttonPanelLocator()
        {
            return Locator.byClass("assay-type-select-btns");
        }
    }
}
