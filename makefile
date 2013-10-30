
EXECUTABLE_NAME = JComp.class
BIN_DIR = bin

$(BIN_DIR):
	mkdir $(BIN_DIR)

$(EXECUTABLE_NAME): $(BIN_DIR)
	javac src\*.java -d $(BIN_DIR)

clean:
	del /Q $(BIN_DIR)\*

all: clean $(EXECUTABLE_NAME)

