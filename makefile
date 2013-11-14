
EXECUTABLE_NAME = jcomp 
BIN_DIR = bin
TEST_DIR = tests

$(BIN_DIR):
	mkdir $(BIN_DIR)


$(EXECUTABLE_NAME): clean-build $(BIN_DIR)
	dir /s/b src\*.java > filelist
	javac @filelist -d $(BIN_DIR)
	del filelist


clean-build:
	rd /s /q jcomp
!if exist("filelist")
		del filelist
!endif


clean-tests:
	@del /Q *.exe
	@del /Q *.obj
	@del /Q *.asm
	@del /Q *.lnk
	@del /Q *.pdb


test1: $(TEST_DIR)\1\1.jc $(TEST_DIR)\1\1.c
	java -cp bin JComp $(TEST_DIR)\1\1.jc > NUL
	cl /nologo /Fa1.asm $(TEST_DIR)\1\1.c
	findstr /v "; TITLE" 1.asm > 1s.asm
	findstr /v "; TITLE" p.asm > ps.asm
	diff --ignore-blank-lines 1s.asm ps.asm


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


tests: clean-tests test1 test2 test3 test4
	


clean: clean-tests clean-build


all: clean $(EXECUTABLE_NAME) tests

