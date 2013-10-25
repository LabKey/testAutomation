package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;
import java.util.List;

public class FileBrowserHelper implements FileBrowserHelperParams
{
    BaseSeleniumWebTest _test;
    public FileBrowserHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    @Override
    public void selectFileBrowserItem(@LoggedParam String path)
    {
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
                clickFileBrowserFileCheckbox(parts[i]);


            }
            else
            {
                // expand tree node: click on expand/collapse icon
                LabKeyExpectedConditions.animationIsDone(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                _test.waitForElement(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                _test.clickAt(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"), "1, 1");

            }
        }
    }

    @Override
    public void clickFileBrowserButton(@LoggedParam String actionName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void expandFileBrowserRootNode()
    {
        selectFileBrowserItem("/");
    }

    @Override
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        waitForFileGridReady();
        _test.waitForElement(Locator.css("div.labkey-filecontent-grid"));
        _test.waitForElement(ExtHelper.locateBrowserFileName(fileName));
        Boolean wasChecked = _test.isElementPresent(Locator.xpath("//div").withClass("x-grid3-row-selected").append("/table/tbody/tr/td/div").withText(fileName));
        _test.getWrapper().getEval("selenium.selectFileBrowserCheckbox('" + fileName + "');");
        if (wasChecked)
            _test.waitForElementToDisappear(Locator.xpath("//div").withClass("x-grid3-row-selected").append("/table/tbody/tr/td/div").withText(fileName));
        else
            _test.waitForElement(Locator.xpath("//div").withClass("x-grid3-row-selected").append("/table/tbody/tr/td/div").withText(fileName));
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
        Locator l = Locator.xpath("//div[text()='" + currentName + "']");
        _test.clickAt(l, "1,1");
        _test.click(Locator.css("button.iconRename"));
        _test.waitForDraggableMask();
        _test._extHelper.setExtFormElementByLabel("Filename:", newName);
        Locator btnLocator = Locator.extButton("Rename");
        _test.click(btnLocator);
    }

    @Override
    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        _test.click(Locator.css("button.iconMove"));
        _test._extHelper.waitForExtDialog("Choose Destination");
        //TODO:  this doesn't yet support nested folders
        Locator folder =Locator.xpath("(//a/span[contains(text(),'" + destinationPath + "')])[2]");
        _test.waitForElement(folder);  //if it still isn't coming up, that's a product bug
        _test.click(folder);
        _test.clickButton("Move", BaseSeleniumWebTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
    }

    @Override
    public void createFolder(String folderName)
    {
        _test.clickButton("Create Folder", BaseSeleniumWebTest.WAIT_FOR_EXT_MASK_TO_APPEAR);
        _test.setFormElement(Locator.name("folderName"), folderName);
        _test.clickButton("Submit", BaseSeleniumWebTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css("#fileBrowser td.x-grid3-cell").withText(folderName));
    }

    @Override
    public void addToolbarButton(String buttonName)
    {
        _test.dragAndDrop(Locator.xpath("//td[contains(@class, 'x-table-layout-cell')]//button[text()='" + buttonName + "']"),
                Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]"));
        _test.waitForElement(Locator.css(".test-custom-toolbar .iconFolderNew"));
    }

    @Override
    public void removeToolbarButton(String buttonName)
    {
        // the button should appear twice, once in the full button list and again in the toolbar. we want the 2nd one.
        _test.click(Locator.buttonContainingText(buttonName).index(2));
        _test.click(Locator.tagContainingText("span", "remove"));
    }

    @Override
    public void goToConfigureButtonsTab()
    {
        goToAdminMenu();

        _test._extHelper.clickExtTab("Toolbar and Grid Settings");
        _test.waitForText("Configure Grid columns and Toolbar");
    }

    @Override
    public void goToAdminMenu()
    {
        try
        {
            _test.assertElementVisible(_test.getButtonLocator("Admin"));
            waitForFileAdminEnabled();
            _test.clickButton("Admin", 0);
        }
        catch(AssertionError e)
        {
            _test.click(Locator.xpath("//span[contains(@class, 'x4-toolbar-more-icon')]"));
            _test.click(Locator.xpath("//span[text()='Admin' and contains(@class, 'x4-menu-item-text')]"));
        }
        _test._extHelper.waitForExtDialog("Manage File Browser Configuration");
    }

    @Override
    public void uploadFile(File file)
    {
        uploadFile(file, null, null);
    }

    @Override
    public void uploadFile(File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties)
    {
        _test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return _test.getFormElement(Locator.xpath("//label[./span[text() = 'Choose a File:']]//..//input[contains(@class, 'x-form-file-text')]")).equals("");
            }
        }, "Upload field did not clear after upload.", BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        _test.setFormElement(Locator.css(".single-upload-panel input[type=file]"), file);
        if (description != null)
            _test._extHelper.setExtFormElementByLabel("Description:", description);

        _test.clickButton("Upload", 0);

        if (fileProperties != null && fileProperties.size() > 0)
        {
            _test._extHelper.waitForExtDialog("Extended File Properties");
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _test._extHelper.selectComboBoxItem(prop.getName(), prop.getValue());
                else
                    _test.setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            _test.clickButton("Done", 0);
            _test._extHelper.waitForExt3MaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                _test.waitForText(prop.getValue());
            }
        }

        _test.waitForElement(Locator.css("#fileBrowser div.x-grid3-col-2").withText(file.getName()));
        if (description != null)
            _test.waitForText(description);    }

    @Override
    public void importFile(String fileName, String importAction)
    {
        clickFileBrowserFileCheckbox(fileName);
        selectImportDataAction(importAction);
    }

    @Override
    public void selectImportDataAction(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        waitForImportDataEnabled();
        _test.clickButton("Import Data", 0);
        _test.waitAndClick(Locator.xpath("//input[@type='radio' and @name='importAction' and not(@disabled)]/../label[text()=" + Locator.xq(actionName) + "]"));
        _test.clickButton("Import");
    }

    @Override
    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForFileAdminEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-admin-enabled')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }
}
