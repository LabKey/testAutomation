package org.labkey.test.components.domain;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.function.Consumer;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/BaseDomainDesigner.tsx
 * Defines standard save, cancel, and error handling
 */
public abstract class BaseDomainDesigner<EC extends BaseDomainDesigner.ElementCache> extends WebDriverComponent<EC>
{
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

    public Object clickCancel()
    {
        clickCancel(true);
        return null;
    }

    protected final void clickCancel(boolean expectPageLoad)
    {
        if (expectPageLoad)
        {
            getWrapper().doAndWaitForPageToLoad(() -> elementCache().cancelButton.click());
        }
        else
        {
            elementCache().cancelButton.click();
        }
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public Object clickSave()
    {
        clickSave(true);
        return null;
    }

    protected final void clickSave(boolean expectPageLoad)
    {
        if (expectPageLoad)
        {
            getWrapper().doAndWaitForPageToLoad(() -> elementCache().saveButton.click());
        }
        else
        {
            elementCache().saveButton.click();
        }
    }

    public List<String> clickSaveExpectingErrors()
    {
        elementCache().saveButton.click();
        return getWrapper().getTexts(BootstrapLocators.errorBanner.waitForElements(getWrapper().shortWait()));
    }

    @Override
    protected abstract EC newElementCache();

    public abstract class ElementCache extends Component<EC>.ElementCache
    {
        protected final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);
        protected final WebElement saveButton = Locator.css(".domain-designer-buttons > .pull-right").findWhenNeeded(this);
    }
}
