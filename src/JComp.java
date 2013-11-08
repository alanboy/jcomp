/*------------------------------------------------------------------------------------
 				JComp
-------------------------------------------------------------------------------------*/
import java.io.*;
import java.util.*;
import jcomp.util.Log;

public class JComp
{
	public static void main(String [] args)
	{
		if(args.length == 1){
			iniciar(args[0]);
		}else{
			usage();
		}
	}

	static void usage()
	{
		System.out.println("jcomp compiler for x86");
		System.out.println("Alan Gonzalez 2013");
		System.out.println("");
		System.out.println("usage: jcomp [ option... ] filename... [ /link linkoption... ]");
	}

	static int iniciar(String file)
	{
		String codigo;
		Lexico a_lex = new Lexico();
		Sintactico a_sin = new Sintactico();
		Semantico a_sem = new Semantico();
		Log debug = Log.getInstance();

		a_lex.setDebugger(debug);
		a_lex.setCodigo(file);

		if(a_lex.iniciar() != 0)
		{
			return 1; 
		}

		codigo = a_lex.getCodigo();
		a_sin.setDebugger(debug);
		a_sin.setCodigo(codigo);
		if(a_sin.iniciar() != 0)
		{
			return 1;
		}

		codigo = a_sin.getCodigo();
		a_sem.setDebugger(debug);
		a_sem.setCodigo(codigo);

		if(a_sem.iniciar() != 0) 
		{
			return 1;
		}
		codigo = a_sem.getCodigo();

		debug.imprimirLinea("----------------------");
		debug.imprimirLinea("CODIGO OBJETO !!");
		debug.imprimirLinea("----------------------");
		debug.imprimirLinea(codigo);

		Ensambler en = new Ensambler();
		en.setCodigo(codigo);
		en.setDebugger(debug);
		if(en.iniciar() != 0)
		{
			return 1;
		}
		codigo = en.getCodigo();

		debug.imprimirLinea("");
		debug.imprimirLinea("");
		debug.imprimirLinea("----------------------");
		debug.imprimirLinea("CODIGO PARA ENSAMBLAR");
		debug.imprimirLinea("----------------------");

		debug.imprimirLinea(codigo);

		try{
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("p.asm")));
			pw.print(codigo);
			pw.close();

		}catch(IOException ioe){
			System.out.println("error creando archivo asm");

		}

		return 0;
	}

	/* 
	 * Destructor ()
	 * {
	 * debug.closeFile();
	 *
	 * }
	 * */
}//main



