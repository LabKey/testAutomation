package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: RyanS
 * Date: 11/26/13
 */
@Category(DailyA.class)
public class ETLErrorTest extends ETLBaseTest
{
    private static final String _projectName = "ETLErrorTestProject";
    private static final String TRANSFORM_REMOTE_STUDY = "/sampledata/dataintegration/ETLTestStudy.zip";
    private static final String TRANSFORM_KEYCONSTRAINT_ERROR = "{simpletest}/SimpleETLCausesKeyConstraintViolation";
    private static final String TRANSFORM_QUERY_ERROR = "{simpletest}/SimpleETLqueryDoesNotExist";
    private static final String TRANSFORM_NOCOL_ERROR = "{simpletest}/SimpleETLCheckerErrorTimestampColumnNonexistent";
    private static final String TRANSFORM_BAD_XML = "{simpletest/SimpleETLbadConfigXML";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return _projectName;
    }

    protected void doCleanup() throws Exception
    {
        super.doCleanup(true);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        runInitialSetup();
        verifyTransformSummary();
        assertTextNotPresent("Should not have loaded invalid transform xml", TRANSFORM_BAD_XML);
        insertSourceRow("0", "Subject 0", null);
        List<String> errors = new ArrayList<String>();
        errors.add("Violation of UNIQUE KEY constraint 'AK_etltarget'. Cannot insert duplicate key in object 'vehicle.etl_target'. The duplicate key value is");
        errors.add("ERROR: Error running executeCopy");
        errors.add("org.labkey.api.pipeline.PipelineJobException: Error running executeCopy");
        runETLandCheckErrors(TRANSFORM_KEYCONSTRAINT_ERROR, true, false, errors);
        errors.clear();
        errors.add("Could not find table: vehicle.etl_source_cheeseburger");
        runETLandCheckErrors(TRANSFORM_QUERY_ERROR, false, true, errors);
        _runETL_NoNav(TRANSFORM_NOCOL_ERROR, false, true);
        //TODO: update the expected error text once issue 19131 is addressed
        assertTextPresent("Data truncation");
    }

    protected void runETLandCheckErrors(String ETLName, boolean hasWork, boolean hasCheckerError, List<String> errors)
    {
        _runETL_NoNav(ETLName, hasWork, hasCheckerError);
        if(!hasCheckerError)
        {
            waitForElement(Locator.xpath("//a[.='Show full log file']"));
            click(Locator.xpath("//a[.='Show full log file']"));
        }
        else
        {
            goToProjectHome();
        }
        assertTextPresent(errors);
    }

    protected void runInitialSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        log("running setup");
        _containerHelper.createProject(getProjectName(), null);
        _expectedErrors = 0;
        _jobsComplete = 0;

        enableModule("DataIntegration", true);
        enableModule("simpletest", true);
        enableModule("Study", true);

        portalHelper.addQueryWebPart("Source", "vehicle", "etl_source", null);
        portalHelper.addQueryWebPart("Target1", "vehicle", "etl_target", null);
        portalHelper.addQueryWebPart("Transfers", "vehicle", "transfer", null);

        String transformRun =  (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ?
                "transformrun" : "TransformRun";

        portalHelper.addQueryWebPart("TransformRun", "dataintegration", transformRun, null);
        portalHelper.addQueryWebPart("TransformHistory", "dataintegration", "TransformHistory", null);
        portalHelper.addQueryWebPart("TransformSummary", "dataintegration", "TransformSummary", null);

        //
        // import our study (used by the remote transform tests).  This study is a continuous time point
        // study to use the same type of ETL source and target that one of our customer uses.
        //
        clickTab("Study");
        importStudyFromZip(new File(getLabKeyRoot(), TRANSFORM_REMOTE_STUDY), true /*ignore query validation*/);
        // bump our pipeline job count since we used the pipeline to import the study
        _jobsComplete++;
    }


}
