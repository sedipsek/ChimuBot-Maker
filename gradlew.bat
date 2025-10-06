@echo off
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set CLASSPATH=%CLASSPATH%;%APP_HOME%\gradle\wrapper\gradle-wrapper-shared-8.5.jar
set CLASSPATH=%CLASSPATH%;%APP_HOME%\gradle\wrapper\gradle-cli-8.5.jar
set CLASSPATH=%CLASSPATH%;%APP_HOME%\gradle\wrapper\gradle-base-annotations-8.5.jar
set CLASSPATH=%CLASSPATH%;%APP_HOME%\gradle\wrapper\gradle-files-8.5.jar
set CLASSPATH=%CLASSPATH%;%APP_HOME%\gradle\wrapper\gradle-functional-8.5.jar
set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m
if not defined JAVA_HOME goto findJavaFromPath
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto init
:findJavaFromPath
set JAVA_EXE=java.exe
:init
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
