/*
 * Copyright (c) 2020 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.remoteapi.puppeteer;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class PuppeteerStatus
{
    final private boolean _isAvailable;
    final private boolean _pingSuccess;
    final private JSONObject _service;

    public PuppeteerStatus(JSONObject payload)
    {
        _isAvailable = payload.getBoolean("isAvailable");
        _pingSuccess = payload.getBoolean("pingSuccess");
        _service = new JSONObject(payload.get("service"));
    }

    public PuppeteerStatus(CommandResponse response)
    {
        this(new JSONObject(response.getParsedData().get("data")));
    }

    public boolean isAvailable()
    {
        return _isAvailable;
    }

    public boolean isPingSuccess()
    {
        return _pingSuccess;
    }

    public JSONObject getService()
    {
        return _service;
    }
}
