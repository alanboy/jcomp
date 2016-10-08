package jcomp.frontend;

/*------------------------------------------------------------------------------------
 				ANALISIS LEXICO
-------------------------------------------------------------------------------------*/
import java.io.*;
import java.util.*;
import jcomp.util.Log;

/**
 * Lexico
 *
 *
 *
 * @author Alan Gonzalez
 * @todo Hacer esta clase singleton
 *
 * */
public class Lexico
{
	String [][] TOKENS;
	char [] ALFABETO;
	String PROGRAMA_FUENTE;
	Log debug;

	public Lexico(String source)
	{
		this.debug = Log.getInstance();
		PROGRAMA_FUENTE = source;
	}

	public int iniciar()
	{
		//todos los metodos ke manipulan el codigo fuente,
		// lo leen de CODIGO_FUENTE y lo guardan manipulado
		// en CODIGO_FUENTE tambien

		//cargar archivos necesarios para analizar
		if(cargarConfiguracion() != 0) return 1;

		/*		funciones del lenguaje ke implicitas */
		// tenemos ke agregarlas aqui tal vez....

		//elimina comentarios, regresa 0 si todo salio bien
		if(eliminarComentarios() != 0) return 1;

		//ke los chars del cod fuente pertenescan al alfabeto
		//regresa 0 si todo salio bien
		if(verificarAlfabeto() != 0)return 1;

		//limpiar codigo de espacios, tabs, y saltos de pag innecesarios
		//ademas pone el numero de linea antes
		//regresa 0 si todo salio bien
		if(limpiarCodigo() != 0)return 1;

		//tokenizar...
		if(tokenize() != 0)return 1;

		debug.imprimirLinea( "------------------------------" );
		debug.imprimirLinea( "ANALISIS LEXICO 	" );
		debug.imprimirLinea( "------------------------------" );

		debug.imprimir( PROGRAMA_FUENTE );

	return 0;
	}

	public String getCodigo()
	{
		return PROGRAMA_FUENTE;
	}

	int cargarConfiguracion()
	{
			//cargar los tokens en un arreglo
			TOKENS = new String[][]{
				{"(","PARENTESIS_ABRE"},
				{")","PARENTESIS_CIERRA"},
				{"{","LLAVE_ABRE"},
				{"}","LLAVE_CIERRA"},
				{"[","CORCHETE_ABRE"},
				{"]","CORCHETE_CIERRA"},
				{"=","ASIGNA"},
				{"+","OP_SUMA"},
				{"-","OP_RESTA"},
				{"/","OP_DIVISION"},
				{"*","OP_MULTIPLICACION"},
				{"<","BOL_MENOR_QUE"},
				{">","BOL_MAYOR_QUE"},
				{".","PUNTUACION_PUNTO"},
				{",","PUNTUACION_COMA"},
				{":","PUNTUACION_DOS_PUNTOS"},
				{";","PUNTUACION_PUNTO_COMA"},
				{"!","PUNTUACION_EXCLAMACION"},
				{"#","PUNTUACION_GATO"},
				{"&","PUNTUACION_Y"},
				{"if","CONTROL_IF"},
				{"for","CONTROL_FOR"},
				{"while","CONTROL_WHILE"},
				{"def","CONTROL_DEF"},
				{"return","CONTROL_RETORNO"},
				{"void","TIPO_VOID"},
				{"int","TIPO_INT"},
				{"int8","TIPO_INT8"},
				{"int16","TIPO_INT16"},
				{"int32","TIPO_INT32"},
				{"char","TIPO_CHAR"},
				{"String","TIPO_STRING"}};

			//escribir el alfabeto ke se puede aceptar
			String _a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklimnopqrstuvwxyz 0123456789{}()<>=;[]#.\",_+/-*";
			int TAM_ALF = _a.length();

			//sumarle dos, para el salto de linea y tabulador
			ALFABETO = new char [ TAM_ALF + 2];

			for(int z=0; z < TAM_ALF; z++) ALFABETO[z] = _a.charAt(z);

			//agregarle caracteres ke no puedo poner en el archivo de alfabeto
			//y ke ademas son de awuebo, tabulador y nueva linea
			ALFABETO[_a.length()] = 10;
			ALFABETO[_a.length()+1] = 9;

		return 0;
	}//fin metodo cargarConfiguracion()

