package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.list.SetDefaultValuesListPage;
import org.labkey.test.components.html.Select;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.Select.Select;

public class PropertiesEditor extends WebPartPanel
{
    public static final String EDITOR_CHANGE_SIGNAL = "propertiesEditorChange";

    WebElement _element;
    WebDriver _driver;
    private ListHelper _listHelper;

    private PropertiesEditor(WebElement element, WebDriver driver)
    {
        super(element, driver);
        _listHelper = new ListHelper(driver);
    }

    public static PropertyEditorFinder PropertyEditor(WebDriver driver)
    {
        return new PropertyEditorFinder(driver);
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _element;
    }

//    public FieldRow selectField(String name)
//    {}
//
//    public FieldRow selectField(int index)
//    {}

    public void addField(ListHelper.ListColumn column)
    {
        _listHelper.addField(column);
    }

    protected class ElementCache extends Component.ElementCache
    {
        private Map<Integer, FieldRow> fieldRows = new HashMap<>();

    }

    public static class PropertyEditorFinder extends WebPartFinder<PropertiesEditor, PropertyEditorFinder>
    {
        public PropertyEditorFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected PropertiesEditor construct(WebElement el, WebDriver driver)
        {
            return new PropertiesEditor(el, driver);
        }
    }

    public class FieldRow extends Component
    {
        WebElement _rowEl;

        private FieldRow(WebElement rowEl)
        {
            _rowEl = rowEl;
        }

        protected FieldRow(String fieldName)
        {

        }

        @Override
        public WebElement getComponentElement()
        {
            return _rowEl;
        }

        private Input name = Input(Locator.tag("td").index(6), getDriver()).findWhenNeeded(this);
        private Locator.XPathLocator fieldRowLoc = Locator.tagWithClass("table", "editor-field-row");
        private Locator requiredFieldLoc = fieldRowLoc.withChild(Locator.tag("td").index(6).child(Locator.tagWithClass("div", "gwt-Label")));
        private Locator customFieldLoc = fieldRowLoc.withDescendant(Locator.tag("input").attributeStartsWith("name", "ff_name"));
    }

    public class FieldPropertyDock extends Component
    {
        private WebElement _element = Locator.css("table.editor-property-dock").findWhenNeeded(PropertiesEditor.this.getComponentElement());
        private AdvancedTabPane _advancedTabPane;
        private ReportingTabPane _reportingTabPane;
        private ValidatorsTabPane _validatorsTabPane;
        private FormatTabPane _formatTabPane;
        private DisplayTabPane _displayTabPane;

        @Override
        public WebElement getComponentElement()
        {
            return _element;
        }

        public DisplayTabPane selectDisplayTab()
        {
            findTab("Display").click();
            if (null == _displayTabPane)
                _displayTabPane = new DisplayTabPane();
            return _displayTabPane;
        }

        public FormatTabPane selectFormatTab()
        {
            findTab("Format").click();
            if (null == _formatTabPane)
                _formatTabPane = new FormatTabPane();
            return _formatTabPane;
        }

        public ValidatorsTabPane selectValidatorsTab()
        {
            findTab("Validators").click();
            if (null == _validatorsTabPane)
                _validatorsTabPane = new ValidatorsTabPane();
            return _validatorsTabPane;
        }

        public ReportingTabPane selectReportingTab()
        {
            findTab("Reporting").click();
            if (null == _reportingTabPane)
                _reportingTabPane = new ReportingTabPane();
            return _reportingTabPane;
        }

        public AdvancedTabPane selectAdvancedTab()
        {
            findTab("Advanced").click();
            if (null == _advancedTabPane)
                _advancedTabPane = new AdvancedTabPane();
            return _advancedTabPane;
        }

        private Map<String, WebElement> tabs = new HashMap<>();

        private WebElement findTab(String tab)
        {
            if (!tabs.containsKey(tab))
                tabs.put(tab, Locator.tagWithClass("a", "x-tab-right").withText(tab).findElement(this));
            return tabs.get(tab);
        }

        private abstract class FieldTabPane extends Component
        {
            WebElement _paneElement;

            protected FieldTabPane(String tabLabel)
            {
                _paneElement = Locator.tagWithClass("div", "x-tab-panel-header")
                        .append(Locator.tagWithClass("a", "x-tab-right").withText(tabLabel))
                        .findElement(FieldPropertyDock.this);
            }

            @Override
            public WebElement getComponentElement()
            {
                return _paneElement;
            }

            public DisplayTabPane selectDisplayTab()
            {
                return FieldPropertyDock.this.selectDisplayTab();
            }

            public FormatTabPane selectFormatTab()
            {
                return FieldPropertyDock.this.selectFormatTab();
            }

