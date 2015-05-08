package org.labkey.test.util;

/**
 * Marks test classes that can run repeatedly without cleanup
 */
public interface ReadOnlyTest
{
    boolean needsSetup();
}
