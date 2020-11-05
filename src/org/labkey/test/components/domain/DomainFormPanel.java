package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.labkey.ui.core.Alert;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.selenium.WebElementWrapper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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

    public DomainFormPanel setField(FieldDefinition fieldDefinition)
    {
        DomainFieldRow fieldRow = getField(fieldDefinition.getName());
        return editField(fieldRow, fieldDefinition);
    }

    private DomainFormPanel editField(DomainFieldRow fieldRow, FieldDefinition fieldDefinition)
    {
        if (fieldDefinition.getLookup() != null)
            fieldRow.setLookup(fieldDefinition.getLookup());
        else if (fieldDefinition.getType() != null)
            fieldRow.setType(fieldDefinition.getType());

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
        if (fieldDefinition.getMvEnabled())
            fieldRow.setMissingValuesEnabled(fieldDefinition.getMvEnabled());
        if (fieldDefinition.getRequired())
            fieldRow.setRequiredField(fieldDefinition.getRequired());
        if (fieldDefinition.getLookupValidatorEnabled() != null)
            fieldRow.setLookupValidatorEnabled(fieldDefinition.getLookupValidatorEnabled());

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

        fieldRow.collapse();

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

    public DomainFieldRow manuallyDefineFields(String name)
    {
        getWrapper().scrollIntoView(elementCache().manuallyDefineButton, true);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().manuallyDefineButton)); // give modal dialogs time to disappear
        elementCache().manuallyDefineButton.click();

        DomainFieldRow newFieldRow = elementCache().findFieldRows().get(0);
        newFieldRow.setName(name);
        return newFieldRow;
    }

    public DomainFieldRow manuallyDefineFields(FieldDefinition fieldDefinition)
    {
        DomainFieldRow fieldRow = manuallyDefineFields(fieldDefinition.getName());
        setField(fieldDefinition);
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

    public File clickExportFields() throws Exception
    {
        getWrapper().scrollIntoView(elementCache().exportFieldsButton);
        File[] exportFiles =  getWrapper().doAndWaitForDownload(()-> {
            elementCache().exportFieldsButton.click();
        }, 1);
        return exportFiles[0];
    }

    public DomainFormPanel setInferFieldFile(File file)
    {
        getWrapper().setFormElement(elementCache().fileUploadInput, file);
        getWrapper().waitFor(()-> elementCache().findFieldRows().size() > 0,
                "fields were not inferred from file in time", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    // TODO: add ability to select "import data"/"don't import" here, after inferring fields from file
    // for datasets, the ability to map key columns is exposed when 'import data' is selected

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
        return getPanelAlertWebElement().getText();
    }

    /**
     * There may be an element in the alert that a test will need to interact with so return the alert element and let
     * the test find the control it needs.
     * @return The div wrapping the alert in the panel, null otherwise.
     */
    public WebElement getPanelAlertWebElement()
    {
        getWrapper().waitFor(()-> BootstrapLocators.infoBanner.existsIn(getDriver()),
                "the info alert did not appear as expected", 1000);

        // It would be better to not return a raw WebElement but who knows what the future holds, different alerts
        // may show different controls.
        return BootstrapLocators.infoBanner.existsIn(getDriver()) ? BootstrapLocators.infoBanner.findElement(getDriver()) : null;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainPanel<ElementCache, DomainFormPanel>.ElementCache
    {
        protected WebElement addFieldButton = new WebElementWrapper()
        {
            WebElement el = Locator.css(".domain-form-add-btn .btn").findWhenNeeded(DomainFormPanel.this);

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

        protected void clearFieldCache()
        {
            fieldRows = null;
            fieldNames.clear();
        }

        // Should only modify row collections with findFieldRows() and addFieldButton.click()
        private List<DomainFieldRow> fieldRows;
        private Map<String, Integer> fieldNames = new TreeMap<>();
        private final Locator rowLoc = Locator.tagWithClass("div", "domain-field-row");

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
            WebElement el = Locator.css(".domain-form-manual-btn").findWhenNeeded(DomainFormPanel.this);

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
