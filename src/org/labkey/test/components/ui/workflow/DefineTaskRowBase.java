package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class DefineTaskRowBase extends WebDriverComponent<DefineTaskRowBase.ElementCache>
{
    final WebElement componentElement;
    final WebDriver driver;

    public DefineTaskRowBase(WebElement element, WebDriver driver)
    {
        componentElement = element;
        this.driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return componentElement;
    }

    @Override
    public WebDriver getDriver()
    {
        return driver;
    }

    protected DefineTaskRowBase expandRow()
    {
        if(!isRowExpanded())
        {
            Locator.tagWithAttribute("div", "title", "Show all fields")
                    .findElement(this)
                    .click();
            waitFor(()  -> elementCache().description.isDisplayed(), "Row did not expand.", 1_000);
        }

        return this;
    }

    protected DefineTaskRowBase collapseRow()
    {
        if(isRowExpanded())
        {
            Locator.tagWithAttribute("div", "title", "Hide additional fields")
                    .findElement(this)
                    .click();
            waitFor(()  -> !elementCache().description.isDisplayed(), "Row did not collapse.", 1_000);
        }

        return this;
    }

    protected boolean isRowExpanded()
    {
        return Locator.tagWithClass("div", "domain-row-container").findElements(this).size() > 1;
    }

    protected DefineTaskRowBase setName(String name)
    {
        getWrapper().setFormElement(elementCache().name, name);
        return this;
    }

    public String getName()
    {
        return getWrapper().getFormElement(elementCache().name);
    }

    public boolean isNameEnabled()
    {
        return elementCache().name.isEnabled();
    }

    protected DefineTaskRowBase setDescription(String description)
    {
        expandRow();
        getWrapper().setFormElement(elementCache().description, description);
        return this;
    }

    public String getDescription()
    {
        expandRow();
        return getWrapper().getFormElement(elementCache().description);
    }

    public boolean isDescriptionEnabled()
    {
        return elementCache().description.isEnabled();
    }

    protected DefineTaskRowBase setAssaysToPerform(String assayName)
    {
        return setAssaysToPerform(Arrays.asList(assayName));
    }

    protected DefineTaskRowBase setAssaysToPerform(List<String> assayNames)
    {
        assayNames.forEach(n -> elementCache().assays().select(n));
        return this;
    }

    protected DefineTaskRowBase clearAssaysToPerform()
    {
        elementCache().assays().clearSelection();
        return this;
    }

    public List<String> getAssaysToPerform()
    {
        return elementCache().assays().getSelections();
    }

    public boolean isAssaysToPerformEnabled()
    {
        return elementCache().assays().isEnabled();
    }

    protected DefineTaskRowBase deleteTask()
    {
        // It feels like there should be a confirm dialog if this is an active job.
        // Tracked in Issue 39444: No warning message when deleting a task from a job when in the update page.
        elementCache().deleteTask.click();
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent<?>.ElementCache
    {
        final WebElement deleteTask = Locator.tagWithAttribute("span", "title", "Delete this task").findWhenNeeded(componentElement);

        final WebElement name = Locator.input("name").findWhenNeeded(componentElement);
        final WebElement description = Locator.textarea("description").findWhenNeeded(componentElement);

        ReactSelect assays()
        {
            WebElement webElement = Locator
                    .tagWithClassContaining("div", "Select--multi")
                    .findElement(componentElement);
            return new ReactSelect(webElement, getDriver());
        }

    }

}
