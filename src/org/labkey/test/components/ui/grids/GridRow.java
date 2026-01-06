package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.components.ui.files.AttachmentCard;
import org.labkey.test.components.ui.files.ImageFileViewDialog;
import org.labkey.test.components.ui.grids.FieldReferenceManager.FieldReference;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class GridRow extends WebDriverComponent<GridRow.ElementCache>
{
    private final WebElement _el;
    protected final ResponsiveGrid<?> _grid;

    protected GridRow(ResponsiveGrid<?> grid, WebElement element)
    {
        _el = element;
        _grid = grid;
    }

    /**
     * indicates whether or not the row has a column containing a select checkbox
     * @return whether or not the row has a selector column
     */
    public boolean hasSelectColumn()
    {
        return _grid.hasSelectColumn();
    }

    public boolean hasConditionalFormatPill(CharSequence columnIdentifier)
    {
        return Locator.tagWithClass("*", "status-pill").existsIn(getCell(columnIdentifier));
    }

    /**
     * Returns the selected state of the row selector checkbox, if one is present
     * @return true if the select checkbox is checked.
     */
    public boolean isSelected()
    {
        return hasSelectColumn() && elementCache().selectCheckbox.isSelected();
    }

    /**
     * Sets the state of the row selector checkbox
     * @param checked the desired state of the checkbox
     * @return  the current instance
     */
    public GridRow select(boolean checked)
    {
        assertTrue("The row does not have a select box", hasSelectColumn());
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().selectCheckbox.getComponentElement()));

        // Make sure the grid header is not hiding the checkbox.
        getWrapper().scrollIntoView(getComponentElement());

        if (elementCache().selectCheckbox.get() != checked)
        {
            _grid.doAndWaitForUpdate(()-> elementCache().selectCheckbox.set(checked));
        }
        return this;
    }

    public ReactCheckBox getCheckbox()
    {
        return elementCache().selectCheckbox;
    }

    /**
     * gets the cell at the specified index
     */
    public WebElement getCell(int colIndex)
    {
        return elementCache().findCells().get(colIndex);
    }

    /**
     * gets the cell corresponding to the specified column
     */
    public WebElement getCell(CharSequence columnIdentifier)
    {
        return getCell(_grid.getColumnIndex(columnIdentifier));
    }

    /**
     * gets the style attribute of the value-wrapper for the specified cell
     * @return
     */
    public String getCellStyle(CharSequence columnIdentifier)
    {
        var cell =  getCell(columnIdentifier);
        return Locator.byClass("table-cell-content").child(Locator.tag("span")).findElement(cell).getAttribute("style");
    }

    /**
     * Returns true if the row contains all of the specified column/value pairs
     * @param partialMap Map of key (column) value (text)
     */
    protected boolean hasMatchingValues(Map<String, String> partialMap)
    {
        for (String key : partialMap.keySet())
        {
            String text = getText(key);
            if (text==null || !text.equals(partialMap.get(key)))
                return false;
        }
        return true;
    }

    /**
     * finds a link with the specified text, clicks it, and waits for the URL to match
     * the HREF of the link.  (this is different from clickAndWait by virtue of not requiring
     * a page load event)
     */
    @LogMethod
    public void clickLink(@LoggedParam String text)
    {
        WebElement link = Locator.linkWithText(text).waitForElement(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        String href = link.getAttribute("href");
        link.click();
        WebDriverWrapper.waitFor(()-> getWrapper().getURL().toString().endsWith(href) &&
                ExpectedConditions.stalenessOf(link).apply(getDriver()), 1000);
    }

    public void clickLinkWithTitle(String text)
    {
        WebElement link = Locator.linkWithTitle(text).waitForElement(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        link.click();
    }

    /**
     * finds a AttachmentCard in the specified column, clicks it, and waits for the image to display in a modal
     */
    public ImageFileViewDialog clickImgFile(CharSequence columnIdentifier)
    {
        return elementCache().waitForAttachment(columnIdentifier).viewImgFile();
    }

    /**
     * finds a AttachmentCard specified filename, clicks it, and waits for the file to download
     */
    public File clickNonImgFile(CharSequence columnIdentifier)
    {
        return elementCache().waitForAttachment(columnIdentifier).clickOnNonImgFile();
    }

    /**
     * Returns the text in the row for the specified column
     */
    public String getText(CharSequence columnIdentifier)
    {
        return getCell(columnIdentifier).getText();
    }

    /**
     * Returns a list of the row values as text
     */
    public List<String> getTexts()
    {
        List<String> columnValues = elementCache().getCellTexts();
        if (hasSelectColumn())
            columnValues.remove(0);
        return columnValues;
    }

    /**
     * gets a map of the row's values, keyed by column label
     */
    public Map<String, String> getRowMapByLabel()
    {
        return getRowMap(FieldReference::getLabel);
    }

    /**
     * gets a map of the row's values, keyed by column name
     */
    public Map<String, String> getRowMapByName()
    {
        return getRowMap(FieldReference::getName);
    }

    /**
     * gets a map of the row's values, keyed by column fieldKey
     */
    public Map<FieldKey, String> getRowMapByFieldKey()
    {
        return getRowMap(FieldReference::getFieldKey);
    }

    <T> Map<T, String> getRowMap(Function<FieldReference, T> keyMapper)
    {
        List<String> columnValues = elementCache().getCellTexts();
        List<FieldReference> headers = _grid.getHeaders();

        Map<T, String> rowMap = new LinkedHashMap<>();

        for (FieldReference header : headers)
        {
            T key = keyMapper.apply(header);
            String value = columnValues.get(header.getDomIndex());

            if (rowMap.containsKey(key))
            {
                TestLogger.warn("Column identifier '%s' is ambiguous, omitting value '%s', consider getting data by name or fieldKey (e.g. %s)".formatted(key, value, header.getFieldKey()));
            }
            else
            {
                rowMap.put(key, value);
            }
        }

        return rowMap;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }   // componentElement is the /tr under tbody

    @Override
    public WebDriver getDriver()
    {
        return _grid.getDriver();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public ReactCheckBox selectCheckbox = new ReactCheckBox(Locator.tagWithAttribute("input", "type", "checkbox")
            .findWhenNeeded(this));

        private List<WebElement> _cells = null;
        protected List<WebElement> findCells()
        {
            if (_cells == null)
            {
                _cells = Locator.xpath("./td").findElements(this);
            }
            return _cells;
        }

        private List<String> cellTexts = null;
        protected List<String> getCellTexts()
        {
            if (cellTexts == null)
            {
                cellTexts = getWrapper().getTexts(findCells());
            }
            return cellTexts;
        }

        public AttachmentCard waitForAttachment(CharSequence columnIdentifier)
        {
            return new AttachmentCard.FileAttachmentCardFinder(getDriver()).waitFor(getCell(columnIdentifier));
        }
    }

    public static class GridRowFinder extends WebDriverComponentFinder<GridRow, GridRowFinder>
    {
        private Locator.XPathLocator _locator = Locator.tag("tbody").child("tr").withoutClass("grid-empty").withoutClass("grid-loading");
        private final ResponsiveGrid<?> _grid;

        public GridRowFinder(ResponsiveGrid<?> grid)
        {
            super(grid.getDriver());
            _grid = grid;
        }

        protected GridRowFinder atIndex(int index)
        {
            _locator = _locator.index(index);
            return this;
        }

        /**
         * Matches rows with a descendant described by the supplied locator
         */
        public GridRowFinder withDescendant(Locator.XPathLocator descendant)
        {
            _locator = _locator.withDescendant(descendant);
            return this;
        }

        /**
         * Matches rows with a cell matching the full text supplied
         */
        public GridRowFinder withCellWithText(String text)
        {
            _locator = _locator.withChild(Locator.tagWithText("td", text));
            return this;
        }

        /**
         * Returns the first row with matching text in the specified column
         */
        protected GridRowFinder withTextAtColumn(String value, int columnIndex)
        {
            _locator = _locator.withChild(Locator.tag("td").index(columnIndex).withText(value));
            return this;
        }

        @Override
        protected GridRowFinder getThis()
        {
            return this;
        }

        @Override
        protected GridRow construct(WebElement el, WebDriver driver)
        {
            return new GridRow(_grid, el);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
