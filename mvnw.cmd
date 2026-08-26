@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@ECHO OFF
SETLOCAL
SET "MAVEN_PROJECTBASEDIR=%~dp0"
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
IF NOT "%JAVA_HOME%"=="" GOTO javaHomeSet
SET "JAVA_EXE=java.exe"
%JAVA_EXE% -version >NUL 2>&1
IF "%ERRORLEVEL%"=="0" GOTO runMaven
ECHO Error: JAVA_HOME is not set and java.exe is not available on PATH. 1>&2
EXIT /B 1
:javaHomeSet
SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
IF EXIST "%JAVA_EXE%" GOTO runMaven
ECHO Error: JAVA_HOME points to an invalid Java installation: %JAVA_HOME% 1>&2
EXIT /B 1
:runMaven
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
IF EXIST "%WRAPPER_JAR%" GOTO launch
ECHO Error: Maven Wrapper JAR is missing: %WRAPPER_JAR% 1>&2
EXIT /B 1
:launch
"%JAVA_EXE%" %MAVEN_OPTS% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
EXIT /B %ERRORLEVEL%
