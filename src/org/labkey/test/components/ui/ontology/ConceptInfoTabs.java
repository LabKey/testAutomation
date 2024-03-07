package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

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

    private boolean isOverviewDisplayed()
    {
        return "false".equals(elementCache().overviewPane.getAttribute("aria-hidden")) &&
                "true".equals(elementCache().pathInformationPane.getAttribute("aria-hidden"));
    }

    public ConceptInfoTabs showOverview()
    {
        if (!isOverviewDisplayed())
        {
            elementCache().overviewTab.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(elementCache().overviewPane));
            WebDriverWrapper.waitFor(() -> isOverviewDisplayed(),
                    "the overview pane did not become enabled", 2000);
        }
        return this;
    }

    public ConceptInfoTabs showPathInformation()
    {
        if (isOverviewDisplayed())
        {
            elementCache().pathInformationTab.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(elementCache().overviewPane));
            WebDriverWrapper.waitFor(() -> !isOverviewDisplayed(),
                    "the path information pane did not become enabled", 2000);
        }
        return this;
    }

    /**
     * If no concept is selected, the 'none selected' element will be present.
     * If a concept is selected, returns the text in the title element
     * @return The text of the title element, or that of the 'none selected' element if none is selected
     */
    public String getTitle()
    {
        showOverview();
        if (elementCache().noneSelectedElement().isPresent())
            return elementCache().noneSelectedElement().get().getText();
        return elementCache().title.getText();
    }

    public String getCode()
    {
        showOverview();
        return elementCache().codeBox.getText();
    }

    public List<String> getSynonyms()
    {
        showOverview();
        return  getWrapper().getTexts(elementCache().synonyms());
    }

    public List<String> getSelectedPath()
    {
        showPathInformation();
        return getWrapper().getTexts(elementCache().conceptPathParts());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        // navigation tabs
        WebElement tabListContainer = Locator.tagWithClass("ul", "nav-tabs")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement overviewTab = Locator.tagWithAttributeContaining("a", "role", "tab")
                .withText("Overview")
                .findWhenNeeded(tabListContainer);
        WebElement pathInformationTab = Locator.tagWithAttributeContaining("a", "role", "tab")
                .withText("Path Information")
                .findWhenNeeded(tabListContainer);

        // panes
        WebElement tabContentContainer = Locator.tagWithClass("div", "tab-content")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        // overview pane
        WebElement overviewPane = Locator.byClass("ontology-concept-overview-container")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement title = Locator.tagWithClass("div", "title").findWhenNeeded(overviewPane);
        WebElement codeBox = Locator.tagWithClass("span", "code").findWhenNeeded(overviewPane);
        List<WebElement> synonyms()
        {
            return Locator.tagWithClass("ul", "synonyms-text").child(Locator.tag("li"))
                    .findElements(overviewPane);
        }

        // if no concept is selected, this element will be shown instead of the 'title' element
        Optional<WebElement> noneSelectedElement()
        {
            return Locator.tagWithClass("div", "none-selected").findOptionalElement(overviewPane);
        }

        // path info pane
        WebElement pathInformationPane = Locator.byClass("ontology-concept-pathinfo-container")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement conceptPathContainer()
        {
            return Locator.tagWithClass("div", "concept-path-container")
                    .findWhenNeeded(pathInformationPane);
        }
        List<WebElement> conceptPathParts()
        {
            return Locator.tagWithClass("div", "concept-path")
                    .child(Locator.tagWithClass("span", "concept-path-label")).findElements(conceptPathContainer());
        }

        WebElement alternatePathContainer()
        {
            return Locator.tagWithClass("div", "alternate-paths-container")
                    .findWhenNeeded(pathInformationPane);
        }
        List<WebElement> alternateConceptPathParts()
        {
            return Locator.tagWithClass("div", "concept-path")
                    .child(Locator.tagWithClass("span", "concept-path-label")).findElements(alternatePathContainer());
        }
    }


    public static class ConceptInfoTabsFinder extends WebDriverComponentFinder<ConceptInfoTabs, ConceptInfoTabsFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("concept-information-tabs");

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