            public ValidatorsTabPane selectValidatorsTab()
            {
                return FieldPropertyDock.this.selectValidatorsTab();
            }

            public ReportingTabPane selectReportingTab()
            {
                return FieldPropertyDock.this.selectReportingTab();
            }

            public AdvancedTabPane selectAdvancedTab()
            {
                return FieldPropertyDock.this.selectAdvancedTab();
            }
        }

        public class DisplayTabPane extends FieldTabPane
        {
            protected DisplayTabPane()
            {
                super("Display");
            }

            public final Input description = Input(Locator.id("propertyDescription"), getDriver()).findWhenNeeded(this);
            public final Input url = Input(Locator.id("url"), getDriver()).findWhenNeeded(this);
            public final Checkbox showInGrid = Checkbox(Locator.css("#propertyShownInGrid > input")).findWhenNeeded(this);
            public final Checkbox showInInsert = Checkbox(Locator.css("#propertyShownInInsert > input")).findWhenNeeded(this);
            public final Checkbox showInUpdate = Checkbox(Locator.css("#propertyShownInUpdate > input")).findWhenNeeded(this);
            public final Checkbox showInDetails = Checkbox(Locator.css("#propertyShownInDetails > input")).findWhenNeeded(this);
        }

        public class FormatTabPane extends FieldTabPane
        {
            protected FormatTabPane()
            {
                super("Format");
            }

            // TODO: Add conditional format stuff
        }

        public class ValidatorsTabPane extends FieldTabPane
        {
            protected ValidatorsTabPane()
            {
                super("Validators");
            }

            public final Checkbox required = Checkbox(Locator.tagWithName("input", "required")).findWhenNeeded(this);
            private final WebElement regexValidatorButton = Locator.lkButton("Add RegEx Validator").findWhenNeeded(this);
            private final WebElement rangeValidatorButton = Locator.lkButton("Add Range Validator").findWhenNeeded(this);
            private final WebElement lookupValidatorButton = Locator.lkButton("Add Lookup Validator").findWhenNeeded(this);

            // TODO: Add validator stuff
        }

        public class ReportingTabPane extends FieldTabPane
        {
            protected ReportingTabPane()
            {
                super("Reporting");
            }

            public final Checkbox measure = Checkbox(Locator.tagWithName("input", "measure")).findWhenNeeded(this);
            public final Checkbox dimension = Checkbox(Locator.tagWithName("input", "dimension")).findWhenNeeded(this);
            public final Checkbox recommendedVariable = Checkbox(Locator.tagWithName("input", "recommendedVariable")).findWhenNeeded(this);
            public final Select<ScaleType> defaultScale = Select(Locator.tagWithName("select", "gwt-ListBox")).findWhenNeeded(this);
        }

        public class AdvancedTabPane extends FieldTabPane
        {
            protected AdvancedTabPane()
            {
                super("Advanced");
            }

            public final Checkbox mvEnabledCheckbox = Checkbox(Locator.tagWithName("input", "mvEnabled")).findWhenNeeded(this);
            public final Select<DefaultType> defaultTypeSelect = Select(Locator.tagWithName("select", "gwt-ListBox")).findWhenNeeded(this);
            public SetDefaultValuesListPage clickSelectDefaultValue()
            {
                new WebDriverWrapperImpl(getDriver()).clickAndWait(Locator.linkWithText("set value"));
                return new SetDefaultValuesListPage(getDriver());
            }
            public final Input importAliasesInput = Input(Locator.id("importAliases"), getDriver()).findWhenNeeded(this);
            public final Checkbox protectedCheckbox = Checkbox(Locator.tagWithName("input", "protected")).findWhenNeeded(this);
            public final Checkbox excludeFromShiftingCheckbox = Checkbox(Locator.tagWithName("input", "excludeFromShifting")).findWhenNeeded(this);
        }
    }

    public enum ScaleType implements Select.SelectOption
    {
        linear("LINEAR", "Linear"),
        log("LOG", "Log");

        String _value;
        String _text;

        ScaleType(String value, String text)
        {
            _value = value;
            _text = text;
        }

        public String getValue()
        {
            return _value;
        }

        public String getText()
        {
            return _text;
        }
    }

    public enum DefaultType implements Select.SelectOption
    {
        FIXED_EDITABLE("FIXED_EDITABLE", "Editable default"),
        LAST_ENTERED("LAST_ENTERED", "Last entered"),
        FIXED_NON_EDITABLE("FIXED_NON_EDITABLE", "Fixed value");

        String _value;
        String _text;

        DefaultType(String value, String text)
        {
            _value = value;
            _text = text;
        }

        public String getValue()
        {
            return _value;
        }

        public String getText()
        {
            return _text;
        }
    }
}
