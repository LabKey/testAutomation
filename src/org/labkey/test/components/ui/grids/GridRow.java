package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.components.ui.files.ImageFileViewDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.util.TestLogger.log;

public class GridRow extends WebDriverComponent<GridRow.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;
    final ResponsiveGrid _grid;
    private Map<String, String> _rowMap = null;

    protected GridRow(ResponsiveGrid grid, WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
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
        elementCache().selectCheckbox.set(checked);
        return this;
    }

    public boolean hasTextInColumn(String column, String text)
    {
        return getRowMap().get(column).equals(text);
    }

    /**
     * gets the cell at the specified index
     * use columnHeader, which computes the appropriate index
     * This method is intended for short-term use, until we can offload usages in the heatmap to its own component
     */
    public WebElement getCell(int colIndex)
    {
        return Locator.tag("td").index(colIndex).findElement(this);
    }

    /**
     * gets the cell corresponding to the specified column
     */
    public WebElement getCell(String columnHeader)
    {
        return getCell(_grid.getColumnIndex(columnHeader));
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
    public void clickLink(String text)
    {
        log("seeking link with text [" + text + "]");
        WebElement link = Locator.linkWithText(text).waitForElement(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        log("found element with text [" + link.getText() + "]");
        String href = link.getAttribute("href");
        link.click();
        log("waiting for url to be: [" + href + "]");
        WebDriverWrapper.waitFor(()-> getWrapper().getURL().toString().endsWith(href) &&
                getWrapper().shortWait().until(ExpectedConditions.stalenessOf(link)), 1000);
    }

    /**
     * finds a AttachmentCard specified filename, clicks it, and waits for the image to display in a modal
     */
    public ImageFileViewDialog clickImgFile(String filename)
    {
        clickOnFile(filename);
        log("waiting for image to display");
        return new ImageFileViewDialog(getDriver(), filename);
    }

    public void clickOnFile(String filename)
    {
        log("seeking cell with filename [" + filename + "]");
        WebElement filenameEl = elementCache().filenameLoc.containing(filename).waitForElement(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        log("found element with filename [" + filename + "]");
        filenameEl.click();
    }

    /**
     * finds a AttachmentCard specified filename, clicks it, and waits for the file to download
     */
    public File clickNonImgFile(String filename)
    {
        return getWrapper()
                .doAndWaitForDownload(() -> {
                    log("waiting for file to download");
                    clickOnFile(filename);
                }, 1)[0];
    }

    /**
     * Returns the text in the row for the specified column
     */
    public String getText(String columnText)
    {
        return getCell(columnText).getText();
    }

    /**
     * Returns a list of the row values as text
     */
    public List<String> getTexts()
    {
        List<String> columnValues = getWrapper().getTexts(Locator.css("td")
                .findElements(this));
        if (hasSelectColumn())
            columnValues.remove(0);
        return columnValues;
    }

    /**
     * gets a map of the row's values, keyed by column name
     */
    public Map<String, String> getRowMap()
    {
        if (_rowMap == null)
        {
            _rowMap = new HashMap<>();
            List<String> columns = _grid.getColumnNames();
            List<String> rowCellTexts = getTexts();
            for (int i = 0; i < columns.size(); i++)
            {
                _rowMap.put(columns.get(i), rowCellTexts.get(i));
            }
        }
        return _rowMap;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }   // componentElement is the /tr under tbody

    @Override
    public WebDriver getDriver()
    {
        return _driver;
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

        public Locator filenameLoc = Locator.tagWithClass("div", "attachment-card__name");
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
            return new GridRow(_grid, el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
