package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
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

    /**
     * If this sample is an aliquot clicking the edit button will only allow the user to edit the description property.
     * No other fields are editable.
     *
     * @return A {@link DetailTableEdit}
     */
    public DetailTableEdit clickEdit()
    {
        String title = getTitle();
        if (!isEditable())
            throw new IllegalStateException("The current pane (with title ["+title+"]) is not editable");

        elementCache().editButton().get().click();

        return new DetailTableEdit.DetailTableEditFinder(getDriver())
                .withTitle("Editing " + title).waitFor();
    }

    /**
     * Get the table that has the details fields (i.e. column data) for this sample.
     *
     * @return A {@link DetailTable}.
     */
    public DetailTable getTable()
    {
        List<DetailTable> tables = elementCache().detailTables();

        // As of release 21.05 the table with the column values for the sample is the last table in the panel.
        return  tables.get(tables.size() - 1);
    }

    /**
     * If this sample is an aliquot there will be multiple tables in the panel, the first table will contain the aliquot
     * information (under the 'Aliquot Data' header). This will return that table.
     *
     * @return A {@link DetailTable}.
     */
    public DetailTable getAliquotTable()
    {
        List<DetailTable> tables = elementCache().detailTables();

        if(tables.size() == 1)
            throw new RuntimeException("There does not appear to be an 'Aliquot' table in this panel.");

        // As of release 21.05 if this sample is an aliquot, then the table with the aliquot information is the first table in the panel.
        return tables.get(0);
    }

    /**
     * If this sample is an aliquot there will be multiple tables in the panel, the second table is under the 'Original
     * Sample Data' header, but is a separate table from the other original data displayed and may only be one or two
     * lines. The fields 'Original sample' and 'Sample description' will show up here.
     *
     * @return A {@link DetailTable}.
     */
    public DetailTable getAliquotDetailMetaTable()
    {
        List<DetailTable> tables = elementCache().detailTables();

        if(tables.size() == 1)
            throw new RuntimeException("There does not appear to be an 'Aliquot (details meta)' table in this panel.");

        // As of release 21.05 if this sample is an aliquot, then the table with the aliquot information is the first table in the panel.
        return tables.get(1);
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

        final List<DetailTable> detailTables()
        {
            return new DetailTable.DetailTableFinder(getDriver()).findAll(this);
        }

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
