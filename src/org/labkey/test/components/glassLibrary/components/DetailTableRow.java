package org.labkey.test.components.glassLibrary.components;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

// TODO Tried to go down this route but had issue so not using this class at this time.
//  I ran out of time and wanted to leave the test in a runnable state.
public class DetailTableRow extends WebDriverComponent
{
    final WebElement _rowElement;
    private WebDriver _driver;

    protected DetailTableRow(WebElement element, WebDriver driver)
    {
        _rowElement = element;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _rowElement;
    }

    protected static abstract class Locators
    {
        static final Locator row = Locator.xpath("//table[contains(@class, 'detail-component--table__fixed')]/tbody/tr");

        static final Locator rowWithTitle(String title)
        {
            return Locator.xpath("//table[contains(@class, 'detail-component--table__fixed')]/tbody/tr[./td/span[@title='" + title + "']]");
        }

        static final Locator rowWithContent(String content)
        {
            return Locator.xpath("//table[contains(@class, 'detail-component--table__fixed')]/tbody/tr[./td/span[text()='" + content + "']]");
        }

    }

    public static class DetailTableRowFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTableRow, DetailTableRowFinder>
    {
        private Locator _locator;

        public DetailTableRowFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locators.row;
        }

        public DetailTableRowFinder withTitle(String title)
        {
            _locator = Locators.rowWithTitle(title);
            return this;
        }

        public DetailTableRowFinder withContent(String content)
        {
            _locator = Locators.rowWithContent(content);
            return this;
        }

        @Override
        protected DetailTableRow construct(WebElement el, WebDriver driver)
        {
            return new DetailTableRow(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
