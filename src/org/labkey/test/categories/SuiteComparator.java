package org.labkey.test.categories;

import java.util.Comparator;

/**
 * User: tchadick
 * Date: 7/30/13
 */
public class SuiteComparator implements Comparator
{
    @Override
    public int compare(Object o1, Object o2)
    {
        Class suite1 = (Class)o1;
        Class suite2 = (Class)o2;
        return suite1.getSimpleName().compareTo(suite2.getSimpleName());
    }
}
