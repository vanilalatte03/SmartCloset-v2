@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%

if not defined GRADLE_USER_HOME set GRADLE_USER_HOME=%APP_HOME%\.gradle-user-home

if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java.exe
)

if defined JAVA_HOME if not exist "%JAVACMD%" (
  echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
  exit /b 1
)

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
