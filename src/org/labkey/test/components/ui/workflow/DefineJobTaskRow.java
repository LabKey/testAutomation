package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.react.ReactDatePicker;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

public class DefineJobTaskRow extends DefineTaskRowBase
{

    public DefineJobTaskRow(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    public DefineJobTaskRow setName(String name)
    {
        super.setName(name);
        return this;
    }

    @Override
    public DefineJobTaskRow setDescription(String description)
    {
        super.setDescription(description);
        return this;
    }

    public DefineJobTaskRow setDueDate(String date)
    {
        expandRow();
        elementCache().dueDate.set(date);

        return this;
    }

    public String getDueDate()
    {
        expandRow();
        return elementCache().dueDate.get();
    }

    public boolean isDueDateEnabled()
    {
        return elementCache().dueDate.getComponentElement().isEnabled();
    }

    @Override
    public DefineJobTaskRow setAssaysToPerform(String assayName)
    {
        return setAssaysToPerform(Arrays.asList(assayName));
    }

    @Override
    public DefineJobTaskRow setAssaysToPerform(List<String> assayNames)
    {
        assayNames.forEach(n -> elementCache().assays().select(n));
        return this;
    }

    @Override
    public DefineJobTaskRow clearAssaysToPerform()
    {
        elementCache().assays().clearSelection();
        return this;
    }

    @Override
    public List<String> getAssaysToPerform()
    {
        return elementCache().assays().getSelections();
    }

    public DefineJobTaskRow setAssignee(String userName)
    {
        elementCache().assignee().select(userName);
        return this;
    }

    public DefineJobTaskRow clearAssignee()
    {
        elementCache().assignee().clearSelection();
        return this;
    }

    public String getAssignee()
    {
        List<String> assigneeList = elementCache().assignee().getSelections();
        String assignee;

        if(assigneeList.size() > 0)
        {
            assignee = assigneeList.get(0);
        }
        else
        {
            assignee = "";
        }

        return assignee;
    }

    public boolean isAssigneeEnabled()
    {
        return elementCache().assignee().getComponentElement().isEnabled();
    }

    public List<String> getAssignToUserList()
    {
        return elementCache().assignee().getOptions();
    }

    @Override
    public DefineJobTaskRow deleteTask()
    {
        super.deleteTask();
        return this;
    }

    public boolean isLocked()
    {
        return Locator.tagWithClass("span", "domain-field-lock-icon").existsIn(this);
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

    protected class ElementCache extends DefineTaskRowBase.ElementCache
    {
        final ReactDatePicker dueDate = new ReactDatePicker.ReactDateInputFinder(getDriver())
                .withName("dueDate").findWhenNeeded(componentElement);

        ReactSelect assignee()
        {
            WebElement webElement = Locator
                    .tagWithClassContaining("div", "Select--single")
                    .findElement(componentElement);
            return new ReactSelect(webElement, getDriver());
        }

    }

}
