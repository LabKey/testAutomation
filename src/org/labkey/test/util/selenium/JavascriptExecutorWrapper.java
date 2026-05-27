/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.util.selenium;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptKey;
import org.openqa.selenium.WebDriver;

import java.util.Set;

public class JavascriptExecutorWrapper implements JavascriptExecutor
{
    private final JavascriptExecutor _wrappedExecutor;

    public JavascriptExecutorWrapper(WebDriver driver)
    {
        _wrappedExecutor = (JavascriptExecutor) driver;
    }

    @Override
    public @Nullable Object executeScript(@Language("JavaScript") @NotNull String script, Object @NotNull ... args)
    {
        return _wrappedExecutor.executeScript(script, args);
    }

    @Override
    public @Nullable Object executeScript(@NotNull ScriptKey key, Object @NotNull ... args)
    {
        return _wrappedExecutor.executeScript(key, args);
    }

    /**
     * Wrapper for executing JavaScript through WebDriver and verifying return type.
     * @param <T> See {@link JavascriptExecutor#executeScript(java.lang.String, java.lang.Object...)} for valid return types
     */
    public <T> @Nullable T executeScript(@Language("JavaScript") String script, Class<T> expectedResultType, @Nullable Object... arguments)
    {
        return verifyType(expectedResultType, executeScript(script, arguments));
    }

    /**
     * Wrapper for synchronous execution of asynchronous JavaScript. This wrapper extracts the 'callback' from the argument list
     * See {@link JavascriptExecutor#executeAsyncScript(java.lang.String, java.lang.Object...)} for details
     */
    @Override
    public @Nullable Object executeAsyncScript(@Language("JavaScript") @NotNull String script, Object @NotNull ... arguments)
    {
        script = "var callback = arguments[arguments.length - 1];\n" + // See WebDriver documentation for details on injected callback
                "try {" +
                script +
                "} catch (error) { callback(error); }"; // ensure that the callback is invoked when an exception would otherwise prevent it
        return _wrappedExecutor.executeAsyncScript(script, arguments);
    }

    public <T> @Nullable T executeAsyncScript(@Language("JavaScript") String script, Class<T> expectedResultType, @Nullable Object... arguments)
    {
        return verifyType(expectedResultType, executeAsyncScript(script, arguments));
    }

    private <T> @Nullable T verifyType(Class<T> expectedResultType, @Nullable Object o)
    {
        if (o != null && !expectedResultType.isAssignableFrom(o.getClass()))
            throw new IllegalStateException("Script return wrong type. Expected '" + expectedResultType.getName() + "'. Got: " + o.getClass().getName() + ". Result: " + o);

        return (T) o;
    }

    @Override
    public @NotNull Set<ScriptKey> getPinnedScripts()
    {
        return _wrappedExecutor.getPinnedScripts();
    }

    @Override
    public void unpin(@NotNull ScriptKey key)
    {
        _wrappedExecutor.unpin(key);
    }

    @Override
    public @NotNull ScriptKey pin(@Language("JavaScript") @NotNull String script)
    {
        return _wrappedExecutor.pin(script);
    }
}
