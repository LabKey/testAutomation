/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.test;


public abstract class BaseBlueGreenTest extends BaseWebDriverTest
{
    protected boolean runValidationOnly()
    {
        if ((null == System.getProperty("testValidationOnly")) || (System.getProperty("testValidationOnly").toLowerCase().trim().equals("false")))
        {
            return false;
        }
        else
        {
            log("Only going to run the validation part of the tests.");
            return true;
        }
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        // Only run cleanup before the running a full test.
        if(!runValidationOnly() && !afterTest)
        {
            super.doCleanup(afterTest);
        }
    }

    protected String recordError(String msg)
    {
        log("****************************");
        log(msg);
        log("****************************");
        return msg + "\n";
    }

}
