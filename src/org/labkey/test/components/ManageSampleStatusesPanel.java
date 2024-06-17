package org.labkey.test.components;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
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
    public static final String DEFAULT_AVAILABLE_STATUS_COLOR_HEX = "#F0F8ED";
    public static final String DEFAULT_CONSUMED_STATUS_COLOR_HEX = "#FCF8E3";
    public static final String DEFAULT_LOCKED_STATUS_COLOR_HEX = "#FDE6E6";

    public static final String PANEL_TITLE = "Manage Sample Statuses";

    public static class SampleStatus  {
        public String label;
        public String description;
        public SampleTypeHelper.StatusType statusType;
        public boolean isLocked;
        public String color;
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
                                elementCache().labelField().getComponentElement().isEnabled() &&
                                elementCache().descriptionField.isEnabled();
                    }
                    catch (NoSuchElementException | StaleElementReferenceException exp)
                    {
                        return false;
                    }
                },
                "Edit part of the panel for a new status did not become active in time.", 2_000);
    }

    public SampleStatus selectStatus(String name)
    {
        return selectStatus(name, null);
    }

    public SampleStatus selectStatus(String name, SampleTypeHelper.StatusType statusType)
    {
        elementCache().statusItem(name).click();
        SampleStatus status = new SampleStatus();

        status.isLocked = isLocked();

        if(!status.isLocked)
        {
            waitForEditReady();
        }
        else
        {

            WebDriverWrapper.waitFor(()->
                    {
                        try
                        {
                            return elementCache().selectedStatusItem().getText().trim().toLowerCase()
                                    .contains(elementCache().labelField().get().trim().toLowerCase());
                        }
                        catch (NoSuchElementException | StaleElementReferenceException exp)
                        {
                            return false;
                        }
                    },
                    String.format("Edit part of the panel for a locked status did not render in time. Value in label textbox '%s' did not contain '%s'.",
                            elementCache().labelField().get(), elementCache().selectedStatusItem().getText()), 1_000);

        }

        status.statusType = getStatusType();
        status.label = getLabel();
        status.description = getDescription();
        status.color = getColor();

        return status;
    }

    public String getLabel()
    {
        return elementCache().labelField().getValue();
    }

    public String getDescription()
    {
        return elementCache().descriptionField.getText();
    }

    public String getColor()
    {
        String style = elementCache().colorPickerChip.getAttribute("style");
        // extract a value such as rgb(235, 253, 249) (trimming off the trailing semicolon)
        return style.substring(style.indexOf("rgb("), style.length()-1);
    }

    public SampleTypeHelper.StatusType getStatusType()
    {
        return SampleTypeHelper.StatusType.valueOf(elementCache().statusTypeSelect.getValue());
    }

    public boolean isLocked()
    {
        return Locator.tagWithClass("span", "domain-field-lock-icon")
                .findWhenNeeded(elementCache().selectedStatusItem())
                .isDisplayed();
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

    public boolean isAddStatusPresent()
    {
        return elementCache().addStatusButton.isDisplayed();
    }

    public ManageSampleStatusesPanel setLabel(String label)
    {
        elementCache().labelField().setValue(label);
        return this;
    }

    public ManageSampleStatusesPanel setColor(String hexColor)
    {
        if (!StringUtils.isEmpty(hexColor))
        {
            elementCache().colorButton.click();
            ColorPickerInput colorInput = new ColorPickerInput.ColorPickerInputFinder(getDriver()).findWhenNeeded();
            colorInput.setHexValue(hexColor);
            elementCache().colorPickerContainer.click(); // need to click outside the color picker to close it
       }
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

    public boolean isSaveEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public String clickSaveExpectError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.waitForElement(getWrapper().shortWait()).getText();
    }


    public ManageSampleStatusesPanel addStatus(String label, String description, SampleTypeHelper.StatusType statusType, String hexColor)
    {
        clickAddStatus();

        waitForEditReady();

        setLabel(label)
                .setColor(hexColor)
                .setDescription(description)
                .setStatusType(statusType);

        elementCache().saveButton.click();

        // Don't know why but on MSSQL/Windows in TC this is taking a long time to complete.
        WebDriverWrapper.waitFor(()->elementCache().deleteButton.isDisplayed(),
                "Delete button did not become visible after adding a status.", 5_000);

        return this;
    }

    public ModalDialog deleteStatus(String label)
    {
        selectStatus(label);

        WebDriverWrapper.waitFor(()->elementCache().deleteButton.isDisplayed(),
                "Delete button is not visible.", 1_000);

        elementCache().deleteButton.click();

        return new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Permanently Delete Status?").find();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement statusItem(String name)
        {
            return Locator.tagWithClass("button", "list-group-item").containing(name).findElement(getComponentElement());
        }

        final Locator statusItems = Locator.tagWithClass("button", "list-group-item");

        final WebElement selectedStatusItem()
        {
            return Locator.tagWithClass("button", "list-group-item").withClass("active")
                    .refindWhenNeeded(this);
        }
        final WebElement addStatusButton = Locator.tagWithText("span", "Add New Status")
                .refindWhenNeeded(getComponentElement());
        final Input labelField()
        {
            return Input.Input(Locator.inputByNameContaining("label"), getDriver()).refindWhenNeeded(this);
        }
        final WebElement colorPickerContainer = Locator.tagWithClassContaining("div", "color-picker").refindWhenNeeded(this);
        final WebElement colorButton = Locator.tagWithClassContaining("button", "color-picker__button").refindWhenNeeded(this);
        final WebElement descriptionField = Locator.textarea("description").refindWhenNeeded(this);
        final WebElement colorPickerChip = Locator.tagWithClass("i", "color-picker__chip-small").refindWhenNeeded(this);
        final ReactSelect statusTypeSelect = ReactSelect.finder(getDriver()).refindWhenNeeded(this);
        final WebElement saveButton = Locator.tagWithText("button", "Save").refindWhenNeeded(this);
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
