package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

/**
 * User: tchadick
 * Date: 12/18/12
 * Time: 11:56 AM
 */
public class WorkbookHelper extends AbstractHelperWD
{
    public WorkbookHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    /**
     * Create a new workbook in a project containing a workbook webpart on the portal page
     * @param project Name of parent project
     * @param title Title of created workbook
     * @param description Description of created workbook
     * @param folderType Type of created workbook
     */
    public void createWorkbook(String project, String title, String description, WorkbookFolderType folderType)
    {
        _test.clickFolder(project);
        _test.clickButton("Insert New");

        _test.setFormElement(Locator.id("workbookTitle"), title);
        _test.setFormElement(Locator.id("workbookDescription"), description);
        _test.selectOptionByValue(Locator.id("workbookFolderType"), folderType.toString());

        _test.clickButton("Create Workbook");
    }



    public enum WorkbookFolderType
    {
        ASSAY_WORKBOOK("Assay Test Workbook"),
        FILE_WORKBOOK("File Test Workbook"),
        DEFAULT_WORKBOOK("Workbook");

        private final String _type;

        WorkbookFolderType(String type)
        {
            this._type = type;
        }

        @Override
        public String toString()
        {
            return _type;
        }
    }

    /**
     * Create and verify file workbook
     * @param projectName Name of parent project
     * @param title Title of created workbook
     * @param description Description of created workbook
     */
    public void createFileWorkbook(String projectName, String title, String description)
    {
        // Create File Workbook
        createWorkbook(projectName, title, description, WorkbookFolderType.FILE_WORKBOOK);
        _test.waitForElement(Locator.linkWithText("Files"));
        Assert.assertEquals(title, _test.getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        Assert.assertEquals(description, _test.getText(Locator.xpath("//div[@id='wb-description']")));
        _test.assertLinkNotPresentWithText(title); // Should not appear in folder tree.
    }

}
