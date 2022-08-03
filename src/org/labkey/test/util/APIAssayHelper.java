/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.AddXarFileCommand;
import org.labkey.remoteapi.assay.AssayListCommand;
import org.labkey.remoteapi.assay.AssayListResponse;
import org.labkey.remoteapi.assay.Batch;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.Run;
import org.labkey.remoteapi.assay.SaveAssayBatchCommand;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.InferDomainCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.ReactAssayDesignerPage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class APIAssayHelper extends AbstractAssayHelper
{

    public APIAssayHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @LogMethod(quiet = true)
    public ImportRunResponse importAssay(int assayID, File file, String projectPath, Map<String, Object> batchProperties)  throws CommandException, IOException
    {
        ImportRunCommand  irc = new ImportRunCommand(assayID, file);
        irc.setBatchProperties(batchProperties);
        irc.setTimeout(180000); // Wait 3 minutes for assay import
        return irc.execute(_test.createDefaultConnection(), "/" + projectPath);
    }

    @LogMethod(quiet = true)
    public ImportRunResponse importAssay(int assayID, String runFilePath, String projectPath, Map<String, Object> batchProperties)  throws CommandException, IOException
    {
        ImportRunCommand  irc = new ImportRunCommand(assayID);
        irc.setRunFilePath(runFilePath);
        irc.setBatchProperties(batchProperties);
        irc.setTimeout(180000); // Wait 3 minutes for assay import
        return irc.execute(_test.createDefaultConnection(), "/" + projectPath);
    }

    @LogMethod(quiet = true)
    public ImportRunResponse importAssay(int assayID, String runName, List<Map<String, Object>> dataRows, String projectPath,
                                         Map<String, Object> runProperties, Map<String, Object> batchProperties)  throws CommandException, IOException
    {
        ImportRunCommand  irc = new ImportRunCommand(assayID, dataRows);
        irc.setName(runName);
        irc.setProperties(runProperties);
        irc.setBatchProperties(batchProperties);
        irc.setTimeout(180000); // Wait 3 minutes for assay import
        return irc.execute(_test.createDefaultConnection(), "/" + projectPath);
    }

    @LogMethod(quiet = true)
    public ImportRunResponse importAssay(int assayID, String runName, File file, String projectPath,
                                         @Nullable Map<String, Object> runProperties, @Nullable Map<String, Object> batchProperties)  throws CommandException, IOException
    {
        ImportRunCommand  irc = new ImportRunCommand(assayID, file);
        irc.setName(runName);

        if(null != runProperties)
        {
            // The 'Comment' property needs to be set by calling setComment, it isn't set by having it in the
            // runProperties map and calling setProperties.
            // At the time of this change having a property 'Comment' in the map has no effect when
            // irc.setProperties is called. To be safe going to remove it from the collection, and so as not to impact
            // the calling function going to use a clone of the parameter passed in.

            runProperties = new HashMap<>(runProperties);
            if (runProperties.containsKey("Comment"))
            {
                irc.setComment(runProperties.get("Comment").toString());
                runProperties.remove("Comment");
            }
            if (runProperties.containsKey("name") && StringUtils.isBlank(runName))
            {
                irc.setName(runProperties.get("name").toString());
                runProperties.remove("name");
            }

            irc.setProperties(runProperties);
        }

        if(null != batchProperties)
            irc.setBatchProperties(batchProperties);

        irc.setTimeout(180000); // Wait 3 minutes for assay import
        return irc.execute(_test.createDefaultConnection(), "/" + projectPath);
    }

    @Override
    public void importAssay(String assayName, File file, String projectPath) throws CommandException, IOException
    {
        importAssay(assayName, file, projectPath, Collections.singletonMap("ParticipantVisitResolver", "SampleInfo"));
    }

    @Override
    public void importAssay(String assayName, File file, String projectPath, @Nullable Map<String, Object> batchProperties) throws CommandException, IOException
    {
        importAssay(getIdFromAssayName(assayName, projectPath), file, projectPath, batchProperties);
    }

    @Override
    public void importAssay(String assayName, File file, Map<String, Object> batchProperties, Map<String, Object> runProperties) throws CommandException, IOException
    {
        importAssay(assayName, "", file, _test.getCurrentContainerPath(), runProperties, batchProperties);
    }

    @LogMethod(quiet = true)
    public void importAssay(String assayName, String runName, File file, String projectPath,
                            @Nullable Map<String, Object> runProperties, @Nullable Map<String, Object> batchProperties)  throws CommandException, IOException
    {
        importAssay(getIdFromAssayName(assayName, projectPath), runName, file, projectPath, runProperties, batchProperties);
    }

    @Override
    public void uploadXarFileAsAssayDesign(File file, int pipelineCount)
    {
        uploadXarFileAsAssayDesign(file);
        PipelineStatusTable.viewJobsForContainer(_test, _test.getCurrentContainerPath());
        _test.waitForPipelineJobsToComplete(pipelineCount, "Uploaded file - " + file.getName(), false);
    }

    @Override
    public void uploadXarFileAsAssayDesign(File file, int pipelineCount, @Nullable String container)
    {
        throw new UnsupportedOperationException();
    }

    @LogMethod
    @Override
    public void uploadXarFileAsAssayDesign(@LoggedParam File file)
    {
        AddXarFileCommand addXarFileCommand = new AddXarFileCommand(file);
        Connection connection = _test.createDefaultConnection();

        try
        {
            addXarFileCommand.execute(connection, _test.getCurrentContainerPath());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to import XAR file", e);
        }
    }

    @Override
    public void uploadXarFileAsAssayDesign(File file, String container)
    {
        throw new UnsupportedOperationException();
    }

    public int getIdFromAssayName(String assayName, String projectPath)
    {
        return getIdFromAssayName(assayName, projectPath, true);
    }

    public int getIdFromAssayName(String assayName, String projectPath, boolean failIfNotFound)
    {
        AssayListCommand alc = new AssayListCommand();
        alc.setName(assayName);
        AssayListResponse alr = null;
        try
        {
            alr = alc.execute(_test.createDefaultConnection(), "/" + projectPath);
        }
        catch (CommandException | IOException e)
        {
            if (e.getMessage().contains("Not Found"))
            {
                if (failIfNotFound)
                    throw new AssertionError("Assay or project not found", e);
                return 0;
            }
            else
                throw new RuntimeException(e);
        }

        if (alr.getDefinition(assayName) == null)
        {
            if (failIfNotFound)
                fail("Assay not found: " + assayName);
            return 0;
        }
        return ((Long) alr.getDefinition(assayName).get("id")).intValue();
    }

    /**
     * For a given container get a list of the assays present.
     *
     * @param containerPath Container path.
     * @return A list of the names of assays that were found.
     * @throws IOException Can be thrown by the SelectRowsCommand.
     * @throws CommandException Can be thrown by the SelectRowsCommand.
     */
    public static List<String> getListOfAssayNames(String containerPath) throws IOException, CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("assay", "AssayList");
        cmd.setColumns(Arrays.asList("Name", "Description", "Type"));

        String formattedContainerPath = containerPath;
        if(!formattedContainerPath.startsWith("/"))
            formattedContainerPath = "/" + formattedContainerPath;

        List<String> resultData = new ArrayList<>();

        SelectRowsResponse response = cmd.execute(connection, formattedContainerPath);
        for(Row row : response.getRowset())
        {
            resultData.add(row.getValue("Name").toString());
        }

        return resultData;
    }

    public void saveBatch(String assayName, String runName, Map<String, Object> runProperties, List<Map<String, Object>> resultRows, String projectName) throws IOException, CommandException
    {
        int assayId = getIdFromAssayName(assayName, projectName);

        Batch batch = new Batch();
        List<Run> runs = new ArrayList<>();
        Run run = new Run();
        run.setName(runName);
        if (!runProperties.isEmpty())
            run.setProperties(runProperties);
        run.setResultData(resultRows);
        runs.add(run);
        batch.setRuns(runs);

        saveBatch(assayId, batch, projectName);
    }

    public void saveBatch(int assayId, Batch batch, String projectPath) throws IOException, CommandException
    {
        SaveAssayBatchCommand cmd = new SaveAssayBatchCommand(assayId, batch);
        cmd.setTimeout(180000); // Wait 3 minutes for assay import
        cmd.execute(_test.createDefaultConnection(), "/" + projectPath);
    }

    public Protocol createAssayDesignWithDefaults(String containerPath, String providerName, String assayName) throws IOException, CommandException
    {
        Connection connection = _test.createDefaultConnection();
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand(providerName);
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, containerPath);

        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName);
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(newAssayProtocol);
        ProtocolResponse saveProtocolResponse = saveProtocolCommand.execute(connection, containerPath);
        return saveProtocolResponse.getProtocol();
    }

    /**
     * Copy an assay design
     * @param assayId rowId of the existing assay design
     * @param containerPath container for the new assay design
     * @param assayName name of the new assay design
     * @return new assay protocol
     * @throws Exception might be thrown by the LabKey Java remote API
     */
    public Protocol copyAssayDesign(Integer assayId, String containerPath, String assayName) throws Exception
    {
        Connection connection = _test.createDefaultConnection();
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand(assayId, true);
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, containerPath);

        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName);
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(newAssayProtocol);
        ProtocolResponse saveProtocolResponse = saveProtocolCommand.execute(connection, containerPath);
        return saveProtocolResponse.getProtocol();
    }

    public void createAssayWithPlateSupport(String name)
    {
        ReactAssayDesignerPage assayDesigner = createAssayDesign("General", name);

        assayDesigner.setPlateMetadata(true);
        assayDesigner.clickFinish();
    }

    public String getPlateTemplateLsid(String folderPath) throws Exception
    {
        SelectRowsCommand selectRowsCmd = new SelectRowsCommand("assay.General", "PlateTemplate");
        selectRowsCmd.setColumns(List.of("Lsid"));

        SelectRowsResponse resp = selectRowsCmd.execute(_test.createDefaultConnection(), folderPath);

        return String.valueOf(resp.getRows().get(0).get("Lsid"));
    }

    public List<PropertyDescriptor> inferFieldsFromFile(File file, String containerPath) throws IOException, CommandException
    {
        return new InferDomainCommand(file, "Assay").execute(_test.createDefaultConnection(), containerPath)
                .getFields();
    }
}
