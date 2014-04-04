/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.test;

/**
 * Thrown to indicate that the test failed by timeout when waiting for the server to respond. The harness should
 * generally try to get a thread dump from the server to track down the issue.
 */
public class TestTimeoutException extends RuntimeException
{
    public TestTimeoutException(Throwable t)
    {
        super(t);
    }

    public TestTimeoutException(String msg)
    {
        super(msg);
    }

    public TestTimeoutException(String msg, Throwable t)
    {
        super(msg, t);
    }
}
