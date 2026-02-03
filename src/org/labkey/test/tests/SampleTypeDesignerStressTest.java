package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.labkey.test.util.query.QueryApiHelper;
import org.labkey.test.util.search.SearchAdminAPIHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Category({Daily.class})
public class SampleTypeDesignerStressTest extends BaseWebDriverTest implements PostgresOnlyTest
{

    private static final String PROJECT_NAME = "SampleType Designer Stress Test";

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

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleTypeDesignerStressTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();
    }

    /**
     *
     * <p>
     * Testing <a href="https://github.com/LabKey/internal-issues/issues/783">Issue 783: Server lockup when updating data class domain design</a>
     * </p>
     * <p>
     * Description from the issue:
     * </p>
     * <p>
     * Lock provisioned table on domain update
     * </p>
     * <p>
     * Operations such as add/drop column requires ACCESS EXCLUSIVE lock on the table. If another transaction performed
     * a SELECT on a provisioned table, adding/dropping columns from the provisioned table would have to wait until the
     * other transaction to complete. If the other transaction happened to be waiting for the add/drop column transaction
     * (in this case, updating exp.dataclass table), the two would deadlock.
     * </p>
     *
     */
    @Test
    public void testDomainDesignerDeadlock() throws IOException, CommandException
    {
        goToProjectHome();

        // Intentionally not using fuzz values for this test. Want to keep it focused on stress.
        final String sampleTypeName = "DomainDesignerStress";

        List<FieldDefinition> fields = new ArrayList<>();

        int numOfFields = 10;
        log(String.format("Create a sample type with %d Int fields.", numOfFields));
        for (int i = 1; i <= numOfFields; i++)
        {
            fields.add(new FieldInfo(String.format("Int%02d", i), FieldDefinition.ColumnType.Integer).getFieldDefinition());
        }

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName).setFields(fields);
        sampleTypeDefinition.setNameExpression("DDStress ${genId}");

        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleTypeDefinition);

        Random randomInt = new Random();

        List<Map<String, Object>> sampleRows = new ArrayList<>();

        int numOfRows = 2_000;
        log(String.format("Add %,d rows with data to the sample type.", numOfRows));
        for (int i = 0; i < numOfRows; i++)
        {
            Map<String, Object> rowMap = new HashMap<>();
            for (FieldDefinition fd : fields)
            {
                rowMap.put(fd.getName(), randomInt.nextInt());
            }
            sampleRows.add(rowMap);
        }

        QueryApiHelper queryApiHelper = new QueryApiHelper(createDefaultConnection(), getProjectName(), "exp.materials", sampleTypeName);
        queryApiHelper.insertRows(sampleRows);

        refresh();

        log("Wait for the indexer to complete after creating the sample type and populating it.");
        SearchAdminAPIHelper.waitForIndexer();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(getDriver());

        sampleTypeHelper.goToSampleType(sampleTypeName);

        // Add as many new fields as there were originally in the sample type design.
        log(String.format("Add %d new fields, one at a time, to the sample type.", numOfFields));
        for (int i = 1; i <= numOfFields; i++)
        {

            // Make each of the new fields a calculation based on an existing field. Adds a little complexity to the scenario.
            FieldDefinition fd = new FieldDefinition(String.format("Half%02d", i), FieldDefinition.ColumnType.Calculation);
            fd.setValueExpression(String.format("Int%02d / 2", i));
            waitAndClickAndWait(Locator.lkButton("Edit Type"));
            UpdateSampleTypePage domainDesignerPage = new UpdateSampleTypePage(getDriver());
            domainDesignerPage.addField(fd);
            domainDesignerPage.clickSave();

            // When running locally if the search indexer isn't allowed to run for every 5th field added the test will
            // fail with a server error because of the indexer. These errors might be valid to investigate, but they are
            // not the kind of errors this test is trying to catch.
            if (i%5 == 0)
            {
                log("Wait for the indexer to complete after adding 5 new fields.");
                SearchAdminAPIHelper.waitForIndexer();
            }

        }

    }

}
