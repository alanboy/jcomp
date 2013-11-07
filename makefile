
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
	del /Q $(BIN_DIR)\*
!if exist("filelist")
		del filelist
!endif


clean-tests:
	@del /Q *.exe
	@del /Q *.obj
	@del /Q *.asm
	@del /Q *.lnk


test1: $(TEST_DIR)\1\1.jc $(TEST_DIR)\1\1.c
	java -cp bin JComp $(TEST_DIR)\1\1.jc > NUL
	cl /nologo /Fa1.asm $(TEST_DIR)\1\1.c
	findstr /v "; TITLE" 1.asm > 1s.asm
	findstr /v "; TITLE" p.asm > ps.asm
	diff 1s.asm ps.asm


test2: $(TEST_DIR)\2\2.jc $(TEST_DIR)\2\2.c
	java -cp bin JComp $(TEST_DIR)\2\2.jc > NUL
	cl /nologo /Fa2.asm $(TEST_DIR)\2\2.c
	findstr /v "; TITLE" 2.asm > 2s.asm
	findstr /v "; TITLE" p.asm > ps.asm
	diff 2s.asm ps.asm

test3: $(TEST_DIR)\3\source.jc $(TEST_DIR)\3\source.c
	java -cp bin JComp $(TEST_DIR)\3\source.jc > NUL
	cl /nologo /Famscl.asm $(TEST_DIR)\3\source.c
	findstr /v "; TITLE" mscl.asm > msclc.asm
	findstr /v "; TITLE" p.asm > ps.asm
	diff msclc.asm ps.asm


tests: clean-tests test1 test2 test3
	

all: $(EXECUTABLE_NAME) tests

