/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class FieldValidatorTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ValidatorVerifyProject";
    private static final String LIST_NAME = "QCList";

    private static final String SEX_ERROR_MSG = "Sex must be either: <male|female>";
    private static final String ID_ERROR_MSG = "ID must be of the format: ID:<any character>:001";
    private static final String AGE_ERROR_MSG = "Age must equal: 25";
    private static final String DATE_ERROR_MSG = "Date must be in 2012.";

    private static final String TEST_DATA_FAIL = """
            Name\tAge\tID\tSex\tDate
            Ted\t37\tID:foo:002\tmale\t1/1/2010
            Alice\t17\tID:EEE:001\tgirl\t2/2/2012
            Bob\t79\tPTID:TTT:001\tboy\t3/3/2012
            """;

    private static final String TEST_DATA_PASS = """
            Name\tAge\tID\tSex\tDate
            Ted\t25\tID:foo:001\tmale\t1/1/2012
            Alice\t25\tID:EEE:001\tfemale\t2/2/2012
            Bob\t25\tID:TTT:001\tmale\t3/3/2012
            """;

    @Test
    public void testSteps() throws Exception
    {
        log("Setup project and list module");
        _containerHelper.createProject(PROJECT_NAME, null);

        List<FieldDefinition> columns = List.of(
                new FieldDefinition("name", FieldDefinition.ColumnType.String).setLabel("Name"),
                new FieldDefinition("id", FieldDefinition.ColumnType.String).setLabel("ID").setValidators(List.of(
                        new FieldDefinition.RegExValidator("idValidator", "idValidator", ID_ERROR_MSG, "ID:.*:001"))),
                new FieldDefinition("age", FieldDefinition.ColumnType.Integer).setLabel("Age").setValidators(List.of(
                        new FieldDefinition.RangeValidator("ageValidator", "ageValidator", AGE_ERROR_MSG, FieldDefinition.RangeType.Equals, "25"))),
                new FieldDefinition("sex", FieldDefinition.ColumnType.String).setLabel("Sex").setValidators(List.of(
                        new FieldDefinition.RegExValidator("sexValidator", "sexValidator", SEX_ERROR_MSG, "male|female"))),
                new FieldDefinition("date", FieldDefinition.ColumnType.DateAndTime).setLabel("Date").setValidators(List.of(
                        new FieldDefinition.RangeValidator("dateValidator", "dateValidator", DATE_ERROR_MSG, FieldDefinition.RangeType.GTE, "1/1/2012", FieldDefinition.RangeType.LT, "1/1/2013")))
                );

        new IntListDefinition(LIST_NAME, "Key").setFields(columns)
                .create(createDefaultConnection(), getProjectName());

        new PortalHelper(this).addWebPart("Lists");

        log("Test upload data");
        _listHelper.goToList(LIST_NAME);
        ImportDataPage importDataPage = _listHelper.clickImportData();
        importDataPage.setText(TEST_DATA_FAIL);
        String errors = importDataPage.submitExpectingError();
        Assertions.assertThat(errors).as("Error messages.").contains(List.of(
                SEX_ERROR_MSG,
                ID_ERROR_MSG,
                AGE_ERROR_MSG,
                DATE_ERROR_MSG));

        importDataPage.setText(TEST_DATA_PASS).submit();
        assertTextPresent("Ted", "Alice", "Bob");

        log("Test range validator on ID column");
        DataRegionTable table = new DataRegionTable("query", getDriver());
        UpdateQueryRowPage updatePage = table.clickInsertNewRow();
        updatePage.setFields(Map.of(
                "id", "id:123abc:001",
                "name", "Sid",
                "sex", "male",
                "age", "25",
                "date", "9/9/2012"));
        updatePage.submitExpectingErrorContaining(ID_ERROR_MSG);
        updatePage.setField("id", "ID:123abc:002");
        updatePage.submitExpectingErrorContaining(ID_ERROR_MSG);
        updatePage.setField("id", "ID:123abc:001");
        updatePage.submit();
        assertTextPresent("Sid", "ID:123abc:001");

        // age range validation
        log("Test range validator on age column");
        updatePage = table.clickInsertNewRow();
        updatePage.setFields(Map.of(
                "id", "ID:123abc:001",
                "name", "Mikey",
                "sex", "male",
                "age", "10",
                "date", "9/9/2012"));
        updatePage.submitExpectingErrorContaining(AGE_ERROR_MSG);
        updatePage.setField("age", "25");
        updatePage.submit();
        assertTextPresent("Mikey");

        // sex validation
        log("Test regex validator on sex column");
        updatePage = table.clickInsertNewRow();
        updatePage.setFields(Map.of(
                "id", "ID:123abc:001",
                "name", "Kim",
                "sex", "Female",
                "age", "25",
                "date", "9/9/2012"));
        updatePage.submitExpectingErrorContaining(SEX_ERROR_MSG);
        updatePage.setField("sex", "femalefemale");
        updatePage.submitExpectingErrorContaining(SEX_ERROR_MSG);
        updatePage.setField("sex", "female");
        updatePage.submit();
        assertTextPresent("Kim");

        log("Test range validator on date column");
        updatePage = table.clickInsertNewRow();
        updatePage.setFields(Map.of(
                "id", "ID:123abc:001",
                "name", "Etta",
                "sex", "male",
                "age", "25",
                "date", "9/9/2020"));
        updatePage.submitExpectingErrorContaining(DATE_ERROR_MSG);
        updatePage.setField("date", "9/9/2010");
        updatePage.submitExpectingErrorContaining(DATE_ERROR_MSG);
        updatePage.setField("date", "10/10/2012");
        updatePage.submit();
        assertTextPresent("Etta", "2012-10-10");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
