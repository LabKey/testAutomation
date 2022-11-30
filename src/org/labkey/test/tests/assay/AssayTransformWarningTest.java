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
package org.labkey.test.tests.assay;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.QCAssayScriptHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Assays.class, Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public final class AssayTransformWarningTest extends BaseWebDriverTest
{
    public static final File JAVA_TRANSFORM_SCRIPT = TestFileUtils.getSampleData("qc/transformWarning.jar");
    public static final File R_TRANSFORM_SCRIPT = TestFileUtils.getSampleData("qc/assayTransformWarning.R");
    public static final File R_TRANSFORM_ERROR_SCRIPT = TestFileUtils.getSampleData("qc/assayTransformError.R");

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @BeforeClass
    public static void initTest()
    {
        AssayTransformWarningTest init = (AssayTransformWarningTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        new RReportHelper(this).ensureRConfig();
        new QCAssayScriptHelper(this).ensureEngineConfig();

        _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Test
    public void testJavaTransformWarning()
    {
        String assayName = "transformWarningJar";
        String importData = "ParticipantId\nJavaWarned";
        String runName = "java transform run";

        _assayHelper.createAssayDesign("General", assayName)
            .addTransformScript(JAVA_TRANSFORM_SCRIPT)
            .clickFinish();

        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");
        assertElementPresent(Locators.labkeyError.containing("Inline warning from Java transform."));
        assertElementPresent(Locator.linkWithText("Warning link").withAttribute("href", "http://www.labkey.test"));

        File extraFile1 = clickAndWaitForDownload(Locator.linkContainingText("test1.txt"));
        File extraFile2 = clickAndWaitForDownload(Locator.linkContainingText("test2.tsv"));

        assertEquals("Wrong text in file generated by transform", "This is test file 1 (Java).", TestFileUtils.getFileContents(extraFile1).trim());
        assertEquals("Wrong text in file generated by transform", "This is test file 2 (Java).", TestFileUtils.getFileContents(extraFile2).trim());

        clickButton("Proceed");

        clickAndWait(Locator.linkWithText(runName), longWaitForPage);

        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals(1, table.getDataRowCount());
        assertTextPresent("JavaWarned");
    }

    @Test
    public void testRTransformWarning()
    {
        String assayName = "transformWarningR";
        String importData = "ParticipantId\nRWarned";
        String runName = "R transform run";

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", assayName)
            .addTransformScript(R_TRANSFORM_SCRIPT);
        assayDesignerPage.goToRunFields()
            .addField("myFile")
            .setLabel("My File")
            .setType(FieldDefinition.ColumnType.File);
        assayDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);

        // Use this file as a sample upload file parameter
        setFormElement(Locator.name("myFile"), JAVA_TRANSFORM_SCRIPT);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");
        assertElementPresent(Locators.labkeyError.containing("Inline warning from R transform."));

        // Verify file parameter is still present. 1 visible + 1 hidden + 2 js texts.
        assertTextPresent(JAVA_TRANSFORM_SCRIPT.getName(), 4);
        assertElementPresent(Locator.linkWithText("remove"));

        assertElementPresent(Locator.linkWithText("Warning link").withAttribute("href", "http://www.labkey.test"));

        File rOutFile = clickAndWaitForDownload(Locator.linkContainingText(R_TRANSFORM_SCRIPT.getName() + "out"));
        File extraFile1 = clickAndWaitForDownload(Locator.linkContainingText("test1.txt"));
        File extraFile2 = clickAndWaitForDownload(Locator.linkContainingText("test2.tsv"));

        String rOut = TestFileUtils.getFileContents(rOutFile);
        assertTrue("Didn't capture R output", rOut.contains("proc.time()"));
        assertEquals("Wrong text in file generated by transform", "This is test file 1 (R).", TestFileUtils.getFileContents(extraFile1).trim());
        assertEquals("Wrong text in file generated by transform", "This is test file 2 (R).", TestFileUtils.getFileContents(extraFile2).trim());

        assertRadioButtonSelected(Locator.id("Previouslyuploadedfiles"));
        clickButton("Proceed");

        clickAndWait(Locator.linkWithText(runName), longWaitForPage);

        // Verify file uploaded
        assertTextPresent("assaydata" + File.separator + JAVA_TRANSFORM_SCRIPT.getName(), 1);

        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals(1, table.getDataRowCount());
        assertTextPresent("RWarned");
    }

    @Test
    public void testRTransformError()
    {
        String assayName = "transformErrorR";
        String importData = "ParticipantId\nRError";
        String runName = "R transform run";

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", assayName)
                .addTransformScript(R_TRANSFORM_ERROR_SCRIPT);
        assayDesignerPage.goToRunFields()
                .addField("myFile")
                .setLabel("My File")
                .setType(FieldDefinition.ColumnType.File);
        assayDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);

        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");
        assertTextPresent("There are errors in the input file");
        assertElementPresent(Locator.tag("td").containing("Col1"));
        assertElementPresent(Locator.tag("td").containing("test2"));
    }
}
