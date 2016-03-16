package org.labkey.test.util.search;

import org.apache.http.HttpStatus;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public abstract class SearchAdminAPIHelper
{
    public static void startCrawler()
    {
        try
        {
            // Invoke a special server action that waits until all previous indexer tasks are complete
            int response = WebTestHelper.getHttpPostResponse(WebTestHelper.buildURL("search", "admin", Maps.of("start", "1")));
            assertEquals("WaitForIndexer action timed out", HttpStatus.SC_MOVED_TEMPORARILY, response);
        }
        catch (IOException e)
        {
            throw new RuntimeException("startCrawler failed", e);
        }
    }

    public static void pauseCrawler()
    {
        try
        {
            // Invoke a special server action that waits until all previous indexer tasks are complete
            int response = WebTestHelper.getHttpPostResponse(WebTestHelper.buildURL("search", "admin", Maps.of("pause", "1")));
            assertEquals("WaitForIndexer action timed out", HttpStatus.SC_MOVED_TEMPORARILY, response);
        }
        catch (IOException e)
        {
            throw new RuntimeException("pauseSearchCrawler failed", e);
        }
    }

    public static void setDirectoryType(DirectoryType type)
    {
        try
        {
            // Invoke a special server action that waits until all previous indexer tasks are complete
            int response = WebTestHelper.getHttpPostResponse(WebTestHelper.buildURL("search", "admin",
                    Maps.of("directory", "1", "directoryType", type.toString())));
            assertEquals("WaitForIndexer action timed out", HttpStatus.SC_MOVED_TEMPORARILY, response);
        }
        catch (IOException e)
        {
            throw new RuntimeException("setDirectoryType failed", e);
        }
    }

    public static void deleteIndex()
    {
        try
        {
            // Invoke a special server action that waits until all previous indexer tasks are complete
            int response = WebTestHelper.getHttpPostResponse(WebTestHelper.buildURL("search", "admin",
                    Maps.of("delete", "1")));
            assertEquals("deleteIndex action timed out", HttpStatus.SC_MOVED_TEMPORARILY, response);
        }
        catch (IOException e)
        {
            throw new RuntimeException("setDirectoryType failed", e);
        }
    }

    public enum DirectoryType // From org.labkey.search.model.LuceneDirectoryType
    {
        MMapDirectory,
        NIOFSDirectory,
        SimpleFSDirectory
    }
}
