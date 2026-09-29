/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.labkey.remoteapi.ApiKeyCredentialsProvider;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.SessionRequestsCommand;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.remoteapi.user.ImpersonateRolesCommand;
import org.labkey.remoteapi.security.WhoAmIResponse;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs a stress {@link Simulation} against an arbitrary server from the command line, with no Selenium and no test
 * project setup. Intended for generating load against an existing deployment while something else about that
 * deployment is varied -- database instance class, Postgres settings, and so on.
 * <p>
 * Everything determining the shape of the load is pinned by the config file and echoed, with digests of the activity
 * files, into a run manifest. Two runs whose manifests agree except for the thing under test are comparable; runs whose
 * manifests differ in any other field are not.
 * <pre>{@code
 * ./gradlew :server:testAutomation:runStressSimulation -PsimulationConfig=/path/to/run.json
 * }</pre>
 * @see RunConfig for the config file format
 */
public class SimulationRunner
{
    /** Infrastructure traffic that is always present and does not represent a competing user */
    private final RunConfig _config;
    private final String _runUuid = UUID.randomUUID().toString();
    private volatile Integer _impersonationGroupId;

    public SimulationRunner(RunConfig config)
    {
        _config = config;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.err.println("Usage: SimulationRunner <config.json>");
            System.exit(1);
        }

