package org.labkey.test.tests;

import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.RelativeUrl;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Category({Assays.class, Daily.class})
public class FlagColumnTest extends BaseWebDriverTest
{
    private static final String ASSAY_NAME = "FlagAssay";

    // Run Flag column on the Runs grid
    public static final String RUN_FLAG = "Flag";

    // Run Flag column on the Results grid
    public static final String RESULT_RUN_FLAG = "Run/Flag";
    public static final String RESULT_SOME_DATA = "SomeData";
    public static final String RESULT_FLAG_A = "ResultFlagA";
    public static final String RESULT_FLAG_B = "ResultFlagB";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("assay");
    }

    @Before
    public void preTest()
    {
        // go to the assay's begin page
        beginAt(new RelativeUrl("assay", "assayBegin")
                .setContainerPath("/" + getProjectName())
                .addParameter("assayName", ASSAY_NAME).toString());
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void initTest()
    {
        FlagColumnTest init = (FlagColumnTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        _containerHelper.createProject(getProjectName(), "Assay");

        // Create an assay with a flag column in the run domain and two flag columns in the results domain
        log("Create assay: " + ASSAY_NAME);
        goToManageAssays();
        var designer = _assayHelper.createAssayDesign("General", ASSAY_NAME);

        designer.goToBatchFields().removeAllFields(false);

        designer.goToRunFields()
                .addField(new FieldDefinition("AnotherRunFlag", FieldDefinition.ColumnType.Flag));

        designer.goToResultsFields()
                .removeAllFields(false)
                .addField(new FieldDefinition(RESULT_SOME_DATA, FieldDefinition.ColumnType.String))
                .addField(new FieldDefinition(RESULT_FLAG_A, FieldDefinition.ColumnType.Flag))
                .addField(new FieldDefinition(RESULT_FLAG_B, FieldDefinition.ColumnType.Flag));

        designer.clickFinish();

        // Import two runs
        log("Import assay data");
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");

        log("Import run1...");
        setFormElement(Locator.name("name"), "Run01");
        setFormElement(Locator.id("TextAreaDataCollector.textArea"),
            "SomeData\nrun1-data1\nrun1-data2");
        clickButton("Save and Import Another Run");

        log("Import run2...");
        setFormElement(Locator.name("name"), "Run02");
        setFormElement(Locator.name("anotherRunFlag"), "run has flag");
        setFormElement(Locator.id("TextAreaDataCollector.textArea"),
                "SomeData\nrun2-data1\nrun2-data2");
        clickButton("Save and Finish");

        log("Add 'Run/Flag' and 'Run/AnotherRunFlag' to the default results grid");
        clickAndWait(Locator.linkWithText("view results"));
        var customizeView = _customizeViewsHelper.openCustomizeViewPanel();
        customizeView.showHiddenItems();
        customizeView.addColumn(new String[] { "Run" });
        customizeView.addColumn(new String[] { "Run", RUN_FLAG});
        customizeView.addColumn(new String[] { "Run", "AnotherRunFlag" });
        customizeView.addSort("RowId", SortDirection.ASC);
        customizeView.saveCustomView();

        // verify expected rows and columns are present
        DataRegionTable grid = new DataRegionTable("Data", getDriver());
        assertThat(grid.getColumnNames(), hasItems(RESULT_SOME_DATA, RESULT_FLAG_A, RESULT_FLAG_B, "Run/AnotherRunFlag", RESULT_RUN_FLAG));
        assertEquals("run1-data1", grid.getDataAsText(0, RESULT_SOME_DATA));
        assertEquals("run1-data2", grid.getDataAsText(1, RESULT_SOME_DATA));
        assertEquals("run2-data1", grid.getDataAsText(2, RESULT_SOME_DATA));
        assertEquals("run2-data2", grid.getDataAsText(3, RESULT_SOME_DATA));

    }

    @Test
    public void testRunFlag()
    {
        final String NEW_FLAG_VALUE = "new flag value: " + RandomUtils.nextInt();

        clickAndWait(Locator.linkWithText("view results"));

        // set the run flag
        DataRegionTable resultsGrid = new DataRegionTable("Data", getDriver());

        // clear any flags from a previous test run
        resultsGrid.clearFlagValues();

        assertEquals("run1-data1", resultsGrid.getDataAsText(0, RESULT_SOME_DATA));
        assertTrue(resultsGrid.isFlagDisabled(0, RESULT_RUN_FLAG));
        resultsGrid.setFlagValue(0, RESULT_RUN_FLAG, NEW_FLAG_VALUE);

        // verify expected flag values
        assertThat(resultsGrid.getFlagValue(0, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(0, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(0, RESULT_RUN_FLAG), is(NEW_FLAG_VALUE));

        assertThat(resultsGrid.getFlagValue(1, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(1, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(1, RESULT_RUN_FLAG), is(NEW_FLAG_VALUE)); // same run

        assertThat(resultsGrid.getFlagValue(2, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(2, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(2, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(3, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(3, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(3, RESULT_RUN_FLAG), is(nullValue()));


        // navigate to runs grid and verify
        clickAndWait(Locator.linkWithText("view runs"));
        DataRegionTable runsGrid = new DataRegionTable("Runs", getDriver());
        assertEquals("Run02", runsGrid.getDataAsText(0, "Name"));
        assertThat(runsGrid.getFlagValue(0, RUN_FLAG), is(nullValue()));

        assertEquals("Run01", runsGrid.getDataAsText(1, "Name"));
        assertThat(runsGrid.getFlagValue(1, RUN_FLAG), is(NEW_FLAG_VALUE));
    }

    @Test
    public void testResultFlag()
    {
        final String NEW_FLAG_VALUE = "new flag value: " + RandomUtils.nextInt();

        clickAndWait(Locator.linkWithText("view results"));

        DataRegionTable resultsGrid = new DataRegionTable("Data", getDriver());

        // clear any flags from a previous test run
        resultsGrid.clearFlagValues();

        // set one of the two results flags
        assertEquals("run1-data1", resultsGrid.getDataAsText(0, RESULT_SOME_DATA));
        assertTrue(resultsGrid.isFlagDisabled(0, RESULT_FLAG_A));
        resultsGrid.setFlagValue(0, RESULT_FLAG_A, NEW_FLAG_VALUE);

        // verify expected flag values
        assertThat(resultsGrid.getFlagValue(0, RESULT_FLAG_A), is(NEW_FLAG_VALUE));
        assertThat(resultsGrid.getFlagValue(0, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(0, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(1, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(1, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(1, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(2, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(2, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(2, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(3, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(3, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(3, RESULT_RUN_FLAG), is(nullValue()));
    }

    @Test
    public void testFlagMultipleRows()
    {
        final String NEW_FLAG_VALUE = "new flag value: " + RandomUtils.nextInt();

        clickAndWait(Locator.linkWithText("view results"));

        DataRegionTable resultsGrid = new DataRegionTable("Data", getDriver());

        // clear any flags from a previous test run
        resultsGrid.clearFlagValues();

        // select multiple rows
        resultsGrid.checkCheckbox(1);
        resultsGrid.checkCheckbox(2);

        // set one of the results flags
        resultsGrid.setFlagValueForSelectedRows(RESULT_FLAG_B, NEW_FLAG_VALUE);

        // verify expected flag values
        assertThat(resultsGrid.getFlagValue(0, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(0, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(0, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(1, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(1, RESULT_FLAG_B), is(NEW_FLAG_VALUE));
        assertThat(resultsGrid.getFlagValue(1, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(2, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(2, RESULT_FLAG_B), is(NEW_FLAG_VALUE));
        assertThat(resultsGrid.getFlagValue(2, RESULT_RUN_FLAG), is(nullValue()));

        assertThat(resultsGrid.getFlagValue(3, RESULT_FLAG_A), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(3, RESULT_FLAG_B), is(nullValue()));
        assertThat(resultsGrid.getFlagValue(3, RESULT_RUN_FLAG), is(nullValue()));
    }

}
