package org.labkey.test;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: Feb 8, 2007
 * Time: 4:26:59 PM
 */
public interface WebTest
{
    File dumpHtml(File failureDumpDir);
    String getResponseText();
    int getResponseCode();
    void beginAt(String url);
    void log(String str);
    URL getURL() throws MalformedURLException;
    String[] getLinkAddresses();
    List<String> getCreatedProjects();
    String getAssociatedModuleDirectory();
}
