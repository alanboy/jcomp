package jcomp.frontend;

public class Metodos
{
	private String Linea;
	private String Nombre;
	private String TipoDeRetorno;
	private String Argumentos;
	private String Cuerpo;
	private Variables [] variables;

	Metodos()
	{
		this.Linea = null;
		this.Nombre = null;
		this.TipoDeRetorno = null;
		this.Argumentos = null;
		this.Cuerpo = null;
	}

	void setNumVariables(int i)
	{
		variables = new Variables[i];
		for(int g=0; g<variables.length; g++)
		{
			variables[g] = new Variables();
		}
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

