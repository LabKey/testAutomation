/*
 * Copyright (c) 2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.components.PropertiesEditor;
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
        waitForElement(Locator.checkboxByName("demographicData"));
        sleep(10000);
    }

    public EditDatasetDefinitionPage saveExpectFail(String partialAlertTxt)
    {
        elementCache().saveButton.click();
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

    public EditDatasetDefinitionPage setDatasetName(String name)
    {
        setFormElement(elementCache().nameTextField,name);
        return this;
    }

    public String getDatasetName()
    {
        return elementCache().nameTextField.getText();
    }

    public EditDatasetDefinitionPage setDatasetLabel(String label)
    {
        setFormElement(elementCache().labelTextField, label);
        return this;
    }

    public String getDatasetLabel()
    {
        return elementCache().labelTextField.getText();
    }

    public EditDatasetDefinitionPage setCategory(String category)
    {
        setFormElement(elementCache().categoryTextField,category);
        return this;
    }

    public String getCategory()
    {
        return elementCache().categoryTextField.getText();
    }

    public EditDatasetDefinitionPage setTag(String tag)
    {
        setFormElement(elementCache().tagTextField,tag);
        return this;
    }

    public String getTag()
    {
        return elementCache().tagTextField.getText();
    }

    public EditDatasetDefinitionPage setDescription(String description)
    {
        setFormElement(elementCache().descriptionTextField,description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionTextField.getText();
    }

    public EditDatasetDefinitionPage setAdditionalKeyColumnType(LookupAdditionalKeyColType type)
    {
        waitForElement(type.getTypeLocator(), WAIT_FOR_JAVASCRIPT);
        new RadioButton(type.getTypeLocator().findElement(getDriver())).check();
        return this;
    }

    public EditDatasetDefinitionPage setIsDemographicData(boolean isDemographic)
    {
        setCheckbox(elementCache().demographicsCheckbox, isDemographic);
        return this;
    }

    public boolean isDemographicsData()
    {
        return elementCache().demographicsCheckbox.isSelected();
    }

    public EditDatasetDefinitionPage setShowInOverview(boolean showInOverview)
    {
        setCheckbox(elementCache().showInOverviewCheckbox, showInOverview);
        return this;
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

    public PropertiesEditor getFieldsEditor()
    {
        return PropertiesEditor.PropertiesEditor(getDriver()).withTitleContaining("Dataset Fields").find();
    }

    public EditDatasetDefinitionPage setAdditionalKeyColDataField(String field)
    {
        setFormElement(Locator.id("list_dataField"),field);
        return this;
    }

    public EditDatasetDefinitionPage setAdditionalKeyColManagedField(String field)
    {
        setFormElement(Locator.id("list_managedField"),field);
        return this;
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
        WebElement importFieldsButton = Locator.lkButton("Import Fields").findWhenNeeded(this);
        WebElement exportFieldsButton = Locator.lkButton("Export Fields").findWhenNeeded(this);
        WebElement inferFieldsButton = Locator.lkButton("Infer Fields from File").findWhenNeeded(this);

        WebElement applyChosenFieldTypeButton = Locator.button("Apply").findWhenNeeded(this);
        WebElement cancelChosenFieldTypeButton = Locator.button("Cancel").findWhenNeeded(this);

        WebElement lookupFieldTypeFolder = Locator.name("lookupContainer").findWhenNeeded(this);
        WebElement lookupFieldSchema = Locator.name("schema").findWhenNeeded(this);
        WebElement lookupFieldTable = Locator.name("table").findWhenNeeded(this);

        WebElement additionalKeyManagedKeyParentSpan = Locator.xpath("//span[descendant::input[@id='button_managedField']]").findWhenNeeded(this);
        WebElement additionalKeyDataFieldParentSpan = Locator.xpath("//span[descendant::input[@id='button_dataField']]").findWhenNeeded(this);
        WebElement additionalKeyNoneParentSpan = Locator.xpath("//span[descendant::input[@id='button_none']]").findWhenNeeded(this);
        
        Locator.XPathLocator additionalKeyDisabledNone = Locator.xpath("//span[contains(@class,'gwt-RadioButton-disabled')]/input[@id='button_none']");
        Locator.XPathLocator additionalKeyDataFieldDisabled = Locator.xpath("//span[contains(@class,'gwt-RadioButton-disabled')]/input[@id='button_none']");
        Locator.XPathLocator additionalKeyMangedFieldDisabled = Locator.xpath("//span[contains(@class,'gwt-RadioButton-disabled')]/input[@id='button_none']");
    }
}
