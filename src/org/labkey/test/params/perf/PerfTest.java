package org.labkey.test.params.perf;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.TestFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class PerfTest
{
    private final String testName;
    private final String comment;
    private final String notes;
    private final String folderArchive;
    private final List<String> sampleTypes;
    private final List<String> sourceTypes;
    private final List<String> assays;
    private final List<PerfScenario> scenarios;

    PerfTest(JSONObject json)
    {
        testName = json.getString("testName");
        comment = json.optString("comment");
        notes = json.optString("notes");
        folderArchive = json.optString("folderArchive");
        sampleTypes = extractNamesOfProjEntities(json, "sampleTypes");
        sourceTypes = extractNamesOfProjEntities(json, "sourceTypes");
        assays = extractNamesOfProjEntities(json, "assays");
        scenarios = extractPerfScenarios(json);
    }

    public String getTestName()
    {
        return testName;
    }

    public String getComment()
    {
        return comment;
    }

    public String getNotes()
    {
        return notes;
    }

    public String getFolderArchive()
    {
        return folderArchive;
    }

    public List<String> getSampleTypes()
    {
        return sampleTypes;
    }

    public List<String> getSourceTypes()
    {
        return sourceTypes;
    }

    public List<String> getAssays()
    {
        return assays;
    }

    public List<PerfScenario> getScenarios()
    {
        return scenarios;
    }

    public static List<PerfTest> loadPerfInfoFile(String sampleDataFilePath)
    {
        return loadPerfInfoFile(TestFileUtils.getSampleData(sampleDataFilePath));
    }

    public static List<PerfTest> loadPerfInfoFile(File perfInfoFile)
    {
        JSONArray jsonArray;
        try (Reader reader = Readers.getReader(perfInfoFile))
        {
            jsonArray = new JSONArray(new JSONTokener(reader));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        List<PerfTest> perfTests = new ArrayList<>(jsonArray.length());
        for (Object obj : jsonArray)
        {
            if (obj instanceof JSONObject perfTestJson)
            {
                perfTests.add(new PerfTest(perfTestJson));
            }
            else
            {
                throw new IllegalArgumentException("Improperly structured perf test JSON. Expected an array of JSON objects: " + perfInfoFile);
            }
        }
        return perfTests;
    }

    /**
     * Get the list of sample types, source types or assays used for the test.
     *
     * @param testInfoObject The json object for a test.
     * @param key The 'type' of entity (samp or source type or arrays) to read from the json object.
     * @return The names of the sample types or source types or arrays.
     */
    private static List<String> extractNamesOfProjEntities(JSONObject testInfoObject, String key)
    {

        List<String> arrayItems = new ArrayList<>();

        JSONArray jsonArray = testInfoObject.optJSONArray(key);

        if(null != jsonArray)
        {
            for (int i = 0; i < jsonArray.length(); i++)
            {
                arrayItems.add(jsonArray.getJSONObject(i).getString("name"));
            }

        }

        return arrayItems;
    }

    /**
     * Get the list of sample types, source types or assays used for the test.
     *
     * @param testInfoObject The json object for a test.
     * @return List of scenario objects
     */
    private static List<PerfScenario> extractPerfScenarios(JSONObject testInfoObject)
    {

        List<PerfScenario> arrayItems = new ArrayList<>();

        JSONArray jsonArray = testInfoObject.optJSONArray("scenarios");

        if(null != jsonArray)
        {
            for (int i = 0; i < jsonArray.length(); i++)
            {
                arrayItems.add(new PerfScenario(jsonArray.getJSONObject(i)));
            }

        }

        return arrayItems;
    }

}
