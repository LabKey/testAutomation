/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
console.log("** evaluating: " + this['javax.script.filename']);
var LABKEY = require("labkey");
var Ext = require("Ext").Ext;

function doTest()
{
    var errors = [];

    var containerPath = "_ServerSideJavascriptTest";
    var userEmail = "securitytest@validation.test";
    var groupName = "validationTest group";
    var currentUser = LABKEY.Security.currentUser;
    var result;

    // Clean up in case previous test didn't delete container.
    LABKEY.Security.deleteContainer({
        containerPath: containerPath
    });

    // LABKEY.Security.currentUser
    if(!currentUser.isSystemAdmin)
        errors[errors.length] = Error("Security.currentUser = " + Ext.util.JSON.encode(currentUser));

    //LABKEY.Security.createContainer()
    var container = LABKEY.Security.createContainer({
        name: containerPath,
        containerPath: '/'
    });
    if( container.path !== "/" + containerPath )
        errors[errors.length] = new Error("Security.createContainer() = "+Ext.util.JSON.encode(container));

    // LABKEY.Security.createGroup()
    var group = LABKEY.Security.createGroup({
        groupName: groupName,
        containerPath: containerPath
    });
    if( group.name !== groupName )
        errors[errors.length] = new Error("Security.createGroup() = "+Ext.util.JSON.encode(group));

    // LABKEY.Security.createNewUser()
    var user = LABKEY.Security.createNewUser({
        email: userEmail,
        sendEmail: false,
        containerPath: containerPath
    });
    if( user.email !== userEmail )
        errors[errors.length] = new Error("Security.createNewUser() = "+Ext.util.JSON.encode(user));

    // LABKEY.Security.addGroupMembers()
    result = LABKEY.Security.addGroupMembers({
        groupId: group.id,
        principalIds: [user.userId],
        containerPath: containerPath
    });
    if(result.added != user.userId )
        errors[errors.length] = new Error("Security.addGroupMembers() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.getFolderTypes()
    result = LABKEY.Security.getFolderTypes({});
    if (!result.Collaboration)
        errors[errors.length] = new Error("Security.getFolderTypes() = " + Ext.util.JSON.encode(result));

    result = LABKEY.Security.getSecurableResources({
        containerPath: containerPath
    });
    if( typeof result === undefined )
        errors[errors.length] = new Error("Security.getSecurableResources() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.getGroupsForCurrentUser()
    result = LABKEY.Security.getGroupsForCurrentUser({
        containerPath: containerPath
    });

    if( !result.groups || result.groups.length < 3) // User should be in at least 3 groups (id): Guests (-3), All Site Users (-2), and Site Administrators (-1)
        errors[errors.length] = new Error("Security.getGroupsForCurrentUser() = "+Ext.util.JSON.encode(result));
    else
    {
        var success = false;
        var g = result.groups.shift();
        if (g.id === -4) // Developers group is fine, but optional... skip to Guests
            g = result.groups.shift();

        if (g.id === -3 && g.name === 'Guests')
        {
            g = result.groups.shift();
            if (g.id === -2 && g.name === 'All Site Users')
            {
                g = result.groups.shift();
                if (g.id === -1 && g.name === 'Site Administrators')
                    success = true;
            }
        }

        if (!success)
            errors[errors.length] = new Error("Security.getGroupsForCurrentUser() = "+Ext.util.JSON.encode(result));
    }

    // LABKEY.Security.getContainers()
    result = LABKEY.Security.getContainers({
        containerPath: containerPath
    });
    if( !result.id )
        errors[errors.length] = new Error("Security.getContainers() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.getGroupPermissions()
    result = LABKEY.Security.getGroupPermissions({
        containerPath: containerPath
    });
    if( !result.container || !result.container.groups )
        errors[errors.length] = new Error("Security.getGroupPermissions() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.getUsers()
    result = LABKEY.Security.getUsers({
        groupId: group.id,
        containerPath: containerPath
    });
    if( !result.users )
        errors[errors.length] = new Error("Security.getUsers() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.getUserPermissions()
    var permissions = LABKEY.Security.getUserPermissions({
        userEmail: userEmail,
        containerPath: containerPath
    });
    if( !permissions.container || !permissions.user || permissions.user.userId!=user.userId )
        errors[errors.length] = new Error("Security.getUserPermissions() = "+Ext.util.JSON.encode(permissions));

    // LABKEY.Security.hasPermission()
    result = LABKEY.Security.hasPermission({
        perms: permissions.container.permissions,
        perm: LABKEY.Security.permissions.read
    });
    if ( result !== 0 )
        errors[errors.length] = new Error("Security.hasPermission() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.getRoles()
    result = LABKEY.Security.getRoles({});
    if (!(result.permissions && result.permissions.length > 0))
        errors[errors.length] = new Error("Security.getRoles() = " + Ext.util.JSON.encode(result));

    // LABKEY.Security.removeGroupMembers()
    result = LABKEY.Security.removeGroupMembers({
        groupId: group.id,
        principalIds: [user.userId],
        containerPath: containerPath
    });
    if( !result.removed )
        errors[errors.length] = new Error("Security.removeGroupMembers() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.renameGroup()
    result = LABKEY.Security.renameGroup({
        groupId: group.id,
        newName: groupName + "Renamed",
        containerPath: containerPath
    });
    if( !result.success )
        errors[errors.length] = new Error("Security.renameGroup() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.deleteGroup()
    result = LABKEY.Security.deleteGroup({
        groupId: group.id,
        containerPath: containerPath
    });
    if( !result.deleted )
        errors[errors.length] = new Error("Security.deleteGroup() = "+Ext.util.JSON.encode(result));

    // LABKEY.Security.deleteContainer()
    result = LABKEY.Security.deleteContainer({
        containerPath: containerPath
    });
    if( Ext.util.JSON.encode(result) !== Ext.util.JSON.encode({}) )
        errors[errors.length] = new Error("Security.deleteContainer() = "+Ext.util.JSON.encode(result));

    if( errors.length > 0 )
        throw errors;
}
