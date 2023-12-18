/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.experiment.CreateDataClassPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataClassHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class DataClassTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "DataClassTestProject";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject()
    {
        DataClassTest init = (DataClassTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Data Classes");
        portalHelper.exitAdminMode();
    }

    // TODO more test cases need to be added for the create/update data class via UI cases

    @Test
    public void testNameAlreadyExists()
    {
        goToProjectHome();

        log("Create an initial data class");
        final String name = "Name Already Exists Test";
        CreateDataClassPage createPage = goToCreateNewDataClass();
        createPage.setName(name).clickSave();

        log("Try creating data class with the same name");
        createPage = goToCreateNewDataClass();
        createPage.setName(name);
        assertEquals("Data class name conflict error", Arrays.asList(
                "DataClass 'Name Already Exists Test' already exists."),
                createPage.clickSaveExpectingErrors());
        createPage.clickCancel();

        log("Try creating data class with same name but different casing");
        createPage = goToCreateNewDataClass();
        createPage.setName(name.toLowerCase());
        assertEquals("Data class name conflict error", Arrays.asList(
                "DataClass 'Name Already Exists Test' already exists."),
                createPage.clickSaveExpectingErrors());
        createPage.clickCancel();

        final String anotherName = "Another Name";
        log("Try creating data class with a different name");
        goToCreateNewDataClass().setName(anotherName).clickSave();

        final String renamed = "Updated Name";
        log("Renaming a data class to a new name");
        CreateDataClassPage updatePage = goToDataClass(anotherName);
        updatePage.setName(renamed).clickSave();
        goToProjectHome();

        log("Try renaming a data class to an existing name");
        updatePage = goToDataClass(renamed);
        updatePage = updatePage.setName(name);

        assertEquals("Data class name conflict error", Arrays.asList(
                        "DataClass 'Name Already Exists Test' already exists.",
                        "Please correct errors in Name Already Exists Test before saving."),
                updatePage.clickSaveExpectingErrors());
        updatePage.clickCancel();
    }

    private CreateDataClassPage goToDataClass(String dataClassName)
    {
        clickAndWait(Locator.linkWithText(dataClassName));
        assertElementPresent(Locator.tagWithText("h3", dataClassName));
        clickButton("Edit Data Class");
        return new CreateDataClassPage(getDriver());
    }

    @Test
    public void testReservedFieldNames()
    {
        goToProjectHome();

        String name = "Reserved Field Names Test";
        CreateDataClassPage createPage = goToCreateNewDataClass();
        createPage.setName(name);

        log("Verify error message for reserved field names");
        DomainFormPanel domainFormPanel = createPage.getDomainEditor();
        domainFormPanel.manuallyDefineFields("created");
        assertEquals("Data class reserved field name error", Arrays.asList(
                "'created' is a reserved field name in 'Reserved Field Names Test'.",
                "Please correct errors in Reserved Field Names Test before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        domainFormPanel.manuallyDefineFields("rowid");
        assertEquals("Data class reserved field name error", Arrays.asList(
                "'rowid' is a reserved field name in 'Reserved Field Names Test'.",
                "Please correct errors in Reserved Field Names Test before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        domainFormPanel.manuallyDefineFields("name");
        assertEquals("Data class reserved field name error", Arrays.asList(
                "'name' is a reserved field name in 'Reserved Field Names Test'.",
                "Please correct errors in Reserved Field Names Test before saving."),
                createPage.clickSaveExpectingErrors());

        createPage.clickCancel();
    }

    @Test
    public void testIgnoreReservedFieldNames() throws Exception
    {
        final String expectedInfoMsg = BaseDomainDesigner.RESERVED_FIELDS_WARNING_PREFIX +
                "These fields are already used by LabKey to support this data class: " +
                "Name, Created, createdBy, Modified, modifiedBy, container, created, createdby, modified, modifiedBy, Container.";

        List<String> lines = new ArrayList<>();
        lines.add("Name,TextField1,DecField1,DateField1,Created,createdBy,Modified,modifiedBy,container,created,createdby,modified,modifiedBy,Container,SampleID");

        File inferenceFile = TestFileUtils.writeTempFile("InferFieldsForDataClass.csv", String.join(System.lineSeparator(), lines));

        goToProjectHome();

        String name = "Ignore Reserved Fields";
        log("Create a Data class Type.");
        CreateDataClassPage createPage = goToCreateNewDataClass();
        createPage.setName(name);

        log("Infer fields from a file that contains some reserved fields.");
        DomainFormPanel domainForm = createPage
                .getDomainEditor()
                .setInferFieldFile(inferenceFile);
        checker().verifyEquals("Reserved field warning not as expected",  expectedInfoMsg, domainForm.getPanelAlertText());
        createPage.clickSave();
        DataRegionTable drt = DataRegion(getDriver()).find();
        checker().verifyTrue("Data class not found in list of data classes", drt.getColumnDataAsText("Name").contains(name));
    }

    @Test
    public void testMissingAndUniqueFieldNames()
    {
        goToProjectHome();

        String name = "Unique Field Names Test";
        CreateDataClassPage createPage = goToCreateNewDataClass();
        createPage.setName(name);

        log("Verify error message for missing field name");
        DomainFormPanel domainFormPanel = createPage.getDomainEditor();
        domainFormPanel.manuallyDefineFields(" ");
        assertEquals("Data class missing field name error", Arrays.asList(
                "Missing required field properties.",
                "Please correct errors in Fields before saving."),
                createPage.clickSaveExpectingErrors());
        assertEquals("Data class missing field name detail error",
                "New Field. Error: Please provide a name for each field.",
                domainFormPanel.getField(0).detailsMessage());
        domainFormPanel.removeAllFields(false);

        log("Verify error message for unique field names");
        domainFormPanel.manuallyDefineFields("duplicate");
        domainFormPanel.addField(new FieldDefinition("Duplicate", FieldDefinition.ColumnType.Boolean));
        assertEquals("Data class unique field name error", Arrays.asList(
                "The field name 'Duplicate' is already taken. Please provide a unique name for each field.",
                "Please correct errors in Unique Field Names Test before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        createPage.clickCancel();
    }

    @Test // Issue 48705
    public void testLongFieldNames()
    {
        goToProjectHome();

        String name = "Long Field Names Test";
        CreateDataClassPage createPage = goToCreateNewDataClass();
        createPage.setName(name);

        log("Add a field name > 49 characters");
        DomainFormPanel domainFormPanel = createPage.getDomainEditor();
        domainFormPanel.manuallyDefineFields("This_field_name_is_longer_than_50_characters_for_testing");
        createPage.clickSave();

        DataClassHelper sourceHelper = DataClassHelper.beginAtDataClassesList(this, getProjectName());
        assertEquals("Data class grid should have zero rows", 0, sourceHelper.goToDataClass(name).getDataCount());
    }

    @Test
    public void testFieldUniqueConstraint()
    {
        goToProjectHome();

        String dataClassName = "Unique Constraint Test";
        CreateDataClassPage createPage = goToCreateNewDataClass();
        createPage.setName(dataClassName);

        log("Add a field with a unique constraint");
        String fieldName1 = "field Name1";
        DomainFormPanel domainFormPanel = createPage.getDomainEditor();
        domainFormPanel.manuallyDefineFields(fieldName1)
                .setType(FieldDefinition.ColumnType.Integer)
                .expand().clickAdvancedSettings().setUniqueConstraint(true).apply();
        log("Add another field with a unique constraint");
        String fieldName2 = "fieldName_2";
        domainFormPanel.addField(fieldName2)
                .setType(FieldDefinition.ColumnType.DateAndTime)
                .expand().clickAdvancedSettings().setUniqueConstraint(true).apply();
        log("Add another field which does not have a unique constraint");
        String fieldName3 = "FieldName@3";
        domainFormPanel.addField(fieldName3)
                .setType(FieldDefinition.ColumnType.Boolean);
        createPage.clickSave();

        viewRawTableMetadata(dataClassName);
        verifyTableIndices("unique_constraint_test_", List.of("field_name1", "fieldname_2"));

        log("Remove a field unique constraint and add a new one");
        goToProjectHome();
        CreateDataClassPage updatePage = goToDataClass(dataClassName);
        domainFormPanel = updatePage.getDomainEditor();
        domainFormPanel.getField(fieldName2)
                .expand().clickAdvancedSettings().setUniqueConstraint(false)
                .apply();
        domainFormPanel.getField(fieldName3)
                .expand().clickAdvancedSettings().setUniqueConstraint(true)
                .apply();
        updatePage.clickSave();
        viewRawTableMetadata(dataClassName);
        verifyTableIndices("unique_constraint_test_", List.of("field_name1", "fieldname_3"));
        assertTextNotPresent("unique_constraint_test_fieldname_2");
    }

    private void viewRawTableMetadata(String dataClassName)
    {
        beginAt(WebTestHelper.buildURL("query", getProjectName(), "rawTableMetaData", Map.of("schemaName", "exp.data", "query.queryName", dataClassName)));
    }

    private void verifyTableIndices(String prefix, List<String> indexSuffixes)
    {
        List<String> suffixes  = new ArrayList<>();
        suffixes.add("lsid");
        suffixes.add("name_classid");
        suffixes.addAll(indexSuffixes);

        for (String suffix : suffixes)
            assertTextPresentCaseInsensitive(prefix + suffix);
    }

    private CreateDataClassPage goToCreateNewDataClass()
    {
        DataRegionTable drt = DataRegion(getDriver()).find();
        drt.clickHeaderButton("New Data Class");
        return new CreateDataClassPage(getDriver());
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
