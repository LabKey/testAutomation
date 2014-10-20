package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

/**
 * org.labkey.core.admin.FolderManagementAction -- ?tabId=folderTree
 * TODO: Most methods here are broken for the Ext4 UI
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
                        "    var el = Ext4.DomQuery.selectNode(\"div[class*='\"+markerCls+\"']\");\n" +
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
                        "                        var e = {};\n" +
                        "                        if (keepExisting) {\n" +
                        "                            e.ctrlKey = true;\n" +
                        "                        }\n" +
                        "                        tree.getSelectionModel().select(node, e, keepExisting);\n" +
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
            _test.assertElementPresent(Locator.xpath("//tr[./td/a[text()='" + folder + "']]/td[@class='labkey-nav-tree-node']/a"));
            if(_test.isElementPresent(Locator.xpath("//tr[./td/a[text()='" + folder + "']]/td[@class='labkey-nav-tree-node']/a/img[contains(@src, 'plus')]")))
                _test.click(Locator.xpath("//tr[./td/a[text()='" + folder + "']]/td[@class='labkey-nav-tree-node']/a"));
        }
    }

    private void collapseFolderNode(String folder)
    {
        if(_test.getAttribute(Locator.xpath("//div[./a/span[text()='" + folder + "']]/img[contains(@class, 'x4-tree-elbow')]"), "class").contains("x4-tree-elbow-minus"))
        {
            _test.click(Locator.xpath("//div[./a/span[text()='" + folder + "']]/img[contains(@class, 'x4-tree-elbow')]"));
            _test.waitForElement(Locator.xpath("//div[./a/span[text()='" + folder + "']]/img[contains(@class, 'x4-tree-elbow-plus')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    private void expandFolders(String... folders)
    {
        for (String folder : folders)
        {
            String folderRowXpath = "//li[@class='x4-tree-node']/div[./a/span[text()='"+folder+"']]";
            _test.waitForElement(Locator.xpath(folderRowXpath), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            if(_test.getAttribute(Locator.xpath(folderRowXpath + "/img[contains(@class, 'x4-tree-elbow')]"), "class").contains("plus"))
            {
                _test.click(Locator.xpath(folderRowXpath + "/img[contains(@class, 'x4-tree-elbow')]"));
                _test.waitForElement(Locator.xpath(folderRowXpath + "/img[not(contains(@class, 'plus'))]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            }
        }
    }

    // Specific to this test's folder naming scheme. Digs to requested folder. Adds brackets.
    public void expandFolderNode(String folder)
    {
        _test.waitForElement(Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        if(_test.getAttribute(Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]" + "/div/img[contains(@class, 'x4-tree-elbow')]"), "class").contains("x4-tree-elbow-plus"))
        {
            _test.click(Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]" + "/div/img[contains(@class, 'x4-tree-elbow')]"));
            _test.waitForElementToDisappear(Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]" + "/div/img[contains(@class, 'x4-tree-elbow-plus')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }

        for (int i = 1; i <= folder.length(); i++ )
        {
            String folderRowXpath = "//li[@class='x4-tree-node' and ./div/a/span[text()='"+ _projectName +"']]" + "//li[@class='x4-tree-node']/div[./a/span[text()='["+folder.substring(0, i)+"]']]";
            _test.waitForElement(Locator.xpath(folderRowXpath), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            if(_test.getAttribute(Locator.xpath(folderRowXpath + "/img[contains(@class, 'x4-tree-elbow')]"), "class").contains("plus"))
            {
                _test.click(Locator.xpath(folderRowXpath + "/img[contains(@class, 'x4-tree-elbow')]"));
                _test.waitForElement(Locator.xpath(folderRowXpath + "/img[not(contains(@class, 'plus'))]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            }
        }
        _test.sleep(500);
    }

    public void moveFolder(String folder, String targetFolder, boolean successExpected, boolean multiple)
    {
        moveFolder(folder, targetFolder, successExpected, multiple, true);
    }

    public void moveFolder(String folder, String targetFolder, boolean successExpected, boolean multiple, boolean confirmMove)
    {
        _test.log("Move folder: '" + folder + "' into '" + targetFolder + "'");
        _test.waitForElement(Locator.xpath("//div/a/span[text()='" + folder + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.sleep(1000); //TODO: Figure out what to wait for

        _test.dragAndDrop(Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]" + "//div/a/span[text()='" + folder + "']"), Locator.xpath("//div/a/span[text()='" + targetFolder + "']"), BaseWebDriverTest.Position.middle);
        if(successExpected)
        {
            _test._extHelper.waitForExtDialog("Move Folder");
            if (multiple)
                _test.assertTextPresent("You are moving multiple folders.");
            else
                _test.assertTextPresent("You are moving folder '" + folder + "'");
            if(confirmMove)
            {
                _test.clickButton("Confirm Move", 0);
                if (multiple) _test._extHelper.waitForExtDialog("Moving Folders");
                _test._extHelper.waitForLoadingMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            }
            else
            {
                _test.clickButton("Cancel", 0);
            }
        }
    }

    public void reorderFolder(String folder, String targetFolder, Reorder order, boolean successExpected)
    {
        _test.log("Reorder folder: '" + folder + "' " + toString() + " '" + targetFolder + "'");
        _test.waitForElement(Locator.xpath("//div/a/span[text()='" + folder + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.sleep(1000); //TODO: Figure out what to wait for

        _test.dragAndDrop(Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]" + "//div/a/span[text()='" + folder + "']"), Locator.xpath("//li[@class='x4-tree-node' and ./div/a/span[text()='" + _projectName + "']]" + "//div/a/span[text()='" + targetFolder + "']"), order == Reorder.preceding ? BaseWebDriverTest.Position.top : BaseWebDriverTest.Position.bottom);
        if(successExpected)
        {
            _test._extHelper.waitForExtDialog("Change Display Order");
            _test.clickButton("Confirm Reorder", 0);
        }
        //TODO: else {confirm failure}
    }
    
    public enum Reorder {following, preceding}
}
