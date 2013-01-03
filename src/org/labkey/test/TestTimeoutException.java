package org.labkey.test;

/**
 * Thrown to indicate that the test failed by timeout when waiting for the server to respond. The harness should
 * generally try to get a thread dump from the server to track down the issue.
 * User: jeckels
 * Date: 1/2/13
 */
public class TestTimeoutException extends Exception
{
    public TestTimeoutException(Exception e)
    {
        super(e);
    }
}
