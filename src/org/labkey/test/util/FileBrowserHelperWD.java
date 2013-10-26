package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

import static org.labkey.test.BaseSeleniumWebTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_PAGE;

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
        waitForFileGridReady();
        _test.waitAndClick(Locator.xpath("//tr[contains(@class, 'x4-grid-tree-node')]//div[contains(@class, 'x4-grid-cell-inner-treecolumn')]"));
        _test.waitForElement(Locator.xpath("//tr[contains(@class, 'x4-grid-row-selected')]//div[contains(@class, 'x4-grid-cell-inner-treecolumn')]"), WAIT_FOR_JAVASCRIPT);
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
            waitForFileGridReady();

            nodeId.append(parts[i]).append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                // select last item: click on tree node name
                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                try
                {
                    Thread.sleep(500);
                }
                catch(InterruptedException e)
                {
                    _test.log("Sleep for file browser item selection interupted.");
                }
                // expand tree node: click on expand/collapse icon
                _test.waitForElement(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                LabKeyExpectedConditions.animationIsDone(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                _test.scrollIntoView(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                _test.clickAt(Locator.xpath("//tr[contains(@id, '" + nodeId + "')]//span[contains(@class, 'x4-tree-node') and text()='" + parts[i] + "']"), 1, 1, 0);
                _test._ext4Helper.waitForMaskToDisappear();

            }
        }
    }

    @Override
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        waitForFileGridReady();
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(fileName));
        Locator rowSelected = locateGridRowCheckbox(fileName, true);
        Boolean wasChecked = _test.isElementPresent(rowSelected);

        _test.click(locateGridRowCheckbox(fileName, wasChecked));

        if (wasChecked)
            _test.waitForElementToDisappear(rowSelected);
        else
            _test.waitForElement(rowSelected);
    }

    @Override
    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    @Override
    public void selectAllFileBrowserFiles()
    {
        // TODO: this doesn't seem to be working with Firefox, it finds the header checkbox but has trouble clicking it
        Locator file = Locator.css("tr.x-grid3-hd-row div.x-grid3-hd-checker");
        _test.waitForElement(file, WAIT_FOR_PAGE);
        _test.sleep(1000);
        _test.click(file);

        file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-inner x-grid3-hd-checker x-grid3-hd-checker-on']");
        _test.waitForElement(file, WAIT_FOR_PAGE);
    }

    @Override
    public void renameFile(String currentName, String newName)
    {
        selectFileBrowserItem(currentName);
        _test.click(Locator.css(".iconRename"));
        _test.waitForElement(Ext4HelperWD.Locators.window("Rename"));
        _test.setFormElement(Locator.name("renameText-inputEl"), newName);
        _test.clickButton("Rename", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(newName));
    }

    @Override
    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        _test.click(Locator.css(".iconMove"));
        _test.waitForElement(Ext4HelperWD.Locators.window("Choose Destination"));
        //TODO:  this doesn't yet support nested folders
        Locator folder = Locator.xpath("//tr//span[contains(@class, 'x4-tree-node-text') and text() = '" + destinationPath + "']");
        _test.waitForElement(folder);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(folder));
        _test.click(folder);
        _test.clickButton("Move", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
    }

    @Override
    public void createFolder(String folderName)
    {
        clickFileBrowserButton("Create Folder");
        _test.setFormElement(Locator.name("folderName"), folderName);
        _test.clickButton("Submit", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(folderName));
    }

    @Override
    public void addToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        _test.assertElementNotPresent(toolbarShownLocator);
        _test.click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    @Override
    public void removeToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        _test.assertElementPresent(toolbarShownLocator);
        _test.click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
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
        clickFileBrowserButton("Admin");
        _test.waitForElement(Ext4HelperWD.Locators.window("Manage File Browser Configuration"));
    }

    @Override
    public void selectImportDataAction(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        waitForImportDataEnabled();
        clickFileBrowserButton("Import Data");
        _test.waitAndClick(Locator.xpath("//input[@type='button' and not(@disabled)]/../label[contains(text(), " + Locator.xq(actionName) + ")]"));
        _test.clickAndWait(Locator.ext4Button("Import"));
    }

    @Override
    public void uploadFile(File file)
    {
        uploadFile(file, null, null);
    }

    @Override
    public void uploadFile(File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return _test.getFormElement(Locator.xpath("//label[text() = 'Choose a File']/../..//input[contains(@class, 'x4-form-field')]")).equals("");
            }
        }, "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        _test.setFormElement(Locator.css(".single-upload-panel input[type=file]"), file);
        if (description != null)
            _test.setFormElement(Locator.name("description"), description);

        _test.clickButton("Upload", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        if (fileProperties != null && fileProperties.size() > 0)
        {
            _test.waitForElement(Ext4HelperWD.ext4Window("Extended File Properties"));
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _test._ext4Helper.selectComboBoxItem(prop.getName(), prop.getValue());
                else
                    _test.setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            _test.clickButton("Done", 0);
            _test._ext4Helper.waitForMaskToDisappear();

            for (FileBrowserExtendedProperty prop : fileProperties)
                _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(prop.getValue()));
        }

        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(file.getName()));
        if (description != null)
            _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(description));

        // verify that the file and description fields are empty
        _test.assertFormElementEquals(Locator.css(".single-upload-panel input[type=file]"), "");
        _test.assertFormElementEquals(Locator.name("description"), "");
    }

    @Override
    public void importFile(String filePath, String importAction)
    {
        selectFileBrowserItem(filePath);
        selectImportDataAction(importAction);
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
    public Locator locateGridRowCheckbox(String file, boolean checkForSelected)
    {
        return Locator.xpath("//tr[contains(@class, '" + (checkForSelected ? "x4-grid-row-selected" : "x4-grid-row") + "') and ./td//span[text()='" + file + "']]//div[@class='x4-grid-row-checker']");
    }

    @Override
    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForFileAdminEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-admin-enabled')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }
}
