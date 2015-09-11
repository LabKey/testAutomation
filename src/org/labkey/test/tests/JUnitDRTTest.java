package org.labkey.test.tests;

import junit.framework.TestSuite;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.External;
import org.labkey.test.categories.UnitTests;

import java.util.Map;

/**
 * Created by matthew on 9/11/15.
 */

@Category({org.labkey.test.categories.DRT.class, UnitTests.class, External.class})
public class JUnitDRTTest
{
    static boolean accept(Map<String, Object> test)
    {
        return "DRT".equals(JUnitTest.getWhen(test));
    }

    public static TestSuite suite() throws Exception
    {
        return JUnitTest._suite(JUnitDRTTest::accept,0,false);
    }
}

