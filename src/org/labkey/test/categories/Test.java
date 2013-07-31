package org.labkey.test.categories;

/**
 * User: tchadick
 * Date: 7/26/13
 */
public abstract class Test
{
    public static int getCrawlerTimeout()
    {
        return 90000;
    }

    public static boolean isSuite()
    {
        return true;
    }
}
