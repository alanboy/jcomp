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
 * @author Alan Gonzalez
 *
 * */
public class Lexico
{
	HashMap<String, String> m_Tokens;
	HashSet<Character> m_Alfabeto;
	String PROGRAMA_FUENTE;
	Log debug;

	public Lexico(String source)
	{
		m_Alfabeto = new HashSet<Character>();
		this.debug = Log.getInstance();
		PROGRAMA_FUENTE = source;
	}

	public int iniciar()
	{
		// todos los metodos que manipulan el codigo fuente,
		// lo leen de CODIGO_FUENTE y lo guardan manipulado
		// en CODIGO_FUENTE tambien

		// cargar archivos necesarios para analizar
		cargarConfiguracion();

		// elimina comentarios, regresa 0 si todo salio bien
		if (eliminarComentarios() != 0) return 1;

		// que los chars del cod fuente pertenescan al alfabeto
		// regresa 0 si todo salio bien
		if (verificarAlfabeto() != 0) return 1;

		// limpiar codigo de espacios, tabs, y saltos de pag innecesarios
		// ademas pone el numero de linea antes
		// regresa 0 si todo salio bien
		if (limpiarCodigo() != 0) return 1;

		if (tokenize() != 0) return 1;

		debug.imprimirEncabezado("ANALISIS LEXICO");

		debug.imprimir(PROGRAMA_FUENTE);

		return 0;
	}

	public String getCodigo()
	{
		return PROGRAMA_FUENTE;
	}

	void cargarConfiguracion()
	{
		m_Tokens = new HashMap<String,String>();

		m_Tokens.put("(", "PARENTESIS_ABRE");
		m_Tokens.put(")", "PARENTESIS_CIERRA");
		m_Tokens.put("{", "LLAVE_ABRE");
		m_Tokens.put("}", "LLAVE_CIERRA");
		m_Tokens.put("[", "CORCHETE_ABRE");
		m_Tokens.put("]", "CORCHETE_CIERRA");
		m_Tokens.put("=", "ASIGNA");
		m_Tokens.put("+", "OP_SUMA");
		m_Tokens.put("-", "OP_RESTA");
		m_Tokens.put("/", "OP_DIVISION");
		m_Tokens.put("*", "OP_MULTIPLICACION");
		m_Tokens.put("<", "BOL_MENOR_QUE");
		m_Tokens.put(">", "BOL_MAYOR_QUE");
		m_Tokens.put(".", "PUNTUACION_PUNTO");
		m_Tokens.put(",", "PUNTUACION_COMA");
		m_Tokens.put(":", "PUNTUACION_DOS_PUNTOS");
		m_Tokens.put(";", "PUNTUACION_PUNTO_COMA");
		m_Tokens.put("!", "PUNTUACION_EXCLAMACION");
		m_Tokens.put("#", "PUNTUACION_GATO");
		m_Tokens.put("&", "PUNTUACION_Y");
		m_Tokens.put("if", "CONTROL_IF");
		m_Tokens.put("for", "CONTROL_FOR");
		m_Tokens.put("while", "CONTROL_WHILE");
		m_Tokens.put("def", "CONTROL_DEF");
		m_Tokens.put("return", "CONTROL_RETORNO");
		m_Tokens.put("void", "TIPO_VOID");
		m_Tokens.put("int", "TIPO_INT");
		m_Tokens.put("int8", "TIPO_INT8");
		m_Tokens.put("int16", "TIPO_INT16");
		m_Tokens.put("int32", "TIPO_INT32");
		m_Tokens.put("char", "TIPO_CHAR");
		m_Tokens.put("string", "TIPO_STRING");

		// escribir el alfabeto que se puede aceptar
		String alfabeto = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklimnopqrstuvwxyz 0123456789{}()<>=;[]#.\",_+/-*";

		// agregar tab y nueva linea
		alfabeto += (char)10;
		alfabeto += (char)9;

		for (char c : alfabeto.toCharArray())
		{
			m_Alfabeto.add(c);
		}
	}

