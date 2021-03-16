package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorkflowOverviewJobPanel extends WebDriverComponent<WorkflowOverviewJobPanel.ElementCache>
{
    private final WebDriver driver;
    private final WebElement componentElement;

    public WorkflowOverviewJobPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        componentElement = element;
    }

    @Override
    public WebElement getComponentElement()
    {
        return componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    // TODO If the user is a reader and does not have permissions to update fields in this panel, the controls
    //  rendered are different. Or if a job has been marked as complete the controls are different.
    //  Basically everything is a span, this makes the controls in the cache not accurate and
    //  will throw a NoSuchElement, or StaleElement exception if accessed. This panel could be changed to use locators
    //  and not the cache and then have a conditional check in the getters to return the text from the appropriate
    //  control.
    //  But that is a job for a different time.

    /**
     * The panel will be considered loaded if there are no spinners showing.
     *
     * @return True if no spinners are showing, false otherwise.
     */
    public boolean isPanelLoaded()
    {
        // Make sure that a spinner is not shown in the Job Overview panel.
        try
        {
            // If the spinner is there isDisplayed will be true, and !true = false so need to wait.
            return !Locators.spinner.findElement(componentElement).isDisplayed();
        }
        catch (NoSuchElementException | StaleElementReferenceException nse)
        {
            // Once the spinner is gone (or was never there) we should return true (no need to wait).
            return true;
        }
    }

    /**
     * Get the status indicated at the top of the 'Job Overview' panel.
     * @return A string with the status.
     */
    public String getJobStatus()
    {
        return elementCache().jobStatus.getText();
    }

    /**
     * This private function and is used to find the text of a field in the panel.
     * From the overview panel get the string value for the given field.
     * This should be used for "simple" fields:
     * <ul>
     *     <li>Created On</li>
     *     <li>Created By</li>
     * </ul>
     * @param fieldName The label next to the field to get.
     * @return String value of the field.
     */
    private String getOverviewField(String fieldName)
    {
        return Locators.overviewField(fieldName).findElement(componentElement).getText();
    }

    /**
     * Get start date of the job.
     * @return String of the start date.
     */
    public String getStartDate()
    {
        return getWrapper().getFormElement(elementCache().startDateInput);
    }

    /**
     * Set the start date of the job
     * @param date A string of the date to set the field to.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel setStartDate(String date)
    {
        getWrapper().setFormElement(elementCache().startDateInput, date);

        // Hack to make the calendar go away after date is set.
        componentElement.click();

        return this;
    }

    /**
     * Click the "x" button to clear the 'Start Date' field.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel clearStartDate()
    {
        Locator.tag("button").findElement(elementCache().startDateInput).click();
        return this;
    }

    /**
     * Get due date of the job.
     * @return String of the due date.
     */
    public String getDueDate()
    {
        return getWrapper().getFormElement(elementCache().dueDateInput);
    }

    /**
     * Set the start date of the job
     * @param date A string of the date to set the field to.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel setDueDate(String date)
    {
        getWrapper().setFormElement(elementCache().dueDateInput, date);

        // Hack to make the calendar go away after date is set.
        componentElement.click();

        return this;
    }

    /**
     * Click the "x" button to clear the 'Due Date' field.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel clearDueDate()
    {
        Locator.tag("button").findElement(elementCache().dueDateInput).click();
        return this;
    }

    /**
     * Get the 'Assignee' field.
     * @return The name of the assignee, empty string if not assigned.
     * @throws IllegalStateException if there is more than one assignee.
     */
    public String getAssignedTo()
    {
        List<String> selections = elementCache().assigneeSelect().getSelections();
        if (selections.size() > 1)
        {
            throw new IllegalStateException("There should only be one assignee: " + selections.toString());
        }
        else if (selections.isEmpty())
        {
            return "";
        }
        else
        {
            return selections.get(0);
        }
    }

    /**
     * Set the 'Assignee' field.
     * @param assignee The name of the user to assign this job to.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel setAssignedTo(String assignee)
    {
        elementCache().assigneeSelect().select(assignee);
        return this;
    }

    /**
     * Clears the 'Assignee' field.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel clearAssignedTo()
    {
        elementCache().assigneeSelect().clearSelection();
        return this;
    }

    /**
     * Get the priority for this job.
     * @return A list containing the value of the priority field.
     * @throws IllegalStateException If there is more than one value for the priority.
     */
    public String getPriority()
    {
        List<String> selections = elementCache().prioritySelect().getSelections();
        if (selections.size() > 1)
        {
            throw new IllegalStateException("There should only be one value for priority: " + selections.toString());
        }
        else if (selections.isEmpty())
        {
            return "";
        }
        else
        {
            return selections.get(0);
        }
    }

    /**
     * Set the priority of the field.
     * Pre-defined priority values are:
     * <ul>
     *     <li>Low</li>
     *     <li>Medium</li>
     *     <li>High</li>
     *     <li>Urgent</li>
     * </ul>
     * @param priority String value to set the priority. Must be a value in the list.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel setPriority(String priority)
    {
        elementCache().prioritySelect().select(priority);
        return this;
    }

    /**
     * Clear the priority field.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel clearPriority()
    {
        elementCache().prioritySelect().clearSelection();
        return this;
    }

    /**
     * Get the Created On date.
     * @return String value of the created on date, can include the time in addition to the date.
     */
    public String getCreatedOn()
    {
        return getOverviewField("Created On");
    }

    /**
     * Name of the user who created this job.
     * @return String of user name.
     */
    public String getCreatedBy()
    {
        return getOverviewField("Created By");
    }

    /**
     * Get the list of user names in the notify field.
     * @return String list of user names.
     */
    public List<String> getNotifyList()
    {
        return elementCache().notifySelect().getSelections();
    }

    /**
     * Add a user to the list of users to notify.
     * @param userName A valid user name.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel setNotifyList(String userName)
    {
        return setNotifyList(Arrays.asList(userName));
    }

    /**
     * This will add a list of users to the Notify list. It does not clear the field.
     * @param listOfUsers List of user names to add to the notify list. The values must be in the list.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel setNotifyList(List<String> listOfUsers)
    {
        listOfUsers.forEach(userName -> elementCache().notifySelect().select(userName));
        return this;
    }

    /**
     * Removes all names from the notify selection.
     * @return A reference to this page object.
     */
    public WorkflowOverviewJobPanel clearNotifyList()
    {
        elementCache().notifySelect().clearSelection();
        return this;
    }

    /**
     * Get the list of assays associated with this job.
     * This widget will be going away soon.
     * @return A list of strings with the assay names and sample counts.
     */
    public List<String> getAssayList()
    {
        List<String> assays = new ArrayList<>();

        List<WebElement> listElements = Locators.assayListItems.findElements(componentElement);
        if(listElements.size() > 0)
        {
            for(WebElement li : listElements)
            {
                assays.add(li.getText());
            }
        }

        return assays;
    }

    public boolean isUpdateButtonEnabled()
    {
        return elementCache().updateButton.isEnabled();
    }

    public WorkflowOverviewJobPanel clickUpdate()
    {
        elementCache().updateButton.click();
        return this;
    }

    private static class Locators
    {
        static final Locator spinner = Locator.css("span i.fa-spinner");

        static Locator overviewField(String fieldName)
        {
            return Locator.xpath("//span[contains(text(), '" + fieldName + "')]/parent::div/following-sibling::div/span");
        }

        static final Locator assayListItems = Locator.xpath("//div[contains(text(), 'Samples with assay data')]//li/a");

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {

        WebElement startDateInput = Locator.input("startDate").findWhenNeeded(componentElement);
        WebElement dueDateInput = Locator.input("dueDate").findWhenNeeded(componentElement);
        WebElement jobStatus = Locator.tagWithClassContaining("span", "job-overview--status-display").findWhenNeeded(componentElement);

        // Find the selects based on their order in the panel.
        ReactSelect assigneeSelect()
        {
            return new ReactSelect(reactSelectOnPanel(componentElement, 0), getDriver());
        }

        ReactSelect prioritySelect()
        {
            return new ReactSelect(reactSelectOnPanel(componentElement, 1), getDriver());
        }

        private WebElement reactSelectOnPanel(WebElement panel, int index)
        {
            List<WebElement> webElement = Locator
                    .tagWithClassContaining("div", "Select--single")
                    .findElements(panel);
            return webElement.get(index);
        }

        ReactSelect notifySelect()
        {
            WebElement webElement = Locator
                    .tagWithClassContaining("div", "Select--multi")
                    .findElement(componentElement);
            return new ReactSelect(webElement, getDriver());
        }

        WebElement updateButton = Locator.buttonContainingText("Update").findWhenNeeded(componentElement);
    }

}
