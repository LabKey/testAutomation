/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
public class NonUSParsingTest extends BaseWebDriverTest
{
    private static final String LIST_SCHEMA = "lists";
    private static final String TEST_MODE = "Date Parsing Mode List";

    private static final String COL_NAME = "name";
    private static final String COL_DATETIME = "dateTimeCol";
    private static final String COL_DATE = "dateCol";
    private static final String COL_TIME = "timeCol";

    private static int completedPipelineJobs = 0;

    private boolean _previousDataParsingMode;

    @BeforeClass
    public static void setupProject()
    {
        NonUSParsingTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        _studyHelper.startCreateStudy()
            .setTimepointType(StudyHelper.TimepointType.DATE)
            .createStudy();

        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Parsing Non-US Dates Test Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return new ArrayList<>();
    }

    private void importBulkNonUSDate(String bulkData)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_MODE));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable
            .clickImportBulkData()
            .setText(bulkData);
        clickButton("Submit");
    }

    private void verifyImportedNonUSDate(List<String> expectedDateTimeCol, List<String> expectedDateCol, List<String> expectedTimeCol)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_MODE));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());

        checker().verifyEquals("Values in " + COL_DATETIME + " are not as expected.",
            expectedDateTimeCol, listTable.getColumnDataAsText(COL_DATETIME));

        checker().verifyEquals("Values in " + COL_DATE + " are not as expected.",
            expectedDateCol, listTable.getColumnDataAsText(COL_DATE));

        checker().verifyEquals("Values in " + COL_TIME + " are not as expected.",
            expectedTimeCol, listTable.getColumnDataAsText(COL_TIME));

        checker().screenShotIfNewError("Non_US_Mode_Error");

        listTable.checkAllOnPage();
        listTable.deleteSelectedRows();
    }

    @Test
    public void testNonUSDateParsingMode() throws IOException, CommandException
    {
        final String bulkData = String.format("%s\t%s\t%s\t%s\n", COL_NAME, COL_DATETIME, COL_DATE, COL_TIME)
                + "A\t23/12/24 14:45\t23/12/24\t14:45\n"
                + "B\t19/11/99 9:32:06.001\t19/11/99\t9:32:06.001\n"
                + "C\t2/3/1972 10:45 pm\t2/3/1972\t10:45 pm\n"
                + "D\t3-2-05 00:00\t3-2-05\t00:00\n"
                + "E\t19July1999 19:32:06\t19/07/99\t19:32:06\n";

        createList(TEST_MODE);

        log("Use 'Non-U.S. date parsing (DMY)'.");
        setDateParsingMode(false);

        List<String> expectedDateTimeCol = List.of("2024-12-23 14:45", "1999-11-19 09:32", "1972-03-02 22:45", "2005-02-03 00:00", "1999-07-19 19:32");
        List<String> expectedDateCol = List.of("2024-12-23", "1999-11-19", "1972-03-02", "2005-02-03", "1999-07-19");
        List<String> expectedTimeCol = List.of("14:45:00", "09:32:06", "22:45:00", "00:00:00", "19:32:06");

        importBulkNonUSDate(bulkData);
        verifyImportedNonUSDate(expectedDateTimeCol, expectedDateCol, expectedTimeCol);

        log("Set a non-standard Date display format.");
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, getProjectName());
        projectSettingsPage.setDefaultDateDisplayInherited(false);
        projectSettingsPage.setDefaultDateDisplay(BaseSettingsPage.DATE_FORMAT.dd_MMM_yyyy);
        projectSettingsPage.save();

        // Issue 50420: LKS/LKSM: Non US parsing doesn't seem to be respected: US parsing setting should be queried at Root folder level
        expectedDateCol = List.of("23-Dec-2024", "19-Nov-1999", "02-Mar-1972", "03-Feb-2005", "19-Jul-1999");

        importBulkNonUSDate(bulkData);
        verifyImportedNonUSDate(expectedDateTimeCol, expectedDateCol, expectedTimeCol);

        log("Reset project settings to clear non-standard Date display format.");
        BaseSettingsPage.resetSettings(createDefaultConnection(), getProjectName());

        restoreDateParsingMode();
    }

    private void createList(String listName) throws IOException, CommandException
    {
        // Delete the list if it already exists.
        if (DomainUtils.doesDomainExist(getProjectName(), LIST_SCHEMA, listName))
        {
            DomainUtils.deleteDomain(getProjectName(), LIST_SCHEMA, listName);
        }

        goToProjectHome();
        new IntListDefinition(listName, "id")
            .setFields(List.of(
                new FieldDefinition(COL_NAME, FieldDefinition.ColumnType.String),
                new FieldDefinition(COL_DATETIME, FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition(COL_DATE, FieldDefinition.ColumnType.Date),
                new FieldDefinition(COL_TIME, FieldDefinition.ColumnType.Time)
            ))
            .create(createDefaultConnection(), getProjectName());
    }

    @Test
    public void testLoadStudyWithNonStandardDateColumn()
    {
        log("Importing a study where a dataset has some dates in the non-standard format");
        goToProjectHome();
        completedPipelineJobs = completedPipelineJobs + 1;
        importFolderFromZip(TestFileUtils.getSampleData("DateParsing/StudyForDateParsing.zip"), false, completedPipelineJobs);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText("dataset"));
        clickAndWait(Locator.linkContainingText("Dataset1")); // dataset where timestamp column ("Date") is configured with a non-standard (date-only) display format.

        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("Incorrect date-time parsed while importing (" + COL_DATETIME + ")", Arrays.asList("2020-11-29 00:23", "2020-11-28 00:23", "2024-02-05 16:36"),
            table.getColumnDataAsText(COL_DATETIME));

        checker().verifyEquals("Incorrect date parsed while importing (" + COL_DATE + ")", Arrays.asList("2020-11-29", "2020-11-28", "2024-02-05"),
            table.getColumnDataAsText(COL_DATE));

        checker().verifyEquals("Incorrect time parsed while importing (" + COL_TIME + ")", Arrays.asList("00:23:00", "00:23:00", "16:36:00"),
            table.getColumnDataAsText(COL_TIME));
    }

    private void setDateParsingMode(boolean useUSMode)
    {
        LookAndFeelSettingsPage lookAndFeelSettingsPage = LookAndFeelSettingsPage.beginAt(this);
        _previousDataParsingMode = lookAndFeelSettingsPage.isUsDateParsingModeChecked();
        lookAndFeelSettingsPage.setDateParsingMode(useUSMode);
        lookAndFeelSettingsPage.save();
    }

    private void restoreDateParsingMode()
    {
        setDateParsingMode(_previousDataParsingMode);
    }
}
