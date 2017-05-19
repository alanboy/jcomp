@echo off

rem
rem When handing Matt stuff, build for windows and 
rem run this script.
rem

mkdir formatt
copy out\\p.o formatt
copy out\\build.out formatt
copy out\\out.valdosta.x86.asm formatt

dir /s /b /a-d formatt > files.txt
makecab /d "CabinetName1=formatt.cab" /f files.txt

copy disk1\formatt.cab formatt.cab

del /q /f files.txt
del /s/q formatt

