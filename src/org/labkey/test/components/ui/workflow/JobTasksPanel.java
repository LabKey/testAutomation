package org.labkey.test.components.ui.workflow;


import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the expand/collapse task editor pane in job create page(s)
 */
public class JobTasksPanel extends DomainPanel
{

    protected JobTasksPanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected JobTasksPanel getThis()
    {
        return this;
    }

    public List<DefineJobTaskRow> getTaskRows()
    {
        expand();
        List<DefineJobTaskRow> tasks = new ArrayList<>();
        List<WebElement> rowContainers = Locator.tagWithClassContaining("div", "domain-row-border-default").findElements(getDriver());
        if (rowContainers.size() > 0)
            rowContainers.forEach(rc -> tasks.add(new DefineJobTaskRow(rc, getDriver())));
        return tasks;
    }

    public DefineJobTaskRow getTask(String taskName)
    {
        expand();
        return getTaskRows().stream().filter(a-> a.getName().equals(taskName)).findFirst().orElse(null);
    }

    public DefineJobTaskRow addTask()
    {
        var tasks = getTaskRows();

        // If this is the first task being added size will be 1 and it will have no length.
        if( (tasks.size() > 1 ) || !tasks.get(0).getName().isEmpty() )
        {
            elementCache().addTaskBtn.click();
            tasks = getTaskRows();
        }

        return tasks.get(tasks.size() - 1);
    }

    public JobTasksPanel removeTask(String taskName)
    {
        getTask(taskName).deleteTask();
        return this;
    }

    protected boolean isLoaded()
    {
        return elementCache().addTaskBtn.isDisplayed() && getTaskRows().size() > 0;
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainPanel.ElementCache
    {

        WebElement addTaskBtn = Locator.tagWithClass("span", "container--action-button")
                .withChild(Locator.tagWithClass("i", "container--addition-icon"))
                .findWhenNeeded(this).withTimeout(2000);
    }

    public static class JobTaskPanelFinder extends BaseDomainPanelFinder<JobTasksPanel, JobTasksPanel.JobTaskPanelFinder>
    {
        public JobTaskPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected JobTasksPanel construct(WebElement el, WebDriver driver)
        {
            return new JobTasksPanel(el, driver);
        }
    }
}
