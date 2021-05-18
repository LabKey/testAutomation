package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * wraps ontologyTreeSearchContainer.tsx
 */
public class OntologyTreeSearch extends WebDriverComponent<OntologyTreeSearch.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected OntologyTreeSearch(WebElement element, WebDriver driver)
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

    public OntologyTreeSearch selectItemWithCode(String searchExpression, String code)
    {
        setInput(searchExpression);
        TreeSearchResult searchResult = new TreeSearchResult.TreeSearchResultFinder(getDriver())
                .withCode(code)
                .waitFor(elementCache().resultContainer());
        searchResult.select();
        return this;
    }

    public OntologyTreeSearch selectItemWithLabel(String searchExpression, String label)
    {
        setInput(searchExpression);
        TreeSearchResult searchResult = new TreeSearchResult.TreeSearchResultFinder(getDriver())
                .withLabel(label)
                .waitFor(elementCache().resultContainer());
        searchResult.select();
        return this;
    }

    private OntologyTreeSearch setInput(String value)
    {
        elementCache().searchInput.set(value);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final Input searchInput = Input.Input(Locator.input("concept-search"), getDriver())
                .timeout(2000).findWhenNeeded(this);

        WebElement resultContainer()
        {
            return Locator.tag("div").withChild(Locator.tagWithClass("ul", "result-menu"))
                    .waitForElement(this, 2000);
        }

        public List<TreeSearchResult> searchHits()
        {
            return new TreeSearchResult.TreeSearchResultFinder(getDriver()).findAll(resultContainer());
        }

        final Locator noSearchResults = Locator.tag("div").withChild(Locator.tagWithText("div", "No search results found."));
    }

    public static class OntologyTreeSearchFinder extends WebDriverComponentFinder<OntologyTreeSearch, OntologyTreeSearchFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "concept-search-container");
        private String _title = null;

        public OntologyTreeSearchFinder(WebDriver driver)
        {
            super(driver);
        }


        @Override
        protected OntologyTreeSearch construct(WebElement el, WebDriver driver)
        {
            return new OntologyTreeSearch(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
