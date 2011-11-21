set arg=%1

set arg=%arg:"=%

if NOT DEFINED JARS goto first
set JARS=%JARS%;%arg%
goto:eof

:first
set JARS=%arg%
