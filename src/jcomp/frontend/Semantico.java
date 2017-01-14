package jcomp.frontend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import jcomp.util.Log;

/**
 *
 * XML para las llamadas dentro de las llamadas, ejemplo:
 * #met1(43, #met2(a) , t)
 * se convierte en:
 * <llamada met1> 43 , <llamada met2> a </llamada> , t</llamada>
 * y asi se forma el arbol.
 *  *
 *
 * */
public class Semantico
{
	private String m_Codigo;
	private Log m_Debug;
	private Variables [] m_Variables;
	private Metodos [] m_Metodos;

	public Semantico()
	{
		this.m_Debug = Log.getInstance();
	}

	public void setCodigo(String codigo)
	{
		m_Codigo = codigo;
	}

	public String getCodigo()
	{
		return m_Codigo;
	}

	public int iniciar()
	{
		m_Debug.imprimirEncabezado("ANALISIS SEMANTICO");
		int resultado = 0;

		try
		{
			resultado = iniciarInterno();
		}
		catch (Exception e)
		{
			System.err.println(e);
			imprimirObjetos();
			return 1;
		}

		return 0; // resultado
	}

	private int iniciarInterno() throws Exception
	{
		// Convertimos los tokens que me pasa el sintactico en objetos. Metodos y Variables
		crearObjetos();

		// -------------------------------------
		// No modifica el codigo
		// -------------------------------------

		// Revisar que no se repitan los nombres de los metodos ni variables
		if (revisarDefiniciones() != 0) return 1;

		// -------------------------------------
		// Modifica el codigo
		// -------------------------------------

		// revisar las declaraciones del cuerpo que no sean iguales, tambien
		// convierte declaraciones en el nuevo tag <declaracion>
		if (revisarCuerpoVariables() != 0) return 1;

		if (convertirArreglos() != 0) return 1;

		// convertir las asignaciones en la nueva nomenclatura xml
		if (convertirAsignaciones() != 0) return 1;

		// revisar que las llamada a metodos existan...
		// osea que exista una definicion para la llamada a ese metodo
		if (revisarExistenciaMetodos() != 0) return 1;

		// convertir llamdas a la nueva nomenclatura
		if (convertirLLamadas() != 0) return 1;

		// verificar que existan las variables que se estan usando y que se declaren antes de ser usadas
		if (revisarExistenciaVariables() != 0) return 1;

		// convertir el token IDENTIFICADOR_variable1
		// en <var-variable1>
		if (convertirVariables() != 0) return 1;

		// revisar que las llamadas tengan el numero exacto de argumentos
		// que la definicion del metodo recibe
		if (revisarArgumentosDeLLamadas() != 0) return 1;

		if (revisarReturn() != 0) return 1;

		// arreglar todo pa que se vea bonito y simetrico
		if (arreglar() != 0) return 1;

		// continuar construllendo el arbol...

		boolean error = false;

		// para ahorrarme un for dentro de cada metodo..
		for (int iMetodo = 0; iMetodo < m_Metodos.length; iMetodo++)
		{
			// convertir las operaciones en la nueva nomenclatura
			m_Metodos[iMetodo].setCuerpo(convertirOP(m_Metodos[iMetodo].getCuerpo()));

			// eliminar los parentesis que ya de nada sirven
			m_Metodos[iMetodo].setCuerpo(eliminarParentesis(m_Metodos[iMetodo].getCuerpo()));

			error = error || !revisarRetorno(m_Metodos[iMetodo].getCuerpo(), iMetodo);

			// hacer un analisis dimensional
			error = error || !analisisDimensional(m_Metodos[iMetodo].getCuerpo(), iMetodo);
		}

		imprimirObjetos();

		if (error) return 1;

		//buscar un main
		if (buscarMain() != 0) return 1;

		// Cambiar el nombre de las variables agregarles el nombre del metodo, para que si se
		// ddeclaran variables con el mismo nombre en distintos metodos, estas sean consideradas diferentes
		//nombresDeVariables();

		//	GENERAR EL CODIGO INTERMEDIO
		generarCodigo();

		//	DESPUES DE ESTO... SE TERMINA EL ANALISIS DE CODIGO
		//	este metodo, pasa los objetos en una cadena de texto, y la guarda
		//	en la variable codigo... la cual se puede obtener con un get de esta
		//	clase
		return 1;
	}

	void nombresDeVariables()
	{
		for (int a = 0; a < m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");
			String linea = m_Metodos[a].getLinea();

			String cuerpo = "";

			for (int b = 0; b<token.length; b++)
			{
				if (token[b].indexOf(" id:") != -1)
				{
					String s1 = token[b].substring(0, token[b].indexOf(" id:")+4);
					String s2 = token[b].substring(token[b].indexOf(" id:")+4);
					token[b] = s1+"_"+m_Metodos[a].getNombre().substring(14)+"_"+s2;
				}

				if ((token[b].indexOf("args:NADA") == -1)&&(token[b].startsWith("<METODO")))
				{
					String s1 = token[b].substring(0, token[b].indexOf(" args:")+6);
					String s2 = token[b].substring(token[b].indexOf(" regresa:"));

					String [] argus = token[b].substring(token[b].indexOf(" args:")+6, token[b].indexOf(" regresa:")).split(" ");
					for (int j = 1; j<argus.length; j = j+2)
					{
						argus[j] = "_"+m_Metodos[a].getNombre().substring(14) + "_"+argus[j];
					}

					String argust = s1;
					for (int u = 0; u<argus.length; u++)
					{
						argust += argus[u] + " ";
					}

					argust += s2;
					token[b] = argust;
				}

				cuerpo += token[b] + "\n";;
			}

			m_Metodos[a].setCuerpo(cuerpo);
		} //cada metodo
	} //metodo nombres de variables

	boolean revisarRetorno(String body, int iMetodo)
	{
		boolean regresa = !m_Metodos[iMetodo].getTipoDeRetorno().equals("TIPO_VOID");

		if (body.indexOf("<retorno") != -1)
		{
			int u = body.indexOf("<retorno");

			String uu = body.substring(body.indexOf("<retorno linea:")+15, body.indexOf("<retorno linea:")+19);
			uu = uu.substring(0, uu.indexOf(">"));

			// si hay return y no deberia
			if (!regresa)
			{
				System.err.println("Imposible regresar algo de un metodo que es void.");
				System.err.println("Linea: "+uu);
				System.err.print("Metodo "+m_Metodos[iMetodo].getNombre().substring(14));
				return false;
			}
		}
		else
		{
			// No hay return y deberia
			if (regresa)
			{
				System.err.println("Linea: " + m_Metodos[iMetodo].getLinea().substring(13, m_Metodos[iMetodo].getLinea().length()-1));
				System.err.print("Metodo `"+m_Metodos[iMetodo].getNombre().substring(14));
				System.err.println("` debe regresar alguna expresion que evalue a `" + m_Metodos[iMetodo].getTipoDeRetorno() + "`.");
				return false;
			}
		}
		return true;
	}

	// Validar que los tipos coincidan en:
	//  Asignacion a variables
	//  LLamadas a metodos
	//  Operaciones aritmeticas
	boolean analisisDimensional(String body, int iMetodo) throws Exception
	{
		String token[] = body.split("\n");

		boolean CAMBIO = true;
		while (CAMBIO)
		{
			CAMBIO = false;

			body = "";
			for (int h = 0; h < token.length; h++)
				if (!token[h].equals(""))
					body += token[h] + "\n";

			token = body.split("\n");

			for (int a = 0; a < token.length; a++)
			{
				if (token[a].startsWith("<declaracion tipo:VOID "))
				{
					String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
					System.err.println("Linea: "+linea);
					System.err.println("Declaracion no valida.");
					return false;
				}
			}

			body = String.join("\n", token);
			token = body.split("\n");

			boolean cambio = true;
			while (cambio)
			{
				cambio = false;
				for (int a = 0; a<token.length; a++)
				{
					if (token[a].startsWith("<llamada") && token[a+1].equals("</llamada>"))
					{
						token[a] = "<" + token[a].substring(token[a].indexOf("tipo:") + 5, token[a].lastIndexOf(" id:")) + ">";
						token[a+1] = "";
						cambio = true;
						CAMBIO = true;
					}
				}
			}

			body = String.join("\n", token);
			token = body.split("\n");

			cambio = true;
			while (cambio)
			{
				cambio = false;
				for (int a = 0; a<token.length; a++)
				{
					if (token[a].startsWith("<INT[] ")) // Nota el espacio al final de "<INT[] "
					{
						token[a] = "<INT[]>";
						cambio = true;
						CAMBIO = true;
					}

					if (token[a].startsWith("<INT "))
					{
						token[a] = "<INT>";
						cambio = true;
						CAMBIO = true;
					}

					if (token[a].startsWith("<STRING "))
					{
						token[a] = "<STRING>";
						cambio = true;
						CAMBIO = true;
					}
				}
			}

			// Unir los tokes de nuevo en un string
			body = String.join("\n", token);
			token = body.split("\n");

			cambio = true;
			while (cambio)
			{
				cambio = false;
				for (int a = 0; a<token.length; a++)
				{
					if (token[a].startsWith("<llamada"))
					{
						int b = a+1;
						boolean not_good = false;

						while (!token[b].equals("</llamada>"))
						{
							if (token[b].startsWith("<op"))not_good = true;
							if (token[b].startsWith("<llamada"))not_good = true;
							b++;
						}

						if (!not_good)
						{
							String texto = "";
							for (int i = a; i<= b; i++)
								if (!token[i].equals(""))
									texto += token[i]+"%";

							/////////////////////////////////////////////////////////////////////////////
							// revisar esta llamada a metodo para ver si los tipos coniciden
							// aqui reviso algo importante, es mas facil llamar al metodo desde aqui
							if (revisarTipoArgumentos(texto) != 0)
								return false;

							token[a] = "<"+token[a].substring(token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" id:"))+">";

							for (int i = a+1; i<= b; i++)
							{
								token[i] = "";
							}
							cambio = true;
							CAMBIO = true;
						}
					}
				}
			}

			body = "";
			for (int h = 0; h<token.length; h++)
				if (!token[h].equals(""))
					body += token[h] + "\n";

			token = body.split("\n");

			cambio = true;
			while (cambio)
			{
				cambio = false;
				for (int a = 0; a < token.length; a++)
				{
					if (token[a].startsWith("<op "))
					{
						int b = a+1;
						boolean not_good = false;

						while (!token[b].equals("</op>"))
						{
							if (token[b].startsWith("<op")) not_good = true;
							if (token[b].startsWith("<llamada")) not_good = true;
							b++;
						}

						if (!not_good)
						{
							String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
							String operacion = token[a].substring(token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" "));

							if ((token[a+1].equals("<STRING>") || token[a+2].equals("<STRING>")))
							{
								System.err.print("Operador "+operacion+" ");
								System.err.println("no puede ser aplicado a : "+token[a+1]+" y "+token[a+2]);
								System.err.println("Linea: "+linea);
								return false;
							}

							token[a]= token[a+1];
							for (int i = a+1; i<= b; i++) token[i] = "";

							cambio = true;
							CAMBIO = true;
						}

					}
				}
			}

