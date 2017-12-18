@echo off

set path=%CD%\nasm;%path%;

rd /s/q out
mkdir out

java -jar jc.jar  %1

if "%ERRORLEVEL%" NEQ "0" (
    echo "Compilation failed: %ERRORLEVEL%"
    goto Exit
)

cd out

nasm.exe -f win32 -o p.o out.windows.x86.asm

link /NOLOGO /SUBSYSTEM:console /ENTRY:start  /DEBUG /defaultlib:kernel32.lib p.o

cd ..
echo Done.
echo Your executable is her: out\p.exe

:Exit

