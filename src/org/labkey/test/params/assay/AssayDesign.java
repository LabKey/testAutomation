package org.labkey.test.params.assay;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AssayDesign<T extends AssayDesign<T>>
{
    final String _providerName;
    final List<Consumer<Protocol>> _transformers = new ArrayList<>();

    protected AssayDesign(String providerName, String name)
    {
        _providerName = providerName;
        _transformers.add(p -> p.setName(name));
    }

    public static AssayDesign<?> of(String providerName, String name)
    {
        return new AssayDesignImpl(name, providerName);
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
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand(_providerName);
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, containerPath);

        Protocol protocol = getProtocolResponse.getProtocol();

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
        Map<String, Domain> domains = new CaseInsensitiveHashMap<>();
        for (Domain domain : protocol.getDomains())
        {
            domains.put(domain.getName(), domain);
        }

        Domain domain = domains.get(domainName);
        if (domain == null)
        {
            domain = domains.get(domainName + " Fields");
            if (domain == null)
            {
                throw new IllegalArgumentException(String.format(
                        "Domain '%s' not found for assay provider '%s'. Found: %s",
                        domainName, protocol.getProviderName(), domains.keySet()));
            }
        }
        return domain;
    }

    protected abstract T getThis();
}

class AssayDesignImpl extends AssayDesign<AssayDesignImpl>
{
    public AssayDesignImpl(String name, String providerName)
    {
        super(providerName, name);
    }

    @Override
    protected AssayDesignImpl getThis()
    {
        return this;
    }
}
