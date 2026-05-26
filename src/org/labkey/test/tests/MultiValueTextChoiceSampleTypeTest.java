/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.labkey.test.util.SampleTypeHelper.beginAtSampleTypesList;
import static org.labkey.test.util.TestDataGenerator.randomDomainName;
import static org.labkey.test.util.TestDataGenerator.randomFieldName;
import static org.labkey.test.util.TestDataGenerator.randomTextChoice;
import static org.labkey.test.util.TestDataGenerator.shuffleSelect;

@Category({Daily.class})
public class MultiValueTextChoiceSampleTypeTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String SUB_FOLDER = "ChildFolder_MultiValueTextChoice_SampleType_Test";
    private final String SUB_FOLDER_PATH = getProjectName() + "/" + SUB_FOLDER;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return "MultiValueTextChoice_SampleType_Test";
    }

    @BeforeClass
    public static void setupProject()
    {
        MultiValueTextChoiceSampleTypeTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        _containerHelper.createSubfolder(getProjectName(), SUB_FOLDER);
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();
    }

    @Before
    public void beforeTest()
    {
        goToProjectHome();
    }

    private TestDataGenerator createSampleType(String sampleTypeName, String sampleNamePrefix, String multiValueTextChoiceFieldName, List<String> multiValueTextChoiceValues)
    {
        log(String.format("Create a new sample type named '%s'.", sampleTypeName));
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName);
        sampleTypeDefinition.setNameExpression(String.format("%s${genId}", sampleNamePrefix));

        log(String.format("Add a MultiValueTextChoice field named '%s'.", multiValueTextChoiceFieldName));
        FieldDefinition textChoiceField = new FieldDefinition(multiValueTextChoiceFieldName, ColumnType.MultiValueTextChoice);
        textChoiceField.setMultiChoiceValues(multiValueTextChoiceValues);

        sampleTypeDefinition.addField(textChoiceField);

        return SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), sampleTypeDefinition);
    }

    /**
     * Validate cross folder MVTC to TC conversion.
     */
    @Test
    public void testCrossFolderMVTCtoTCConversion() throws IOException, CommandException
    {
        final String sampleTypeName = randomDomainName("MVTC_Sample_Edit", DomainUtils.DomainKind.SampleSet);
        final String multiValueTextChoiceFieldName = randomFieldName("MultiValueTextChoice_Field");
        final String namePrefix = "MVTC_";
        int samplesCount = 3;
        List<String> mvtcValues = randomTextChoice(10);

        // Create Sample type in main folder.
        TestDataGenerator dataGenerator = createSampleType(sampleTypeName, namePrefix, multiValueTextChoiceFieldName, mvtcValues);

        log("Create some samples in child folder. They have MultiValueTextChoice filed filled with random multiple values.");

        for (int i = 1; i <= samplesCount; i++)
        {
            Map<String, Object> sample = new HashMap<>();
            String sampleName = String.format("%s%d", namePrefix, i);
            sample.put("Name", sampleName);
            sample.put(multiValueTextChoiceFieldName, shuffleSelect(mvtcValues, 2));
            dataGenerator.addCustomRow(sample);
        }

        dataGenerator.insertRows(WebTestHelper.getRemoteApiConnection(), SUB_FOLDER_PATH);

        // Check that impossible to convert MVTC to TC.
        DomainFieldRow fieldRow = beginAtSampleTypesList(this, getProjectName())
                .goToEditSampleType(sampleTypeName)
                .getFieldsPanel()
                .getField(multiValueTextChoiceFieldName)
                .expand();
        checker().wrapAssertion(() ->
                assertThatThrownBy(() -> fieldRow.setAllowMultipleSelections(false))
                        .as("'Allow Multiple Selections' checkbox should not be available")
                        .hasMessageContaining("Allow Multiple Selections checkbox isn't enabled")
        );

        // Edit all MVTC fields to have 1 chosen value.
        DataRegionTable samplesTable = beginAtSampleTypesList(this, SUB_FOLDER_PATH)
                .goToSampleType(sampleTypeName)
                .getSamplesDataRegionTable();

        for (int i = 0; i < samplesCount; i++)
        {
            UpdateQueryRowPage updateQueryRowPage = samplesTable.clickEditRow(i);
            updateQueryRowPage.setField(multiValueTextChoiceFieldName, shuffleSelect(mvtcValues, 1));
            updateQueryRowPage.submit();
        }

        // Convert MVTC to TC.
        UpdateSampleTypePage updateSampleTypePage = beginAtSampleTypesList(this, getProjectName())
                .goToEditSampleType(sampleTypeName);
        updateSampleTypePage.getFieldsPanel()
                .getField(multiValueTextChoiceFieldName)
                .expand()
                .setAllowMultipleSelections(false, true);
        updateSampleTypePage.clickSave();

        // Check that impossible to choose multiple values.
        samplesTable = beginAtSampleTypesList(this, SUB_FOLDER_PATH)
                .goToSampleType(sampleTypeName)
                .getSamplesDataRegionTable();
        UpdateQueryRowPage updateQueryRowPage = samplesTable.clickEditRow(0);
        checker().wrapAssertion(() ->
                assertThatThrownBy(() -> updateQueryRowPage.setField(multiValueTextChoiceFieldName, shuffleSelect(mvtcValues, 2)))
                        .as("MVTC element isn't found on the page.")
                        .hasMessageContaining("Unable to find element")
        );
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), false);
    }
}
