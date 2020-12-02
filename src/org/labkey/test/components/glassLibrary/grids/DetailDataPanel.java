package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;


/**
 * This wraps DetailPanelHeader.tsx
 */
public class DetailDataPanel extends WebDriverComponent<DetailDataPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    private String _title;

    protected DetailDataPanel(WebElement element, WebDriver driver)
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
        if (_title == null)
            _title = elementCache().heading.getText();
        return _title;
    }

    /**
     * When the logged-in user does not have edit permissions, the edit button will not be present
     * @return
     */
    public boolean isEditable()
    {
        return elementCache().editButton().isPresent();
    }

    public DetailTableEdit clickEdit()
    {
        String title = getTitle();
        if (!isEditable())
            throw new IllegalStateException("The current pane (with title ["+title+"]) is not editable");

        elementCache().editButton().get().click();

        return new DetailTableEdit.DetailTableEditFinder(getDriver())
                .withTitle("Editing " + title).waitFor();
    }

    public DetailTable getTable()
    {
        return  elementCache().detailTable;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement heading = Locator.tagWithClass("div", "detail__edit--heading").findWhenNeeded(this);
        public Optional<WebElement> editButton()
        {
            return Locator.tagWithClass("div", "detail__edit-button")
                    .findOptionalElement(heading);
        }
        final DetailTable detailTable = new DetailTable.DetailTableFinder(getDriver()).findWhenNeeded(this);
    }

    public static class DetailDataPanelFinder extends WebDriverComponentFinder<DetailDataPanel, DetailDataPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel")
                .withDescendant(Locator.tagWithClass("table", "detail-component--table__fixed"));
        private String _title = null;

        public DetailDataPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public DetailDataPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected DetailDataPanel construct(WebElement el, WebDriver driver)
        {
            return new DetailDataPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withChild(Locator.tagWithClass("div", "panel-heading")
                        .withText(_title));
            else
                return _baseLocator;
        }
    }
}
