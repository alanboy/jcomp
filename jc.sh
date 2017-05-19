@echo off

rm -rf out
mkdir out

java -cp bin jcomp.JComp %1

#if "%ERRORLEVEL%" NEQ "0" (
#    echo "Compilation failed: %ERRORLEVEL%"
#    goto Exit
#)

cd out
nasm -f elf -o p.out out.linux.x86.asm
ld -m elf_i386 -s -o a.out p.out

cd ..
echo Done.

