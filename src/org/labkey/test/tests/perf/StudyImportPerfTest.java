/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.tests.perf;

import org.aspectj.lang.annotation.Aspect;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 12/5/12
 * Time: 11:52 AM
 * To change this template use File | Settings | File Templates.
 */

@Aspect
public class StudyImportPerfTest extends PerformanceTest
{
    @Override
    protected String getProjectName()
    {
        return "Study Perf Project";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setIsPerfTest(true);
        _containerHelper.createProject(getProjectName(), "Study");

        long startTime = System.currentTimeMillis();
//        importFolderFromZip(getLabKeyRoot() + "/sampledata/study/LabkeyDemoStudy.zip");
        sleep(4000); //TODO:  // Issue 16877: error importing demo study
        elapsedTime = System.currentTimeMillis() - startTime;
        writePerfDataToFile();
    }
}
