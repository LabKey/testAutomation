package org.labkey.test.components;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.util.SampleTypeHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class ManageSampleStatusesPanel extends WebDriverComponent<ManageSampleStatusesPanel.ElementCache>
{
    private final WebDriver _driver;
    final WebElement _componentElement;

    public static final String PANEL_TITLE = "Manage Sample Statuses";

    public static class SampleStatus  {
        public String label;
        public String description;
        public SampleTypeHelper.StatusType statusType;
    }

    public ManageSampleStatusesPanel(WebElement element, WebDriver driver)
    {
        _driver = driver;
        _componentElement = element;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    private void waitForEditReady()
    {
        WebDriverWrapper.waitFor(()->
                {
                    try
                    {
                        return elementCache().statusTypeSelect.isInteractive() &&
                                elementCache().labelField.getComponentElement().isEnabled() &&
                                elementCache().descriptionField.isEnabled();
                    }
                    catch (NoSuchElementException | StaleElementReferenceException exp)
                    {
                        return false;
                    }
                },
                "Edit part of the panel for a new status did not become active in time.", 1_000);
    }

    public SampleStatus selectStatus(String name)
    {
        return selectStatus(name, null);
    }

    public SampleStatus selectStatus(String name, SampleTypeHelper.StatusType statusType)
    {
        elementCache().statusItem(name, statusType).click();
        SampleStatus status = new SampleStatus();

        waitForEditReady();

        status.statusType = getStatusType();
        status.label = getLabel();
        status.description = getDescription();

        return status;
    }

    public String getLabel()
    {
        return elementCache().labelField.getValue();
    }

    public String getDescription()
    {
        return elementCache().descriptionField.getText();
    }

    public SampleTypeHelper.StatusType getStatusType()
    {
        return SampleTypeHelper.StatusType.valueOf(elementCache().statusTypeSelect.getValue());
    }

    public List<String> getStatusNames()
    {
        return elementCache().statusItems
                .findElements(this)
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public ManageSampleStatusesPanel clickAddStatus()
    {
        elementCache().addStatusButton.click();
        return this;
    }

    public ManageSampleStatusesPanel setLabel(String label)
    {
        elementCache().labelField.setValue(label);
        return this;
    }

    public ManageSampleStatusesPanel setDescription(String description)
    {
        elementCache().descriptionField.clear();
        if (description != null)
        {
           elementCache().descriptionField.sendKeys(description);
        }
        return this;
    }

    public ManageSampleStatusesPanel setStatusType(SampleTypeHelper.StatusType statusType)
    {
        elementCache().statusTypeSelect.select(statusType.toString());
        return this;
    }

    public ManageSampleStatusesPanel clickSave()
    {
        elementCache().saveButton.click();
        return this;
    }

    public String clickSaveExpectError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.waitForElement(getWrapper().shortWait()).getText();
    }


    public ManageSampleStatusesPanel addStatus(String label, String description, SampleTypeHelper.StatusType statusType)
    {
        clickAddStatus();

        waitForEditReady();

        setLabel(label).setDescription(description).setStatusType(statusType);

        elementCache().saveButton.click();

        WebDriverWrapper.waitFor(()->elementCache().deleteButton.isDisplayed(),
                "Delete button did not become visible after adding a status.", 1_000);

        return this;
    }

    public ManageSampleStatusesPanel deleteStatus(String label)
    {
        selectStatus(label);

        WebDriverWrapper.waitFor(()->elementCache().deleteButton.isDisplayed(),
                "Delete button is not visible.", 1_000);

        elementCache().deleteButton.click();
        return this;
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement statusItem(String name, SampleTypeHelper.StatusType statusType)
        {
            if (statusType == null || statusType.toString().equals(name))
                return Locator.tagWithClass("button", "list-group-item").containing(name).findElement(getComponentElement());
            else
                return Locator.tagWithClass("button", "list-group-item").containing(name +  statusType).findElement(getComponentElement());
        }

        final Locator statusItems = Locator.tagWithClass("button", "list-group-item");

        final WebElement addStatusButton = Locator.tagWithText("span", "Add New Status")
                .findWhenNeeded(getComponentElement());
        final Input labelField = Input.Input(Locator.inputByNameContaining("label"), getDriver()).findWhenNeeded(this);
        final WebElement descriptionField = Locator.textarea("description").findWhenNeeded(this);
        final ReactSelect statusTypeSelect = ReactSelect.finder(getDriver()).findWhenNeeded(this);
        final WebElement saveButton = Locator.tagWithText("button", "Save").findWhenNeeded(this);
        final WebElement deleteButton = Locator.tag("button").withChild(Locator.tagContainingText("span", "Delete")).refindWhenNeeded(this);
    }

    public static class ManageSampleStatusesPanelFinder extends WebDriverComponentFinder<ManageSampleStatusesPanel, ManageSampleStatusesPanelFinder>
    {
        private final Locator _locator;

        public ManageSampleStatusesPanelFinder(WebDriver driver)
        {
            super(driver);
            _locator = BootstrapLocators.panel(PANEL_TITLE);
        }

        @Override
        protected ManageSampleStatusesPanel construct(WebElement element, WebDriver driver)
        {
            return new ManageSampleStatusesPanel(element, driver);
        }

        @Override
        protected Locator locator() { return _locator; }
    }
}
