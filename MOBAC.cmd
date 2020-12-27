@Echo off
REM This file will start the TrekBuddy Atlas Creator with custom memory settings for
REM the JVM. With the below settings the heap size (Available memory for the application)
REM will range up to 1200 megabyte.

start javaw.exe -Xms64M -Xmx1200M -jar Mobile_Atlas_Creator.jar