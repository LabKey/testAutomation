/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.core.webdav;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.core.webdav.WebDavUrlFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({InDevelopment.class})
public class WebDavPerfTest extends BaseWebDriverTest
{
    private final File FILE_1 = TestFileUtils.getSampleData("TargetedMS/SProCoPTutorial.zip");

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        WebDavPerfTest init = (WebDavPerfTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Test
    public void testLargeFileUploadPerf()
    {
    }

    @Test
    public void testManyFilesListingPerf() throws IOException
    {
        final int fileCount = 1000;
        final String subfolder = "manyFilesTestFolder";
        WebDavUrlFactory davUrl = WebDavUrlFactory.webDavUrlFactory(getProjectName() + "/" + subfolder);

        _containerHelper.createSubfolder(getProjectName(), subfolder);
        final Sardine sardine = SardineFactory.begin(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        log("Uploading files");

        Instant startTime = Instant.now();
        List<DavResource> list = sardine.list(davUrl.getPath(""));
        Duration duration = Duration.between(startTime, Instant.now());
        log("Listing " + fileCount + " files: " + duration);
        assertEquals("Uploaded files", fileCount + 1, list.size());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "WebDavPerfTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
