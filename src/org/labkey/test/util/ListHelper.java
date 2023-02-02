/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.Locator.tag;
import static org.labkey.test.util.DataRegionTable.DataRegion;

public class ListHelper extends LabKeySiteWrapper
{
    public static final String IMPORT_ERROR_SIGNAL = "importFailureSignal"; // See query/import.jsp
    private final WrapsDriver _wrapsDriver;

    public ListHelper(WrapsDriver wrapsDriver)
    {
        _wrapsDriver = wrapsDriver;
    }

    public ListHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _wrapsDriver.getWrappedDriver();
    }

    public void uploadData(String listData)
    {
        clickImportData().setText(listData).submit();
    }

    @LogMethod
    public void importDataFromFile(@LoggedParam File inputFile)
    {
        importDataFromFile(inputFile, BaseWebDriverTest.WAIT_FOR_PAGE * 5);
    }

    @LogMethod
    public void importDataFromFile(@LoggedParam File inputFile, int wait)
    {
        clickImportData();
        chooseFileUpload();
        setFormElement(Locator.name("file"), inputFile);
        clickButton("Submit", wait);
    }

    public void chooseFileUpload()
    {
        click(Locator.tagWithClass("h3", "panel-title").containing("Upload file"));
    }

    public void chooseCopyPasteText()
    {
        click(Locator.tagWithClass("h3", "panel-title").containing("Copy/paste text"));
    }


    public void checkIndexFileAttachments(boolean index)
    {
        Locator indexFileAttachmentsCheckbox = Locator.checkboxByLabel("Index file attachments", false);
        if (index)
            checkCheckbox(indexFileAttachmentsCheckbox);
        else
            uncheckCheckbox(indexFileAttachmentsCheckbox);
    }

    /**
     * From the list data grid, insert a new entry into the current list
     *
     * @param data key = the the name of the field, value = the value to enter in that field
     */

    public void insertNewRow(Map<String, ?> data)
    {
        insertNewRow(data, true);
    }

    public void insertNewRow(Map<String, ?> data, boolean validateText)
    {
        DataRegionTable list = new DataRegionTable("query", getDriver());
        list.clickInsertNewRow();
        setRowData(data, validateText);
    }

    protected void setRowData(Map<String, ?> data, boolean validateText)
    {
        for (String key : data.keySet())
        {
            WebElement field = waitForElement(Locator.name("quf_" + key));
            String inputType = field.getAttribute("type");
            Object value = data.get(key);
            if (value instanceof File)
                setFormElement(field, (File) value);
            else if (value instanceof Boolean)
                setCheckbox(field, (Boolean) value);
            else if (value instanceof OptionSelect.SelectOption)
                new OptionSelect<>(field).selectOption((OptionSelect.SelectOption)value);
            else
            {
                String strVal = String.valueOf(value);
                switch (inputType)
                {
                    case "file":
                        setFormElement(field, new File(strVal));
                        break;
                    case "checkbox":
                        setCheckbox(field, strVal.equalsIgnoreCase("true"));
                        break;
                    default:
                        setFormElement(field, strVal);
                }
            }
        }
        clickButton("Submit");

        if (validateText)
        {
            assertTextPresent(String.valueOf(data.values().iterator().next()));  //make sure some text from the map is present
        }
    }

    /**
     * From the list data grid, edit an existing row
     *
     * @param id the row number (1 based)
     * @deprecated use {@link DataRegionTable#updateRow(String, Map)}
     */
    @Deprecated
    public void updateRow(int id, Map<String, String> data)
    {
        updateRow(id, data, true);
    }

    /**
     *
     * @deprecated use {@link DataRegionTable#updateRow(String, Map, boolean)}
     */
    @Deprecated
    public void updateRow(int id, Map<String, String> data, boolean validateText)
    {
        DataRegionTable dr = new DataRegionTable("query", getDriver());
        clickAndWait(dr.updateLink(id - 1));
        setRowData(data, validateText);
    }

    /**
     * Starting at the grid view of a list, delete it
     */
    public void deleteList()
    {
        String url = getCurrentRelativeURL().replace("grid.view", "deleteListDefinition.view");
        beginAt(url);
        clickButton("Confirm Delete");
    }

    /**
     * Starting at the grid view of a list, confirm dependencies, and delete it
     */
    public void deleteList(String confirmText)
    {
        String url = getCurrentRelativeURL().replace("grid.view", "deleteListDefinition.view");
        beginAt(url);
        assertTextPresent(confirmText);
        clickButton("Confirm Delete");
    }

    @LogMethod
    public void createList(String containerPath, @LoggedParam String listName, ListColumnType listKeyType, String listKeyName, FieldDefinition... cols)
    {
        beginCreateList(containerPath, listName);
        createListHelper(listKeyType, listKeyName, cols);
    }

    private void createListHelper(ListColumnType listKeyType, String listKeyName, FieldDefinition... cols)
    {
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());
        DomainFormPanel fieldsPanel;
        if (listKeyType == ListColumnType.AutoInteger)
        {
            fieldsPanel = listDefinitionPage.manuallyDefineFieldsWithAutoIncrementingKey(listKeyName);
        }
        else
        {
            fieldsPanel = listDefinitionPage.manuallyDefineFieldsWithKey(new FieldDefinition(listKeyName, listKeyType.toNew()));
        }
        for (FieldDefinition col : cols)
        {
            fieldsPanel.addField(col);
        }
        listDefinitionPage.clickSave();
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname));
    }

    // initial "create list" steps common to both manual and import from file scenarios
    public EditListDefinitionPage beginCreateList(String containerPath, String listName)
    {
        beginAt(WebTestHelper.buildURL("project", containerPath, "begin"));
        return beginCreateListHelper(listName);
    }

    private EditListDefinitionPage beginCreateListHelper(String listName)
    {
        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        waitAndClickAndWait(Locator.linkWithText("manage lists"));

        log("Add List");
        clickButton("Create New List");
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());
        listDefinitionPage.setName(listName);
        return listDefinitionPage;
    }

    public void createListFromFile(String containerPath, String listName, File inputFile)
    {
        inferFieldsFromFile(containerPath, listName, inputFile)
                .clickSave(); // assumes we intend to import from file
    }

    public EditListDefinitionPage inferFieldsFromFile(String containerPath, String listName, File inputFile)
    {
        EditListDefinitionPage listEditPage = beginCreateList(containerPath, listName);
        listEditPage.getFieldsPanel()
                .setInferFieldFile(inputFile);

        // assumes we intend to key on auto-integer
        DomainFieldRow keyRow = listEditPage.getFieldsPanel().getField("Key");
        if (keyRow != null)
            keyRow.clickRemoveField(false);
        listEditPage.selectAutoIntegerKeyField();
        return listEditPage;
    }

    /**
     * Import a list archive to a target folder
     * @param folderName target folder
     * @param inputFile Full path/filename to list archive
     */
    public void importListArchive(String folderName, String inputFile)
    {
        importListArchive(folderName, new File(inputFile));
    }

    public void importListArchive(String folderName, File inputFile)
    {
        clickFolder(folderName);
        importListArchive(inputFile);
    }

    public void importListArchive(File inputFile)
    {
        assertTrue("Unable to locate input file: " + inputFile, inputFile.exists());

        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        goToManageLists().importListArchive(inputFile);
        assertElementNotPresent(Locators.labkeyError);
    }

    public ImportDataPage clickImportData()
    {
        Optional<WebElement> importButton = Locator.lkButton("Import Data").findOptionalElement(getDriver());
        if(importButton.isPresent())
        {
            // Probably at list-editListDefinition after creating a list
            clickAndWait(importButton.get());
            return new ImportDataPage(getDriver());
        }
        else
        {
            // Importing from list data region
            return DataRegionTable.DataRegion(getDriver()).find()
                    .clickImportBulkData();
        }
    }

    public void bulkImportData(String listData)
    {
        clickImportData()
                .setText(listData)
                .submit();
    }

    public EditListDefinitionPage goToEditDesign(String listName)
    {
        goToList(listName);
        clickAndWait(Locator.lkButton("Design"));
        return new EditListDefinitionPage(getDriver());
    }

    public void goToList(String listName)
    {
        if (!isElementPresent(Locators.bodyTitle("Available Lists")))
        {
            goToManageLists();
        }
        clickAndWait(Locator.linkWithText(listName));
    }

    public void beginAtList(String projectName, String listName)
    {
        beginAt("/query/" + EscapeUtil.encode(projectName) + "/executeQuery.view?schemaName=lists&query.queryName=" + listName);
    }

    public void verifyListData(List<FieldDefinition> columns, String[][] data, DeferredErrorCollector checker)
    {
        final DataRegionTable dataRegion = DataRegion(getDriver()).withName("query").find();
        for (int r = 0; r < data.length; r++)
        {
            Map<String, String> row = dataRegion.getRowDataAsMap(r);
            for (int c = 0; c < columns.size(); c++)
            {
                FieldDefinition column = columns.get(c);
                checker.verifyEquals(String.format("Value for column %s in row %d not as expected", column.getName(), r), data[r][c], row.get(column.getName()));
            }
        }
    }

    /**
     * @deprecated Use {@link ColumnType}
     */
    @Deprecated
    public enum ListColumnType
    {
        MultiLine("Multi-Line Text", ColumnType.MultiLine),
        Integer("Integer", ColumnType.Integer),
        String("Text (String)", ColumnType.String),
        Subject("Subject/Participant (String)", ColumnType.Subject),
        DateAndTime("Date Time", ColumnType.DateAndTime),
        Boolean("Boolean", ColumnType.Boolean),
        Decimal("Decimal", ColumnType.Decimal),
        File("File", ColumnType.File),
        AutoInteger("Auto-Increment Integer", null),
        Flag("Flag (String)", ColumnType.Flag),
        Attachment("Attachment", ColumnType.Attachment),
        User("User", ColumnType.User);

        private final String _description;
        private final ColumnType _newType;

        ListColumnType(String description, ColumnType newType)
        {
            _description = description;
            _newType = newType;
        }

        public String toString()
        {
            return _description;
        }

        public ColumnType toNew()
        {
            if (_newType == null)
            {
                throw new IllegalArgumentException("Not a valid column type: " + name());
            }
            return _newType;
        }

        public static ListColumnType fromNew(@NotNull ColumnType newType)
        {
            for (ListColumnType thisType : values())
            {
                if (newType == thisType.toNew())
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + newType);
        }
    }

    /**
     * @deprecated Use {@link FieldDefinition}
     */
    @Deprecated
    public static class ListColumn extends FieldDefinition
    {
        public ListColumn(String name, String label, ListColumnType type, String description)
        {
            super(name, type.toNew());
            setLabel(label);
            setDescription(description);
        }

        public ListColumn(String name, String label, ListColumnType type)
        {
            this(name, label, type, null);
        }
    }
}
