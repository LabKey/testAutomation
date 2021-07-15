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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.experiment.CreateDataClassPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        String name = "Name Already Exists Test";
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
                "Property name 'created' is a reserved name."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        domainFormPanel.manuallyDefineFields("rowid");
        assertEquals("Data class reserved field name error", Arrays.asList(
                "Property name 'rowid' is a reserved name."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        domainFormPanel.manuallyDefineFields("name");
        assertEquals("Data class reserved field name error", Arrays.asList(
                "Property name 'name' is a reserved name."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        createPage.clickCancel();
    }

    @Test
    public void testIgnoreReservedFieldNames()
    {
        final String expectedInfoMsg = "Reserved fields found in your file are not shown below. " +
                "These fields are already used by LabKey to support this data class: " +
                "Name, Created, createdBy, Modified, modifiedBy, container, created, createdby, modified, modifiedBy, Container.";

        List<String> lines = new ArrayList<>();
        lines.add("Name,TextField1,DecField1,DateField1,Created,createdBy,Modified,modifiedBy,container,created,createdby,modified,modifiedBy,Container,SampleID");

        if (!TestFileUtils.getTestTempDir().exists())
            TestFileUtils.getTestTempDir().mkdirs();
        File inferenceFile = TestFileUtils.saveFile(TestFileUtils.getTestTempDir(), "InferFieldsForDataClass.csv", String.join(System.lineSeparator(), lines));

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
                "Please correct errors in Fields before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        createPage.clickCancel();
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
