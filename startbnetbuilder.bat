@echo off

rem -------------------------------------------------------------------------------
rem Batch file that starts BayesNetBuilder
rem -------------------------------------------------------------------------------

rem -------------------------------------------------------------------------------
rem Log4j configuration file
rem -------------------------------------------------------------------------------

set ARGS=-Dlog4j.configuration=log.properties

rem -------------------------------------------------------------------------------
rem Memory extension, specific to Sun java
rem -------------------------------------------------------------------------------
set ARGS=%ARGS% -Xms128M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=512

set LOG_CONFIG=./config

rem -------------------------------------------------------------------------------
rem Jar files
rem -------------------------------------------------------------------------------

set JARS=
for %%G in (*.jar) do call append.bat %%G
for %%G in (lib/*.jar) do call append.bat lib/%%G

rem -------------------------------------------------------------------------------
rem BayesNetBuilder boot class
rem -------------------------------------------------------------------------------
set BOOT_CLASS=selrach.bnetbuilder.gui.NetworkBuilder

set cmd=java %ARGS% -classpath %JARS%;%LOG_CONFIG% %BOOT_CLASS%

echo %cmd%

%cmd%
