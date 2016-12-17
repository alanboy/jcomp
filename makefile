
EXECUTABLE_NAME=jcomp
BIN_DIR=bin
TEST_DIR=tests


#ifeq ($(OS),Windows_NT)
CMD_DEL=del
CMD_DEL_RECURSIVO=del /s/q
CMD_FIND_JAVA=dir /s/b src\*.java 
CMD_LINK=link /NOLOGO /SUBSYSTEM:console /ENTRY:start  /DEBUG /defaultlib:kernel32.lib p.o
CMD_RUN=p.exe
OUTPUT_ASM=out.windows.x86.asm
CMD_ASSEMBLE=nasm.exe -f win32 -o p.o $(OUTPUT_ASM)
#else
#CMD_DEL=rm
#CMD_DEL_RECURSIVO=rm -rf
#CMD_FIND_JAVA=find src/ | grep java
#CMD_LINK=ld -m elf_i386 -s -o a.out p.out
#CMD_RUN=./a.out
#OUTPUT_ASM=out.linux.x86.asm
#CMD_ASSEMBLE=nasm -f elf -o p.out $(OUTPUT_ASM)
#RED='\033[1;32m'
#NC='\033[0m' # No Color
#endif

all: clean $(EXECUTABLE_NAME) tests

tests: clean-tests test1 test2 test5 test6 test7 test8 test9
	@echo "$(RED)============== TESTS SUCCEDED ==============$(NC)"

clean: clean-tests clean-build

$(BIN_DIR):
	mkdir $(BIN_DIR)

$(EXECUTABLE_NAME): clean-build $(BIN_DIR)
	$(CMD_FIND_JAVA) > filelist
	javac -g @filelist -d $(BIN_DIR)
	$(CMD_DEL) filelist
	@echo "$(RED)============== BUILD SUCCEDED ==============$(NC)"

clean-build:
	$(CMD_DEL_RECURSIVO) $(BIN_DIR)
	$(CMD_DEL_RECURSIVO) filelist

clean-tests:
	$(CMD_DEL) *.exe
	$(CMD_DEL) *.obj
	$(CMD_DEL) *.asm
	$(CMD_DEL) *.lnk
	$(CMD_DEL) *.pdb
	$(CMD_DEL) *.ilk
	$(CMD_DEL) *.o
	$(CMD_DEL) *.out

test1: $(EXECUTABLE_NAME) $(TEST_DIR)/2/2.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/2/2.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN)

test2: $(EXECUTABLE_NAME) $(TEST_DIR)/3/source.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/3/source.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN)

test3: $(EXECUTABLE_NAME) $(TEST_DIR)/locals/source.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/locals/source.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN)

test5: $(EXECUTABLE_NAME) $(TEST_DIR)/args/args.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/args/args.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN)

test6: $(EXECUTABLE_NAME) $(TEST_DIR)/print/print.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/print/print.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/print/Ref.txt TestOut.txt

test7: $(EXECUTABLE_NAME) $(TEST_DIR)/while/while.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/while/while.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/while/Ref.txt TestOut.txt 

test8: $(EXECUTABLE_NAME) $(TEST_DIR)/arreglos/arreglos.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/arreglos/arreglos.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/arreglos/Ref.txt TestOut.txt

test9: $(EXECUTABLE_NAME) $(TEST_DIR)/arreglos2/arreglos.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/arreglos2/arreglos.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/arreglos2/Ref.txt TestOut.txt

test10: $(EXECUTABLE_NAME) $(TEST_DIR)/getc/getc.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/getc/getc.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	echo zg | $(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/getc/Ref.txt TestOut.txt

console: $(EXECUTABLE_NAME) $(TEST_DIR)/console/console.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/console/console.jc
	$(CMD_ASSEMBLE)
	$(CMD_LINK)
	$(CMD_RUN)

