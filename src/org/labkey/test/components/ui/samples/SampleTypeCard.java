package org.labkey.test.components.ui.samples;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.Card;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Automates shared component implemented by /internal/components/samples/SampleSetCards.tsx
 */
public class SampleTypeCard extends Card
{
    protected SampleTypeCard(WebElement element, WebDriver driver)
    {
        super(element, driver);
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



    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends Card.ElementCache
    {
        final WebElement cardBlockCenterElement = Locator.tagWithClass("div", "cards__block-center")
                .findWhenNeeded(this);
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
