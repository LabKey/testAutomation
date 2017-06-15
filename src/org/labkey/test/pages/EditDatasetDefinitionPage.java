package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.components.html.RadioButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by RyanS on 5/17/2017.
 */
public class EditDatasetDefinitionPage extends LabKeyPage<EditDatasetDefinitionPage.ElementCache>
{
    public EditDatasetDefinitionPage(WebDriver driver)
    {
        super(driver);
        //WebDriverWrapper.waitFor(()-> elementCache().datasetFieldRow.isDisplayed(),WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.checkboxByName("demographicData"));
        sleep(10000);
    }

    public EditDatasetDefinitionPage saveExpectFail(String partialAlertTxt)
    {
        click(elementCache().saveButton);
        sleep(1000);
        assertAlertContains(partialAlertTxt);
        cancel();
        return this;
    }

    public DatasetPropertiesPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new DatasetPropertiesPage(getDriver());
    }

    public void cancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    public void setDatasetName(String name)
    {
        setFormElement(elementCache().nameTextField,name);
    }

    public String getDatasetName()
    {
        return elementCache().nameTextField.getText();
    }

    public void setDatasetLabel(String label)
    {
        setFormElement(elementCache().labelTextField, label);
    }

    public String getDatasetLabel()
    {
        return elementCache().labelTextField.getText();
    }

    public void setCategory(String category)
    {
        setFormElement(elementCache().categoryTextField,category);
    }

    public String getCategory()
    {
        return elementCache().categoryTextField.getText();
    }

    public void setTag(String tag)
    {
        setFormElement(elementCache().tagTextField,tag);
    }

    public String getTag()
    {
        return elementCache().tagTextField.getText();
    }

    public void setDescription(String description)
    {
        setFormElement(elementCache().descriptionTextField,description);
    }

    public String getDescription()
    {
        return elementCache().descriptionTextField.getText();
    }

    public void setAdditionalKeyColumnType(LookupAdditionalKeyColType type)
    {
        waitForElement(type.getTypeLocator(), WAIT_FOR_JAVASCRIPT);
        RadioButton typeBtn = new RadioButton(type.getTypeLocator().findElement(getDriver()));
        if(!typeBtn.isSelected()){typeBtn.check();}
    }

    public void setIsDemographicData(boolean isDemographic)
    {
        if(isDemographic)
        checkCheckbox(elementCache().demographicsCheckbox);
        else uncheckCheckbox(elementCache().demographicsCheckbox);
    }

    public boolean isDemographicsData()
    {
        return elementCache().demographicsCheckbox.isSelected();
    }

    public void setShowInOverview(boolean showInOverview)
    {
        if(showInOverview)
            checkCheckbox(elementCache().showInOverviewCheckbox);
        else uncheckCheckbox(elementCache().showInOverviewCheckbox);
    }

    public boolean isAdditionalKeyManagedEnabled()
    {
        return !isElementPresent(elementCache().additionalKeyMangedFieldDisabled);
    }

    public boolean isAdditionalKeyDataFieldEnabled()
    {
        return  !isElementPresent(elementCache().additionalKeyDataFieldDisabled);
    }

    public boolean isAdditionalFieldNoneEnabled()
    {
        return !isElementPresent(elementCache().additionalKeyDisabledNone);
    }

    public boolean isShownInOverview()
    {
        return elementCache().showInOverviewCheckbox.isSelected();
    }

    public void addDatasetField(String name, String label, FieldType fieldType)
    {
        click(elementCache().addFieldButton);
        //waitForElement(elementCache().newDatasetLabel);
        waitFor(()->{return elementCache().newDatasetLabel.isDisplayed();},WAIT_FOR_JAVASCRIPT);
        setFormElement(elementCache().newDatasetName,name);
        setFormElement(elementCache().newDatasetLabel, label);
        click(elementCache().newDatasetType);
        waitForElement(getChooseFieldTypeRadio(fieldType));
        getChooseFieldTypeRadioButton(fieldType).check();
        click(elementCache().applyChosenFieldTypeButton);
        //waitForElementToDisappear(elementCache().applyChosenFieldTypeButton);
        waitFor(()->{return elementCache().applyChosenFieldTypeButton.isDisplayed();},WAIT_FOR_JAVASCRIPT);
    }

    public void setAdditionalKeyColDataField(String field)
    {
        setFormElement(Locator.id("list_dataField"),field);
    }

    public void setAdditionalKeyColManagedField(String field)
    {
        setFormElement(Locator.id("list_managedField"),field);
    }

    enum FieldType{
        TEXT("Text (String)"), MULTILINE("Multi-Line Text"), BOOLEAN("Boolean"), INTEGER("Integer"), NUMBER_DBL("Number (Double)"), DATETIME("DateTime"), FLAG("Flag (String)"),
        FILE("File"), ATTACHMENT("Attachment"), USER("User"), SUBJECT_PARTICIPANT("Subject/Participant (String)"),LOOKUP("Lookup");

        private String type;

        public String getType(){
            return this.type;
        }
        FieldType(String type){
            this.type = type;
        }
    }

    public enum LookupAdditionalKeyColType
    {
        NONE(Locator.xpath("//input[@id='button_none']")), DATAFIELD(Locator.xpath("//input[@id='button_dataField']")), MANAGEDFIELD(Locator.xpath("//input[@id='button_managedField']"));

        private Locator typeLocator;

        public Locator getTypeLocator(){
            return this.typeLocator;
        }
        LookupAdditionalKeyColType(Locator type){
            this.typeLocator = type;
        }
    }

    private RadioButton getRadioBtn(Locator radioBtnLoc)
    {
        return new RadioButton(radioBtnLoc.findElement(getDriver()));
    }

    Locator getChooseFieldTypeRadio(FieldType fieldType)
    {
        return Locator.xpath("//div[./label[text()='"+fieldType.type+"']]/input");
    }

    RadioButton getChooseFieldTypeRadioButton(FieldType fieldType)
    {
        return new RadioButton(getChooseFieldTypeRadio(fieldType).findElement(getDriver()));
    }

    private boolean isButtonSelected(Locator radioBtnLoc)
    {
        return getRadioBtn(radioBtnLoc).isSelected();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        WebElement nameTextField = Locator.id("DatasetDesignerName").findWhenNeeded(this);
        WebElement labelTextField = Locator.name("dsLabel").findWhenNeeded(this);
        WebElement categoryTextField = Locator.name("dsCategory").findWhenNeeded(this);
        WebElement tagTextField = Locator.name("tag").findWhenNeeded(this);
        WebElement descriptionTextField = Locator.name("description").findWhenNeeded(this);
        WebElement cohortAssociation = Locator.xpath("//tr/td[@class='labkey-form-label'][descendant::div[text()='Cohort Association' and @class='gwt-Label']]/following-sibling::td/descendant::em").findWhenNeeded(this);
        WebElement additionalKeyNoneRadio = Locator.radioButtonById("button_none").findWhenNeeded(this);
        WebElement additionalKeyDataFieldRadio = Locator.radioButtonById("button-datafield").findWhenNeeded(this);
        WebElement additionalKeyManagedFieldRadio = Locator.radioButtonById("button_managedField").findWhenNeeded(this);
        WebElement additionalKeyDataFieldSelect = Locator.inputById("list_datafield").findWhenNeeded(this);
        WebElement additionalKeyManagedFieldSelect = Locator.inputById("list_managedField").findWhenNeeded(this);
        WebElement demographicsCheckbox = Locator.checkboxByName("demographicData").findWhenNeeded(this);
        WebElement showInOverviewCheckbox = Locator.checkboxByName("showByDefault").findWhenNeeded(this);
        WebElement datasetFieldRow = Locator.xpath("//tr[contains(@class,'editor-field-row')]").findWhenNeeded(this);
        WebElement datasetFieldNames = Locator.xpath("//input[contains(@name,'ff_name')]").findWhenNeeded(this);
        WebElement datasetLabels = Locator.xpath("//input[contains(@name,'ff_label')]").findWhenNeeded(this);
        WebElement datasetTypes = Locator.xpath("//input[contains(@name,'ff_type')").findWhenNeeded(this);
        WebElement addFieldButton = Locator.lkButton("Add Field").findWhenNeeded(this);
        WebElement importFieldsButton = Locator.lkButton("Import Fields").findWhenNeeded(this);
        WebElement exportFieldsButton = Locator.lkButton("Export Fields").findWhenNeeded(this);
        WebElement inferFieldsButton = Locator.lkButton("Infer Fields from File").findWhenNeeded(this);

        WebElement applyChosenFieldTypeButton = Locator.button("Apply").findWhenNeeded(this);
        WebElement cancelChosenFieldTypeButton = Locator.button("Cancel").findWhenNeeded(this);

        WebElement lookupFieldTypeFolder = Locator.name("lookupContainer").findWhenNeeded(this);
        WebElement lookupFieldSchema = Locator.name("schema").findWhenNeeded(this);
        WebElement lookupFieldTable = Locator.name("table").findWhenNeeded(this);

        Locator.XPathLocator newDatasetFieldRow = Locator.xpath("//tr[contains(@class,'editor-field-row')][descendant::span[contains(@class,'fa-plus-circle')]]");
        WebElement newDatasetName = newDatasetFieldRow.append("/descendant::input[contains(@name,'ff_name')]").findWhenNeeded(this);
        WebElement newDatasetLabel = newDatasetFieldRow.append("/descendant::input[contains(@name,'ff_label')]").findWhenNeeded(this);
        WebElement newDatasetType = newDatasetFieldRow.append("/descendant::input[contains(@name,'ff_type')]").findWhenNeeded(this);

        WebElement additionalKeyManagedKeyParentSpan = Locator.xpath("//span[descendant::input[@id='button_managedField']]").findWhenNeeded(this);
        WebElement additionalKeyDataFieldParentSpan = Locator.xpath("//span[descendant::input[@id='button_dataField']]").findWhenNeeded(this);
        WebElement additionalKeyNoneParentSpan = Locator.xpath("//span[descendant::input[@id='button_none']]").findWhenNeeded(this);
        
        Locator.XPathLocator additionalKeyDisabledNone = Locator.xpath("//span[contains(@class,'gwt-RadioButton-disabled')]/input[@id='button_none']");
        Locator.XPathLocator additionalKeyDataFieldDisabled = Locator.xpath("//span[contains(@class,'gwt-RadioButton-disabled')]/input[@id='button_none']");
        Locator.XPathLocator additionalKeyMangedFieldDisabled = Locator.xpath("//span[contains(@class,'gwt-RadioButton-disabled')]/input[@id='button_none']");



//        WebElement getDatasetFieldRowByIndex(int index)
//        {
//            return datasetFieldRow.findElements().get(index);
//        }

    }
}
