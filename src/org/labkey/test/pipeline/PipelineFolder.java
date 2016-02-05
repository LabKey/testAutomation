/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
package org.labkey.test.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;

public class PipelineFolder
{
    protected PipelineWebTestBase _test;
    protected String _folderName;
    protected String _folderType = "None";
    protected String[] _tabs = new String[0];
    protected String[] _webParts = new String[0];
    protected String _pipelinePath;
    protected MailSettings _mailSettings;

    public PipelineFolder(PipelineWebTestBase test,
                          String folderName,
                          String pipelinePath)
    {
        _test = test;
        _folderName = folderName;
        _pipelinePath = pipelinePath;
    }

    public String getFolderName()
    {
        return _folderName;
    }

    public String getPipelinePath()
    {
        return _pipelinePath;
    }

    public String getFolderType()
    {
        return _folderType;
    }

    public void setFolderType(String folderType)
    {
        _folderType = folderType;
    }

    public String[] getTabs()
    {
        return _tabs;
    }

    public void setTabs(String... tabs)
    {
        _tabs = tabs;
    }

    public String[] getWebParts()
    {
        return _webParts;
    }

    public void setWebParts(String... webParts)
    {
        _webParts = webParts;
    }

    public void setup()
    {
        String projectName = _test.getProjectName();
        _test._containerHelper.createProject(projectName, null);
        _test._containerHelper.createSubfolder(projectName, projectName, _folderName, _folderType, _tabs);

        for (String webPartName : _webParts)
            _test.addWebPart(webPartName);

        _test.pushLocation();
        setupPipeline();
        _test.popLocation();
    }

    protected void setupPipeline()
    {
        _test.log("Setup pipeline.");
        _test.clickButton("Setup");

        _test.log("Set pipeline root.");
        _test.setPipelineRoot(_pipelinePath);

        _test.submit();

        if (_mailSettings != null)
            _mailSettings.setup();
    }

    public MailSettings getMailSettings()
    {
        return _mailSettings;
    }

    public void setMailSettings(MailSettings mailSettings)
    {
        _mailSettings = mailSettings;
    }

    public void clean()
    {
        try
        {
            _test._containerHelper.deleteFolder(_test.getProjectName(), _folderName);
        }
        catch (Throwable ignored)
        {}
    }

    public static class MailSettings
    {
        private PipelineWebTestBase _test;
        private boolean _notifyOnSuccess;
        private boolean _notifyOwnerOnSuccess;
        private String[] _notifyUsersOnSuccess = new String[0];
        private boolean _notifyOnError;
        private boolean _notifyOwnerOnError;
        private String[] _notifyUsersOnError = new String[0];
        private String[] _escalateUsers = new String[0];

        public MailSettings(PipelineWebTestBase test)
        {
            _test = test;
        }

        protected void setup()
        {
            _test.log("Updating email settings");
            // Assumes the setup page is already active
            check("notifyOnSuccess", _notifyOnSuccess);
            if (_notifyOnSuccess)
            {
                check("notifyOwnerOnSuccess", _notifyOwnerOnSuccess);
                if (_notifyUsersOnSuccess != null)
                    _test.setFormElement(Locator.id("notifyUsersOnSuccess"), StringUtils.join(_notifyUsersOnSuccess, '\n'));
            }
            check("notifyOnError", _notifyOnError);
            if (_notifyOnError)
            {
                check("notifyOwnerOnError", _notifyOwnerOnError);
                if (_notifyUsersOnError != null)
                    _test.setFormElement(Locator.id("notifyUsersOnError"), StringUtils.join(_notifyUsersOnError, '\n'));
                if (_escalateUsers != null)
                    _test.setFormElement(Locator.id("escalationUsers"), StringUtils.join(_escalateUsers, '\n'));
            }
            _test.clickButton("Update");
        }

        private void check(String name, boolean check)
        {
            if (check)
                _test.checkCheckbox(Locator.checkboxByName(name));
            else
                _test.uncheckCheckbox(Locator.checkboxByName(name));
        }

        public boolean isNotifyOnSuccess()
        {
            return _notifyOnSuccess;
        }

        public boolean isNotifyOwnerOnSuccess()
        {
            return _notifyOwnerOnSuccess;
        }

        public String[] getNotifyUsersOnSuccess()
        {
            return _notifyUsersOnSuccess;
        }

        public void setNotifyOnSuccess(boolean notify, boolean owner, String... users)
        {
            _notifyOnSuccess = notify;
            if (notify)
            {
                _notifyOwnerOnSuccess = owner;
                _notifyUsersOnSuccess = users;
            }
        }

        public boolean isNotifyOnError()
        {
            return _notifyOnError;
        }

        public boolean isNotifyOwnerOnError()
        {
            return _notifyOwnerOnError;
        }

        public String[] getNotifyUsersOnError()
        {
            return _notifyUsersOnError;
        }

        public void setNotifyOnError(boolean notify, boolean owner, String... users)
        {
            _notifyOnError = notify;
            if (notify)
            {
                _notifyOwnerOnError = owner;
                _notifyUsersOnError = users;
            }
        }

        public String[] getEscalateUsers()
        {
            return _escalateUsers;
        }

        public void setEscalateUsers(String... users)
        {
            _escalateUsers = users;
        }
    }
}
