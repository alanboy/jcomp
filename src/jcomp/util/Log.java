package jcomp.util;

import java.io.*;
import java.util.*;

public class Log
{
	PrintWriter pw;
	static Log instance;
	String logFilename;
	LOG_DESTINATION destination;

	public static Log getInstance()
	{
		if (instance == null)
		{
			instance = new Log();
		}
		return instance;
	}

	public boolean setFilename(String sf)
	{
		boolean status = false;
		if (pw == null)
		{
			status = true;
			this.logFilename = sf;
		}
		return status;
	}

	public void warning(String s)
	{
		pw.print("WARNING:" + s);
	}

	public void log(String s)
	{
		switch(destination)
		{
			case LOG_TO_STDOUT:
				System.out.print(s);
			break;
			case LOG_TO_FILE:
				pw.println(s);
			break;
		}
	}

	public void imprimirLinea(String s)
	{
		log(s + "\n");
	}

	public void imprimir(String s)
	{
		log(s);
	}

	private enum LOG_DESTINATION
	{
		LOG_TO_FILE,
		LOG_TO_STDOUT,
		LOG_TO_STDERR
	};

	private Log ()
	{
		logFilename = "output.log";
		destination = LOG_DESTINATION.LOG_TO_STDOUT;
	}

	private void OpenLogFile()
	{
		try
		{
			pw = new PrintWriter(
					new BufferedWriter(
						new FileWriter(logFilename)));
		}
		catch(IOException ioe)
		{
			System.out.println(ioe);
		}
	}

	private void closeFile()
	{
		pw.close();
	}

}//class Log


class Debugger{


}
