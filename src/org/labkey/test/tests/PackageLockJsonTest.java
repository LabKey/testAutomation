package org.labkey.test.tests;

import org.apache.commons.lang3.CharUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.stream.Stream;

@Category({})
public class PackageLockJsonTest
{
    private static final Set<String> ALLOWED_SOURCES = Set.of("registry.npmjs.org", "labkey.jfrog.io");
    private final Map<String, AtomicInteger> depCounts = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    @Test @Ignore("package-lock check seems sufficient")
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
        Assert.assertTrue("Bad sources: " + String.join("\n", errors), errors.isEmpty());
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
                    if (val.contains(":")) // URL, file, or workspace dependency
                    {
                        errors.add(key + " = " + val + " - " + packageJson);
                    }
                });
            }
            if (devDependencies != null)
            {
                devDependencies.keySet().forEach(key -> {
                    String val = devDependencies.getString(key);
                    if (val.contains(":")) // URL, file, or workspace dependency
                    {
                        errors.add(key + " = " + val + " - " + packageJson);
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
        Assert.assertTrue("Bad sources: " + errors, errors.isEmpty());
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
                    verifyPackage(packageName, packageJson, packageLock);
                }
            }
        }
    }

    private void verifyPackage(String packageName, JSONObject packageJson, File packageLock) throws URISyntaxException
    {
        String resolved = packageJson.optString("resolved");
        if (resolved.isBlank())
        {
            TestLogger.debug("Resolved field is blank for package " + packageName + " in " + packageLock.getAbsolutePath());

        }
        else
        {
            URI resolvedURL = new URI(resolved);
            String host = resolvedURL.getHost();
            if (!ALLOWED_SOURCES.contains(host))
            {
                String message = "Package " + packageName + " resolved to unrecognized host " + host + " in " + packageLock.getAbsolutePath();
                errors.add(message);
                TestLogger.error(message);
            }
            depCounts.computeIfAbsent(host, k -> new AtomicInteger()).incrementAndGet();
        }

        String version = packageJson.optString("version");
        if (version.isBlank() || !CharUtils.isAsciiNumeric(version.charAt(0)))
        {
            String message = "Package " + packageName + " has bad version [" + version + "] in " + packageLock.getAbsolutePath();
            errors.add(message);
            TestLogger.error(message);
        }

        JSONObject transitiveDeps = packageJson.optJSONObject("dependencies", new JSONObject());
        for (String tDep : transitiveDeps.keySet())
        {
            JSONObject packageJsonDep = transitiveDeps.optJSONObject(tDep);
            if (packageJsonDep != null)
            {
                verifyPackage(tDep, packageJsonDep, packageLock);
            }
            else
            {
                String tVer = transitiveDeps.optString(tDep);
                if (tVer == null || tVer.contains(":") && !tVer.startsWith("npm:")) // URL, file, or workspace dependency
                {
                    String message = "Package " + packageName + " has bad transitive dependency [" + tVer + "] in " + packageLock.getAbsolutePath();
                    errors.add(message);
                    TestLogger.error(message);
                }
            }
        }
    }
}
