/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.test.tests;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.UIContainerHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebDavTest extends BaseSeleniumWebTest
{
    final String PROJECT_NAME="WebDavTest";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }


    @Override
    protected void doTestSteps() throws Exception
    {
        if (!isLinkPresentWithText(getProjectName()))
            _containerHelper.createProject(getProjectName(), null);

        // including context Path
        String baseURL = getBaseURL();
        String testDirectory = "/_webdav/" + getProjectName() + "/@files/";
        String testURL = baseURL + testDirectory;
        beginAt(testDirectory + "?listing=html");

        assertTextNotPresent("testfile1");

        Sardine sardine = SardineFactory.begin(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        List<String> names = _listNames(sardine, testURL);
        Assert.assertEquals(1, names.size());
        Assert.assertFalse(names.contains("testfile1"));

        sardine.put(testURL + "testfile1", new byte[] {0x4D});
        refresh();
        assertTextPresent("testfile1");
        names = _listNames(sardine,testURL);
        Assert.assertEquals(2, names.size());
        Assert.assertTrue(names.contains("testfile1"));

        sardine.delete(testURL + "testfile1");
        refresh();
        assertTextNotPresent("testfile1");
        names = _listNames(sardine,testURL);
        Assert.assertEquals(1, names.size());
        Assert.assertFalse(names.contains("testfile1"));

        try
        {
            sardine.list(testURL + "nonexistant/");
            Assert.fail("Expected 404");
        }
        catch (IOException x)
        {
            Assert.assertTrue(x.toString().contains("404"));
        }
    }


    private List<String> _listNames(Sardine s, String path) throws IOException
    {
        ArrayList<String> names = new ArrayList<String>();
        for (DavResource r : s.list(path))
        {
            // ignore .deleted and .upload.log
            if (r.getName().startsWith("."))
                continue;
            names.add(r.getName());
        }
        return names;
    }


    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }


    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
