/*
 * Copyright (c) 2011-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function r(msg, last) {
    var el = Ext.get('testDiv');
    el.update(el.dom.innerHTML + '<br/>' + msg);
    if (last) el.update(el.dom.innerHTML + '<br/>Finished DomainTests.');
}

function getSuccessHandler(domainDesign)
{
    var html = '';
    html += '<b>' + domainDesign.name + '</b><br> ';

    for (var i in domainDesign.fields)
    {
        html += '   ' + domainDesign.fields[i].name + '<br>';
    }
    r(html);

    LABKEY.Domain.save(saveHandler, saveErrorHandler, domainDesign, 'study', 'StudyProperties');
}

function getErrorHandler()
{
    r("Failed to get StudyProperties domain.");
    sampleSetDomainTypeTest();
}

function saveHandler()
{
    r("Updated StudyProperties domain.");
    sampleSetDomainTypeTest();
}
function saveErrorHandler()
{
    r("Failed to save StudyProperties domain.");
    sampleSetDomainTypeTest();
}

function sampleSetDomainTypeTest() {

    var NAME = 'AlternativeSet';

    function getSuccessHandler(dd) {
        r('The Sample Set \'' + NAME + '\' already exists.', true);
    }

    function getErrorHandler() {
        r('Did not find the \'' + NAME + '\' Sample Set.');

        // Try to create the domain
        var domainDesign = {
            name : NAME,
            description : 'A Sample Set created via the Client JavaScript API',
            fields : [{
                name : 'id',
                rangeURI : 'int'
            },{
                name : 'tag',
                rangeURI : 'string'
            }]
        };

        var domainOptions = {
            idCols : [0]
        };

        function createSuccessHandler() {
            r('Successfully created the \'' + NAME + '\' Sample Set.');
            LABKEY.Domain.get(function(_dd){

                _dd.description = 'An updated description via the Client JavaScript API';
                // Save the Domain
                LABKEY.Domain.save(function() {
                    r('Successfully updated the description.', true);
                },
                        function(response){
                            r('Failed to update the \'' + NAME + '\' Sample Set.' +
                                    '<br/><span class="labkey-error" style="color: red;">' +
                                    response.exception +
                                    (response['stackTrace'] != undefined ? ('<br/>' + response.stackTrace[0]) : '') +
                                    '</span>', true);
                        },
                        _dd, 'Samples', NAME);

            }, function(response) {
                r('Failed to update the \'' + NAME + '\' Sample Set.<br/><br/><span class="labkey-error" style="color: red;">' +
                        response.exception +
                        (response['stackTrace'] != undefined ? ('<br/>' + response.stackTrace[0]) : '') +
                        '</span>', true);
            }, 'Samples', NAME);
        }

        function createErrorHandler(response) {
            r('Failed to create the \'' + NAME + '\' Sample Set.<br/><br/><span class="labkey-error" style="color: red;">' +
                    response.exception +
                    (response['stackTrace'] != undefined ? ('<br/>' + response.stackTrace[0]) : '') +
                    '</span>', true);
        }

        LABKEY.Domain.create(createSuccessHandler, createErrorHandler,
                'sampleset', domainDesign, domainOptions, LABKEY.containerPath);
    }

    // See if the domain already exists
    LABKEY.Domain.get(getSuccessHandler, getErrorHandler, 'Samples', NAME);
}

// Start the test
LABKEY.Domain.get(getSuccessHandler, getErrorHandler, 'study', 'StudyProperties');
