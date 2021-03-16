package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.grids.GridRow;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.labkey.test.pages.samplemanagement.workflow.WorkflowJobTasksPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class WorkflowOverviewTaskPanel extends WebDriverComponent<WorkflowOverviewTaskPanel.ElementCache>
{
    private final WebDriver driver;
    private final WebElement componentElement;

    public WorkflowOverviewTaskPanel(WebElement element, WebDriver driver)
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

    /**
     * The panel will be considered loaded if there are no spinners showing.
     *
     * @return True if no spinners are showing, false otherwise.
     */
    public boolean isPanelLoaded()
    {
        try
        {
            return elementCache().tasksGrid.isLoaded();
        }
        catch (NoSuchElementException | StaleElementReferenceException retry)
        {
            return false;
        }
    }

    /**
     * Get the data from the task grid. Data is returned as a list of map object. For the map the key is the column name.
     * This java list does not include the header text, so the 0th item is the first task in the grid. Don't confuse
     * the task # with it's position in the list.
     * @return List of map objects containing the grid data. The key to the map is the column name.
     */
    public List<Map<String, String>> getTasks()
    {
        return elementCache().tasksGrid.getRowMaps();
    }

    /**
     * Click on a specific task name. If there are multiple tasks with the same name the first one found will be clicked.
     * @param taskName The name of the task.
     * @return A reference to this panel.
     */
    public WorkflowJobTasksPage clickTaskName(String taskName)
    {
        Locator.linkWithText(taskName).findElement(componentElement).click();
        return new WorkflowJobTasksPage(getWrapper());
    }

    /**
     * Click on a task in the gird based on the task number.
     * @param taskNumber The value in the 'Task #' column.
     * @return A reference to this panel.
     */
    public WorkflowJobTasksPage clickTaskOnRow(int taskNumber)
    {
        // Allow for 0 based index in the collection and 1 based number for task number.
        int rowNumber = taskNumber - 1;
        List<GridRow> rows = elementCache().tasksGrid.getRows();
        rows.get(rowNumber).findElements(Locator.tag("a")).get(0).click();
        return new WorkflowJobTasksPage(getWrapper());
    }

    /**
     * Check to see if the 'Complete Current Task' button is enabled. This should be disabled if all tasks have
     * been completed.
     * @return True if button is enabled, false otherwise.
     */
    public boolean isCompleteCurrentTaskEnabled()
    {
        return elementCache().completeTaskButton.isEnabled();
    }

    /**
     * Click the 'Complete Current Task' button.
     * @return A reference to this panel.
     */
    public WorkflowOverviewTaskPanel clickCompleteCurrentTask()
    {
        elementCache().completeTaskButton.click();
        clearElementCache();
        return this;
    }

    /**
     * Check to see if the drop down menu item 'Complete All Tasks' is enabled. Should be false of there are no active tasks.
     * @return True if enabled, false otherwise.
     */
    public boolean isCompleteAllTasksEnabled()
    {
        // elementCache().completeAllMenuItem.isEnabled() Isn't working for this control. If this element is disabled
        // the class is set to "disabled".
        // Maybe isEnabled looks at styles and not class.
        return !elementCache().completeAllMenuItem.getAttribute("class").toLowerCase().contains("disabled");
    }

    /**
     * Click the 'Complete All Tasks' menu item. You should check for a success or failure banner after clicking.
     * @return A reference to this panel.
     */
    public WorkflowOverviewTaskPanel clickCompleteAllTasks()
    {
        elementCache().stateDropdownButton.click();
        waitFor(() -> elementCache().completeAllMenuItem.isDisplayed(), 500);
        elementCache().completeAllMenuItem.click();
        return this;
    }

    /**
     * Check to see if the 'Reactivate Job' menu item is enabled. Should only be enabled if all tasks have been commpleted.
     * @return True if enabled, false otherwise.
     */
    public boolean isReactivateJobEnabled()
    {
        // elementCache().completeAllMenuItem.isEnabled() Isn't working for this control. If this element is disabled
        // the class is set to "disabled".
        // Maybe isEnabled looks at styles and not class?
        return !elementCache().reactivateMenuItem.getAttribute("class").toLowerCase().contains("disabled");
    }

    /**
     * Click the 'Reactive Job' menu item. Returns a dialog asking if you are sure. If yes this should reactivate the
     * last task in the list.
     * @return A dialog asking it you are sure.
     */
    public ModalDialog clickReactivateJob()
    {
        elementCache().stateDropdownButton.click();
        waitFor(() -> elementCache().reactivateMenuItem.isDisplayed(), 500);
        elementCache().reactivateMenuItem.click();
        return new ModalDialog.ModalDialogFinder(getDriver())
                .withTitle("Reactivate This Job?").timeout(500).waitFor();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        ResponsiveGrid tasksGrid = new ResponsiveGrid.ResponsiveGridFinder(getDriver()).findWhenNeeded();

        // The references to the buttons go stale if the state of the job has changed.
        WebElement completeTaskButton = Locator.buttonContainingText("Complete Current Task").refindWhenNeeded(componentElement);
        WebElement stateDropdownButton = Locator.tagWithClassContaining("div", "dropdown").childTag("button").withAttribute("id", "split-btn-group-dropdown-btn").refindWhenNeeded(componentElement);

        WebElement completeAllMenuItem = Locator.linkWithText("Complete All Tasks").withAttribute("role", "menuitem").parent("li").findWhenNeeded(componentElement);
        WebElement reactivateMenuItem = Locator.linkWithText("Reactivate Job").withAttribute("role", "menuitem").parent("li").findWhenNeeded(componentElement);

    }

}
