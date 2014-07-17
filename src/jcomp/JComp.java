package jcomp;

import java.io.*;
import java.util.*;
import jcomp.Opciones;
import jcomp.util.Log;

public class JComp
{
	public static void main(String [] args)
	{
		Opciones o = new Opciones(args);
		if (!o.isValid())
		{
			uso();
		}
		else
		{
			iniciar(o);
		}
	}

	static void uso()
	{
		System.out.println("jcomp compiler for x86");
		System.out.println("Alan Gonzalez 2013");
		System.out.println("");
		System.out.println("uso: jcomp [ option... ] filename... [ /link linkoption... ]");
	}

	static int iniciar(Opciones lineaDeComandos)
	{
		String codigo;

		// Cargar el archivo fuente
		try{
			BufferedReader br = new BufferedReader(new FileReader(lineaDeComandos.getCodigoFuentePath()));
			codigo = "";
			String k = "";
			while( (k = br.readLine()) != null ) codigo += (k+"\n");

		}catch(Exception e){
			System.out.println( "No he podido leer el archivo de entrada:" + lineaDeComandos.getCodigoFuentePath());
			return 1;
		}

		Lexico a_lex = new Lexico();
		Sintactico a_sin = new Sintactico();
		Semantico a_sem = new Semantico();
		Log s_pLog = Log.getInstance();

		a_lex.setCodigo(codigo);

		if(a_lex.iniciar() != 0)
		{
			return 1; 
		}

		codigo = a_lex.getCodigo();
		a_sin.setCodigo(codigo);
		if(a_sin.iniciar() != 0)
		{
			return 1;
		}

		codigo = a_sin.getCodigo();
		a_sem.setCodigo(codigo);

		if(a_sem.iniciar() != 0) 
		{
			return 1;
		}
		codigo = a_sem.getCodigo();

		s_pLog.imprimirLinea("----------------------");
		s_pLog.imprimirLinea("CODIGO OBJETO !!");
		s_pLog.imprimirLinea("----------------------");
		s_pLog.imprimirLinea(codigo);

		Ensambler en = new Ensambler();
		en.setCodigo(codigo);
		if(en.iniciar() != 0)
		{
			return 1;
		}
		codigo = en.getCodigo();

		s_pLog.imprimirLinea("");
		s_pLog.imprimirLinea("");
		s_pLog.imprimirLinea("----------------------");
		s_pLog.imprimirLinea("CODIGO PARA ENSAMBLAR");
		s_pLog.imprimirLinea("----------------------");

		s_pLog.imprimirLinea(codigo);

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
	 * s_pLog.closeFile();
	 *
	 * }
	 * */
}//main

