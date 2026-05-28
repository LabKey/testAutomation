/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.util.perf;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.query.ImportDataCommand;
import org.labkey.remoteapi.query.ImportDataResponse;
import org.labkey.test.TestFileUtils;
import org.labkey.test.params.perf.PerfScenario;
import org.labkey.test.stress.AbstractScenario;
import org.labkey.test.stress.RequestInfoTsvWriter;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TestDateUtils;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.Timer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class JsonPerfScenarioHelper
{
    private static final Map<String, String> SCHEMA_NAMES = Map.of("sourcetype", "exp.data", "sampletype", "exp.materials", "assay", "assay.General");

    private final String containerPath;
    private final Connection connection;
    private final Function<String, File> perfDataFileSupplier;
    private final Map<String, Integer> assayIds;
    private Function<Result, Result> resultHandler = Function.identity();
    private int importThreads = 3;

    public JsonPerfScenarioHelper(String containerPath, Connection connection, File perfDataDir) throws IOException, CommandException
    {
        this.containerPath = containerPath;
        this.connection = connection;
        this.perfDataFileSupplier = perfDataDir == null ? TestFileUtils::getSampleData : name -> new File(perfDataDir, name);
        this.assayIds = APIAssayHelper.getProtocolIds(containerPath, connection);
    }

    public JsonPerfScenarioHelper setResultHandler(Function<Result, Result> resultHandler)
    {
        this.resultHandler = resultHandler;
        return this;
    }

    public JsonPerfScenarioHelper setImportThreads(int importThreads)
    {
        this.importThreads = importThreads;
        return this;
    }

    @LogMethod
    public Map<String, Result> runPerfScenarios(List<PerfScenario> scenarios) throws Exception
    {
        final ExecutorService importExecutor = Executors.newFixedThreadPool(importThreads);

        try
        {
            Map<String, Future<Result>> futures = new LinkedHashMap<>();
            Integer expectedDuration = 0;
            for (PerfScenario perfScenario : scenarios)
            {
                futures.put(perfScenario.getName(), importExecutor.submit(() -> resultHandler.apply(startImport(perfScenario))));
                expectedDuration += perfScenario.getAverage();
            }
            importExecutor.shutdown();
            if (!importExecutor.awaitTermination(expectedDuration, TimeUnit.MILLISECONDS))
            {
                TestLogger.warn("Import timed out");
            }
            Map<String, Result> results = new LinkedHashMap<>();
            for (Map.Entry<String, Future<Result>> entry : futures.entrySet())
            {
                results.put(entry.getKey(), entry.getValue().get());
            }
            return results;
        }
        finally
        {
            importExecutor.shutdownNow();
        }
    }

    private Result startImport(PerfScenario perfScenario)
    {
        TestLogger.log("Starting perf scenario: [%s]".formatted(perfScenario.getName()));
        String schemaName = SCHEMA_NAMES.get(perfScenario.getType());
        if (schemaName == null)
        {
            throw new IllegalArgumentException("Unsupported scenario type '%s' in '%s'".formatted(perfScenario.getType(), perfScenario.getName()));
        }

        PostCommand<?> command;
        if (schemaName.startsWith("assay."))
        {
            String key = schemaName.split("\\.", 2)[1] + "." + perfScenario.getTypeName();
            Integer assayId = assayIds.get(key);
            if (assayId == null)
            {
                throw new IllegalArgumentException("'%s' not defined in '%s'. Found %s".formatted(key, containerPath, assayIds.keySet()));
            }
            command = new ImportRunCommand(assayId, perfDataFileSupplier.apply(perfScenario.getFileName()));
        }
        else
        {
            ImportDataCommand importDataCommand = new ImportDataCommand(schemaName, perfScenario.getTypeName());
            importDataCommand.setFile(perfDataFileSupplier.apply(perfScenario.getFileName()));
            importDataCommand.setInsertOption(ImportDataCommand.InsertOption.MERGE);

            command = importDataCommand;
        }

        command.setTimeout(Math.max(connection.getTimeout(), perfScenario.getAverage() * importThreads) * 2);
        Timer timer = new Timer();
        String msgSuffix = "";
        try
        {
            CommandResponse response = command.execute(connection, containerPath);
            if (response instanceof ImportDataResponse idr)
            {
                msgSuffix = "Imported %d rows".formatted(idr.getRowCount());
            }
            return new Result(perfScenario, response.getStatusCode(), timer);
        }
        catch (IOException e)
        {
            msgSuffix = "IOException: " + e.getMessage();
            return new Result(perfScenario, e, timer);
        }
        catch (CommandException e)
        {
            msgSuffix = e.getStatusCode() + " CommandException: " + e.getMessage();
            return new Result(perfScenario, e, timer);
        }
        finally
        {
            TestLogger.log("Finished perf scenario [%s] in %s. %s".formatted(perfScenario.getName(),
                    TestDateUtils.durationString(timer.elapsed()), msgSuffix));
        }
    }

    public static class Result
    {
        private final PerfScenario _scenario;
        private final Integer _statusCode;
        private final Exception _exception;
        private final LocalDateTime _startTime;
        private final Duration _duration;

        private Result(PerfScenario scenario, Integer statusCode, Exception exception, Timer timer)
        {
            _scenario = scenario;
            _statusCode = statusCode;
            _exception = exception;
            _startTime = timer.getStartTime();
            _duration = timer.elapsed();
        }

        public Result(PerfScenario scenario, Integer statusCode, Timer stopWatch)
        {
            this(scenario, statusCode, null, stopWatch);
        }

        public Result(PerfScenario scenario, Exception exception, Timer stopWatch)
        {
            this(scenario, exception instanceof CommandException ce ? ce.getStatusCode() : null, exception, stopWatch);
        }

        public PerfScenario getScenario()
        {
            return _scenario;
        }

        public Integer getStatusCode()
        {
            return _statusCode;
        }

        public Exception getException()
        {
            return _exception;
        }

        public LocalDateTime getStartTime()
        {
            return _startTime;
        }

        public Duration getDuration()
        {
            return _duration;
        }
    }

    public static class RequestInfoTsvWriterWrapper implements Function<JsonPerfScenarioHelper.Result, JsonPerfScenarioHelper.Result>
    {
        private final AbstractScenario.TsvResultsWriter<RequestInfo> _requestInfoTsvWriter;
        private final List<Map<String, ?>> _scenarioMetadata;

        @SafeVarargs
        public RequestInfoTsvWriterWrapper(AbstractScenario.TsvResultsWriter<RequestInfo> requestInfoTsvWriter, Map<String, ?>... scenarioMetadata)
        {
            _requestInfoTsvWriter = requestInfoTsvWriter;
            _scenarioMetadata = List.of(scenarioMetadata);
        }

        @Override
        public JsonPerfScenarioHelper.Result apply(JsonPerfScenarioHelper.Result result)
        {
            Map<String, Object> values = new HashMap<>();
            for (Map<String, ?> metadata : _scenarioMetadata)
            {
                values.putAll(metadata);
            }
            values.put(RequestInfoTsvWriter.REQUEST_URL, result.getScenario().getFileName());
            values.put(RequestInfoTsvWriter.DURATION, result.getDuration().toMillis());
            values.put(RequestInfoTsvWriter.START_TIME, result.getStartTime().toString());
            _requestInfoTsvWriter.writeRow(RequestInfo.BLANK, values);
            return result;
        }
    }
}
