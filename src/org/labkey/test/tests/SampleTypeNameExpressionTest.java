/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class SampleTypeNameExpressionTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeNameExprTest";
    private static final String DEFAULT_SAMPLE_PARENT_VALUE = "SS";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleTypeNameExpressionTest test = (SampleTypeNameExpressionTest)getCurrentTest();
        test.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Sample Types");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSimpleNameExpression()
    {
        String nameExpression = "${A}-${B}.${genId}.${batchRandomId}.${randomId}";
        String data = "A\tB\tC\n" +
                "a\tb\tc\n" +
                "a\tb\tc\n" +
                "a\tb\tc\n";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition("SimpleNameExprTest")
                        .setNameExpression(nameExpression)
                        .setFields(List.of(new FieldDefinition("A", FieldDefinition.ColumnType.String),
                                new FieldDefinition("B", FieldDefinition.ColumnType.String),
                                new FieldDefinition("C", FieldDefinition.ColumnType.String))),
                data
                );

        // Verify SampleType details
        assertTextPresent(nameExpression);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> names = materialTable.getColumnDataAsText("Name");

        log("generated sample names:");
        names.forEach(this::log);

        assertEquals(3, names.size());

        String batchRandomId = names.get(0).split("\\.")[2];
        assertThat(names.get(0), startsWith("a-b.3." + batchRandomId + "."));
        assertThat(names.get(1), startsWith("a-b.2." + batchRandomId + "."));
        assertThat(names.get(2), startsWith("a-b.1." + batchRandomId + "."));
    }

    @Test
    public void testInputsExpression()
    {
        verifyNames(
                "InputsExpressionTest",
                "Name\tB\tMaterialInputs/InputsExpressionTest",
                "${Inputs:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Pat");
    }

    @Test
    public void testParentAliasExpression()
    {
        verifyNames(
                "ParentAliasInputsExpressionTest",
                "Name\tB\tParent",
                "${Parent:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                "Parent", "Jessi");
    }

    @Test
    public void testLookupNameExpression() throws Exception
    {
        String lookupList = "Colors";
        FieldDefinition.LookupInfo colorsLookup = new FieldDefinition.LookupInfo(getProjectName(), "lists", lookupList);
        String nameExpSamples = "NameExpressionSamples";

        // begin by creating a lookupList of colors, the sampleType will reference it
        TestDataGenerator colorsGen = new TestDataGenerator(colorsLookup)
                .withColumns(List.of(new FieldDefinition("ColorName", FieldDefinition.ColumnType.String),
                        new FieldDefinition("Code", FieldDefinition.ColumnType.String)));
        colorsGen.addCustomRow(Map.of("ColorName", "green", "Code", "gr"));
        colorsGen.addCustomRow(Map.of("ColorName", "yellow", "Code", "yl"));
        colorsGen.addCustomRow(Map.of("ColorName", "red", "Code", "rd"));
        colorsGen.addCustomRow(Map.of("ColorName", "blue", "Code", "bl"));
        colorsGen.createList(createDefaultConnection(), "Key");
        colorsGen.insertRows();

        String pasteData = "Color\tNoun\n" +
                "rd\tryder\n" +
                "gr\tgiant\n" +
                "bl\tangel\n" +
                "yL\tjersey";

        // now create a sampleType with a Color column that looks up to Colors
        var sampleTypeDef = new SampleTypeDefinition(nameExpSamples)
                .setFields(List.of(new FieldDefinition("Color", FieldDefinition.ColumnType.String).setLookup(colorsLookup),
                        new FieldDefinition("Noun", FieldDefinition.ColumnType.String)))
                .setNameExpression("TEST-${Color/Code}");   // hopefully this will resolve the 'Code' column from the list
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleTypeDef);

        SampleTypeHelper.beginAtSampleTypesList(this, getProjectName());
        clickAndWait(Locator.linkWithText(nameExpSamples));
        var dataRegion = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var importDataPage = dataRegion.clickImportBulkData();
        importDataPage.selectCopyPaste().
            setImportLookupByAlternateKey(true)
                .setFormat(ImportDataPage.Format.TSV)
                .setText(pasteData)
                .submit();

        // todo: validate once this is working
    }

    @Test
    public void testMaterialInputsExpressionWithParentAliasData()
    {
        verifyNames(
                "MaterialInputsExpressionWithParentAliasData",
                "Name\tB\tParent",
                "${MaterialInputs:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                "Parent", "Sam");
    }

    private void verifyNames(String sampleTypeName, String header, String nameExpression, @Nullable String currentTypeAlias, String namePrefix)
    {
        String name1 = namePrefix + "_1";
        String name2 = namePrefix + "_2";
        String data = header + "\n" +

                // Name provided
                name1 + "\tb\t\n" +
                name2 + "\tb\t\n" +

                // Name generated and uses first input "Bob"
                "\tb\t" + name1 + "," + name2 + "\n" +

                // Name generated and uses defaultValue('SS')
                "\tb\t\n";

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        SampleTypeDefinition definition = new SampleTypeDefinition(sampleTypeName)
                .setNameExpression(nameExpression);
        if (currentTypeAlias != null)
            definition = definition.setParentAliases(Map.of(currentTypeAlias, "(Current Sample Type)"));
        definition = definition.setFields(List.of(new FieldDefinition("B", FieldDefinition.ColumnType.String)));
        sampleHelper.createSampleType(definition, data);

        assertTextPresent(nameExpression);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> names = materialTable.getColumnDataAsText("Name");

        assertTrue("First name (" + names.get(0) + ") not as expected", names.get(0).startsWith(DEFAULT_SAMPLE_PARENT_VALUE + "_"));
        String batchRandomId = names.get(0).split("_")[1];

        assertEquals("Second name not as expected",  name1 + "_" + batchRandomId, names.get(1));

        assertEquals("Third name not as expected", name2,  names.get(2));
        assertEquals("Fourth name not as expected", name1, names.get(3));
    }

}
