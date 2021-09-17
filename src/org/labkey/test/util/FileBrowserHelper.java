/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;
import static org.labkey.test.components.ext4.Window.Window;

public class FileBrowserHelper extends WebDriverWrapper
{
    private static final String IMPORT_SIGNAL_NAME = "import-actions-updated";
    private static final String FILE_LIST_SIGNAL_NAME = "file-list-updated";

    public static final String ABSOLUTE_FILE_PATH_COLUMN_ID = "10";
    private static final Locator fileGridCell = Locator.tagWithClass("div", "labkey-filecontent-grid").append(Locator.tagWithClass("div", "x4-grid-cell-inner"));

    private final WrapsDriver _driver;

    public FileBrowserHelper(WrapsDriver driver)
    {
        _driver = driver;
    }

    public FileBrowserHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driver.getWrappedDriver();
    }

    private WebDriverWait uploadWait(File file)
    {
        long fileSizeMb = file.length() / (1024 * 1024);
        long uploadTimeout = Math.max(20, fileSizeMb); // 1 second per MB, minimum of 20 seconds to upload a file.
        if (TestProperties.isCloudPipelineEnabled())
            uploadTimeout = uploadTimeout * 3; // Triple time to upload to a cloud file root

        return new WebDriverWait(getDriver(), uploadTimeout);
    }

    @LogMethod(quiet = true)
    public void expandFileBrowserRootNode()
    {
        selectFileBrowserItem("/");
    }

    @LogMethod
    public void selectFileBrowserItem(@LoggedParam String path)
    {
        waitForFileGridReady();
        openFolderTree();

        String[] parts;
        if (path.equals("/"))
            parts = new String[]{""}; // select root node
        else
            parts = path.split("/");

        String baseNodeId;
        try
        {
            baseNodeId = Locators.treeRow(0).findElement(getDriver()).getAttribute("data-recordid");
        }
        catch (StaleElementReferenceException retry)
        {
            baseNodeId = Locators.treeRow(0).findElement(getDriver()).getAttribute("data-recordid");
        }

        StringBuilder nodeId = new StringBuilder();
        nodeId.append(baseNodeId);

        for (int i = 0; i < parts.length; i++)
        {
            nodeId.append(parts[i]);
            if (!parts[i].equals(""))
            {
                nodeId.append('/');
            }

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
                checkFileBrowserFileCheckbox(parts[i]);// select last item: click on tree node name
            else
            {
                try
                {
                    selectFolderTreeNode(nodeId.toString());
                }
                catch (NoSuchElementException nse)
                {
                    throw new AssertionError("Folder not found: " + parts[i], nse);
                }
            }
        }
    }

    public void sortFileBrowserColumn(String columnText, SortDirection sortDirection)
    {
        Locator headerLoc = Locator.tagWithClass("div", "fbrowser")
                .descendant(Locator.tagWithClass("div", "x4-column-header")
                .withChild(Locator.tagWithClass("div", "x4-column-header-inner")
                .withChild(Locator.tagWithClass("span", "x4-column-header-text").withText(columnText))));
        String sortText = sortDirection == SortDirection.DESC ? "Sort Descending" : "Sort Ascending";

        WebElement headerElement = headerLoc.findElement(getDriver());
        mouseOver(headerElement);   // reveal the element to toggle the sort menu
        WebElement headerMenuTrigger = Locator.tagWithClass("div", "x4-column-header-trigger").waitForElement(headerElement, 1500);
        mouseOver(headerMenuTrigger); // make it interactable
        headerMenuTrigger.click();
        WebElement sortMenuLink = Locator.tagWithClass("a", "x4-menu-item-link")
                .withDescendant(Locator.tagWithClass("span", "x4-menu-item-text").withText(sortText))
                .waitForElement(getDriver(), 1500);
        doAndWaitForFileListRefresh(()-> sortMenuLink.click());

        assertThat("Did not get expected sort for column ["+columnText+"]",
                getSortDirection(headerLoc), is(sortDirection));
    }

    private SortDirection getSortDirection(Locator headerElementLoc)
    {
        String elementClass = headerElementLoc.findElement(getDriver()).getAttribute("class");
        if (elementClass.contains("column-header-sort-DESC"))
            return SortDirection.DESC;
        else if (elementClass.contains("column-header-sort-ASC"))
            return SortDirection.ASC;
        else
            return null; // No sort defined for column
    }

    @LogMethod(quiet = true)
    private void selectFolderTreeNode(@LoggedParam String nodeId)
    {
        final Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        final Locator.XPathLocator folderTreeNodeLoc = fBrowser.append(Locator.tag("tr").withPredicate("starts-with(@id, 'treeview')").attributeEndsWith("data-recordid", nodeId));

        final WebElement folderTreeNode = waitForElement(folderTreeNodeLoc);
        waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation

        boolean atRoot = !isElementPresent(fBrowser.append(Locator.byClass("x4-grid-row-selected"))); // Root node is not initially highlighted
        if (atRoot && "0".equals(folderTreeNode.getAttribute("data-recordindex")))
        {
            doAndWaitForPageSignal(folderTreeNode::click, IMPORT_SIGNAL_NAME);
        }
        else if (!folderTreeNode.getAttribute("class").contains("x4-grid-row-selected"))
        {
            // Scroll bars get in the way sometimes, need to scroll folder tree manually
            scrollIntoView(folderTreeNode);
            doAndWaitForFileListRefresh(folderTreeNode::click);
        }
    }

    private String doAndWaitForFileListRefresh(Runnable func)
    {
        return doAndWaitForFileListRefresh(func, shortWait());
    }

    private String doAndWaitForFileListRefresh(Runnable func, WebDriverWait wait)
    {
        waitForFileGridReady();
        Mutable<String> signal = new MutableObject<>();
        doAndWaitForElementToRefresh(() -> signal.setValue(doAndWaitForPageSignal(func, FILE_LIST_SIGNAL_NAME, wait)), this::waitForGrid, wait);
        return signal.getValue();
    }

    private WebElement waitForGrid()
    {
        final Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        final Locator.XPathLocator emptyGrid = fBrowser.append(Locator.tagWithClass("div", "x4-grid-empty"));
        final Locator.XPathLocator gridRow = fBrowser.append(Locators.gridRow());

        _ext4Helper.waitForMaskToDisappear();
        return Locator.waitForAnyElement(shortWait(), gridRow, emptyGrid);
    }

    @LogMethod(quiet = true)
    public void checkFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        checkFileBrowserFileCheckbox(fileName, true);
    }

    private void checkFileBrowserFileCheckbox(String fileName, boolean checkTheBox)
    {
        scrollToGridRow(fileName);

        final Checkbox checkbox;
        try
        {
            checkbox = Ext4Checkbox().locatedBy(Locators.gridRowCheckbox(fileName)).find(getDriver());
        }
        catch (NoSuchElementException nse)
        {
            throw new AssertionError("File not found: " + fileName, nse);
        }

        // Check the box if it is not checked and should be or if if it is checked and it should not be.
        if (checkbox.isChecked() != checkTheBox)
        {
            scrollIntoView(checkbox.getComponentElement());
            doAndWaitForPageSignal(() -> checkbox.set(checkTheBox), IMPORT_SIGNAL_NAME);
        }
    }

    @LogMethod(quiet = true)
    public void uncheckFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        checkFileBrowserFileCheckbox(fileName, false);
    }

    public boolean fileIsPresent(String nodeIdEndsWith)
    {
        waitForFileGridReady();
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);
        return isElementPresent(targetFile);
    }

    //In case desired element is not present due to infinite scrolling
    private void scrollToGridRow(String nodeIdEndsWith)
    {
        Locator lastRowLoc = Locators.gridRow().last();
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);

        waitForFileGridReady();
        waitForElement(lastRowLoc);

        String previousLastItemText = null;
        String currentLastItemText = null;
        while (!isElementPresent(targetFile) && (currentLastItemText == null || !currentLastItemText.equals(previousLastItemText)))
        {
            try
            {
                WebElement lastRow = lastRowLoc.findElementOrNull(getDriver());
                if (lastRow == null)
                    return;
                scrollIntoView(lastRowLoc);
                previousLastItemText = currentLastItemText;
                currentLastItemText = lastRowLoc.findElement(getDriver()).getAttribute("data-recordid");
            }
            catch (StaleElementReferenceException ignore) {}
        }
    }

    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    public void renameFile(String currentName, String newName)
    {
        selectFileBrowserItem(currentName);
        clickFileBrowserButton(BrowserAction.RENAME);
        Window renameWindow = Window(getDriver()).withTitle("Rename").waitFor();
        setFormElement(Locator.name("renameText-inputEl").findElement(renameWindow), newName);
        renameWindow.clickButton("Rename", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForElement(fileGridCell.withText(newName));
    }

    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.MOVE);
        Window moveWindow = Window(getDriver()).withTitle("Choose Destination").waitFor();
        //NOTE:  this doesn't yet support nested folders
        WebElement folder = Locator.tagWithClass("span", "x4-tree-node-text").withText(destinationPath).waitForElement(moveWindow, 1000);
        shortWait().until(LabKeyExpectedConditions.animationIsDone(folder));
        sleep(500);
        folder.click();
        moveWindow.clickButton("Move", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        waitForElementToDisappear(fileGridCell.withText(fileName));
    }

    public void deleteFile(String fileName)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.DELETE);
        Window(getDriver()).withTitle("Delete Files").waitFor()
                .clickButton("Yes", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForElementToDisappear(fileGridCell.withText(fileName));
    }

    public void deleteAll()
    {
        selectFileBrowserRoot();
        List<Checkbox> checkboxes = Ext4Checkbox().locatedBy(Locators.gridRowCheckbox()).findAll(getDriver());
        if (!checkboxes.isEmpty())
        {
            for (Checkbox checkbox : checkboxes)
            {
                scrollIntoView(checkbox.getComponentElement());
                checkbox.check();
            }
            deleteSelectedFiles();
        }
    }

    public File downloadSelectedFiles()
    {
        return doAndWaitForDownload(() -> clickFileBrowserButton(BrowserAction.DOWNLOAD));
    }

    public void deleteSelectedFiles()
    {
        doAndWaitForFileListRefresh(() ->
        {
            clickFileBrowserButton(BrowserAction.DELETE);
            Window(getDriver()).withTitle("Delete Files").waitFor()
                    .clickButton("Yes", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        });
    }

    public void refreshFileList()
    {
        doAndWaitForFileListRefresh(() ->
                clickFileBrowserButton(BrowserAction.REFRESH));
    }

    public List<String> getFileList()
    {
        return getTexts(Locators.gridRow().childTag("td").position(3).findElements(getDriver()));
    }

    public void createFolder(String folderName)
    {
        clickFileBrowserButton(BrowserAction.NEW_FOLDER);
        setFormElement(Locator.name("folderName"), folderName);
        clickButton("Submit", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForElement(fileGridCell.withText(folderName));
    }

    public void addToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        assertElementNotPresent(toolbarShownLocator);
        click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void removeToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        assertElementPresent(toolbarShownLocator);
        click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void unhideGridColumn(String columnId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td//*[contains(@class, 'x4-grid-checkcolumn-checked')]";               // first column is Hidden checkbox
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + columnId + "']").append(checkboxSelectedXpath);

        assertElementPresent(toolbarShownLocator);
        click(Locator.xpath("//tr[@data-recordid='" + columnId + "']").append(checkboxXpath));
    }

    public void hideGridColumn(String columnId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + columnId + "']").append(checkboxSelectedXpath);

        assertElementNotPresent(toolbarShownLocator);
        click(Locator.xpath("//tr[@data-recordid='" + columnId + "']").append(checkboxXpath));
    }

    public void goToConfigureButtonsTab()
    {
        if (Window(getDriver()).withTitle("Manage File Browser Configuration").findOrNull() == null)
            goToAdminMenu();

        _ext4Helper.clickExt4Tab("Toolbar and Grid Settings");
        waitForText("Configure Toolbar Options");
    }

    public void goToAdminMenu()
    {
        clickFileBrowserButton(BrowserAction.ADMIN);
        Window(getDriver()).withTitle("Manage File Browser Configuration").waitFor();
    }

    public DomainDesignerPage goToEditProperties()
    {
        goToAdminMenu();
        Window window = Window.Window(getDriver()).withTitle("Manage File Browser Configuration").waitFor();
        Ext4Checkbox().locatedBy(Locator.id("importAction-inputEl")).waitFor(window).check();

        waitAndClick(Ext4Helper.Locators.ext4Tab("File Properties"));
        RadioButton().withLabel("Use Custom File Properties").find(window).check();
        window.clickButton("edit properties");
        return new DomainDesignerPage(getDriver());
    }

    public Window clickImportData()
    {
        doAndWaitForPageSignal(() -> clickFileBrowserButton(BrowserAction.IMPORT_DATA), IMPORT_SIGNAL_NAME);
        return Window(getDriver()).withTitle("Import Data").waitFor();
    }

    public void selectImportDataAction(@LoggedParam String actionName)
    {
        Window importWindow = clickImportData();
        RadioButton actionRadioButton = RadioButton().withLabelContaining(actionName).find(importWindow);
        actionRadioButton.check();
        if (!actionRadioButton.isSelected())
        {
            scrollIntoView(actionRadioButton.getComponentElement(), true);
            actionRadioButton.check();
            assertTrue("Failed to select action: " + actionName, actionRadioButton.isSelected());
        }
        importWindow.clickButton("Import");
    }

    /** If the upload panel isn't visible, click the "Upload Files" button in the toolbar. */
    public void openUploadPanel()
    {
        Locator.XPathLocator uploadPanel = Locator.tagWithClass("div", "upload-files-panel").notHidden();
        waitForElement(BrowserAction.UPLOAD.button());
        if (isElementPresent(uploadPanel))
        {
            log("Upload panel visible");
        }
        else
        {
            log("Opening upload panel...");
            click(BrowserAction.UPLOAD.button());
            WebElement uploadPanelEl = waitForElement(uploadPanel, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            shortWait().until(LabKeyExpectedConditions.animationIsDone(uploadPanelEl));
            fireEvent(BrowserAction.UPLOAD.button(), BaseWebDriverTest.SeleniumEvent.mouseout); // Dismiss qtip
        }
    }

    public void uploadFile(File file)
    {
        uploadFile(file, null, null, false);
    }

    @LogMethod
    public void uploadFile(@LoggedParam final File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties, boolean replace)
    {
        int initialCount = waitForFileGridReady();

        openUploadPanel();

        waitFor(() -> getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).equals(""),
                "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.css(".single-upload-panel input:last-of-type[type=file]"), file);
        waitFor(() -> getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).contains(file.getName()),
                "Upload field was not set to '" + file.getName() + "'.", WAIT_FOR_JAVASCRIPT);

        if (description != null)
            setFormElement(Locator.name("description"), description);

        String signalValue = doAndWaitForPageSignal(() -> { // Don't wait for full file list refresh yet, may need to set file properties
            shortWait().until(ExpectedConditions.invisibilityOfElementLocated(Locator.byClass("x4-tip"))); // tooltip sometimes blocks upload button
            clickButton("Upload", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

            if (replace)
            {
                Window confirmation = Window(getDriver()).withTitle("File Conflict:").waitFor();
                assertTrue("Unexpected confirmation message.", confirmation.getBody().contains("Would you like to replace it?"));
                confirmation.clickButton("Yes", true);
            }
        }, FILE_LIST_SIGNAL_NAME, uploadWait(file));
        int fileCount = Integer.parseInt(signalValue);
        assertEquals("Wrong number of files after upload", initialCount + (replace ? 0 : 1), fileCount);
        Locator uploadedFile = fileGridCell.withText(file.getName());

        if (description != null)
            waitForElement(fileGridCell.withText(description));

        if (fileProperties != null && fileProperties.size() > 0)
        {
            Window propWindow = Window(getDriver()).withTitle("Extended File Properties").waitFor();
            waitForText("File (1 of ");
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _ext4Helper.selectComboBoxItem(prop.getName(), Ext4Helper.TextMatchTechnique.CONTAINS, prop.getValue());
                else
                    setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            doAndWaitForElementToRefresh(() -> propWindow.clickButton("Save", true), uploadedFile, shortWait());
            _ext4Helper.waitForMaskToDisappear();

            for (FileBrowserExtendedProperty prop : fileProperties)
                waitForElement(fileGridCell.withText(prop.getValue()));
        }

        // verify that the description field is empty
        assertEquals("Description didn't clear after upload", "", getFormElement(Locator.name("description")));
    }

    /**
     *
     * @param file
     */
    @Override
    @LogMethod
    public void dragAndDropFileInDropZone(@LoggedParam File file)
    {
        waitForFileGridReady();

        //Offsets for the drop zone
        int offsetX = 0;
        int offsetY = 0;

        //Min version of the JS script - creates the dataTransfer object
        String JS_DROP_FILES = "var c=arguments,b=c[0],k=c[1];c=c[2];for(var d=b.ownerDocument||document,l=0;;){var e=b.getBoundingClientRect(),g=e.left+(k||e.width/2),h=e.top+(c||e.height/2),f=d.elementFromPoint(g,h);if(f&&b.contains(f))break;if(1<++l)throw b=Error('Element not interactable'),b.code=15,b;}var a=d.createElement('INPUT');a.setAttribute('type','file');a.setAttribute('multiple','');a.setAttribute('style','position:fixed;z-index:2147483647;left:0;top:0;');a.onchange=function(b){a.parentElement.removeChild(a);b.stopPropagation();var c={constructor:DataTransfer,effectAllowed:'all',dropEffect:'none',types:['Files'],files:a.files,setData:function(){},getData:function(){},clearData:function(){},setDragImage:function(){}};window.DataTransferItemList&&(c.items=Object.setPrototypeOf(Array.prototype.map.call(a.files,function(a){return{constructor:DataTransferItem,kind:'file',type:a.type,getAsFile:function(){return a},getAsString:function(b){var c=new FileReader;c.onload=function(a){b(a.target.result)};c.readAsText(a)}}}),{constructor:DataTransferItemList,add:function(){},clear:function(){},remove:function(){}}));['dragenter','dragover','drop'].forEach(function(a){var b=d.createEvent('DragEvent');b.initMouseEvent(a,!0,!0,d.defaultView,0,0,0,g,h,!1,!1,!1,!1,0,null);Object.setPrototypeOf(b,null);b.dataTransfer=c;Object.setPrototypeOf(b,DragEvent.prototype);f.dispatchEvent(b)})};d.documentElement.appendChild(a);a.getBoundingClientRect();return a;";

        //Execute the script to make the drop zone visible.
        executeScript("LABKEY.internal.FileDrop.showDropzones()");

        WebElement dropzone = Locator.tagWithClass("div", "dropzone").findElement(getDriver());
        WebElement input = (WebElement) executeScript(JS_DROP_FILES, dropzone, offsetX, offsetY);

        //setting the input
        input.sendKeys(file.getAbsolutePath());
    }

    @LogMethod (quiet = true)
    public void dragDropUpload(@LoggedParam File file)
    {
        dragDropUpload(file, file.getName());
    }

    @LogMethod (quiet = true)
    public void dragDropUpload(@LoggedParam File file, @LoggedParam String expectedFileName)
    {
        doAndWaitForFileListRefresh(() -> dragAndDropFileInDropZone(file));
        waitForElement(fileGridCell.withText(expectedFileName));
    }

    public void importFile(String filePath, String importAction)
    {
        selectFileBrowserItem(filePath);
        selectImportDataAction(importAction);
    }

    @LogMethod (quiet = true)
    public void clickFileBrowserButton(@LoggedParam BrowserAction action)
    {
        waitForFileGridReady();
        WebElement button = action.findButton(getDriver());
        if (button.isDisplayed())
        {
            waitFor(() -> !button.getAttribute("class").contains("disabled"), "Button not enabled: " + action, WAIT_FOR_JAVASCRIPT);
            clickAndWait(button, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
        }
        else
            clickFileBrowserButtonOverflow(action);
    }

    private void clickFileBrowserButtonOverflow(BrowserAction action)
    {
        Locator overflowMenuButton = Locator.css("div.fbrowser > div > a.x4-box-menu-after");
        Locator menuItem = Locator.css("a.x4-menu-item-link").withText(action._buttonText);

        click(overflowMenuButton);
        waitAndClick(WAIT_FOR_JAVASCRIPT, menuItem, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
    }

    public List<WebElement> findBrowserButtons()
    {
        waitForFileGridReady();
        return Locator.css(".fbrowser > .x4-toolbar a.x4-btn[data-qtip]").findElements(getDriver());
    }

    public List<BrowserAction> getAvailableBrowserActions()
    {
        List<BrowserAction> actions = new ArrayList<>();
        List<WebElement> buttons = findBrowserButtons();

        for (WebElement button : buttons)
        {
            String cssClassString = button.getAttribute("class");
            String[] cssClasses = cssClassString.split("[ ]+");
            BrowserAction action = null;
            for (int i = 0; action == null && i < cssClasses.length; i++)
            {
                action = BrowserAction.getActionFromButtonClass(cssClasses[i]);
            }
            if (action == null)
                throw new IllegalStateException("No button found for unrecognized action: " + button.getAttribute("data-qtip"));
            actions.add(action);
        }

        return actions;
    }

    public enum BrowserAction
    {
        FOLDER_TREE("sitemap", "Toggle Folder Tree", "folderTreeToggle"),
        UP("arrow-up", "Parent Folder", "parentFolder"),
        REFRESH("refresh", "Refresh", "refresh"),
        NEW_FOLDER("folder", "Create Folder", "createDirectory"),
        DOWNLOAD("download", "Download", "download"),
        DELETE("trash-o", "Delete", "deletePath"),
        RENAME("pencil", "Rename", "renamePath"),
        MOVE("sign-out", "Move", "movePath"),
        EDIT_PROPERTIES("pencil", "Edit Properties", "editFileProps"),
        UPLOAD("file", "Upload Files", "upload"),
        IMPORT_DATA("database", "Import Data", "importData"),
        EMAIL_SETTINGS("envelope", "Email Preferences", "emailPreferences"),
        AUDIT_HISTORY("users", "Audit History", "auditLog", true),
        ADMIN("cog", "Admin", "customize"),
        CREATE_RUN("sitemap", "Create Run", "createRun");

        private String _iconName;
        private String _buttonText;
        private String _extId; // from Browser.js
        private boolean _triggersPageLoad;

        BrowserAction(String iconName, String buttonText, String extId, boolean triggersPageLoad)
        {
            _iconName = iconName;
            _buttonText = buttonText;
            _extId = extId;
            _triggersPageLoad = triggersPageLoad;
        }

        BrowserAction(String iconName, String buttonText, String extId)
        {
            this(iconName, buttonText, extId, false);
        }

        private static BrowserAction getActionFromButtonClass(String cssClass)
        {
            for (BrowserAction a : BrowserAction.values())
            {
                if (a.buttonCls().equals(cssClass))
                    return a;
            }
            return null;
        }

        public String toString()
        {
            return _buttonText;
        }

        public WebElement findButton(final SearchContext context)
        {
            return Locator.css("." + buttonCls()).findElement(context);
        }

        private String buttonCls()
        {
            return _extId + "Btn";
        }

        public Locator button()
        {
            return Locator.tagWithClass("a", buttonCls());
        }

        public Locator getButtonIconLocator()
        {
            return Locator.css(".fa-" + _iconName);
        }

        public Locator getButtonTextLocator()
        {
            return button().containing(_buttonText);
        }
    }

    /**
     * Waits for file grid to be ready for use
     * @return Count of files in selected folder
     */
    public int waitForFileGridReady()
    {
        waitForElement(org.labkey.test.Locators.pageSignal(IMPORT_SIGNAL_NAME));
        String signalValue = waitForElement(org.labkey.test.Locators.pageSignal(FILE_LIST_SIGNAL_NAME)).getAttribute("value");
        waitForGrid();
        return Integer.parseInt(signalValue);
    }

    public void openFolderTree()
    {
        Locator collapsedTreePanel = Locator.css("div.fbrowser .treenav-panel.x4-collapsed");
        shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css("div.fbrowser .treenav-panel")));
        if (isElementPresent(collapsedTreePanel))
        {
            WebElement rootNode = Locator.css("div.fbrowser .treenav-panel tr[data-recordindex = '0']").findElement(getDriver());
            clickFileBrowserButton(BrowserAction.FOLDER_TREE);
            WebElement browser = Locators.fBrowser.findElement(getDriver());
            new Actions(getDriver()).moveToElement(browser, 0, 0).perform(); // Dismiss tooltip
            waitForElementToDisappear(collapsedTreePanel);
            shortWait().until(ExpectedConditions.stalenessOf(rootNode));
            waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation
        }
    }

    public static abstract class Locators
    {
        static Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
        static Locator.XPathLocator folderTree = fBrowser.append(Locator.tagWithClass("div", "treenav-panel").withoutClass("x4-collapsed"));
        static Locator.XPathLocator collapsedFolderTree = fBrowser.append(Locator.tagWithClass("div", "treenav-panel").withClass("x4-collapsed"));

        public static Locator.XPathLocator gridRowCheckbox(String fileName, boolean checkForSelected)
        {
            return Locator.xpath("//tr[contains(@class, '" + (checkForSelected ? "x4-grid-row-selected" : "x4-grid-row") + "') and ./td//span[text()='" + fileName + "']]//div[@class='x4-grid-row-checker']");
        }

        public static Locator.XPathLocator gridRowCheckbox(String fileName)
        {
            return gridRowWithNodeId(fileName).append(Locator.tagWithClass("div", "x4-grid-row-checker"));
        }

        public static Locator.XPathLocator gridRowCheckbox()
        {
            return gridRow().append(Locator.tagWithClass("div", "x4-grid-row-checker"));
        }

        public static Locator.XPathLocator gridRow()
        {
            return Locator.tag("tr")
                    .withClass("x4-grid-data-row")
                    .attributeStartsWith("id", "gridview");
        }

        public static Locator.XPathLocator gridRow(String fileName)
        {
            return gridRowWithNodeId("/" + fileName);
        }

        public static Locator.XPathLocator gridRowWithNodeId(String nodeIdEndsWith)
        {
            return gridRow().attributeEndsWith("data-recordid", nodeIdEndsWith);
        }

        public static Locator.XPathLocator treeRow(Integer recordIndex)
        {
            return Locators.folderTree.append(Locator.tagWithAttribute("tr", "data-recordindex", recordIndex.toString()));
        }

        public static Locator.XPathLocator selectedTreeRow(Integer recordIndex)
        {
            return Locators.folderTree.append(Locator.tagWithAttribute("tr", "data-recordindex", recordIndex.toString()).withClass("x4-grid-row-selected"));
        }
    }
}
