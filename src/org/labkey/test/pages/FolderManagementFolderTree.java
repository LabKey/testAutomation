/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.pages;

import org.apache.commons.lang3.SystemUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

/**
 * org.labkey.core.admin.FolderManagementAction -- ?tabId=folderTree
 */
public class FolderManagementFolderTree
{
    private BaseWebDriverTest _test;
    private String _projectName;

    public FolderManagementFolderTree(BaseWebDriverTest test, String projectName)
    {
        _test = test;
        _projectName = projectName;
    }

    public void selectFolderManagementTreeItem(String path, boolean keepExisting)
    {
        selectExtFolderTreeNode(path, "folder-management-tree", keepExisting);
    }

    private void selectExtFolderTreeNode(String path, String markerCls, boolean keepExisting)
    {
        String script =
                "selectExtFolderTreeNode = function(containerPath, markerCls, keepExisting) {\n" +
                        "    // Get Path Array\n" +
                        "    var pathArray = containerPath.split(\"/\");\n" +
                        "    if (pathArray.length == 0)\n" +
                        "        throw 'Unable to parse path: ' + containerPath;\n" +
                        "    // Remove invalid paths due to parsing\n" +
                        "    if (pathArray[0] == \"\")\n" +
                        "        pathArray = pathArray.slice(1);\n" +
                        "    if (pathArray[pathArray.length-1] == \"\")\n" +
                        "        pathArray = pathArray.slice(0, pathArray.length-1);\n" +
                        "    var el = Ext4.DomQuery.selectNode(\"div.\"+markerCls);\n" +
                        "    if (el) {\n" +
                        "        var tree = Ext4.getCmp(el.id);\n" +
                        "        if (tree) {\n" +
                        "            var root = tree.getRootNode();\n" +
                        "            if (!root) {\n" +
                        "                throw 'Unable to find root node.';\n" +
                        "            }\n" +
                        "            var _path = \"\";\n" +
                        "            var node;\n" +
                        "            for (var i=0; i < pathArray.length; i++) {\n" +
                        "                _path += '/' + pathArray[i];\n" +
                        "                node = root.findChild('containerPath', _path, true);\n" +
                        "                if (node) {\n" +
                        "                    if (i==(pathArray.length-1)) {\n" +
                        "                        tree.getSelectionModel().select(node, keepExisting);\n" +
                        "                    }\n" +
                        "                }\n" +
                        "                else {\n" +
                        "                    throw 'Unable to find node: ' + _path;\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        else {\n" +
                        "            throw el.id + ' does not appear to be a valid Ext Component.';\n" +
                        "        }\n" +
                        "    }\n" +
                        "    else {\n" +
                        "        throw 'Unable to locate tree panel: ' + markerCls;\n" +
                        "    }\n" +
                        "};" +
                        "selectExtFolderTreeNode(arguments[0], arguments[1], arguments[2]);";
        _test.executeScript(script, path, markerCls, keepExisting);
    }

    public void expandNavFolders(String... folders)
    {
        for (String folder : folders)
        {
            if (! isFolderExpanded(folder))
            expandFolderNode(folder);
            _test.sleep(500);
        }
    }

    public void deselectFolder(String folder)
    {
        Keys multiSelectKey = SystemUtils.IS_OS_MAC ? Keys.COMMAND : Keys.CONTROL;
        Actions builder = new Actions(_test.getDriver());
        builder.keyDown(multiSelectKey).click(Locator.xpath("//span[text()='"+ folder +"']").findElement(_test.getDriver())).keyUp(multiSelectKey).build().perform();
    }

