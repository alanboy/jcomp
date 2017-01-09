[![Build status](https://ci.appveyor.com/api/projects/status/ncqyco1mxbcok61p?svg=true)](https://ci.appveyor.com/project/alanboy/jcomp)

jcomp
=====

```
// Variables globales
def int myvar;

def int #main()
{
	// Declaracion de variables locales
	int index;
	int buffer[27]; // Arreglos !
	int temp;

	index = 0;
	while(index < 26) // Ciclos !
	{
		buffer[index] = index + 65;
		index = index + 1;
	}

	#rev(buffer, 25);
}

// Metodos con arreglos como argumentos
def void #rev(int [] arreglo, int tam)
{
	int index;
	int temp;
	int mitad;
	mitad = tam/2;
	index = 0;
	while(index < mitad)
	{
		temp = arreglo[index];
		arreglo[index] = arreglo[tam-index];
		arreglo[tam-index] = temp;
		index = index + 1;
	}
}

```

http://askubuntu.com/questions/454253/how-to-run-32-bit-app-in-ubuntu-64-bit
http://www.csee.umbc.edu/portal/help/nasm/sample.shtml

http://cs.lmu.edu/~ray/notes/x86assembly/

Debugging
=====
Para debuggear el compilador:
`jdb -sourcepath src  -classpath bin jcomp.JComp "tests\arreglos\arreglos.jc"`

How to See the Contents of Windows library (*.lib)
DUMPBIN /EXPORTS user32.lib | vim -


http://stackoverflow.com/questions/1023593/how-to-write-hello-world-in-assembler-under-windows

To run a 32-bit executable file on a 64-bit multi-architecture Ubuntu system, you have to add the i386 architecture and install the three library packages libc6:i386, libncurses5:i386, and libstdc++6:i386:

sudo dpkg --add-architecture i386

sudo apt-get update
sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386


=====

nasm -f elf hello.asm
ld -m elf_i386  -s -o a.out hello.o

elf_i386  -> cannot execute binary file: Exec format error
a.out: ELF 32-bit LSB  executable, Intel 80386, version 1 (SYSV), statically linked, stripped


 ELF 64-bit LSB  executable, x86-64, version 1 (SYSV), statically linked, stripped
 ELF 32-bit LSB  executable, Intel 80386, version 1 (SYSV), statically linked, stripped



Supported emulations: elf_x86_64 elf32_x86_64 elf_i386 i386linux elf_l1om elf_k1om i386pep i386pe

http://www.nasm.us/doc/nasmdoc9.html
https://www.cs.uaf.edu/2006/fall/cs301/support/x86/
http://matthieuhauglustaine.blogspot.com/
http://www.cavestory.org/guides/csasm/guide/structure_of_func.html
https://www.cs.uaf.edu/2006/fall/cs301/support/x86/

