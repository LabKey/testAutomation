/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.ListHelper;

/*
* User: Karl Lum
* Date: Oct 6, 2008
* Time: 12:47:28 PM
*/
public class FieldValidatorTest extends BaseSeleniumWebTest
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

    protected void doTestSteps() throws Exception
    {
        log("Setup project and list module");
        createProject(PROJECT_NAME);

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[] {

                new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, ""),
                new ListHelper.ListColumn("id", "ID", ListHelper.ListColumnType.String, "",
                        new ListHelper.RegExValidator("idValidator", "idValidator", ID_ERROR_MSG, "ID:.*:001")),
                new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "",
                        new ListHelper.RangeValidator("ageValidator", "ageValidator", AGE_ERROR_MSG, ListHelper.RangeType.Equals, "25")),
                new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "",
                        new ListHelper.RegExValidator("sexValidator", "sexValidator", SEX_ERROR_MSG, "male|female")),
        };

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload data");
        clickNavButton("Import Data");
        setFormElement("text", TEST_DATA_FAIL);
        ListHelper.submitImportTsv_error(this, null);
        assertTextPresent(SEX_ERROR_MSG);
        assertTextPresent(ID_ERROR_MSG);
        assertTextPresent(AGE_ERROR_MSG);

        ListHelper.submitTsvData(this, TEST_DATA_PASS);
        assertTextPresent("Ted");
        assertTextPresent("Alice");
        assertTextPresent("Bob");

        // ID regex validation
        log("Test inserting new row");
        clickNavButton("Insert New");
        setFormElement("quf_id", "id:123abc:001");
        setFormElement("quf_name", "Sid");
        setFormElement("quf_sex", "male");
        setFormElement("quf_age", "25");
        submit();
        assertTextPresent(ID_ERROR_MSG);
        setFormElement("quf_id", "ID:123abc:002");
        submit();
        assertTextPresent(ID_ERROR_MSG);
        setFormElement("quf_id", "ID:123abc:001");
        submit();
        assertTextPresent("ID:123abc:001");

        // age range validation
        log("Test inserting new row");
        clickNavButton("Insert New");
        setFormElement("quf_id", "ID:123abc:001");
        setFormElement("quf_name", "Mikey");
        setFormElement("quf_sex", "male");
        setFormElement("quf_age", "10");
        submit();
        assertTextPresent(AGE_ERROR_MSG);
        setFormElement("quf_age", "25");
        submit();
        assertTextPresent("Mikey");

        // sex validation
        log("Test inserting new row");
        clickNavButton("Insert New");
        setFormElement("quf_id", "ID:123abc:001");
        setFormElement("quf_name", "Kim");
        setFormElement("quf_sex", "Female");
        setFormElement("quf_age", "25");
        submit();
        assertTextPresent(SEX_ERROR_MSG);
        setFormElement("quf_sex", "femalefemale");
        submit();
        assertTextPresent(SEX_ERROR_MSG);

        setFormElement("quf_sex", "female");
        submit();
        assertTextPresent("Kim");
    }

    protected void doCleanup() throws Exception
    {
        try {
            deleteProject(PROJECT_NAME);
        }
        catch (Throwable t) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/experiment";
    }
}
