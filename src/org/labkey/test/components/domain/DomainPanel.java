package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps the functionality of the LabKey ui component defined in domainproperties/CollapsiblePanelHeader.tsx
 * Subclasses should add panel-specific functionality.
 */
public abstract class DomainPanel<EC extends DomainPanel<?, ?>.ElementCache, T extends DomainPanel<?, T>> extends WebDriverComponent<EC>
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
        if (!isExpanded())
        {
            elementCache().expandToggle.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
        }
        return getThis();
    }

    public T collapse()
    {
        if (isExpanded())
        {
            elementCache().expandToggle.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
        }
        return getThis();
    }

    public boolean isExpanded()
    {
        return elementCache().panelBody.isDisplayed();
    }

    public boolean hasPanelTitle()
    {
        return elementCache().panelTitleLoc.existsIn(this);
    }

    public String getPanelTitle()
    {
        return hasPanelTitle() ? elementCache().panelTitle.getText() : null;
    }

    @Override
    protected abstract EC newElementCache();

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected final WebElement expandToggle = Locator.tagWithClass("svg", "domain-form-expand-btn").findWhenNeeded(this);
        protected final Locator.XPathLocator panelTitleLoc = Locator.tagWithClass("span", "domain-panel-title");
        protected final WebElement panelTitle = panelTitleLoc.findWhenNeeded(this);
        protected final WebElement panelBody = Locator.byClass("panel-body").findWhenNeeded(this);
    }

    protected abstract static class BaseDomainPanelFinder<P extends DomainPanel<?, ?>, F extends BaseDomainPanelFinder<P,F>> extends WebDriverComponentFinder<P, F>
    {
        private final Locator.XPathLocator panelLocator = Locator.tagWithClass("div", "domain-form-panel");

        private Locator.XPathLocator titleLoc;

        protected BaseDomainPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public F withTitle(String title)
        {
            this.titleLoc = Locator.byClass("domain-panel-title").startsWith(title);
            return getThis();
        }

        @Override
        protected Locator locator()
        {
            if (titleLoc != null)
            {
                return panelLocator.withDescendant(titleLoc);
            }
            else
            {
                return panelLocator;
            }
        }
    }

    public static class DomainPanelFinder extends BaseDomainPanelFinder<DomainPanelImpl, DomainPanelFinder>
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

    private static final class DomainPanelImpl extends DomainPanel<DomainPanel<?, ?>.ElementCache, DomainPanelImpl>
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
        protected DomainPanel<?, ?>.ElementCache newElementCache()
        {
            return null;
        }
    }
}
