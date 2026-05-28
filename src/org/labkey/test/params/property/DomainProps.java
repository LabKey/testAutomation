/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.params.property;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for domain properties that might be created via the 'createDomain' API
 */
public abstract class DomainProps
{
    protected abstract @NotNull Domain getDomainDesign();
    protected abstract @NotNull String getKind();
    protected abstract @NotNull Map<String, Object> getOptions();

    protected abstract @NotNull String getSchemaName();
    protected abstract @NotNull String getQueryName();

    public final CreateDomainCommand getCreateCommand()
    {
        CreateDomainCommand command = new CreateDomainCommand(getKind(), getDomainDesign().getName())
        {
            @Override
            public DomainResponse execute(Connection connection, String folderPath) throws IOException, CommandException
            {
                TestLogger.info(String.format("Creating %s domain '%s.%s' in '%s'", getKind(), getSchemaName(), getQueryName(), folderPath));

                DomainResponse response = super.execute(connection, folderPath);

                TestLogger.log().debug("Successfully created domain, '{}':\n{}",
                        () -> response.getDomain().getName(),
                        () -> response.getDomain().toJSONObject().toString(2));

                return response;
            }
        };
        command.setOptions(new HashMap<>(getOptions()));
        command.setDomainDesign(getDomainDesign());
        return command;
    }

    public TestDataGenerator getTestDataGenerator(String containerPath)
    {
        return new TestDataGenerator(getSchemaName(), getQueryName(), containerPath).withColumns(getDomainDesign().getFields());
    }

    public final TestDataGenerator create(Connection connection, String containerPath) throws IOException, CommandException
    {
        getCreateCommand().execute(connection, containerPath);
        return getTestDataGenerator(containerPath);
    }
}
