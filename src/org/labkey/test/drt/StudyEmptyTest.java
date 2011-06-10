/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
package org.labkey.test.drt;

import java.io.File;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public abstract class StudyEmptyTest extends StudyBaseTest
{
    protected static final String ARCHIVE_TEMP_DIR = getSampleDataPath() + "drt_temp";
    protected static final String SPECIMEN_ARCHIVE_A = getSampleDataPath() + "specimens/sample_a.specimens";

    private SpecimenImporter _specimenImporter;

    protected void doCreateSteps()
    {
        importStudy();
        startSpecimenImport(2);

        // wait for study (but not specimens) to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
    }

    // Start importing the specimen archive.  This can load in the background while executing the first set of
    // verification steps to speed up the test.  Call waitForSpecimenImport() before verifying specimens.
    protected void startSpecimenImport(int completeJobsExpected)
    {
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), completeJobsExpected);
        _specimenImporter.startImport();
    }

    protected void waitForSpecimenImport()
    {
        _specimenImporter.waitForComplete();
    }

    @Override
    protected void doCleanup() throws Exception
    {
        super.doCleanup();

        deleteDir(new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR));
    }
}
