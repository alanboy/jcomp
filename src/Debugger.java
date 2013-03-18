
import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------------
 				DEBUGGER
-------------------------------------------------------------------------------------*/
class Debugger{

	PrintWriter pw;

	Debugger (){
		try{
			pw = new PrintWriter(new BufferedWriter(new FileWriter("salida.txt")));

		}catch(IOException ioe){
			System.out.println("error escribiendo archivo de debugger");

		}
	}

	void imprimir(String s){
		pw.print(s);
	}


	void imprimirLinea(String s){
		pw.println(s);
	}

	void closeFile(){
		pw.close();
	}

}//class Debugger


