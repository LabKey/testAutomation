/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.util.Arrays;
import java.util.List;

@Category({DailyA.class})
public class FieldValidatorTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ValidatorVerifyProject";
    private static final String LIST_NAME = "QCList";

    private static final String SEX_ERROR_MSG = "Sex must be either: <male|female>";
    private static final String ID_ERROR_MSG = "ID must be of the format: ID:<any character>:001";
    private static final String AGE_ERROR_MSG = "Age must equal: 25";

    private static final String TEST_DATA_FAIL = "Name" + "\t" + "Age" + "\t" + "ID" + "\t" + "Sex" + "\n" +
            "Ted" + "\t" + "37" + "\t" + "ID:foo:002" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "17" + "\t" + "ID:EEE:001" + "\t" + "girl" + "\n" +
            "Bob" + "\t" + "79" + "\t" + "PTID:TTT:001" + "\t" + "boy" + "\n";

    private static final String TEST_DATA_PASS = "Name" + "\t" + "Age" + "\t" + "ID" + "\t" + "Sex" + "\n" +
            "Ted" + "\t" + "25" + "\t" + "ID:foo:001" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "25" + "\t" + "ID:EEE:001" + "\t" + "female" + "\n" +
            "Bob" + "\t" + "25" + "\t" + "ID:TTT:001" + "\t" + "male" + "\n";

    @Test
    public void testSteps()
    {
        log("Setup project and list module");
        _containerHelper.createProject(PROJECT_NAME, null);

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[]{

                new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, ""),
                new ListHelper.ListColumn("id", "ID", ListHelper.ListColumnType.String, "",
                        new ListHelper.RegExValidator("idValidator", "idValidator", ID_ERROR_MSG, "ID:.*:001")),
                new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "",
                        new ListHelper.RangeValidator("ageValidator", "ageValidator", AGE_ERROR_MSG, ListHelper.RangeType.Equals, "25")),
                new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "",
                        new ListHelper.RegExValidator("sexValidator", "sexValidator", SEX_ERROR_MSG, "male|female")),
        };

        _listHelper.createList(PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload data");
        clickButton("Import Data");
        setFormElement(Locator.name("text"), TEST_DATA_FAIL);
        _listHelper.submitImportTsv_error(SEX_ERROR_MSG);
        assertTextPresent(ID_ERROR_MSG, AGE_ERROR_MSG);

        _listHelper.submitTsvData(TEST_DATA_PASS);
        assertTextPresent("Ted", "Alice", "Bob");

        // ID regex validation
        log("Test inserting new row");
        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_id"), "id:123abc:001");
        setFormElement(Locator.name("quf_name"), "Sid");
        setFormElement(Locator.name("quf_sex"), "male");
        setFormElement(Locator.name("quf_age"), "25");
        submit();
        waitForText(ID_ERROR_MSG);
        setFormElement(Locator.name("quf_id"), "ID:123abc:002");
        submit();
        waitForText(ID_ERROR_MSG);
        setFormElement(Locator.name("quf_id"), "ID:123abc:001");
        submit();
        assertTextPresent("ID:123abc:001");

        // age range validation
        log("Test inserting new row");
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_id"), "ID:123abc:001");
        setFormElement(Locator.name("quf_name"), "Mikey");
        setFormElement(Locator.name("quf_sex"), "male");
        setFormElement(Locator.name("quf_age"), "10");
        submit();
        waitForText(AGE_ERROR_MSG);
        setFormElement(Locator.name("quf_age"), "25");
        submit();
        assertTextPresent("Mikey");

        // sex validation
        log("Test inserting new row");
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_id"), "ID:123abc:001");
        setFormElement(Locator.name("quf_name"), "Kim");
        setFormElement(Locator.name("quf_sex"), "Female");
        setFormElement(Locator.name("quf_age"), "25");
        submit();
        waitForText(SEX_ERROR_MSG);
        setFormElement(Locator.name("quf_sex"), "femalefemale");
        submit();
        waitForText(SEX_ERROR_MSG);

        setFormElement(Locator.name("quf_sex"), "female");
        submit();
        assertTextPresent("Kim");
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

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
