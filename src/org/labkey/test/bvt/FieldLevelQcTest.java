/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
* User: Jess Garms
* Date: Jan 16, 2009
*/
public class FieldLevelQcTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "FieldLevelQcVerifyProject";
    private static final String LIST_NAME = "QCList";

    private static final String TEST_DATA_SINGLE_COLUMN_QC =
            "Name" + "\t" + "Age" + "\t"  + "Sex" + "\n" +
            "Ted" + "\t" + ".N" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "17" + "\t" + "female" + "\n" +
            "Bob" + "\t" + ".Q" + "\t" + ".N" + "\n";

    private static final String TEST_DATA_TWO_COLUMN_QC =
            "Name" +    "\t" + "Age" +  "\t" + "AgeQCIndicator" +   "\t" + "Sex" +  "\t" + "SexQCIndicator" + "\n" +
            "Franny" +  "\t" + "" +     "\t" + ".N" +               "\t" + "male" + "\t" +  "" + "\n" +
            "Zoe" +     "\t" + "25" +   "\t" + ".Q" +               "\t" + "" +     "\t" +  ".N" + "\n" +
            "J.D." +    "\t" + "50" +   "\t" + "" +                 "\t" + "male" + "\t" +  "" + "\n";

    protected void doTestSteps() throws Exception
    {
        log("Setup project and list module");
        createProject(PROJECT_NAME);

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[3];

        ListHelper.ListColumn listColumn = new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "");
        columns[0] = listColumn;

        listColumn = new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "");
        listColumn.setAllowsQc(true);
        columns[1] = listColumn;

        listColumn = new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "");
        listColumn.setAllowsQc(true);
        columns[2] = listColumn;


        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload data");
        clickLinkWithText("import data");
        setFormElement("ff_data", TEST_DATA_SINGLE_COLUMN_QC);
        submit();
        assertTextPresent("Ted");
        assertTextPresent("Alice");
        assertTextPresent("Bob");
        assertTextPresent(".Q");
        assertTextPresent(".N");

        log("Test inserting new row");
        clickNavButton("Insert New");
        setFormElement("quf_name", "Sid");
        setFormElement("quf_sex", "male");
        setFormElement("quf_age", ".N");
        submit();
        assertTextPresent("Sid");
        assertTextPresent("Ted");

        log("Test QCIndicator column");
        clickNavButton("Import Data");
        setFormElement("ff_data", TEST_DATA_TWO_COLUMN_QC);
        submit();
        assertTextPresent("Franny");
        assertTextPresent("Zoe");
        assertTextPresent("J.D.");
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
        return "";
    }
}