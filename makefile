
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
	del filelist

clean-tests:
	del /Q $(TEST_DIR)\*.exe
	del /Q $(TEST_DIR)\*.obj

test1: $(TEST_DIR)\1\1.jc $(TEST_DIR)\1\1.c
	java -cp bin JComp $(TEST_DIR)\1\1.jc
	cl /Fa1.asm $(TEST_DIR)\1\1.c
	diff $(TEST_DIR)\1\1.asm $(TEST_DIR)\1\p.asm

tests: clean-tests test1
	

all: $(EXECUTABLE_NAME) tests

