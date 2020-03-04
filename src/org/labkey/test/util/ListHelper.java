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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ListHelper extends LabKeySiteWrapper
{
    public static final String IMPORT_ERROR_SIGNAL = "importFailureSignal"; // See query/import.jsp
    private WrapsDriver _wrapsDriver;

    public ListHelper(WrapsDriver wrapsDriver)
    {
        _wrapsDriver = wrapsDriver;
    }

    public ListHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    private boolean NEW_LIST_DESIGNER_ENABLED = false;
    private void enabledNewListDesigner()
    {
        ExperimentalFeaturesHelper.enableExperimentalFeature(createDefaultConnection(true), "experimental-reactlistdesigner");
        NEW_LIST_DESIGNER_ENABLED = true;
    }
    private void disableNewListDesigner()
    {
        ExperimentalFeaturesHelper.disableExperimentalFeature(createDefaultConnection(true), "experimental-reactlistdesigner");
        NEW_LIST_DESIGNER_ENABLED = false;
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _wrapsDriver.getWrappedDriver();
    }

    public void uploadCSVData(String listData)
    {
        clickImportData();
        setFormElement(Locator.id("tsv3"), listData);
        _extHelper.selectComboBoxItem("Format:", "Comma-separated text (csv)");
        submitImportTsv_success();
    }

    public PropertiesEditor getListFieldEditor()
    {
        return PropertiesEditor.PropertiesEditor(getDriver()).withTitle("List Fields").find();
    }

    public void uploadData(String listData)
    {
        clickImportData();
        submitTsvData(listData);
    }

    public void submitTsvData(String listData)
    {
        setFormElement(Locator.id("tsv3"), listData);
        submitImportTsv_success();
    }

    public void submitImportTsv_success()
    {
        clickButton("Submit");
        waitForElement(Locator.css(".labkey-data-region"), 30000);
    }

    // null means any error
    public void submitImportTsv_error(String error)
    {
        doAndWaitForPageSignal(() -> clickButton("Submit", 0),
                IMPORT_ERROR_SIGNAL);
        if (error != null)
        {
            String errors = String.join(", ", getTexts(Locators.labkeyError.findElements(getDriver())));
            assertTrue("Didn't find expected error ['" + error + "'] in [" + errors + "]", errors.contains(error));
        }
    }

    public void submitImportTsv_errors(List<String> errors)
    {
        doAndWaitForPageSignal(() -> clickButton("Submit", 0),
                IMPORT_ERROR_SIGNAL);
        if (errors == null || errors.isEmpty())
            waitForElement(Locator.css(".labkey-error"));
        else
        {
            for (String err : errors)
            {
                waitForElement(Locator.css(".labkey-error").containing(err));
            }
        }
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
        click(Locator.tagWithClass("h3", "panel-title").containing("Upload file"));
        setFormElement(Locator.name("file"), inputFile);
        clickButton("Submit", wait);
    }

    public void checkIndexFileAttachements(boolean index)
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

    @Deprecated // Temporary method for debugging list metadata problems
    @LogMethod
    public RuntimeException dumpListMetadataInfo(NoSuchElementException nse)
    {
        if (nse.getMessage().contains("quf_"))
        {
            Map<String, String> urlParameters = getUrlParameters();
            String schemaName = urlParameters.get("schemaName");
            String queryName = urlParameters.get("query.queryName");
            if (schemaName == null || queryName == null)
                throw nse;

            String containerPath = getCurrentContainerPath();
            String subdir = schemaName + " MetadataError";
            ArtifactCollector artifactCollector = BaseWebDriverTest.getCurrentTest().getArtifactCollector();

            artifactCollector.dumpPageSnapshot("insertPage", subdir, false);
            if ("lists".equals(schemaName))
            {
                try
                {
                    WebElement cancelButton = Locator.lkButton("Cancel").findElement(getDriver());
                    String cancelHref = cancelButton.getAttribute("href");
                    int listId = Integer.parseInt(WebTestHelper.parseUrlQuery(new URL(cancelHref)).get("listId"));

                    // list-editListDefinition.view?listId=11
                    EditListDefinitionPage.beginAt(this, containerPath, listId);
                    waitForElement(Locator.lkButton("Export Fields"));
                    artifactCollector.dumpPageSnapshot("listDefinitionById", subdir, false);
                }
                catch (MalformedURLException | NumberFormatException | NoSuchElementException ignore) { }

                // list-editListDefinition.view?name=People
                EditListDefinitionPage.beginAt(this, containerPath, queryName);
                waitForElement(Locator.lkButton("Export Fields"));
                artifactCollector.dumpPageSnapshot("listDefinitionByName", subdir, false);
            }
            // query-begin.view?#sbh-qdp-%26lists%26People
            beginAt(WebTestHelper.buildURL("query", containerPath, "begin") + "#sbh-qdp-%26" + schemaName + "%26" + queryName);
            waitForAnyElement(Locator.linkWithText("view data"), Locator.byClass("lk-qd-error"));
            artifactCollector.dumpPageSnapshot("schemaBrowser", subdir, false);

            // query-metadataQuery.view?schemaName=lists&query.queryName=People
            beginAt(WebTestHelper.buildURL("query", containerPath, "metadataQuery", Map.of("schemaName", schemaName, "query.queryName", queryName)));
            waitForElement(Locators.pageSignal("propertiesEditorChange"));
            artifactCollector.dumpPageSnapshot("metadataEditor", subdir, false);

            // query-rawTableMetaData.view?schemaName=lists&query.queryName=People
            beginAt(WebTestHelper.buildURL("query", containerPath, "rawTableMetaData", Map.of("schemaName", schemaName, "query.queryName", queryName)));
            artifactCollector.dumpPageSnapshot("rawMetadata", subdir, false);

            // Clear caches
            TestLogger.log("Clear cache: " +
                    WebTestHelper.getHttpResponse(WebTestHelper.buildURL("admin", "memTracker", Map.of("clearCaches", "1"))).getResponseCode());

            // Check schema browser after clearing caches
            beginAt(WebTestHelper.buildURL("query", containerPath, "begin") + "#sbh-qdp-%26" + schemaName + "%26" + queryName);
            waitForAnyElement(Locator.linkWithText("view data"), Locator.byClass("lk-qd-error"));
            artifactCollector.dumpPageSnapshot("schemaBrowserClearedCache", subdir, false);

            return new RuntimeException("Detected possible metadata problem.", nse);
        }
        return nse;
    }

    protected void setRowData(Map<String, ?> data, boolean validateText)
    {
        for (String key : data.keySet())
        {
            WebElement field;
            try
            {
                field = waitForElement(Locator.name("quf_" + key));
            }
            catch (NoSuchElementException nse)
            {
                throw dumpListMetadataInfo(nse);
            }
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
                        setCheckbox(field, strVal.toLowerCase().equals("true"));
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
        clickButton("OK");
    }

    @LogMethod
    public void createListFromTab(String tabName, String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        enabledNewListDesigner();
        beginCreateListFromTab(tabName, listName);
        createListHelper(listKeyType, listKeyName, cols);
    }

    @LogMethod
    public void createList(String containerPath, @LoggedParam String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        enabledNewListDesigner();
        beginCreateList(containerPath, listName);
        createListHelper(listKeyType, listKeyName, cols);
    }

    private void createListHelper(ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());
        DomainFormPanel fieldsPanel = listDefinitionPage.setKeyField(listKeyType, listKeyName);
        for (ListColumn col : cols)
            fieldsPanel.addField(col);

        clickSave();
    }

    public void addField(ListColumn col)
    {
        getListFieldEditor().addField(col);
    }

    private void beginCreateListFromTab(String tabName, String listName)
    {
        clickTab(tabName.replace(" ", ""));
        beginCreateListHelper(listName);
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
        listDefinitionPage.setListName(listName);
        return listDefinitionPage;
    }

    public void createListFromFile(String containerPath, String listName, File inputFile)
    {
        enabledNewListDesigner();
        EditListDefinitionPage listEditPage = beginCreateList(containerPath, listName);
        listEditPage.expandFieldsPanel()
            .setInferFieldFile(inputFile);

        // assumes we intend to key on auto-integer
        DomainFieldRow keyRow = listEditPage.getFieldsPanel().getField("Key");
        if (keyRow != null)
            keyRow.clickRemoveField(false);
        listEditPage.selectAutoIntegerKeyField();

        // assumes we intend to import from file
        listEditPage.clickSave();
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

    public void clickImportData()
    {
        if(isElementPresent(Locator.lkButton("Import Data")))
        {
            // Probably at list-editListDefinition after creating a list
            waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Import Data"), BaseWebDriverTest.WAIT_FOR_PAGE);
        }
        else
        {
            // Importing from list data region
            DataRegionTable list = DataRegionTable.DataRegion(getDriver()).find();
            list.clickImportBulkData();
        }
        waitForElement(Locator.id("tsv3"));
    }

    public void bulkImportData(String listData)
    {
        clickImportData();
        setFormElement(Locator.name("text"), listData);
        submitImportTsv_success();
    }

    public EditListDefinitionPage clickEditDesign()
    {
        disableNewListDesigner();
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Edit Design"), 0);
        waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.id("ff_description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        return new EditListDefinitionPage(getDriver());
    }

    public EditListDefinitionPage goToEditDesign(String listName)
    {
        enabledNewListDesigner();
        goToList(listName);
        clickAndWait(Locator.lkButton("Design"));
        return new EditListDefinitionPage(getDriver());
    }

    public void goToList(String listName)
    {
        // if we are on the Manage List page, click the list name first
        if (isElementPresent(Locators.bodyTitle("Available Lists")))
            clickAndWait(Locator.linkWithText(listName));
    }

    private void clickSave_OLD()
    {
        WebElement saveButton = Locator.lkButton("Save").waitForElement(getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        scrollToTop(); // After clicking save, sometimes the page scrolls so that the project menu is under the mouse
        saveButton.click();
        waitForElement(Locator.lkButton("Edit Design"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.lkButton("Done"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickSave()
    {
        if (!NEW_LIST_DESIGNER_ENABLED)
        {
            clickSave_OLD();
            return;
        }

        // TODO move this to a ListDesignerPage test helper
        clickAndWait(Locator.button("Save").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
    }

    public void clickDeleteList()
    {
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Delete List"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    /**
     * @deprecated Use {@link PropertiesEditor#addField(FieldDefinition)}
     */
    @Deprecated
    @LogMethod(quiet = true)
    public void addField(String areaTitle, @LoggedParam String name, String label, ListColumnType type)
    {
        PropertiesEditor.PropertiesEditor(getDriver()).withTitleContaining(areaTitle).find()
                .addField(new FieldDefinition(name).setLabel(label).setType(type.toNew()));
    }

    public List<String> getColumnNames()
    {
        List<String> columns = new ArrayList<>();

        List<WebElement> nameFields = Locator.xpath("//input[contains(@name, 'ff_name')]").findElements(getDriver());
        for(WebElement webElement : nameFields)
        {
            // If it is not the list name element then add it to the list.
            if (!webElement.getAttribute("name").trim().toLowerCase().equals("ff_name"))
            {
                columns.add(webElement.getAttribute("value"));
            }
        }

        return columns;
    }

    public enum RangeType
    {
        Equals("Equals"), NE("Does Not Equal"), GT("Greater than"), GTE("Greater than or Equals"), LT("Less than"), LTE("Less than or Equals");
        private final String _description;

        RangeType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }

        private FieldDefinition.RangeType toNew()
        {
            for (FieldDefinition.RangeType thisType : FieldDefinition.RangeType.values())
            {
                if (name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + this);
        }
    }

    public enum ListColumnType
    {
        MultiLine("Multi-Line Text"),
        Integer("Integer"),
        String("Text (String)"),
        Subject("Subject/Participant (String)"),
        DateAndTime("Date Time"),
        Boolean("Boolean"),
        Decimal("Decimal"),
        File("File"),
        AutoInteger("Auto-Increment Integer"),
        Flag("Flag (String)"),
        Attachment("Attachment"),
        User("User");

        private final String _description;

        ListColumnType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }

        private FieldDefinition.ColumnType toNew()
        {
            for (FieldDefinition.ColumnType thisType : FieldDefinition.ColumnType.values())
            {
                if (name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + this);
        }

        public static ListColumnType fromNew(FieldDefinition.ColumnType newType)
        {
            for (ListColumnType thisType : values())
            {
                if (newType.name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + newType);
        }
    }

    public static class LookupInfo extends FieldDefinition.LookupInfo
    {
        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            super(folder, schema, table);
        }
    }

    public static class RegExValidator extends FieldDefinition.RegExValidator
    {
        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message, expression);
        }
    }

    public static class RangeValidator extends FieldDefinition.RangeValidator
    {
        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            super(name, description, message, firstType.toNew(), firstRange);
        }

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange, RangeType secondType, String secondRange)
        {
            super(name, description, message, firstType.toNew(), firstRange, secondType.toNew(), secondRange);
        }
    }

    public static class LookUpValidator extends FieldDefinition.LookUpValidator
    {
        public LookUpValidator()
        {
            super();
        }
    }

    public static class ListColumn extends FieldDefinition
    {
        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url, Integer scale)
        {
            super(name);
            setLabel(label);
            setType(type.toNew());
            setDescription(description);
            setFormat(format);
            setLookup(lookup);
            setValidator(validator);
            setURL(url);
            setScale(scale);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url)
        {
            this(name, label, type, description, format, lookup, validator, url, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, LookupInfo lookup)
        {
            this(name, label, type, description, null, lookup, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format)
        {
            this(name, label, type, description, format, null, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description)
        {
            this(name, label, type, description, null, null, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, FieldValidator validator)
        {
            this(name, label, type, description, null, null, validator, null);
        }

        public ListColumn(String name, String label, ListColumnType type)
        {
            this(name, label, type, null, null, null, null, null);
        }

        public ListColumn(String name, ListColumnType type)
        {
            this(name, null, type);
        }
    }

    /**
     * Set of locators for navigating the List Designer page
     */
    public static class DesignerLocators
    {
        public static Locator.XPathLocator maxCheckbox = Locator.xpath("//input[@name='isMaxText']");
        public static Locator.XPathLocator scaleTextbox = Locator.xpath("//input[@name='scale']");
    }
}
