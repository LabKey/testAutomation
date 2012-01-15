package org.labkey.test.tests;

import org.labkey.test.Locator;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 1/11/12
 * Time: 7:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ListExportTest extends ListTest
{
    String downloadDirectory = "";
    @Override
    protected String getProjectName()
    {
        return "List Download Test";
    }

    @Override
    protected void doTestSteps()
    {
        downloadDirectory = System.getenv("DL_DIR");

        setUpList(getProjectName());
        String filter = ".*Colors.*\\.xlsx";
        int fileCount = getNumFilesWithNameInDlDir(downloadDirectory, filter);
        exportList();
        sleep(1000);
        File[] fileAfterDownload =  getFilesWithNameInDlDir(downloadDirectory, filter);
        assertEquals(fileCount+1, fileAfterDownload.length);
        fileAfterDownload[fileAfterDownload.length-1].deleteOnExit();

        //read back the file and verify it has the number of rows expected
        //TODO:  read and verify downloaded file
    }

    private File[] getFilesWithNameInDlDir(String dir, String fileRegEx)
    {
        File folder = new File(dir);
        File[] list = folder.listFiles(new FilterOnName((fileRegEx)));
        return list;

    }
    private int getNumFilesWithNameInDlDir(String dir, String fileRegEx)
    {
        return getFilesWithNameInDlDir(dir, fileRegEx).length;
    }

    private void exportList()
    {
        clickButton("Export", 0);
        click(Locator.name("excelExportType", 1));
        clickButton("Export to Excel", 0);
    }

    @Override
    protected void doCleanup()
    {
       deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class FilterOnName implements FilenameFilter
    {
        protected String regex = "";

        public FilterOnName(String regex)
        {
            this.regex = regex;
        }
        @Override
        public boolean accept(File dir, String name)
        {
            boolean b = name.matches(regex);
            return b;
        }
    }
}
