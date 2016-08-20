package jcomp.frontend;

import jcomp.util.Log;

/**
 *
 *
 * XML para las llamadas dentro de las llamadas, ejemplo:*
 * #met1( 43, #met2( a) , t)*
 * se convierte en:*
 * <llamada met1> 43 , <llamada met2> a </llamada> , t</llamada>*
 * y asi se forma el arbol.*
 *  *
 * YA SE QUE EL ARBOL SE DEBERIA DE HACER EN EL SINTACTICO PERO*
 * ES MAS FACIL HACERLO EN EL SEMANTICO PARA AL MISMO TIEMPO IR CHECANDO ERRORES
 *
 * */
public class Semantico
{
	String codigo;
	Log debug;
	Variables [] g_pVariables;
	Metodos [] metodos;

	public Semantico()
	{
		this.debug = Log.getInstance();
	}

	public void setCodigo(String c)
	{
		codigo = c;
	}

	public String getCodigo()
	{
		return codigo;
	}

	public int iniciar()
	{
		debug.imprimirLinea( "------------------------------");
		debug.imprimirLinea( "      ANALISIS SEMANTICO");
		debug.imprimirLinea( "------------------------------");

		// Convertimos los tokens que me pasa el sintactico en objetos. Metodos y Variables
		crearObjetos();

		// Revisar que no se repitan los nombres de los metodos ni variables
		if (revisarDefiniciones() != 0) return 1;

		// revisar las declaraciones del cuerpo que no sean iguales
		if (revisarCuerpoVariables() != 0) return 1;

		// bueno, TODAS las declaraciones, han sido comprobadas, y se han creado nuevos tokens

		//convertir las asignaciones en la nueva nomenclatura xml
		if (convertirAsignaciones() != 0) return 1;

		//revisar que las llamada a metodos existan...
		//osea que exista una definicion para la llamada a ese metodo
		if (revisarExistenciaMetodos() != 0) return 1;

		// convertir llamdas a la nueva nomenclatura
		if (convertirLLamadas() != 0) return 1;

		// verificar que existan las variables que se estan usando y que se declaren antes de ser usadas
		if (revisarExistenciaVariables() != 0) return 1;

		//convertir el token IDENTIFICADOR_variable1
		//en <var-variable1>
		if (convertirVariables() != 0)return 1;

		//revisar que las llamadas tengan el numero exacto de argumentos
		//que la definicion del metodo recibe
		if (revisarArgumentosDeLLamadas() != 0) return 1;

		if (revisarReturn() != 0) return 1;

		//arreglar todo pa que se vea bonito y simetrico
		if (arreglar() != 0) return 1;

		// continuar construllendo el arbol...

		// para ahorrarme un for dentro de cada metodo..
		for (int f=0; f<metodos.length; f++)
		{
			//convertir las operaciones en la nueva nomenclatura
			metodos[f].setCuerpo( convertirOP( metodos[f].getCuerpo()));

			//ahora eliminar los parentesis que ya de nada sirven
			metodos[f].setCuerpo( eliminarParentesis( metodos[f].getCuerpo()));

			if ( revisarRetorno(metodos[f].getCuerpo(), f) != 0) return 1;

			//okay, ahora de este arbol hacer un analisis dimensional
			if ( analisisDimensional(metodos[f].getCuerpo(), f) != 0) return 1;
		}

		//buscar un main
		if (buscarMain() != 0) return 1;

		imprimirObjetos();

		// Cambiar el nombre de las variables agregarles el nombre del metodo, para que si se
		// ddeclaran variables con el mismo nombre en distintos metodos, estas sean consideradas diferentes
		//nombresDeVariables();

		//	GENERAR EL CODIGO INTERMEDIO
		generarCodigo();

		//	DESPUES DE ESTO... SE TERMINA EL ANALISIS DE CODIGO
		//	este metodo, pasa los objetos en una cadena de texto, y la guarda
		//	en la variable codigo... la cual se puede obtener con un get de esta
		//	clase
		return 0;
	}//inicio

	void nombresDeVariables()
	{
		for (int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split("\n");
			String linea = metodos[a].getLinea();

			String cuerpo = "";

			for (int b=0; b<token.length; b++)
			{
				if ( token[b].indexOf( " id:") != -1)
				{
					String s1 = token[b].substring(0, token[b].indexOf(" id:")+4);
					String s2 = token[b].substring( token[b].indexOf(" id:")+4);
					token[b] = s1+"_"+metodos[a].getNombre().substring(14)+"_"+s2;
				}

				/*System.out.println("->"+token[b]+"<-");
				if ( token[b].indexOf( "args:NADA") == -1) {
					System.out.println("*");
					if (token[b].startsWith("<METODO"))
						System.out.println("-");
				}*/

				if ( (token[b].indexOf( "args:NADA") == -1)&&(token[b].startsWith("<METODO")))
				{
					//System.out.println(token[b]);
					String s1 = token[b].substring(0, token[b].indexOf(" args:")+6);
					String s2 = token[b].substring( token[b].indexOf(" regresa:"));

					String [] argus = token[b].substring(token[b].indexOf(" args:")+6, token[b].indexOf(" regresa:")).split(" ");
					for (int j = 1; j<argus.length; j = j+2)
					{
						argus[j] = "_"+metodos[a].getNombre().substring(14) + "_"+argus[j];
					}

					String argust = s1;
					for (int u=0; u<argus.length; u++)
					{
						argust += argus[u] + " ";
					}

					argust += s2;
					token[b] = argust;
					System.out.println(token[b]);
				}

				cuerpo += token[b] + "\n";;
			}

			metodos[a].setCuerpo(cuerpo);
		}//cada metodo
	}//metodo nombres de variables

	int revisarRetorno(String body, int f)
	{

		boolean regresa = true;
		if (metodos[f].getTipoDeRetorno().equals("TIPO_VOID"))
		{
			regresa=false;
		}

		if ( body.indexOf("<retorno") != -1)
		{
			int u = body.indexOf("<retorno");

			String uu = body.substring( body.indexOf("<retorno linea:")+15, body.indexOf("<retorno linea:")+19);
			uu = uu.substring( 0, uu.indexOf(">"));

			//si hay return y no deberia
			if (!regresa)
			{
				System.err.println("Imposible regresar algo de un metodo que es void.");
				System.err.println("Linea: "+uu);
				System.err.print("Metodo "+metodos[f].getNombre().substring(14));
				return 1;
			}
		}
		else
		{
			//si no hay return y deberia
			if (regresa)
			{
				System.err.println("Linea: " + metodos[f].getLinea().substring(13, metodos[f].getLinea().length()-1));
				System.err.print("Metodo "+metodos[f].getNombre().substring(14));
				System.err.println(" debe regresar alguna expresion.");
				return 1;
			}
		}
		return 0;
	}