	int eliminarComentarios()
	{

		String pf="";
		String [] result = PROGRAMA_FUENTE.split("\n");

		for (int x=0; x<result.length; x++)
		{
			String linea = result[x];
			int d = linea.indexOf("//");

			if( d != -1)
			{
				linea = linea.substring( 0, d );
			}
			pf += (linea + "\n");
		}

		PROGRAMA_FUENTE = pf;
		return 0;
	}

	int verificarAlfabeto()
	{
		int linea = 1;

		for(int z=0; z < PROGRAMA_FUENTE.length(); z++)
		{
			char c;
			c = PROGRAMA_FUENTE.charAt(z);

			//buscar en el vector de alfabeto
			boolean found = false;
			for(int x=0; x<ALFABETO.length; x++)
			{
				if(c==ALFABETO[x])found = true;
			}

			if( !found  )
			{
				//encontro un caracter ke no esta en el alfabeto
				System.out.println("He encontrado un caracter que no pertenece al alfabeto.");
				System.out.println("Linea: " + linea);
				System.out.println("Caracter: [" + c +"]");
				return 1;
			}

			if(c == '\n')
			{
				linea ++;
			}

		}
	return 0;
	}//metodo verificarAlfabeto

	int limpiarCodigo()
	{
		//elimina espacios demas, tabulaciones, y lineas en blanco
		//agrega el numerod de linea

		//TO DO: hacer ke no kite espacios en cadenas solo en codigo normal

		String pf="";
		String [] result = PROGRAMA_FUENTE.split("\n");

		StringBuffer linea;

		for (int x=0; x<result.length; x++)
		{
			linea = new StringBuffer(result[x]);

			boolean cambio = true;
			while(cambio)
			{
				cambio = false;
				int a = linea.indexOf("  ");
				if( a != -1 )
				{
					linea.replace(a,a+2, " ");
					cambio = true;
				}

				int b = linea.indexOf(String.valueOf((char)9));

				if( b != -1 )
				{
					linea.setCharAt(b, ' ');
					cambio = true;
				}
			}

			//si dentro de una cadena long mayor a cero el primero es
			//un espacio en blanclo, borrarlo
			if( linea.length() > 0)
				if( linea.charAt(0) == 32)
					linea.deleteCharAt(0);

			//si la linea no tiene nada, tons no agregarla al final
			//y a las ke si, separa el numero de linea con el caracter
			//175.... 		 (2:59AM waaaaaa)
			if(linea.length() != 0) pf += ((x+1)+ ""+(char)175 +""+linea + "\n");

		}//for ke recorre cada linea

		PROGRAMA_FUENTE = pf;
		return 0;
	}