        System.exit(new SimulationRunner(RunConfig.fromFile(new File(args[0]))).run() ? 0 : 1);
    }

    /**
     * @return true if the measured run completed without a simulation dying
     */
    public boolean run() throws Exception
    {
        Map<String, String> replacements = preflight();
        boolean miniProfilerAvailable = checkMiniProfiler();

        if (_config.preflightOnly())
        {
            TestLogger.log("Mini-profiler %s".formatted(miniProfilerAvailable ? "reachable" : "UNAVAILABLE"));
            TestLogger.log("Preflight only; no load generated.");
            return miniProfilerAvailable || !_config.requireMiniProfiler();
        }

        if (_config.requireMiniProfiler() && !miniProfilerAvailable)
        {
            throw new IllegalStateException("Mini-profiler is not available on " + _config.baseUrl()
                    + " but 'requireMiniProfiler' is set. Enable it via Admin Console > Mini Profiler, confirm the "
                    + "credential's user is a site admin, or set 'requireMiniProfiler' false to record client timings only.");
        }
        if (!miniProfilerAvailable)
        {
            TestLogger.warn("Mini-profiler unavailable; recording client-observed timings only. "
                    + "Server-side duration and per-request SQL counts will be missing.");
        }

        if (!_config.clearCachesBeforeRun().isEmpty())
        {
            clearCaches(newConnection());
        }

        if (_config.warmupSeconds() > 0)
        {
            TestLogger.log("Warm-up: %d sessions for %ds (results discarded)".formatted(_config.sessions(), _config.warmupSeconds()));
            runScenario(new DiscardedScenario(buildDefinitions(replacements)), Duration.ofSeconds(_config.warmupSeconds()), null);
        }

        File clientResults = new File(_config.resultsDir(), _config.scenarioName() + "-" + _runUuid + "-client.tsv");
        RequestResultTsvWriter clientWriter = new RequestResultTsvWriter(clientResults);

        StabilityWatcher stability = _config.stopWhenStable() == null ? null
                : new StabilityWatcher(_config.stopWhenStable().samples(), _config.stopWhenStable().maxRatioToPeak());
        AbstractScenario<?> scenario = miniProfilerAvailable
                ? new TeedSessionScenario(buildDefinitions(replacements), _config.scenarioName(), _config.resultsDir(), clientWriter, stability)
                : new ClientOnlyScenario(buildDefinitions(replacements), _config.scenarioName(), clientWriter, stability);

        TestLogger.log("Measured run: %d sessions for %ds".formatted(_config.sessions(), _config.durationSeconds()));
        Instant start = Instant.now();
        boolean completed = runScenario(scenario, Duration.ofSeconds(_config.durationSeconds()), stability);
        Instant end = Instant.now();

        writeManifest(replacements, miniProfilerAvailable, start, end, completed, clientResults, scenario.getResultsFile());
        return completed;
    }

    /**
     * Verifies the credentials work before generating any load, and resolves USERID so activity files don't have to
     * hard-code a user id that differs per server.
     * @return replacements, with USERID added unless the config set it explicitly
     */
    private Map<String, String> preflight() throws IOException, CommandException, InterruptedException
    {
        Connection connection = newConnection();
        WhoAmIResponse whoAmI = new WhoAmICommand().execute(connection, null);
        TestLogger.log("Authenticated to %s as %s (id %s)%s".formatted(_config.baseUrl(), whoAmI.getEmail(),
                whoAmI.getUserId(), whoAmI.isImpersonated() ? ", impersonating" : ""));

        if (_config.impersonates() && !whoAmI.isImpersonated())
        {
            throw new IllegalStateException("Impersonation was requested but the session is not impersonating. "
                    + "Check that the credential's user holds an impersonating role and may impersonate '"
                    + _config.impersonateGroup() + "' in " + _config.impersonateContainer() + ".");
        }

        if (!_config.clearCachesBeforeRun().isEmpty())
        {
            verifyCacheNames(connection);
        }

        Map<String, String> replacements = new HashMap<>(_config.replacements());
        replacements.putIfAbsent("USERID", String.valueOf(whoAmI.getUserId()));
        return replacements;
    }

    private boolean checkMiniProfiler()
    {
        try
        {
            new SessionRequestsCommand(0L).execute(newConnection(), null);
            return true;
        }
        catch (Exception e)
        {
            TestLogger.debug("Mini-profiler probe failed: " + e.getMessage());
            return false;
        }
    }

    private Connection newConnection()
    {
        Connection connection = new Connection(_config.baseUrl(), new ApiKeyCredentialsProvider(_config.credential()));
        // The client API defaults to 60s, which several LabKey queries in these workloads exceed; a timed-out request
        // is recorded as an error while the server keeps running the query, so the run measures load it cannot see.
        connection.setTimeout(_config.requestTimeoutSeconds() * 1000);
        if (_config.impersonates())
        {
            impersonate(connection);
        }
        return connection;
    }

    /**
     * Clears named server caches so a run starts from a known cold state. The one that matters here is
     * "materialized sample types": dropping it forces the next read of a sample type to rebuild its temp table, which
     * is the transition the materialization probe measures.
     * <p>
     * 'admin-clearCaches' is an {@code @AdminConsoleAction}, so it is POSTed to the root container. It binds
     * 'debugName' from <em>form-encoded</em> parameters -- a JSON body leaves the field null, whereupon the action
     * clears nothing and still reports success. Hence {@link ApiTestCommand} with "post_form" rather than
     * {@link SimplePostCommand}.
     */
    private void clearCaches(Connection connection)
    {
        for (String name : _config.clearCachesBeforeRun())
        {
            ApiTestCommand command = new ApiTestCommand(
                    "admin-clearCaches.api",
                    "post_form",
                    "debugName=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
            try
            {
                command.execute(connection);
            }
            catch (CommandException e)
            {
                if (e.getStatusCode() == 401 || e.getStatusCode() == 403)
                {
                    throw new IllegalStateException(("Not permitted to clear caches. 'admin-clearCaches' needs "
                            + "AdminPermission in the root container, and this session has %s. Either grant it, or "
                            + "drop 'clearCachesBeforeRun' and have an admin clear the cache by hand (Admin Console > "
                            + "Caches) immediately before the run.")
                            .formatted(_config.impersonates() ? "only what its impersonation grants" : "no impersonation"), e);
                }
                throw new RuntimeException("Failed to clear cache '" + name + "': " + e.getMessage(), e);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to clear cache '" + name + "'", e);
            }
            TestLogger.log("Cleared cache '%s'".formatted(name));
        }
    }

    /**
     * Checks each configured cache name against the Cache Statistics page so a typo is visible before the run
     * rather than after it. A GET of an admin console action needs only Troubleshooter.
     * <p>
     * A name that is absent is reported but does not stop the run: caches are created lazily, so one that nothing
     * has touched since the last restart is legitimately missing from the page and will exist by the time the
     * workload asks for it.
     */
    private void verifyCacheNames(Connection connection)
    {
        String html;
        try
        {
            html = new ApiTestCommand("admin-caches.view", "get", "").execute(connection).getText();
        }
        catch (CommandException | IOException e)
        {
            TestLogger.warn("Could not read the Cache Statistics page to verify cache names: " + e.getMessage());
            return;
        }

        List<String> missing = _config.clearCachesBeforeRun().stream().filter(name -> !html.contains(name)).toList();
        if (!missing.isEmpty())
        {
            TestLogger.warn(("No cache is currently named %s. That is expected for a cache nothing has used since "
                    + "the last restart, since they are created on first use -- but it is also what a typo looks "
                    + "like. Check the exact debugName on Admin Console > Caches if the run measures a warm server.")
                    .formatted(missing));
        }
        List<String> present = _config.clearCachesBeforeRun().stream().filter(html::contains).toList();
        if (!present.isEmpty())
        {
            TestLogger.log("Cache name(s) verified against the server: " + String.join(", ", present));
        }
    }

    /**
     * Adopts a group's permissions for this session. A troubleshooter-style credential can reach the API but sees none
     * of the data without this, so every simulation's connection needs it, not just the preflight one.
     */
    private void impersonate(Connection connection)
    {
        try
        {
            if (!_config.impersonateRoles().isEmpty())
            {
                String[] roleNames = resolveImpersonationRoles(connection);
                new ImpersonateRolesCommand(roleNames).execute(connection, _config.impersonateContainer());
            }
            else
            {
                SimplePostCommand command = new SimplePostCommand("user", "impersonateGroup");
                JSONObject json = new JSONObject();
                json.put("groupId", resolveImpersonationGroupId(connection));
                command.setJsonObject(json);
                command.execute(connection, _config.impersonateContainer());
            }
        }
        catch (CommandException e)
        {
            // Sessions are per-connection, so this means the connection was reused rather than that anything is wrong
            if (e.getMessage() != null && e.getMessage().contains("already impersonating"))
            {
                return;
            }
            throw new RuntimeException("Failed to impersonate %s in %s: %s".formatted(
                    _config.impersonateRoles().isEmpty() ? "group '" + _config.impersonateGroup() + "'" : _config.impersonateRoles(),
                    _config.impersonateContainer(), e.getMessage()), e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to impersonate", e);
        }
    }

    /**
     * Resolves the configured group name against the groups this credential is actually allowed to impersonate, so a
     * name that cannot be impersonated fails with the list of ones that can.
     */
    private int resolveImpersonationGroupId(Connection connection) throws IOException, CommandException
    {
        if (_config.impersonateGroupId() != null)
        {
            return _config.impersonateGroupId();
        }
        if (_impersonationGroupId != null)
        {
            return _impersonationGroupId;
        }

        CommandResponse response = new SimplePostCommand("user", "getImpersonationGroups")
                .execute(connection, _config.impersonateContainer());
        List<Map<String, Object>> groups = response.getProperty("groups");

        List<String> available = new ArrayList<>();
        for (Map<String, Object> group : groups)
        {
            // Site groups are reported as 'Site: <name>'; accept either form
            String displayName = String.valueOf(group.get("displayName"));
            available.add(displayName);
            if (displayName.equals(_config.impersonateGroup())
                    || displayName.equals("Site: " + _config.impersonateGroup()))
            {
                _impersonationGroupId = ((Number) group.get("groupId")).intValue();
                TestLogger.log("Impersonating '%s' (group %d) in %s"
                        .formatted(displayName, _impersonationGroupId, _config.impersonateContainer()));
                return _impersonationGroupId;
            }
        }
        throw new IllegalStateException("Group '%s' is not impersonatable in %s (found %s).%s"
                .formatted(_config.impersonateGroup(), _config.impersonateContainer(), available,
                        describeImpersonationOptions(connection)));
    }

    private String[] resolveImpersonationRoles(Connection connection) throws IOException, CommandException
    {
        List<Map<String, Object>> roles = new SimplePostCommand("user", "getImpersonationRoles")
                .execute(connection, _config.impersonateContainer()).getProperty("roles");

        List<String> resolved = new ArrayList<>();
        for (String wanted : _config.impersonateRoles())
        {
            roles.stream()
                    .filter(r -> wanted.equals(r.get("displayName")) || wanted.equals(r.get("roleName")))
                    .findFirst()
                    .ifPresentOrElse(
                            r -> resolved.add(String.valueOf(r.get("roleName"))),
                            () -> {
                                throw new IllegalStateException("Role '%s' is not impersonatable in %s.%s".formatted(
                                        wanted, _config.impersonateContainer(), describeImpersonationOptions(connection)));
                            });
        }
        TestLogger.log("Impersonating roles %s in %s".formatted(resolved, _config.impersonateContainer()));
        return resolved.toArray(new String[0]);
    }

    /**
     * Everything this credential may impersonate, so a misconfigured name reports the valid alternatives instead of
     * requiring another round trip. Group lists are per-project, so the root container is reported too: it is a common
     * mistake to point at a container that has no project and therefore offers no groups.
     */
    private String describeImpersonationOptions(Connection connection)
    {
        StringBuilder detail = new StringBuilder("\n\n  Impersonation options for this credential:");
        for (String container : new String[]{_config.impersonateContainer(), "/"})
        {
            detail.append("\n    container ").append(container).append(':');
            appendOptions(detail, connection, container, "getImpersonationGroups", "groups", "displayName");
            appendOptions(detail, connection, container, "getImpersonationRoles", "roles", "displayName");
        }
        return detail.append("\n\n  Set impersonate.group, or impersonate.roles, to one of the above.").toString();
    }

    private void appendOptions(StringBuilder detail, Connection connection, String container, String action,
                               String property, String nameKey)
    {
        try
        {
            List<Map<String, Object>> items = new SimplePostCommand("user", action)
                    .execute(connection, container).getProperty(property);
            List<String> names = items.stream().map(i -> String.valueOf(i.get(nameKey))).toList();
            detail.append("\n      ").append(property).append(": ").append(names.isEmpty() ? "(none)" : names);
        }
        catch (IOException | CommandException e)
        {
            detail.append("\n      ").append(property).append(": lookup failed (").append(e.getMessage()).append(')');
        }
    }

    private List<Simulation.Definition> buildDefinitions(Map<String, String> replacements)
    {
        List<Simulation.Definition> definitions = new ArrayList<>();
        for (int i = 0; i < _config.sessions(); i++)
        {
            definitions.add(new Simulation.Definition(this::newConnection)
                    .setActivityFiles(_config.activities().toArray(File[]::new))
                    .setReplacements(replacements)
                    .setDelayBetweenActivities(_config.delayBetweenActivitiesMillis())
                    .setMaxActivityThreads(_config.maxActivityThreads())
                    // A stopped session finishes its in-flight requests first, so anything shorter than the request
                    // timeout gives up on a session that is merely slow rather than stuck
                    .setShutdownGrace(Duration.ofSeconds(_config.requestTimeoutSeconds() + 60))
                    .setRunOnce(_config.runOnce()));
        }
        return definitions;
    }

    /**
     * @return true if the load ran its full duration and every session then reported its results
     */
    private boolean runScenario(AbstractScenario<?> scenario, Duration duration, @Nullable StabilityWatcher stability) throws InterruptedException
    {
        scenario.setBaselineDataCollectionDuration(Duration.ZERO);
        scenario.startScenario();
        boolean completed = false;
        try
        {
            completed = generateLoad(scenario, duration, stability);
        }
        finally
        {
            completed &= stopAndCollect(scenario);
        }
        return completed;
    }

    /**
     * @return true if the load ran to its intended end
     */
    private boolean generateLoad(AbstractScenario<?> scenario, Duration duration, @Nullable StabilityWatcher stability) throws InterruptedException
    {
        if (!_config.runOnce())
        {
            Instant end = Instant.now().plus(duration);
            while (Instant.now().isBefore(end))
            {
                if (stability != null && stability.isStable())
                {
                    TestLogger.log("Latency has been flat for %s; stopping early with %ds of the window unused."
                            .formatted(stability.describe(), Duration.between(Instant.now(), end).toSeconds()));
                    return true;
                }
                Thread.sleep(1_000);
            }
            return true;
        }

        // Each session runs every activity once and stops on its own, so the run is defined by the work rather
        // than by the clock; 'duration' is only a safety cap. Waiting here keeps the measured window equal to
        // the work, which is what makes two runs of the same config comparable.
        Instant deadline = Instant.now().plus(duration);
        while (!scenario.allSimulationsFinished())
        {
            if (Instant.now().isAfter(deadline))
            {
                TestLogger.warn(("Sessions had not finished their pass after %ds; stopping them. Raise "
                        + "'durationSeconds' -- with 'runOnce' it is a cap, and a truncated pass is not "
                        + "comparable to a complete one.").formatted(duration.toSeconds()));
                return false;
            }
            Thread.sleep(1_000);
        }
        return true;
    }

    /**
     * Collecting results can fail even when the load itself was clean: stopping a session is cooperative, so a
     * session mid-activity does not notice until its in-flight requests return. Reporting that rather than throwing
     * keeps the manifest and both TSVs, which already hold every request that finished.
     *
     * @return true if every session stopped and reported within its shutdown grace
     */
    private boolean stopAndCollect(AbstractScenario<?> scenario) throws InterruptedException
    {
        try
        {
            scenario.finishScenario();
            return true;
        }
        catch (RuntimeException e)
        {
            TestLogger.warn("Results are incomplete -- the last requests of a session are missing: " + e.getMessage());
            return false;
        }
    }

    private void writeManifest(Map<String, String> replacements, boolean miniProfilerAvailable, Instant start,
                               Instant end, boolean completed, File clientResults, File miniProfilerResults) throws IOException
    {
        JSONArray activities = new JSONArray();
        for (File activity : _config.activities())
        {
            activities.put(new JSONObject()
                    .put("file", activity.getName())
                    .put("sha256", sha256(activity)));
        }

        JSONObject manifest = new JSONObject()
                .put("runUuid", _runUuid)
                .put("scenarioName", _config.scenarioName())
                .put("baseUrl", _config.baseUrl())
                .put("sessions", _config.sessions())
                .put("durationSeconds", _config.durationSeconds())
                .put("warmupSeconds", _config.warmupSeconds())
                .put("delayBetweenActivitiesMillis", _config.delayBetweenActivitiesMillis())
                .put("maxActivityThreads", _config.maxActivityThreads())
                .put("requestTimeoutSeconds", _config.requestTimeoutSeconds())
                .put("runOnce", _config.runOnce())
                .put("stopWhenStable", _config.stopWhenStable() == null ? JSONObject.NULL
                        : new JSONObject().put("samples", _config.stopWhenStable().samples())
                                .put("maxRatioToPeak", _config.stopWhenStable().maxRatioToPeak()))
                .put("clearCachesBeforeRun", new JSONArray(_config.clearCachesBeforeRun()))
                .put("activities", activities)
                .put("replacements", new JSONObject(replacements))
                .put("configUnderTest", new JSONObject(_config.configUnderTest()))
                .put("miniProfilerAvailable", miniProfilerAvailable)
                .put("completed", completed)
                .put("measuredStartEpochMillis", start.toEpochMilli())
                .put("measuredEndEpochMillis", end.toEpochMilli())
                .put("measuredStart", start.toString())
                .put("measuredEnd", end.toString())
                .put("clientResultsFile", clientResults.getName())
                .put("miniProfilerResultsFile", miniProfilerResults == null ? JSONObject.NULL : miniProfilerResults.getName());

        File manifestFile = new File(_config.resultsDir(), _config.scenarioName() + "-" + _runUuid + "-run.json");
        try (PrintWriter writer = new PrintWriter(manifestFile, StandardCharsets.UTF_8))
        {
            writer.println(manifest.toString(2));
        }
        TestLogger.log("Run manifest: " + manifestFile.getAbsolutePath());
    }

    private static String sha256(File file) throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file.toPath())));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Watches client-observed latency for the flat tail that means a materialization probe has nothing left to
     * measure: every one of the last {@code samples} requests at or under {@code maxRatioToPeak} of the slowest
     * request seen so far.
     * <p>
     * Relative to the observed peak rather than an absolute threshold, because the fast-path latency differs per
     * instance class and is the thing being measured. Requiring a run of samples rather than one is what keeps a
     * transient dip mid-build from ending the run early -- the observed build dips for three polls before its
     * slowest phase.
     */
    private static class StabilityWatcher
    {
        private final int _samples;
        private final double _maxRatioToPeak;
        private long _peakMillis;
        private int _consecutiveFast;

        StabilityWatcher(int samples, double maxRatioToPeak)
        {
            _samples = samples;
            _maxRatioToPeak = maxRatioToPeak;
        }

        synchronized void observe(Simulation.RequestResult result)
        {
            long millis = result.getDuration().toMillis();
            _peakMillis = Math.max(_peakMillis, millis);
            _consecutiveFast = millis <= _peakMillis * _maxRatioToPeak ? _consecutiveFast + 1 : 0;
        }

        synchronized boolean isStable()
        {
            return _consecutiveFast >= _samples;
        }

        synchronized String describe()
        {
            return "%d consecutive requests at or under %.0f%% of the %dms peak"
                    .formatted(_consecutiveFast, _maxRatioToPeak * 100, _peakMillis);
        }
    }

    /**
     * Mini-profiler results plus a client-observed timing for every request. The client rows are the only ones that
     * exist for requests the mini-profiler drops, and they stay comparable against runs where it is unavailable.
     */
    private static class TeedSessionScenario extends SessionDataScenario
    {
        private final RequestResultTsvWriter _clientWriter;
        private final StabilityWatcher _stability;

        TeedSessionScenario(List<Simulation.Definition> definitions, String scenarioName, File resultsDir, RequestResultTsvWriter clientWriter, @Nullable StabilityWatcher stability)
        {
            super(definitions, scenarioName, resultsDir);
            _clientWriter = clientWriter;
            _stability = stability;
        }

        @Override
        protected Simulation.ResultCollector<RequestInfo> getResultsCollectorForSimulation(Connection connection)
        {
            Simulation.ResultCollector<RequestInfo> delegate = super.getResultsCollectorForSimulation(connection);
            Map<String, String> metadata = getScenarioMetadata();

            return new Simulation.ResultCollector<>()
            {
                @Override
                public void submitResult(Simulation.RequestResult requestResult) throws InterruptedException
                {
                    _clientWriter.writeRow(requestResult, metadata);
                    if (_stability != null)
                    {
                        _stability.observe(requestResult);
                    }
                    delegate.submitResult(requestResult);
                }

                @Override
                public @NotNull Collection<RequestInfo> getResults()
                {
                    return delegate.getResults();
                }
            };
        }

        @Override
        protected void afterComplete()
        {
            super.afterComplete();
            _clientWriter.close();
        }
    }

    /**
     * Client-observed timings only, for servers without an accessible mini-profiler.
     */
    private static class ClientOnlyScenario extends AbstractScenario<Void>
    {
        private final RequestResultTsvWriter _clientWriter;
        private final StabilityWatcher _stability;

        ClientOnlyScenario(List<Simulation.Definition> definitions, String scenarioName, RequestResultTsvWriter clientWriter, @Nullable StabilityWatcher stability)
        {
            super(definitions, scenarioName, null);
            _clientWriter = clientWriter;
            _stability = stability;
        }

        @Override
        protected Simulation.ResultCollector<Void> getResultsCollectorForSimulation(Connection connection)
        {
            Map<String, String> metadata = getScenarioMetadata();

            return new Simulation.ResultCollector<>()
            {
                @Override
                public void submitResult(Simulation.RequestResult requestResult)
                {
                    _clientWriter.writeRow(requestResult, metadata);
                    if (_stability != null)
                    {
                        _stability.observe(requestResult);
                    }
                }

                @Override
                public @NotNull Collection<Void> getResults()
                {
                    return Collections.emptyList();
                }
            };
        }

        @Override
        protected void afterComplete()
        {
            _clientWriter.close();
        }
    }

    /**
     * Generates load without recording anything, for the warm-up pass whose whole point is that its numbers are
     * thrown away.
     */
    private static class DiscardedScenario extends AbstractScenario<Void>
    {
        DiscardedScenario(List<Simulation.Definition> definitions)
        {
            super(definitions, "warmup", null);
        }

        @Override
        protected Simulation.ResultCollector<Void> getResultsCollectorForSimulation(Connection connection)
        {
            return Simulation.RESULTS_NOOP;
        }
    }

    /**
     * Parameters for one measured run, read from JSON:
     * <pre>{@code
     * {
     *   "scenarioName": "rds-baseline",
     *   "baseUrl": "https://labkey.example.com",
     *   "sessions": 20,
     *   "durationSeconds": 900,
     *   "warmupSeconds": 300,
     *   "delayBetweenActivitiesMillis": 5000,
     *   "maxActivityThreads": 6,
     *   "requireMiniProfiler": false,
     *   "requestTimeoutSeconds": 300,
     *   "runOnce": false,
     *   "clearCachesBeforeRun": ["materialized sample types"],
     *   "resultsDir": "results",
     *   "replacements": { "PROJECT": "My%20Project", "FOLDER": "My%20Project/My%20Folder" },
     *   "configUnderTest": { "instanceClass": "db.r6g.2xlarge", "parameterGroup": "lk-pg16-baseline" },
     *   "impersonate": { "container": "/Home", "group": "Administrators" },
     *   // ...or impersonate roles instead: { "container": "/Home", "roles": ["Site Administrator"] }
     *   "activities": [
     *     { "file": "activities/sample-grid.xml" },
     *     { "file": "activities/dashboard-load.xml" }
     *   ]
     * }
     * }</pre>
     * Relative paths resolve against the config file's directory. The credential comes from the environment variable
     * named by {@link #CREDENTIAL_ENV} so it stays out of the config file and out of the run manifest.
     * <p>
     * {@code stopWhenStable} ends the measured run once client latency has been flat for a while, making
     * {@code durationSeconds} a cap rather than a fixed cost. It exists for the materialization probe, which has
     * nothing left to measure once the sample type is built; leave it out of any run whose numbers are aggregates
     * over the window.
     * <pre>{@code   "stopWhenStable": { "samples": 20, "maxRatioToPeak": 0.1 }}</pre>
     * <p>
     * Each session runs the activities in order. With {@code runOnce} it makes one pass and stops, and
     * {@code durationSeconds} becomes a safety cap rather than the run length.
     */
    /**
     * @param samples consecutive fast requests required before the run ends
     * @param maxRatioToPeak what counts as fast, as a fraction of the slowest request seen so far
     */
    public record StabilityStop(int samples, double maxRatioToPeak)
    {
        static @Nullable StabilityStop from(@Nullable JSONObject json)
        {
            if (json == null)
            {
                return null;
            }
            StabilityStop stop = new StabilityStop(json.optInt("samples", 20), json.optDouble("maxRatioToPeak", 0.1));
            if (stop.samples() < 2 || stop.maxRatioToPeak() <= 0 || stop.maxRatioToPeak() >= 1)
            {
                throw new IllegalArgumentException("'stopWhenStable' needs samples >= 2 and 0 < maxRatioToPeak < 1, got " + stop);
            }
            return stop;
        }
    }

    public record RunConfig(String scenarioName, String baseUrl, String credential, int sessions, int durationSeconds,
                            int warmupSeconds, int delayBetweenActivitiesMillis, int maxActivityThreads,
                            boolean requireMiniProfiler, int requestTimeoutSeconds, boolean runOnce, List<String> clearCachesBeforeRun, File resultsDir, Map<String, String> replacements,
                            Map<String, String> configUnderTest, @Nullable StabilityStop stopWhenStable,
                            List<File> activities,
                            String impersonateContainer, String impersonateGroup, Integer impersonateGroupId,
                            List<String> impersonateRoles,
                            boolean preflightOnly)
    {
        public boolean impersonates()
        {
            return impersonateGroup != null || impersonateGroupId != null || !impersonateRoles.isEmpty();
        }

        public static final String CREDENTIAL_ENV = "LABKEY_STRESS_APIKEY";

        public static RunConfig fromFile(File configFile) throws IOException
        {
            JSONObject json;
            try (InputStream in = Files.newInputStream(configFile.toPath()))
            {
                json = new JSONObject(new JSONTokener(in));
            }
            File configDir = configFile.getAbsoluteFile().getParentFile();

            String credential = System.getenv(CREDENTIAL_ENV);
            if (credential == null || credential.isBlank())
            {
                throw new IllegalArgumentException("Set the " + CREDENTIAL_ENV
                        + " environment variable to a LabKey API key for " + json.getString("baseUrl"));
            }

            List<File> activities = new ArrayList<>();
            JSONArray activityArray = json.getJSONArray("activities");
            for (int i = 0; i < activityArray.length(); i++)
            {
                JSONObject activity = activityArray.getJSONObject(i);
                File activityFile = resolve(configDir, activity.getString("file"));
                if (!activityFile.isFile())
                {
                    throw new IllegalArgumentException("Activity file not found: " + activityFile.getAbsolutePath());
                }
                activities.add(activityFile);
            }
            if (activities.isEmpty())
            {
                throw new IllegalArgumentException("No activities defined in " + configFile);
            }

            File resultsDir = resolve(configDir, json.optString("resultsDir", "results"));
            Files.createDirectories(resultsDir.toPath());

            JSONObject impersonate = json.optJSONObject("impersonate", new JSONObject());

            return new RunConfig(
                    json.getString("scenarioName"),
                    json.getString("baseUrl"),
                    credential,
                    json.optInt("sessions", 10),
                    json.getInt("durationSeconds"),
                    json.optInt("warmupSeconds", 0),
                    json.optInt("delayBetweenActivitiesMillis", 5_000),
                    json.optInt("maxActivityThreads", 6),
                    json.optBoolean("requireMiniProfiler", false),
                    json.optInt("requestTimeoutSeconds", 300),
                    json.optBoolean("runOnce", false),
                    toStringList(json.optJSONArray("clearCachesBeforeRun")),
                    resultsDir,
                    toStringMap(json.optJSONObject("replacements")),
                    toStringMap(json.optJSONObject("configUnderTest")),
                    StabilityStop.from(json.optJSONObject("stopWhenStable")),
                    activities,
                    impersonate.optString("container", "/Home"),
                    impersonate.has("group") ? impersonate.getString("group") : null,
                    impersonate.has("groupId") ? impersonate.getInt("groupId") : null,
                    toStringList(impersonate.optJSONArray("roles")),
                    json.optBoolean("preflightOnly", false));
        }

        private static File resolve(File configDir, String path)
        {
            File file = new File(path);
            return file.isAbsolute() ? file : new File(configDir, path);
        }

        private static List<String> toStringList(JSONArray json)
        {
            if (json == null)
            {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            json.forEach(v -> values.add(String.valueOf(v)));
            return List.copyOf(values);
        }

        private static Map<String, String> toStringMap(JSONObject json)
        {
            if (json == null)
            {
                return Map.of();
            }
            Map<String, String> map = new HashMap<>();
            json.keySet().forEach(key -> map.put(key, json.get(key).toString()));
            return Collections.unmodifiableMap(map);
        }
    }
}
