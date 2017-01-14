@echo off

mkdir formatt
copy p.o formatt
copy build.out formatt
copy out.valdosta.x86.asm formatt

dir /s /b /a-d formatt >files.txt
makecab /d "CabinetName1=formatt.cab" /f files.txt

copy disk1\formatt.cab formatt.cab

del /q /f files.txt
del /s/q formatt
