
EXECUTABLE_NAME = JComp.class
BIN_DIR = bin

$(BIN_DIR):
	mkdir $(BIN_DIR)


$(EXECUTABLE_NAME): $(BIN_DIR)
	dir /s/b src\*.java > filelist
	javac @filelist -d $(BIN_DIR)

clean:
	del /Q $(BIN_DIR)\*
	del filelist

all: clean $(EXECUTABLE_NAME)

