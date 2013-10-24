package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;

public class FileBrowserHelperWD implements FileBrowserHelperParams
{
    BaseWebDriverTest _test;
    public FileBrowserHelperWD(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public void expandFileBrowserRootNode()
    {
        // expand root tree node
        _test.waitAndClick(Locator.xpath("//tr[contains(@class, 'x4-grid-tree-node') and @*='/']//div[contains(@class, 'x4-grid-cell-inner-treecolumn')]"));
        _test.waitForElement(Locator.xpath("//tr[contains(@class, 'x4-grid-row-selected') and @*='/']//div[contains(@class, 'x4-grid-cell-inner-treecolumn')]"), WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void selectFileBrowserItem(@LoggedParam String path)
    {
        try
        {
            Thread.sleep(500);
        }
        catch(InterruptedException e)
        {
            _test.log("Sleep for file browser item selection interupted.");
        }

        String[] parts = {};
        StringBuilder nodeId = new StringBuilder();
        if (path.startsWith("/"))
            path = path.substring(1);
        if (!path.equals(""))
        {
            parts = path.split("/");
            nodeId.append('/');
        }
        waitForFileGridReady();

        if (parts.length > 1)
        {
            expandFileBrowserRootNode();
        }

        for (int i = 0; i < parts.length; i++)
        {
//            _test.waitForLoadingMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            waitForFileGridReady();

            nodeId.append(parts[i]).append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                // select last item: click on tree node name
                try
                {
                    Thread.sleep(500);
                }
                catch(InterruptedException e)
                {
                    _test.log("Sleep for file browser item selection interupted.");
                }
                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                // expand tree node: click on expand/collapse icon
                _test.waitForElement(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                LabKeyExpectedConditions.animationIsDone(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                _test.scrollIntoView(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                _test.clickAt(Locator.xpath("//tr[contains(@id, '" + nodeId + "')]//span[contains(@class, 'x4-tree-node') and text()='" + parts[i] + "']"), 1, 1, 0);

            }
        }
    }

    @Override
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        Locator.XPathLocator gridItem;
        gridItem = Locator.xpath("//td[contains(@class, 'x4-grid-cell-row-checker')]/..//span[text()='"+fileName+"']");
        WebElement element = gridItem.waitForElmement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.xpath("//tr[contains(@class, 'x4-grid-row')]/td//span[text()='"+fileName+"']"));
        _test.click(gridItem);
    }

    @Override
    public void selectFileBrowserRoot()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void selectAllFileBrowserFiles()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renameFile(String currentName, String newName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createFolder(String folderName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void goToConfigureButtonsTab()
    {
        goToAdminMenu();

        _test._ext4Helper.clickExt4Tab("Toolbar and Grid Settings");
        _test.waitForText("Configure Grid columns and Toolbar");
    }

    @Override
    public void goToAdminMenu()
    {
        try
        {
            _test.assertElementVisible(Locator.ext4ButtonContainingText("Admin"));
            _test.click(Locator.ext4ButtonContainingText("Admin"));
        }
        catch(AssertionError e)
        {
            _test.click(Locator.xpath("//span[contains(@class, 'x4-toolbar-more-icon')]"));
            _test.click(Locator.xpath("//span[text()='Admin' and contains(@class, 'x4-menu-item-text')]"));
        }
        _test.waitForElement(Ext4HelperWD.Locators.window("Manage File Browser Configuration"));
    }

    @Override
    public void selectImportDataAction(@LoggedParam String actionName)
    {
         waitForFileGridReady();
//       waitForImportDataEnabled();
        _test.click(Locator.linkContainingText("Import Data"));

        _test.waitAndClick(Locator.xpath("//input[@type='button' and not(@disabled)]/../label[contains(text(), " + Locator.xq(actionName) + ")]"));
        _test.clickAndWait(Locator.ext4Button("Import"));
    }

    @Override
    public void uploadFile(File file)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void importFile(String fileName, String importAction)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void clickFileBrowserButton(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        try
        {
            _test.assertElementVisible(Locator.ext4ButtonContainingText(actionName));
            _test.click(Locator.ext4ButtonContainingText(actionName));
        }
        catch(AssertionError e)
        {
            _test.click(Locator.xpath("//span[contains(@class, 'x4-toolbar-more-icon')]"));
            _test.click(Locator.xpath("//span[text()='"+actionName+"' and contains(@class, 'x4-menu-item-text')]"));
        }
    }

    @Override
    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }
}
