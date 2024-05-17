/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;

public abstract class SearchAdminAPIHelper
{

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

       executeWaitForIndexer(cmd);
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
        cmd.setParameters(Map.of("priority", "background"));

        executeWaitForIndexer(cmd);
    }

    private static void executeWaitForIndexer(PostCommand cmd)
    {
        try
        {
            var response = cmd.execute(WebTestHelper.getRemoteApiConnection(), null);
            assertEquals("WaitForIndexer action timed out", HttpStatus.SC_OK, response.getStatusCode());
        } catch (Exception cmdException)
        {
            throw new RuntimeException("an error occurred while waiting for search indexing to finish", cmdException);
        }
    }

    @LogMethod(quiet = true)
    public static void startCrawler(WebDriver driver)
    {
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.buildURL("search", "admin", Maps.of("start", "true")));
        request.copySession(driver);
        request.setRequestMethod("POST");
        try
        {
            SimpleHttpResponse response = request.getResponse();
            assertEquals("Failed to start search crawler", HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to start search crawler", e);
        }
    }

    @LogMethod(quiet = true)
    public static void pauseCrawler(WebDriver driver)
    {
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.buildURL("search", "admin", Maps.of("pause", "true")));
        request.copySession(driver);
        request.setRequestMethod("POST");
        try
        {
            SimpleHttpResponse response = request.getResponse();
            assertEquals("Failed to pause search crawler", HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to pause search crawler", e);
        }
    }

    @LogMethod(quiet = true)
    public static void setDirectoryType(@LoggedParam DirectoryType type, WebDriver driver)
    {
        pauseCrawler(driver);
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.buildURL("search", "admin",
                Maps.of("directory", "true", "directoryType", type.toString())));
        request.copySession(driver);
        request.setRequestMethod("POST");
        try
        {
            SimpleHttpResponse response = request.getResponse();
            assertEquals("Failed to set search directoryType", HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to set search directoryType", e);
        }
        startCrawler(driver);
    }

    @LogMethod(quiet = true)
    public static void deleteIndex(WebDriver driver)
    {
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.buildURL("search", "admin", Maps.of("delete", "true")));
        request.copySession(driver);
        request.setRequestMethod("POST");
        try
        {
            SimpleHttpResponse response = request.getResponse();
            assertEquals("Failed to delete search index", HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete search index", e);
        }
    }

    public enum DirectoryType // From org.labkey.search.model.LuceneDirectoryType
    {
        Default,
        MMapDirectory,
        NIOFSDirectory
    }
}
