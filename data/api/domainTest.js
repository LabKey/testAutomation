function getSuccessHandler(domainDesign)
{
    var html = '';

        html += '<b>' + domainDesign.name + '</b><br> ';
    for (var i in domainDesign.fields)
    {
        html += '   ' + domainDesign.fields[i].name + '<br>';
        }
        document.getElementById('testDiv').innerHTML = html;

        LABKEY.Domain.save(saveHandler, saveErrorHandler, domainDesign, 'study', 'StudyProperties');
    }

    function getErrorHandler()
    {
        document.getElementById('testDiv').innerHTML = "Failed to get StudyProperties domain";
    }

    function saveHandler()
    {
        document.getElementById('testDiv').innerHTML = "Updated StudyProperties domain";
    }
    function saveErrorHandler()
    {
        document.getElementById('testDiv').innerHTML = "Failed to save";
    }

    LABKEY.Domain.get(getSuccessHandler, getErrorHandler, 'study', 'StudyProperties');
