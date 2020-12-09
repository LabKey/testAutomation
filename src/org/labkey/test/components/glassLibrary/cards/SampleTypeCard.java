package org.labkey.test.components.glassLibrary.cards;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

/**
 * Automates shared component implemented by /internal/components/base/cards.tsx
 */
public class SampleTypeCard extends WebDriverComponent<SampleTypeCard.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected SampleTypeCard(WebElement element, WebDriver driver)
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

    public String getTitle()
    {
        return elementCache().titleElement.getText();
    }

    /**
     * gets the contents of the 'content' element, minus the title (which is a child of the content element) because
     * title is separately available here
     * @return the text of the 'content' element, minus the title
     */
    public String getContent()
    {
        String content = elementCache().contentElement.getText();
        return content.replace(getTitle(), "").trim();
    }

    /**
     * To indicate that a sampleType might not have samples in it, the 'cards__block-disabled' style grays out the
     * center-block element
     * @return true if the element class contains "cards__block-disabled"
     */
    public Boolean isCenterBlockDisabled()
    {
        return elementCache().cardBlockCenterElement.getAttribute("class").contains("cards__block-disabled");
    }

    /**
     * gets the contents of the 'alt' attribute of the center Icon element
     * @return The text of the 'alt' attribute
     */
    public String getIconAltString()
    {
        return elementCache().cardBlockCenterContentImg.getAttribute("alt");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement titleElement = Locator.tagWithClass("div", "cards__card-title")
                .findWhenNeeded(this);
        final WebElement contentElement = Locator.tagWithClass("div", "cards__card-content")
                .findWhenNeeded(this);
        final WebElement cardBlockCenterElement = Locator.tagWithClass("div", "cards__block-center")
                .findWhenNeeded(this);
        final WebElement cardBlockCenterContentImg = Locator.tagWithClass("div", "cards__block-center-content")
                .child("img").findWhenNeeded(this);
    }


    public static class SampleTypeCardFinder extends WebDriverComponentFinder<SampleTypeCard, SampleTypeCardFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("a", "cards__card");
        private String _title = null;

        public SampleTypeCardFinder(WebDriver driver)
        {
            super(driver);
        }

        public SampleTypeCardFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected SampleTypeCard construct(WebElement el, WebDriver driver)
        {
            return new SampleTypeCard(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "cards__card-title").withText(_title));
            else
                return _baseLocator;
        }
    }
}
