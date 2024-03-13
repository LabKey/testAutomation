package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.Locator.XPathLocator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.Tabs;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ConceptInfoTabs extends WebDriverComponent<ConceptInfoTabs.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected ConceptInfoTabs(WebElement element, WebDriver driver)
    {
        _el = element;
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

    public WebElement showOverview()
    {
        return elementCache().conceptTabs.selectTab("Overview");
    }

    public WebElement showPathInformation()
    {
        return elementCache().conceptTabs.selectTab("Path Information");
    }

    /**
     * If no concept is selected, the 'none selected' element will be present.
     * If a concept is selected, returns the text in the title element
     * @return The text of the title element, or that of the 'none selected' element if none is selected
     */
    public String getTitle()
    {
        return elementCache().getTitle(showOverview());
    }

    public String getCode()
    {
        return Locator.tagWithClass("span", "code").findElement(showOverview()).getText();
    }

    public List<String> getSynonyms()
    {
        return getWrapper().getTexts(elementCache().synonyms(showOverview()));
    }

    public List<String> getSelectedPath()
    {
        return getWrapper().getTexts(elementCache().conceptPathParts(showPathInformation()));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final Tabs conceptTabs = new Tabs(getComponentElement(), getDriver());

        // overview pane items

        List<WebElement> synonyms(WebElement overviewPane)
        {
            return Locator.tagWithClass("ul", "synonyms-text").child(Locator.tag("li"))
                    .findElements(overviewPane);
        }

        String getTitle(WebElement overviewPane)
        {
            return XPathLocator.union(
                            Locator.byClass("none-selected"), // if no concept is selected, this element will be shown instead of the 'title' element
                            Locator.byClass("title"))
                    .findElement(overviewPane).getText();
        }

        // path info pane items
        List<WebElement> conceptPathParts(WebElement pathInformationPane)
        {
            return Locator.tagWithClass("div", "concept-path-container").append(Locator.tagWithClass("div", "concept-path")
                    .child(Locator.tagWithClass("span", "concept-path-label"))).findElements(pathInformationPane);
        }

        List<WebElement> alternateConceptPathParts(WebElement pathInformationPane)
        {
            return Locator.tagWithClass("div", "alternate-paths-container")
                    .append(Locator.tagWithClass("div", "concept-path").child(Locator.tagWithClass("span", "concept-path-label")))
                    .findElements(pathInformationPane);
        }
    }


    public static class ConceptInfoTabsFinder extends WebDriverComponentFinder<ConceptInfoTabs, ConceptInfoTabsFinder>
    {
        private final XPathLocator _baseLocator = Locator.id("concept-information-tabs");

        public ConceptInfoTabsFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ConceptInfoTabs construct(WebElement el, WebDriver driver)
        {
            return new ConceptInfoTabs(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
