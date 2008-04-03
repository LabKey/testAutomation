@echo off

if "%CC_HOME%" == "" (
    echo %TIME%: Environment variable 'CC_HOME' must be set. >> c:\cc_wrapper.log
    @echo on
    exit /b 1;
)
if "%CC_PROJECT_HOME%" == "" (
    echo %TIME%: Environment variable 'CC_PROJECT_HOME' must be set. >> c:\cc_wrapper.log
    @echo on
    exit /b 1;
)
if "%CC_PORT%" == "" (
    echo %TIME%: Environment variable 'CC_PORT' must be set. >> c:\cc_wrapper.log
    @echo on
    exit /b 1;
)
if "%CC_SERVER%" == "" (
    echo %TIME%: Environment variable 'CC_SERVER' must be set. >> c:\cc_wrapper.log
    @echo on
    exit /b 1;
)

pushd %CC_HOME%
echo %TIME%: Starting CruiseControl >> c:\cc_wrapper.log
echo %TIME%: CC_HOME:         %CC_HOME% >> c:\cc_wrapper.log
echo %TIME%: CC_PROJECT_HOME: %CC_PROJECT_HOME% >> c:\cc_wrapper.log
echo %TIME%: CC_PORT:         %CC_PORT% >> c:\cc_wrapper.log
echo %TIME%: CC_SERVER:       %CC_SERVER% >> c:\cc_wrapper.log

set /A CC_MPORT=(%CC_PORT%+1)
set STARTUP_COMMAND=cruisecontrol.bat -configfile %CC_PROJECT_HOME%\trunk\server\test\cruisecontrol\cruisecontrol-config.xml -webport %CC_PORT% -jmxport %CC_MPORT% >> c:\cc_wrapper.log
echo %TIME%: %STARTUP_COMMAND% >> c:\cc_wrapper.log
%STARTUP_COMMAND%
popd

@echo on