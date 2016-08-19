
EXECUTABLE_NAME = jcomp
BIN_DIR = bin
TEST_DIR = tests

all: clean $(EXECUTABLE_NAME) tests

tests: clean-tests test1 test2 test3 test4

clean: clean-tests clean-build

ifeq ($(OS),Windows_NT)
CMD_DEL=del
CMD_DEL_RECURSIVO=rd /s /q
CMD_FIND_JAVA=dir /s/b src\*.java 
CMD_ASSEMBLE=ml /nologo
CMD_LINK=
CMD_RUN=main.exe
else
CMD_DEL=rm
CMD_DEL_RECURSIVO=rm -rf
CMD_FIND_JAVA=find src/ | grep java
CMD_ASSEMBLE=nasm -f elf 
CMD_LINK=ld -s -o a.out
CMD_RUN=./a.out
endif

$(BIN_DIR):
	mkdir $(BIN_DIR)

$(EXECUTABLE_NAME): clean-build $(BIN_DIR)
	$(CMD_FIND_JAVA) > filelist
	javac @filelist -d $(BIN_DIR)
	$(CMD_DEL) filelist

clean-build:
	$(CMD_DEL_RECURSIVO) $(BIN_DIR)
	$(CMD_DEL_RECURSIVO) filelist

clean-tests:
	$(CMD_DEL_RECURSIVO) *.exe
	$(CMD_DEL_RECURSIVO) *.obj
	$(CMD_DEL_RECURSIVO) *.asm
	$(CMD_DEL_RECURSIVO) *.lnk
	$(CMD_DEL_RECURSIVO) *.pdb

test1: $(EXECUTABLE_NAME) $(TEST_DIR)/1/1.jc $(TEST_DIR)/1/1.c
	java -cp bin jcomp.JComp $(TEST_DIR)/locals/source.jc > out
	$(CMD_ASSEMBLE) p.asm
	$(CMD_LINK) p.o
	$(CMD_RUN)

test2: $(TEST_DIR)\2\2.jc $(TEST_DIR)\2\2.c
	java -cp bin JComp $(TEST_DIR)\2\2.jc > NUL
	cl /nologo /Fa2.asm $(TEST_DIR)\2\2.c
	findstr /v "; TITLE" 2.asm > 2s.asm
	findstr /v "; TITLE" p.asm > ps.asm
	diff --ignore-blank-lines 2s.asm ps.asm

test3: $(TEST_DIR)\3\source.jc $(TEST_DIR)\3\source.c
	java -cp bin JComp $(TEST_DIR)\3\source.jc > NUL
	cl /nologo /Famscl.asm $(TEST_DIR)\3\source.c
	findstr /v "; TITLE" mscl.asm > msclc.asm
	findstr /v "; TITLE" p.asm > ps.asm
	diff --ignore-blank-lines msclc.asm ps.asm

test4:
	java -cp bin JComp $(TEST_DIR)\locals\source.jc > log
	cl /nologo /Fa1.asm $(TEST_DIR)\locals\1.c
	findstr /V "^;" p.asm > ps.asm
	findstr /V "^;" 1.asm > 1s.asm
	ml /nologo ps.asm >NUL
	ml /nologo 1s.asm >NUL

