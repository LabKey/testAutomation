/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.params.assay;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AssayDesign<T extends AssayDesign<T>>
{
    final String _name;
    final String _providerName;
    final List<Consumer<Protocol>> _transformers = new ArrayList<>();

    protected AssayDesign(String providerName, String name)
    {
        _providerName = providerName;
        _transformers.add(p -> p.setName(name));
        _name = name;
    }

    public static AssayDesign<?> of(String providerName, String name)
    {
        return new AssayDesignImpl(providerName, name);
    }

    public T addProtocolTransformer(Consumer<Protocol> transformer)
    {
        _transformers.add(transformer);
        return getThis();
    }

    public T addDomainTransformer(String domainName, Consumer<Domain> transformer)
    {
        _transformers.add(protocol -> {
            Domain domain = extractDomain(domainName, protocol);
            transformer.accept(domain);
        });
        return getThis();
    }

    public T setFields(String domainName, List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return addDomainTransformer(domainName, domain -> {
            List<PropertyDescriptor> pds = new ArrayList<>();
            if (keepExisting)
            {
                pds.addAll(domain.getFields());
            }
            pds.addAll(fields);
            domain.setFields(pds);
        });
    }

    public Protocol createAssay(String containerPath, Connection connection) throws IOException, CommandException
    {
        TestLogger.info("Creating %s assay '%s' in '%s'".formatted(_providerName, _name, containerPath));

        GetProtocolCommand getProtocolCommand = new GetProtocolCommand(_providerName);
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, containerPath);

        Protocol protocol = updateProtocol(containerPath, connection, getProtocolResponse.getProtocol());

        TestLogger.log().debug(() -> String.format("Successfully created %s assay '%s':\n%s", _providerName,
                protocol.getName(), protocol.toJSONObject().toString(2)));

        return protocol;
    }

    private Protocol updateProtocol(String containerPath, Connection connection, Protocol protocol) throws IOException, CommandException
    {
        for (var transformer : _transformers)
        {
            transformer.accept(protocol);
        }

        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(protocol);
        ProtocolResponse saveProtocolResponse = saveProtocolCommand.execute(connection, containerPath);
        return saveProtocolResponse.getProtocol();
    }

    protected Domain extractDomain(String domainName, Protocol protocol)
    {
        for (Domain domain : protocol.getDomains())
        {
            if (domain.getName().endsWith(domainName + " Fields"))
            {
                return domain;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Domain '%s' not found for assay provider '%s'. Found: %s",
                domainName, protocol.getProviderName(), protocol.getDomains().stream().map(Domain::getName).toList()));
    }

    protected abstract T getThis();
}

class AssayDesignImpl extends AssayDesign<AssayDesignImpl>
{
    public AssayDesignImpl(String providerName, String name)
    {
        super(providerName, name);
    }

    @Override
    protected AssayDesignImpl getThis()
    {
        return this;
    }
}
