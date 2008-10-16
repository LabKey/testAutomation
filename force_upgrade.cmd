@echo Endlessly upgrades your bootstrap server; use for stress testing the upgrade process.

:start
call ant -f ..\build.xml force_upgrade
call ant drt -Dtest=basic
goto :start
