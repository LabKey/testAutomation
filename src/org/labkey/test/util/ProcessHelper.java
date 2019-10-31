package org.labkey.test.util;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ProcessHelper
{
    private final ProcessBuilder pb;
    private Duration timeout = Duration.ofSeconds(60);

    public ProcessHelper(String executable, String... args)
    {
        pb = new ProcessBuilder();
        pb.command(ArrayUtils.addAll(new String[]{executable}, args));
        pb.redirectErrorStream(true);
    }

    public ProcessHelper(File executable, String... args)
    {
        this(executable.toPath().normalize().toAbsolutePath().toString(), args);
    }

    public Map<String, String> environment()
    {
        return pb.environment();
    }

    public ProcessHelper setTimeout(@NotNull Duration timeout)
    {
        this.timeout = timeout;
        return this;
    }

    public String getProcessOutput() throws IOException
    {
        return getProcessOutput(false);
    }

    public String getProcessOutput(boolean logOutput) throws IOException
    {
        StringBuffer output = new StringBuffer();
        runProcess(output, logOutput);
        return output.toString();
    }

    /**
     * Adapted from `org.labkey.api.reports.ExternalScriptEngine#runProcess()`
     * Execute the external process in separate thread
     * @return the exit code for the invocation - 0 if the process completed successfully.
     */
    @LogMethod
    public int runProcess(StringBuffer output, boolean logOutput) throws IOException
    {
        TestLogger.log("Running process: '" + pb.command() + "'");
        Process proc;
        try
        {
            pb.redirectErrorStream(true);
            proc = pb.start();
        }
        catch (IOException ioe)
        {
            Map<String, String> env = pb.environment();
            throw new RuntimeException("Failed starting process '" + pb.command() + "'. " +
                    "(PATH=" + env.get("PATH") + ")", ioe);
        }

        // create thread pool for collecting the process output
        ExecutorService pool = Executors.newSingleThreadExecutor();

        // collect output using separate thread so we can enforce a timeout on the process
        Future<Integer> out = pool.submit(() -> {
            try (BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream(), Charset.defaultCharset())))
            {
                String line;
                int count = 0;
                while ((line = procReader.readLine()) != null)
                {
                    count++;
                    output.append(line);
                    output.append('\n');
                    if (logOutput)
                        TestLogger.log(line);
                }
                return count;
            }
        });

        try
        {
            if (!timeout.isZero())
            {
                if (!proc.waitFor(timeout.toNanos(), TimeUnit.NANOSECONDS))
                {
                    proc.destroyForcibly().waitFor();
                    String msg = "Process killed after exceeding timeout of " + timeout.toString();
                    output.append("\n");
                    output.append(msg);
                    TestLogger.error(msg);
                }
            }
            else
            {
                proc.waitFor();
            }

            int code = proc.exitValue();

            int count = out.get();

            return code;
        }
        catch (InterruptedException ei)
        {
            throw new RuntimeException("Interrupted process for '" + pb.command() + " in " + pb.directory() + "'.", ei);
        }
        catch (ExecutionException ex)
        {
            // Exception thrown in output collecting thread
            Throwable cause = ex.getCause();
            if (cause instanceof IOException)
                throw new IOException("Failed writing output for process in '" + pb.directory().getPath() + "'.", cause);

            throw new RuntimeException(cause);
        }
        finally
        {
            if (proc.isAlive())
                proc.destroyForcibly();
        }
    }
}
