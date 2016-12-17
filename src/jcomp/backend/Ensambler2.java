package jcomp.backend;

import jcomp.util.Log;
import jcomp.util.PseudoTag;
import java.util.Hashtable;
import java.util.Arrays;

public class Ensambler2
{
	private Log debug;
	private String codigo;
	private Hashtable<String, PseudoTag> mapaVariablesLocales;

	public Ensambler2(String codigo)
	{
		this.mapaVariablesLocales = new Hashtable<String, PseudoTag>();
		this.debug = Log.getInstance();
		this.codigo = codigo;
	}

	public String getCodigo()
	{
		return codigo;
	}

	public int iniciar()
	{
		debug.imprimirEncabezado("GENERACION DE CODIGO INTERMEDIO");

		String dseg, cseg;
		dseg = agregarDeclaracionesGlobales();

		cseg = "section .text\n";
		cseg += "  global _start\n";
		cseg += "  UsosExternos\n";

		cseg += procesarMetodos();

		codigo = dseg + cseg;

		String lineas [] = codigo.split("\n");

		for (int a = 0; a < lineas.length; a++)
		{
			debug.imprimirLinea(a + ": " + lineas[a]);
		}

		return 0;
	}

	private String agregarDeclaracionesGlobales()
	{
		String tokens[] = codigo.split("\n");
		String dataSegmentTmp = "";

		for (int a = 0; a<tokens.length; a++)
		{
			if (tokens[a].startsWith("<declaracion global"))
			{
				String s [] = tokens[a].split(" ");
				dataSegmentTmp += "  "+s[3].substring(3, s[3].length()-1)+": dd 0\n";
			}
		}

		String temp = "";
		if (dataSegmentTmp.length() > 0)
		{
			temp = "section .data\n";
			temp += dataSegmentTmp;
		}

		return temp + "\n";
	}//declaraciones

