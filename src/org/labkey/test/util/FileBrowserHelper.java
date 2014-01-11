/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;
import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;

public class FileBrowserHelper implements FileBrowserHelperParams
{
    BaseSeleniumWebTest _test;
    public FileBrowserHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    @LogMethod(quiet = true)
    public void selectImportDataAction(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        waitForImportDataEnabled();
        clickFileBrowserButton("Import Data");
        _test.waitAndClick(Locator.xpath("//input[@type='button' and not(@disabled)]/../label[contains(text(), " + Locator.xq(actionName) + ")]"));
        _test.clickAndWait(Locator.ext4Button("Import"));
    }

    @LogMethod(quiet = true)
    public void selectFileBrowserItem(@LoggedParam String path)
    {
        boolean startAtRoot = false;

        String[] parts = {};
        StringBuilder nodeId = new StringBuilder();
        if (path.startsWith("/"))
        {
            startAtRoot = true;
            path = path.substring(1);
        }
        if (!path.equals(""))
        {
            parts = path.split("/");
            nodeId.append('/');
        }
        startAtRoot = startAtRoot || parts.length > 1;

        waitForFileGridReady();

        if (startAtRoot)
        {
            expandFileBrowserRootNode();
        }

        for (int i = 0; i < parts.length; i++)
        {
            waitForFileGridReady();

            nodeId.append(parts[i]);

            if (i > 0 || startAtRoot)
                scrollToGridRow(nodeId.toString());

            nodeId.append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                // select last item: click on tree node name
                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                Locator.XPathLocator folderTreeNode = FileBrowserHelperWD.Locators.treeRow(nodeId.toString());

                _test.waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation

                // select/expand tree node
                _test.waitAndClick(folderTreeNode);
                _test.waitForElement(folderTreeNode.withClass("x4-grid-row-selected"));
                _test._ext4Helper.waitForMaskToDisappear();
            }
        }
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
    public void expandFileBrowserRootNode()
    {
        waitForFileGridReady();

        _test.waitAndClick(Locator.css("div.treenav-panel div.x4-panel-body tr[data-recordindex = '0']"));
        _test.waitForElement(Locator.css("div.treenav-panel div.x4-panel-body tr.x4-grid-row-selected[data-recordindex = '0']"), WAIT_FOR_JAVASCRIPT);
  }

    @Override
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        waitForFileGridReady();
        scrollToGridRow(fileName);
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(fileName));
        _test.getWrapper().getEval("selenium.selectFileBrowserCheckbox('" + fileName + "');");
    }

    //In case desired element is not present due to infinite scrolling
    private void scrollToGridRow(String nodeIdEndsWith)
    {
        Locator lastFileGridItem = FileBrowserHelperWD.Locators.gridRow().withPredicate("last()");
        Locator targetFile = FileBrowserHelperWD.Locators.gridRowWithNodeId(nodeIdEndsWith);

        _test.waitForElement(lastFileGridItem);

        String previousLastItemText = null;
        String currentLastItemText = _test.getAttribute(lastFileGridItem, "data-recordid");
        while (!_test.isElementPresent(targetFile) && !currentLastItemText.equals(previousLastItemText))
        {
            _test.scrollIntoView(lastFileGridItem);
            previousLastItemText = currentLastItemText;
            currentLastItemText = _test.getAttribute(lastFileGridItem, "data-recordid");
        }
    }

    @Override
    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    @Override
    public void renameFile(String currentName, String newName)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void moveFile(String fileName, String destinationPath)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void createFolder(String folderName)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void addToolbarButton(String buttonId)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void removeToolbarButton(String buttonId)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void goToConfigureButtonsTab()
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void goToAdminMenu()
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void uploadFile(File file)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void uploadFile(File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties, boolean replace)
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }

    @Override
    public void importFile(String filePath, String importAction)
    {
        selectFileBrowserItem(filePath);
        selectImportDataAction(importAction);
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
    public void openFolderTree()
    {
        throw new UnsupportedOperationException("Method only supported for WebDriver");
    }
}
