/*------------------------------------------------------------------------------------
 				Metodo
-------------------------------------------------------------------------------------*/
//los metodos ke se encuentran en el programa, son un objeto de esta clase
public class Metodos{

	private String Linea;
	private String Nombre;
	private String TipoDeRetorno;
	private String Argumentos;
	private String Cuerpo;

	private Variables [] variables;

	Metodos(){
		this.Linea = "ns";
		this.Nombre = "ns";
		this.TipoDeRetorno = "ns";
		this.Argumentos = "ns";
		this.Cuerpo = "ns";
	}


	void setNumVariables(int i){
		variables = new Variables[i];
		for(int g=0; g<variables.length; g++)variables[g] = new Variables();
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