	int tokenize()
	{
		String numero_linea;//el caracter 175 separa el numero de linea de la instruccion
		String linea;
		String pf="";	//aqui se guarda el resultado intermedio de este pedo
		StringTokenizer separacion_linea;
		String lineas [] = PROGRAMA_FUENTE.split("\n");

		for(int a=0; a<lineas.length; a++)
		{
			separacion_linea = new StringTokenizer( lineas[a], String.valueOf((char)175) );

			try{
				numero_linea = separacion_linea.nextToken();
			}
			catch(Exception nsee)
			{
				System.out.println("Archivo vacio");
			   	return 1;
			}
			linea = separacion_linea.nextToken();

			//aqui ya analizo linea por linea
			//el contenido de la linea actual
			//esta en el string linea
			//La linea se procesa y se concatena al string pf
			StringTokenizer st = new StringTokenizer(linea, "*/;{}#()[]<>!=+-\", ", true);

			//agregar en el programa fuente nuevo el numero de linea
			pf += "NUMERO_LINEA_"+numero_linea+"\n";

			String token="";
			boolean espacio = false;
			boolean cadena = false;

			while( st.hasMoreTokens() )
			{
				cadena = false;
				String s = st.nextToken();

				//Checar si estoy analizando una cadena
				//si si encuentro comillas tons
				//seguir agregando tokens hasta ke encuentre
				//otras comillas
				if(s.charAt(0)=='\"')
				{
					//ir agregando los tokens a 's', si el ultimo token
					//Agregado es una comilla tons ya akabo con la cadena
					//si no encuentra una comilla marka un error
					s += st.nextToken();
					try{
						while (s.charAt(s.length()-1) != 34)
							s += st.nextToken();
					}catch(Exception e)
					{
						System.out.println("Error, no encontre el fin de la cadena.");
						System.out.println("Linea: "+numero_linea);
					return 1;
					}
				}

				//checar si es un white_space
				if((s.length() == 1) && (s.charAt(0)==32))
					espacio = true; else espacio = false;

				//si no es un espacio tons agregar este nuevo token al codigo fuente
				if( !espacio )pf += s + "\n";

			}//while de cada token

		}//for de cada linea

		//aqui el programa fuente ya tiene los tokens en cada linea
		//ahora hay ke compararlos con las palabras reservadas
		PROGRAMA_FUENTE = pf;

		//--------> RECUERDA !!
		//TOKENS tiene en la columna 1
		//el alto "nivel" y en la 2 ps ya el token ke le corresponde

		//SALE !
		pf="";
		boolean found;

		lineas = PROGRAMA_FUENTE.split("\n");
		for(int a=0; a<lineas.length; a++)
		{

			found = false;

			for(int b=0; b<TOKENS.length; b++)
			{
				if(lineas[a].equals( TOKENS[b][0] ))
				{
					pf += TOKENS[b][1] + "\n";
					found = true;
					break;
				}
			}//for palabras reservadas

			//si no encontro palabra puede ser un
			// identificador, un valor, o una estupidez
			// para tronar el programa ahhh o el numero de linea, o una cadena
			boolean yaloencontre=false;
			if(!found)
			{
				String token = lineas[a];

				//intentemos ver si es un numero
				try
				{
					int num = Integer.parseInt( token );
					pf += "VALOR_NUMERO_" + lineas[a] + "\n";
					yaloencontre = true;
				}
				catch(Exception e)
				{
					// Nada que hacer
				}

				//VEAMOS SI ES UN NUMERO DE LINEA
				if(token.startsWith("NUMERO_LINEA"))
				{
					pf += lineas[a] + "\n";
					yaloencontre = true;
				}

				//si eske aun no lo he encontrado
				//ps haber si es un identificador
				//valido de java
				if(Character.isJavaIdentifierStart(token.charAt(0)) && !yaloencontre)
				{
					pf += "IDENTIFICADOR_" + lineas[a] + "\n";
					yaloencontre = true;
				}

				//haber si es un string
				if((token.charAt(0) == '\"') && !yaloencontre)
				{
					pf += "STRING_" + token + "\n";
					yaloencontre = true;
				}

				//si no es nada de esto... ps algo anda mal
				if(!yaloencontre)
				{
					System.out.println("Encontre algo ke no es una expresion.");
					String err="";
					//hay ke buscar en ke linea esta el error
					//regresando hasta encontrar una linea
					for(int c = a; c >= 0; c--)
					{
						if(lineas[c].startsWith("NUMERO_LINEA"))
						{
						err = lineas[c].substring(13,lineas[c].length());
						break;
						}
					}

					System.out.println("Linea: "+ err);
					System.out.println("Token: "+ token);
					//TO DO: ke muestre la linea complera
					return 1;
				}
			}//if de si no es una palabra reservada
		}//For de cada linea

		pf += "FINAL\n";
		PROGRAMA_FUENTE = pf;

		return 0;
	}

}
