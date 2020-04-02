package org.labkey.test.components.domain;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/BaseDomainDesigner.tsx
 */
public abstract class BaseDomainDesigner<EC extends BaseDomainDesigner<EC>.ElementCache> extends WebDriverComponent<EC>
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

    public void clickCancel()
    {
        elementCache().cancelButton.click();
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public void clickSave()
    {
        elementCache().saveButton.click();
    }

    public List<WebElement> clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.waitForElements(getWrapper().shortWait());
    }

    @Override
    protected abstract EC newElementCache();

    protected class ElementCache extends Component<EC>.ElementCache
    {
        protected final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);
        protected final WebElement saveButton = Locator.css(".domain-designer-buttons > .btn-primary").findWhenNeeded(this);

    }
}
