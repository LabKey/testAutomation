package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Wraps the functionality of the LabKey ui component defined in domainproperties/CollapsiblePanelHeader.tsx
 * Subclasses should add panel-specific functionality.
 */
public abstract class DomainPanel<EC extends DomainPanel<EC, T>.ElementCache, T extends DomainPanel<EC, T>> extends WebDriverComponent<EC>
{
    private final WebElement el;
    private final WebDriver driver;

    protected DomainPanel(WebElement element, WebDriver driver)
    {
        el = element;
        this.driver = driver;
    }

    public DomainPanel(DomainPanel<?, ?> panel)
    {
        this(panel.getComponentElement(), panel.getDriver());
    }

    protected abstract T getThis();

    @Override
    public WebElement getComponentElement()
    {
        return el;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    public T expand()
    {
        return setExpanded(true);
    }

    public T collapse()
    {
        return setExpanded(false);
    }

    protected T setExpanded(boolean expand)
    {
        if (isExpanded() != expand)
        {
            getWrapper().scrollIntoView(elementCache().expandToggle, true);
            elementCache().expandToggle.click();
            getWrapper().shortWait()
                    .until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
        }
        return getThis();
    }

    public WebElement getStatusIcon()
    {
        return elementCache().headerStatusIcon;
    }

    public boolean isExpanded()
    {
        return elementCache().panelBody.isDisplayed();
    }

    public String getPanelTitle()
    {
        return elementCache().panelTitle.getText();
    }

    /**
     * Fetch the text of '.domain-panel-header-fields-defined'.
     * This will usually indicate the number of fields defined in the domain.
     * @return Text in the field count or 'null' if the element doesn't exist.
     */
    public String getFieldCountMessage()
    {
        return elementCache().getHeaderFieldCount().map(WebElement::getText).orElse(null);
    }

    @Override
    protected abstract EC newElementCache();

    public abstract class ElementCache extends Component<EC>.ElementCache
    {
        protected final WebElement expandToggle = Locator.css(".domain-form-expand-btn")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        protected final WebElement headerStatusIcon = Locator.css(".domain-panel-status-icon > svg").findWhenNeeded(this);
        protected final WebElement panelTitle = panelTitleLocator.findWhenNeeded(this);
        protected final WebElement panelBody = Locator.byClass("panel-body").findWhenNeeded(this);

        protected final Optional<WebElement> getHeaderFieldCount()
        {
            return Locator.byClass("domain-panel-header-fields-defined").findOptionalElement(this);
        }
    }

    private static final Locator.XPathLocator panelLocator = Locator.tagWithClass("div", "domain-form-panel");
    private static final Locator.XPathLocator panelTitleLocator = Locator.byClass("domain-panel-title");

    protected abstract static class BaseDomainPanelFinder<P extends DomainPanel, F extends BaseDomainPanelFinder<P, F>> extends WebDriverComponentFinder<P, F>
    {
        private Locator.XPathLocator titleLoc = panelTitleLocator;

        protected BaseDomainPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public F withTitle(String titleStartsWith)
        {
            this.titleLoc = panelTitleLocator.startsWith(titleStartsWith);
            return getThis();
        }

        @Override
        protected Locator locator()
        {
            return panelLocator.withDescendant(titleLoc);
        }
    }

    public static class DomainPanelFinder extends BaseDomainPanelFinder<DomainPanel, DomainPanelFinder>
    {
        public DomainPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected DomainPanelImpl construct(WebElement el, WebDriver driver)
        {
            return new DomainPanelImpl(el, driver);
        }
    }

    private static final class DomainPanelImpl extends DomainPanel
    {
        private DomainPanelImpl(WebElement element, WebDriver driver)
        {
            super(element, driver);
        }

        @Override
        protected DomainPanelImpl getThis()
        {
            return this;
        }

        @Override
        protected DomainPanel.ElementCache newElementCache()
        {
            return new DomainPanel.ElementCache(){};
        }
    }
}
