package jcomp.util;

import java.io.*;
import java.util.*;

public class Log
{
	private PrintWriter printWriterObj;
	private static Log instance;
	private String logFilename;

	public static Log getInstance()
	{
		if (instance == null)
		{
			instance = new Log("out\\build.out");
		}
		return instance;
	}

	public void warning(String s)
	{
		printWriterObj.print("WARNING:" + s);
	}

	public void imprimirEncabezado(String s)
	{
		log("------------------------------------------------\n");
		log("- " + s + "\n");
		log("------------------------------------------------\n");
	}

	public void imprimirLinea(String s)
	{
		log(s + "\n");
	}

	public void close()
	{
		printWriterObj.flush();
		printWriterObj.close();
		printWriterObj = null;
	}

	public void imprimir(String s)
	{
		log(s);
	}

	public void log(String s)
	{
		if (printWriterObj != null)
		{
			printWriterObj.print(s);
		}
		else
		{
			System.err.println("Compilador: El log ya ha sido cerrado.");
		}
	}

	private Log(String filename)
	{
		logFilename = filename;
		OpenLogFile();
	}

	private void OpenLogFile()
	{
		try
		{
			printWriterObj = new PrintWriter(
					new BufferedWriter(
						new FileWriter(logFilename)));
		}
		catch(IOException ioe)
		{
			System.out.println(ioe);
		}
	}
}