	int eliminarComentarios()
	{
		String pf = "";
		String [] result = PROGRAMA_FUENTE.split("\n");

		for(int x=0; x < result.length; x++)
		{
			String linea = result[x];
			int d = linea.indexOf("//");

			if (d != -1)
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

		for (char c : PROGRAMA_FUENTE.toCharArray())
		{
			if (!m_Alfabeto.contains(c))
			{
				// encontre un caracter que no esta en el alfabeto
				System.out.println("He encontrado un caracter que no pertenece al alfabeto.");
				System.out.println("Linea: " + linea);
				System.out.println("Caracter: [" + c +"]");
				return 1;
			}

			if (c == '\n')
			{
				linea++;
			}
		}

		return 0;
	}

	int limpiarCodigo()
	{
		// elimina espacios demas, tabulaciones, y lineas en blanco
		// agrega el numero de linea
		// TODO: hacer que no quite espacios en cadenas solo en codigo normal

		String pf = "";
		String [] result = PROGRAMA_FUENTE.split("\n");

		StringBuffer linea;

		for (int x = 0; x < result.length; x++)
		{
			linea = new StringBuffer(result[x]);

			boolean cambio = true;
			while (cambio)
			{
				cambio = false;

				int a = linea.indexOf("  ");
				if (a != -1)
				{
					linea.replace(a, a+2, " ");
					cambio = true;
				}

				int b = linea.indexOf(String.valueOf((char)9));

				if (b != -1)
				{
					linea.setCharAt(b, ' ');
					cambio = true;
				}
			}

			// si dentro de una cadena long mayor a cero el primero es
			// un espacio en blanclo, borrarlo
			if (linea.length() > 0)
				if (linea.charAt(0) == 32)
					linea.deleteCharAt(0);

			// si la linea no tiene nada, tons no agregarla al final
			// y a las que si, separa el numero de linea con el caracter
			// 175
			if (linea.length() != 0)
			{
				pf += ((x+1)+ ""+(char)175 +""+linea + "\n");
			}
		}

		PROGRAMA_FUENTE = pf;
		return 0;
	}

	int tokenize()
	{
		String numero_linea;//el caracter 175 separa el numero de linea de la instruccion
		String linea;
		String pf=""; //aqui se guarda el resultado intermedio de este pedo
		StringTokenizer separacion_linea;
		String lineas [] = PROGRAMA_FUENTE.split("\n");

		for (int a=0; a<lineas.length; a++)
		{
			separacion_linea = new StringTokenizer( lineas[a], String.valueOf((char)175) );

			try
			{
				numero_linea = separacion_linea.nextToken();
			}
			catch (Exception nsee)
			{
				System.out.println("Archivo vacio");
				return 1;
			}

			linea = separacion_linea.nextToken();

			// aqui ya analizo linea por linea el contenido de la linea actual
			// esta en el string linea. La linea se procesa y se concatena al string pf
			StringTokenizer st = new StringTokenizer(linea, "*/;{}#()[]<>!=+-\", ", true);

			// agregar en el programa fuente nuevo el numero de linea
			pf += "NUMERO_LINEA_"+numero_linea+"\n";

			String token = "";
			boolean espacio = false;
			boolean cadena = false;

			while (st.hasMoreTokens())
			{
				cadena = false;
				String s = st.nextToken();

				// Checar si estoy analizando una cadena
				// si si encuentro comillas tons
				// seguir agregando tokens hasta que encuentre
				// otras comillas
				if (s.charAt(0) == '\"')
				{
					// ir agregando los tokens a 's', si el ultimo token
					// Agregado es una comilla tons ya akabo con la cadena
					// si no encuentra una comilla marka un error
					s += st.nextToken();

					try
					{
						while (s.charAt(s.length()-1) != 34)
						{
							s += st.nextToken();
						}
					}
					catch(Exception e)
					{
						System.out.println("Error, no encontre el fin de la cadena.");
						System.out.println("Linea: "+numero_linea);
						return 1;
					}
				}

				//checar si es un white_space
				espacio = (s.length() == 1) && (s.charAt(0)==32);

				// si no es un espacio tons agregar este nuevo token al codigo fuente
				if (!espacio) pf += s + "\n";

			} //while de cada token

		} //for de cada linea

		//aqui el programa fuente ya tiene los tokens en cada linea
		//ahora hay que compararlos con las palabras reservadas
		PROGRAMA_FUENTE = pf;

		pf = "";

		lineas = PROGRAMA_FUENTE.split("\n");
		for(int a=0; a < lineas.length; a++)
		{
			if (m_Tokens.containsKey(lineas[a]))
			{
				pf += m_Tokens.get(lineas[a]) + "\n";
			}
			else
			{
				// si no encontro palabra puede ser un
				// identificador, un valor, o una estupidez
				// para tronar el programa ahhh o el numero de linea, o una cadena

				String token = lineas[a];
				boolean yaloencontre = false;

				// intentemos ver si es un numero
				try
				{
					int num = Integer.parseInt(token);
					pf += "VALOR_NUMERO_" + lineas[a] + "\n";
					yaloencontre = true;
				}
				catch(Exception e)
				{
					// Nada que hacer
				}

				// VEAMOS SI ES UN NUMERO DE LINEA
				if (token.startsWith("NUMERO_LINEA"))
				{
					pf += lineas[a] + "\n";
					yaloencontre = true;
				}

				// si esque aun no lo he encontrado
				// ps haber si es un identificador
				// valido de java
				if (Character.isJavaIdentifierStart(token.charAt(0)) && !yaloencontre)
				{
					pf += "IDENTIFICADOR_" + lineas[a] + "\n";
					yaloencontre = true;
				}

				// haber si es un string
				if ((token.charAt(0) == '\"') && !yaloencontre)
				{
					pf += "STRING_" + token + "\n";
					yaloencontre = true;
				}

				// si no es nada de esto... ps algo anda mal
				if (!yaloencontre)
				{
					System.out.println("Encontre algo que no es una expresion.");

					// hay que buscar en que linea esta el error
					// regresando hasta encontrar una linea
					for(int c = a; c >= 0; c--)
					{
						if (lineas[c].startsWith("NUMERO_LINEA"))
						{
							// TODO: que muestre la linea completa
							System.out.println("Linea: "+ lineas[c].substring(13,lineas[c].length()));
							System.out.println("Token: "+ token);
							return 1;
						}
					}
				}
			} //if de si no es una palabra reservada
		} //For de cada linea

		pf += "FINAL\n";
		PROGRAMA_FUENTE = pf;

		return 0;
	}
}