    public void expandFolderNode(String... folders)
    {
        for(String folder : folders)
        {
            if (!isFolderExpanded(folder))
            {
                _test.longWait().until(ExpectedConditions.elementToBeClickable(Locator.tagWithText("span", folder).parent().child(Locator.tagWithClassContaining("img", "x4-tree-expander"))));
                _test.sleep(500);
                _test.click(Locator.tagWithText("span", folder).parent().child(Locator.tagWithClassContaining("img", "x4-tree-expander")));
                _test.waitForElement(Locator.xpath("//tr[contains(@class,'x4-grid-tree-node-expanded')]/td/div/span[text()='" + folder + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            }
        }
    }

    public void collapseFolderNode(String folder)
    {
        if(isFolderExpanded(folder))
        {
            _test.waitForElement(Locator.xpath("//span[.='" + folder + "']/../img[contains(@class,'x4-tree-expander')]"));
            _test.click(Locator.xpath("//span[text()='" + folder + "']/../img[contains(@class,'x4-tree-expander')]"));
            _test.waitForElement(Locator.tagWithText("span", folder).parent().child(Locator.tagWithClassContaining("img", "x4-tree-expander")));
        }
    }

    public boolean isFolderExpanded(String folder)
    {
        return _test.isElementPresent(Locator.xpath("//tr[contains(@class,'expanded')]/td/div/span[text()='" + folder + "']"));
    }

    public void moveFolder(String folder, String targetFolder, String hoverText, String confirmationText, boolean successExpected, boolean multiple)
    {
        moveFolder(folder, targetFolder, hoverText,confirmationText, successExpected, multiple, true);
    }

    public void moveFolder(String folder, String targetFolder, String hoverText, String confirmationText, boolean successExpected, boolean multiple, boolean confirmMove)
    {
        _test.log("Move folder: '" + folder + "' into '" + targetFolder + "'");
        _test.waitForElement(Locator.xpath("//tr[contains(@id,'treeview')]/td/div/span[text()='" + folder +"']/../img[contains(@class,'x4-tree-icon-parent')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.sleep(1000); //TODO: Figure out what to wait for

        dragAndDrop(Locator.xpath("//tr[contains(@id,'treeview')]/td/div/span[text()='" + folder + "']/../img[contains(@class,'x4-tree-icon-parent')]"), Locator.xpath("//tr[contains(@id,'treeview')]/td/div/span[text()='"+ targetFolder +"']/../img[contains(@class,'x4-tree-icon-parent')]"), BaseWebDriverTest.Position.middle, hoverText);

        if(successExpected)
        {
            _test._extHelper.waitForExtDialog(confirmationText);
            _test.assertTextPresent("You are moving folder '" + folder + "'");
            if(confirmMove)
            {
                _test.clickButton("Confirm Move", 0);
                _test.sleep(500);
                if (multiple)
                {
                    boolean dialogPresent = _test.isElementVisible(Locator.tagContainingText("div", "Are you sure you would like to move this folder?"));
                    while(dialogPresent)
                    {
                        _test.clickButton("Confirm Move", 0);
                        _test.sleep(500);
                        dialogPresent = _test.isElementVisible(Locator.tagContainingText("div", "Are you sure you would like to move this folder?"));
                    }
                }
            }
            else
            {
                _test.clickButton("Cancel", 0);
            }
        }
    }

    private void dragAndDrop(Locator from, Locator to, BaseWebDriverTest.Position pos, String hoverText)
    {
        WebElement fromEl = from.findElement(_test.getDriver());
        WebElement toEl = to.findElement(_test.getDriver());
        int offset = 0;
        int y;
        switch (pos)
        {
            case top:
                y = 5;
                offset = -1;
                break;
            case bottom:
                y = toEl.getSize().getHeight() - 5;
                offset = 1;
                break;
            case middle:
                y = toEl.getSize().getHeight() / 2;
                break;
            default:
                throw new IllegalArgumentException("Unexpected position: " + pos.toString());
        }

        Locator.XPathLocator dragBubble = Locator.byClass("x4-grid-dd-wrap");
        /* For reference, this is the HTML for the drag bubble. Saving here because it's a pain to retrieve.
         * This particular instance was for a legal folder reorder. Will probably vary other drop targets and validity states
        <div style="right: auto; left: 122px; top: 542.7px; z-index: 19010;" class="x4-component x4-layer x4-component-default x4-border-box x4-dd-drag-proxy x4-dd-drag-current x4-tree-drop-ok-above" id="treepanel-1010-body-drag-status-proxy">
          <div class="x4-dd-drop-icon"></div>
          <div id="treepanel-1010-body-drag-status-proxy-ghost" class="x4-dd-drag-ghost">
            <div style="margin: 0px; float: none;" class="x4-grid-dd-wrap" id="ext4-ext-gen1053">Change Display Order</div>
          </div>
        </div>
        */

        Actions builder = new Actions(_test.getDriver());
        builder.clickAndHold(fromEl).build().perform();
        _test.waitForElement(dragBubble);
        builder.moveToElement(toEl, toEl.getSize().getWidth()/2, y)
                .moveByOffset(1, offset) // A little extra move helps trigger the correct hover target
                .build().perform();
        _test.waitForElement(dragBubble.containing(hoverText));
        builder.release().build().perform();
    }

    public void reorderFolder(String folder, String targetFolder, Reorder order, boolean successExpected)
    {
        _test.log("Reorder folder: '" + folder + "' " + toString() + " '" + targetFolder + "'");
        _test.waitForElement(Locator.xpath("//tr/td/div/span[text()='" + folder +"']/../img[contains(@class,'x4-tree-icon-parent')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.sleep(1000); //TODO: Figure out what to wait for
        _test._ext4Helper.waitForMaskToDisappear();
        dragAndDrop(Locator.xpath("//tr/td/div/span[text()='"+ folder + "']/../img[contains(@class,'x4-tree-icon-parent')]"),
                Locator.xpath("//tr/td/div/span[text()='"+ targetFolder + "']/../img[contains(@class,'x4-tree-icon-parent')]"),
                order == Reorder.preceding ? BaseWebDriverTest.Position.top : BaseWebDriverTest.Position.bottom, "Change Display Order");

        if(successExpected)
        {
            Window.Window(_test.getDriver()).withTitle("Change Display Order").waitFor()
                    .clickButton("Yes", true);
            _test._ext4Helper.waitForMaskToDisappear();
        }
        _test.sleep(500);
        //TODO: else {confirm failure}
    }

    private List<String> getDisplayedFolderNames()
    {
        List<String> names = new ArrayList<>();
        List<WebElement> folderEls = _test.getDriver().findElements(Locator.xpath("//div[@class='x4-grid-cell-inner x4-grid-cell-inner-treecolumn']/span"));
        for(WebElement el : folderEls)
        {
            names.add(el.getText());
        }
        return names;
    }
    
    public enum Reorder {following, preceding}
}
