package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

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

    public boolean showsNoSearchResultsFound()
    {
        return Locator.tagWithClass("div", "col").withText("No search results found.")
                .existsIn(elementCache().resultContainer());
    }

    public boolean isResultContainerExpanded()
    {
        try
        {
            return elementCache().resultContainer().isDisplayed();
        } catch (NoSuchElementException nse)
        {
            return false;
        }
    }

    public OntologyTreeSearch setInput(String value)
    {
        clearInput();
        elementCache().searchInput.set(value);
        return this;
    }

    /**
     * sets the input text to empty and waits for the search results container to collapse
     * and for the placeholder to become visible.
     * Clearing the search results in this manner (and ensuring that they are not present) is helpful to ensure
     * that subsequent calls to waitForResults won't find the last set as they go stale
     * @return
     */
    public OntologyTreeSearch clearInput()
    {
        var input = elementCache().searchInput;
        String placeholder = input.getComponentElement().getAttribute("placeholder");
        getWrapper().actionClear(elementCache().searchInput.getComponentElement());
        // wait for the results container to collapse, and for the placeholder for the input to be shown
        getWrapper().waitFor(()-> !isResultContainerExpanded() &&
                Locator.css("input:placeholder-shown").withAttribute("placeholder", placeholder)
                        .existsIn(this),  2000);
        return this;
    }

    public List<TreeSearchResult> waitForResults()
    {
        getWrapper().waitFor(()-> isResultContainerExpanded() && getSearchResults().size() > 0 || showsNoSearchResultsFound()
                , WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return getSearchResults();
    }

    private List<TreeSearchResult> getSearchResults()
    {
        return new TreeSearchResult.TreeSearchResultFinder(getDriver()).findAll(elementCache().resultContainer());
    }

    /**
     * when more search hits exist than are shown (max of 20 can be shown), an added search result footer element
     * will appear with text advising the user to refine their search
     * @return
     */
    Optional<WebElement> searchResultFooterElement()
    {
        return Locator.tagWithClass("div", "result-footer").findOptionalElement(elementCache().resultContainer());
    }

    public String getSearchResultFooterText()
    {
        getWrapper().waitFor(()-> searchResultFooterElement().isPresent(),
                "the search footer did not appear", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return searchResultFooterElement().get().getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final Input searchInput = Input.Input(Locator.tagWithClass("input", "form-control"), getDriver())
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
        private final String _title = null;

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
