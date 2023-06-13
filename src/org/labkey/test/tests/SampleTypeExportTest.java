/*
 * Copyright (c) 2019 LabKey Corporation
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
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper.ColumnHeaderType;
import org.labkey.test.util.SampleTypeHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class SampleTypeExportTest extends AbstractExportTest
{
    private static final String SAMPLE_TYPE_NAME = SampleTypeExportTest.class.getSimpleName();

    private static final String SAMPLE_DATA = "Name\tBarcode\n" +
            "Q\tabc123\n" +
            "A\tabc123\n" +
            "D\tabc123\n" +
            "C\tabc123\n" +
            "Z\tabc123\n" +
            "E\tabc123\n";

    @BeforeClass
    public static void doSetup() throws Exception
    {
        SampleTypeExportTest initTest = (SampleTypeExportTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);

        initTest.beginAt("/" + initTest.getProjectName() + "/experiment-listSampleTypes.view");
        SampleTypeHelper sampleHelper = new SampleTypeHelper(initTest);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE_NAME)
                .setFields(List.of(new FieldDefinition("Barcode", FieldDefinition.ColumnType.String))),
                SAMPLE_DATA);
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getProjectName()
    {
        return "SampleType Download Test";
    }

    @Override
    protected boolean hasSelectors()
    {
        return true;
    }

    @Override
    protected boolean hasBrokenLookup()
    {
        return false;
    }

    @Override
    protected String getTestColumnTitle()
    {
        return "Name";
    }

    @Override
    protected String getTestLookUpColumnHeader()
    {
        return null;
    }

    @Override
    protected int getTestLookUpColumnIndex()
    {
        return 0;
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 0;
    }

    @Override
    protected String getExportedXlsTestColumnHeader(ColumnHeaderType exportType)
    {
        return "Name";
    }

    @Override
    protected String getExportedTsvTestColumnHeader(ColumnHeaderType exportType)
    {
        return "Name";
    }

    @Override
    protected String getDataRegionColumnName()
    {
        return "Name";
    }

    @Override
    protected String getDataRegionSchemaName()
    {
        return "samples";
    }

    @Override
    protected String getDataRegionQueryName()
    {
        return SAMPLE_TYPE_NAME;
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return SAMPLE_TYPE_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "Material";
    }

    @Override
    protected void goToDataRegionPage()
    {
        beginAt("/" + getProjectName() + "/experiment-listSampleTypes.view");
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));
    }


    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }
}
