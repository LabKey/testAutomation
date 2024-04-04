package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.Map;

import static org.labkey.test.components.html.Input.Input;

public class QueryChartDialog extends ModalDialog
{
    private final QueryGrid _queryGrid;

    /**
     * Contains
     * @param title "Create Chart" or "Edit Chart"
     * @param driver the webDriver
     * @param grid the associated QueryGrid
     */
    public QueryChartDialog(String title, WebDriver driver, QueryGrid grid)
    {
        super(new ModalDialogFinder(driver).withTitle(title));
        _queryGrid = grid;
    }

    public QueryChartDialog setName(String value)
    {
        elementCache().nameInput.set(value);
        return this;
    }

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    public QueryChartDialog setShared(boolean checked)
    {
        elementCache().sharedCheckbox.set(checked);
        return this;
    }

    public boolean getShared()
    {
        return elementCache().sharedCheckbox.get();
    }

    // field selects

    /*
        X axis is an option for bar, box, line, scatter charts
     */
    public QueryChartDialog selectXAxis(String field)
    {
        elementCache().reactSelectByLabel("X Axis").select(field);
        return this;
    }

    public String getXAxisSelection()
    {
        return elementCache().reactSelectByLabel("X Axis").getValue();
    }

    /*
        Y Axis is an option for bar, box, line, scatter charts
     */
    public QueryChartDialog selectYAxis(String field)
    {
        elementCache().reactSelectByLabel("Y Axis").select(field);
        return this;
    }

    public String getYAxisSelection()
    {
        return elementCache().reactSelectByLabel("Y Axis").getValue();
    }

    /*
        groupBy is an option for bar charts only
     */
    public QueryChartDialog selectGroupBy(String field)
    {
        elementCache().reactSelectByLabel("Group By").select(field);
        return this;
    }

    /*
     * color is an option for Box, Scatter charts
     */
    public QueryChartDialog selectColor(String field)
    {
        elementCache().reactSelectByLabel("Color").select(field);
        return this;
    }

    /*
        shape is an option for Scatter, Box charts
     */
    public QueryChartDialog selectShape(String field)
    {
        elementCache().reactSelectByLabel("Shape").select(field);
        return this;
    }

    /*
        series is an option for line
     */
    public QueryChartDialog selectSeries(String field)
    {
        elementCache().reactSelectByLabel("Series").select(field);
        return this;
    }

    /*
        categories is an option for pie charts
     */
    public QueryChartDialog selectCategories(String field)
    {
        elementCache().reactSelectByLabel("Categories").select(field);
        return this;
    }

    // chart type selection
    /*
        Note: Chart Type is not settable when opened for edit/ it is only settable
        when creating the chart
     */
    public QueryChartDialog setChartType(CHART_TYPE chartType)
    {
        elementCache().selectChartType(chartType);
        return this;
    }

    public CHART_TYPE getSelectedChartType()
    {
        return elementCache().getSelectedChartType();
    }

    public WebElement waitForPreview()
    {
        WebDriverWrapper.waitFor(()-> elementCache().isPreviewPresent(),
                "the preview was not present in time", 2000);
        return elementCache().svg();
    }

    public WebElement svg()
    {
        waitForPreview();
        return elementCache().svg();
    }

    public void clickCancel()
    {
        dismiss("Cancel");
    }

    public boolean isCreateChartButtonEnabled()
    {
        Locator createChartBtnLoc = Locator.button("Create Chart");
        return createChartBtnLoc.existsIn(this ) &&
                createChartBtnLoc.findElement(this).isEnabled();
    }

    public boolean isSaveChartButtonEnabled()
    {
        Locator createChartBtnLoc = Locator.button("Save Chart");
        return createChartBtnLoc.existsIn(this ) &&
                createChartBtnLoc.findElement(this).isEnabled();
    }

    /*
        appears when dialog is in 'edit' mode
     */
    public QueryChartPanel clickCreateChart()
    {
        WebDriverWrapper.waitFor(this::isCreateChartButtonEnabled,
                "the create chart button did not become enabled", 2000);
        dismiss("Create Chart");
        return _queryGrid.getChartPanel();
    }

