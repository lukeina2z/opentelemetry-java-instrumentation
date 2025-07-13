@echo off
setlocal

call gradlew.bat assemble
if %errorlevel% neq 0 (
  echo ERROR: Gradle assemble failed.
  exit /b %errorlevel%
)

call gradlew.bat publishToMavenLocal
endlocal