			body = "";
			for (int h = 0; h<token.length; h++)
				if (!token[h].equals(""))
					body += token[h] + "\n";

			token = body.split("\n");

			cambio = true;
			while (cambio)
			{
				cambio = false;
				for (int a = 0; a < token.length; a++)
				{
					//if (token[a].startsWith("<asignacion "))
					//{
					//	int b = a + 1;
					//	boolean not_good = false;

					//	while (!token[b].equals("</asignacion>"))
					//	{
					//		if (token[b].startsWith("<op")) not_good = true;
					//		if (token[b].startsWith("<llamada")) not_good = true;
					//		if (token[b].startsWith("<arreglo")) not_good = true;
					//		b++;
					//	}

					//	if (!not_good)
					//	{
					//		String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
					//		String recibe = token[a].substring(token[a].indexOf("tipo:")+5, token[a].indexOf(" id:"));
					//		String _id = token[a].substring(token[a].indexOf("id:")+3, token[a].lastIndexOf(" "));
					//		recibe = "<" + recibe + ">";

					//		if (!recibe.equals(token[a+1]))
					//		{
					//			System.err.println("Linea: "+linea);
					//			System.err.println("Tipos incompatibles.");
					//			System.err.println("Encontrado:"+token[a+1]);
					//			System.err.println("Requerido:"+recibe);
					//			return false;
					//		}

					//		token[a] = recibe;
					//		for (int i = a+1; i<= b; i++) token[i] = "";

					//		cambio = true;
					//		CAMBIO = true;
					//	}
					//}
				}
			}

			body = "";
			for (int h = 0; h<token.length; h++)
				if (!token[h].equals(""))
					body += token[h] + "\n";

			token = body.split("\n");

