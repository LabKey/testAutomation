package org.labkey.test.tests;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Category({})
public class PackageLockJsonTest
{
    private static final Set<String> ALLOWED_SOURCES = Set.of("registry.npmjs.org", "labkey.jfrog.io");
    private static final Pattern STARTS_WITH_ALPHA = Pattern.compile("^[a-zA-Z]");
    private final Map<String, AtomicInteger> depCounts = new HashMap<>();
    private final List<String> badSources = new ArrayList<>();

    @Test
    public void packageJsonTest() throws Exception
    {
        Path modulesDir = new File(TestFileUtils.getLabKeyRoot(), "server/modules").toPath();
        PathMatcher packageJsonMatcher = FileSystems.getDefault().getPathMatcher("glob:**/package.json");

        try (Stream<Path> paths = Files.walk(modulesDir))
        {
            paths
                .filter(path -> !path.toString().contains("/.gradle/"))
                .filter(packageJsonMatcher::matches)
                .forEach(this::scanPackageJson);
        }
        Assert.assertTrue("Bad sources: " + String.join("\n", badSources), badSources.isEmpty());
    }

    public void scanPackageJson(Path packageJson)
    {
        TestLogger.log("Scanning " + packageJson);

        try (InputStream is = Files.newInputStream(packageJson))
        {
            JSONObject jsonObject = new JSONObject(new JSONTokener(is));
            JSONObject dependencies = jsonObject.optJSONObject("dependencies");
            JSONObject devDependencies = jsonObject.optJSONObject("devDependencies");
            if (dependencies != null)
            {
                dependencies.keySet().forEach(key -> {
                    String val = dependencies.getString(key);
                    if (STARTS_WITH_ALPHA.matcher(val).matches())
                    {
                        badSources.add(key + " = " + val + " - " + packageJson);
                    }
                });
            }
            if (devDependencies != null)
            {
                devDependencies.keySet().forEach(key -> {
                    String val = devDependencies.getString(key);
                    if (STARTS_WITH_ALPHA.matcher(val).matches())
                    {
                        badSources.add(key + " = " + val + " - " + packageJson);
                    }
                });
            }
        }
        catch (JSONException ex)
        {
            TestLogger.log("  JSONException: " + ex.getMessage());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading " + packageJson, e);
        }

    }

    @Test
    public void testPackageLockJson() throws Exception
    {
        File modulesDir = new File(TestFileUtils.getLabKeyRoot(), "server/modules");
        File[] files = modulesDir.listFiles();
        if (files == null)
        {
            throw new RuntimeException("No files found in modules directory: " + modulesDir.getAbsolutePath());
        }
        for (File file : files)
        {
            if (file.isDirectory())
            {
                if (new File(file, "module.properties").exists())
                    testModule(file);
                else
                    testModuleContainer(file);
            }
        }
        TestLogger.log("Package lock JSON dependencies:" + depCounts);
        Assert.assertTrue("Bad sources: " + badSources, badSources.isEmpty());
    }

    private void testModuleContainer(File moduleContainer) throws Exception
    {
        File[] modules = moduleContainer.listFiles();
        if (modules == null)
        {
            throw new RuntimeException("No files found in modules directory: " + moduleContainer.getAbsolutePath());
        }
        for (File module : modules)
        {
            if (module.isDirectory() && new File(module, "module.properties").isFile())
            {
                testModule(module);
            }
        }
    }

    private void testModule(File module) throws Exception
    {
        File packageLock = new File(module, "package-lock.json");
        if (packageLock.isFile())
        {
            TestLogger.log("Testing module: " + module.getAbsolutePath());

            JSONObject packages;
            try (Reader reader = Readers.getReader(packageLock))
            {
                JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
                packages = jsonObject.optJSONObject("packages");
                if (packages == null)
                    packages = jsonObject.getJSONObject("dependencies"); // old lockfile version
            }
            catch (JSONException e)
            {
                TestLogger.error("Testing module: " + module.getName() + " failed to parse package-lock.json: " + e.getMessage());
                return;
            }
            for (String packageName : packages.keySet())
            {
                if (!packageName.isBlank())
                {
                    JSONObject packageJson = packages.getJSONObject(packageName);
                    String resolved = packageJson.optString("resolved");
                    if (resolved.isBlank())
                    {
                        TestLogger.warn("Resolved field is blank for package " + packageName + " in " + packageLock.getAbsolutePath());
                        continue;
                    }
                    URL resolvedURL = new URL(resolved);
                    String host = resolvedURL.getHost();
                    if (!ALLOWED_SOURCES.contains(host))
                    {
                        String message = "Package " + packageName + " resolved to unrecognized host " + host + " in " + packageLock.getAbsolutePath();
                        badSources.add(message);
                        TestLogger.error(message);
                    }
                    depCounts.computeIfAbsent(host, k -> new AtomicInteger()).incrementAndGet();
                }
            }
        }
    }
}
