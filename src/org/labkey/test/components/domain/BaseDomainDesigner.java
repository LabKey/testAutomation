package org.labkey.test.components.domain;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/BaseDomainDesigner.tsx
 * Defines standard save, cancel, and error handling
 */
public abstract class BaseDomainDesigner<EC extends BaseDomainDesigner.ElementCache> extends WebDriverComponent<EC>
{
    public static final String RESERVED_FIELDS_WARNING_PREFIX = "Fields with reserved names or prefixes found in your file are not shown below. ";
    private final WebElement el;
    private final WebDriver driver;

    protected BaseDomainDesigner(WebDriver driver)
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

    public boolean isCancelButtonEnabled()
    {
        return elementCache().cancelButton.isEnabled();
    }

    /**
     * Click cancel button. Subclasses should override to return a particular destination page page or if a page load is
     * not expected.
     * @return Varies by designer and context. 'null' by default
     */
    public Object clickCancel()
    {
        getWrapper().clickAndWait(elementCache().cancelButton);
        return null;
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    /**
     * Save changes to domain. Subclasses should override to return a particular destination page or if a page load is
     * not expected.
     * @return Varies by designer and context. 'null' by default
     */
    public Object clickSave()
    {
        getWrapper().clickAndWait(elementCache().saveButton);
        return null;
    }

    public List<String> clickSaveExpectingErrors()
    {
        elementCache().saveButton.click();
        return getWrapper().getTexts(BootstrapLocators.errorBanner.waitForElements(getWrapper().shortWait())).stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    protected abstract EC newElementCache();

    public abstract class ElementCache extends Component<EC>.ElementCache
    {
        protected final WebElement buttonPanel = buttonPanelLocator().findWhenNeeded(this);
        public final WebElement cancelButton = Locator.byClass("cancel-button").findWhenNeeded(buttonPanel);
        public final WebElement saveButton = saveButtonLocator().findWhenNeeded(buttonPanel);

        protected Locator.XPathLocator saveButtonLocator()
        {
            return Locator.byClass("save-button");
        }

        protected Locator.XPathLocator buttonPanelLocator()
        {
            return Locator.byClass("form-buttons");
        }
    }
}