			cambio = true;
			while (cambio)
			{
				cambio = false;
				for (int a = 0; a<token.length; a++)
				{
					if (token[a].startsWith("<retorno linea:") && token[a+2].equals("</retorno>"))
					{
						String tipejo = "TIPO_"+token[a+1].substring(1,token[a+1].length()-1);
						String ret = m_Metodos[iMetodo].getTipoDeRetorno();
						if (!tipejo.equals(ret))
						{
							System.err.print("Linea : ");
							System.err.println(token[a].substring(token[a].indexOf(" linea:")+7, token[a].length()-1));
							System.err.print("Metodo `"+m_Metodos[iMetodo].getNombre().substring(14));
							System.err.print("` debe regresar " + ret);
							System.err.println(" pero " +tipejo+" encontrado. ");
							return false;
						}
					}
				}
			}
		} // while (CAMBIO)

		body = String.join("\n", token);
		return  true;
	}

	// Revisar que la llamada al metodo sea correcta incluyendo los tipos
	// <llamada tipo:VOID id:putc linea:20>%<INT>%</llamada>%
	int revisarTipoArgumentos(String llamadaAMetodoTags) throws Exception
	{
		String llamadaNombreMetodo = llamadaAMetodoTags.substring(llamadaAMetodoTags.indexOf(" id:")+4, llamadaAMetodoTags.indexOf(" linea:"));
		String llamadaLinea = llamadaAMetodoTags.substring(llamadaAMetodoTags.indexOf(" linea:")+7, llamadaAMetodoTags.length()-1);
		String llamadaArgumentos = llamadaAMetodoTags.substring(llamadaAMetodoTags.indexOf("%")+1, llamadaAMetodoTags.length()-12);

		// buscar el metodo que estamos llamando para saber cuales son los argumentos correctos
		String metodoArgumentos = "";
		for (int b = 0; b<m_Metodos.length; b++)
			if (m_Metodos[b].getNombre().substring(14).equals(llamadaNombreMetodo))
				metodoArgumentos = m_Metodos[b].getArgumentos();

		if (!compararArgumentos(metodoArgumentos, llamadaArgumentos))
		{
			System.err.println("Linea : " + llamadaLinea);
			System.err.println("Error en argumentacion de metodo.");
			System.err.println("Requerido : #" + llamadaNombreMetodo +" ("+ metodoArgumentos + ")");
			System.err.println("Encontrado: #" + llamadaNombreMetodo +" ("+ llamadaArgumentos + ")");

			throw new Exception();
		}

		return 0;
	}

	// Validar si los tipos son iguales en tokens que pueden tener ids, por ejemplo:
	//
	//   <INT-j>         y <INT>
	//   <INT[]-arreglo> y <INT[27] id:buffer scope:local linea:25>
	//   <INT-a> <INT-b> y <INT>%<coma>%<INT>
	//
	// deben ser compatibles ya que son los mismos tipos.
	//
	private boolean compararArgumentos(String metodoArgumentos, String llamadaArgumentos)
	{
		// las comas no importan, porque vamos a hacer tokens basados en `<`
		String [] metodoArgTokens = metodoArgumentos.replaceAll("\\%", "").replaceAll("<coma>", "").split("<");
		String [] llamadaArgTokens = llamadaArgumentos.replaceAll("\\%", "").replaceAll("<coma>", "").split("<");

		if (metodoArgTokens.length != llamadaArgTokens.length)
		{
			return false;
		}

		nextTag:
		for (int i =0; i < metodoArgTokens.length; i++)
		{
			// comparar caracter por caracter
			for (int j = 0; j < metodoArgTokens[i].length(); j++)
			{
				if (metodoArgTokens[i].charAt(j) == llamadaArgTokens[i].charAt(j))
				{
					continue;
				}

				// encontramos loa diferencia, vamos a ver si lo que
				// tenemos hasta ahora es valido
				switch (metodoArgTokens[i].substring(0, j))
				{
					case "INT":
					case "INT[":
					case "INT[]":
						continue nextTag;

					default:
						return false;
				}
			}
		}

		return true;
	}

	int buscarMain()
	{
		int indiceDeMain = -1;

		for (int a = 0; a<m_Metodos.length; a++)
		{
			if (m_Metodos[a].getNombre().substring(14).equals("main")
				&& m_Metodos[a].getArgumentos().equals("NADA")
				&& m_Metodos[a].getTipoDeRetorno().equals("TIPO_INT"))
			{
				indiceDeMain = a;
			}
		}

		if (indiceDeMain == -1)
		{
			System.err.println("WARNING: Metodo void #main() no existe.");
		}

		if (indiceDeMain != 0)
		{
			// Poner main al inicio
			Metodos temp = m_Metodos[indiceDeMain];
			m_Metodos[indiceDeMain] = m_Metodos[0];
			m_Metodos[0] = temp;
		}

		return 0;
	}

	void generarCodigo()
	{
		m_Codigo = "";

		// Bsucar declaracion de variables globales
		for (int a = 0; a<m_Variables.length; a++)
		{
			m_Codigo += "<declaracion global tipo:"+m_Variables[a].getTipo().substring(5)
				+ " id:" +m_Variables[a].getNombre().substring(14)+">\n";
		}

		// Buscar declaracion de metodos
		for (int a = 0; a<m_Metodos.length; a++)
		{
			m_Codigo += "<METODO id:"+m_Metodos[a].getNombre().substring(14)+" ";
			m_Codigo += "args:";

			if (!m_Metodos[a].getArgumentos().equals("NADA"))
			{
				String [] args = m_Metodos[a].getArgumentos().split(" ");
				for (int c = 0; c<args.length; c++)
				{
					args[c] = args[c].substring(1, args[c].length()-1);
					String f [] = args[c].split("-");
					m_Codigo += f[0]+" "+f[1] + ", ";
				}
				m_Codigo = m_Codigo.substring(0, m_Codigo.length()-2) + " ";
			}
			else
			{
				m_Codigo += "NADA ";
			}

			m_Codigo += "regresa:" + m_Metodos[a].getTipoDeRetorno().substring(5) ;
			m_Codigo += ">\n";

			// Iterar por el cuerpo del metodo
			String tokens [] = m_Metodos[a].getCuerpo().split("\n");
			String newBody = "";
			for (int t = 0; t < tokens.length; t++)
			{
				// Remover todo antes de "linea:xx"
				if (tokens[t].indexOf(" linea:") != -1)
				{
					// excepto en los while:
					if (tokens[t].indexOf("<while") != -1)
					{
						m_Codigo += tokens[t]+"\n";
						continue;
					}

					// excepto en los if:
					if (tokens[t].indexOf("<if") != -1)
					{
						m_Codigo += tokens[t]+"\n";
						continue;
					}

					tokens[t] = tokens[t].substring(0, tokens[t].indexOf(" linea:"))+">";
				}

				m_Codigo += tokens[t]+"\n";
			}

			m_Codigo += "</METODO>\n";
		} //for de cada metodo
	} //metodo

	String eliminarParentesis(String body)
	{
		String [] token = body.split("\n");

		for (int a = 0; a<token.length; a++)
		{
			if (token[a].equals("<parentesis>") || token[a].equals("</parentesis>"))
			{
				token[a]="";
			}
		}

		body ="";
		for (int a = 0; a<token.length; a++)
		{
			if (!token[a].equals("")) body += token[a] + "\n";
		}

		return body;
	}

	///
	//	<INT valor:5 linea:19 scope:local>
	//	<operacion tipo:MUL linea:19>
	//	<INT id:a linea:19 scope:local>
	//
	//	convertir eso en :
	//
	//	<op tipo:MUL linea:19>
	//		<INT valor:5 linea:19>
	//		<INT id:a linea:19>
	//	</op>
	String convertirOP(String body)
	{
		boolean cambio = true;

		while (cambio)
		{
			cambio = !cambio;
			String [] token = body.split("\n");

			// primero ver donde esta la operacion mas profunda...
			int tabs = 0;
			int mayor = 0;
			int mayor_token = 0;

			int lugar_operacion = 0; //donde esta la operacion
			int lugar_inicio_a = 0;
			int lugar_fin_a = 0;
			int lugar_inicio_b = 0;
			int lugar_fin_b = 0;

			for (int a = 0; a < token.length; a++)
			{
				if (token[a].startsWith("</")) tabs--;

				if (token[a].startsWith("<llamada")) tabs++;
				if (token[a].startsWith("<llave>")) tabs++;
				if (token[a].startsWith("<izquierdo")) tabs++;
				if (token[a].startsWith("<derecho")) tabs++;
				if (token[a].startsWith("<asignacion")) tabs++;
				if (token[a].startsWith("<parentesis>")) tabs++;
				if (token[a].startsWith("<if")) tabs++;
				if (token[a].startsWith("<while")) tabs++;
				if (token[a].startsWith("<op")) tabs++; // El token completo se llama <operacion

				if (token[a].startsWith("<operacion"))
				{
					if (tabs > mayor)
					{
						mayor = tabs;
						mayor_token = a;
					}
				}

			}

			if (mayor == 0) return body;

			cambio = true;
			lugar_operacion = mayor_token;

			String argumento_a = token[mayor_token-1];
			String argumento_b = token[mayor_token+1];

			// ahora guardar los argumentos que recibe la operacion
			// si el que esta antes es una llamada o parentesis
			int _a = mayor_token-1;
			if (argumento_a.startsWith("</llamada>")
					|| argumento_a.startsWith("</parentesis>")
					|| argumento_a.startsWith("</op>"))
			{
				String buscando = "";

				if (argumento_a.equals("</parentesis>")) buscando = "<parentesis>";
				if (argumento_a.startsWith("</llamada")) buscando = "<llamada";
				if (argumento_a.startsWith("</op>")) buscando = "<op ";

				int closure_m = 0;
				int closure_p = 0;
				int closure_o = 0;
				_a--;

				while (true)
				{
					if (token[_a].startsWith(buscando) && closure_m == 0 && closure_p == 0 && closure_o == 0) break;

					if (token[_a].equals("</llamada>")) closure_m++;
					if (token[_a].startsWith("<llamada")) closure_m--;
					if (token[_a].equals("</parentesis>")) closure_p++;
					if (token[_a].equals("<parentesis>")) closure_p--;
					if (token[_a].equals("</op>")) closure_o++;
					if (token[_a].startsWith("<op ")) closure_o--;

					--_a;
				}

				argumento_a = "";
				for (int _b = _a; _b<mayor_token; _b++)
					argumento_a += token[_b]+"\n";

				lugar_inicio_a = _a;
				lugar_fin_a = mayor_token-1;
			}
			else
			{
				lugar_inicio_a = _a;
				lugar_fin_a = lugar_inicio_a;
				argumento_a += "\n";
			}

			//si el que esta despues es una llamada o parentesis
			_a = mayor_token+1;
			if (argumento_b.startsWith("<llamada") || argumento_b.equals("<parentesis>") || argumento_b.startsWith("<op "))
			{
				String buscando ="";
				if (argumento_b.equals("<parentesis>")) buscando = "</parentesis>";
				if (argumento_b.startsWith("<llamada")) buscando = "</llamada";
				if (argumento_b.startsWith("<op ")) buscando = "</op>";

				int aperture_m = 0;
				int aperture_p = 0;
				int aperture_o = 0;
				_a++;
				while (true)
				{
					if (token[_a].startsWith(buscando) && aperture_m == 0 && aperture_p == 0 && aperture_o == 0) break;

					if (token[_a].equals("</llamada>")) aperture_m--;
					if (token[_a].startsWith("<llamada")) aperture_m++;
					if (token[_a].equals("</parentesis>")) aperture_p--;
					if (token[_a].equals("<parentesis>")) aperture_p++;
					if (token[_a].equals("</op>")) aperture_p--;
					if (token[_a].startsWith("<op ")) aperture_p++;
					++_a;
				}

				argumento_b = "";
				for (int _b = mayor_token+1; _b<=_a; _b++)
					argumento_b += token[_b]+"\n";

				lugar_inicio_b = mayor_token+1;
				lugar_fin_b = _a;
			}
			else
			{
				lugar_inicio_b = mayor_token+1;
				lugar_fin_b = lugar_inicio_b;
				argumento_b += "\n";
			}

			String ope = token[lugar_operacion];

			for (int b = lugar_inicio_a; b<= lugar_fin_b; b++)
				token[b] = "";

			token[lugar_inicio_a] = "<op "+ope.substring(11)+"\n" + argumento_a + argumento_b + "</op>";

			body = "";
			for (int b = 0; b<token.length; b++)
				if (!token[b].equals("")) body += token[b]+ "\n";

		} //while de si hubo cambios

		return body;
	} //convertir OPeraciones a la nueva nomenclatura

	int arreglar()
	{
		for (int a = 0; a<m_Metodos.length; a++)
		{
			// Primero voy a hacer un vector con las varibles, para poder ponerle de que tipo es
			String [] token = m_Metodos[a].getCuerpo().split("<declaracion-");
			String [] args = m_Metodos[a].getArgumentos().split(" ");

			// Total de variables locales es numero de <declaraciones> + el numero de Argumentos + variables globales.
			int total = token.length + args.length - 1 + m_Variables.length;
			if (m_Metodos[a].getArgumentos().equals("NADA"))
			{
				total--;
			}

			// Crear un arreglo para guardar variables locales
			String [][] variables = new String[total][3];

			// Para ir llenando variables
			int declaracion_actual = 0;

			// Es esto necesario ?
			for (int u = 0; u<variables.length; u++)
			{
				variables[u][0] = "";
				variables[u][1] = "";
				variables[u][2] = "";
			}

			// Creo que estas son las variables globales
			for (int z = 0; z<m_Variables.length; z++)
			{
				variables[declaracion_actual][0] = m_Variables[z].getNombre().substring(14);
				variables[declaracion_actual][1] = m_Variables[z].getTipo().substring(5);
				variables[declaracion_actual][2] = "global";
				declaracion_actual++;
			}

			// Argumentos del metodo
			args = m_Metodos[a].getArgumentos().split(" ");
			if (!m_Metodos[a].getArgumentos().equals("NADA"))
			{
				for (int z = 0; z<args.length; z++)
				{
					String [] _b = args[z].split("-");
					variables[declaracion_actual][0] = _b[1].substring(0, _b[1].length()-1);
					variables[declaracion_actual][1] = _b[0].substring(1);
					variables[declaracion_actual][2] = "arg";
					declaracion_actual++;
				}
			}

			// Variables locales
			token = m_Metodos[a].getCuerpo().split("\n");
			for (int b = 0; b<token.length; b++)
			{
				if (token[b].startsWith("<declaracion-"))
				{
					String [] _a = token[b].split("-");
					variables[declaracion_actual][0] = _a[3].substring(0, _a[3].length()-1);
					variables[declaracion_actual][1] = _a[2]; //.substring(0, _a[3].length()-1);
					variables[declaracion_actual][2] = "local";
					declaracion_actual++;
				}
			}

			/////////////////////////////////////
			// HAY QUE REORGANIZAR LOS TOKENS
			// ahora los tokens seran separados por el caracter '\n'
			//
			// <declaracion tipo:int8 id:alan linea:4>
			// <llamada tipo:int id:metodo1 linea:4> </llamada>
			// <int id:var1 linea:5>
			// <int valor:125 linea:5>
			// <string id:var1 linea:5>
			// <string valor:hola linea:5>
			// <asignacion tipo:string id:cadena linea:12> </asignacion>
			// <parentesis> </parentesis>
			// <llave> </llave>
			// <operacion tipo:suma linea:32> </operacion>
			// <operacion tipo:resta linea:32> </operacion>
			// <operacion tipo:mul linea:32> </operacion>
			// <linea linea:5>
			// <if linea:43> </if>
			// <while linea:54> </while>
			// <operacion tipo:bol_mayor linea:3>
			// <operacion tipo:bol_menor linea:3>
			// <operacion tipo:bol_igual linea:3>
			// <coma>
			// <retorno> </retorno>

			String tokens[] = m_Metodos[a].getCuerpo().split("\n");

			String cuerpo = "";
			for (int g = 0; g<tokens.length; g++)
			{
				cuerpo += tokens[g]+"\n";
			}

			cuerpo = reorganizarDeclaraciones(cuerpo, variables);

			cuerpo = reorganizarLLamadas(cuerpo);

			// <var-8-a>
			// <int8 id:var1 linea:5 scope:local>
			cuerpo = reorganizarVars(cuerpo, variables);

			cuerpo = reorganizarNumeros(cuerpo, m_Metodos[a].getLinea());

			cuerpo = reorganizarStrings(cuerpo, m_Metodos[a].getLinea());

			// <asignacion-12-cadena>
			// <asignacion tipo:string id:cadena linea:12> </asignacion>
			cuerpo = reorganizarAsignacion(cuerpo, variables);

			cuerpo = reorganizarParYLLaves(cuerpo);

			cuerpo = reorganizarOperaciones(cuerpo, m_Metodos[a].getLinea());

			cuerpo = reorganizarLineas(cuerpo);

			m_Metodos[a].setLinea("<linea linea:"+m_Metodos[a].getLinea().substring(13)+">");

			cuerpo = reorganizarif(cuerpo, m_Metodos[a].getLinea());

			cuerpo = reorganizarwhile(cuerpo, m_Metodos[a].getLinea());

			cuerpo = reorganizarOpBoleanas(cuerpo, m_Metodos[a].getLinea());

			cuerpo = reorganizarComa(cuerpo);

			cuerpo = reorganizarReturn(cuerpo);

			// ya de nada sirve el punto y coma y la linea
			cuerpo = quitarPuntoComaYLinea(cuerpo);

			// awebooooo ahora si se ve bonito el arbol
			m_Metodos[a].setCuerpo(cuerpo);

		} //for metodos

		return 0;
	} //metodod

	String quitarPuntoComaYLinea(String cuerpo)
	{
		String [] token = cuerpo.split("\n");
		cuerpo = "";
		for (int a = 0; a<token.length; a++)
			if (!(token[a].equals("PUNTUACION_PUNTO_COMA") || token[a].startsWith("<linea")))
				cuerpo += token[a] + "\n";

		return cuerpo;
	}

	String reorganizarReturn(String body)
	{
		String [] token = body.split("\n");
		body = "";
		for (int a = 0; a<token.length; a++)
		{
			if (token[a].startsWith("<retorno"))
			{
				token[a] = "<retorno "+token[a].substring(9);
			}
			body += token[a] + "\n";
		}

		return body;
	}

	String reorganizarComa(String body)
	{
		String [] token = body.split("\n");
		body = "";
		for (int a = 0; a<token.length; a++)
		{
			if (token[a].equals("PUNTUACION_COMA"))
				token[a] = "<coma>";
			body += token[a] + "\n";
		}
		return body;
	}

	String reorganizarOpBoleanas(String body, String linea)
	{
		//<operacion tipo:igual linea:3>
		//<operacion tipo:mayor linea:3>
		//<operacion tipo:menor linea:3>
		//BOL_MAYOR_QUE ASIGNA ASIGNA BOL_MENOR_QUE

		String [] token = body.split("\n");
		body = "";
		String nuevo;

		for (int a = 0; a<token.length; a++)
		{
			nuevo = "";
			if (token[a].startsWith("<linea"))linea = token[a];

			if (token[a].startsWith("BOL_"))
			{
				String g [] = token[a].split("_");
				nuevo += "<operacion tipo:" + g[1] + " linea:" + linea.substring(13, linea.length()-1)+">";
				token[a] = nuevo;
			}

			if (BuscarPatron("ASIGNA ASIGNA", token, a))
			{
				token[a] = "<operacion tipo:IGUAL linea:"+ linea.substring(13, linea.length()-1)+">";
				token[a+1] = "";
			}

			if (!token[a].equals("")) body += token[a]+ "\n";
		}

		return body;
	}

	String reorganizarwhile(String body, String linea)
	{
		String [] token = body.split("\n");

		// Convertir todos los CONTROL_WHILE en <while>
		for (int  a = 0; a<token.length; a++)
		{
			if (token[a].startsWith("<linea")) linea = token[a];

			if (token[a].equals("CONTROL_WHILE"))
			{
				linea = linea.substring(13, linea.length() -1);
				token[a] = "<~while linea:"+linea+">";
				token[a+1] = ""; // Este token en PARENTESIS_ABRE
			}
		}

		// ahora buscar los parentesis que cierran justo despues
		// de un <while>
		boolean cambio = true;
		while (cambio)
		{
			int r = token.length-1;

			for (;;r--)
			{
				if (r<0) { cambio = false; break; }
				if (token[r].startsWith("<~while linea:")) { cambio = true; break; }
			}

			if (!cambio) break;

			token[r] = "<while "+token[r].substring(8);

			int parentesis = 0;
			while (true)
			{
				if (token[r].equals("</parentesis>") && parentesis == 0) break;
				if (token[r].equals("<parentesis>")) parentesis++;
				if (token[r].equals("</parentesis>")) parentesis--;
				r++;
			}
			token[r] = "</while>";
		}

		return String.join("\n", token);
	}

	String reorganizarif(String body, String linea)
	{
		String [] token = body.split("\n");

		for (int  a = 0; a < token.length; a++)
		{
			if (token[a].startsWith("<linea")) linea = token[a];

			if (token[a].equals("CONTROL_IF"))
			{
				linea = linea.substring(13, linea.length() -1);
				token[a] = "<~if linea:"+linea+">";
				token[a+1] = "";
			}
		}

		boolean cambio = true;
		while (cambio)
		{
			int r = token.length - 1;

			// iniciar desde el ultimo token, buscando ~if
			for (;;r--)
			{
				if (r<0) { cambio = false; break; }
				if (token[r].startsWith("<~if linea:")) { cambio = true; break; }
			}

			if (!cambio) break;

			token[r] = "<if "+token[r].substring(5);

			int parentesis = 0;
			while (true)
			{
				if (token[r].equals("</parentesis>") && parentesis == 0) break;
				if (token[r].equals("<parentesis>")) parentesis++;
				if (token[r].equals("</parentesis>")) parentesis--;
				r++;
			}
			token[r] = "</if>";
		}

		return String.join("\n", token);
	}

	String reorganizarLineas(String body)
	{
		String token [] = body.split("\n");
		body = "";

		for (int a = 0; a<token.length; a++)
		{
			if (token[a].startsWith("NUMERO_LINEA_"))
			{
				token[a] = "<linea linea:" + token[a].substring(13) + ">";
			}

			body += token[a]+ "\n";
		}

		return body;
	}

	String reorganizarOperaciones(String body, String linea)
	{
		String [] token = body.split("\n");
		body = "";

		for (int a = 0; a < token.length; a++)
		{
			String nuevo = "";
			if (BuscarPatron("NUMERO_LINEA_*", token, a))
			{
				linea = token[a];
			}

			if (token[a].startsWith("OP_"))
			{
				String g [] = token[a].split("_");
				nuevo += "<operacion tipo:";

				if (g[1].equals("MULTIPLICACION")) g[1] = "MUL";
				if (g[1].equals("DIVISION")) g[1] = "DIV";

				nuevo += g[1] + " linea:" + linea.substring(13)+">";

				token[a] = nuevo;
			}

			body += token[a]+ "\n";
		}

		return body;
	}

	String reorganizarParYLLaves(String body)
	{
		//	<parentesis> </parentesis>
		//	<llave> </llave>
		String [] token = body.split("\n");
		body = "";
		for (int a = 0; a<token.length; a++)
		{
			if (token[a].equals("PARENTESIS_ABRE"))token[a]="<parentesis>";
			if (token[a].equals("PARENTESIS_CIERRA"))token[a]="</parentesis>";
			if (token[a].equals("LLAVE_ABRE"))token[a]="<llave>";
			if (token[a].equals("LLAVE_CIERRA"))token[a]="</llave>";

			body += token[a]+ "\n";
		}
	return body;
	} //metodo


	// Token antes:
	//   <asignacion-12-cadena>
	//
	// Tokens despues:
	//   <asignacion tipo:INT id:z scope:local linea:28>
	//       <INT valor:3 linea:28>
	//   </asignacion>
	//
	//   <asignacion tipo:INT id:a[0] scope:local linea:29>
	//       <INT valor:1 linea:29>
	//   </asignacion>
	String reorganizarAsignacion(String body, String [][] variables)
	{
		String [] token = body.split("\n");
		body = "";

		for (int a = 0; a<token.length; a++)
		{
			String nuevo = "";
			if (!token[a].startsWith("<asignacion-"))
			{
				body += token[a]+ "\n";
				continue;
			}

			token[a] = token[a].substring(1, token[a].length()-1);
			String asignacionPartes [] = token[a].split("-");

			nuevo += "<asignacion tipo:";

			String variableNombre = asignacionPartes[2];
			if (variableNombre.indexOf("[") >= 0)
			{
				variableNombre = variableNombre.substring(0, variableNombre.indexOf("["));
			}

			// buscar esta variable en la lista de variables
			// para determinar su tipo
			int foundIndex = -1;
			for (int b = 0; b < variables.length; b++)
			{
				if (variables[b][0].equals(variableNombre))
				{
					foundIndex = b;
					break;
				}
			}

			if (variables[foundIndex][1].indexOf("[") >= 0)
			{
				nuevo += variables[foundIndex][1].substring(0, variables[foundIndex][1].indexOf("["));
			}
			else
			{
				nuevo += variables[foundIndex][1];
			}

			nuevo += " id:" + asignacionPartes[2];
			nuevo += " scope:" + variables[foundIndex][2];
			nuevo += " linea:" + asignacionPartes[1];
			nuevo += ">";
			token[a] = nuevo;

			body += token[a]+ "\n";
		}
		return body;
	}

	String reorganizarStrings(String body, String linea)
	{
		//STRING_"dfasd"
		String [] token = body.split("\n");
		body = "";

		for (int a = 0; a<token.length; a++)
		{
			String nuevo = "";

			if (token[a].startsWith("NUMERO_LINEA"))linea = token[a];

			if (token[a].startsWith("STRING_"))
			{
				nuevo += "<STRING valor:"+token[a].substring(7);
				nuevo += " linea:"+linea.substring(13)+">";
				token[a] = nuevo;
			}

			body += token[a]+"\n";
		}

		return body;
	}

	String reorganizarNumeros(String body, String linea)
	{
		String [] token = body.split("\n");
		body = "";

		for (int a = 0; a<token.length; a++)
		{
			String nuevo = "";

			if (token[a].startsWith("NUMERO_LINEA"))linea = token[a];

			if (token[a].startsWith("VALOR_NUMERO_"))
			{
				nuevo += "<INT valor:"+token[a].substring(13);
				nuevo += " linea:"+linea.substring(13)+">";
				token[a] = nuevo;
			}

			body += token[a]+"\n";
		}

		return body;
	}

	// Todo: el metodo convertirVariables() organiza de tokens a XML
	//       y este medoto organize de nuevo ese XML a otro estilo de
	//       XML. Hay que modificar convertirVariables() para que llegue
	//       al estilo final sin tener que dar tanta vuelta.
	//
	// Convierte:
	//     <var-8-a>
	//     <int8 id:var1 linea:5 scope:local>
	//
	// variables[][] es un arreglo con las variables locales de este metodo
	String reorganizarVars(String body, String variables[][])
	{
		String [] token = body.split("\n");
		body = "";
		for (int a = 0; a<token.length; a++)
		{
			if (!token[a].startsWith("<var"))
			{
				body += token[a]+"\n";
				continue;
			}

			String f = token[a].substring(1, token[a].length()-1);
			String [] variableParts = f.split("-");
			String nuevo = "<";

			// Buscar este uso de variable en la declaracion
			// para establecer su tipo
			int foundIndex = -1;
			for (int z = 0; z<variables.length; z++)
			{
				if (variables[z][0].equals(nombreDeVariable(variableParts[2])))
				{
					foundIndex = z;
					break;
				}
			}

			if (foundIndex == -1)
			{
				System.err.println("No encontre la variable local: " + variableParts[2]);
				continue;
			}

			// si la variable que estoy procesando es un indice dentro de un arreglo:
			//    <INT[5] id:a[0] scope:local linea:35>
			//        ^^^    ^^^^
			//     VarLoc    EstaVar
			// entonces el tipo en `variables` me diria que es `INT[5]` pero dado que
			// estoy indexando un solo elemento, el tipo resultante es ahora solo `INT`
			//

			String tipoDeVariableLocal = variables[foundIndex][1];

			if (variableParts[2].contains("["))
			{
				// Agregar el tipo
				nuevo += "INT";
			}
			else
			{
				// Agregar el tipo
				nuevo += tipoDeVariableLocal;
			}

			// Agregar el id de la variable....
			nuevo += " id:" + variableParts[2];

			// Agregar el scope de la variable
			nuevo += " scope:"+variables[foundIndex][2];

			// Agregar la linea de la variable
			nuevo += " linea:" + variableParts[1] ;

			nuevo += ">";

			token[a] = nuevo;

			body += token[a]+"\n";
		}
		return body;
	}

	String reorganizarLLamadas(String body)
	{
		//old  <llamada-8-m3> </llamada>
		//new one <llamada tipo:int id:metodo1 linea:4> </llamada>
		String [] token = body.split("\n");
		body = "";
		for (int a = 0; a<token.length; a++)
		{
			String nuevo = "";
			if (token[a].startsWith("<llamada"))
			{
				String f = token[a].substring(1, token[a].length()-1);
				String [] ff = f.split("-");
				nuevo += "<llamada tipo:";
				for (int _fff = 0; _fff < m_Metodos.length; _fff++)
				{
					if (ff[2].equals(m_Metodos[_fff].getNombre().substring(14)))
					{
						nuevo += m_Metodos[_fff].getTipoDeRetorno().substring(5);
					}
				}
				nuevo += " id:" + ff[2]+" linea:"+ff[1]+">";
				token[a] = nuevo;
			}
			body += token[a]+"\n";
		}

		return body;
	}

	//old  <declaracion-7-INT16-a>
	//new one <declaracion tipo:int8 id:alan linea:4>
	String reorganizarDeclaraciones(String body, String[][]variables)
	{
		String [] token = body.split("\n");
		body = "";
		for (int a = 0; a<token.length; a++)
		{
			String nuevo = "";
			if (token[a].startsWith("<declaracion-"))
			{
				nuevo += "<declaracion tipo:";
				String partes [] = token[a].split("-");
				nuevo += partes[2]+" id:";
				nuevo += partes[3].substring(0,partes[3].length()-1)+ " linea:";
				nuevo += partes[1]+">";
				token[a] = nuevo;
			}
			body += token[a]+"\n";
		}

		return body;
	} //metodo reorganizarDeclaraciones

	//convertir el token IDENTIFICADOR_variable1
	//en <var-13-_variable1>
	int convertirVariables()
	{
		for (int a = 0; a<m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");
			String numDeLinea = m_Metodos[a].getLinea().substring(13);
			String cuerpo = "";

			for (int b = 0; b<token.length; b++)
			{
				if (BuscarPatron("NUMERO_LINEA_*", token, b))
				{
					numDeLinea = token[b].substring(13);
				}

				if (BuscarPatron("IDENTIFICADOR_* CORCHETE_ABRE VALOR_NUMERO_* CORCHETE_CIERRA", token, b))
				{
					String indice = token[b+2].substring(13);
					token[b] = "<var-"+numDeLinea +"-"+token[b].substring(14)+"["+ indice +"]>";
					token[b+1] = token[b+2] = token[b+3] = "";
				}

				if (BuscarPatron("IDENTIFICADOR_*", token, b))
				{
					token[b] = "<var-"+numDeLinea +"-"+token[b].substring(14)+">";
				}

				cuerpo += token[b] + "\n";
			}

			m_Metodos[a].setCuerpo(cuerpo);
		} //cada metodo

		return 0;
	} //convertirVariables

	int revisarReturn()
	{
		for (int a = 0; a < m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");
			String linea = m_Metodos[a].getLinea();

			for (int b = 0; b < token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA_")) linea = token[b];

				if (token[b].equals("CONTROL_RETORNO"))
				{
					token[b] = "<retorno-linea:"+linea.substring(13)+">";
					while (!token[b].equals("PUNTUACION_PUNTO_COMA")) b++;
					token[b] = "</retorno>";
				}
			} //for de cada token

			m_Metodos[a].setCuerpo(String.join("\n", token));
		}

		return 0;
	}

	int revisarArgumentosDeLLamadas() throws Exception
	{
		for (int a = 0; a < m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");

			boolean cambio = true;

			while (cambio)
			{
				cambio = false;

				int indiceDeTokenDeLlamada = token.length - 1;

				// Continuar procesando mientras existan mas llamadas a metodos
				for (; indiceDeTokenDeLlamada >= 0; indiceDeTokenDeLlamada--)
				{
					if (token[indiceDeTokenDeLlamada].startsWith("<llamada-"))
					{
						cambio = true;
						break;
					}
				}

				// ya no hay mas llamadas a metodos
				if (!cambio) break;

				// `indiceDeTokenDeLlamada` es el indice donde esta la llamada
				String llamada_a = token[indiceDeTokenDeLlamada].substring(
																	token[indiceDeTokenDeLlamada].lastIndexOf("-")+1,
																	token[indiceDeTokenDeLlamada].length()-1);

				String linea = token[indiceDeTokenDeLlamada].substring(9, token[indiceDeTokenDeLlamada].lastIndexOf("-"));

				token[indiceDeTokenDeLlamada] = "<~llamada-"+token[indiceDeTokenDeLlamada].substring(9);

				indiceDeTokenDeLlamada++;
				int argumentosEnLaLlamada = 0;

				while (true)
				{
					if (token[indiceDeTokenDeLlamada].equals("</llamada>"))
					{
						break;
					}

					// si hay algo dentro de la llamada, es un argumento siempre y cuando no sea una coma `,`
					if (token[indiceDeTokenDeLlamada].trim().length() > 0 && !(token[indiceDeTokenDeLlamada].equals("PUNTUACION_COMA")))
					{
						argumentosEnLaLlamada++;
					}

					token[indiceDeTokenDeLlamada] = "";
					indiceDeTokenDeLlamada++;
				}

				boolean encontrado = false;
				int deberian = 0;
				for (int h = 0; h < m_Metodos.length; h++)
				{
					if (m_Metodos[h].getNombre().substring(14).equals(llamada_a))
					{
						String [] t = m_Metodos[h].getArgumentos().split(" ");

						deberian = t.length;

						if (m_Metodos[h].getArgumentos().equals("NADA")) deberian = 0;

						if (deberian == argumentosEnLaLlamada) encontrado = true;

						break;
					}
				}

				if (!encontrado)
				{
					System.err.println("Linea: "+linea);
					System.err.println("Metodo "+llamada_a+" deberia recibir "+deberian+" argumentos, enviando " + argumentosEnLaLlamada + ".");

					throw new Exception();
				}

			} //por cada cambio hecho
		} //for de cada objeto
		return 0;
	} //revisarArgumentosDeLLamadas()

	int convertirArreglos()
	{
		for (int a = 0; a<m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");
			String linea = m_Metodos[a].getLinea();

			for (int b = 0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA"))
				{
					linea = token[b];
				}

				// asignacion a un elemento de un arreglo usando expresion: a[ i ] = 3;
				if (BuscarPatron("IDENTIFICADOR_* CORCHETE_ABRE", token, b))
				{
					token[b] = "<arreglo-tipo:INT-linea:"+linea.substring(13)+"-id:"+token[b].substring(14) + ">";
					token[++b] = ""; // CORCHETE_ABRE

					while (!token[b].equals("CORCHETE_CIERRA")) { b++; }

					token[b] = "</arreglo>";
				}
			}

			m_Metodos[a].setCuerpo(String.join("\n", token));
		}
		return 0;
	}

	//
	// a[0] = 80;
	//
	// <arreglo-tipo:INT-linea:8-id:a>
	//     <INT valor:0 linea:6>
	// </arreglo>
	// ASIGNA
	// VALOR_NUMERO_4
	// PUNTUACION_PUNTO_COMA
	//
	// <asignacion>
	//    <lvalue>
	//    </lvalue>
	//    <rvalue>
	//    </rvalue>
	// </asignacion>
	//
	int convertirAsignaciones()
	{
		for (int a = 0; a < m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");
			String linea = m_Metodos[a].getLinea();

			for (int b = 0; b < token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA"))
				{
					linea = token[b];
				}

				// Asignacion simple IDENTIFICADOR_* ASIGNA ... PUNTUACION_PUNTO_COMA
				if (BuscarPatron("IDENTIFICADOR_* ASIGNA", token, b) && !BuscarPatron("* ASIGNA ASIGNA", token, b))
				{
					token[b] = "<asignacion-"+linea.substring(13)+"-"+token[b].substring(14)+">";
					token[++b] = "";

					// avanzar hasta encontrar punto y coma
					while (!token[b++].equals("PUNTUACION_PUNTO_COMA"));

					token[b] = "</asignacion>";
				}

				// Asignacion a un elemento de un arreglo: a[1] = 3;
				//
				//    <arreglo-tipo:INT-linea:8-id:a>
				//        VALOR_NUMERO_0
				//    </arreglo>
				//    ASIGNA
				//    VALOR_NUMERO_4
				//    PUNTUACION_PUNTO_COMA
				//
				// Convertir a:
				//
				//    <asignacion>
				//        <izquierdo>
				//            <arreglo-tipo:INT-linea:8-id:a>
				//                VALOR_NUMERO_0
				//            </arreglo>
				//        </izquierdo>
				//        <derecho>
				//            VALOR_NUMERO_4
				//        </derecho>
				//    </asignacion>
				if (BuscarPatron("</arreglo> ASIGNA", token, b) && !BuscarPatron("* ASIGNA ASIGNA", token, b))
				{
					// b = arreglo
					int primerToken = 0, ultimoToken = 0;

					// Regresa hasta encontrar el inicio de <arreglo>
					ArrayList<String> ladoIzquierdo = new ArrayList<String>();
					for (primerToken = b; !token[primerToken].startsWith("<arreglo"); primerToken--)
					{
						ladoIzquierdo.add(token[primerToken]);
					}

					ladoIzquierdo.add(token[primerToken]);

					Collections.reverse(ladoIzquierdo);

					//    <arreglo-tipo:INT-linea:8-id:a>
					String id = ladoIzquierdo.get(0).split("-")[3];
					id = id.substring(0, id.length()-1);

					// quita primero y ultimo
					ladoIzquierdo.remove(0);
					ladoIzquierdo.remove(ladoIzquierdo.size()-1);

					ArrayList<String> ladoDerecho = new ArrayList<String>();
					for (ultimoToken = b + 2; !token[ultimoToken].equals("PUNTUACION_PUNTO_COMA"); ultimoToken++)
					{
						ladoDerecho.add(token[ultimoToken]);
					}

					for (int i = primerToken; i < ultimoToken; i++)
					{
						token[i] = "";
					}


					token[b] = "<asignacion tipo:[]INT "+ id +" linea:0> \n<izquierdo>\n"
							+ String.join("\n", ladoIzquierdo.toArray(new String[1]))
							+ "\n</izquierdo>\n<derecho>\n"
							+ String.join("\n", ladoDerecho.toArray(new String[1]))
							+ "\n</derecho>\n</asignacion>\n";

				}
			}

			m_Metodos[a].setCuerpo(String.join("\n", token));
		}
		return 0;
	}

	int convertirLLamadas()
	{
		for (int a = 0; a < m_Metodos.length; a++)
		{
			String [] token = m_Metodos[a].getCuerpo().split("\n");
			String linea = m_Metodos[a].getLinea();

			for (int b = 0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if (token[b].equals("PUNTUACION_GATO"))
				{
					token[b] = "<~llamada-"+linea.substring(13)+"-"+token[b+1].substring(14)+">";

					while (!token[b].startsWith("IDENTIFICADOR")) b++;
						token[b] = "";

					while (!token[b].equals("PARENTESIS_ABRE")) b++;
						token[b] = "";
				}
			}

			String cuerpo = "";

			m_Metodos[a].setCuerpo(String.join("\n", token));

			token = m_Metodos[a].getCuerpo().split("\n");

			boolean cambio = true;
			while (cambio)
			{
				cambio = false;

				int r = token.length-1;

				for (;;r--)
				{
					if (r<0) break;
					if (token[r].startsWith("<~llamada-")) { cambio = true; break; }
				}

				if (!cambio) break;

				token[r] = "<llamada-"+token[r].substring(10);

				int parentesis = 0;
				while (true)
				{
					if (token[r].equals("PARENTESIS_CIERRA") && parentesis == 0) break;
					if (token[r].equals("PARENTESIS_ABRE")) parentesis++;
					if (token[r].equals("PARENTESIS_CIERRA")) parentesis--;
					r++;
				}

				token[r] = "</llamada>";
			}

			m_Metodos[a].setCuerpo(String.join("\n", token));
		}

		return 0;
	}

	//
	// Contar las diferentes definiciones de variables dados los tokens, ejemplo:
	//
	//   NUMERO_LINEA_2
	//   CONTROL_DEF        <----- definicion de metodo inicia
	//   TIPO_VOID
	//   PUNTUACION_GATO
	//   IDENTIFICADOR_metodo <----- definicion de metodo termina
	//   PARENTESIS_ABRE
	//
	// y popular los arreglos m_Metodos y m_Variables
	//
	void crearObjetos()
	{
		ArrayList<Metodos> listaDeMetodos = new ArrayList<Metodos>();
		ArrayList<Variables> listaDeVariables = new ArrayList<Variables>();

		String [] definiciones = m_Codigo.split("CONTROL_DEF");
		String ultimoNumeroDeLinea = null;

		for (String defCuerpo : definiciones)
		{
			String [] defTokens = defCuerpo.split("\n");

			if (defCuerpo.indexOf("PUNTUACION_GATO") > 0)
			{
				Metodos met = new Metodos();
				met.setTipoDeRetorno(defTokens[1]);
				met.setNombre(defTokens[3]);

				met.setArgumentos(
						defCuerpo.substring(
							defCuerpo.indexOf("PARENTESIS_ABRE") + 15,
							defCuerpo.indexOf("PARENTESIS_CIERRA")));

				met.setArgumentos(met.getArgumentos().replaceAll("\n", " ").trim());

				met.setCuerpo(
						defCuerpo.substring(
							defCuerpo.indexOf("LLAVE_ABRE") + 11,
							defCuerpo.lastIndexOf("LLAVE_CIERRA")));

				met.setCuerpo(met.getCuerpo());

				met.setLinea(ultimoNumeroDeLinea);

				listaDeMetodos.add(met);
			}
			else if (defCuerpo.indexOf("TIPO_") > 0)
			{
				Variables var = new Variables();
				var.setLinea(ultimoNumeroDeLinea);
				var.setNombre(defTokens[2]);
				var.setTipo(defTokens[1]);

				listaDeVariables.add(var);
			}

			// Recordar el ultimo numero de linea en esta definicion
			int ultimoNumeroDeLineaIndice = defCuerpo.lastIndexOf("NUMERO_LINEA");
			if (ultimoNumeroDeLineaIndice >= 0)
			{
				int finDeNumeroLinea = defCuerpo.indexOf("\n", ultimoNumeroDeLineaIndice);
				ultimoNumeroDeLinea = defCuerpo.substring(
											ultimoNumeroDeLineaIndice,
											finDeNumeroLinea);
			}
		}

		m_Variables = listaDeVariables.toArray(new Variables[listaDeVariables.size()]);
		m_Metodos = listaDeMetodos.toArray(new Metodos[listaDeMetodos.size()]);
	}

	int revisarDefiniciones()
	{
		// Revisar metodos con el mismo nombre
		for (int a = 0; a < m_Metodos.length; a++)
		{
			String comp = m_Metodos[a].getNombre();
			for (int b = 0; b < m_Metodos.length; b++)
			{
				if (a == b) b++;

				if (b == m_Metodos.length) break;
				
				if (comp.equals(m_Metodos[b].getNombre()))
				{
					System.err.println("Linea: "+ m_Metodos[b].getLinea().substring(13));
					System.err.println("Metodo "+ comp.substring(14)+" ya esta definido en este programa.");
					m_Debug.imprimirLinea("\nMetodo ya definido !!!");
					return 1;
				}
			}
		}

		// Revisar variables con el mismo nombre
		for (int a = 0; a < m_Variables.length; a++)
		{
			String comp = m_Variables[a].getNombre();
			for (int b = 0; b<m_Variables.length; b++)
			{
				if (a == b) b++;
				if (b == m_Variables.length) break;
				if (comp.equals(m_Variables[b].getNombre()))
				{
					System.err.println("Linea: "+m_Variables[b].getLinea().substring(13));
					System.err.println("Variable "+comp.substring(14)+" ya esta definido en este programa.");
					m_Debug.imprimirLinea("\nVariable ya definida !!!");
					return 1;
				}
			}
		}

		return 0;
	} //revisarDefiniciones()

	// revisar que dentro del cuerpo de los metodos no se declaren las variables dos veces
	// acuerdate que los argumentos que recibe el metodo son tambien declaraciones
	// y que no pueden tener el mismo nombre que declaraciones globales def
	int revisarCuerpoVariables()
	{
		String cuerpo = "";
		String linea = "";
		int declaracion;

		for (int d = 0; d < m_Metodos.length; d++)
		{
			String [] tokens = m_Metodos[d].getCuerpo().split("\n");
			declaracion = 0;

			String argumentos [] = m_Metodos[d].getArgumentos().split(" ");
			for (int h = 0; h < argumentos.length; h++)
			{
				// cuantas declaraciones en los argumentos que recibe el metodo
				if (BuscarPatron("TIPO_* IDENTIFICADOR_*", argumentos, h))
				{
					declaracion++;
				}
			}

			for (int a = 0; a<tokens.length; a++)
			{
				//contar cuantas declaraciones hay en el cuerpo
				if (BuscarPatron("TIPO_* IDENTIFICADOR_* PUNTUACION_PUNTO_COMA", tokens, a))
				{
					declaracion++;
				}

				//contar cuantas declaraciones tipo arreglo
				if (BuscarPatron("TIPO_* IDENTIFICADOR_* CORCHETE_ABRE * CORCHETE_CIERRA PUNTUACION_PUNTO_COMA", tokens, a))
				{
					declaracion++;
				}
			}

			//crear un vector bidimensional.. declaracion-> linea, tipo, id
			String [][] declaraciones = new String[declaracion][3];

			int inicio = 0;
			for (int h = 0; h<argumentos.length; h++)
			{
				if (argumentos[h].startsWith("TIPO_") && argumentos[h+1].startsWith("IDENTIFICADOR_"))
				{
					declaraciones[inicio][0] = m_Metodos[d].getLinea();
					declaraciones[inicio][1] = argumentos[h];
					declaraciones[inicio][2] = argumentos[h+1];
					inicio++;
				}
			}

			linea = m_Metodos[d].getLinea();
			for (int a = 0; a<tokens.length; a++)
			{
				if (tokens[a].startsWith("NUMERO_LINEA"))
				{
					linea = tokens[a];
				}

				if (BuscarPatron("TIPO_* IDENTIFICADOR_* PUNTUACION_PUNTO_COMA", tokens, a))
				{
					declaraciones[inicio][0] = linea;
					declaraciones[inicio][1] = tokens[a];
					declaraciones[inicio][2] = tokens[a+1];
					inicio++;
				}

				if (BuscarPatron("TIPO_* IDENTIFICADOR_* CORCHETE_ABRE * CORCHETE_CIERRA PUNTUACION_PUNTO_COMA", tokens, a))
				{
					declaraciones[inicio][0] = linea;
					declaraciones[inicio][1] = tokens[a];
					declaraciones[inicio][2] = tokens[a+1];
					inicio++;
				}
			} //cada token

			// por fin !! ya tengo la declaraciones de este metodo... ahora ver que no sean iguales
			// o que no sean iguales a alguna definicion global
			// las declaraciones globales estan en el el arreglo m_Variables[].getNombre()
			for (int p = 0; p<declaraciones.length; p++)
			{
				String id = declaraciones[p][2];
				for (int q = 0; q<declaraciones.length; q++)
				{
					if (p == q) q++;
					if (q == declaraciones.length) break;
					if (id.equals(declaraciones[q][2]))
					{
						System.err.println("Linea: "+declaraciones[q][0].substring(13));
						System.err.println("Variable "+ declaraciones[p][2].substring(14)+" ya esta definida localmente.");
						return 1;
					}
				}

				for (int f = 0; f<m_Variables.length; f++)
				{
					if (id.equals(m_Variables[f].getNombre()))
					{
						System.err.println("Linea: "+declaraciones[p][0].substring(13));
						System.err.println("Variable "+ declaraciones[p][2].substring(14)+" ya esta definida globalmente, en la linea "+m_Variables[f].getLinea().substring(13)+".");
						return 1;
					}
				}
			} // for de comprobaciones

			// en vez de tener 3 tokens, hacer unos solo
			// que contenga <declaracion-linea-tipo-id>
			linea = m_Metodos[d].getLinea();
			for (int x = 0; x < tokens.length; x++)
			{
				if (tokens[x].startsWith("NUMERO_LINEA_"))
				{
					linea = tokens[x].substring(13);
					continue;
				}

				// Declaracion de un arreglo:
				if (BuscarPatron("TIPO_* IDENTIFICADOR_* CORCHETE_ABRE VALOR_NUMERO_* CORCHETE_CIERRA PUNTUACION_PUNTO_COMA", tokens, x))
				{
					int tamArreglo = Integer.parseInt(tokens[x+3].substring(13));
					tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"["+tamArreglo+"]-"+tokens[x+1].substring(14)+">";
					tokens[x+1] = tokens[x+2] = tokens[x+3] = tokens[x+4] = tokens[x+5] = "";
				}

				if (BuscarPatron("TIPO_* IDENTIFICADOR_* PUNTUACION_PUNTO_COMA", tokens, x))
				{
					tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+1].substring(14)+">";
					tokens[x+1] = tokens[x+2] = "";
				}

				if (BuscarPatron("TIPO_* IDENTIFICADOR_* NUMERO_LINEA_* PUNTUACION_PUNTO_COMA", tokens, x))
				{
					tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+1].substring(14)+">";
					tokens[x+1] = tokens[x+2];
					tokens[x+2] = tokens[x+3] = "";
				}

				if (BuscarPatron("TIPO_* NUMERO_LINEA_* IDENTIFICADOR_* PUNTUACION_PUNTO_COMA", tokens, x))
				{
					tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+2].substring(14)+">";
					tokens[x+1] = tokens[x+1];
					tokens[x+2] = tokens[x+3] = "";
				}

				if (BuscarPatron("TIPO_* NUMERO_LINEA_* IDENTIFICADOR_* NUMERO_LINEA_* PUNTUACION_PUNTO_COMA", tokens, x))
				{
					tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+2].substring(14)+">";
					tokens[x+1] = tokens[x+3];
					tokens[x+2] = tokens[x+3] = tokens[x+4] = "";
				}

				if (BuscarPatron("TIPO_* CORCHETE_ABRE CORCHETE_CIERRA IDENTIFICADOR_* PUNTUACION_PUNTO_COMA", tokens, x))
				{
					tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"[]-"+tokens[x+3].substring(14)+">";
					tokens[x+1] = "NUMERO_LINEA_" + linea;
					tokens[x+2] = tokens[x+3] = tokens[x+4] = "";
				}
			}

			m_Metodos[d].setCuerpo(String.join("\n", tokens));
		} //for de cada cuerpo de cada metodo

		return 0;
	} //revisarCuerpoVariables()

	int revisarExistenciaMetodos()
	{
		// Antes de empezar quiero limpiar los argumentos
		// el formato   <tipo-id>
		for (int b = 0; b < m_Metodos.length; b++)
		{
			String [] args = m_Metodos[b].getArgumentos().split(" ");

			for (int c = 0; c < args.length; c++)
			{
				if (args[c].startsWith("TIPO_"))
				{
					if (args[c+1].equals("CORCHETE_ABRE"))
					{
						args[c] = "<" + args[c].substring(5) + "[]-" + args[c+3].substring(14)+">";
						args[c+1] = "";
						args[c+2] = "";
						args[c+3] = "";
					}
					else
					{
						args[c] = "<" + args[c].substring(5) + "-" + args[c+1].substring(14)+">";
						args[c+1] = "";
					}
				}

				if (args[c].equals("PUNTUACION_COMA"))
					args[c] = "";
			}

			String new_args = "";

			for (int d = 0; d<args.length; d++)
			{
				if (!args[d].equals(""))
				{
					new_args += args[d]+" ";
				}
			}

			if (new_args.trim().equals(""))
			{
				new_args = "NADA";
			}

			m_Metodos[b].setArgumentos(new_args);
		} //okay con los argumentos

		//ahora si revisar que exista el id del metodo al que se esta llamando
		for (int b = 0; b<m_Metodos.length; b++)
		{
			String [] token = m_Metodos[b].getCuerpo().split("\n");
			String linea = m_Metodos[b].getLinea();

			for (int c = 0; c<token.length; c++)
			{
				if (token[c].startsWith("NUMERO_LINEA_")) linea = token[c];

				if (token[c].equals("PUNTUACION_GATO"))
				{
					String id = token[c+1];
					boolean found = false;

					for (int d = 0; d < m_Metodos.length; d++)
						if (m_Metodos[d].getNombre().equals(id))
							found = true;

					if (!found)
					{
						System.err.println("Linea: "+linea.substring(13));
						System.err.println("Metodo "+ id.substring(14) +" no ha sido definido.");
						return 1;
					}
				} //si encuentra llamada a algun metodo
			}
		} //for de cada cupero de cada metodo

		return 0;
	}

	int revisarExistenciaVariables()
	{
		for (int a = 0; a < m_Metodos.length; a++)
		{
			// contar cuantas declaraciones hay en TOTAL
			String [] token = m_Metodos[a].getCuerpo().split("<declaracion-");
			String [] args = m_Metodos[a].getArgumentos().split(" ");

			int total = token.length + args.length - 1 + m_Variables.length;
			if (m_Metodos[a].getArgumentos().equals("NADA"))
				total--;

			// hacer un recorrido por todos lo tokens
			// ir guardando las declaraciones en el vector variables
			// luego si encuentro algun uso de variable, checar en el vector, si si existe
			// si no, es porque, o aun no ha sido declarada, o nunca ha sido declarada
			HashSet<String> variablesDeclaradas = new HashSet<String>();

			int declaracion_actual = 0;
			String linea = m_Metodos[a].getLinea();

			token = m_Metodos[a].getCuerpo().split("\n");

			for (int z = 0; z < m_Variables.length; z++)
			{
				variablesDeclaradas.add(m_Variables[z].getNombre().substring(14));
			}

			args = m_Metodos[a].getArgumentos().split(" ");

			if (!m_Metodos[a].getArgumentos().equals("NADA"))
			{
				for (int z = 0; z < args.length; z++)
				{
					String [] _b = args[z].split("-");

					// error cuando no hay argumentos
					variablesDeclaradas.add(_b[1].substring(0, _b[1].length()-1));
				}
			}

			// Hasta este punto, String [] variables, contiene todas las variables
			// globales y los argumentos que recibe el metodo.

			for (int b = 0; b < token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if (token[b].startsWith("<declaracion-"))
				{
					String [] partes = token[b].split("-");
					variablesDeclaradas.add(partes[3].substring(0, partes[3].length()-1));
				}

				String id = null;

				if (token[b].startsWith("IDENTIFICADOR_"))
				{
					id = token[b].substring(14);
				}

				if (token[b].startsWith("<arreglo"))
				{

					String [] partes = token[b].split("-");
					id = partes[3].substring(3, partes[3].length() - 1);
				}

				// TODO: Arreglar esto
				if (token[b].startsWith("<asignacion-"))
				{
					String [] partes = token[b].split("-");
					id = partes[2].substring(0, partes[2].length() - 1);

					if (id.contains("["))
					{
						id = id.substring(0, id.indexOf("["));
					}
				}

				if (token[b].startsWith("<asignacion "))
				{
					String [] partes = token[b].split(" ");
					id = partes[2].substring(3, partes[2].length());
				}

				if (id != null && !variablesDeclaradas.contains(id))
				{
					System.err.println("Linea: " + linea.substring(13));
					System.err.println("Variable " + id + " es asignada antes de ser declarada.");
					return 1;
				}

			} //for de cada token
		} //for de metodos

		return 0;
	}

	private String nombreDeVariable(String nombreDelArreglo)
	{
		int llave = nombreDelArreglo.indexOf("[");
		if (llave >= 0)
		{
			nombreDelArreglo = nombreDelArreglo.substring(0, llave);
		}

		return nombreDelArreglo;
	}

	private boolean BuscarPatron(String patron, String [] tokens, int inicio)
	{
		boolean encontrado = true;
		String [] patronTokens = patron.split(" ");

		if (inicio + patronTokens.length > tokens.length)
		{
			return false;
		}

		for (String pT : patronTokens)
		{
			if (pT.indexOf("*") >= 0)
			{
				pT = pT.replaceAll("\\*", "");
				encontrado = tokens[inicio].startsWith(pT);
			}
			else
			{
				encontrado = tokens[inicio].equals(pT);
			}

			if (encontrado == false) break;
			inicio++;
		}

		return encontrado;
	}

	void imprimirObjetos()
	{
		m_Debug.imprimirLinea("-------- Objetos -----");

		for (int z = 0; z < m_Metodos.length; z++)
		{
			m_Debug.imprimirLinea(" Metodo    :" +z);
			m_Debug.imprimirLinea(" Linea     : " + m_Metodos[z].getLinea());
			m_Debug.imprimirLinea(" Nombre    : " + m_Metodos[z].getNombre());
			m_Debug.imprimirLinea(" Retorno   : " + m_Metodos[z].getTipoDeRetorno());
			m_Debug.imprimirLinea(" Argumentos: " + m_Metodos[z].getArgumentos());
			m_Debug.imprimirLinea(" Cuerpo    :");

			String linea [] = m_Metodos[z].getCuerpo().split("\n");
			int tabs = 0;

			for (int _h = 0; _h < linea.length; _h++)
			{
				if (linea[_h].startsWith("</")) tabs--;

				if (linea[_h].length() ==  0) continue;

				for (int b = 0; b<tabs; b++) m_Debug.imprimir("    ");

				m_Debug.imprimirLinea("    " + linea[_h]);

				if (linea[_h].startsWith("<llamada")
					|| linea[_h].startsWith("<llave>")
					|| linea[_h].startsWith("<asignacion")
					|| linea[_h].startsWith("<arreglo")
					|| linea[_h].startsWith("<parentesis>")
					|| linea[_h].startsWith("<if")
					|| linea[_h].startsWith("<while")
					|| linea[_h].startsWith("<op ")
					|| linea[_h].startsWith("<izquierdo")
					|| linea[_h].startsWith("<derecho")
					|| linea[_h].startsWith("<retorno"))
				{
					tabs++;
				}
			}
		}

		for (int z = 0; z<m_Variables.length; z++)
		{
			m_Debug.imprimirLinea("\n Variable "+z);
			m_Debug.imprimirLinea(" Linea: " + m_Variables[z].getLinea());
			m_Debug.imprimirLinea(" Nombre: " + m_Variables[z].getNombre());
			m_Debug.imprimirLinea(" Tipo: " + m_Variables[z].getTipo());
		}
	}
}

