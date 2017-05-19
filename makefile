
EXECUTABLE_NAME=jcomp
BIN_DIR=bin
TEST_DIR=tests


#ifeq ($(OS),Windows_NT)
CMD_DEL=del
CMD_DEL_RECURSIVO=del /s/q
CMD_FIND_JAVA=dir /s/b src\*.java 
CMD_RUN=out\p.exe
JCOMP_COMPILE=jc.cmd 
#else
#CMD_DEL=rm
#CMD_DEL_RECURSIVO=rm -rf
#CMD_FIND_JAVA=find src/ | grep java
#CMD_RUN=./a.out
#RED='\033[1;32m'
#NC='\033[0m' # No Color
#endif

all: clean $(EXECUTABLE_NAME) tests

tests: clean-tests test1 test2 test3 test5 test6 test7 test8 test9 test10 test11 test12
	@echo "$(RED)============== TESTS SUCCEDED ==============$(NC)"

clean: clean-tests clean-build

$(BIN_DIR):
	mkdir $(BIN_DIR)

$(EXECUTABLE_NAME): clean-build $(BIN_DIR)
	$(CMD_FIND_JAVA) > $(BIN_DIR)\filelist
	cd $(BIN_DIR)
	javac -g @filelist -d ..\$(BIN_DIR)
	$(CMD_DEL) filelist
	cd ..
	@echo "$(RED)============== BUILD SUCCEDED ==============$(NC)"

clean-build:
	$(CMD_DEL_RECURSIVO) $(BIN_DIR)
	$(CMD_DEL_RECURSIVO) filelist

clean-tests:
	$(CMD_DEL_RECURSIVO) out

test1: $(EXECUTABLE_NAME) $(TEST_DIR)/2/2.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/2/2.jc
	$(CMD_RUN)

test2: $(EXECUTABLE_NAME) $(TEST_DIR)/3/source.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/3/source.jc
	$(CMD_RUN)

test3: $(EXECUTABLE_NAME) $(TEST_DIR)/locals/source.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/locals/source.jc
	$(CMD_RUN)

test5: $(EXECUTABLE_NAME) $(TEST_DIR)/args/args.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/args/args.jc
	$(CMD_RUN)

test6: $(EXECUTABLE_NAME) $(TEST_DIR)/print/print.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/print/print.jc
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/print/Ref.txt TestOut.txt

test7: $(EXECUTABLE_NAME) $(TEST_DIR)/getc/getc.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/getc/getc.jc
	echo zg | $(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/getc/Ref.txt TestOut.txt

test8: $(EXECUTABLE_NAME) $(TEST_DIR)/while/while.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/while/while.jc
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/while/Ref.txt TestOut.txt

test9: $(EXECUTABLE_NAME) $(TEST_DIR)/arreglos/arreglos.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/arreglos/arreglos.jc
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/arreglos/Ref.txt TestOut.txt

test10: $(EXECUTABLE_NAME) $(TEST_DIR)/arreglos2/arreglos.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/arreglos2/arreglos.jc
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/arreglos2/Ref.txt TestOut.txt

test11: $(EXECUTABLE_NAME) $(TEST_DIR)/cadenas/cadena.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/cadenas/cadena.jc
	$(CMD_RUN) > TestOut.txt
	diff --text --ignore-all-space tests/cadenas/Ref.txt TestOut.txt

test12: $(EXECUTABLE_NAME) $(TEST_DIR)/assemblyiniline/asm.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/assemblyiniline/asm.jc
	$(CMD_RUN)


gato: $(EXECUTABLE_NAME) $(TEST_DIR)/gato/gato.jc
	$(JCOMP_COMPILE) $(TEST_DIR)/gato/gato.jc
	$(CMD_RUN)

