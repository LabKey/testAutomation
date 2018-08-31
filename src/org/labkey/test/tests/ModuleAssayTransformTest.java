package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;

@Category({DailyA.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class ModuleAssayTransformTest extends ModuleAssayTest
{
    public ModuleAssayTransformTest()
    {
        super(true);
    }


}
