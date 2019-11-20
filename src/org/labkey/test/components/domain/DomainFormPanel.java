package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.selenium.WebElementWrapper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class DomainFormPanel extends WebDriverComponent<DomainFormPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public DomainFormPanel(WebElement element, WebDriver driver)
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

    public DomainFormPanel addField(FieldDefinition fieldDefinition)
    {
        DomainFieldRow fieldRow = addField(fieldDefinition.getName());

        if (fieldDefinition.getLookup() != null)
            throw new IllegalArgumentException("Lookups are not yet supported");
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
        if (fieldDefinition.getValidator() != null)
            throw new IllegalArgumentException("Validators are not yet supported");
        if (fieldDefinition.isMvEnabled())
            fieldRow.setMissingValue(fieldDefinition.isMvEnabled());
        if (fieldDefinition.isRequired())
            fieldRow.setRequiredField(fieldDefinition.isRequired());

        fieldRow.collapse();

        return this;
    }

    public DomainFieldRow addField(String name)
    {
        getWrapper().scrollIntoView(elementCache().addFieldButton, true);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().addFieldButton)); // give modal dialogs time to disappear
        elementCache().addFieldButton.click();
        List<DomainFieldRow> fieldRows = elementCache().findFieldRows();
        DomainFieldRow newFieldRow = fieldRows.get(fieldRows.size() - 1);

        newFieldRow.setName(name);
        return newFieldRow;
    }

    public DomainFieldRow startNewDesign(String name)
    {
        getWrapper().scrollIntoView(elementCache().startNewDesignLink, true);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().startNewDesignLink)); // give modal dialogs time to disappear
        elementCache().startNewDesignLink.click();

        DomainFieldRow newFieldRow = elementCache().findFieldRows().get(0);
        newFieldRow.setName(name);
        return newFieldRow;
    }

    public DomainFormPanel removeField(String name)
    {
        getWrapper().log("attempting to remove field " + name);
        getField(name).clickRemoveField().dismiss("Yes, Remove Field");
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

    public DomainFormPanel removeAllFields()
    {
        List<String> fieldNames = fieldNames();
        for (String name : fieldNames)
        {
            removeField(name);
        }
        return this;
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

    public DomainFormPanel expand()
    {
        if (isCollapsed())
        {
            elementCache().expandIcon.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
        }
        return this;
    }

    public boolean isExpanded()
    {
        return elementCache().collapseIconLoc.existsIn(this);
    }

    public DomainFormPanel collapse()
    {
        if (isExpanded())
        {
            elementCache().collapseIcon.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
        }
        return this;
    }

    public boolean isCollapsed()
    {
        return elementCache().expandIconLoc.existsIn(this);
    }

    public String getPanelTitle()
    {
        return elementCache().panelTitle.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
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

        WebElement startNewDesignLink = Locator.tagWithClass("span", "domain-form-add-link")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement fileUploadInput = Locator.inputById("fileUpload").findWhenNeeded(this).withTimeout(2000);

        // TODO since the Assay Properties panel also has the notion of expand/collapse,
        //  we should split that part out into an Abstract test class that both can use
        Locator.XPathLocator expandIconLoc = Locator.tagWithClass("svg", "domain-form-expand-btn");
        WebElement expandIcon = expandIconLoc.refindWhenNeeded(DomainFormPanel.this);
        Locator.XPathLocator collapseIconLoc = Locator.tagWithClass("svg", "domain-form-collapse-btn");
        WebElement collapseIcon = collapseIconLoc.refindWhenNeeded(DomainFormPanel.this);
        WebElement panelTitle = Locator.xpath("//div//span[not(@class)]").findWhenNeeded(DomainFormPanel.this);
    }

    public static class DomainFormPanelFinder extends WebDriverComponentFinder<DomainFormPanel, DomainFormPanelFinder>
    {
        final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "domain-form-panel");
        final Locator.XPathLocator _activeLocator = Locator.tagWithClass("div", "domain-form-panel").withClass("panel-active");
        private String _title = null;
        private boolean _active;

        public DomainFormPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public DomainFormPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        public DomainFormPanelFinder active()
        {
            _active = true;
            return this;
        }

        @Override
        protected DomainFormPanel construct(WebElement el, WebDriver driver)
        {
            return new DomainFormPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
            {
                Locator.XPathLocator titleLoc = Locator.tagWithClass("div", "domain-panel-header").child(Locator.tag("span")).startsWith(_title);
                return _active ? _activeLocator.withDescendant(titleLoc) : getBaseLocator().withDescendant(titleLoc);
            }
            else
                return getBaseLocator();
        }

        public Locator.XPathLocator getBaseLocator()
        {
            return _baseLocator;
        }
    }
}
