package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.components.study.AdvancedDatasetSettingsDialog;
import org.labkey.test.pages.DatasetPropertiesPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class DatasetDesignerPage extends DomainDesigner<DatasetDesignerPage.ElementCache>
{
    public DatasetDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public static DatasetDesignerPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static DatasetDesignerPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study", containerPath, "defineDatasetType"));
        return new DatasetDesignerPage(webDriverWrapper.getDriver());
    }

    public void waitForPage()
    {
        // Wait for a panel body to be displayed.
        WebElement panelBody = Locator.tagWithClass("div", "panel-body").findWhenNeeded(getDriver());
        waitFor(panelBody::isDisplayed,
                "The page did not render in time", WAIT_FOR_PAGE);
    }

    public DatasetDesignerPage shareDemographics(String option)
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

    public DatasetDesignerPage setName(String name)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return this;
    }

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    public DatasetDesignerPage setDescription(String description)
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
    public DatasetDesignerPage setCategory(String category)
    {
        expandPropertiesPanel();
        elementCache().categorySelect.createValue(category);
        return this;
    }

    public String getCategory()
    {
        return elementCache().categorySelect.getValue();
    }

    // get/set label
    public DatasetDesignerPage setDatasetLabel(String label)
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
    public DatasetDesignerPage setIsDemographicData(boolean checked)
    {
        expandPropertiesPanel();
        if (checked)
            setDataRowUniquenessType(DataRowUniquenessType.PTID_ONLY);
        else
            setDataRowUniquenessType(DataRowUniquenessType.PTID_TIMEPOINT);
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

    public DatasetDesignerPage setAdditionalKeyColDataField(String field)
    {
        setDataRowUniquenessType(DataRowUniquenessType.PTID_TIMEPOINT_ADDITIONAL_KEY);
        elementCache().keyFieldSelect.select(field);
        return this;
    }

    public DatasetDesignerPage setAdditionalKeyColManagedField(String field)
    {
        setDataRowUniquenessType(DataRowUniquenessType.PTID_TIMEPOINT_ADDITIONAL_KEY);
        elementCache().keyFieldSelect.select(field);
        elementCache().keyPropertyManagedBox.check();
        return this;
    }

    public DatasetDesignerPage setDataRowUniquenessType(DataRowUniquenessType type)
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


    public DatasetDesignerPage setShowInOverview(boolean checked)
    {
        expandPropertiesPanel();
        return openAdvancedDatasetSettings()
                .setShowInOverview(checked)
                .clickApply();
    }

    public DatasetDesignerPage saveExpectFail(String expectedError)
    {
        List<String>  errors = clickSaveExpectingErrors();
        assertThat("Errors on the page should include this expected one", errors, hasItem(expectedError));
        return this;
    }

    @Override
    public DatasetPropertiesPage clickSave()
    {
        getWrapper().doAndWaitForPageToLoad(()-> elementCache().saveButton.click());
        return new DatasetPropertiesPage(getDriver());
    }

    public enum DataRowUniquenessType
    {
        PTID_ONLY("Participants only (demographic data)", 0),
        PTID_TIMEPOINT("Participants and visits", 1),
        PTID_TIMEPOINT_ADDITIONAL_KEY("Participants, visits, and additional key field", 2);

        private final String _label;
        private final Integer _index;

        public String getLabel(){
            return this._label;
        }
        public Integer getIndex(){
            return this._index;
        }
        DataRowUniquenessType(String label, Integer index){
            this._label = label;
            this._index = index;
        }
    }

    public enum ShareDemographicsBy
    {
        NONE("No"),
        PTID("Share by ParticipantId");

        private final String _option;
        public String getOption()
        {
            return _option;
        }
        ShareDemographicsBy(String option)
        {
            _option = option;
        }
    }

    // note: auto-import slider is only shown when you've inferred fields from file
    public DatasetDesignerPage setAutoImport(boolean autoImport)
    {
        elementCache().autoImportToggle().set(autoImport);
        return this;
    }

    public boolean getAutoImport()
    {
        return elementCache().autoImportToggle().get();
    }

    public DatasetDesignerPage setPreviewMappedColumn(String columnLabel, String value)
    {
        elementCache().columnMapSelect(columnLabel).filterSelect(value);
        return this;
    }

    public String getPreviewMappedColumnValue(String columnLabel)
    {
        return elementCache().columnMapSelect(columnLabel).getValue();
    }
    // find ptid column select
    // find visit column select


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainDesigner<?>.ElementCache
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
        protected Input labelInput = new Input(Locator.inputById("label").findWhenNeeded(propertiesPanel), getDriver());

        private WebElement rowUniquenessContainer = Locator.tagWithClass("div", "dataset_data_row_uniqueness_container")
                .findWhenNeeded(propertiesPanel);
        protected Locator dataRowRadioBtn(Integer index)
        {
            return Locator.tag("label").withAttribute("title").child(Locator.input("dataRowSetting")
                    .withAttribute("value", index.toString()));
        }
        protected final RadioButton participantsOnlyRadioBtn = new RadioButton(dataRowRadioBtn(0) // demographic data
            .findWhenNeeded(rowUniquenessContainer));
        protected final RadioButton participantsAndVisitsRadioBtn = new RadioButton(dataRowRadioBtn(1) //Participants and visits"
                .findWhenNeeded(rowUniquenessContainer));
        protected final RadioButton additionalKeyFieldRadioBtn = new RadioButton(dataRowRadioBtn(2) // additional key field"
                .findWhenNeeded(rowUniquenessContainer));

        private final WebElement keyFieldRow = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "dataset_data_row_element_show"),
                Locator.tagWithClass("div", "dataset_data_row_element_hide"))
                .containing("Additional Key Field").refindWhenNeeded(propertiesPanel);
        protected final ReactSelect keyFieldSelect = ReactSelect.finder(getDriver()).findWhenNeeded(keyFieldRow);

        protected final Checkbox keyPropertyManagedBox = new Checkbox(Locator.inputById("keyPropertyManaged")
                .findWhenNeeded(propertiesPanel));

        // this is only shown when inferring fields from a file
        protected ToggleButton autoImportToggle()
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).withState("Import Data").waitFor(fieldsPanel);
        }

        protected FilteringReactSelect columnMapSelect(String labelText)
        {   // find the row with the specified label span, then get the select in it
            WebElement container = Locator.tag("div").withChild(Locator.tag("div")
                    .withChild(Locator.tagWithClass("span", "domain-no-wrap").withText(labelText)))
                    .waitForElement(fieldsPanel, 2000);
            return FilteringReactSelect.finder(getDriver()).find(container);
        }
    }
}
