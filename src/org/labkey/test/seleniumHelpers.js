/*
 * Copyright (c) 2007-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/*
 * This file contains a set of helper functions that are injected into
 * the selenium frame at test startup. These make it somewhat easier
 * to run old LabKey tests and write new ones.
 */

/**
 * Return hrefs for all links on the current page as \n delimited string
 */

window.getLinkAddresses = function () {
        var links = selenium.browserbot.getCurrentWindow().document.links;
        var addresses = new Array();
        for (var i = 0; i < links.length; i++)
          addresses[i] = links[i].getAttribute('href');
        return addresses.join('\\n')
};

window.countLinksWithText = function (txt) {
    var doc = selenium.browserbot.getCurrentWindow().document;
    var count = 0;
    for (var i = 0; i < doc.links.length; i++) {
        if (doc.links[i].innerHTML && doc.links[i].innerHTML.indexOf(txt) >= 0)
            count++;
    }
    return count;
}

window.appendToFormField = function (fieldName, txt) {
    var doc = selenium.browserbot.getCurrentWindow().document;
    doc.forms[0][fieldName].value = doc.forms[0][fieldName].value + txt;
    return "OK";
}
"OK";