	int analisisDimensional(String body, int metodo_)
	{
		String token[]= body.split("\n");

		boolean CAMBIO = true;
		while(CAMBIO)
		{
			CAMBIO=false;

			// primero simplificar lo basico

			body = "";
			for (int h=0; h<token.length; h++)
				if (!token[h].equals(""))
					body += token[h] + "\n";

			token = body.split("\n");

			for (int a=0; a<token.length; a++)
			{
				if (token[a].startsWith("<declaracion tipo:VOID "))
				{
					String linea = token[a].substring( token[a].indexOf("linea:")+6, token[a].length()-1);
					System.err.println("Linea: "+linea);
					System.err.println("Declaracion no valida.");
					return 1;
				}
			}

			body = "";for (int h=0; h<token.length; h++)if (!token[h].equals(""))body += token[h] + "\n";
			token = body.split("\n");

			boolean cambio = true;

			while(cambio)
			{
				cambio = false;
				for (int a=0; a<token.length; a++)
				{
					if (token[a].startsWith("<llamada") && token[a+1].equals("</llamada>"))
					{
						token[a] = "<"+token[a].substring( token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" id:"))+">";
						token[a+1] = "";
						cambio = true;
						CAMBIO = true;
					}
				}
			}

			body = "";
			for (int h=0; h<token.length; h++)
				if (!token[h].equals(""))
					body += token[h] + "\n";

			token = body.split("\n");

			cambio = true;

			while(cambio)
			{
				cambio = false;
				for (int a=0; a<token.length; a++)
				{
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


		body = "";
		for (int h=0; h<token.length; h++)
			if (!token[h].equals(""))
				body += token[h] + "\n";

		token = body.split("\n");

		cambio = true;

		while(cambio)
		{
			cambio = false;
			for (int a=0; a<token.length; a++)
			{
				if (token[a].startsWith("<llamada"))
				{
					int b=a+1;
					boolean not_good = false;

					while(!token[b].equals("</llamada>"))
					{
						if (token[b].startsWith("<op"))not_good = true;
						if (token[b].startsWith("<llamada"))not_good = true;
						b++;
					}

					if (!not_good)
					{


						String texto = "";
						for (int i=a; i<=b; i++)
							if (!token[i].equals(""))
								texto += token[i]+"%";

						/////////////////////////////////////////////////////////////////////////////
						// revisar esta llamada a metodo para ver si los tipos coniciden
						// aqui reviso algo importante, es mas facil llamar al metodo desde aqui
						if ( revisarTipoArgumentos( texto) != 0)
							return 1;

						token[a] = "<"+token[a].substring( token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" id:"))+">";

						for (int i=a+1; i<=b; i++)
						{
							token[i] = "";
						}
						cambio=true;
						CAMBIO = true;
					}
				}
			}
		}

		body = "";
		for (int h=0; h<token.length; h++)
			if (!token[h].equals(""))
				body += token[h] + "\n";

		token = body.split("\n");

		cambio = true;
		while(cambio)
		{
			cambio = false;
			for (int a=0; a<token.length; a++)
			{
				if (token[a].startsWith("<op "))
				{
				int b=a+1;
				boolean not_good = false;

				while(!token[b].equals("</op>"))
				{
				if (token[b].startsWith("<op"))not_good = true;
				if (token[b].startsWith("<llamada"))not_good = true;
				b++;
				}

				if (!not_good)
				{
					String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
					String operacion = token[a].substring(token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" "));

					if ((token[a+1].equals( "<STRING>") || token[a+2].equals( "<STRING>")))
					{
						System.err.print("Operador "+operacion+" ");
						System.err.println("no puede ser aplicado a : "+token[a+1]+" y "+token[a+2]);
						System.err.println("Linea: "+linea);
						return 1;
					}

					token[a]=token[a+1];
					for (int i=a+1; i<=b; i++) token[i] = "";

					cambio = true;
					CAMBIO = true;
				}

				}
			}
		}

		body = "";for (int h=0; h<token.length; h++)if (!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		cambio = true;
		while(cambio)
		{
			cambio = false;
			for (int a=0; a<token.length; a++)
			{
				if (token[a].startsWith("<asignacion "))
				{
					int b=a+1;
					boolean not_good = false;

					while(!token[b].equals("</asignacion>"))
					{
						if (token[b].startsWith("<op"))not_good = true;
						if (token[b].startsWith("<llamada"))not_good = true;
						b++;
					}

					if (!not_good)
					{
						String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
						String recibe = token[a].substring(token[a].indexOf("tipo:")+5, token[a].indexOf(" id:"));
						String _id = token[a].substring(token[a].indexOf("id:")+3, token[a].lastIndexOf(" "));
						recibe = "<" + recibe + ">";

						if (!recibe.equals( token[a+1]))
						{
							System.err.println("Linea: "+linea);
							System.err.println("Tipos incompatibles.");
							System.err.println("Encontrado:"+token[a+1]);
							System.err.println("Requerido:"+recibe);
							return 1;
						}

						token[a]=recibe;
						for (int i=a+1; i<=b; i++) token[i] = "";

						cambio=true;
						CAMBIO = true;
					}

				}
			}
		}

		body = "";
		for (int h=0; h<token.length; h++)
			if (!token[h].equals(""))
				body += token[h] + "\n";

		token = body.split("\n");

		cambio = true;
		while(cambio)
		{
			cambio = false;
			for (int a=0; a<token.length; a++)
			{
				if (token[a].startsWith("<retorno linea:") && token[a+2].equals("</retorno>"))
				{
					String tipejo = "TIPO_"+token[a+1].substring(1,token[a+1].length()-1);
					String ret = metodos[metodo_].getTipoDeRetorno();
					if (!tipejo.equals(ret))
					{
						System.err.print("Linea : ");
						System.err.println(token[a].substring(token[a].indexOf(" linea:")+7, token[a].length()-1));
						System.err.print("Metodo "+metodos[metodo_].getNombre().substring(14));
						System.err.print(" debe regresar " + ret);
						System.err.println(" pero " +tipejo+" encontrado. ");
						return 1;
					}
				}
			}
		}
	}//while super grandote

	body = "";
	for (int h=0; h<token.length; h++)
		if (!token[h].equals(""))
			body += token[h] + "\n";
	return 0;
	}

	int revisarTipoArgumentos(String s)
	{
		String tokens[] = s.split("%");
		//-</llamada tipo:INT id:numeros4 linea:12>
		String id = tokens[0].substring( tokens[0].indexOf(" id:")+4, tokens[0].indexOf(" linea:"));
		String linea = tokens[0].substring( tokens[0].indexOf(" linea:")+7, tokens[0].length()-1);

		int a = tokens[0].length();
		s = s.substring( a +1 , s.length()-12);

		String argus_met = "";
		for (int b=0; b<metodos.length; b++)
			if (metodos[b].getNombre().substring(14).equals(id))
				argus_met = metodos[b].getArgumentos();

		String partes [] = s.split("%");
		s = "";
		for (int b=0; b<partes.length; b++)
			if (!partes[b].equals("<coma>")) s += partes[b]+" ";

		partes = argus_met.split( " ");
		argus_met = "";
		for (int b=0; b<partes.length; b++)
		{
			argus_met += partes[b].substring(0, partes[b].indexOf("-"))+"> ";
		}


		if ( !argus_met.equals(s))
		{
			System.err.println("Linea : " + linea);
			System.err.println("Error en argumentacion de metodo.");
			System.err.println("Requerido : #"+id+" ( "+argus_met+ ")");
			System.err.println("Encontrado: #"+id+" ( "+s+ ")");
			return 1;
		}
	return 0;
	}

