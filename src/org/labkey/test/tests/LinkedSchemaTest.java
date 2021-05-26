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
package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.QueryMetadataEditorPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.SchemaHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test created linked schemas from a source container in a target container.
 *
 * To check the metadata is applied correctly, each letter column has metadata applied
 * at each level that metadata can be applied.  Using this strategy we only need to
 * check the end query instead of at each level.
 *
 * For the LinkedSchemaTestPeople list, metadata should be applied in this order:
 *
 * In the source container:
 * - LinkedSchemaTestPeople list definition
 *   (original title and URL for  P, Q, R, S, T, U, V, W, X, Y, Z)
 * - File metadata xml in linkedschematest/queries/lists/LinkedSchemaTestPeople.query.xml
 *   (overrides title and URL for P, Q, R, S, T, U, V, W, X, Y)
 * - Database metadata xml
 *   (overrides title and URL for P, Q, R, S, T, U, V, W, X)
 *
 * In linked schema target container:
 * - Linked schema tables have FKs and URLs removed, other metadata is intact.
 * - Linked schema template or instance metadata xml
 *   (overrides title and URL for P, Q, R, S, T, U, V, W)
 * - File metadata xml in linkedschematest/queries/&lt;schema&gt;/LinkedSchemaTestPeople.query.xml
 *   (overrides title and URL for P, Q, R, S, T, U, V)
 * - Database metadata xml
 *   (overrides title and URL for P, Q, R, S, T, U)
 *
 * For LinkedSchemaTestQuery query, metadata should be applied in this order:
 *
 * In the source container:
 * - The LinkedSchemaTestPeople metadata as applied above
 * - File metadata xml in linkedschematest/queries/lists/LinkedSchemaTestQuery.query.xml
 *   (overrides title and URL for P, Q, R, S, T) (U, V, W, X, Y, Z should be the same as from LinkedSchemaTestPeople in the source container)
 * - Database metadata xml
 *   (overrides title and URL for P, Q, R, S)
 *   (BUGBUG? Can't override the metadata xml of a file-based query with .query.xml apparently.)
 *
 * In linked schema target container:
 * - Linked schema tables have FKs and URLs removed, other metadata is intact.
 * - Linked schema template or instance metadata xml
 *   (overrides title and URL for P, Q, R)
 * - File metadata xml in linkedschematest/queries/&lt;schema&gt;/LinkedSchemaTestQuery.query.xml
 *   (overrides title and URL for P, Q)
 * - Database metadata xml
 *   (overrides title and URL for P)
 */
@Category({DailyA.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 11)
public class LinkedSchemaTest extends BaseWebDriverTest
{
    private SchemaHelper _schemaHelper = new SchemaHelper(this);
    private static final String PROJECT_NAME = LinkedSchemaTest.class.getSimpleName() + "Project";
    private static final String SOURCE_FOLDER = "SourceFolder";
    private static final String TARGET_FOLDER = "TargetFolder";
    private static final String OTHER_FOLDER = "OtherFolder";
    private static final String STUDY_FOLDER = "StudyFolder"; // Folder used to validate fix for issues 32454 & 32456
    private static final String MOTHER = "Mother";
    private static final String READER_USER = "reader@linkedschema.test";

    public static final String LIST_NAME = "LinkedSchemaTestPeople";
    public static final String LIST_DATA = "Name\tAge\tCrazy\tP\tQ\tR\tS\tT\tU\tV\tW\tX\tY\tZ\n" +
            "Dave\t39\tTrue\tp\tq\tr\ts\tt\tu\tv\tw\tx\ty\tz\n" +
            "Adam\t65\tTrue\tp\tq\tr\ts\tt\tu\tv\tw\tx\ty\tz\n" +
            "Britt\t30\tFalse\tp\tq\tr\ts\tt\tu\tv\tw\tx\ty\tz\n" +
            "Josh\t30\tTrue\tp\tq\tr\ts\tt\tu\tv\tw\tx\ty\tz";

    // List used to validate fix for issues 32454 & 32456
    public static final String STUDY_LIST_NAME = "Consent";
    public static final String STUDY_LIST_PK = "GlobalPid";
    public static final String STUDY_LIST_DATA = STUDY_LIST_PK + "\tStudy\n" +
            "249318596\tStudyA\n" +
            "249320107\tStudyB\n" +
            "249320127\tStudyA";

    // Original list definition title and URL
    public static final String LIST_DEF_TITLE = "Original List";
    public static final String LIST_DEF_URL   = "list_original.view";

    // File-based metadata lives in linkedschematest/queries/lists/LinkedSchemaTestPeople.query.xml
    public static final String LIST_FILE_METADATA_TITLE = "file_metadata List";
    public static final String LIST_FILE_METADATA_URL   = "file_metadata.view";

    // Metadata override applied to the list in the database in source container.
    public static final String LIST_METADATA_OVERRIDE_TITLE = "db_metadata List";
    public static final String LIST_METADATA_OVERRIDE_URL   = "db_metadata.view";
    public static final String LIST_METADATA_OVERRIDE =
            "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
            "  <table tableName=\"" + LIST_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "    <columns>\n" +
            "      <column columnName=\"P\">\n" +
            "        <columnTitle>db_metadata List P</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"Q\">\n" +
            "        <columnTitle>db_metadata List Q</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"R\">\n" +
            "        <columnTitle>db_metadata List R</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"S\">\n" +
            "        <columnTitle>db_metadata List S</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"T\">\n" +
            "        <columnTitle>db_metadata List T</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"U\">\n" +
            "        <columnTitle>db_metadata List U</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"V\">\n" +
            "        <columnTitle>db_metadata List V</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"W\">\n" +
            "        <columnTitle>db_metadata List W</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"X\">\n" +
            "        <columnTitle>db_metadata List X</columnTitle>\n" +
            "        <url>fake/db_metadata.view</url>\n" +
            "      </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>";

    public static final String QUERY_NAME = "LinkedSchemaTestQuery";
    // BUGBUG? Can't apply database metadata to a file-based query.
//    public static final String QUERY_METADATA_OVERRIDE_TITLE = "db_metadata Query";
//    public static final String QUERY_METADATA_OVERRIDE_URL   = "db_metadata.view";
//    public static final String QUERY_METADATA_OVERRIDE =
//            "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
//            "  <table tableName=\"" + QUERY_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
//            "    <columns>\n" +
//            "      <column columnName=\"P\">\n" +
//            "        <columnTitle>db-metadata List P</columnTitle>\n" +
//            "        <url>fake/db_metadata.view</url>\n" +
//            "      </column>\n" +
//            "      <column columnName=\"Q\">\n" +
//            "        <columnTitle>db_metadata Query Q</columnTitle>\n" +
//            "        <url>fake/db_metadata.view</url>\n" +
//            "      </column>\n" +
//            "      <column columnName=\"R\">\n" +
//            "        <columnTitle>db_metadata Query R</columnTitle>\n" +
//            "        <url>fake/db_metadata.view</url>\n" +
//            "      </column>\n" +
//            "      <column columnName=\"S\">\n" +
//            "        <columnTitle>db_metadata Query S</columnTitle>\n" +
//            "        <url>fake/db_metadata.view</url>\n" +
//            "      </column>\n" +
//            "      <column columnName=\"T\">\n" +
//            "        <columnTitle>db_metadata Query T</columnTitle>\n" +
//            "        <url>fake/db_metadata.view</url>\n" +
//            "      </column>\n" +
//            "      <column columnName=\"U\">\n" +
//            "        <columnTitle>db_metadata Query U</columnTitle>\n" +
//            "        <url>fake/db_metadata.view</url>\n" +
//            "      </column>\n" +
//            "    </columns>\n" +
//            "  </table>\n" +
//            "</tables>";

    // A_People linked schema definition and metadata override
    public static final String A_PEOPLE_SCHEMA_NAME = "A_People";
    public static final String A_PEOPLE_METADATA_TITLE = "A_People template List Name";
    public static final String A_PEOPLE_METADATA =
            "<tables xmlns=\"http://labkey.org/data/xml\" xmlns:cv=\"http://labkey.org/data/xml/queryCustomView\">\n" +
            "    <table tableName=\"" + LIST_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <tableUrl>/query/recordDetails.view?schemaName=" + A_PEOPLE_SCHEMA_NAME + "&amp;queryName=" + LIST_NAME + "&amp;keyField=Key&amp;key=${Key}</tableUrl>\n" +
            "        <filters>\n" +
            "          <cv:where>Name LIKE 'A%'</cv:where>\n" +
            "        </filters>\n" +
            "        <columns>\n" +
            "          <column columnName=\"Name\">\n" +
            "            <columnTitle>" + A_PEOPLE_METADATA_TITLE + "</columnTitle>\n" +
            "          </column>\n" +
            "          <column columnName=\"P\">\n" +
            "            <columnTitle>A_People template List P</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"Q\">\n" +
            "            <columnTitle>A_People template List Q</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"R\">\n" +
            "            <columnTitle>A_People template List R</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"S\">\n" +
            "            <columnTitle>A_People template List S</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"T\">\n" +
            "            <columnTitle>A_People template List T</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"U\">\n" +
            "            <columnTitle>A_People template List U</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"V\">\n" +
            "            <columnTitle>A_People template List V</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"W\">\n" +
            "            <columnTitle>A_People template List W</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "        </columns>\n" +
            "    </table>\n" +
            "    <table tableName=\"" + QUERY_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <columns>\n" +
            "          <column columnName=\"P\">\n" +
            "            <columnTitle>A_People template Query P</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"Q\">\n" +
            "            <columnTitle>A_People template Query Q</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "          <column columnName=\"R\">\n" +
            "            <columnTitle>A_People template Query R</columnTitle>\n" +
            "            <url>fake/a_template_metadata.view</url>\n" +
            "          </column>\n" +
            "        </columns>\n" +
            "    </table>\n" +
            "</tables>\n";

    public static final String A_PEOPLE_LIST_METADATA_OVERRIDE =
            "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
            "  <table tableName=\"" + LIST_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "    <columns>\n" +
            "      <column columnName=\"P\">\n" +
            "        <columnTitle>A_People db_metadata List P</columnTitle>\n" +
            "        <url>fake/a_db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"Q\">\n" +
            "        <columnTitle>A_People db_metadata List Q</columnTitle>\n" +
            "        <url>fake/a_db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"R\">\n" +
            "        <columnTitle>A_People db_metadata List R</columnTitle>\n" +
            "        <url>fake/a_db_metadata.view</url>\n" +
            "      </column>\n" +
            "      <column columnName=\"S\">\n" +
            "        <columnTitle>A_People db_metadata List S</columnTitle>\n" +
            "        <url>fake/a_db_metadata.view</url>\n" +
            "      </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>";

    public static final String A_PEOPLE_QUERY_METADATA_OVERRIDE =
            "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
            "  <table tableName=\"" + QUERY_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "    <columns>\n" +
            "      <column columnName=\"P\">\n" +
            "        <columnTitle>A_People db_metadata Query P</columnTitle>\n" +
            "        <url>fake/a_db_metadata.view</url>\n" +
            "      </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>";

    // B_People linked schema definition is in linkedschematest/schemas/BPeopleTemplate.template.xml
    public static final String B_PEOPLE_SCHEMA_NAME = "B_People";
    public static final String B_PEOPLE_TEMPLATE_METADATA_TITLE = "BPeopleTemplate List Name";

    // D_People linked schema definition and metadata override
    public static final String D_PEOPLE_SCHEMA_NAME = "D_People";
    public static final String D_PEOPLE_METADATA_TITLE = "D_PEOPLE_METADATA List Name";
    public static final String D_PEOPLE_METADATA =
            "<dat:tables xmlns:dat=\"http://labkey.org/data/xml\" xmlns:cv=\"http://labkey.org/data/xml/queryCustomView\">\n" +
            "    <dat:table tableName=\"" + LIST_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <!-- disable details url -->\n" +
            "        <dat:tableUrl></dat:tableUrl>\n" +
            "        <dat:filters>\n" +
            "          <cv:where>Name LIKE 'D%'</cv:where>\n" +
            "        </dat:filters>\n" +
            "        <dat:columns>\n" +
            "          <dat:column columnName=\"Name\">\n" +
            "            <dat:columnTitle>" + D_PEOPLE_METADATA_TITLE + "</dat:columnTitle>\n" +
            "          </dat:column>\n" +
            "        </dat:columns>\n" +
            "    </dat:table>\n" +
            "    <dat:table tableName=\"" + QUERY_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <dat:filters>\n" +
            "          <!-- NOTE: where clauses don't get applied to queries yet <cv:where>Age &gt; 30</cv:where>-->\n" +
            "          <cv:filter column=\"Age\" operator=\"gt\" value=\"30\" />\n" +
            "        </dat:filters>\n" +
            "        <dat:columns>\n" +
            "          <dat:column columnName=\"Name\">\n" +
            "            <dat:columnTitle>Crazy " + D_PEOPLE_METADATA_TITLE + "</dat:columnTitle>\n" +
            "          </dat:column>\n" +
            "        </dat:columns>\n" +
            "    </dat:table>\n" +
            "</dat:tables>\n";

    // CommonData schema used to validate fix for issues 32454 & 32456
    public static final String STUDY_SCHEMA_NAME = "CommonData";
    public static final String STUDY_FILTER_METADATA =
            "<tables xmlns=\"http://labkey.org/data/xml\" xmlns:cv=\"http://labkey.org/data/xml/queryCustomView\"> \n" +
                    "<filters name=\"study-filter2\"> \n" +
                    "     <cv:filter column=\"ParticipantId/Study\" operator=\"eq\" value=\"StudyA\"/>\n" +
                    "</filters>\n" +
                    "  <table tableName=\"Demographics\" tableDbType=\"TABLE\">\n" +
                    "    <!-- <filters ref=\"study-filter2\"/> -->\n" +
                    "    <columns>\n" +
                    "      <column columnName=\"ParticipantId\">\n" +
                    "        <fk>\n" +
                    "          <fkDbSchema>lists</fkDbSchema>\n" +
                    "          <fkTable>" + STUDY_LIST_NAME + "</fkTable>\n" +
                    "          <fkColumnName>" + STUDY_LIST_PK + "</fkColumnName>\n" +
                    "          <fkFolderPath>/" + PROJECT_NAME + "/" + STUDY_FOLDER+ "</fkFolderPath>\n" +
                    "        </fk>\n" +
                    "      </column>\n" +
                    "  </columns>\n" +
                    "  </table>\n" +
                    "</tables>";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(false, READER_USER);
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        LinkedSchemaTest initTest = (LinkedSchemaTest)getCurrentTest();

        initTest.setupProject();
        initTest.createList();
        initTest.importListData();
    }

    @Test
    public void basicLinkedSchemaTest()
    {
        createLinkedSchema();
        verifyLinkedSchema();
        deleteLinkedSchema();
    }

    @LogMethod
    void deleteLinkedSchema()
    {
        log("Verify linked schema deletion");
        String containerPath = getProjectName() + "/" + TARGET_FOLDER;
        _schemaHelper.deleteSchema(containerPath, A_PEOPLE_SCHEMA_NAME);
    }

    @Test
    public void lookupTest()
    {
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "BasicLinkedSchema", sourceContainerPath, null, "lists", "NIMHDemographics,NIMHPortions", null);

        //Ensure that all the columns we would expect to come through are coming through
        assertColumnsPresent(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", "SubjectID", "Name", "Family", "Mother", "Father", "Species", "Occupation",
                "MaritalStatus", "CurrentStatus", "Gender", "BirthDate", "Image");
        //Make sure the columns in the source that should be hidden are hidden, then check them in the linked schema
        assertColumnsNotPresent(SOURCE_FOLDER, "lists", "NIMHDemographics", "EntityId", "LastIndexed");
        assertColumnsNotPresent(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", "EntityId", "LastIndexed");

        //Make sure that the lookup columns propagated properly into the linked schema
        assertLookupsWorking(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", true, "Mother", "Father");

        // Linked schemas disallow lookups to other folders outside of the current folder.
        //Change the Mother column lookup to point to the other folder, then ensure that the mother lookup is no longer propagating
        changelistLookup(SOURCE_FOLDER, "NIMHDemographics", MOTHER,
                new ListHelper.LookupInfo("/" + PROJECT_NAME + "/" + OTHER_FOLDER, "lists", "NIMHDemographics")
                        .setTableType(FieldDefinition.ColumnType.Integer));
        assertLookupsWorking(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", true, "Father");
        assertLookupsWorking(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", false, "Mother");

        //Create a query over the table with lookups
        createLinkedSchemaQuery(SOURCE_FOLDER, "lists", "QueryOverLookup", "NIMHDemographics");

        //Create a new linked schema that includes that query, and ensure that it is propagating lookups in the expected manner
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "QueryLinkedSchema", sourceContainerPath, null, "lists", "NIMHDemographics,NIMHPortions,QueryOverLookup", null);
        assertLookupsWorking(TARGET_FOLDER, "QueryLinkedSchema", "QueryOverLookup", true, "Father");
        assertLookupsWorking(TARGET_FOLDER, "QueryLinkedSchema", "QueryOverLookup", false, "Mother");

        //Change the Mother column lookup to point to the query, and then make sure that the table has lookups appropriately.
        changelistLookup(SOURCE_FOLDER, "NIMHDemographics", MOTHER,
                new FieldDefinition.LookupInfo("/" + PROJECT_NAME + "/" + SOURCE_FOLDER, "lists", "QueryOverLookup")
                .setTableType(FieldDefinition.ColumnType.Integer));
        assertLookupsWorking(TARGET_FOLDER, "QueryLinkedSchema", "NIMHDemographics", true, "Mother", "Father");
    }

    @Test
    public void customFilters()
    {
        log("** Creating linked schema filtered by 'Frisby' family");
        String customFilterMetadata = getCustomFilterMetadata("Frisby");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "CustomFilterLinkedSchema", sourceContainerPath, null, "lists", "NIMHDemographics,NIMHPortions", customFilterMetadata);

        log("** Verifying linked schema tables are filtered");
        navigateToQuery("CustomFilterLinkedSchema", "NIMHDemographics");
        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("Expected 7 Frisby members", 7, table.getDataRowCount());

        Set<String> families = new HashSet<>(table.getColumnDataAsText("Family"));
        assertTrue("Expected only 'Frisby' in family collection: " + families, families.contains("Frisby") && families.size() == 1);

        List<String> names = table.getColumnDataAsText("Name");
        assertTrue("Expected 'Mrs. Frisby' in names: " + names, names.contains("Mrs. Frisby"));
        assertFalse("Unexpected 'Nicodemus' in names: " + names, names.contains("Nicodemus"));

        navigateToQuery("CustomFilterLinkedSchema", "NIMHPortions");
        table = new DataRegionTable("query", this);
        Set<String> subjectNames = new HashSet<>(table.getColumnDataAsText("SubjectID"));
        assertTrue("Expected 'Mrs. Frisby' in names: " + subjectNames, subjectNames.contains("Mrs. Frisby"));
        assertFalse("Unexpected 'Nicodemus' in names: " + subjectNames, subjectNames.contains("Nicodemus"));
    }

    @Test
    public void verifyLinkedSchemaWithLookup()
    {
        // This test validates the fixes for issues 32454 & 32456
        log("Create a list in the study folder.");
        _listHelper.createList(getProjectName() + "/" + STUDY_FOLDER, STUDY_LIST_NAME,
                ListHelper.ListColumnType.String, "GlobalPid",
                new ListHelper.ListColumn("Study", "Study", ListHelper.ListColumnType.String, "Study"));
        _listHelper.goToList(STUDY_LIST_NAME);
        _listHelper.clickImportData();
        _listHelper.submitTsvData(STUDY_LIST_DATA);

        log("Create the linked schema to the study.");
        String sourceContainerPath = "/" + getProjectName() + "/" + STUDY_FOLDER;
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, STUDY_SCHEMA_NAME, sourceContainerPath, null, "study", "Demographics", STUDY_FILTER_METADATA);

        log("Validate that with no filter all of the participants are visible in the linked schema.");
        checkLinkedSchema(STUDY_FILTER_METADATA, null, 6);

        log("Apply the filter. This should limit the user to those in 'StudyA'.");
        String updatedMetaData = STUDY_FILTER_METADATA.replace("<!-- ", "").replace(" -->", "");
        checkLinkedSchema(updatedMetaData, "StudyA", 2);

        log("Replace the study name with a different value (should limit the number of participants returned).");
        updatedMetaData = updatedMetaData.replace("StudyA", "StudyB");
        checkLinkedSchema(updatedMetaData, "StudyB", 1);

        log("Replace the study name with one that is not there, no participants should be returned.");
        updatedMetaData = updatedMetaData.replace("StudyB", "StudyC");
        checkLinkedSchema(updatedMetaData, "StudyC", 0);

        log("Now validate that a wrapped field gives the expected error.");
        goToProjectHome();
        navigateToFolder(getProjectName(), STUDY_FOLDER);
        wrapField("study", "Demographics","Participant ID", "Pid2Consent");

        log("Update the filter to use the wrapped field. Because the field is only wrapped and there is no foreign key it should error.");
        updatedMetaData = updatedMetaData.replace("ParticipantId/Study", "Pid2Consent/Study");
        _schemaHelper.updateLinkedSchema(getProjectName(), TARGET_FOLDER, STUDY_SCHEMA_NAME, sourceContainerPath, null, null, null, updatedMetaData);

        beginAt("/" + PROJECT_NAME + "/" + TARGET_FOLDER + "/query-begin.view?schemaName=CommonData&queryName=Demographics");
        assertElementContains(Locator.tagWithClass("div", "lk-qd-error"),
                "Error creating linked schema table 'Demographics': Column Pid2Consent.Study not found in column map.");

        beginAt("/" + PROJECT_NAME + "/" + TARGET_FOLDER + "/query-executeQuery.view?schemaName=CommonData&queryName=Demographics");
        assertTextPresent("Error creating linked schema table 'Demographics': Column Pid2Consent.Study not found in column map.");

        log("Looks good, going home.");
        goToProjectHome();
    }

    private void checkLinkedSchema(String updatedMetaData, String studyName, int expectedUsersCount)
    {
        String sourceContainerPath = "/" + getProjectName() + "/" + STUDY_FOLDER;
        _schemaHelper.updateLinkedSchema(getProjectName(), TARGET_FOLDER, STUDY_SCHEMA_NAME, sourceContainerPath, null, null, null, updatedMetaData);

        navigateToQuery("CommonData", "Demographics");
        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("Expected " + expectedUsersCount + " users from this study", expectedUsersCount, table.getDataRowCount());

        if((expectedUsersCount > 0) && (null != studyName))
        {
            if (!table.getColumnNames().contains("ParticipantId/Study"))
            {
                CustomizeView cv = table.openCustomizeGrid();
                cv.addColumn("ParticipantId/Study");
                cv.saveDefaultView();
            }

            List<String> studyValues = table.getColumnDataAsText("ParticipantId/Study");
            studyValues.forEach(value -> assertEquals("Value in column not as expected", studyName, value));
        }

    }

    // TODO this should really be in a page object, however in the interest of time doing it this way for now.
    private void wrapField(String schema, String query, String fieldToWrap, String aliasFieldName)
    {
        navigateToMetadataQuery(schema, query);
        QueryMetadataEditorPage queryMetadataEditorPage = new QueryMetadataEditorPage(getDriver());
        queryMetadataEditorPage.aliasField().selectAliasField(fieldToWrap).clickApply();
        queryMetadataEditorPage.getFieldsPanel().getField("WrappedParticipantId").setName(aliasFieldName);
        queryMetadataEditorPage.clickSave();
    }

    private String getCustomFilterMetadata(String familyName)
    {
        return "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                "     <schemaCustomizer class = \"org.labkey.linkedschematest.TestLinkedSchemaCustomizer\">\n" +
                "          <family xmlns=\"\">" + familyName + "</family>\n" +
                "     </schemaCustomizer>\n" +
                "</tables>";
    }

    @Test
    public void schemaTemplateTest()
    {
        createLinkedSchemaUsingTemplate();
        verifyLinkedSchemaUsingTemplate();
    }

    @Test
    public void templateOverrideTest()
    {
        createLinkedSchemaTemplateOverride();
        verifyLinkedSchemaTemplateOverride();
    }

    @LogMethod
    void setupProject()
    {
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        _userHelper.createUser(READER_USER);

        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SOURCE_FOLDER);
        // Enable linkedschematest in source folder so the "BPeopleTemplate" is visible.
        _containerHelper.enableModule("linkedschematest");

        _containerHelper.createSubfolder(getProjectName(), TARGET_FOLDER);
        apiPermissionsHelper.setUserPermissions(READER_USER, "Reader");

        // Create a study folder.
        _containerHelper.createSubfolder(getProjectName(), STUDY_FOLDER, "Study");
        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"), true);
    }

    @LogMethod
    void createList() throws IOException, CommandException
    {
        log("** Creating list via API...");
        ListDefinition listDef = new IntListDefinition(LIST_NAME, "Key")
                .addField(new FieldDefinition("Name", FieldDefinition.ColumnType.String).setLabel("Name").setDescription("Name"))
                .addField(new FieldDefinition("Age", FieldDefinition.ColumnType.Integer).setLabel("Age").setDescription("Age"))
                .addField(new FieldDefinition("Crazy", FieldDefinition.ColumnType.Boolean).setLabel("Crazy").setDescription("Crazy?"))
                .addField(new FieldDefinition("P", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " P").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("Q", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " Q").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("R", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " R").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("S", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " S").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("T", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " T").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("U", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " U").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("V", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " V").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("W", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " W").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("X", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " X").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("Y", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " Y").setURL("fake/" + LIST_DEF_URL))
                .addField(new FieldDefinition("Z", FieldDefinition.ColumnType.String).setLabel(LIST_DEF_TITLE + " Z").setURL("fake/" + LIST_DEF_URL));
        listDef.getCreateCommand().execute(createDefaultConnection(), getProjectName() + "/" + SOURCE_FOLDER);

        log("** Importing some data...");
        beginAt("/" + PROJECT_NAME + "/" + SOURCE_FOLDER + "/list-begin.view");
        _listHelper.goToList(LIST_NAME);
        _listHelper.clickImportData();
        _listHelper.submitTsvData(LIST_DATA);

        log("** Applying metadata xml override to list...");
        beginAt("/query/" + PROJECT_NAME + "/" + SOURCE_FOLDER + "/sourceQuery.view?schemaName=lists&query.queryName=" + LIST_NAME + "#metadata");
        setCodeEditorValue("metadataText", LIST_METADATA_OVERRIDE);
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    void importListData()
    {
        File lists = TestFileUtils.getSampleData("lists/ListDemo.lists.zip");
        _listHelper.importListArchive(SOURCE_FOLDER, lists);

        //Create second folder that should be not visible to linked schemas and import lists again
        _containerHelper.createSubfolder(getProjectName(), OTHER_FOLDER);
        _listHelper.importListArchive(OTHER_FOLDER, lists);
    }

    @LogMethod
    void createLinkedSchema()
    {
        log("** Creating linked schema APeople without template");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, A_PEOPLE_SCHEMA_NAME, sourceContainerPath, null, "lists", LIST_NAME + "," + QUERY_NAME, A_PEOPLE_METADATA);

        log("** Applying metadata to " + LIST_NAME + " in linked schema container");
        beginAt("/query/" + PROJECT_NAME + "/" + TARGET_FOLDER + "/sourceQuery.view?schemaName=" + A_PEOPLE_SCHEMA_NAME + "&query.queryName=" + LIST_NAME + "#metadata");
        setCodeEditorValue("metadataText", A_PEOPLE_LIST_METADATA_OVERRIDE);
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);

        log("** Applying metadata to " + QUERY_NAME + " in linked schema container");
        beginAt("/query/" + PROJECT_NAME + "/" + TARGET_FOLDER + "/sourceQuery.view?schemaName=" + A_PEOPLE_SCHEMA_NAME + "&queryName=" + QUERY_NAME);
        assertElementPresent(Locator.tagWithClass("div", "labkey-customview-message").withText("This query is not editable"));
        _ext4Helper.clickExt4Tab("XML Metadata");
        setCodeEditorValue("metadataText", A_PEOPLE_QUERY_METADATA_OVERRIDE);
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    void verifyLinkedSchema()
    {
        log("** Check template filter is applied");
        goToSchemaBrowser();
        viewQueryData(A_PEOPLE_SCHEMA_NAME, LIST_NAME);
        impersonate(READER_USER);
        DataRegionTable table = new DataRegionTable("query", getDriver());
        assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        assertEquals("Expected to filter table to only Adam", "Adam", table.getDataAsText(0, A_PEOPLE_METADATA_TITLE));
        log("** Verify table metadata overrides when simplemodule is not active in TargetFolder");
        assertHrefContains(table, "A_People db_metadata List P", "a_db_metadata.view");
        assertHrefContains(table, "A_People db_metadata List Q", "a_db_metadata.view");
        assertHrefContains(table, "A_People db_metadata List R", "a_db_metadata.view");
        assertHrefContains(table, "A_People db_metadata List S", "a_db_metadata.view");
        assertHrefContains(table, "A_People template List T", "a_template_metadata.view");
        assertHrefContains(table, "A_People template List U", "a_template_metadata.view");
        assertHrefContains(table, "A_People template List V", "a_template_metadata.view");
        assertHrefContains(table, "A_People template List W", "a_template_metadata.view");
        // Columns X, Y and Z have their URL removed by the linked schema.
        assertHrefNotPresent(table, "db_metadata List X");
        assertHrefNotPresent(table, "file_metadata List Y");
        assertHrefNotPresent(table, "Original List Z");
        stopImpersonating(false);

        log("** Verify table metadata overrides when simplemodule is active in TargetFolder");
        pushLocation();
        _containerHelper.enableModules(Arrays.asList("linkedschematest"));
        popLocation();
        impersonate(READER_USER);
        table = new DataRegionTable("query", this);
        assertHrefContains(table, "A_People db_metadata List P", "a_db_metadata.view");
        assertHrefContains(table, "A_People db_metadata List Q", "a_db_metadata.view");
        assertHrefContains(table, "A_People db_metadata List R", "a_db_metadata.view");
        assertHrefContains(table, "A_People db_metadata List S", "a_db_metadata.view");
        assertHrefContains(table, "A_People file_metadata List T", "a_template_file_metadata.view");
        assertHrefContains(table, "A_People file_metadata List U", "a_template_file_metadata.view");
        assertHrefContains(table, "A_People file_metadata List V", "a_template_file_metadata.view");
        assertHrefContains(table, "A_People template List W",      "a_template_metadata.view");
        // Columns X, Y and Z have their URL removed by the linked schema.
        assertHrefNotPresent(table, "db_metadata List X");
        assertHrefNotPresent(table, "file_metadata List Y");
        assertHrefNotPresent(table, "Original List Z");
        // Check the custom details url is used
        clickAndWait(table.detailsLink(0));
        assertTitleContains("Record Details:");
        waitForText("Adam");
        stopImpersonating(false);

        log("** Verify query metadata overrides are correctly applied");
        goToSchemaBrowser();
        table = viewQueryData(A_PEOPLE_SCHEMA_NAME, QUERY_NAME);
        impersonate(READER_USER);
        assertHrefContains(table, "A_People db_metadata Query P", "a_db_metadata.view");
        assertHrefContains(table, "A_People file_metadata Query Q", "a_template_file_metadata.view");
        assertHrefContains(table, "A_People template Query R", "a_template_metadata.view");
        // Columns S-Z have their URL removed by the linked schema.
        assertHrefNotPresent(table, "file_metadata Query S");
        assertHrefNotPresent(table, "file_metadata Query T");
        assertHrefNotPresent(table, "db_metadata List U");
        assertHrefNotPresent(table, "db_metadata List V");
        assertHrefNotPresent(table, "db_metadata List W");
        assertHrefNotPresent(table, "db_metadata List X");
        assertHrefNotPresent(table, "file_metadata List Y");
        assertHrefNotPresent(table, "Original List Z");
        stopImpersonating(false);

        // Disable the module in the TargetFolder container so query validation will pass at the end of the test
        _containerHelper.disableModules("linkedschematest");
    }

    @LogMethod
    void createLinkedSchemaUsingTemplate()
    {
        log("** Creating linked schema BPeople using BPeopleTemplate");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, B_PEOPLE_SCHEMA_NAME, sourceContainerPath, "BPeopleTemplate", null, null, null);
    }

    @LogMethod
    void verifyLinkedSchemaUsingTemplate()
    {
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData(B_PEOPLE_SCHEMA_NAME, LIST_NAME);
        assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        // Check Name column is renamed and 'Britt' is the only value
        assertEquals("Expected to filter table to only Britt", "Britt", table.getDataAsText(0, B_PEOPLE_TEMPLATE_METADATA_TITLE));

        // Check the linkedschematest/other.view details url is used
        String detailsUrl = table.getDetailsHref(0);
        assertTrue("Expected details url to contain other.view, got '" + detailsUrl + "'", detailsUrl.contains("other.view"));
    }

    @LogMethod
    void createLinkedSchemaTemplateOverride()
    {
        log("** Creating linked schema BPeople using BPeopleTemplate with metadata override to only show 'D' people");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        _schemaHelper.createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, D_PEOPLE_SCHEMA_NAME, sourceContainerPath, "BPeopleTemplate", null, LIST_NAME + "," + QUERY_NAME, D_PEOPLE_METADATA);
    }

    @LogMethod
    void verifyLinkedSchemaTemplateOverride()
    {
        goToSchemaBrowser();
        viewQueryData(D_PEOPLE_SCHEMA_NAME, LIST_NAME);

        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        // Check Name column is renamed and 'Dave' is the only value
        assertEquals("Expected to filter table to only Dave", "Dave", table.getDataAsText(0, D_PEOPLE_METADATA_TITLE));

        // Check the details url has been disabled
        assertElementNotPresent(Locator.linkWithText("details"));

        // Check query is available and metadata is overridden
        goToSchemaBrowser();
        viewQueryData(D_PEOPLE_SCHEMA_NAME, QUERY_NAME);

        table = new DataRegionTable("query", this);
        table.setSort("Name", SortDirection.ASC);
        // Query is executed over the original People table, NOT the 'D_People' filtered People table.
        // So all crazy people are available in the original query (Dave, Adam, Josh),
        // but the D_People template metadata filters LinkedSchemaPeopleQuery to those > 30 (Adam, Dave).
        assertEquals("Unexpected number of rows", 2, table.getDataRowCount());
        assertEquals("Adam", table.getDataAsText(0, "Crazy " + D_PEOPLE_METADATA_TITLE));
        assertEquals("Dave", table.getDataAsText(1, "Crazy " + D_PEOPLE_METADATA_TITLE));
    }

    protected void goToSchemaBrowserTable(String schemaName, String tableName)
    {
        goToSchemaBrowser();
        selectQuery(schemaName, tableName);
    }

    protected void assertHrefContains(DataRegionTable table, String columnTitle, String expected)
    {
        assertTrue("Expected column '" + columnTitle + "' to be in table, was not found.", table.getColumnIndex(columnTitle) != -1);
        String href = table.getHref(0, columnTitle);
        assertNotNull("Expected column '" + columnTitle + "' to have href containing '" + expected + "', was null", href);
        assertTrue("Expected column '" + columnTitle + "' to have href containing '" + expected + "', got '" + href + "'", href.contains(expected));
    }

    protected void assertHrefNotPresent(DataRegionTable table, String columnTitle)
    {
        boolean hasHref = table.hasHref(0, columnTitle);
        assertFalse("Expected column '" + columnTitle + "' to have null href", hasHref);
    }

    protected void assertColumnsPresent(String sourceFolder, String schemaName, String tableName, String... columnNames)
    {
        clickFolder(sourceFolder);

        goToSchemaBrowserTable(schemaName, tableName);
        waitAndClick(Locator.linkWithText("view data"));
        waitForText(tableName);

        for (String name : columnNames)
        {
            waitForElement(DataRegionTable.Locators.columnHeader("query", name));
        }

        clickFolder("TargetFolder");
    }

    protected void assertColumnsNotPresent(String sourceFolder, String schemaName, String tableName, String... columnNames)
    {
        clickFolder(sourceFolder);

        goToSchemaBrowserTable(schemaName, tableName);
        waitAndClick(Locator.linkWithText("view data"));
        waitForText(tableName);

        for (String name : columnNames)
        {
            assertElementNotPresent(DataRegionTable.Locators.columnHeader("query", name));
        }
    }

    protected void assertLookupsWorking(String sourceFolder, String schemaName, String listName, boolean present, String... lookupColumns)
    {
        clickFolder(sourceFolder);

        goToSchemaBrowserTable(schemaName, listName);
        waitAndClick(Locator.linkWithText("view data"));

        _customizeViewsHelper.openCustomizeViewPanel();

        for (String column : lookupColumns)
        {
            assertEquals("Expected lookup column '" + column + "' to be " + (present ? "present" : "not present"), present, _customizeViewsHelper.isLookupColumn(column));
        }
    }

    protected void changelistLookup(String sourceFolder, String tableName, String fieldName, FieldDefinition.LookupInfo info)
    {
        clickFolder(sourceFolder);

        goToManageLists();

        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(tableName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName).setLookup(info);
        listDefinitionPage.clickSave();
    }

    protected void createLinkedSchemaQuery(String sourceFolder, String schemaName, String queryName, String tableName)
    {
        clickFolder(sourceFolder);

        goToSchemaBrowser();

        createNewQuery(schemaName, tableName)
            .setName(queryName)
            .setBaseTable(tableName)
            .clickCreate()
            .clickSaveAndFinish();
    }

}
