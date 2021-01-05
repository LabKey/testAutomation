package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AssayTypeSelectPage<EC extends AssayTypeSelectPage.ElementCache> extends LabKeyPage<EC>
{
    public AssayTypeSelectPage(WebDriver driver)
    {
        super(driver);
    }

    public ReactAssayDesignerPage selectStandardAssay()
    {
        click(elementCache().stdAssayTab);
        // TODO: Verify this clickAndWait works in app environment
        clickAndWait(elementCache().saveButton);
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
        clickAndWait(elementCache().saveButton);
        return new ReactAssayDesignerPage(getDriver());
    }

    protected EC newElementCache()
    {
        return (EC) new AssayTypeSelectPage.ElementCache();
    }

    public class ElementCache extends LabKeyPage.ElementCache
    {
        protected final WebElement buttonPanel = buttonPanelLocator().findWhenNeeded(this);
        public final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(buttonPanel);
        public final WebElement saveButton = Locator.byClass("pull-right").findWhenNeeded(buttonPanel);

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
