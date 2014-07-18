package jcomp;

import jcomp.frontend.Semantico;
import jcomp.frontend.Lexico;
import jcomp.frontend.Sintactico;
import jcomp.backend.Ensambler;
import java.io.*;
import java.util.*;
import jcomp.Opciones;
import jcomp.util.Log;

public class JComp
{
    
    private static final int VersionMajor      = 0;
    private static final int VersionMinor      = 0;
    private static final int VersionRevision   = 1;
	public static void main(String [] args)
	{
        title();

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

    static void title()
	{
		System.out.println("jcomp Compiler Version "
                + VersionMajor
                + "."
                + VersionMinor 
                + "."
                + VersionRevision 
                + " for X86");
		System.out.println("Alan Gonzalez 2010 - 2014");
        System.out.println("");
	}
        
	static void uso()
	{
		System.out.println("uso: jcomp [ option... ] filename... [ /link linkoption... ]");
        
        System.out.println("options:");
        System.out.println("-v\t\t\t\tVerbose output");
        System.out.println("-h\t\t\t\tShow help");
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

		}catch(IOException e){
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

