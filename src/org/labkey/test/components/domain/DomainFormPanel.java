package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.selenium.WebElementWrapper;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/DomainForm.tsx
 */
public class DomainFormPanel extends DomainPanel<DomainFormPanel.ElementCache, DomainFormPanel>
{
    public DomainFormPanel(DomainPanel<?,?> panel)
    {
        super(panel);
    }

    private DomainFormPanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static List<AdvancedFieldSetting> advancedSettingsFromFieldDefinition(FieldDefinition def)
    {
        List<AdvancedFieldSetting> advancedSettings = new ArrayList<>();

        //TODO: Add missing settings to 'FieldDefinitions:
        //      Show in default view
        //      Default type
        //      Exclude from shifting
        //      Recommended variable

        if (def.getShownInUpdateView() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.shownInUpdateView(def.getShownInUpdateView()));
        }
        if (def.getShownInInsertView() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.shownInInsertView(def.getShownInInsertView()));
        }
        if (def.getShownInDetailsView() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.shownInDetailsView(def.getShownInDetailsView()));
        }
        if (def.getMeasure() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.measure(def.getMeasure()));
        }
        if (def.getDimension() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.measure(def.getDimension()));
        }
        if (def.getMvEnabled() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.mvEnabled(def.getMvEnabled()));
        }
        if (def.getPHI() != null)
        {
            advancedSettings.add(AdvancedFieldSetting.PHI(def.getPhiLevel()));
        }

        return advancedSettings;
    }

    @Override
    protected DomainFormPanel getThis()
    {
        return this;
    }

    public DomainFormPanel addField(FieldDefinition fieldDefinition)
    {
        DomainFieldRow fieldRow = addField(fieldDefinition.getName());
        return editField(fieldRow, fieldDefinition);
    }

    public DomainFormPanel addFields(List<FieldDefinition> fieldDefinitions)
    {
        for (FieldDefinition fieldDefinition : fieldDefinitions)
        {
            addField(fieldDefinition);
        }
        return this;
    }

    public DomainFormPanel setField(FieldDefinition fieldDefinition)
    {
        DomainFieldRow fieldRow = getField(fieldDefinition.getName());
        return editField(fieldRow, fieldDefinition);
    }

    private DomainFormPanel editField(DomainFieldRow fieldRow, FieldDefinition fieldDefinition)
    {
        if (fieldDefinition.getType() != null)
        {
            if (fieldDefinition.getType().isLookup())
                fieldRow.setLookup(fieldDefinition.getLookup());
            else
                fieldRow.setType(fieldDefinition.getType());
        }

        if (fieldDefinition.getDescription() != null)
            fieldRow.setDescription(fieldDefinition.getDescription());
        if (fieldDefinition.getLabel() != null)
            fieldRow.setLabel(fieldDefinition.getLabel());
        if (fieldDefinition.getFormat() != null)
            fieldRow.setNumberFormat(fieldDefinition.getFormat());
        if (fieldDefinition.getScale() != null)
            fieldRow.setCharCount(fieldDefinition.getScale());
        if (fieldDefinition.getURL() != null)
            fieldRow.setUrl(fieldDefinition.getURL());
        if (fieldDefinition.getImportAliases() != null)
            fieldRow.setImportAliases(fieldDefinition.getImportAliases());
        if (fieldDefinition.getRequired())
            fieldRow.setRequiredField(fieldDefinition.getRequired());
        if (fieldDefinition.getLookupValidatorEnabled() != null)
            fieldRow.setLookupValidatorEnabled(fieldDefinition.getLookupValidatorEnabled());
        if (fieldDefinition.getAliquotOption() != null)
            fieldRow.setAliquotOption(fieldDefinition.getAliquotOption());
        // ontology-specific
        if (fieldDefinition.getSourceOntology() != null)
            fieldRow.setSelectedOntology(fieldDefinition.getSourceOntology());
        if (fieldDefinition.getConceptImportColumn() != null)
            fieldRow.setConceptImportField(fieldDefinition.getConceptImportColumn());
        if (fieldDefinition.getConceptLabelColumn() != null)
            fieldRow.setConceptLabelField(fieldDefinition.getConceptLabelColumn());
        if (fieldDefinition.getConceptSubTree() != null)
        {
            var subTreePath = Arrays.asList(fieldDefinition.getConceptSubTree().split("/"));
            fieldRow.clickExpectedVocabulary()
                    .selectOntology(fieldDefinition.getPrincipalConceptSearchSourceOntology())
                    .selectNodeFromPath(subTreePath)
                    .clickApply();
        }
        if (fieldDefinition.getPrincipalConceptCode() != null)
            fieldRow.clickSelectConcept()
                    .selectOntology(fieldDefinition.getPrincipalConceptSearchSourceOntology())
                    .searchConcept(fieldDefinition.getPrincipalConceptSearchExpression(), fieldDefinition.getPrincipalConceptCode())
                    .waitForActiveTreeNode()
                    .clickApply();

        if (fieldDefinition.getValidators() != null && !fieldDefinition.getValidators().isEmpty())
        {
            List<FieldDefinition.RegExValidator> regexValidators = new ArrayList<>();
            List<FieldDefinition.RangeValidator> rangeValidators = new ArrayList<>();

            List<FieldDefinition.FieldValidator<?>> validators = fieldDefinition.getValidators();
            for (FieldDefinition.FieldValidator<?> validator : validators)
            {
                if (validator instanceof FieldDefinition.RegExValidator)
                {
                    regexValidators.add((FieldDefinition.RegExValidator) validator);
                }
                else if (validator instanceof FieldDefinition.RangeValidator)
                {
                    rangeValidators.add((FieldDefinition.RangeValidator) validator);
                }
                else if (validator instanceof FieldDefinition.TextChoiceValidator textChoiceValidator)
                {
                    // TextChoice is a field type; implemented using a special validator. TextChoice field cannot have other validators.
                    if (validators.size() > 1)
                    {
                        throw new IllegalArgumentException("TextChoice fields cannot have additional validators.");
                    }
                    fieldRow.setTextChoiceValues(textChoiceValidator.getValues());
                }
                else
                {
                    throw new IllegalArgumentException("Validator not supported: " + validator.getClass().getName());
                }
            }

            if (!regexValidators.isEmpty())
            {
                fieldRow.setRegExValidators(regexValidators);
            }
            if (!rangeValidators.isEmpty())
            {
                fieldRow.setRangeValidators(rangeValidators);
            }
        }

        fieldRow.setAdvancedSettings(advancedSettingsFromFieldDefinition(fieldDefinition));

        fieldRow.collapse();

        return this;
    }

    /**
     * Get the fields panel without adding/creating a field. Can be useful when testing fields that should be auto-created.
     *
     * @return A {@link DomainFormPanel}
     */
    public DomainFormPanel clickManuallyDefineFields()
    {
        getWrapper().scrollIntoView(elementCache().manuallyDefineButton, true);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().manuallyDefineButton)); // give modal dialogs time to disappear
        elementCache().manuallyDefineButton.click();

        return this;
    }

    public DomainFieldRow addField(String name)
    {
        if (isManuallyDefineFieldsPresent())
            return manuallyDefineFields(name);

        getWrapper().scrollIntoView(elementCache().addFieldButton, true);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().addFieldButton)); // give modal dialogs time to disappear
        elementCache().addFieldButton.click();

        List<DomainFieldRow> fieldRows = elementCache().findFieldRows();
        DomainFieldRow newFieldRow = fieldRows.get(fieldRows.size() - 1);
        newFieldRow.setName(name);
        return newFieldRow;
    }

    public boolean isManuallyDefineFieldsPresent()
    {
        return getThis().findElements(elementCache().manuallyDefineFieldsLoc).size() > 0;
    }

    /**
     * Switch from "Import or infer fields from file" mode to "Manually define fields" mode.
     * The designer adds a field automatically.
     * This is only valid for a domain with no fields defined (usually a newly created domain)
     * @param name Name will be applied to the automatically added field.
     * @return row for the initially added field
     */
    public DomainFieldRow manuallyDefineFields(String name)
    {
        clickManuallyDefineFields();

        DomainFieldRow newFieldRow = elementCache().findFieldRows().get(0);
        newFieldRow.setName(name);
        return newFieldRow;
    }

    public DomainFieldRow manuallyDefineFields(FieldDefinition fieldDefinition)
    {
        DomainFieldRow fieldRow = manuallyDefineFields(fieldDefinition.getName());
        editField(fieldRow, fieldDefinition);
        return fieldRow;
    }

    public DomainFormPanel removeField(String name)
    {
        return  removeField(name, false);
    }

    public DomainFormPanel removeField(String name, boolean confirmDialogExpected)
    {
        getWrapper().log("attempting to remove field " + name);
        getField(name).clickRemoveField(confirmDialogExpected);
        clearElementCache();
        return this;
    }

    public DomainFieldRow getField(String name)
    {
        DomainFieldRow row = elementCache().findFieldRow(name);
        scrollRowIntoView(row);
        return row;
    }

    public DomainFieldRow getField(int tabIndex)
    {
        DomainFieldRow row = elementCache().findFieldRows().get(tabIndex);
        scrollRowIntoView(row);
        return row;
    }

    private void scrollRowIntoView(DomainFieldRow row)
    {
        if (null != row)    // only do this if it's non-null
        {
            getWrapper().scrollIntoView(row.getComponentElement());
        }
    }

    public DomainFormPanel removeAllFields(boolean confirmDialogExpected)
    {
        List<String> fieldNames = fieldNames();
        for (String name : fieldNames)
        {
            removeField(name, confirmDialogExpected);
        }
        return this;
    }

    public DomainFormPanel checkSelectAll(boolean value)
    {
        elementCache().selectAll.set(value);
        return this;
    }

    public File clickExportFields() throws Exception
    {
        getWrapper().scrollIntoView(elementCache().exportFieldsButton);
        File[] exportFiles =  getWrapper().doAndWaitForDownload(()-> {
            elementCache().exportFieldsButton.click();
        }, 1);
        return exportFiles[0];
    }

    public DomainFormPanel clickDeleteFields()
    {
        getWrapper().scrollIntoView(elementCache().deleteFieldsButton);
        elementCache().deleteFieldsButton.click();

        ModalDialog confirmDialog = new ModalDialog.ModalDialogFinder(getDriver())
                .withTitle("Confirm Delete Selected Fields").timeout(1000).waitFor();
        confirmDialog.dismiss("Yes, Delete Fields");

        return this;
    }

    // default system fields
    /*
        Default System Fields, for the nonce, will appear in the 'Fields' form of the domain designer for Sample and
        DataClass domains. They will be shown in a ResponsiveGrid that can be shown/hidden via a toggle
     */

    /*
        shows the DefaultSystemFields grid if it is available and not shown
     */
    public DomainFormPanel expandDefaultSystemFields()
    {
        if (!isDefaultSystemFieldsExpanded())
            elementCache().defaultSystemFieldsToggle().click();

        WebDriverWrapper.waitFor(()-> isDefaultSystemFieldsExpanded(),
                "the default system fields display did not expand in time", 2000);
        return this;
    }

    /*
        collapses the DefaultSystemFields grid if it is available and expanded
    */
    public DomainFormPanel collapseDefaultSystemFields()
    {
        if (isDefaultSystemFieldsExpanded())
            elementCache().defaultSystemFieldsToggle().click();

        WebDriverWrapper.waitFor(()-> !isDefaultSystemFieldsExpanded(),
                "the default system fields display did not collapse in time", 2000);
        return this;
    }

    public ResponsiveGrid getDefaultSystemFieldsGrid()
    {
        expandDefaultSystemFields();
        WebElement gridContainer = Locator.tagWithClass("div", "domain-system-fields__grid")
                .waitForElement(this, 2000);
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).waitFor(gridContainer);
    }

    /*
        When expanded, the i tag will show a minus, when collapsed it will advertise its expandability
        by showing a "+" icon
     */
    public boolean isDefaultSystemFieldsExpanded()
    {
        var toggleClass = elementCache().defaultSystemFieldsToggle().getAttribute("class");
        return toggleClass != null && toggleClass.contains("fa-minus-square");
    }

    /*
        Mode, in this context, means how the fields in this domain will be shown.
        Mode is toggled via the toggle in the custom fields action bar.
        in 'Summary mode', fields are shown in a grid, with columns representing field attributes like 'name', 'range uri', etc
        in 'Detail mode', fields appear as editable, in FieldRows
     */
    public String getMode()
    {
        return elementCache().customFieldsViewToggle.getSelectedStatus(); // will be either "Summary" or "Detail"
    }

    /**
     * Selects the desired mode.  Possible values are "Summary" and "Detail"
     * @param name The name of the desired mode, or the text to appear in the toggle in its desired state
     * @return an instance of the current DomainFormPanel
     */
    public DomainFormPanel switchMode(String name)
    {
        if (!getMode().equalsIgnoreCase(name))
        {
            boolean isSummary = elementCache().customFieldsViewToggle.isEnabled();
            elementCache().customFieldsViewToggle.set(!isSummary);
        }
        WebDriverWrapper.waitFor(()-> getMode().equalsIgnoreCase(name),
                "the mode select toggle did not become [" +name+ "] as expected", 2000);

        return this;
    }

    /*
        Summary mode shows a responsive grid containing a row per field, with columns for field attributes
    */
    public boolean isSummaryMode()
    {
        return getMode().equalsIgnoreCase("Summary");
    }

    /*
        gets the grid containing row data/metadata in summary view
     */
    public ResponsiveGrid getSummaryModeGrid()
    {
        switchMode("Summary");
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver())
                .locatedBy(Locator.tagWithClass("div", "domain-field-toolbar").followingSibling("div").followingSibling("div"))
                .waitFor(this);
    }

    /*

     */
    public DomainFormPanel clickSummaryGridNameLink(String linkText)
    {
        var targetCell = getSummaryModeGrid().getRow("Name", linkText).getCell("Name");
        var clickable = Locator.linkWithText(linkText).findElement(targetCell);
        clickable.click();
        WebDriverWrapper.waitFor(()-> isDetailMode(),
                "DomainFormPanel did not switch to detail mode as expected", 2000);
        return this;
    }

    public List<String> getSummaryModeColumns()
    {
        var rawColumns = getSummaryModeGrid().getColumnNames();
        return rawColumns.subList(1, rawColumns.size());    // omit the 'select' column, it is an empty value
    }

    /*
        Detail mode is the default view, it shows the list of (expandable, collapsable, editable) fieldRows
     */
    public boolean isDetailMode()
    {
        return getMode().equalsIgnoreCase("Detail");
    }

    /*
        Summary mode shows a table containing a row per field, with columns representing field attributes
     */
    public int getRowcountInSummaryMode()
    {
        switchMode("Summary");

        return getSummaryModeGrid().getRows().size();
    }

    public DomainFormPanel setInferFieldFile(File file)
    {
        getWrapper().setFormElement(elementCache().fileUploadInput, file);
        getWrapper().waitFor(()-> elementCache().findFieldRows().size() > 0,
                "fields were not inferred from file in time", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public List<String> fieldNames()
    {
        return elementCache().findFieldRows()
                .stream()
                .map(DomainFieldRow::getName)
                .collect(Collectors.toList());
    }

    public String getPanelErrorText()
    {
        return getPanelErrorWebElement().getText();
    }

    public WebElement getPanelErrorWebElement()
    {
        getWrapper().waitFor(()-> BootstrapLocators.errorBanner.existsIn(getDriver()),
                "the error alert did not appear as expected", 1000);

        // It would be better to not return a raw WebElement but who knows what the future holds, different alerts
        // may show different controls.
        return BootstrapLocators.errorBanner.existsIn(getDriver()) ? BootstrapLocators.errorBanner.findElement(getDriver()) : null;
    }

    /**
     * Get the alert message that is shown only in the alert panel. An example of this is the Results Field in
     * Sample Manager requires a field that is a sample look-up, if it missing an alert is shown.
     * This alert can only be dismissed by adding the field.
     * @return String of the alert message, empty if not present.
     */
    public String getPanelAlertText()
    {
        WebElement alertEl = getPanelAlertWebElement();
        if (alertEl != null)
            return alertEl.getText();

        return "";
    }

    public String getPanelAlertText(int index)
    {
        return getPanelAlertWebElement(index).getText();
    }

    /**
     * There may be an element in the alert that a test will need to interact with so return the alert element and let
     * the test find the control it needs.
     * @return The div wrapping the alert in the panel, null otherwise.
     */
    public WebElement getPanelAlertWebElement()
    {
        return getPanelAlertWebElement(0);
    }

    public WebElement getPanelAlertWebElement(int index)
    {
        try
        {
            getWrapper().waitFor(() -> BootstrapLocators.infoBanner.existsIn(getDriver()),
                    "the info alert did not appear as expected", 1000);
        }
        catch (TimeoutException e)
        {
            return null;
        }

        // It would be better to not return a raw WebElement but who knows what the future holds, different alerts
        // may show different controls.
        return BootstrapLocators.infoBanner.index(index).findElement(this);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainPanel<ElementCache, DomainFormPanel>.ElementCache
    {
        public final Checkbox selectAll = new Checkbox(Locator.tagWithAttributeContaining("input", "id", "domain-select-all-checkbox")
                .findWhenNeeded(this));

        public final WebElement toggleButton = Locator.tagWithAttributeContaining("div", "id", "domain-toggle-summary").
                findWhenNeeded(this);
        public final ToggleButton customFieldsViewToggle = new ToggleButton.ToggleButtonFinder(getDriver())
                .withState("Detail").timeout(5000).findWhenNeeded(this);

        protected WebElement addFieldButton = new WebElementWrapper()
        {
            final WebElement el = Locator.css(".domain-form-add-btn .btn").findWhenNeeded(DomainFormPanel.this);

            @Override
            public WebElement getWrappedElement()
            {
                return el;
            }

            @Override
            public void click()
            {
                int initialCount = findFieldRows().size();
                super.click();
                WebDriverWrapper.waitFor(() -> {
                    clearFieldCache();
                    return findFieldRows().size() == initialCount + 1;
                }, "New field didn't appear", 10000);
            }
        };

        protected WebElement exportFieldsButton = Locator.tagWithClass("div", "domain-toolbar-export-btn")
                .findWhenNeeded(this);

        protected WebElement deleteFieldsButton = Locator.tagWithClass("div", "domain-toolbar-delete-btn")
                .findWhenNeeded(this);

        protected void clearFieldCache()
        {
            fieldRows = null;
            fieldNames.clear();
        }

        // Should only modify row collections with findFieldRows() and addFieldButton.click()
        private List<DomainFieldRow> fieldRows;
        private final Map<String, Integer> fieldNames = new TreeMap<>();
        private final Locator rowLoc = Locator.tagWithClass("div", "domain-field-row").withoutClass("domain-floating-hdr");

        private List<DomainFieldRow> findFieldRows()
        {
            fieldRows = new ArrayList<>();          // this method used to cache this arraylist,
                                                    // but it was too fragile and didn't save us much runtime
                                                    // now we look for it when we ask for it
            rowLoc.findElements(DomainFormPanel.this.getComponentElement())
                    .forEach(e -> fieldRows.add(new DomainFieldRow(DomainFormPanel.this, e, getDriver())));
            return fieldRows;
        }

        private DomainFieldRow findFieldRow(String name)
        {
            List<DomainFieldRow> fieldRows = findFieldRows();
            for (int i = 0; i < fieldRows.size(); i++)
            {
                DomainFieldRow fieldRow = fieldRows.get(i);
                String fieldRowName = fieldRow.getName();
                if (!fieldNames.containsValue(i) && !StringUtils.trimToEmpty(fieldRowName).isEmpty())
                {
                    fieldNames.put(fieldRowName, i);
                }
                if (name.equalsIgnoreCase(fieldRowName))
                {
                    return fieldRow;
                }
            }
            if (!fieldNames.containsKey(name))
                return null;
            return fieldRows.get(fieldNames.get(name));
        }

        Locator.XPathLocator manuallyDefineFieldsLoc = Locator.tagWithClass("div", "domain-form-manual-btn");
        protected WebElement manuallyDefineButton = new WebElementWrapper()
        {
            final WebElement el = Locator.css(".domain-form-manual-btn").findWhenNeeded(DomainFormPanel.this);

            @Override
            public WebElement getWrappedElement()
            {
                return el;
            }

            @Override
            public void click()
            {
                super.click();
                WebDriverWrapper.waitFor(() -> {
                    clearFieldCache();
                    return findFieldRows().size() == 1;
                }, "New manually defined field didn't appear", 10000);
            }
        };

        WebElement fileUploadInput = Locator.tagWithClass("input", "file-upload--input").findWhenNeeded(DomainFormPanel.this).withTimeout(2000);

        WebElement defaultSystemFieldsContainer = Locator.tagWithClass("div", "domain-system-fields")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement defaultSystemFieldsToggle()
        {
            return Locator.tagWithClass("div", "domain-system-fields-header__icon")
                    .child("i").waitForElement(defaultSystemFieldsContainer, 2000);
        }

    }

    /**
     * This will find any domain panel.
     * There is no simple method to differentiate field editor panels from other domain panels
     */
    public static class DomainFormPanelFinder extends BaseDomainPanelFinder<DomainFormPanel, DomainFormPanelFinder>
    {
        public DomainFormPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected DomainFormPanelFinder getThis()
        {
            return this;
        }

        @Override
        protected DomainFormPanel construct(WebElement el, WebDriver driver)
        {
            return new DomainFormPanel(el, driver);
        }
    }
}
