/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.query;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.data.TestDataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebTestHelper.buildURL;


@Category({Daily.class})
public class QueryLookupTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "QueryLookupTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String LIST_NAME = "l&ist q";

    private static final FieldInfo NAME_COLUMN = FieldInfo.random("Key", FieldDefinition.ColumnType.String, DomainUtils.DomainKind.VarList);
    private static final FieldInfo TSHIRT_COLUMN = FieldInfo.random("TShirt", FieldDefinition.ColumnType.String, DomainUtils.DomainKind.VarList);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        QueryLookupTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, null);

        // create a list
        var dgen = new VarListDefinition(LIST_NAME)
                .setFields(List.of(NAME_COLUMN.getFieldDefinition(), TSHIRT_COLUMN.getFieldDefinition()))
                .create(createDefaultConnection(), PROJECT_NAME);
        dgen.withGeneratedRows(10)
                .insertRows();
    }

    // Issue 49511 Setting lookup to custom query shows "Error: Lookup target table does not exist."
    @Test
    public void testLookupToQueryColumn() throws Exception
    {
        var insertedRows = executeSelectRowCommand("lists", LIST_NAME).getRows();
        var itemNames = insertedRows.stream().map(a-> a.get(NAME_COLUMN.getName()).toString()).toList();
        String secondList = "secondList";

        // create a query from LIST_NAME list, with a key defined in the query xml
        String queryName = "query from list";
        String querySql = """
                SELECT [list_name].[name_column] AS Name,
                [list_name].[tshirt_column] AS TShirt
                FROM [list_name]
                """.replace("[list_name]", EscapeUtil.getSqlQuotedValue(LIST_NAME))
                .replace("[name_column]", EscapeUtil.getSqlQuotedValue(NAME_COLUMN.getName()))
                .replace("[tshirt_column]", EscapeUtil.getSqlQuotedValue(TSHIRT_COLUMN.getName()));
        String queryXml = """
                <tables xmlns="http://labkey.org/data/xml">
                  <table tableName="[query_name]" tableDbType="NOT_IN_DB">
                    <columns>
                      <column columnName="Name">
                        <isKeyField>true</isKeyField>
                      </column>
                    </columns>
                  </table>
                </tables>
                """.replace("[query_name]", EscapeUtil.getMarkupEscapedValue(queryName));
        // create a query on the list
        goToSchemaBrowser();
        createQuery(getProjectName(), queryName, "lists", querySql, queryXml, false, queryName);

        // now create another list, with a lookup to the custom query
        new IntListDefinition(secondList, "Key")
                .setFields(List.of(NAME_COLUMN.getFieldDefinition(),
                        new FieldDefinition("lookup", new FieldDefinition.StringLookup(getProjectName(), "lists", queryName))))
                .create(createDefaultConnection(), PROJECT_NAME);

        // Issue 53846: Character limit on query property limit throws unhandled exception
        String queryURL = buildURL("query", getProjectName(), "begin", Map.of("schemaName", "lists"));
        beginAt(queryURL);
        editQueryProperties("lists", queryName);
        setFormElement(Locator.tagWithName("textarea", "description"), TestDataGenerator.randomString(300));
        clickButton("Save");

        // insert data into the list
        goToManageLists();
        waitAndClickAndWait(Locator.linkWithText(secondList));
        var importPage = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor()
                .clickImportBulkData();
        List<Map<String, Object>> importData = new ArrayList<>();
        int textIndex = 0;
        for (String lookupValue : itemNames)
        {
            String nameVal = String.format("text-%d", textIndex);
            importData.add(Map.of("Name", nameVal, "lookup", lookupValue));
            textIndex++;
        }
        importPage.setText(TestDataUtils.tsvStringFromRowMaps(importData, List.of("Name", "lookup"), true));
        importPage.submit();

        // verify the query-list-items resolve here
        var secondListDataRegion = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        var resolvedLookupsInSecondList = secondListDataRegion.getColumnDataAsText("lookup");
        checker().withScreenshot("query lookup values not resolved")
                .wrapAssertion(()-> Assertions.assertThat(resolvedLookupsInSecondList)
                        .as("the expected sample items were not resolved via query lookup")
                        .containsAll(itemNames));

        // navigate to the secondlist edit page and verify the details on the lookup field row are correct
        secondListDataRegion.clickHeaderButtonAndWait("Design");
        var listEditPage = new EditListDefinitionPage(getDriver());
        var lookupFieldRow = listEditPage.getFieldsPanel().getField("Lookup");
        checker().withScreenshot("unexpected lookup details")
                .wrapAssertion(()-> Assertions.assertThat(lookupFieldRow.detailsMessage())
                        .as("query lookup table not resolved")
                        .isEqualTo(String.format("/%s > lists > %s", PROJECT_NAME, queryName)));
        // click the link in the details message, expect to navigate to a view of the query
        clickAndWait(Locator.linkWithText(queryName).findElement(lookupFieldRow));
        assertTextPresent("lists Schema", queryName, PROJECT_NAME);

    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
