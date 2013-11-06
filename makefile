
EXECUTABLE_NAME = JComp.class
BIN_DIR = bin
TEST_DIR = tests



$(BIN_DIR):
	mkdir $(BIN_DIR)

$(EXECUTABLE_NAME): clean-build $(BIN_DIR)
	dir /s/b src\*.java > filelist
	javac @filelist -d $(BIN_DIR)

clean-build:
	del /Q $(BIN_DIR)\*
!if exist("filelist")
		del filelist
!endif



clean-tests:
	@del /Q *.exe
	@del /Q *.obj
	@del /Q *.asm

test1: $(TEST_DIR)\1\1.jc $(TEST_DIR)\1\1.c
	java -cp bin JComp $(TEST_DIR)\1\1.jc > NUL
	cl /nologo /Fa1.asm $(TEST_DIR)\1\1.c
!if ([diff 1.asm p.asm > NUL] == 1) 
	@echo  ===================== TEST FAILED =========================
	diff 1.asm p.asm -y 
!endif


tests: clean-tests test1
	



all: $(EXECUTABLE_NAME) tests

