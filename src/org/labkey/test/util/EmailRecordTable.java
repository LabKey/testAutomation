package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.WebDriver;

/**
 * Retaining temporarily for feature branch compatibility
 * @deprecated Use {@link org.labkey.test.components.dumbster.EmailRecordTable}
 */
@Deprecated
public class EmailRecordTable extends org.labkey.test.components.dumbster.EmailRecordTable
{
    public EmailRecordTable(WebDriver driver)
    {
        super(driver);
    }

    public EmailRecordTable(BaseWebDriverTest test)
    {
        super(test);
    }
}
