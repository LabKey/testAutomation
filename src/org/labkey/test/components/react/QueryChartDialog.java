package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.List;
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

    public QueryChartDialog setInheritable(boolean checked)
    {
        elementCache().inheritableCheckbox.set(checked);
        return this;
    }

    public boolean getInheritable()
    {
        return elementCache().inheritableCheckbox.get();
    }

    // field selects

    public QueryChartDialog clearFieldValue(String fieldLabel)
    {
        elementCache().reactSelectByLabel(fieldLabel).clearSelection();
        return this;
    }

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

    public List<String> getXAxisSelectionOptions()
    {
        return elementCache().reactSelectByLabel("X Axis").getOptions();
    }

    public boolean hasAxisFieldOptions(String label)
    {
        return elementCache().fieldOptionIconByLabel(label) != null;
    }

    private void clickFieldOptions(String label)
    {
        elementCache().fieldOptionIconByLabel(label).click();
    }

    /**
     * Set the axis scale to 'linear' or 'log' for the given axis
     * @param label the axis label
     * @param value the value to set (linear or log)
     * @return this
     */
    public QueryChartDialog setAxisScaleType(String label, String value)
    {
        clickFieldOptions(label); // open the popover
        if ("linear".equalsIgnoreCase(value))
            elementCache().scaleLinearRadio.check();
        else if ("log".equalsIgnoreCase(value))
            elementCache().scaleLogRadio.check();
        else
            throw new IllegalArgumentException("Invalid scale value: " + value);
        clickFieldOptions(label); // close the popover
        return this;
    }

    public boolean isAxisScaleTypeSelected(String label, String value)
    {
        clickFieldOptions(label); // open the popover
        boolean selected = "linear".equalsIgnoreCase(value) ? elementCache().scaleLinearRadio.isChecked() :
                "log".equalsIgnoreCase(value) && elementCache().scaleLogRadio.isChecked();
        clickFieldOptions(label); // close the popover
        return selected;
    }

    /**
     * Set the axis range to 'automatic' or 'manual' for the given axis
     * @param label the axis label
     * @param value the value to set (automatic or manual)
     * @return this
     */
    public QueryChartDialog setAxisRangeType(String label, String value)
    {
        clickFieldOptions(label); // open the popover
        if ("automatic".equalsIgnoreCase(value))
            elementCache().scaleAutomaticRadio.check();
        else if ("manual".equalsIgnoreCase(value))
            elementCache().scaleManualRadio.check();
        else
            throw new IllegalArgumentException("Invalid range value: " + value);
        clickFieldOptions(label); // close the popover
        return this;
    }

    public boolean isAxisRangeTypeSelected(String label, String value)
    {
        clickFieldOptions(label); // open the popover
        boolean selected = "automatic".equalsIgnoreCase(value) ? elementCache().scaleAutomaticRadio.isChecked() :
                "manual".equalsIgnoreCase(value) && elementCache().scaleManualRadio.isChecked();
        clickFieldOptions(label); // close the popover
        return selected;
    }

    public QueryChartDialog setAxisRange(String label, String min, String max)
    {
        clickFieldOptions(label); // open the popover
        if (elementCache().scaleManualRadio.isChecked())
        {
            elementCache().scaleRangeMinInput.set(min);
            elementCache().scaleRangeMaxInput.set(max);
        }
        clickFieldOptions(label); // close the popover
        return this;
    }

    public String getAxisRangeMin(String label)
    {
        clickFieldOptions(label); // open the popover
        String min = elementCache().scaleRangeMinInput.get();
        clickFieldOptions(label); // close the popover
        return min;
    }

    public String getAxisRangeMax(String label)
    {
        clickFieldOptions(label); // open the popover
        String max = elementCache().scaleRangeMaxInput.get();
        clickFieldOptions(label); // close the popover
        return max;
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

    public List<String> getYAxisSelectionOptions()
    {
        return elementCache().reactSelectByLabel("Y Axis").getOptions();
    }

    public QueryChartDialog selectYAxisAggregateMethod(String option)
    {
        clickFieldOptions("Y Axis"); // open the popover
        getAggregateMethodSelect().select(option);
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return this;
    }

    public List<String> getYAxisAggregateMethodOptions()
    {
        clickFieldOptions("Y Axis"); // open the popover
        List<String> options = getAggregateMethodSelect().getOptions();
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return options;
    }

    public String getYAxisAggregateMethod()
    {
        clickFieldOptions("Y Axis"); // open the popover
        String value = getAggregateMethodSelect().getValue();
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return value;
    }

    private ReactSelect getAggregateMethodSelect()
    {
        // can't use elementCache() because the popover is outside the dialog
        Locator loc = Locator.tag("div").withChild(Locator.tagContainingText("label", "Aggregate Method"));
        return ReactSelect.finder(getDriver()).find(loc.waitForElement(getDriver(), 1500));
    }

    private RadioButton getErrorBarsRadio(String value)
    {
        // can't use elementCache() because the popover is outside the dialog
        return RadioButton.RadioButton(Locator.radioButtonByNameAndValue("error-bar-method", value)).find(getDriver());
    }

    public QueryChartDialog selectYAxisErrorBar(String value)
    {
        clickFieldOptions("Y Axis"); // open the popover
        getErrorBarsRadio(value).check();
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return this;
    }

    public boolean isYAxisErrorBarOptionEnabled(String value)
    {
        clickFieldOptions("Y Axis"); // open the popover
        boolean enabled = getErrorBarsRadio(value).isEnabled();
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return enabled;
    }

    /*
        groupBy is an option for bar charts only
     */
    public QueryChartDialog selectGroupBy(String field)
    {
        elementCache().reactSelectByLabel("Group By").select(field);
        return this;
    }

    public String getGroupBySelection()
    {
        return elementCache().reactSelectByLabel("Group By").getValue();
    }

    public List<String> getGroupBySelectionOptions()
    {
        return elementCache().reactSelectByLabel("Group By").getOptions();
    }

    /*
     * color is an option for Box, Scatter charts
     */
    public QueryChartDialog selectColor(String field)
    {
        elementCache().reactSelectByLabel("Color").select(field);
        return this;
    }

    public String getSelectedColor()
    {
        return elementCache().reactSelectByLabel("Color").getValue();
    }

    /*
        shape is an option for Scatter, Box charts
     */
    public QueryChartDialog selectShape(String field)
    {
        elementCache().reactSelectByLabel("Shape").select(field);
        return this;
    }

    public String getSelectedShape()
    {
        return elementCache().reactSelectByLabel("Shape").getValue();
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
        trendline is an option for line
     */
    public boolean hasTrendlineOption()
    {
        return elementCache().reactSelectByLabel("Trendline", true) != null;
    }

    /*
        trendline is an option for line
     */
    public List<String> getTrendlineOptions()
    {
        return elementCache().reactSelectByLabel("Trendline").getOptions();
    }

    /*
        trendline is an option for line
     */
    public QueryChartDialog selectTrendline(String field)
    {
        elementCache().reactSelectByLabel("Trendline").select(field);
        return this;
    }

    public String getSelectedSeries()
    {
        return elementCache().reactSelectByLabel("Series").getValue();
    }

    /*
        categories is an option for pie charts
     */
    public QueryChartDialog selectCategories(String field)
    {
        elementCache().reactSelectByLabel("Categories").select(field);
        return this;
    }

    public String getSelectedCategory()
    {
        return elementCache().reactSelectByLabel("Categories").getValue();
    }

    public List<String> getCategorySelectOptions()
    {
        return elementCache().reactSelectByLabel("Categories").getOptions();
    }

    // chart type selection
    /*
        Note: Chart Type is not settable when opened for edit/ it is only settable
        when creating the chart
        Also, when updating the chart type, expect field selects to be re-drawn or made stale
     */
    public QueryChartDialog setChartType(CHART_TYPE chartType)
    {
        if (getSelectedChartType().equals(chartType))
            return this;

        var el = elementCache().chartBuilderType.withAttribute("data-name", chartType.getChartType())
                .waitForElement(this, 1500);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(el));
        el.click();
        WebDriverWrapper.waitFor(()-> getSelectedChartType().equals(chartType),
                "The requested chart type did not become selected", 2000);

        return this;
    }

    public CHART_TYPE getSelectedChartType()
    {
        var selectedEl = elementCache().chartBuilderType.withAttributeContaining("class", "selected")
                .waitForElement(this, 1500);
        String dataName = selectedEl.getAttribute("data-name");
        return CHART_TYPE.fromChartType(dataName);
    }

    public boolean isPreviewPresent()
    {
        return elementCache().previewBodyLoc.existsIn(elementCache().previewContainer()) &&
                elementCache().svgLoc.existsIn(elementCache().previewContainer());
    }

    public WebElement getSvgChart()
    {
        WebDriverWrapper.waitFor(()-> isPreviewPresent(),
                "the preview was not present in time", 2000);
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
        String name = getName();
        dismiss("Create Chart");
        return _queryGrid.getChartPanel(name);
    }

    /*
        appears when dialog is in 'edit' mode
     */
    public QueryChartPanel clickSaveChart()
    {
        return clickSaveChart(getName());
    }
    public QueryChartPanel clickSaveChart(String previousChartName)
    {
        String name = getName();
        WebElement prevChart = _queryGrid.getChartPanel(previousChartName).getSvgChart();
        WebDriverWrapper.waitFor(this::isSaveChartButtonEnabled,
                "the Save chart button did not become enabled", 2000);
        dismiss("Save Chart");
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(prevChart));
        return _queryGrid.getChartPanel(name);
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
        final Checkbox inheritableCheckbox = Checkbox.Checkbox(Locator.input("inheritable")).findWhenNeeded(this);

        Locator.XPathLocator chartBuilderType = Locator.tagWithClass("div", "chart-builder-type");

        public ReactSelect reactSelectByLabel(String label)
        {
            return reactSelectByLabel(label, false);
        }

        public ReactSelect reactSelectByLabel(String label, boolean allowNull)
        {
            Locator loc = Locator.tag("div").withChild(Locator.tagContainingText("label", label));
            if (allowNull && loc.findElementOrNull(this) == null)
                return null;
            else
                return ReactSelect.finder(getDriver()).find(loc.waitForElement(this, 1500));
        }

        public WebElement fieldOptionIconByLabel(String label)
        {
            Locator loc = Locator.tag("div").withChild(Locator.tagContainingText("label", label));
            return Locator.tagWithClass("div", "field-option-icon").descendant("span").findElementOrNull(loc.findElement(this));
        }
        private final Locator.XPathLocator previewContainerLoc = Locator.tag("div").withChild(Locator.tagWithText("label", "Preview"));
        public WebElement previewContainer()
        {
            return previewContainerLoc.waitForElement(this, 1500);
        }
        public String grayTextPreviewInstruction()
        {
            return Locator.tagWithClass("div", "gray-text").waitForElement(previewContainer(), 1500).getText();
        }

        private final Locator previewBodyLoc = Locator.tagWithClass("div", "chart-builder-preview-body");
        private final Locator svgLoc = Locator.tagWithClass("div", "svg-chart__chart").childTag("svg");

        public WebElement svg()
        {
            return svgLoc.waitForElement(previewContainer(), 1500);
        }

        private final WebElement fieldOptionPopover = Locator.tagWithId("div", "chart-field-option-popover").refindWhenNeeded(getDriver());
        public RadioButton scaleLinearRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleTrans", "linear")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton scaleLogRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleTrans", "log")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton scaleAutomaticRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleType", "automatic")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton scaleManualRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleType", "manual")).refindWhenNeeded(fieldOptionPopover);
        public Input scaleRangeMinInput = Input(Locator.input("scaleMin"), getDriver()).refindWhenNeeded(fieldOptionPopover);
        public Input scaleRangeMaxInput = Input(Locator.input("scaleMax"), getDriver()).refindWhenNeeded(fieldOptionPopover);
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
            Map<String,CHART_TYPE> map = new HashMap<>();
            for(CHART_TYPE instance : CHART_TYPE.values())
            {
                map.put(instance.getChartType(), instance);
            }
            CHART_TYPE_MAP = map;
        }
        private final String _chartType;
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