	int buscarMain()
	{
		boolean found = false;
		for (int a=0; a<metodos.length; a++)
		{
			if ( metodos[a].getNombre().substring(14).equals("main")
				&& metodos[a].getArgumentos().equals("NADA")
				&& metodos[a].getTipoDeRetorno().equals("TIPO_VOID"))
			{
				found = true;
			}
		}

		if (!found)
		{
			System.err.println("WARNING: Metodo void #main() no existe.");
		}
	return 0;
	}

	void generarCodigo()
	{
		codigo = "";

		// Bsucar declaracion de variables globales
		for (int a=0; a<g_pVariables.length; a++)
		{
			codigo += "<declaracion global tipo:"+g_pVariables[a].getTipo().substring(5)
				+ " id:" +g_pVariables[a].getNombre().substring(14)+">\n";
		}

		// Buscar declaracion de metodos
		for (int a=0; a<metodos.length; a++)
		{
			codigo += "<METODO id:"+metodos[a].getNombre().substring(14)+" ";
			codigo += "args:";

			if (!metodos[a].getArgumentos().equals("NADA"))
			{
				String [] args = metodos[a].getArgumentos().split(" ");
				for (int c=0; c<args.length; c++)
				{
					args[c] = args[c].substring(1, args[c].length()-1);
					String f [] = args[c].split("-");
					codigo += f[0]+" "+f[1] + ", ";
				}
				codigo = codigo.substring(0, codigo.length()-2) + " ";
			}
			else
			{
				codigo += "NADA ";
			}

			codigo += "regresa:" + metodos[a].getTipoDeRetorno().substring(5) ;
			codigo += ">\n";

			// Iterar por el cuerpo del metodo
			String tokens [] = metodos[a].getCuerpo().split("\n");
			String newBody = "";
			for (int t=0; t<tokens.length; t++)
			{
				// Remover todo antes de "linea:xx"
				if (tokens[t].indexOf(" linea:") != -1)
				{
					tokens[t] = tokens[t].substring(0, tokens[t].indexOf(" linea:"))+">";
				}

				codigo += tokens[t]+"\n";
			}

			codigo += "</METODO>\n";
		}//for de cada metodo
	}//metodo

	String eliminarParentesis(String body)
	{
		String [] token = body.split("\n");

		for (int a=0; a<token.length; a++)
		{
			if (token[a].equals("<parentesis>") || token[a].equals("</parentesis>"))
			{
				token[a]="";
			}
		}

		body="";
		for (int a=0; a<token.length; a++)
		{
			if (!token[a].equals("")) body += token[a] + "\n";
		}

		return body;
	}

