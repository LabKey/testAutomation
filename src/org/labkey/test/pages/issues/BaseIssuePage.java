package org.labkey.test.pages.issues;

import org.labkey.test.components.ComponentElements;
import org.labkey.test.components.labkey.FormItem;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.TreeMap;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

public abstract class BaseIssuePage extends LabKeyPage
{
    Elements _elements;

    protected BaseIssuePage(WebDriver driver)
    {
        super(driver);
    }

    public FormItem<String> status()
    {
        return elements().status;
    }

    public FormItem<String> assignedTo()
    {
        return elements().assignedTo;
    }

    public FormItem<String> priority()
    {
        return elements().priority;
    }

    public FormItem<String> related()
    {
        return elements().related;
    }

    public FormItem<String> resolution()
    {
        return elements().resolution;
    }

    public FormItem<String> duplicate()
    {
        return elements().duplicate;
    }

    public FormItem<String> notifyList()
    {
        return elements().notifyList;
    }

    public FormItem getCustomField(String label)
    {
        return elements().getCustomFormItem(label);
    }

    public String openedDate()
    {
        return elements().openedDate.getValue();
    }

    public String closedDate()
    {
        return elements().closedDate.getValue();
    }

    public String changedDate()
    {
        return elements().changedDate.getValue();
    }

    public String resolvedDate()
    {
        return elements().resolvedDate.getValue();
    }

    protected Elements elements()
    {
        if (_elements == null)
            _elements = newElements();
        return _elements;
    }

    protected abstract Elements newElements();

    protected class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

        protected FormItem<String> status = ReadOnlyFormItem(getDriver()).withLabel("Status").findWhenNeeded();
        protected FormItem<String> assignedTo = ReadOnlyFormItem(getDriver()).withLabel("Assigned To").findWhenNeeded();
        protected FormItem<String> priority = ReadOnlyFormItem(getDriver()).withLabel("Pri").findWhenNeeded();
        protected FormItem<String> resolution = ReadOnlyFormItem(getDriver()).withLabel("Resolution").findWhenNeeded();
        protected FormItem<String> duplicate = ReadOnlyFormItem(getDriver()).withLabel("Duplicate").findWhenNeeded();
        protected FormItem<String> related = ReadOnlyFormItem(getDriver()).withLabel("Related").findWhenNeeded();
        protected FormItem<String> notifyList = ReadOnlyFormItem(getDriver()).withLabel("Notify").findWhenNeeded();
        protected FormItem<String> openedDate = ReadOnlyFormItem(getDriver()).withLabel("Opened").findWhenNeeded();
        protected FormItem<String> changedDate = ReadOnlyFormItem(getDriver()).withLabel("Changed").findWhenNeeded();
        protected FormItem<String> resolvedDate = ReadOnlyFormItem(getDriver()).withLabel("Resolved").findWhenNeeded();
        protected FormItem<String> closedDate = ReadOnlyFormItem(getDriver()).withLabel("Closed").findWhenNeeded();

        private Map<String, FormItem> customFormItems = new TreeMap<>();

        protected FormItem getCustomFormItem(String label)
        {
            if (!customFormItems.containsKey(label))
                customFormItems.put(label, ReadOnlyFormItem(getDriver()).withLabel(label).findWhenNeeded());
            return customFormItems.get(label);
        }
    }
}