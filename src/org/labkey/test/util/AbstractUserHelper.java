/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.BaseWebDriverTest;

public abstract class AbstractUserHelper
{
    protected BaseWebDriverTest _test;

    public AbstractUserHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public CreateUserResponse createUser(String userName, String clonedUserName)
    {
        return createUser(userName);
    }

    public CreateUserResponse createUser(String userName)
    {
        return createUser(userName, false, true);
    }

    public CreateUserResponse createUser(String userName, boolean verifySuccess)
    {
        return createUser(userName, false, verifySuccess);
    }

    public CreateUserResponse createUserAndNotify(String userName)
    {
        return createUser(userName, true, true);
    }

    public CreateUserResponse createUserAndNotify(String userName, boolean verifySuccess)
    {
        return createUser(userName, true, verifySuccess);
    }

    public abstract CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess);
}
