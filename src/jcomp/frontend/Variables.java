package jcomp.frontend;

public class Variables
{
	private String Linea;
	private String Nombre;
	private String Tipo;

	Variables()
	{
		this.Linea = null;
		this.Nombre = null;
		this.Tipo = null;
	}

	void setLinea(String s){ Linea = s; }
	void setNombre(String s){ Nombre = s; }
	void setTipo(String s){ Tipo = s; }

	String getLinea(){return Linea;}
	String getNombre(){return Nombre;}
	String getTipo(){ return Tipo; }
}

