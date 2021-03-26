package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactDatePicker;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Wraps the 'Job Details and Priority' domain panel, on workflow job create pages
 */
public class JobDetailsPanel extends DomainPanel
{
    protected JobDetailsPanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected JobDetailsPanel getThis()
    {
        return this;
    }

    public JobDetailsPanel setName(String name)
    {
        expand();
        elementCache().jobNameInput.set(name);
        return this;
    }

    public String getName()
    {
        expand();
        return elementCache().jobNameInput.get();
    }

    public JobDetailsPanel setDescription(String description)
    {
        expand();
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }

    public JobDetailsPanel selectOwner(String owner)
    {
        elementCache().jobOwnerSelect.select(owner);
        return this;
    }

    public String getOwner()
    {
        return elementCache().jobOwnerSelect.getValue();
    }

    public JobDetailsPanel setNotify(List<String> notify)
    {
        for (String owner : notify)
        {
            elementCache().notifySelect.select(owner);
        }
        return this;
    }

    public List<String> getNotify()
    {
        return elementCache().notifySelect.getSelections();
    }

    public JobDetailsPanel setStartDate(String date)
    {
        elementCache().startDatePicker.set(date, false);
        return this;
    }

    public String getStartDate()
    {
        return elementCache().startDatePicker.get();
    }

    public JobDetailsPanel setDueDate(String date)
    {
        expand();
        elementCache().dueDatePicker.set(date, false);
        return this;
    }

    public String getDueDate()
    {
        expand();
        return elementCache().dueDatePicker.get();
    }

    public JobDetailsPanel setPriority(String priority)
    {
        expand();
        elementCache().prioritySelect.select(priority);
        return this;
    }

    public String getPriority()
    {
        expand();
        return elementCache().prioritySelect.getValue();
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
        Input jobNameInput = Input.Input(Locator.name("name"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(panelBody);
        Input descriptionInput = Input.Input(Locator.name("description"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(panelBody);

        WebElement jobOwnerContainer = Locator.tagWithClass("div", "top-spacing")
                .withDescendant(Locator.tagWithClass("div", "job-details-field-label").containing("Job Owner"))
                .findWhenNeeded(panelBody).withTimeout(WAIT_FOR_JAVASCRIPT);
        ReactSelect jobOwnerSelect = ReactSelect.finder(getDriver()).findWhenNeeded(jobOwnerContainer);

        WebElement notifyContainer = Locator.tagWithClass("div", "row")
                .withChild(Locator.tagWithClass("div", "job-details-field-label").containing("Notify These Users"))
                .findWhenNeeded(panelBody).withTimeout(WAIT_FOR_JAVASCRIPT);
        ReactSelect notifySelect = ReactSelect.finder(getDriver()).findWhenNeeded(notifyContainer);

        ReactDatePicker startDatePicker =  new ReactDatePicker.ReactDateInputFinder(getDriver()).withName("startDate")
                .findWhenNeeded();
        ReactDatePicker dueDatePicker =  new ReactDatePicker.ReactDateInputFinder(getDriver()).withName("dueDate")
                .findWhenNeeded();

        WebElement priorityContainer = Locator.tagWithClass("div", "row")
                .withChild(Locator.tagWithClass("div", "job-details-field-label").containing("Priority"))
                .findWhenNeeded(panelBody).withTimeout(WAIT_FOR_JAVASCRIPT);
        ReactSelect prioritySelect = ReactSelect.finder(getDriver()).findWhenNeeded(priorityContainer);
    }

    public static class JobDetailsPanelFinder extends BaseDomainPanelFinder<JobDetailsPanel, JobDetailsPanel.JobDetailsPanelFinder>
    {
        public JobDetailsPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected JobDetailsPanel construct(WebElement el, WebDriver driver)
        {
            return new JobDetailsPanel(el, driver);
        }
    }
}
