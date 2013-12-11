/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");
var Ext = require("Ext").Ext;

function doTest()
{
    var userEmail = "messagetest@validation.test";
    // need a user to send email to/from
    LABKEY.Security.createNewUser({
        email: userEmail,
        sendEmail: false,
        containerPath: "/Shared/_junit"
    });

    var msg = LABKEY.Message.createMsgContent(LABKEY.Message.msgType.plain, "Hello World");
    var recipient = LABKEY.Message.createRecipient(LABKEY.Message.recipientType.to, userEmail);
    var response = LABKEY.Message.sendMessage({
        msgFrom:userEmail,
        msgRecipients:[recipient],
        msgContent:[msg]
    });
    if( !response.success )
        throw new Error("Message.sendMessage() = "+Ext.util.JSON.encode(response));
}
