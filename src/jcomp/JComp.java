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
		Log logger = Log.getInstance();
		String codigoFuente = "";

		//
		// Cargar el archivo con el codigo fuente
		//
		try{
			logger.imprimirLinea("Compilando " + lineaDeComandos.getCodigoFuentePath());

			BufferedReader br = new BufferedReader(new FileReader(lineaDeComandos.getCodigoFuentePath()));
			codigoFuente = "";
			String k = "";
			while((k = br.readLine()) != null ) codigoFuente += (k+"\n");

		}catch(IOException e){
			System.out.println("No he podido leer el archivo de entrada:" + lineaDeComandos.getCodigoFuentePath());
			logger.close();
			System.exit(1);
		}

		//
		// FrontEnd: Analisis Lexico
		//
		Lexico a_lex = new Lexico(codigoFuente);
		if(a_lex.iniciar() != 0)
		{
			logger.close();
			System.exit(1);
		}

		//
		// FrontEnd: Analisis sintactico
		//
		Sintactico a_sin = new Sintactico();
		a_sin.setCodigo(a_lex.getCodigo());
		if(a_sin.iniciar() != 0)
		{
			logger.close();
			System.exit(1);
		}

		//
		// FrontEnd: Analisis semantico
		//
		Semantico a_sem = new Semantico();
		a_sem.setCodigo(a_sin.getCodigo());
		if(a_sem.iniciar() != 0)
		{
			logger.close();
			System.exit(1);
		}

		//
		// BackEnd: Generacion de codigo intermedio
		//
		Ensambler2 en = new Ensambler2(a_sem.getCodigo());
		en.iniciar();

		//
		// BackEnd: Generacion de codigo final dependiente de arquitectura
		//
		GeneracionDeCodigoAsm asmGen = new GeneracionDeCodigoAsm(en.getCodigo());
		asmGen.iniciar();

		logger.close();

		return 0;
	}

}

