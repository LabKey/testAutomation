var console = require("console");

function beforeInsert(row, errors) {
    // This is intended to force a timeout exception:
    if (this.extraContext.scriptTimeout && !!this.extraContext.simulateScriptTimeout) {
        console.log("Simulating script timeout!")
        java.lang.Thread.sleep(100 + (this.extraContext.scriptTimeout * 1000))
    }
}