    /*
        appears when dialog is in 'edit' mode
     */
    public QueryChartPanel clickSaveChart()
    {
        WebDriverWrapper.waitFor(this::isSaveChartButtonEnabled,
                "the Save chart button did not become enabled", 2000);
        dismiss("Save Chart");
        return _queryGrid.getChartPanel();
    }

    /*
        appears when dialog is in 'edit' mode
     */
    public void clickDeleteChart(boolean confirmDelete)
    {
        // clicking 'delete chart' shows a prompt + cancel/delete buttons
        var dismissBtn = Locators.dismissButton("Delete Chart").waitForElement(getComponentElement(), 1500);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(dismissBtn));
        dismissBtn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(dismissBtn));

        // warn div contains cancel and delete buttons
        var warnDiv = Locator.tagWithClass("div", "form-buttons__right")
                .containing("Are you sure you want to permanently delete this chart?")
                .waitForElement(this, 1000);
        if (confirmDelete)
        {
            Locators.dismissButton("Delete").waitForElement(warnDiv, 500).click();
            waitForClose();
        }
        else
        {
            var cancelBtn = Locators.dismissButton("Cancel").waitForElement(warnDiv, 500);
            cancelBtn.click();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(cancelBtn));
        }
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

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final Input nameInput = Input(Locator.input("name"), getDriver()).findWhenNeeded(this);
        final Checkbox sharedCheckbox = Checkbox.Checkbox(Locator.input("shared")).findWhenNeeded(this);

        Locator.XPathLocator chartBuilderType = Locator.tagWithClass("div", "chart-builder-type");
        public CHART_TYPE getSelectedChartType()
        {
            var selectedEl = chartBuilderType.withAttributeContaining("class", "selected")
                    .waitForElement(this, 1500);
            String dataName = selectedEl.getAttribute("data-name");
            return CHART_TYPE.fromChartType(dataName);
        }

        // note: selecting a chart type will cause most select fields to go stale
        public void selectChartType(CHART_TYPE chartType)
        {
            if (getSelectedChartType().equals(chartType))
                return;

            var el = chartBuilderType.withAttribute("data-name", chartType.getChartType())
                    .waitForElement(this, 1500);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(el));
            el.click();
            WebDriverWrapper.waitFor(()-> getSelectedChartType().equals(chartType),
                    "The requested chart type did not become selected", 2000);
        }

        public ReactSelect reactSelectByLabel(String label)
        {
            WebElement container = Locator.tag("div").withChild(Locator.tagContainingText("label", label))
                    .waitForElement(this, 1500);
            return ReactSelect.finder(getDriver()).find(container);
        }

        private Locator.XPathLocator previewContainerLoc = Locator.tag("div").withChild(Locator.tagWithText("label", "Preview"));
        public WebElement previewContainer()
        {
            return previewContainerLoc.waitForElement(this, 1500);
        }
        public String grayTextPreviewInstruction()
        {
            return Locator.tagWithClass("div", "gray-text").waitForElement(previewContainer(), 1500).getText();
        }

        private Locator previewBodyLoc = Locator.tagWithClass("div", "chart-builder-preview-body");
        private Locator svgLoc = Locator.tagWithClass("div", "svg-chart__chart");
        public boolean isPreviewPresent()
        {
            return previewBodyLoc.existsIn(previewContainer()) && svgLoc.existsIn(previewContainer());
        }

        public WebElement svg()
        {
            return svgLoc.waitForElement(previewContainer(), 1500);
        }

    }

    public enum CHART_TYPE{
        Bar("bar_chart"),
        Box("box_plot"),
        Line("line_plot"),
        Pie("pie_chart"),
        Scatter("scatter_plot");
        CHART_TYPE(String chartType)
        {
            _chartType = chartType;
        }

        static {
            Map<String,CHART_TYPE> map = new HashMap<String,CHART_TYPE>();
            for(CHART_TYPE instance : CHART_TYPE.values())
            {
                map.put(instance.getChartType(), instance);
            }
            CHART_TYPE_MAP = map;
        }
        private String _chartType;
        private static final Map<String,CHART_TYPE> CHART_TYPE_MAP;
        public String getChartType()
        {
            return _chartType;
        }
        public static CHART_TYPE fromChartType(String chartType)
        {
            return CHART_TYPE_MAP.get(chartType);
        }
    }
}
