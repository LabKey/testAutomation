package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ReactAssayTypeSelectPage<EC extends ReactAssayTypeSelectPage.ElementCache> extends WebDriverComponent<EC>
{
    private final WebElement el;
    private final WebDriver driver;

    public ReactAssayTypeSelectPage(WebDriver driver)
    {
        this.driver = driver;
        el = Locator.id("app").findElement(this.driver); // Full page component
    }

    @Override
    public WebElement getComponentElement()
    {
        return el;
    }

    @Override
    public WebDriver getDriver()
    {
        return driver;
    }

    public ReactAssayDesignerPage selectStandardAssay()
    {
        getWrapper().click(elementCache().stdAssayTab);
        getWrapper().clickAndWait(elementCache().saveButton);
        return new ReactAssayDesignerPage(getDriver());
    }

    public ReactAssayDesignerPage selectAssayType(String name)
    {
        if (name.equals("General") || name.equals("Standard")) {
            return selectStandardAssay();
        }

        getWrapper().clickAndWait(elementCache().specialtyAssayTab);
        getWrapper().selectOptionByText(elementCache().specialtySelect, name);
        getWrapper().clickAndWait(elementCache().saveButton);
        return new ReactAssayDesignerPage(getDriver());
    }

    @Override
    protected EC elementCache()
    {
        return super.elementCache();
    }

    @Override
    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    public class ElementCache extends Component<EC>.ElementCache
    {
        protected final WebElement buttonPanel = buttonPanelLocator().findWhenNeeded(this);
        public final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(buttonPanel);
        public final WebElement saveButton = Locator.byClass("pull-right").findWhenNeeded(buttonPanel);

        public final WebElement stdAssayTab = Locator.tagContainingText("a", "Standard Assay").findWhenNeeded(this);
        public final WebElement specialtyAssayTab = Locator.tagContainingText("a", "Specialty Assays").findWhenNeeded(this);
        public final WebElement importAssayTab = Locator.tagContainingText("a", "Import Assay Design").findWhenNeeded(this);

        public final WebElement containerSelect = Locator.tagWithId("select", "assay-type-select-container").findWhenNeeded(this);
        public final WebElement specialtySelect = Locator.tagWithId("select", "specialty-assay-type-select").findWhenNeeded(this);

        protected Locator.XPathLocator buttonPanelLocator()
        {
            return Locator.byClass("assay-type-select-btns");
        }
    }
}
