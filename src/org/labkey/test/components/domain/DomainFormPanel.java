package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.selenium.WebElementWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        getField(name).expand().clickRemoveField().dismiss("Yes");
        clearElementCache();
        return this;
    }

    public DomainFieldRow getField(String name)
    {
        return elementCache().findFieldRow(name);
    }

    public DomainFieldRow getField(int tabIndex)
    {
        return elementCache().findFieldRows().get(tabIndex);
    }

    public DomainFormPanel expand()
    {
        elementCache().expandIcon.click();
        return this;
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
            if (fieldRows == null)
            {
                fieldRows = new ArrayList<>();
                rowLoc.findElements(DomainFormPanel.this.getComponentElement())
                        .forEach(e -> fieldRows.add(new DomainFieldRow(DomainFormPanel.this, e, getDriver())));
            }
            return fieldRows;
        }

        private DomainFieldRow findFieldRow(String name)
        {
            if (!fieldNames.containsKey(name))
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
            }
            if (!fieldNames.containsKey(name))
                return null;
            return fieldRows.get(fieldNames.get(name));
        }

        WebElement startNewDesignLink = Locator.tagWithClass("span", "domain-form-add-link")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement expandIcon = Locator.tagWithClass("svg", "fa-plus-square")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
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

        public DomainFormPanelFinder withTitle(String title, boolean active)
        {
            _title = title;
            _active = active;
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
                Locator.XPathLocator titleLoc = Locator.tagWithClass("div", "panel-heading").startsWith(_title);
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
