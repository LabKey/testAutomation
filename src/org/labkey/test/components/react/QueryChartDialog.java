package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ColorPickerInput;
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
import java.util.Optional;

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

    public QueryChartDialog setTitle(String value) {
        elementCache().titleInput.set(value);
        elementCache().title.click(); // blur the element
        return this;
    }

    public QueryChartDialog setSubtitle(String value)
    {
        elementCache().subtitleInput.set(value);
        elementCache().title.click(); // blur the element
        return this;
    }

    public QueryChartDialog setHeight(String value)
    {
        elementCache().heightInput.set(value);
        elementCache().title.click(); // blur the element
        return this;
    }

    public QueryChartDialog setWidth(String value)
    {
        elementCache().widthInput.set(value);
        elementCache().title.click(); // blur the element
        return this;
    }

    public QueryChartDialog setUseFullWidth(boolean checked)
    {
        elementCache().fullWidthCheckbox.set(checked);
        return this;
    }

    public boolean getUseFullWidth()
    {
        return elementCache().sharedCheckbox.get();
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

    public QueryChartDialog setLegendPos(String legendPos)
    {

        if ("bottom".equals(legendPos))
            elementCache().legendBottomRadio.check();
        else if ("right".equals(legendPos))
            elementCache().legendRightRadio.check();
        else
            throw new IllegalArgumentException("Invalid legend value: " + legendPos);

        return this;
    }

    public String getLegendPos()
    {
        if (elementCache().legendBottomRadio.isChecked())
            return "bottom";
        else if (elementCache().legendRightRadio.isChecked())
            return "right";

        throw new IllegalStateException("Unexpected legend position");
    }

    public boolean isLegendPosVisible()
    {
        return elementCache().legendBottomRadio.isDisplayed() && elementCache().legendRightRadio.isDisplayed();
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

    public boolean isAxisScaleTypeAvailable(String label)
    {
        clickFieldOptions(label);
        return elementCache().radioGroupWithLabel("Scale").isPresent();
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

    public QueryChartDialog setXAxisLabel(String value)
    {
        clickFieldOptions("X Axis");
        elementCache().xLabelInput.set(value);
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return this;
    }

    public QueryChartDialog setYAxisLabel(String value)
    {
        clickFieldOptions("Y Axis");
        elementCache().yLabelInput.set(value);
        Locator.tagWithText("label", "Name *").findElement(this).click(); // close the popover
        return this;
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

    public QueryChartDialog setTrendlineProvidedParameters(String field)
    {
        clickFieldOptions("Trendline"); // open the popover
        if (field != null)
            getTrendlineProvidedParametersSelect().select(field);
        else
            getTrendlineProvidedParametersSelect().clearSelection();
        elementCache().title.click(); // close the popover
        return this;
    }

    public List<String> getTrendlineProvidedParametersOptions()
    {
        clickFieldOptions("Trendline"); // open the popover
        List<String> options = getTrendlineProvidedParametersSelect().getOptions();
        elementCache().title.click(); // close the popover
        return options;
    }

    private ReactSelect getTrendlineProvidedParametersSelect()
    {
        // can't use elementCache() because the popover is outside the dialog
        Locator loc = Locator.tag("div").withChild(Locator.tagContainingText("label", "Provided Parameters"));
        return ReactSelect.finder(getDriver()).find(loc.waitForElement(getDriver(), 1500));
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

        var chartTypeDropdown = elementCache().reactSelectByLabel("Chart Type");
        // ChartTypeDropdown component uses a custom option renderer
        chartTypeDropdown.setOptionLocator((String type) -> Locator.byClass("chart-builder-type-option").withAttribute("data-chart-type", type));
        chartTypeDropdown.select(chartType.getChartType());
        WebDriverWrapper.waitFor(()-> getSelectedChartType().equals(chartType),
                "The requested chart type did not become selected", 2000);

        return this;
    }

    public CHART_TYPE getSelectedChartType()
    {
        var chartTypeDropdown = elementCache().reactSelectByLabel("Chart Type");
        // The ChartTypeDropdown component has a custom value renderer, so we need to find that manually because
        // chartTypeDropdown.getValue will throw an exception
        WebElement selectedOption = Locator.byClass("chart-builder-type-option--value").findElement(chartTypeDropdown);
        return CHART_TYPE.fromChartType(selectedOption.getAttribute("data-chart-type"));
    }

    public boolean isPreviewPresent()
    {
        return elementCache().previewBodyLoc.existsIn(elementCache().previewContainer()) &&
                elementCache().svgLoc.existsIn(elementCache().previewContainer());
    }

    public List<String> getPreviewErrors()
    {
        return elementCache().svgErrorLoc.findElements(elementCache().previewContainer()).stream()
                .map(WebElement::getText).toList();
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

    public boolean hasFillColorOption()
    {
        return elementCache().fillColorPicker.isDisplayed();
    }

    public QueryChartDialog setFillColor(String hexColor)
    {
        return setColor(elementCache().fillColorPicker, hexColor);
    }

    public boolean hasLineColorOption()
    {
        return elementCache().lineColorPicker.isDisplayed();
    }

    public QueryChartDialog setLineColor(String hexColor)
    {
        return setColor(elementCache().lineColorPicker, hexColor);
    }

    public boolean hasPointColorOption()
    {
        return elementCache().pointColorPicker.isDisplayed();
    }

    public QueryChartDialog setPointColor(String hexColor)
    {
        return setColor(elementCache().pointColorPicker, hexColor);
    }

    private QueryChartDialog setColor(WebElement colorPickerContainer, String hexColor)
    {
        Locator.tagWithClassContaining("button", "color-picker__button").findElement(colorPickerContainer).click();
        ColorPickerInput colorInput = new ColorPickerInput.ColorPickerInputFinder(getDriver()).findWhenNeeded();
        colorInput.setHexValue(hexColor);
        colorPickerContainer.click(); // need to click outside the color picker to close it
        return this;
    }

    public boolean hasColorPaletteOption()
    {
        return elementCache().reactSelectByLabel("Color Palette", true) != null;
    }

    public QueryChartDialog selectColorPalette(String option)
    {
        elementCache().reactSelectByLabel("Color Palette").select(option);
        return this;
    }

    public boolean hasLineColorAndStyleSelectOption()
    {
        return elementCache().reactSelectByLabel("Line Color and Style", true) != null;
    }

    public QueryChartDialog selectLineColorAndStyleOption(String option, String hexColor)
    {
        var seriesDropdown = elementCache().reactSelectByLabel("Line Color and Style");
        // series select component uses a custom option renderer
        seriesDropdown.setOptionLocator((String type) -> Locator.byClass("chart-builder-type-option").withAttribute("data-series-shape", type));
        seriesDropdown.select(option);
        if (hexColor != null)
            setColor(elementCache().seriesColorPicker, hexColor);
        return this;
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
        Locator.XPathLocator settingsPanelLoc = Locator.byClass("chart-settings");
        final WebElement settingsPanel = settingsPanelLoc.findWhenNeeded(this);
        final Input nameInput = Input(Locator.input("name"), getDriver()).findWhenNeeded(settingsPanel);
        final Input titleInput = Input(Locator.input("main-label"), getDriver()).findWhenNeeded(settingsPanel);
        final Input subtitleInput = Input(Locator.input("subtitle-label"), getDriver()).findWhenNeeded(settingsPanel);
        final Input heightInput = Input(Locator.input("height"), getDriver()).findWhenNeeded(settingsPanel);
        final Input widthInput = Input(Locator.input("width"), getDriver()).findWhenNeeded(settingsPanel);
        final Checkbox sharedCheckbox = Checkbox.Checkbox(Locator.input("shared")).findWhenNeeded(settingsPanel);
        final Checkbox inheritableCheckbox = Checkbox.Checkbox(Locator.input("inheritable")).findWhenNeeded(settingsPanel);
        final Checkbox fullWidthCheckbox = Checkbox.Checkbox(Locator.input("use-full-width")).findWhenNeeded(settingsPanel);

        final WebElement fillColorPicker = Locator.byClass("color-picker").withAttribute("data-name", "boxFillColor").refindWhenNeeded(settingsPanel);
        final WebElement lineColorPicker = Locator.byClass("color-picker").withAttribute("data-name", "lineColor").refindWhenNeeded(settingsPanel);
        final WebElement pointColorPicker = Locator.byClass("color-picker").withAttribute("data-name", "pointFillColor").refindWhenNeeded(settingsPanel);
        final WebElement seriesColorPicker = Locator.byClass("color-picker").withAttribute("data-name", "seriesColor").refindWhenNeeded(settingsPanel);

        public ReactSelect reactSelectByLabel(String label)
        {
            return reactSelectByLabel(label, false);
        }

        public ReactSelect reactSelectByLabel(String label, boolean allowNull)
        {
            Locator loc = Locator.tag("div").withChild(Locator.tagContainingText("label", label));
            if (allowNull && loc.findElementOrNull(settingsPanel) == null)
                return null;
            else
                return ReactSelect.finder(getDriver()).find(loc.waitForElement(settingsPanel, 1500));
        }

        public WebElement fieldOptionIconByLabel(String label)
        {
            Locator loc = Locator.tag("div").withChild(Locator.tagContainingText("label", label));
            return Locator.tagWithClass("div", "field-option-icon").descendant("span").findElementOrNull(loc.findElement(this));
        }
        private final Locator.XPathLocator previewContainerLoc = Locator.tag("div").withChild(Locator.tagWithText("h4", "Preview"));
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
        private final Locator svgErrorLoc = Locator.tagWithClass("div", "svg-chart__chart").childTag("div");

        public WebElement svg()
        {
            return svgLoc.waitForElement(previewContainer(), 1500);
        }

        private final WebElement fieldOptionPopover = Locator.tagWithId("div", "chart-field-option-popover").refindWhenNeeded(getDriver());
        public RadioButton scaleLinearRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleTrans", "linear")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton scaleLogRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleTrans", "log")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton scaleAutomaticRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleType", "automatic")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton scaleManualRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("scaleType", "manual")).refindWhenNeeded(fieldOptionPopover);
        public RadioButton legendRightRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("legendPos", "right")).refindWhenNeeded(settingsPanel);
        public RadioButton legendBottomRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("legendPos", "bottom")).refindWhenNeeded(settingsPanel);
        public Input scaleRangeMinInput = Input(Locator.input("scaleMin"), getDriver()).refindWhenNeeded(fieldOptionPopover);
        public Input scaleRangeMaxInput = Input(Locator.input("scaleMax"), getDriver()).refindWhenNeeded(fieldOptionPopover);
        public Input yLabelInput = Input(Locator.input("y-label"), getDriver()).refindWhenNeeded(fieldOptionPopover);
        public Input xLabelInput = Input(Locator.input("x-label"), getDriver()).refindWhenNeeded(fieldOptionPopover);

        final Locator.XPathLocator radioGroupLoc = Locator.byClass("field-option-radio-group");

        public Optional<WebElement> radioGroupWithLabel(String label)
        {
            Locator.XPathLocator labelLoc = Locator.tagWithText("label", label);
            return radioGroupLoc.withChild(labelLoc).findOptionalElement(fieldOptionPopover);
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