	private String procesarMetodos()
	{
		String cseg = "";
		boolean returnExplicto = false;

		// Declarar variables locales
		String tokens[] = codigo.split("\n");

		for (int a = 0; a<tokens.length; a++)
		{
			if (!tokens[a].startsWith("<METODO"))
			{
				continue;
			}

			int inicio = a+1;

			String s [] = tokens[a].split(" ");
			String nombre = s[1].substring(3);

			if (nombre.equals("putc"))
			{
				continue;
			}

			if (nombre.equals("getc"))
			{
				continue;
			}

			// Cambiar el nombre del metodo main
			if (nombre.equals("main"))
			{
				nombre = "_start";
			}

			debug.imprimirLinea("\nProcesando metodo: " + nombre);

			// Prologo
			cseg += nombre +":\n";
			cseg += "  ; create the stack frame\n";
			cseg += "  push ebp \n";
			cseg += "  mov ebp, esp\n";

			int espacioParaVariablesLocales = 0;
			String cseg_temp = "\n  ; inicializar las variables locales que han sido declaradas aqui (no los argumentos)\n";

			// Un metodo que recibe argumentos se ve asi:
			//
			//    <METODO id:print args:INT a, INT b regresa:INT>
			//
			// hay que hacer espacio para esos argumentos en
			// variables locales. Un metodo que no recibe nada:
			//
			//    <METODO id:main args:NADA regresa:INT>
			//
			boolean argumentosAlMetodo = tokens[a].indexOf("args:NADA") == -1;

			if (argumentosAlMetodo)
			{
				int inicioDeArgumentos = tokens[a].indexOf("args:") + 5;
				int finDeArgumentos = tokens[a].lastIndexOf("regresa:");
				String [] argumentos = tokens[a].substring(inicioDeArgumentos, finDeArgumentos).trim().split(",");

				int argumentoActual = 8;

				for (int i = argumentos.length-1; i >= 0; i--)
				{
					String argumento = argumentos[i];
					String variableNombre = argumento.trim().split(" ")[1];

					PseudoTag variableLocal = new PseudoTag("", true /*failSilently*/);
					variableLocal.set("id", variableNombre);
					variableLocal.set("stackpos", "+" + argumentoActual);
					variableLocal.set("scope", "arg");

					debug.imprimirLinea("Nuevo argumento: id=" + variableNombre + " \nposicion en stack=+" + argumentoActual);
					mapaVariablesLocales.put(variableNombre, variableLocal);

					argumentoActual += 4;
				}
			}

			while (!tokens[++a].equals("</METODO>"))
			{
				// Hay dos tipos de declaraciones, tipos primitivos y arreglos:
				// <declaracion tipo:INT id:z linea:24>
				// <declaracion tipo:INT[5] id:a linea:25>
				//
				if (tokens[a].startsWith("<declaracion tipo:"))
				{
					// Obtener el nombre de la variable
					String declaracionLocalSp [] = tokens[a].split(" ");
					String variableNombre = declaracionLocalSp[2].substring(3, declaracionLocalSp[2].length()-1);
					String variableTipo = declaracionLocalSp[1].substring(5);

					if (variableTipo.contains("[")) {
						// este es un arrelgo, y el tamano es el tamano del arreglo
						// multiplicado por el tamano de la variable que contiene
						int tamArreglo = Integer.parseInt(variableTipo.substring(variableTipo.indexOf("[")+1, variableTipo.length() - 1));
						espacioParaVariablesLocales += (4 * tamArreglo);

						// los arreglos no estan inicializados por el momento
						cseg_temp += "                      "
									+ ";  variable="+ variableNombre + ", tipo=" + variableTipo + " no ha sido inicializado\n";

					} else {
						// por ahora todas las variables locales son de 4 bytes
						espacioParaVariablesLocales += 4;

						// Escribir la inicializacion : mov	DWORD PTR _local_var$[ebp], 5
						cseg_temp += "  mov DWORD [ebp-" + espacioParaVariablesLocales + "], 0 "
									+ ";  variable="+ variableNombre + ", tipo=" + variableTipo + "\n";
					}

					PseudoTag variableLocal = new PseudoTag(tokens[a]);
					variableLocal.set("stackpos", "-" + espacioParaVariablesLocales);

					debug.imprimirLinea("Nueva variable local: id=" + variableNombre + " \n posicion en stack= -" + espacioParaVariablesLocales);

					mapaVariablesLocales.put(variableNombre, variableLocal);
				}
			}

			// Hacer espacio para variables locales
			if (espacioParaVariablesLocales > 0)
			{
				cseg += "  sub esp, " + espacioParaVariablesLocales + "\n";
			}

			cseg += cseg_temp;

			int fin = a-1;

			// Insertar el cuerpo
			cseg += convertirNomenclatura(inicio, fin, nombre.equals("_start"));

			// Fin
			if (nombre.equals("_start"))
			{
				cseg += "  salir\n";
			}
			else
			{
				// solo hay que hacer esto para metodos void
				cseg += "  mov esp, ebp\n";
				cseg += "  pop ebp\n";
				cseg += "  ret\n";
			}

			cseg += "  ; fin de "+nombre + "\n\n";
		}

		return cseg;
	} //procedimientos

