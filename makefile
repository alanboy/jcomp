
EXECUTABLE_NAME=jcomp
BIN_DIR=bin
TEST_DIR=tests


#ifeq ($(OS),Windows_NT)
CMD_DEL=del
CMD_DEL_RECURSIVO=del /s/q
CMD_FIND_JAVA=dir /s/b src\*.java 
CMD_ASSEMBLE=C:\Users\alan\AppData\Local\NASM\nasm.exe -f win32 -o p.o 
CMD_LINK=link p.o  /SUBSYSTEM:windows /ENTRY:start
CMD_RUN=p.exe
#else
#CMD_DEL=rm
#CMD_DEL_RECURSIVO=rm -rf
#CMD_FIND_JAVA=find src/ | grep java
#CMD_ASSEMBLE=nasm -f elf 
#CMD_LINK=ld -s -o a.out p.out
#CMD_RUN=./a.out
#RED='\033[1;32m'
#NC='\033[0m' # No Color
#endif

all: clean $(EXECUTABLE_NAME) tests

tests: clean-tests test1 test2 test3 test4 test5 test6 test7 test8
	@echo "$(RED)============== TESTS SUCCEDED ==============$(NC)"

clean: clean-tests clean-build

$(BIN_DIR):
	mkdir $(BIN_DIR)

$(EXECUTABLE_NAME): clean-build $(BIN_DIR)
	$(CMD_FIND_JAVA) > filelist
	javac @filelist -d $(BIN_DIR)
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

test1: $(EXECUTABLE_NAME) $(TEST_DIR)/1/1.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/locals/source.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN)

test2: $(EXECUTABLE_NAME) $(TEST_DIR)/2/2.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/2/2.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN)

test3: $(EXECUTABLE_NAME) $(TEST_DIR)/3/source.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/3/source.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN)

test4: $(EXECUTABLE_NAME) $(TEST_DIR)/locals/source.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/locals/source.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN)

test5: $(EXECUTABLE_NAME) $(TEST_DIR)/args/args.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/args/args.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN)

test6: $(EXECUTABLE_NAME) $(TEST_DIR)/print/print.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/print/print.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN) > TestOut.txt
	diff --ignore-all-space tests/print/Ref.txt TestOut.txt

test7: $(EXECUTABLE_NAME) $(TEST_DIR)/while/while.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/while/while.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN) > TestOut.txt
	diff --ignore-all-space tests/while/Ref.txt TestOut.txt 

test8: $(EXECUTABLE_NAME) $(TEST_DIR)/arreglos/arreglos.jc
	java -cp bin jcomp.JComp $(TEST_DIR)/arreglos/arreglos.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK)
	$(CMD_RUN)

