/*
 * Copyright (c) 2016-2018 LabKey Corporation
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
import org.labkey.test.categories.DailyC;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class SampleSetNameExpressionTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleSetNameExprTest";

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
        SampleSetNameExpressionTest test = (SampleSetNameExpressionTest)getCurrentTest();
        test.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Sample Sets");
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
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet("SimpleNameExprTest", nameExpression,
                Map.of("A", FieldDefinition.ColumnType.String,
                        "B", FieldDefinition.ColumnType.String,
                        "C", FieldDefinition.ColumnType.String),
                data
                );

        // Verify SampleSet details
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
        String nameExpression = "${Inputs:first:defaultValue('SS')}_${batchRandomId}";
        String data = "Name\tB\tMaterialInputs/InputsExpressionTest\n" +

                // Name provided
                "Bob\tb\t\n" +
                "Susie\tb\t\n" +

                // Name generated and uses first input "Bob"
                "\tb\tBob,Susie\n" +

                // Name generated and uses defaultValue('SS')
                "\tb\t\n";

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet("InputsExpressionTest", nameExpression, Map.of("B", FieldDefinition.ColumnType.String), data);

        assertTextPresent(nameExpression);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> names = materialTable.getColumnDataAsText("Name");

        assertTrue(names.get(0).startsWith("SS_"));
        String batchRandomId = names.get(0).split("_")[1];

        assertTrue(names.get(1).equals("Bob_" + batchRandomId));

        assertTrue(names.get(2).equals("Susie"));
        assertTrue(names.get(3).equals("Bob"));
    }

}
