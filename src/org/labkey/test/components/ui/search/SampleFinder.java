package org.labkey.test.components.ui.search;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/SampleFinderSection.tsx</code>
 */
public class SampleFinder extends WebDriverComponent<SampleFinder.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected SampleFinder(WebDriver driver)
    {
        _el = Locator.byClass("g-section")
                .withDescendant(Locator.byClass("panel-content-title-large").withText("Find Samples"))
                .waitForElement(getDriver(), 10_000);
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public EntityFieldFilterModal addEntityParentProperties(String parentNoun)
    {
        elementCache().findAddParentButton(parentNoun).click();
        return new EntityFieldFilterModal(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement panelHeading = Locator.byClass("panel-heading").findWhenNeeded(this);

        WebElement findAddParentButton(String parentNoun)
        {
            return BootstrapLocators.button(" " + parentNoun + " Properties")
                    .withChild(Locator.byClass("container--addition-icon")).findElement(panelHeading);
        }


    }
}
