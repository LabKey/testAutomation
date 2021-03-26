package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class WorkflowTaskProgressPanel extends WebDriverComponent<WorkflowTaskProgressPanel.ElementCache>
{

    private final WebDriver driver;
    private final WebElement componentElement;

    public WorkflowTaskProgressPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        componentElement = element;
    }

    public boolean isPanelLoaded()
    {

        // If there is a spinner the panel is not loaded.
        if(Locators.spinner.existsIn(componentElement))
            return false;

        // If there is "No Tasks" message the panel is loaded.
        if (getWrapper().isElementPresent(Locators.noTasksMsg))
            return true;

        try
        {
            // Finally check to see if the button is present.
            return elementCache().completeTaskButton.isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }

    }

    public boolean areThereTasks()
    {
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

    public String getPanelText()
    {
        return componentElement.getText();
    }

    public boolean isCompleteThisTaskButtonEnabled()
    {
        return elementCache().completeTaskButton.isEnabled();
    }

    public WorkflowTaskProgressPanel clickCompleteThisTask()
    {
        elementCache().completeTaskButton.click();
        return this;
    }

    public boolean isDeleteTaskEnabled()
    {
        // elementCache().completeAllMenuItem.isEnabled() Isn't working for this control. If this element is disabled
        // the class is set to "disabled".
        // Maybe isEnabled looks at styles and not class.
        return !elementCache().deleteTaskMenuItem.getAttribute("class").toLowerCase().contains("disabled");
    }

    public ModalDialog deleteTask()
    {
        elementCache().deleteDropdownButton.click();
        waitFor(() -> elementCache().deleteTaskMenuItem.isDisplayed(), 500);
        elementCache().deleteTaskMenuItem.click();
        return new ModalDialog.ModalDialogFinder(getDriver())
                .withTitle("Permanently delete task?").timeout(500).waitFor();
    }

    public boolean isReactivateTaskEnabled()
    {
        // elementCache().completeAllMenuItem.isEnabled() Isn't working for this control. If this element is disabled
        // the class is set to "disabled".
        // Maybe isEnabled looks at styles and not class.
        return !elementCache().reactivateTaskMenuItem.getAttribute("class").toLowerCase().contains("disabled");
    }

    public ModalDialog reactivateTask()
    {
        elementCache().deleteDropdownButton.click();
        waitFor(() -> elementCache().reactivateTaskMenuItem.isDisplayed(), 500);
        elementCache().reactivateTaskMenuItem.click();

        // The modal dialog title changes depending on the task being reactivated.
        // If you are deleting one tasks of many active tasks the title is "Reactivate This Task?"
        // If you are reactivating a task in a closed job the title is "Reactivate This Job?".
        // So look for a dialog with the text containing "Reactivating".
        return new ModalDialog.ModalDialogFinder(getDriver()).withBodyTextContaining("Reactivating").timeout(500).waitFor();
    }

    public String getTaskStatus()
    {
        return Locators.taskStatus.findElement(componentElement).getText();
    }

    public String getTaskOwner()
    {
        List<String> owners = elementCache().taskOwnerSelect.getSelections();
        return owners.get(0);
    }

    public WorkflowTaskProgressPanel setTaskOwner(String owner)
    {
        elementCache().taskOwnerSelect.select(owner);
        return this;
    }

    public boolean isReassignButtonEnabled()
    {
        return elementCache().reassignButton.isEnabled();
    }

    public WorkflowTaskProgressPanel clickReassignButton()
    {
        elementCache().reassignButton.click();
        return this;
    }

    public String getAssaysNeeded()
    {
        return Locators.assaysNeeded.findElement(componentElement).getText();
    }

    public String getTaskDescription()
    {
        return Locators.taskDescription.findElement(componentElement).getText();
    }

    public List<String> getPreviousComments()
    {
        List<WebElement> commentRows = Locators.previousComments.findElements(componentElement);
        return getWrapper().getTexts(commentRows);
    }

    public boolean isCommentFieldEnabled()
    {
        return elementCache().commentText.isEnabled();
    }

    public WorkflowTaskProgressPanel setCommentField(String comment)
    {
        getWrapper().setFormElement(elementCache().commentText, comment);
        return this;
    }

    public boolean isCommentButtonEnabled()
    {
        return elementCache().commentButton.isEnabled();
    }

    public WorkflowTaskProgressPanel clickCommentButton()
    {
        elementCache().commentButton.click();
        return this;
    }

    // TODO
    public WorkflowTaskProgressPanel addCommentWithSuccess(String comment)
    {
        int currentCommentCount = getPreviousComments().size();
        setCommentField(comment).clickCommentButton();

        // Comments appear to take a long time to show up.
        waitFor( () -> getPreviousComments().size() > currentCommentCount, 5_000);

        return this;
    }

    public String getCommentWarningMessage()
    {
        try
        {
            return Locators.commentWarning.findElement(componentElement).getText();
        }
        catch(NoSuchElementException nse)
        {
            return "";
        }
    }

    public List<String> getTasksList()
    {
        List<String> taskSteps = new ArrayList<>();

        for(WebElement taskRow : elementCache().tasksList())
        {
            taskSteps.add(taskRow.getText());
        }

        return taskSteps;
    }

    public String getActiveTaskName()
    {
        String taskName = "";

        for(WebElement taskRow : elementCache().tasksList())
        {
            if(taskRow.getAttribute("class").toLowerCase().contains("active"))
            {
                taskName = taskRow.getText();
            }
        }

        return taskName;
    }

    public List<String> getCompletedTasks()
    {
        List<String> taskSteps = new ArrayList<>();

        for(WebElement taskRow : elementCache().tasksList())
        {
            if(taskRow.findElement(Locator.tag("i")).getAttribute("class").toLowerCase().contains("fa-check-square-o"))
            {
                taskSteps.add(taskRow.getText());
            }
        }

        return taskSteps;
    }

    public WorkflowTaskProgressPanel selectTask(String taskName)
    {
        for(WebElement taskRow : elementCache().tasksList())
        {
            if(taskRow.getText().equals(taskName))
            {
                taskRow.click();
            }
        }

        return this;
    }

    public boolean isImportDataButtonVisible()
    {
        try
        {
            return getWrapper().isElementVisible(Locators.importDataButton);
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }
    }

    public void clickImportData()
    {
        Locators.importDataButton.findElement(componentElement).click();
    }

    private static class Locators
    {
        static final Locator spinner = Locator.css("span i.fa-spinner");
        static final Locator noTasksMsg = Locator.tagWithText("div", "No tasks available for current job...");
        static final Locator taskStatus = Locator.tagWithClassContaining("div", "task-status-text");
        static final Locator previousComments = Locator.tag("span").withChild(Locator.tagWithClassContaining("div", "bottom-spacing"));
        static final Locator assaysNeeded = Locator.tagWithText("span", "Assays Needed").parent("div").followingSibling("div").childTag("span");
        static final Locator taskDescription = Locator.tagWithText("span", "Task Description").parent("div").followingSibling("div").childTag("span");
        static final Locator commentWarning = Locator.tagWithClass("Span", "text-danger");
        static final Locator tasksListItem = Locator.tagWithClassContaining("li", "workflow-wizard-step");
        static final Locator importDataButton = Locator.linkWithText("Import Data");
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement completeTaskButton = Locator.buttonContainingText("Complete This Task").refindWhenNeeded(componentElement);
        ReactSelect taskOwnerSelect = ReactSelect.finder(driver).findWhenNeeded(componentElement);
        WebElement reassignButton = Locator.button("Reassign").findWhenNeeded(componentElement);
        WebElement commentText = Locator.tag("textarea").findWhenNeeded(componentElement);
        WebElement commentButton = Locator.button("Comment").findWhenNeeded(componentElement);
        WebElement deleteDropdownButton = Locator.tagWithClassContaining("div", "dropdown").childTag("button").withAttribute("id", "split-btn-group-dropdown-btn").refindWhenNeeded(componentElement);
        WebElement deleteTaskMenuItem = Locator.linkWithText("Delete Task").withAttribute("role", "menuitem").parent("li").refindWhenNeeded(componentElement);
        WebElement reactivateTaskMenuItem = Locator.linkWithText("Reactivate Task").withAttribute("role", "menuitem").parent("li").refindWhenNeeded(componentElement);

        // The task list updates when tasks status is changed. Cached elements become stale, so refind the collection when it is used.
        List<WebElement> tasksList()
        {
            return Locators.tasksListItem.findElements(componentElement);
        }
    }

}