	// Convertir tokens dentro del metodo. Los tokens de este metodo inician
	// en `inicio` y terminan en `fin`.
	String convertirNomenclatura(int inicio, int fin, boolean metodoMain)
	{
		String [] tokens = codigo.split("\n");
		String cseg = "";
		String whileActual = "";

		for (int a = inicio; a <= fin; a++)
		{
			while (tokens[a].equals("<coma>") || tokens[a].startsWith("<declaracion"))
			{
				tokens[a++] = "*";
			}

			PseudoTag variableTag = new PseudoTag(tokens[a]);

			// <INT id:_local_var scope:local>
			// <INT id:_global_var scope:global>
			if (tokens[a].startsWith("<INT id"))
			{
				if (variableTag.get("scope").equals("global")) // Empujar variable global
				{
					cseg += "  empujarapuntador " + variableTag.get("id") + "\n";
				}
				else if (variableTag.get("scope").equals("local")) // Empujar variable local
				{
					PseudoTag tokenTag = mapaVariablesLocales.get(nombreDeVariable(variableTag.get("id")));

					cseg += "  ; empujar variable local \n"
						+ "  push DWORD [ebp"+tokenTag.get("stackpos")+"]\n";
				}
				else if (variableTag.get("scope").equals("arg")) // Empujar variable local
				{
					PseudoTag tokenTag = mapaVariablesLocales.get(variableTag.get("id"));

					cseg += "  ; empujar arg \n" + "  push DWORD [ebp"+tokenTag.get("stackpos")+"]\n";
				}

				tokens[a] = "*";
			}

			// Esto sera necesario cuando quiera implmentar arreglos como argumentos de un metodo
			if (tokens[a].startsWith("<INT["))
			{
				cseg += "\n";

				if (variableTag.get("scope").equals("global"))
				{
					cseg += "  empujarapuntador "
						+ variableTag.get("id") + "\n";
				}
				else if (variableTag.get("scope").equals("local"))
				{
					PseudoTag tokenTag = mapaVariablesLocales.get(variableTag.get("id"));
					cseg += "  ; empujar referencia a variable local \n";
					cseg += "  mov ebx, ebp\n";
					cseg += "  mov eax, "+tokenTag.get("stackpos")+"\n";
					cseg += "  add eax, ebx\n";
					cseg += "  push eax\n";
				}
				else if (variableTag.get("scope").equals("arg"))
				{
					PseudoTag tokenTag = mapaVariablesLocales.get(variableTag.get("id"));
					cseg += "  ; empujar referencia a variable local (argumento al metodo)\n";
					cseg += "  mov ebx, ebp\n";
					cseg += "  mov eax, "+tokenTag.get("stackpos")+"\n";
					cseg += "  add eax, ebx\n";
					cseg += "  push eax\n";
				}


				tokens[a] = "*";
			}

			// <STRING valor:"asdf"> Una literal
			if (tokens[a].startsWith("<STRING valor:"))
			{
				cseg += "  empujar " + variableTag.get("valor") + "\n";
				tokens[a] = "*";
			}

			// <INT valor:7> Una literal
			if (tokens[a].startsWith("<INT valor:"))
			{
				cseg += "  empujar " + variableTag.get("valor") + "\n";
				tokens[a] = "*";
			}

			// <while linea:22>
			if (tokens[a].startsWith("<while") )
			{
				int lineaInicio = tokens[a].indexOf("linea:") + 6;
				int lineaFin = tokens[a].indexOf(">");
				String linea = tokens[a].substring(lineaInicio, lineaFin);

				whileActual = linea;

				cseg += "while_"+ linea +"_cond:\n";
				tokens[a] = "*";
			}

			if (tokens[a].startsWith("</while") )
			{
				cseg += "while_"+ whileActual+"_body:\n";
				tokens[a] = "*";
			}

			if (tokens[a].indexOf("MAYOR") != -1 )
			{
				tokens[a] = "<op tipo:MAYOR_"+ whileActual + "\\> n";
			}

			if (tokens[a].indexOf("MENOR") != -1 )
			{
				tokens[a] = "<op tipo:MENOR_"+ whileActual + "\\> n";
			}

			boolean vacio = false;

			if (tokens[a].startsWith("</"))
			{
				tokens[a] = "*";

				while (!tokens[--a].startsWith("<"))
				{
					if (a == inicio) vacio = true;
				}

				if (vacio)
				{
					continue;
				}

				if (tokens[a].indexOf("<arreglo") != -1)
				{
					PseudoTag variableTag1 = new PseudoTag(tokens[a].replaceAll("-", " "));
					PseudoTag tokenTag = mapaVariablesLocales.get(variableTag1.get("id"));

					// los arreglos declarados en este metodo, la direccion esta en ebp + el offset,
					// para los arreglos que me pasaron por arguemntos, usar esa direccion en vez de
					// ebp
					if (tokenTag.get("scope") != null && tokenTag.get("scope").equals("arg"))
					{
						cseg += "\n  ;;;;;;;;; deref a un arreglo pasado como argumento\n";
						cseg += "  mov ebx, ebp\n";
						cseg += "  mov eax, " + tokenTag.get("stackpos")+"\n";
						cseg += "  add eax, ebx\n";

						cseg += "  mov ecx, [eax]\n";
						cseg += "  mov eax, ecx\n";

						cseg += "  pop ecx\n";
						cseg += "  imul ecx, 4\n";
						cseg += "  add eax, ecx\n";
						cseg += "  push DWORD [eax]\n\n";
					}
					else
					{
						cseg += "\n  ;;;;;;;;; deref a un arreglo declarado localmente\n";
						cseg += "  mov ebx, ebp\n";
						cseg += "  mov eax, " + tokenTag.get("stackpos")+"\n";
						cseg += "  add eax, ebx\n";

						cseg += "  pop ecx\n";
						cseg += "  imul ecx, 4\n";
						cseg += "  add eax, ecx\n";
						cseg += "  push DWORD [eax]\n\n";
					}

					tokens[a] = "";
				}

				if (tokens[a].indexOf("llamada tipo:") != -1)
				{
					tokens[a] = "\n  ; llamada a metodo\n"
					    + "  call "+ tokens[a].substring(tokens[a].indexOf("id:") + 3, tokens[a].length()-1)
						+ "\n  push eax\n";
				}

				if (tokens[a].indexOf("op tipo:") != -1 )
				{
					tokens[a] = "  "+tokens[a].substring(tokens[a].indexOf("tipo:") + 5, tokens[a].length()-1);
				}

				if (tokens[a].indexOf("<retorno>") != -1)
				{
					tokens[a] = metodoMain ? "\n  salir" : "  retornar";
				}

				// Resolver los id's de la asignacion.
				// Si es una variable global:
				// 			mov     DWORD PTR _global_var, 45
				//
				// Si es una variable logal:
				//			mov     eax, DWORD PTR _local_var$[ebp]
				//
                // Token tag se reescribe ?
				PseudoTag tokenTag = new PseudoTag(tokens[a]);
				String varId = tokenTag.get("id");

				if (tokens[a].indexOf("asignacion tipo:INT") != -1)
				{
					tokenTag = mapaVariablesLocales.get(nombreDeVariable(tokenTag.get("id")));
					if (tokenTag == null)
					{
						// Variable global
						tokenTag = new PseudoTag(tokens[a]);
						tokens[a] = "  asignaAGlobal "+ tokenTag.get("id") + "\n";
					}
					else if (varId.contains("["))
					{
						int indice = Integer.parseInt(varId.substring(varId.indexOf("[")+1, varId.indexOf("]")));

						// Esta es una variable local
						tokens[a] = "\n  ; asignacion local indice en arreglo: " + varId + "\n";
						tokens[a] += "  pop eax\n";
						tokens[a] += "  mov DWORD [ebp"+tokenTag.get("stackpos")+"+" + (indice*4) +"], eax\n";
					}
					else
					{
						// Esta es una variable local
						tokens[a] = "\n  ; asignacion local " + varId + "\n";
						tokens[a] += "  pop eax\n";
						tokens[a] += "  mov DWORD [ebp"+tokenTag.get("stackpos")+"], eax\n";
					}
				}

				if (tokens[a].indexOf("asignacion tipo:STRING") != -1)
				{
					tokenTag = mapaVariablesLocales.get(tokenTag.get("id"));
					tokens[a] = "\n  ; asignacion local a " + tokenTag.get("id") + "\n";
					tokens[a] += "  pop eax\n";
					tokens[a] += "  mov DWORD [ebp"+tokenTag.get("stackpos")+"], eax\n";
				}

				if (tokens[a].indexOf("derecho") != -1)
				{
					tokens[a] = "  ; termina lado dercho\n";
				}

				if (tokens[a].indexOf("izquierdo") != -1)
				{
					tokens[a] = "  ; termina lado izquiero\n";
				}

				if (tokens[a].indexOf("asignacion tipo:[]INT") != -1)
				{
					tokenTag = mapaVariablesLocales.get(tokenTag.get("id"));

					tokens[a] = "\n  ; asignacion a elemento de arreglo `" + tokenTag.get("id") + "`\n";
					tokens[a] += "  ; el arreglo esta en ebp" + tokenTag.get("stackpos") + " el indice esta en la pila\n";

					tokens[a] += "  pop ecx ; el valor a guardar\n\n";
					tokens[a] += "  push " + tokenTag.get("stackpos") + " \n";
					tokens[a] += "  push ebp\n";
					tokens[a] += "  SUMA\n";

					tokens[a] += "  pop ebx         ; the address\n";

					if (tokenTag.get("scope") != null && tokenTag.get("scope").equals("arg"))
					{
						tokens[a] += "  mov eax, [ebx] ; este es un apuntador\n";
						tokens[a] += "  mov ebx, eax\n";
					}

					tokens[a] += "  pop eax         ; the index\n";
					tokens[a] += "  imul eax, 4     ; \n";

					tokens[a] += "  add ebx, eax    ; final address in ebx\n";

					tokens[a] += "\n  ; termina asignacion aca\n";
					tokens[a] += "  mov DWORD [ebx], ecx\n";
					tokens[a] += "\n";
				}

				if (tokens[a].indexOf("<llave") != -1)
				{
					tokens[a] = "\n  jmp while_"+whileActual+"_cond\n";
					tokens[a] += "while_"+whileActual+"_fin:\n";
				}

				cseg += tokens[a] + "\n";

				tokens[a] = "*";
			}
		} //for de cada metodo
		return cseg;
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
}//class ensambler

