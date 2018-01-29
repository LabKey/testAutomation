/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.list.SetDefaultValuesListPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.selenium.WebElementWrapper;
import org.labkey.test.util.ExtHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.EnumSelect.EnumSelect;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;
import static org.labkey.test.params.FieldDefinition.RangeValidator;
import static org.labkey.test.params.FieldDefinition.RegExValidator;
import static org.labkey.test.util.TestLogger.log;

public class PropertiesEditor extends WebPartPanel
{
    public static final String EDITOR_CHANGE_SIGNAL = "propertiesEditorChange";
    private boolean _haveAlreadyDeletedOneFieldRow = false;

    private PropertiesEditor(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static PropertiesEditorFinder PropertiesEditor(WebDriver driver)
    {
        return new PropertiesEditorFinder(driver);
    }

    public FieldRow selectField(String name)
    {
        elementCache().findFieldRow(name).select();
        return elementCache().findFieldRow(name);
    }

    public FieldRow selectField(int index)
    {
        elementCache().findFieldRows().get(index).select();
        return elementCache().findFieldRows().get(index);
    }

    public  List<FieldRow> getFields()
    {
        return elementCache().findFieldRows();
    }

    private FieldRow getSelectedField()
    {
        return new FieldRow(Locator.css(".editor-field-row.selected-field-row").findElement(this));
    }

    private void selectLastField()
    {
        if (!elementCache().findFieldRows().isEmpty())
        {
            FieldRow lastRow = elementCache().findFieldRows().get(elementCache().findFieldRows().size() - 1);
            getWrapper().scrollIntoView(lastRow._rowEl);
            lastRow.select();
        }
    }

    public FieldRow addField(FieldDefinition col)
    {
        FieldRow newFieldRow = addField();
        newFieldRow.setName(col.getName());
        if (col.getLabel() != null)
            newFieldRow.setLabel(col.getLabel());
        newFieldRow.setType(col.getLookup(), col.getType());

        if (col.getDescription() != null)
            fieldProperties().selectDisplayTab().description.set(col.getDescription());

        if (col.getFormat() != null)
            fieldProperties().selectFormatTab().propertyFormat.set(col.getFormat());

        if (col.getURL() != null)
            fieldProperties().selectDisplayTab().url.set(col.getURL());

        if (col.isRequired())
            fieldProperties().selectValidatorsTab().required.check();

        FieldDefinition.FieldValidator validator = col.getValidator();
        if (validator != null)
        {
            final FieldPropertyDock.ValidatorsTabPane validatorsTab = fieldProperties().selectValidatorsTab();
            if (validator instanceof FieldDefinition.RegExValidator)
                validatorsTab.regexValidatorButton.click();
            else
                validatorsTab.rangeValidatorButton.click();
            getWrapper().setFormElement(Locator.name("name"), validator.getName());
            getWrapper().setFormElement(Locator.name("description"), validator.getDescription());
            getWrapper().setFormElement(Locator.name("errorMessage"), validator.getMessage());

            if (validator instanceof FieldDefinition.RegExValidator)
            {
                getWrapper().setFormElement(Locator.name("expression"), ((RegExValidator)validator).getExpression());
            }
            else if (validator instanceof RangeValidator)
            {
                getWrapper().setFormElement(Locator.name("firstRangeValue"), ((RangeValidator)validator).getFirstRange());
            }
            getWrapper().clickButton("OK", 0);
        }

        if (col.isMvEnabled())
        {
            fieldProperties().selectAdvancedTab().mvEnabledCheckbox.check();
        }

        if (col.getScale() != null)
        {
            fieldProperties().selectAdvancedTab().maxTextInput.set(col.getScale().toString());
        }

        return newFieldRow;
    }

    public FieldRow addField()
    {
        int initialRowCount = elementCache().findFieldRows().size();
        selectLastField(); // So that new field appears at the bottom
        getWrapper().scrollIntoView(elementCache().addFieldButton);
        WebDriverWrapper.waitFor(() -> {
                    elementCache().addFieldButton.click();
                    return initialRowCount + 1 == elementCache().findFieldRows().size();
                },
                "Failed to add field", 4000 );
        return getSelectedField();
    }

    public FieldPropertyDock fieldProperties()
    {
        return elementCache().fieldPropertyDock;
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebPartPanel.ElementCache
    {
        protected WebElement addFieldButton = new WebElementWrapper()
        {
            WebElement el = Locator.id("button_Add Field").findWhenNeeded(PropertiesEditor.this);
            @Override
            public WebElement getWrappedElement()
            {
                return el;
            }

            @Override
            public void click()
            {
                super.click();
                fieldRows = null;
                fieldNames = null;
            }
        };

        // Should only modify row collections with findFieldRows() and addFieldButton.click()
        private List<FieldRow> fieldRows;
        private Map<String, Integer> fieldNames = new TreeMap<>();
        private Locator rowLoc = Locator.tagWithClass("tr", "editor-field-row");

        private List<FieldRow> findFieldRows()
        {
            if (fieldRows == null)
            {
                fieldRows = new ArrayList<>();
                rowLoc.findElements(PropertiesEditor.this.getComponentElement())
                        .stream()
                        .forEachOrdered(e -> fieldRows.add(new FieldRow(e)));
            }
            return fieldRows;
        }

        private FieldRow findFieldRow(String name)
        {
            if (!fieldNames.containsKey(name))
            {
                List<FieldRow> fieldRows = findFieldRows();
                for (int i = 0; i < fieldRows.size(); i++)
                {
                    FieldRow fieldRow = fieldRows.get(i);
                    String fieldRowName = fieldRow.getName();
                    if (!fieldNames.containsValue(i) && !StringUtils.trimToEmpty(fieldRowName).isEmpty())
                        fieldNames.put(fieldRowName, i);
                    if (name.equalsIgnoreCase(fieldRowName))
                        return fieldRow;
                }
            }
            return fieldRows.get(fieldNames.get(name));
        }

        private FieldPropertyDock fieldPropertyDock = new FieldPropertyDock();
    }

    public static class PropertiesEditorFinder extends WebPartFinder<PropertiesEditor, PropertiesEditorFinder>
    {
        public PropertiesEditorFinder(WebDriver driver)
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

        @Override
        public WebElement getComponentElement()
        {
            return _rowEl;
        }

        public FieldRow select()
        {
            spacer.click();
            WebDriverWrapper.waitFor(this::isSelected, "Failed to select field row", 1000);
            return this;
        }

        private boolean isSelected()
        {
            return _rowEl.getAttribute("class").contains("selected-field-row");
        }

        public String getName()
        {
            return findNameEl().get();
        }

        public FieldRow setName(String name)
        {
            findNameEl().set(name);
            return this;
        }

        public FieldRow setLabel(String label)
        {
            labelInput.set(label);
            return this;
        }

        public FieldRow setType(FieldDefinition.LookupInfo lookupInfo, FieldDefinition.ColumnType type)
        {
            if (lookupInfo == null && type == null)
                throw new IllegalArgumentException("Specify a type or lookup");
            fieldTypeTrigger.click();
            final FieldTypeWindow window = new FieldTypeWindow();
            if (lookupInfo == null)
                window.selectType(type);
            else
                window.selectLookup(lookupInfo);

            window.clickApply();
            return this;
        }

        public PropertiesEditor markForDeletion()
        {
            Locator.xpath(".//div[contains(@id, 'partdelete_')]").waitForElement(getComponentElement(), 500).click();
            if (!_haveAlreadyDeletedOneFieldRow) // if you've already dismissed this dialog, it won't appear again
            {
                WebDriverWrapper.waitFor(() ->
                        Locator.xpath("//div[@class='gwt-Label' and contains(text(), 'Are you sure you want to remove this field?')]")
                                .findElementOrNull(getDriver()) != null, 1000);
                WebElement okBtn = Locator.lkButton("OK").findElementOrNull(getDriver());
                if (null != okBtn)
                    okBtn.click();
                _haveAlreadyDeletedOneFieldRow = true;
            }
            return PropertiesEditor.this;
        }

        public FieldPropertyDock properties()
        {
            return PropertiesEditor.this.elementCache().fieldPropertyDock;
        }

        public PropertiesEditor up()
        {
            return PropertiesEditor.this;
        }

        private FormItem<String> findNameEl()
        {
            WebElement nameEl = this.customFieldName.findElementOrNull(this);
            if (nameEl != null)
                nameInput = new Input(nameEl, getDriver());
            else
                nameInput = ReadOnlyFormItem().locatedBy(requiredFieldName).find(this);
            return nameInput;
        }

        private FormItem<String> nameInput;
        private final Locator requiredFieldName = Locator.tag("td").index(5).child(Locator.tagWithClass("div", "gwt-Label"));
        private final Locator customFieldName = Locator.tag("input").attributeStartsWith("name", "ff_name");
        private final Input labelInput = Input(Locator.tag("input").attributeStartsWith("name", "ff_label"), getDriver()).findWhenNeeded(this);
        private final WebElement spacer = Locator.css("td:last-child").findWhenNeeded(this);
        private final WebElement fieldTypeTrigger = Locator.tag("input").attributeStartsWith("name", "ff_type").append(Locator.xpath("/../div").withClass("x-form-trigger-arrow")).findWhenNeeded(this);
    }

    public class FieldPropertyDock extends Component
    {
        private WebElement _element;
        private AdvancedTabPane _advancedTabPane;
        private ReportingTabPane _reportingTabPane;
        private ValidatorsTabPane _validatorsTabPane;
        private FormatTabPane _formatTabPane;
        private DisplayTabPane _displayTabPane;

        private FieldTabPane _currentTab;

        protected FieldPropertyDock()
        {
            _element = Locator.css("table.editor-property-dock").findWhenNeeded(PropertiesEditor.this.getComponentElement());
        }

        @Override
        public WebElement getComponentElement()
        {
            return _element;
        }

        public FieldPropertyDock setFieldProperties(FieldDefinition fieldDefinition)
        {
            // TODO
            return this;
        }

        public DisplayTabPane selectDisplayTab()
        {
            findTab("Display").click();
            if (null == _displayTabPane)
                _displayTabPane = new DisplayTabPane();
            _currentTab = _displayTabPane;
            return _displayTabPane;
        }

        public FormatTabPane selectFormatTab()
        {
            findTab("Format").click();
            if (null == _formatTabPane)
                _formatTabPane = new FormatTabPane();
            _currentTab = _formatTabPane;
            return _formatTabPane;
        }

        public ValidatorsTabPane selectValidatorsTab()
        {
            findTab("Validators").click();
            if (null == _validatorsTabPane)
                _validatorsTabPane = new ValidatorsTabPane();
            _currentTab = _validatorsTabPane;
            return _validatorsTabPane;
        }

        public ReportingTabPane selectReportingTab()
        {
            findTab("Reporting").click();
            if (null == _reportingTabPane)
                _reportingTabPane = new ReportingTabPane();
            _currentTab = _reportingTabPane;
            return _reportingTabPane;
        }

        public AdvancedTabPane selectAdvancedTab()
        {
            findTab("Advanced").click();
            if (null == _advancedTabPane)
                _advancedTabPane = new AdvancedTabPane();
            _currentTab = _advancedTabPane;
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
                _paneElement = Locator.name(tabLabel + "Pane").waitForElement(FieldPropertyDock.this, 1000);
            }

            @Override
            public WebElement getComponentElement()
            {
                return _paneElement;
            }

            private DisplayTabPane selectDisplayTab()
            {
                return FieldPropertyDock.this.selectDisplayTab();
            }

            private FormatTabPane selectFormatTab()
            {
                return FieldPropertyDock.this.selectFormatTab();
            }

            private ValidatorsTabPane selectValidatorsTab()
            {
                return FieldPropertyDock.this.selectValidatorsTab();
            }

            private ReportingTabPane selectReportingTab()
            {
                return FieldPropertyDock.this.selectReportingTab();
            }

            private AdvancedTabPane selectAdvancedTab()
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

            public final Input propertyFormat = Input(Locator.id("propertyFormat"), getDriver()).findWhenNeeded(this);
            public final WebElement addConditionalFormat = Locator.lkButton("Add Conditional Format").findWhenNeeded(this);
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
            public final EnumSelect<ScaleType> defaultScale = EnumSelect(Locator.tagWithClass("select", "gwt-ListBox"), ScaleType.class).findWhenNeeded(this);
        }

        public class AdvancedTabPane extends FieldTabPane
        {
            protected AdvancedTabPane()
            {
                super("Advanced");
            }

            public final Checkbox mvEnabledCheckbox = Checkbox(Locator.tagWithName("input", "mvEnabled")).findWhenNeeded(this);
            public final EnumSelect<DefaultType> defaultTypeSelect = EnumSelect(Locator.tagWithName("select", "defaultValue"), DefaultType.class).findWhenNeeded(this);
            public SetDefaultValuesListPage clickSelectDefaultValue()
            {
                new WebDriverWrapperImpl(getDriver()).clickAndWait(Locator.linkWithText("set value"));
                return new SetDefaultValuesListPage(getDriver());
            }
            public final Input importAliasesInput = Input(Locator.id("importAliases"), getDriver()).findWhenNeeded(this);
            public final EnumSelect<PhiSelectType> phi = EnumSelect(Locator.tagWithName("select", "phiLevel"), PhiSelectType.class).findWhenNeeded(this);
            public final Checkbox excludeFromShiftingCheckbox = Checkbox(Locator.tagWithName("input", "excludeFromShifting")).findWhenNeeded(this);
            public final Checkbox maxTextCheckbox = Checkbox(Locator.tagWithName("input", "isMaxText")).findWhenNeeded(this);
            public final Input maxTextInput = Input(Locator.name("scale"), getDriver()).findWhenNeeded(this);
        }
    }

    public class FieldTypeWindow extends Window
    {
        private FieldTypeWindow()
        {
            super(ExtHelper.Locators.window("Choose Field Type").waitForElement(PropertiesEditor.this.getWrapper().shortWait()), PropertiesEditor.this.getDriver());
        }

        public FieldTypeWindow selectType(FieldDefinition.ColumnType type)
        {
            Locator.xpath("//label[text()='" + type + "']/../input[@name = 'rangeURI']").findElement(this).click();
            return this;
        }

        public FieldTypeWindow selectLookup(FieldDefinition.LookupInfo lookup)
        {
            Locator.xpath("//label[text()='Lookup']/../input[@name = 'rangeURI']").findElement(this).click();

            if (lookup.getFolder() != null)
            {
                selectLookupComboItem("lookupContainer", lookup.getFolder());
            }

            if (!lookup.getSchema().equals(getWrapper().getFormElement(Locator.css("input[name=schema]").findElement(this))))
            {
                selectLookupComboItem("schema", lookup.getSchema());
            }
            else
                Locator.tagWithClass("div", "test-marker-" + lookup.getSchema()).childTag("input").withAttribute("name", "schema").waitForElement(this, WAIT_FOR_JAVASCRIPT);

            selectLookupTableComboItem(lookup.getTable(), lookup.getTableType());
            return this;
        }

        private void selectLookupComboItem(String fieldName, String value)
        {
            selectLookupComboItem(fieldName, value, 1);
        }

        private void selectLookupComboItem(String fieldName, String value, int attempt)
        {
            log("Select lookup combo item '" + fieldName + "', value=" + value + ", attempt=" + attempt);
            Locator.tagWithName("input", fieldName)
                    .followingSibling("div")
                    .withClass("x-form-trigger").findElement(this).click();
            try
            {
                getWrapper().scrollIntoView(Locator.tag("div").withClass("x-combo-list-item").withText(value), false);
                getWrapper().waitAndClick(500 * attempt, Locator.tag("div").withClass("x-combo-list-item").withText(value), 0);
                log(".. selected");
            }
            catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
            {
                // Stop after 4 attempts
                if (attempt >= 4)
                    throw retry;

                getWrapper().fireEvent(Locator.tagWithName("input", fieldName).findElement(this), BaseWebDriverTest.SeleniumEvent.blur);
                selectLookupComboItem(fieldName, value, attempt + 1);
            }

            try
            {
                Locator.tagWithClass("div", "test-marker-" + value).childTag("input").withAttribute("name", fieldName).waitForElement(this, WAIT_FOR_JAVASCRIPT);
                log(".. test-marker updated");
            }
            catch (NoSuchElementException ignore)
            {
                log(".. failed to update test-marker, soldier on anyway");
            }
        }

        private void selectLookupTableComboItem(String table, String tableType)
        {
            selectLookupTableComboItem(table, tableType, 1);
        }

        private void selectLookupTableComboItem(String table, String tableType, int attempt)
        {
            final String comboSubstring =
                    null == tableType || tableType.isEmpty() ?
                            table + " (" :
                            String.format("%s (%s)", table, tableType);
            log("Select lookup table combo item '" + table + "', attempt=" + attempt);
            String fieldName = "table";
            Locator.tagWithName("input", fieldName).followingSibling("div").withClass("x-form-trigger").findElement(this).click();
            try
            {
                getWrapper().waitAndClick(Locator.tag("div").withClass("x-combo-list-item").startsWith(comboSubstring));
            }
            catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
            {
                // Stop after 4 attempts
                if (attempt >= 4)
                    throw retry;

                getWrapper().fireEvent(Locator.tagWithName("input", fieldName).findElement(this), BaseWebDriverTest.SeleniumEvent.blur);
                selectLookupTableComboItem(table, tableType, attempt + 1);
            }
            Locator.tagWithClass("div", "test-marker-" + table).childTag("input").withAttribute("name", fieldName).waitForElement(this, WAIT_FOR_JAVASCRIPT);
        }

        public void clickApply()
        {
            Locator.button("Apply").findElement(this).click();
            waitForClose();
        }

        public void clickCancel()
        {
            Locator.button("Cancel").findElement(this).click();
            waitForClose();
        }
    }

    public enum ScaleType implements OptionSelect.SelectOption
    {
        LINEAR("Linear"),
        LOG("Log");

        String _text;

        ScaleType(String text)
        {
            _text = text;
        }

        public String getValue()
        {
            return name();
        }

        public String getText()
        {
            return _text;
        }
    }

    public enum DefaultType implements OptionSelect.SelectOption
    {
        FIXED_EDITABLE("Editable default"),
        LAST_ENTERED("Last entered"),
        FIXED_NON_EDITABLE("Fixed value");

        String _text;

        DefaultType(String text)
        {
            _text = text;
        }

        public String getValue()
        {
            return name();
        }

        public String getText()
        {
            return _text;
        }
    }

    public enum PhiSelectType
    {
        NotPHI("Not PHI", org.labkey.api.data.PHI.NotPHI.ordinal(), null),
        Limited("Limited PHI", org.labkey.api.data.PHI.Limited.ordinal(), "Limited PHI Reader"),
        PHI("Full PHI", org.labkey.api.data.PHI.PHI.ordinal(), "Full PHI Reader"),
        Restricted("Restricted PHI", org.labkey.api.data.PHI.Restricted.ordinal(), "Restricted PHI Reader");

        String _text;
        int _rank;
        private String _roleName;

        PhiSelectType(String text, int rank, String roleName)
        {
            _text = text;
            _rank = rank;
            _roleName = roleName;
        }

        public String getValue()
        {
            return name();
        }

        public String getText()
        {
            return _text;
        }

        public int getRank()
        {
            return _rank;
        }

        public String getRoleName()
        {
            return _roleName;
        }
    }
}
