package org.labkey.test.util;

import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.openqa.selenium.WebDriver;

public class LinkedSchemaTestUtils
{
    public static final String LIST_NAME = "LinkedSchemaTestPeople";
    public static final String QUERY_NAME = "LinkedSchemaTestQuery";
    public static final String A_PEOPLE_SCHEMA_NAME = "A_People";

    // Original list definition title and URL
    public static final String ORIGINAL_TITLE = "Original List";
    private static final String ORIGINAL_URL = "fake/list_original.view";

    private LinkedSchemaTestUtils()
    {
    }

    public static void createOriginalList(String containerPath, WebDriver driver)
    {
        new ListHelper(driver).createList(containerPath, LIST_NAME,
            new FieldDefinition("Key", ColumnType.AutoInteger),
            new FieldDefinition("Name", ColumnType.String).setDescription("Name"),
            new FieldDefinition("Age", ColumnType.String).setDescription("Age"),
            new FieldDefinition("Crazy", ColumnType.String).setDescription("Crazy?"),
            new FieldDefinition("P", ColumnType.String).setLabel(ORIGINAL_TITLE + " P").setURL(ORIGINAL_URL),
            new FieldDefinition("Q", ColumnType.String).setLabel(ORIGINAL_TITLE + " Q").setURL(ORIGINAL_URL),
            new FieldDefinition("R", ColumnType.String).setLabel(ORIGINAL_TITLE + " R").setURL(ORIGINAL_URL),
            new FieldDefinition("S", ColumnType.String).setLabel(ORIGINAL_TITLE + " S").setURL(ORIGINAL_URL),
            new FieldDefinition("T", ColumnType.String).setLabel(ORIGINAL_TITLE + " T").setURL(ORIGINAL_URL),
            new FieldDefinition("U", ColumnType.String).setLabel(ORIGINAL_TITLE + " U").setURL(ORIGINAL_URL),
            new FieldDefinition("V", ColumnType.String).setLabel(ORIGINAL_TITLE + " V").setURL(ORIGINAL_URL),
            new FieldDefinition("W", ColumnType.String).setLabel(ORIGINAL_TITLE + " W").setURL(ORIGINAL_URL),
            new FieldDefinition("X", ColumnType.String).setLabel(ORIGINAL_TITLE + " X").setURL(ORIGINAL_URL),
            new FieldDefinition("Y", ColumnType.String).setLabel(ORIGINAL_TITLE + " Y").setURL(ORIGINAL_URL),
            new FieldDefinition("Z", ColumnType.String).setLabel(ORIGINAL_TITLE + " Z").setURL(ORIGINAL_URL));
    }
}
