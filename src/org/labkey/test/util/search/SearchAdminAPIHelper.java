package org.labkey.test.util.search;

import org.apache.http.HttpStatus;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public abstract class SearchAdminAPIHelper
{
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

    public static void setDirectoryType(DirectoryType type, WebDriver driver)
    {
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
    }

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
        NIOFSDirectory,
        SimpleFSDirectory
    }
}
