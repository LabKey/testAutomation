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
import org.labkey.test.util.core.webdav.WebDavUtils;

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
        WebDavUtils.WebDavUrlFactory davUrl = new WebDavUtils.WebDavLocalUrlFactory(getProjectName() + "/" + subfolder);

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
