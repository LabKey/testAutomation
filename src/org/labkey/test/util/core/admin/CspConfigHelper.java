package org.labkey.test.util.core.admin;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.admin.ExternalSourcesPage.Directive;
import org.labkey.test.pages.core.admin.logger.ManagerPage;
import org.labkey.test.util.Log4jUtils;
import org.labkey.test.util.OptionalFeatureHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class CspConfigHelper
{
    public static final String DISABLE_ENFORCE_CSP_OPTIONAL_FEATURE = "disableEnforceCsp";
    private final Supplier<Connection> _connectionSupplier;

    public CspConfigHelper(Supplier<Connection> connectionSupplier)
    {
        _connectionSupplier = connectionSupplier;
    }

    public CspConfigHelper(Connection connection)
    {
        this(() -> connection);
    }

    public CspConfigHelper(WebDriverWrapper driverWrapper)
    {
        this(driverWrapper::createDefaultConnection);
    }

    private Connection getConnection()
    {
        return _connectionSupplier.get();
    }

    public void setAllowedHosts(Map<Directive, List<String>> allowedHosts) throws IOException, CommandException
    {
        StringBuilder existingValues = new StringBuilder();
        for (Map.Entry<Directive, List<String>> entry : allowedHosts.entrySet())
        {
            Directive directive = entry.getKey();
            for (String value : entry.getValue())
            {
                existingValues.append(directive.name()).append("|").append(value).append("\n");
            }
        }

        SimplePostCommand postCommand = new SimplePostCommand("admin", "externalSources");
        postCommand.setParameters(Map.of(
            "saveAll", true,
            "existingValues", existingValues));
        postCommand.execute(getConnection(), null);
    }

    public void clearAllowedHosts() throws IOException, CommandException
    {
        setAllowedHosts(Collections.emptyMap());
    }

    public void setEnforceCsp(boolean enforce)
    {
        Objects.requireNonNull(OptionalFeatureHelper.setOptionalFeature(getConnection(), DISABLE_ENFORCE_CSP_OPTIONAL_FEATURE, !enforce), () -> "Unable to configure enforce CSP.");
    }

    public static void debugCspWarnings()
    {
        Log4jUtils.setLogLevel("org.labkey.core.admin.AdminController.ContentSecurityPolicyReportAction", ManagerPage.LoggingLevel.DEBUG);
    }
}
