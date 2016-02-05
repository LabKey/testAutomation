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

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.remoteapi.security.GetUsersCommand;
import org.labkey.remoteapi.security.GetUsersResponse;
import org.labkey.test.BaseWebDriverTest;

import java.io.IOException;

import static org.junit.Assert.*;

public class APIUserHelper extends AbstractUserHelper
{
    public APIUserHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public CreateUserResponse createUser(String userName, boolean sendEmail, boolean verifySuccess)
    {
        CreateUserCommand command = new CreateUserCommand(userName);
        command.setSendEmail(sendEmail);
        Connection connection = _test.createDefaultConnection(false);
        try
        {
            CreateUserResponse response = command.execute(connection, "");

            if (verifySuccess)
            {
                assertEquals(userName, response.getEmail());
                assertTrue("Invalid userId", response.getUserId() != null);
            }
            return response;
        }
        catch (CommandException | IOException e)
        {
            if(verifySuccess)
                throw new RuntimeException("Error while creating user", e);
            return null;
        }
    }

    public GetUsersResponse getUsers()
    {
        GetUsersCommand command = new GetUsersCommand();
        Connection connection = _test.createDefaultConnection(false);

        try
        {
            return command.execute(connection, "/");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }
}
