package org.labkey.test.util;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;

import java.util.Arrays;
import java.util.List;

/**
 * Bootstrap a server without the initial user validation done by {@link LabKeySiteWrapper#signIn()}
 * Not actually a test. Just piggy-backing on the test harness to make it easier to run.
 */
@Category({})
public class QuickBootstrapPseudoTest
{
    @Test
    public void bootstrap()
    {
        new ApiBootstrapHelper().signIn();
    }
}
