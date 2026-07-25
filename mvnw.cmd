@ECHO OFF
SETLOCAL
SET BASE_DIR=%~dp0
SET PROPS=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties
FOR /F "tokens=1,* delims==" %%A IN (%PROPS%) DO IF "%%A"=="distributionUrl" SET DIST_URL=%%B
SET MAVEN_VERSION=3.9.11
IF "%MAVEN_USER_HOME%"=="" SET MAVEN_USER_HOME=%USERPROFILE%\.m2
SET CACHE_DIR=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%
SET MAVEN_HOME=%CACHE_DIR%\apache-maven-%MAVEN_VERSION%
IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  IF NOT EXIST "%CACHE_DIR%" MKDIR "%CACHE_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%CACHE_DIR%\maven.zip'; Expand-Archive -Force '%CACHE_DIR%\maven.zip' '%CACHE_DIR%'; Remove-Item '%CACHE_DIR%\maven.zip'"
)
CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
ENDLOCAL
