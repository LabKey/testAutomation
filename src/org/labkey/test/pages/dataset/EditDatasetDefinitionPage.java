package org.labkey.test.pages.dataset;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.dataset.AdvancedDatasetSettingsDialog;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.DatasetPropertiesPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class EditDatasetDefinitionPage extends DomainDesigner<EditDatasetDefinitionPage.ElementCache>
{
    public EditDatasetDefinitionPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditDatasetDefinitionPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static EditDatasetDefinitionPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study", containerPath, "defineDatasetType"));
        return new EditDatasetDefinitionPage(webDriverWrapper.getDriver());
    }

    public void waitForPage()
    {
        waitFor(()-> getFieldsPanel().getComponentElement().isDisplayed(),
                "The page did not render in time", WAIT_FOR_PAGE);
    }

    public EditDatasetDefinitionPage shareDemographics(String option)
    {
        expandPropertiesPanel();
        openAdvancedDatasetSettings()
            .shareDemographics(option)
            .clickApply();
        return this;
    }

    public AdvancedDatasetSettingsDialog openAdvancedDatasetSettings()
    {
        expandPropertiesPanel();
        elementCache().advancedSettingsButton.click();
        return new AdvancedDatasetSettingsDialog(this);
    }

    public EditDatasetDefinitionPage setName(String name)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return this;
    }

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    public EditDatasetDefinitionPage setDescription(String description)
    {
        expandPropertiesPanel();
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }

    // properties panel interactions
    // get/set category
    public EditDatasetDefinitionPage setCategory(String category)
    {
        expandPropertiesPanel();
        elementCache().categorySelect.filterSelect(category);
        return this;
    }

    public String getCategory()
    {
        return elementCache().categorySelect.getValue();
    }

    // get/set label
    public EditDatasetDefinitionPage setDatasetLabel(String label)
    {
        expandPropertiesPanel();
        elementCache().labelInput.set(label);
        return this;
    }

    public String getDatasetLabel()
    {
        return elementCache().labelInput.get();
    }

    // get/select radio button for: participants only(demographic data)/participants and visit/participants, visit, and additional key field
    public EditDatasetDefinitionPage setIsDemographicData(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().participantsOnlyRadioBtn.set(checked);
        return this;
    }

    public boolean isDemographicsData()
    {
        return  elementCache().participantsOnlyRadioBtn.get();
    }

    public String getAdditionalKeyColDataField()
    {
        if (!elementCache().keyFieldSelect.isEnabled())
            return null;
        else
            return elementCache().keyFieldSelect.getValue();
    }

    public EditDatasetDefinitionPage setAdditionalKeyColDataField(String field)
    {
        setAdditionalKeyColumnType(LookupAdditionalKeyColType.MANAGEDFIELD);
        elementCache().keyFieldSelect.select(field);
        return this;
    }

    public EditDatasetDefinitionPage setAdditionalKeyColManagedField(String field)
    {
        setAdditionalKeyColumnType(LookupAdditionalKeyColType.MANAGEDFIELD);
        elementCache().keyFieldSelect.select(field);
        elementCache().keyPropertyManagedBox.check();
        return this;
    }

    public EditDatasetDefinitionPage setAdditionalKeyColumnType(LookupAdditionalKeyColType type)
    {
        expandPropertiesPanel();
        new RadioButton(elementCache().dataRowRadioBtn(type.getIndex()).findElement(getDriver())).check();
        return this;
    }

    public boolean isAdditionalKeyManagedEnabled()
    {
        expandPropertiesPanel();
        return elementCache().additionalKeyFieldRadioBtn.isChecked() &&
                elementCache().keyFieldSelect.hasValue() &&
                elementCache().keyPropertyManagedBox.isChecked();
    }

    public boolean isAdditionalKeyDataFieldEnabled()
    {
        return  elementCache().keyFieldSelect.isEnabled();
    }

    public boolean isAdditionalFieldNoneEnabled()
    {
        return elementCache().additionalKeyFieldRadioBtn.isChecked() &&
                !elementCache().keyFieldSelect.hasValue();
    }

    // get/select additional key field
    // get/set 'let server manage fields to make entries unique' checkbox

    public EditDatasetDefinitionPage setShowInOverview(boolean checked)
    {
        expandPropertiesPanel();
        return openAdvancedDatasetSettings()
                .setShowInOverview(checked)
                .clickApply();
    }

    public EditDatasetDefinitionPage saveExpectFail(String expectedError)
    {
        List<String>  errors = clickSaveExpectingErrors();
        assertThat("Errors on the page should include this expected one", errors, hasItem(expectedError));
        return this;
    }

    public DatasetPropertiesPage clickSave()
    {
        getWrapper().doAndWaitForPageToLoad(()-> elementCache().saveButton.click());
        return new DatasetPropertiesPage(getDriver());
    }

    public enum LookupAdditionalKeyColType
    {
        NONE("Participants only (demographic data)", 0),
        DATAFIELD("Participants and visits", 1),
        MANAGEDFIELD("Participants, visits, and additional key field", 2);

        private String _label;
        private Integer _index;

        public String getLabel(){
            return this._label;
        }
        public Integer getIndex(){
            return this._index;
        }
        LookupAdditionalKeyColType(String label, Integer index){
            this._label = label;
            this._index = index;
        }
    }

    public enum ShareDemographicsBy
    {
        NONE("No"),
        PTID("Share by ParticipantId");

        private String _option;
        public String getOption()
        {
            return _option;
        }
        ShareDemographicsBy(String option)
        {
            _option = option;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainDesigner.ElementCache
    {
        public WebElement advancedSettingsButton = Locator.tagWithText("button", "Advanced Settings")
                .findWhenNeeded(propertiesPanel);

        protected Input nameInput = new Input(Locator.id("name").findWhenNeeded(propertiesPanel),
                getDriver());
        protected Input descriptionInput = new Input(Locator.id("description").findWhenNeeded(propertiesPanel),
                getDriver());

        private WebElement categoryRow = Locator.tagWithClass("div", "margin-top")
                .containingIgnoreCase("Category").findWhenNeeded(propertiesPanel);
        protected FilteringReactSelect categorySelect = FilteringReactSelect.finder(getDriver()).findWhenNeeded(categoryRow);
        protected Input labelInput = new Input(Locator.inputById("label").findWhenNeeded(categoryRow), getDriver());

        private WebElement rowUniquenessContainer = Locator.tagWithClass("div", "dataset_data_row_uniqueness_container")
                .findWhenNeeded(propertiesPanel);
        protected Locator dataRowRadioBtn(Integer index)
        {
            return Locator.tag("label").withAttribute("title").child(Locator.input("dataRowSetting")
                    .withAttribute("value", index.toString()));
        }
        protected RadioButton participantsOnlyRadioBtn = new RadioButton(dataRowRadioBtn(0) // demographic data
            .findWhenNeeded(rowUniquenessContainer));
        protected RadioButton participantsAndVisitsRadioBtn = new RadioButton(dataRowRadioBtn(1) //Participants and visits"
                .findWhenNeeded(rowUniquenessContainer));
        protected RadioButton additionalKeyFieldRadioBtn = new RadioButton(dataRowRadioBtn(2) // additional key field"
                .findWhenNeeded(rowUniquenessContainer));

        private WebElement keyFieldRow = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "dataset_data_row_element_show"),
                Locator.tagWithClass("div", "dataset_data_row_element_hide"))
                .containing("Additional Key Field").refindWhenNeeded(propertiesPanel);
        protected ReactSelect keyFieldSelect = ReactSelect.finder(getDriver()).findWhenNeeded(keyFieldRow);

        Checkbox keyPropertyManagedBox = new Checkbox(Locator.inputById("keyPropertyManaged")
                .findWhenNeeded(propertiesPanel));
    }
}
