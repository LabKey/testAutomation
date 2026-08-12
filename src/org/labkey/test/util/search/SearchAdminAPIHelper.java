/*
 * Copyright (c) 2016-2026 LabKey Corporation
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
package org.labkey.test.util.search;

import org.apache.hc.core5.http.HttpStatus;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.Timer;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;

public abstract class SearchAdminAPIHelper
{

    @LogMethod(quiet = true)
    public static void purgeForContainer(String containerPath)
    {
        var cmd = new SimplePostCommand("search", "cancelIndexing");
        executeCommand(cmd, containerPath);
    }

    public static void waitForIndexer()
    {
        waitForIndexer(WAIT_FOR_PAGE);
    }

    /**
     * Wait for search indexer to be idle via SearchController.WaitForIndexerAction
     */
    @LogMethod(quiet = true)
    public static void waitForIndexer(int timeout)
    {
        // Invoke a special server action that waits until all previous indexer tasks are complete
        var cmd = new SimplePostCommand("search", "waitForIndexer");
        cmd.setTimeout(timeout);

       executeCommand(cmd, null);
    }

    public static void waitForIndexerBackground()
    {
        waitForIndexerBackground(WAIT_FOR_PAGE);
    }

    @LogMethod(quiet = true)
    public static void waitForIndexerBackground(int timeout)
    {
        // Invoke a special server action that waits until all previous indexer tasks are complete, even wait for background indexing tasks to complete (e.g. deleteContainer)
        var cmd = new SimplePostCommand("search", "waitForIndexer");
        cmd.setTimeout(timeout);
        cmd.setParameters(Map.of("priority", "idle"));

        executeCommand(cmd, null);
    }

    @LogMethod(quiet = true)
    public static void waitForSearchServiceBootstrap(Connection cn)
    {
        SimpleGetCommand command = new SimpleGetCommand("search", "json");
        command.setParameters(Map.of("q", "pinging to check server is started", "scope", "All"));
        Timer timer = new Timer(Duration.ofMinutes(3));
        do
        {
            try
            {
                CommandResponse response = command.execute(cn, "/");
                if (response.getStatusCode() == 200)
                    return; // Server is up, we can exit the loop
                else
                    throw new RuntimeException("Search service did not start properly");
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (CommandException e)
            {
                if (e.getStatusCode() == 404)
                    return; //Search service is not available.
                WebDriverWrapper.sleep(500); //poll the re-request
            }
        }
        while (!timer.isTimedOut());
    }

    private static void executeCommand(PostCommand<?> cmd, String containerPath)
    {
        try
        {
            var response = cmd.execute(WebTestHelper.getRemoteApiConnection(), containerPath);
            assertEquals("WaitForIndexer action timed out", HttpStatus.SC_OK, response.getStatusCode());
        } catch (Exception cmdException)
        {
            throw new RuntimeException("an error occurred while waiting for search indexing to finish", cmdException);
        }
    }

    @LogMethod(quiet = true)
    public static void startCrawler(WebDriver driver)
    {
        invokeAdminAction(driver, "start search crawler", Map.of("start", "true"));
    }

    @LogMethod(quiet = true)
    public static void pauseCrawler(WebDriver driver)
    {
        invokeAdminAction(driver, "pause search crawler", Map.of("pause", "true"));
    }

    @LogMethod(quiet = true)
    public static void hideSearchIcon(WebDriver driver)
    {
        invokeAdminAction(driver, "hide search icon", Map.of("hideSearchIcon", "true"));
    }

    @LogMethod(quiet = true)
    public static void showSearchIcon(WebDriver driver)
    {
        invokeAdminAction(driver, "show search icon", Map.of("showSearchIcon", "true"));
    }

    @LogMethod(quiet = true)
    public static void setDirectoryType(@LoggedParam DirectoryType type, WebDriver driver)
    {
        pauseCrawler(driver);
        invokeAdminAction(driver, "set search directoryType", Map.of("directory", "true", "directoryType", type.toString()));
        startCrawler(driver);
    }

    @LogMethod(quiet = true)
    public static void deleteIndex(WebDriver driver)
    {
        invokeAdminAction(driver, "delete search index", Map.of("delete", "true"));
    }

    private static void invokeAdminAction(WebDriver driver, String operation, Map<String, String> map)
    {
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.buildURL("search", "admin", map));
        request.copySession(driver);
        request.setRequestMethod("POST");
        try
        {
            SimpleHttpResponse response = request.getResponse();
            assertEquals("Failed to " + operation, HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to " + operation, e);
        }
    }

    public enum DirectoryType // From org.labkey.search.model.LuceneDirectoryType
    {
        Default,
        MMapDirectory,
        NIOFSDirectory
    }
}
