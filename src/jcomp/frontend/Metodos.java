package jcomp.frontend;

import java.util.HashMap;

public class Metodos
{
	private String Linea;
	private String Nombre;
	private String TipoDeRetorno;
	private String Argumentos;
	private String Cuerpo;

	private HashMap<String, Variables> variablesLocales;

	Metodos()
	{
		this.Linea = null;
		this.Nombre = null;
		this.TipoDeRetorno = null;
		this.Argumentos = null;
		this.Cuerpo = null;
	}

	void setVariablesLocales(HashMap<String, Variables> locales)
	{
		variablesLocales = locales;
	}

	Variables getVariableLocal(String nombre)
	{
		return variablesLocales.get(nombre);
	}

	void setLinea(String s){ Linea = s; }
	void setNombre(String s){ Nombre = s; }
	void setTipoDeRetorno(String s){ TipoDeRetorno = s; }
	void setArgumentos(String s){Argumentos = s; }
	void setCuerpo(String s){ Cuerpo = s; }

	String getLinea(){return Linea;}
	String getNombre(){return Nombre;}
	String getTipoDeRetorno(){ return TipoDeRetorno; }
	String getArgumentos(){return Argumentos; }
	String getCuerpo(){ return Cuerpo; }
}

