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
        _containerHelper.createProject(getProjectName(), "Study");

        long startTime = System.currentTimeMillis();
        importFolderFromZip("C:\\labkey_base\\sampledata\\study\\LabkeyDemoStudy.zip"); //TODO
        elapsedTime = System.currentTimeMillis() - startTime;
        writePerfDataToFile();
    }
}