	//	<INT valor:5 linea:19 scope:local>
	//	<operacion tipo:MUL linea:19>
	//	<INT id:a linea:19 scope:local>
	//
	//	convertir eso en ....
	//
	//	<op tipo:MUL linea:19>
	//		<INT valor:5 linea:19>
	//		<INT id:a linea:19>
	//	</op>
	String convertirOP( String body)
	{
		boolean cambio = true;

		while(cambio)
		{
			cambio = !cambio;
			String [] token = body.split("\n");

			// primero ver donde esta la operacion mas profunda...
			int tabs = 0;
			int mayor = 0;
			int mayor_token=0;

			int lugar_operacion=0;//donde esta la operacion
			int lugar_inicio_a = 0;
			int lugar_fin_a = 0;
			int lugar_inicio_b = 0;
			int lugar_fin_b = 0;

			for (int a=0; a<token.length; a++)
			{
				if ( token[a].startsWith("</")) tabs--;

				if ( token[a].startsWith("<operacion"))
				{
					if (tabs>mayor) { mayor=tabs; mayor_token=a; }
				}
				if ( token[a].startsWith("<llamada")) tabs++;
				if ( token[a].startsWith("<llave>")) tabs++;
				if ( token[a].startsWith("<asignacion")) tabs++;
				if ( token[a].startsWith("<parentesis>")) tabs++;
				if ( token[a].startsWith("<if")) tabs++;
				if ( token[a].startsWith("<while")) tabs++;
				if ( token[a].startsWith("<op")) tabs++; //aguas akiii
			}

			if (mayor==0)return body;

			cambio = true;
			lugar_operacion = mayor_token;

			String argumento_a = token[mayor_token-1];
			String argumento_b = token[mayor_token+1];

			//ahora guardar los argumentos que recibe la operacion
			//si el que esta antes es una llamada o parentesis
			int _a = mayor_token-1;
			if (argumento_a.startsWith("</llamada>") || argumento_a.startsWith("</parentesis>") || argumento_a.startsWith("</op>"))
			{
				String buscando="";
				if (argumento_a.equals("</parentesis>")) buscando = "<parentesis>";
				if (argumento_a.startsWith("</llamada")) buscando = "<llamada";
				if (argumento_a.startsWith("</op>")) buscando = "<op ";

				int closure_m = 0;
				int closure_p = 0;
				int closure_o = 0;
				_a--;

				while( true)
				{
					if (token[_a].startsWith(buscando) && closure_m == 0 && closure_p==0 && closure_o==0) break;

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
				String buscando="";
				if (argumento_b.equals("<parentesis>")) buscando = "</parentesis>";
				if (argumento_b.startsWith("<llamada")) buscando = "</llamada";
				if (argumento_b.startsWith("<op ")) buscando = "</op>";

				int aperture_m = 0;
				int aperture_p = 0;
				int aperture_o = 0;
				_a++;
				while( true)
				{
					if (token[_a].startsWith(buscando) && aperture_m == 0 && aperture_p==0 && aperture_o==0) break;

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

			for (int b=lugar_inicio_a; b<=lugar_fin_b; b++)
				token[b] = "";

			token[lugar_inicio_a] = "<op "+ope.substring(11)+"\n" + argumento_a + argumento_b + "</op>";

			body = "";
			for (int b=0; b<token.length; b++)
				if (!token[b].equals(""))body += token[b]+ "\n";

		}//while de si hubo cambios
		return body;
	}//convertir OPeraciones a la nueva nomenclatura

	int arreglar()
	{
		for (int a=0; a<metodos.length; a++)
		{
			// Primero voy a hacer un vector con las varibles, para poder ponerle de que tipo es
			String [] token = metodos[a].getCuerpo().split("<declaracion-");
			String [] args = metodos[a].getArgumentos().split(" ");

			// Total de variables locales es numero de <declaraciones> + el numero de Argumentos + variables globales.
			int total = token.length + args.length - 1 + g_pVariables.length;
			if (metodos[a].getArgumentos().equals("NADA"))
			{
				total--;
			}

			// Crear un arreglo para guardar variables locales
			String [][] variables = new String[total][3];

			// Para ir llenando variables
			int declaracion_actual = 0;

			// Es esto necesario ?
			for (int u=0; u<variables.length; u++)
			{
				variables[u][0] = "";
				variables[u][1] = "";
				variables[u][2] = "";
			}

			// Creo que estas son las variables globales
			for (int z=0; z<g_pVariables.length; z++)
			{
				variables[declaracion_actual][0] = g_pVariables[z].getNombre().substring(14);
				variables[declaracion_actual][1] = g_pVariables[z].getTipo().substring(5);
				variables[declaracion_actual][2] = "global";
				declaracion_actual++;
			}

			// Argumentos del metodo
			args = metodos[a].getArgumentos().split(" ");
			if ( !metodos[a].getArgumentos().equals("NADA"))
			{
				for (int z=0; z<args.length; z++)
				{
					String [] _b = args[z].split("-");
					variables[declaracion_actual][0] = _b[1].substring(0, _b[1].length()-1);
					variables[declaracion_actual][1] = _b[0].substring(1);
					variables[declaracion_actual][2] = "arg";
					declaracion_actual++;
				}
			}

			// Variables locales
			token = metodos[a].getCuerpo().split(" ");
			for (int b=0; b<token.length; b++)
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

			String tokens[] = metodos[a].getCuerpo().split(" ");

			String cuerpo = "";
			for (int g=0; g<tokens.length; g++)
			{
				cuerpo += tokens[g]+"\n";
			}

			cuerpo = reorganizarDeclaraciones(cuerpo, variables);

			cuerpo = reorganizarLLamadas(cuerpo);

			// <var-8-a> TO <int8 id:var1 linea:5 scope:local>
			cuerpo = reorganizarVars(cuerpo, variables);

			cuerpo = reorganizarNumeros(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarStrings(cuerpo, metodos[a].getLinea());

			//old <asignacion-12-cadena>
			//new <asignacion tipo:string id:cadena linea:12> </asignacion>
			cuerpo = reorganizarAsignacion(cuerpo, variables);

			cuerpo = reorganizarParYLLaves(cuerpo);

			cuerpo = reorganizarOperaciones(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarLineas(cuerpo);

			metodos[a].setLinea( "<linea linea:"+metodos[a].getLinea().substring(13)+">");

			cuerpo = reorganizarif (cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarWhile(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarOpBoleanas(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarComa(cuerpo);

			cuerpo = reorganizarReturn(cuerpo);

			//ya de nada sirve el punto y coma y la linea
			cuerpo = quitarPuntoComaYLinea(cuerpo);

			//awebooooo ahora si se ve bonito el arbol
			metodos[a].setCuerpo(cuerpo);
		}//for metodos

	return 0;
	}//metodod

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

		for (int a=0; a<token.length; a++)
		{
			nuevo = "";
			if (token[a].startsWith("<linea"))linea = token[a];

			if (token[a].startsWith("BOL_"))
			{
			String g [] = token[a].split("_");
			nuevo += "<operacion tipo:" + g[1] + " linea:" + linea.substring(13, linea.length()-1)+">";
			token[a] = nuevo;
			}

			if (token[a].startsWith("ASIGNA") && token[a+1].startsWith("ASIGNA"))
			{
			token[a] = "<operacion tipo:IGUAL linea:"+ linea.substring(13, linea.length()-1)+">";
			token[a+1] = "";
			}

			if (!token[a].equals("")) body += token[a]+ "\n";
		}
		return body;
	}

	String reorganizarWhile(String body, String linea)
	{
		String [] token = body.split("\n");
		body = "";
		for (int  a = 0; a<token.length; a++)
		{

			if (token[a].startsWith("<linea")) linea = token[a];

			if (token[a].equals("CONTROL_WHILE"))
			{
				linea = linea.substring( 13, linea.length() -1);
				token[a] = "<~while linea:"+linea+">";
				token[a+1] = "";
			}
		}

		boolean cambio=true;
		while(cambio)
		{
			int r = token.length-1;

			for (;;r--)
			{
				if ( r<0) { cambio=false; break; }
				if ( token[r].startsWith("<~while linea:")) { cambio=true; break; }
			}

			if (!cambio) break;

			token[r] = "<while "+token[r].substring(8);

			int parentesis=0;
			while( true)
			{
				if (token[r].equals("</parentesis>") && parentesis ==0) break;
				if (token[r].equals("<parentesis>")) parentesis++;
				if (token[r].equals("</parentesis>")) parentesis--;
				r++;
			}
			token[r] = "</while>";
		}

		body = "";
		for (int b=0; b<token.length; b++)
			if (!token[b].equals("")) body += token[b]+"\n";

		return body;
	}//metodo

	String reorganizarif (String body, String linea)
	{
		String [] token = body.split("\n");
		body = "";
		for (int  a = 0; a<token.length; a++)
		{

			if (token[a].startsWith("<linea")) linea = token[a];

			if (token[a].equals("CONTROL_IF"))
			{
				linea = linea.substring( 13, linea.length() -1);
				token[a] = "<~if linea:"+linea+">";
				token[a+1] = "";
			}
		}

		boolean cambio=true;
		while(cambio)
		{
			int r = token.length-1;

			for (;;r--)
			{
				if ( r<0) { cambio=false; break; }
				if ( token[r].startsWith("<~if linea:")) { cambio=true; break; }
			}

			if (!cambio) break;

			token[r] = "<if "+token[r].substring(5);

			int parentesis=0;
			while( true)
			{
				if (token[r].equals("</parentesis>") && parentesis ==0) break;
				if (token[r].equals("<parentesis>")) parentesis++;
				if (token[r].equals("</parentesis>")) parentesis--;
				r++;
			}
			token[r] = "</if>";
		}

		body = "";
		for (int b=0; b<token.length; b++)
			if (!token[b].equals("")) body += token[b]+"\n";

	return body;
	}//metodo

	String reorganizarLineas(String body)
	{
		String token [] = body.split("\n");
		body = "";
		for (int a=0; a<token.length; a++)
		{
			if (token[a].startsWith("NUMERO_LINEA_"))
				token[a] = "<linea linea:" + token[a].substring(13) + ">";

			body += token[a]+ "\n";
		}

		return body;
	}


	String reorganizarOperaciones(String body, String linea)
	{
		String [] token = body.split("\n");
		body = "";
		String nuevo;
		for (int a=0; a<token.length; a++)
		{
			nuevo = "";
			if (token[a].startsWith("NUMERO_LINEA_"))linea = token[a];

			if (token[a].startsWith("OP_"))
			{
			String g [] = token[a].split("_");
			nuevo += "<operacion tipo:";
			if (g[1].equals("MULTIPLICACION"))g[1] = "MUL";
			if (g[1].equals("DIVISION"))g[1] = "DIV";
			nuevo += g[1] + " linea:" + linea.substring(13)+">";
			token[a] = nuevo;
			}

		body += token[a]+ "\n";
		}
	return body;
	}//metodo

	String reorganizarParYLLaves(String body)
	{
		//	<parentesis> </parentesis>
		//	<llave> </llave>
		String [] token = body.split("\n");
		body = "";
		for (int a=0; a<token.length; a++)
		{
			if (token[a].equals("PARENTESIS_ABRE"))token[a]="<parentesis>";
			if (token[a].equals("PARENTESIS_CIERRA"))token[a]="</parentesis>";
			if (token[a].equals("LLAVE_ABRE"))token[a]="<llave>";
			if (token[a].equals("LLAVE_CIERRA"))token[a]="</llave>";

			body += token[a]+ "\n";
		}
	return body;
	}//metodo

	//old <asignacion-12-cadena>
	//new <asignacion tipo:string id:cadena linea:12> </asignacion>
	String reorganizarAsignacion(String body, String [][] variables)
	{
		String [] token = body.split("\n");
		body = "";
		String nuevo;
		for (int a=0; a<token.length; a++)
		{
			nuevo = "";
			if (token[a].startsWith("<asignacion-"))
			{
				token[a] = token[a].substring( 1, token[a].length()-1);
				String g [] = token[a].split("-");
				nuevo += "<asignacion tipo:";

				int foundIndex = -1;
				for (int b=0; b<variables.length; b++)
				{
					if (variables[b][0].equals(g[2]))
					{
						foundIndex = b;
						break;
					}
				}

				nuevo += variables[foundIndex][1];
				nuevo += " id:" + g[2];
				nuevo += " scope:" + variables[foundIndex][2];
				nuevo += " linea:" + g[1];
				nuevo += ">";
				token[a] = nuevo;
			}
			body += token[a]+ "\n";
		}
		return body;
	}

	String reorganizarStrings(String body, String linea)
	{
		//STRING_"dfasd"
		String [] token = body.split("\n");
		body = "";

		for (int a=0; a<token.length; a++)
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

		for (int a=0; a<token.length; a++)
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

	// <var-8-a>
	// <int8 id:var1 linea:5 scope:local>
	String reorganizarVars(String body, String variables[][])
	{
		String [] token = body.split("\n");
		body = "";
		for (int a=0; a<token.length; a++)
		{
			String nuevo = "";
			if (token[a].startsWith("<var"))
			{
				String f = token[a].substring(1, token[a].length()-1);
				String [] variableParts = f.split("-");
				nuevo += "<";

				int foundIndex = -1;
				for (int z=0; z<variables.length; z++)
				{
					if (variables[z][0].equals( variableParts[2]))
					{
						foundIndex = z;
						break;
					}
				}

				// Agregar el tipo
				nuevo += variables[foundIndex][1];

				// Agregar el id de la variable....
				nuevo += " id:" + variableParts[2];

				// Agregar el scope de la variable
				nuevo += " scope:"+variables[foundIndex][2];

				// Agregar la linea de la variable
				nuevo += " linea:" + variableParts[1] ;

				nuevo += ">";

				token[a] = nuevo;
			}
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
		for (int a=0; a<token.length; a++)
		{
			String nuevo = "";
			if (token[a].startsWith("<llamada"))
			{
				String f = token[a].substring(1, token[a].length()-1);
				String [] ff = f.split("-");
				nuevo += "<llamada tipo:";
				for (int _fff=0; _fff<metodos.length; _fff++)
				{
					if (ff[2].equals(metodos[_fff].getNombre().substring(14)))
					{
						nuevo += metodos[_fff].getTipoDeRetorno().substring(5);
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
		for (int a=0; a<token.length; a++)
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
	}//metodo reorganizarDeclaraciones


	//convertir el token IDENTIFICADOR_variable1
	//en <var-13-_variable1>
	int convertirVariables()
	{
		for (int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();
			String cuerpo = "";

			for (int b=0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA"))
				{
					linea = token[b];
				}

				if (token[b].startsWith("IDENTIFICADOR_"))
				{
					token[b] = "<var-"+linea.substring(13)+"-"+token[b].substring(14)+">";
				}
				cuerpo += token[b] + " ";
			}

			metodos[a].setCuerpo(cuerpo);
		}//cada metodo

		return 0;
	}//convertirVariables



	int revisarReturn()
	{
		for (int a = 0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();
			for (int b=0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA_")) linea = token[b];

				if (token[b].equals("CONTROL_RETORNO"))
				{
					token[b] = "<retorno-linea:"+linea.substring(13)+">";
					while(!token[b].equals("PUNTUACION_PUNTO_COMA")) b++;
					token[b] = "</retorno>";
				}
			}//for de cada token

			String body = "";
			for (int b=0; b<token.length; b++)
			{
				body += token[b] + " ";
			}

			metodos[a].setCuerpo( body);
		}//for de cada metodo
		return 0;
	}

	int revisarArgumentosDeLLamadas()
	{
		Metodos [] metodos_test = metodos;

		for (int a=0; a<metodos_test.length; a++)
		{
			String token[] = metodos_test[a].getCuerpo().split(" ");

			// Filtrar los numeros de linea de este metodo
			String __g = "";
			for (int g=0; g<token.length; g++)
				if (!token[g].startsWith("NUMERO_LINEA"))
					__g += token[g]+" ";

			token = __g.split(" ");

			int argu=0;
			boolean cambio=true;

			while (cambio)
			{
				cambio = false;

				int r = token.length-1;

				// Continuar procesando mientras existan mas llamadas a metodos
				for (;;r--)
				{
					if (r<0) break;
					if (token[r].startsWith("<llamada-"))
					{
						cambio = true;
						break;
					}
				}

				if (!cambio) break;

				// `r` es el indice donde esta la llamada
				String llamada_a = token[r].substring( token[r].lastIndexOf("-")+1, token[r].length()-1);
				String linea = token[r].substring(9, token[r].lastIndexOf("-"));

				token[r] = "<~llamada-"+token[r].substring(9);

				r++;
				argu = 0;
				while (true)
				{
					if (token[r].equals("</llamada>")) break;

					if (token[r-1].startsWith("<~llamada")) argu++;

					if (token[r].equals("PUNTUACION_COMA") && argu > 0) argu++;

					token[r]="";
					r++;
				}

				token[r] = "</llamada>";

				boolean encontrado = false;
				int deberian = 0;
				for (int h=0; h<metodos.length; h++)
				{
					if (metodos[h].getNombre().substring(14).equals(llamada_a))
					{
						String [] t = metodos[h].getArgumentos().split(" ");
						deberian = t.length;
						if ( metodos[h].getArgumentos().equals("NADA")) deberian=0;
						if ( deberian == argu) encontrado=true;
					}
				}

				if (!encontrado)
				{
					System.err.println("Linea: "+linea);
					System.err.println("Metodo "+llamada_a+" deberia recibir "+deberian+" argumentos.");
					return 1;
				}

			}//por cada cambio hecho
		}//for de cada objeto
		return 0;
	}//revisarArgumentosDeLLamadas()

	int convertirAsignaciones()
	{
		for (int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();
			for (int b=0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA"))
				{
					linea = token[b];
				}

				if (token[b].startsWith("IDENTIFICADOR_") && token[b+1].equals("ASIGNA") && !token[b+2].equals("ASIGNA"))
				{
					token[b] = "<asignacion-"+linea.substring(13)+"-"+token[b].substring(14)+">";
					token[++b] = "";

					while(!token[b].equals("PUNTUACION_PUNTO_COMA"))
					{
						b++;
					}
					token[b] = "</asignacion>";
				}
			}

			String nuevo_cuerpo = "";
			for (int b=0; b<token.length; b++)
			{
				if (!token[b].equals(""))
				{
					nuevo_cuerpo += token[b] + " ";
				}
			}
			metodos[a].setCuerpo(nuevo_cuerpo);
		}
		return 0;
	}


	int convertirLLamadas()
	{
		for (int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();

			for (int b=0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if (token[b].equals("PUNTUACION_GATO"))
				{

				token[b] = "<~llamada-"+linea.substring(13)+"-"+token[b+1].substring(14)+">";

				while(!token[b].startsWith("IDENTIFICADOR")) b++;
					token[b] = "";

				while(!token[b].equals("PARENTESIS_ABRE")) b++;
					token[b] = "";
				}
			}



			String cuerpo = "";
			for (int b=0; b<token.length; b++)
				if (!token[b].equals("")) cuerpo += token[b]+" ";

			metodos[a].setCuerpo( cuerpo);


			token = metodos[a].getCuerpo().split(" ");

			boolean cambio=true;
			while(cambio)
			{
				cambio=false;

				int r = token.length-1;

				for (;;r--)
				{
					if ( r<0) break;
					if ( token[r].startsWith("<~llamada-")) { cambio=true; break; }
				}

				if (!cambio) break;

				token[r] = "<llamada-"+token[r].substring(10);

				int parentesis=0;
				while( true)
				{
					if (token[r].equals("PARENTESIS_CIERRA") && parentesis ==0) break;
					if (token[r].equals("PARENTESIS_ABRE")) parentesis++;
					if (token[r].equals("PARENTESIS_CIERRA")) parentesis--;
					r++;
				}

				token[r] = "</llamada>";
			}

			cuerpo = "";
			for (int b=0; b<token.length; b++)
				if (!token[b].equals("")) cuerpo += token[b]+" ";

			metodos[a].setCuerpo( cuerpo);
		}//for de cada metodo

		return 0;
	}

	int crearObjetos()
	{
		String [] token = codigo.split("\n");

		int total_definiciones = 0;
		for (int d = 0; d<token.length; d++)
		{
			if (token[d].equals("CONTROL_DEF"))
			{
				total_definiciones ++;
			}
		}

		debug.log("Total definiciones " + total_definiciones + "\n");
		String [] definiciones = new String[total_definiciones];

		int definicion = -1;
		String linea = "";

		for (int d = 0; d < token.length; d++)
		{
			if (token[d].equals("CONTROL_DEF"))
			{
				definicion++;
				definiciones[definicion] = linea + " ";
			}

			if (d < token.length-1)
			{
				if ((definicion > -1) && (!token[d+1].equals("CONTROL_DEF")))
				{
					definiciones[definicion] += (token[d]+" ");
				}
			}

			if (token[d].startsWith("NUMERO_LINEA"))
			{
				linea = token[d];
			}
		}

		//listo aqui el vector definiciones sale con todas las definiciones incluyendo el numero de linea
		//en fin las definiciones de instrucciones basicas se kedan en
		//un  vector de strings que se llama definiciones[]
		//ahora contar si son metodos o variables
		int num_de_metodos=0, num_de_variables=0;

		for (int x=0; x< total_definiciones; x++)
		{
			if (definiciones[x].indexOf("PUNTUACION_GATO") != -1)
			{
				num_de_metodos++;
			}
			else
			{
				num_de_variables++;
			}
		}

		debug.log("Total metodos " + num_de_metodos + "\n");
		//pero antes para mayor comodidad, mejor hacer dos vectores,
		//def_met[] y def_var[]
		String [] def_met = new String[num_de_metodos];
		String [] def_var = new String[num_de_variables];

		int v=0, m=0;

		for (int b=0; b<total_definiciones; b++)
		{
			if (definiciones[b].indexOf("PUNTUACION_GATO") == -1)
				def_var[v++]=definiciones[b];
			else
				def_met[m++]=definiciones[b];
		}

		//ahora si crear bien los objetos de la clase Metodos
		metodos = new Metodos[num_de_metodos];
		for ( int a = 0; a<metodos.length; a++)
			metodos[a] = new Metodos();

		int met = 0;
		for (int a=0; a<num_de_metodos; a++)
		{
			String [] _a = def_met[a].split(" ");

			metodos[met].setLinea( _a[0]);

			String [] _b = new String[_a.length];
			int _d = 0;

			for (int _c = 0; _c < _a.length; _c++)
				if ( ! _a[_c].startsWith("NUMERO_LINEA")) _b[_d++] = _a[_c];

			String _e = "";
			for (int _c = 0; _c < _d; _c++) _e += _b[_c]+" ";

			String _f = "";
			for (int _c = 0; _c < _a.length; _c++) _f += _a[_c]+" ";

			metodos[met].setTipoDeRetorno( _b[1]);
			metodos[met].setNombre( _b[3]);

			metodos[met].setArgumentos( _e.substring( _e.indexOf("PARENTESIS_ABRE") + 15 , _e.indexOf("PARENTESIS_CIERRA")));
			metodos[met].setCuerpo( _f.substring( _f.indexOf("LLAVE_ABRE")));

			met++;
		}//for de cada metodo

		//agregar el metodo de impresion

		//ahora crear los objetos de la clase Variables
		g_pVariables = new Variables[num_de_variables];
		for ( int a = 0; a<g_pVariables.length; a++) g_pVariables[a] = new Variables();

		int nvar = 0;
		for (int a=0; a<num_de_variables; a++)
		{
			String [] _a = def_var[a].split(" ");

			g_pVariables[nvar].setLinea( _a[0]);

			String [] _b = new String[_a.length];
			int _d = 0;
			for (int _c = 0; _c < _a.length; _c++)
				if ( ! _a[_c].startsWith("NUMERO_LINEA")) _b[_d++] = _a[_c];

			g_pVariables[nvar].setTipo( _b[1]);
			g_pVariables[nvar].setNombre( _b[2]);
			nvar++;
		}//for de cada variable

		//listo g_pVariables[] y metodos[] ya estan llenos ... 1:14pm
		return 0;
	}//fin metodo crearObjetos()

	int revisarDefiniciones()
	{
		// Revisar metodos con el mismo nombre
		for (int a = 0; a < metodos.length; a++)
		{
			String comp = metodos[a].getNombre();
			for (int b=0; b<metodos.length; b++)
			{
				if (a==b) b++;

				if (b==metodos.length) break;
				
				if (comp.equals( metodos[b].getNombre()))
				{
					System.err.println("Linea: "+metodos[b].getLinea().substring(13));
					System.err.println("Metodo "+comp.substring(14)+" ya esta definido en este programa.");
					debug.imprimirLinea("\nMetodo ya definido !!!");
					return 1;
				}
			}
		}

		//Revisar variables con el mismo nombre
		for (int a = 0; a<g_pVariables.length; a++)
		{
		String comp = g_pVariables[a].getNombre();
			for (int b=0; b<g_pVariables.length; b++)
			{
				if (a==b) b++;
				if (b==g_pVariables.length) break;
				if ( comp.equals( g_pVariables[b].getNombre()))
				{
					System.err.println("Linea: "+g_pVariables[b].getLinea().substring(13));
					System.err.println("Variable "+comp.substring(14)+" ya esta definido en este programa.");
					debug.imprimirLinea("\nVariable ya definida !!!");
					return 1;
				}
			}
		}

		return 0;
	}//revisarDefiniciones()

	//revisar que dentro del cuerpo de los metodos no se declaren las variables dos veces
	//acuerdate que los argumentos que recibe el metodo son tambien declaraciones
	//y que no pueden tener el mismo nombre que declaraciones globales def
	int revisarCuerpoVariables()
	{
		String cuerpo="";
		String linea="";
		String declaraciones [][];
		int declaracion;

		for (int d=0; d<metodos.length; d++)
		{
			cuerpo = metodos[d].getCuerpo();
			String [] tokens = cuerpo.split(" ");
			declaracion=0;

			//contar cuantas declaraciones en los argumentos que recibe
			String s = metodos[d].getArgumentos();
			String ss [] = s.split(" ");
			for (int h=0; h<ss.length; h++)
			{
				if ( ss[h].startsWith("TIPO_") && ss[h+1].startsWith("IDENTIFICADOR_"))
				{
					declaracion++;
				}
			}

			int argumentos = declaracion;

			//contar cuantas declaraciones hay en el cuerpo--
			for (int a=0; a<tokens.length; a++)
				if ( tokens[a].startsWith("TIPO_") && tokens[a+1].startsWith("IDENTIFICADOR_") && tokens[a+2].equals("PUNTUACION_PUNTO_COMA"))
					declaracion++;

			//crear un vector bidimensional.. declaracion-> linea, tipo, id
			declaraciones = new String[declaracion][3];

			int inicio=0;

			for (int h=0; h<ss.length; h++)
			{
				if ( ss[h].startsWith("TIPO_") && ss[h+1].startsWith("IDENTIFICADOR_"))
				{
					declaraciones[inicio][0] = metodos[d].getLinea();
					declaraciones[inicio][1] = ss[h];
					declaraciones[inicio][2] = ss[h+1];
					inicio++;
				}
			}

			linea = metodos[d].getLinea();
			for (int a=0; a<tokens.length; a++)
			{
				if ( tokens[a].startsWith("NUMERO_LINEA"))
				{
					linea = tokens[a];
				}

				if ( tokens[a].startsWith("TIPO_") && tokens[a+1].startsWith("IDENTIFICADOR_") && tokens[a+2].equals("PUNTUACION_PUNTO_COMA"))
				{
					declaraciones[inicio][0] = linea;
					declaraciones[inicio][1] = tokens[a];
					declaraciones[inicio][2] = tokens[a+1];
					inicio++;
				}
			}//cada token

			//por fin !! ya tengo la declaraciones de este metodo... ahora ver que no sean iguales
			//o que no sean iguales a alguna definicion global
			//las declaraciones globales estan en el el arreglo g_pVariables[].getNombre()
			for (int p=0; p<declaraciones.length; p++)
			{
				String id = declaraciones[p][2];
				for (int q=0; q<declaraciones.length; q++)
				{
					if ( p == q) q++;
					if ( q == declaraciones.length) break;
					if ( id.equals( declaraciones[q][2]))
					{
						System.err.println("Linea: "+declaraciones[q][0].substring(13));
						System.err.println("Variable "+ declaraciones[p][2].substring(14)+" ya esta definida localmente.");
						return 1;
					}
				}

				for (int f=0; f<g_pVariables.length; f++)
				{
					if ( id.equals( g_pVariables[f].getNombre()))
					{
						System.err.println("Linea: "+declaraciones[p][0].substring(13));
						System.err.println("Variable "+ declaraciones[p][2].substring(14)+" ya esta definida globalmente, en la linea "+g_pVariables[f].getLinea().substring(13)+".");
						return 1;
					}
				}
			}// for de comprobaciones

			// en vez de tener 3 tokens, hacer unos solo
			// que contenga <declaracion-linea-tipo-id>
			// declaraciones[p][0,1,2]

			//pido perdon a los dioses por tanto if (ya vez omar, son necesarios jaja)
			linea = metodos[d].getLinea();
			for (int x=0; x<tokens.length; x++)
			{
				if (tokens[x].startsWith("NUMERO_LINEA_"))
					linea = tokens[x].substring(13);

				if (tokens[x].startsWith("TIPO_"))
				{
					if (tokens[x+1].startsWith("IDENTIFICADOR_") && tokens[x+2].equals("PUNTUACION_PUNTO_COMA"))
					{
						tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+1].substring(14)+">";
						tokens[x+1] = "";
						tokens[x+2] = "";
					}

					if (tokens[x+1].startsWith("IDENTIFICADOR_") && tokens[x+2].startsWith("NUMERO_LINEA_") && tokens[x+3].equals("PUNTUACION_PUNTO_COMA"))
					{
						tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+1].substring(14)+">";
						tokens[x+1] = tokens[x+2];
						tokens[x+2] = "";
						tokens[x+3] = "";
					}

					if (tokens[x+1].startsWith("NUMERO_LINEA_") && tokens[x+2].startsWith("IDENTIFICADOR_") && tokens[x+3].equals("PUNTUACION_PUNTO_COMA"))
					{
						tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+2].substring(14)+">";
						tokens[x+1] = tokens[x+1];
						tokens[x+2] = "";
						tokens[x+3] = "";
					}

					if (tokens[x+1].startsWith("NUMERO_LINEA_") && tokens[x+2].startsWith("IDENTIFICADOR_") && tokens[x+3].startsWith("NUMERO_LINEA_") && tokens[x+4].equals("PUNTUACION_PUNTO_COMA"))
					{
						tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+2].substring(14)+">";
						tokens[x+1] = tokens[x+3];
						tokens[x+2] = "";
						tokens[x+3] = "";
						tokens[x+4] = "";
					}
				}
			}

			String cuerpo2 = "";
			for (int y=0; y<tokens.length; y++)
				if (!tokens[y].equals(""))
					cuerpo2 += tokens[y] + " ";

			metodos[d].setCuerpo(cuerpo2);
		}//for de cada cuerpo de cada metodo

		return 0;
	}//revisarCuerpoVariables()

	int revisarExistenciaMetodos()
	{
		//Antes de empezar quiero limpiar los argumentos
		//argumentos en el formato   <tipo-id>
		for ( int b=0; b<metodos.length; b++)
		{
			String [] args = metodos[b].getArgumentos().split(" ");

			for (int c=0; c<args.length; c++)
			{
				if (args[c].startsWith("TIPO_"))
				{
					args[c] = "<" + args[c].substring(5) + "-" + args[c+1].substring(14)+">";
					args[c+1] = "";
				}

				if (args[c].equals("PUNTUACION_COMA"))
					args[c] = "";
			}

			String new_args = "";
			for (int d=0; d<args.length; d++)
				if (!args[d].equals(""))
					new_args += args[d]+" ";

			if (new_args.equals(""))
				new_args = "NADA";

			metodos[b].setArgumentos(new_args);
		}//okay con los argumentos

		//ahora si revisar que exista el id del metodo al que se esta llamando
		for ( int b=0; b<metodos.length; b++)
		{
			String [] token = metodos[b].getCuerpo().split(" ");
			String linea = metodos[b].getLinea();
			for (int c=0; c<token.length; c++)
			{
				if (token[c].startsWith("NUMERO_LINEA_")) linea = token[c];

				if (token[c].equals("PUNTUACION_GATO"))
				{
					String id = token[c+1];
					boolean found = false;

					for (int d=0; d < metodos.length; d++)
						if (metodos[d].getNombre().equals(id))
							found = true;

					if (!found)
					{
						System.err.println("Linea: "+linea.substring(13));
						System.err.println("Metodo "+ id.substring(14) +" no ha sido definido.");
						return 1;
					}
				}//si encuentra llamada a algun metodo
			}
		}//for de cada cupero de cada metodo

		return 0;
	}//revisarMetodos()

	int revisarExistenciaVariables()
	{
		for (int a = 0; a<metodos.length; a++)
		{
			//contar cuantas declaraciones hay en TOTAL
			String [] token = metodos[a].getCuerpo().split("<declaracion-");
			String [] args = metodos[a].getArgumentos().split(" ");

			int total = token.length + args.length - 1 + g_pVariables.length;
			if (metodos[a].getArgumentos().equals("NADA"))
				total--;

			//hacer un recorrido por todos lo tokens
			//ir guardando las declaraciones en el vector variables
			//luego si encuentro algun uso de variable, checar en el vector, si si existe
			//si no, es porque, o aun no ha sido declarada, o nunca ha sido declarada
			String [] variables = new String[total];

			for (int u=0; u<variables.length; u++)
				variables[u] = "";

			int declaracion_actual = 0;
			String linea = metodos[a].getLinea();

			token = metodos[a].getCuerpo().split(" ");

			for (int z=0; z<g_pVariables.length; z++)
				variables[declaracion_actual++] = g_pVariables[z].getNombre().substring(14);

			args = metodos[a].getArgumentos().split(" ");

			if ( !metodos[a].getArgumentos().equals("NADA"))
			{
				for (int z=0; z<args.length; z++)
				{
					String [] _b = args[z].split("-");
					variables[declaracion_actual++] = _b[1].substring(0, _b[1].length()-1);///// error cuando no hay argumentos
				}
			}


			for (int b=0; b<token.length; b++)
			{
				if (token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if (token[b].startsWith("<declaracion-"))
				{
					String [] _a = token[b].split("-");
					variables[declaracion_actual++] = _a[3].substring(0, _a[3].length()-1);
				}

				if (token[b].startsWith("IDENTIFICADOR_"))
				{
					boolean found = false;
					for (int h=0; h<variables.length; h++)
						if (variables[h].equals(token[b].substring(14))) found=true;

					if (!found)
					{
						System.err.println("Linea: " + linea.substring(13));
						System.err.println("Variable " + token[b].substring(14) + " es utilizada antes de ser declarada.");
						return 1;
					}
				}

				if (token[b].startsWith("<asignacion-"))
				{
					String [] _t = token[b].split("-");
					String _id = _t[2].substring(0, _t[2].length() - 1);

					boolean found = false;
					for (int h=0; h<variables.length; h++)
						if (variables[h].equals(_id)) found=true;

					if (!found)
					{
						System.err.println("Linea: " + linea.substring(13));
						System.err.println("Variable " + _id + " es utilizada antes de ser declarada.");
						return 1;
					}

				}
			}//for de cada token
		}//for de metodos
		return 0;
	}//revisarExistenciaVariables

	void imprimirObjetos()
	{
		for (int z=0; z<metodos.length; z++)
		{
			debug.imprimirLinea( "\nMetodo "+z);
			debug.imprimirLinea( "Linea: " + metodos[z].getLinea());
			debug.imprimirLinea( "Nombre: " + metodos[z].getNombre());
			debug.imprimirLinea( "Retorno: " + metodos[z].getTipoDeRetorno());
			debug.imprimirLinea( "Argumentos: " + metodos[z].getArgumentos());
			debug.imprimirLinea( "Cuerpo: ");

			String _g [] = metodos[z].getCuerpo().split("\n");
			int tabs = 0;

			for (int _h = 0; _h < _g.length; _h++)
			{
				if ( _g[_h].startsWith("</")) tabs--;

				for (int b=0; b<tabs; b++) debug.imprimir("	");

				debug.imprimirLinea( _g[_h]);

				if ( _g[_h].startsWith("<llamada")) tabs++;
				if ( _g[_h].startsWith("<llave>")) tabs++;
				if ( _g[_h].startsWith("<asignacion")) tabs++;
				if ( _g[_h].startsWith("<parentesis>")) tabs++;
				if ( _g[_h].startsWith("<if")) tabs++;
				if ( _g[_h].startsWith("<while")) tabs++;
				if ( _g[_h].startsWith("<op ")) tabs++;
				if ( _g[_h].startsWith("<retorno")) tabs++;
			}
		}

		for (int z=0; z<g_pVariables.length; z++)
		{
			debug.imprimirLinea( "\nVariable "+z);
			debug.imprimirLinea( "linea: " + g_pVariables[z].getLinea());
			debug.imprimirLinea( "nombre: " + g_pVariables[z].getNombre());
			debug.imprimirLinea( "tipo: " + g_pVariables[z].getTipo());
		}
	}//imprimir objetos
}//clase analisis_semantico

