OVERVIEW
---------
Scriptpad is an ad hoc test tool used to test R report execution through the LabKey Javascript client API.  Scriptpad
is a file module so that it can also test module reports as well as database reports.  Within the reports/schemas
directory you'll see various R scripts for testing R, Rserve, and Knitr.

The Scriptpad webpart itself allows you to execute the report either as a script (using the LABKEY.Report.execute() API) or as
a report webpart.

Execute Function, Create Report Session, Delete Report Session, and List Report Session buttons only work if Rserve has
been enabled as an Experimental Features.

USING SCRIPTPAD
---------------
You can reference any R file that is in the resources/reports/schemas directory.  You must specify the full report name
in the 'script name:' text field.  For example, to run 'script2.r' enter the full name (with the .r extension) and then
hit one of the execute buttons.

If Rserve has been enabled and you have a Remote R Script engine configured then you can play with Report sessions.  To
create a report session, click the 'Create Report Session' button.  You can then use this in the 'report session:' text
field to execute a report within that report session.

If the 'report session:' text field is filled in with a valid report session then it can be deleted by using the
'Delete Report Session' button.  You should see the list of report sessions reflect the current state.

You can execute a function surfaced in the report session by putting the function name in the 'script name:' field.

R SCRIPT SAMPLES
----------------
script_r, script2.r, script3.r show various 'run of the mill' output parameters and input parameters.

script_rhtml.rhtml, script_rmd.rmd, kable.rmd test Knitr and client dependencies.

errOnly.r and consoleOnly.r test error and console output

script_rserve.r tests that a function ('helloWorld') can be invoked using 'Execute Function' if it is published in a report
session and that report session is used.  The metadata file script_rserve.report.xml adds the function to the whitelist.

To test execute function, you must:
- enable Rserve
- add a remote R engine configuration
- go to scriptpad (create a project with a folder type of Scriptpad and add the Scriptpad webpart in a portal page)
- create a report session
- fill in the 'report session:' text field with the created report session
- fill in the 'script name:' text field with the 'script_rserve.r'
- click 'execute as script'
- fill in the 'script name:' text field with 'helloWorld'
- be sure the 'report session:' text field is the same report session as the one you executed 'script_rserve.r' under
- click 'execute function'
- if successful you should a single json:jsonout parameter that says "hello!" and has the current time.

Note that script_r and script_rserve.r have Sys.sleep(10) in it.  This is just to show that report session sharing works.  The
first execution should take > 15 seconds while subsequent executions under the same session should be very quick since
the 'sharedSession' will exist in the environment.

DEPENDENCIES
-----------
Note that the KnitrReportTest depends on script_rhtml.rhtml, script_rmd.rmd, and kable.rmd reports so if you change these, you'll need
to update the KnitrReportTest.  At the time of this writing, no other tests depend on Scriptpad.
