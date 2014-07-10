package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;

import java.io.File;
import java.io.IOException;

public class StudyImporter
{
    BaseWebDriverTest _test;

    public StudyImporter(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void zipAndImportStudy(File studyFolder)
    {
        ZipUtil zipper = new ZipUtil(studyFolder, BaseWebDriverTest.getDownloadDir());
        File studyZip;
        try
        {
            studyZip = zipper.zipIt();
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }
        _test.importStudyFromZip(studyZip, true, true);
    }
}
