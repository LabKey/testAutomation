@echo Endlessly bootstraps your bootstrap server and runs a couple DRT tests; use for stress testing the bootstrap process.

:start
call ant -f ..\build.xml stop_tomcat
call ant -f ..\build.xml bootstrap
call ant -f ..\build.xml start_tomcat
call ant drt -Dtest=basic
call ant drt -Dtest=xtandem
goto :start
