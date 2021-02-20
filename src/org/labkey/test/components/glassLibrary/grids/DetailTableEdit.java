package org.labkey.test.components.glassLibrary.grids;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

/**
 * automates /QueryModel/DetailPanel.tsx in its editable mode
 */
public class DetailTableEdit extends WebDriverComponent<DetailTableEdit.ElementCache>
{
    private final WebElement _formElement;
    private final WebDriver _driver;
    private String _title;

    protected DetailTableEdit(WebElement formElement, WebDriver driver)
    {
        _formElement = formElement;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _formElement;
    }

    public String getTitle()
    {
        if (_title == null)
            _title = elementCache().header.getText();
        return _title;
    }

    /**
     * Check to see if a field is editable. Could be state dependent, that is it returns false if the field is
     * loading but if checked later could return true.
     *
     * @param fieldCaption The caption/label of the field to check.
     * @return True if it is false otherwise.
     **/
    public boolean isFieldEditable(String fieldCaption)
    {
        // TODO Could put a check here to see if a field is loading then return false, or wait.
        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption).findElement(elementCache().editPanel);
        return isEditableField(fieldValueElement);
    }

    private boolean isEditableField(WebElement element)
    {
        // If the div does not have the class value of 'field__un-editable' then it is an editable field.
        return Locator.css("div:not(.field__un-editable)").findOptionalElement(element).isPresent();
    }

    /**
     * Get the value of a read only field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value in the field.
     **/
    public String getReadOnlyField(String fieldCaption)
    {
        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption).findElement(elementCache().editPanel);
        return fieldValueElement.findElement(By.xpath("./div/*")).getText();
    }

    /**
     * Get the value of a text field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value in the field.
     **/
    public String getTextField(String fieldCaption)
    {
        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption).findElement(elementCache().editPanel);
        WebElement textElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
        if(textElement.getTagName().equalsIgnoreCase("textarea"))
            return textElement.getText();
        else
            return textElement.getAttribute("value");
    }

    /**
     * Set a text field.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param value The value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setTextField(String fieldCaption, String value)
    {
        if(isFieldEditable(fieldCaption))
        {
            WebElement fieldValueElement = elementCache().fieldValue(fieldCaption).findElement(getComponentElement());

            WebElement editableElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
            String elementType = editableElement.getTagName().toLowerCase().trim();

            switch(elementType)
            {
                case "textarea":
                case "input":
                    editableElement.clear();
                    editableElement.sendKeys(value);
                    break;
                default:
                    throw new NoSuchElementException("This doesn't look like an 'input' or 'textarea' element, are you sure you are calling the correct method?");
            }
        }
        else
        {
            throw new IllegalArgumentException("Field with caption '" + fieldCaption + "' is read-only. This field can not be set.");
        }

        return this;
    }

    public DetailTableEdit setInputByFieldName(String fieldName, String value)
    {
        Locator inputloc = Locator.tagWithClass("input", "form-control")
            .withAttribute("name",  fieldName);
        Input input = Input.Input(inputloc,
                getDriver()).waitFor();
        input.set(value);
        return this;
    }

    /**
     * Get the value of a boolean field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value of the field.
     **/
    public boolean getBooleanField(String fieldCaption)
    {
        // The text used in the field caption and the value of the name attribute in the checkbox don't always have the same case.
        WebElement editableElement = Locator.tagWithAttributeIgnoreCase("input", "name", fieldCaption).findElement(getComponentElement());
        String elementType = editableElement.getAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be get true/false value.", fieldCaption), "checkbox", elementType);

        return new Checkbox(editableElement).isChecked();
    }

    /**
     * Set a boolean field (a checkbox).
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param value True will check it, false will uncheck it.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setBooleanField(String fieldCaption, boolean value)
    {

        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption).findElement(getComponentElement());
        Assert.assertTrue(String.format("Field '%s' is not editable and cannot be set.", fieldCaption), isEditableField(fieldValueElement));

        // The text used in the field caption and the value of the name attribute in the checkbox don't always have the same case.
        WebElement editableElement = fieldValueElement.findElement(Locator.tagWithAttributeIgnoreCase("input", "name", fieldCaption));
        String elementType = editableElement.getAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be set to true/false.", fieldCaption), "checkbox", elementType);

        Checkbox checkbox = new Checkbox(editableElement);

        checkbox.set(value);

        return this;
    }

    /**
     * Get the value of an int field. You could also call getTextField
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value of the field as an int.
     **/
    public int getIntField(String fieldCaption)
    {
        return Integer.getInteger(getTextField(fieldCaption));
    }

    /**
     * Set an int field.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param value The int value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setIntField(String fieldCaption, int value)
    {
        return setTextField(fieldCaption, Integer.toString(value));
    }

    /**
     * Get the value of an select field. This could be one or many values, because of this the result is returned as a list.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return A (String) list of the values selected.
     **/
    public List<String> getSelectValue(String fieldCaption)
    {
        return ReactSelect.finder(_driver).followingLabelWithSpan(fieldCaption).find().getSelections();
    }

    /**
     * clears the selections from the specified reactSelect
     * @param fieldCaption The label text for the select box
     * @return A reference to the current object
     */
    public DetailTableEdit clearSelectionValues(String fieldCaption)
    {
        ReactSelect reactSelect =  ReactSelect.finder(_driver).followingLabelWithSpan(fieldCaption).find();
        reactSelect.clearSelection();
        return this;
    }

    /**
     * Select a single value from a select list.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param selectValue The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldCaption, String selectValue)
    {
        List<String> selection = Arrays.asList(selectValue);
        return setSelectValue(fieldCaption, selection);
    }

    /**
     * Select multiple values from a select list.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param selectValues The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldCaption, List<String> selectValues)
    {
        FilteringReactSelect reactSelect =  FilteringReactSelect.finder(_driver).followingLabelWithSpan(fieldCaption).find();
        selectValues.forEach(s -> {reactSelect.typeAheadSelect(s);});
        return this;
    }

    /**
     * Clear a given select field.
     *
     * @param fieldCaption The caption/label of the field to clear.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit clearSelectValue(String fieldCaption)
    {
        ReactSelect.finder(_driver).followingLabelWithSpan(fieldCaption).find().clearSelection();
        return this;
    }

    private String getSourceTitle()
    {
        return getTitle().replace("Editing ", "");
    }

    /**
     * A validation message happens if a value of a particular field is out of bounds or incorrect in some other way.
     *
     * @return The text of the validation message or an empty string if there is none.
     */
    public String getValidationMessage()
    {
        if(elementCache().validationMsg.existsIn(this))
            return elementCache().validationMsg.findElement(getDriver()).getText();
        else
            return "";
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public DetailDataPanel clickSave()
    {
        String title = getSourceTitle();
        elementCache().saveButton.click();
        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.findElement(getDriver()).getText();
    }

    public DetailDataPanel clickCancel()
    {
        String title = getSourceTitle();
        elementCache().cancelButton.click();
        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickCancelExpectingError()
    {
        elementCache().cancelButton.click();
        return BootstrapLocators.errorBanner.findElement(getDriver()).getText();
    }


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement header = Locator.tagWithClass("div", "detail__edit--heading")
                .findWhenNeeded(this);
        public WebElement editPanel = Locator.tagWithClass("div", "detail__editing")
                .findWhenNeeded(this);

        public Locator fieldValue(String caption)
        {
            return Locator.tagWithAttribute("td", "data-caption", caption);
        }
        public Locator validationMsg = Locator.tagWithClass("span", "validation-message");

        public WebElement saveButton = Locator.tagWithAttribute("button", "type", "submit")
                .findWhenNeeded(this);
        public WebElement cancelButton = Locator.tagWithAttribute("button", "type", "button")
                .findWhenNeeded(this);
    }

    public static class DetailTableEditFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTableEdit, DetailTableEditFinder>
    {
        private Locator.XPathLocator _baseLocator = Locator.tag("form")
                .withDescendant(Locator.tagWithClass("table", "detail-component--table__fixed"));
        private Locator _locator;

        public DetailTableEditFinder(WebDriver driver)
        {
            super(driver);
            _locator= _baseLocator;
        }

        public DetailTableEditFinder withTitle(String title)
        {
            _locator = _baseLocator.withDescendant(Locator.tagWithClass("div", "detail__edit--heading")
                    .withText(title));
            return this;
        }

        @Override
        protected DetailTableEdit construct(WebElement el, WebDriver driver)
        {
            return new DetailTableEdit(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
