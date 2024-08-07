package org.labkey.test.util.perf;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ImportDataCommand;
import org.labkey.remoteapi.query.ImportDataResponse;
import org.labkey.test.TestFileUtils;
import org.labkey.test.params.perf.PerfScenario;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.Timer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
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

    public JsonPerfScenarioHelper(String containerPath, Connection connection, File perfDataDir)
    {
        this.containerPath = containerPath;
        this.connection = connection;
        this.perfDataFileSupplier = perfDataDir == null ? TestFileUtils::getSampleData : name -> new File(perfDataDir, name);
    }

    public JsonPerfScenarioHelper(String containerPath, Connection connection)
    {
        this(containerPath, connection, null);
    }

    public Map<String, Result> runPerfScenariosSerial(List<PerfScenario> scenarios) throws Exception
    {
        Map<String, Result> results = new LinkedHashMap<>();
        for (PerfScenario perfScenario : scenarios)
        {
            results.put(perfScenario.getName(), startImport(perfScenario, 1));
        }
        return results;
    }

    public Map<String, Result> runPerfScenarios(List<PerfScenario> scenarios, int importThreads) throws Exception
    {
        final ExecutorService importExecutor = Executors.newFixedThreadPool(importThreads);

        try
        {
            Map<String, Future<Result>> futures = new LinkedHashMap<>();
            Integer expectedDuration = 0;
            for (PerfScenario perfScenario : scenarios)
            {
                futures.put(perfScenario.getName(), importExecutor.submit(() -> startImport(perfScenario, importThreads)));
                expectedDuration += perfScenario.getAverage();
            }
            importExecutor.shutdown();
            importExecutor.awaitTermination(expectedDuration, TimeUnit.MILLISECONDS);
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

    private Result startImport(PerfScenario perfScenario, int importThreads)
    {
        TestLogger.log("Starting perf scenario: [%s]".formatted(perfScenario.getName()));
        String schemaName = SCHEMA_NAMES.get(perfScenario.getType());
        if (schemaName == null)
        {
            return null; // unsupported type
        }
        ImportDataCommand command = new ImportDataCommand(schemaName, perfScenario.getTypeName());
        command.setFile(perfDataFileSupplier.apply(perfScenario.getFileName()));
        command.setTimeout(Math.max(connection.getTimeout(), perfScenario.getAverage() * importThreads));
        Timer timer = new Timer(Duration.ofSeconds(0));
        String msgSuffix = "";
        try
        {
            ImportDataResponse response = command.execute(connection, containerPath);
            msgSuffix = "Imported %d rows".formatted(response.getRowCount());
            return new Result(perfScenario.getName(), response.getStatusCode(), timer.elapsed());
        }
        catch (IOException e)
        {
            msgSuffix = "IOException: " + e.getMessage();
            return new Result(perfScenario.getName(), e, timer.elapsed());
        }
        catch (CommandException e)
        {
            msgSuffix = e.getStatusCode() + " CommandException: " + e.getMessage();
            return new Result(perfScenario.getName(), e, timer.elapsed());
        }
        finally
        {
            TestLogger.log("Finished perf scenario [%s] in %s. %s".formatted(perfScenario.getName(),
                    timer.elapsed().toString().replace("PT", ""), msgSuffix));
        }
    }

    public static class Result
    {
        private final String _name;
        private final Integer _statusCode;
        private final Exception _exception;
        private final Duration _duration;

        public Result(String name, Integer statusCode, Duration duration)
        {
            _name = name;
            _statusCode = statusCode;
            _exception = null;
            _duration = duration;
        }

        public Result(String name, Exception exception, Duration duration)
        {
            _name = name;
            _statusCode = exception instanceof CommandException ce ? ce.getStatusCode() : null;
            _exception = exception;
            _duration = duration;
        }

        public String getName()
        {
            return _name;
        }

        public Integer getStatusCode()
        {
            return _statusCode;
        }

        public Exception getException()
        {
            return _exception;
        }

        public Duration getDuration()
        {
            return _duration;
        }
    }
}
