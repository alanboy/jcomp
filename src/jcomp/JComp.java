package jcomp;

import java.io.*;
import java.util.*;
import jcomp.Opciones;
import jcomp.backend.*;
import jcomp.frontend.Lexico;
import jcomp.frontend.Semantico;
import jcomp.frontend.Sintactico;
import jcomp.util.Log;

public class JComp
{
	private static final int VersionMajor = 0;
	private static final int VersionMinor = 0;
	private static final int VersionRevision = 2;

	public static void main(String [] args)
	{
		title();

		Opciones opciones = new Opciones(args);
		if (!opciones.isValid())
		{
			uso();
		}
		else
		{
			iniciar(opciones);
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
				+ " for x86");
		System.out.println("Alan Gonzalez 2010 - 2016");
		System.out.println("");
	}

	static void uso()
	{
		System.out.println("uso: jcomp [ option... ] filename... [ /link linkoption... ]");

		System.out.println("options:");
		System.out.println("-v mostrar salida detallada (verbose)");
		System.out.println("-a nasm|masm generar ensamblador compatible con masm o nasm, el default es nasm");
		System.out.println("-h mostrar ayuda");
	}

	static int iniciar(Opciones lineaDeComandos)
	{
		String codigo = "";

		// Cargar el archivo fuente
		try{
			BufferedReader br = new BufferedReader(new FileReader(lineaDeComandos.getCodigoFuentePath()));
			codigo = "";
			String k = "";
			while((k = br.readLine()) != null ) codigo += (k+"\n");

		}catch(IOException e){
			System.out.println("No he podido leer el archivo de entrada:" + lineaDeComandos.getCodigoFuentePath());
			System.exit(1);
		}

		Lexico a_lex = new Lexico();
		Sintactico a_sin = new Sintactico();
		Semantico a_sem = new Semantico();
		Log s_pLog = Log.getInstance();

		a_lex.setCodigo(codigo);

		if(a_lex.iniciar() != 0)
		{
			System.exit(1);
		}

		codigo = a_lex.getCodigo();
		a_sin.setCodigo(codigo);
		if(a_sin.iniciar() != 0)
		{
			System.exit(1);
		}

		codigo = a_sin.getCodigo();
		a_sem.setCodigo(codigo);

		if(a_sem.iniciar() != 0)
		{
			System.exit(1);
		}
		codigo = a_sem.getCodigo();

		s_pLog.imprimirLinea("----------------------");
		s_pLog.imprimirLinea("CODIGO OBJETO !!");
		s_pLog.imprimirLinea("----------------------");
		s_pLog.imprimirLinea(codigo);

		if (lineaDeComandos.getNasmOrMasm().equals("nasm"))
		{
			Ensambler2 en = new Ensambler2(codigo);

			if(en.iniciar() != 0) System.exit(1);

			codigo = en.getCodigo();
		}
		else
		{
			Ensambler en = new Ensambler();

			en.setCodigo(codigo);

			if(en.iniciar() != 0) System.exit(1);

			codigo = en.getCodigo();
		}


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

}

