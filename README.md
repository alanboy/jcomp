jcomp
=====

```
//define a global variable
def int myvar;  

//defining a method
def void #main(){
	
	//character not in alphabet
	//@

	//unfinished string
	String foo;
	//foo = "this string is missing ending quote

	
	//return from a void
	//return 3;

	//strong typed lang
	String foobar;
	int bar;
	//bar = foobar + bar;

	//using before declaring
	//undeclared = 9;
}

//functions with parameters
def int #adding( int a , int b ){

	//complex expressions	
	return a+b+1000-5-995;
}

def int #method(){
	//not an expression
	// return 3FF44;

	//complex call structures
	return #adding(1, 5); 
}
```
http://askubuntu.com/questions/454253/how-to-run-32-bit-app-in-ubuntu-64-bit
http://www.csee.umbc.edu/portal/help/nasm/sample.shtml

http://cs.lmu.edu/~ray/notes/x86assembly/


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

