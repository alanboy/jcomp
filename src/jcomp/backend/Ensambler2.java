package jcomp.backend;

import jcomp.util.Log;
import jcomp.util.PseudoTag;
import java.util.Hashtable;

public class Ensambler2
{
	Log debug;
	private String codigo;
	private Hashtable<String, PseudoTag> mapaVariablesLocales;

	private static String join(String [] a, char c)
	{
		StringBuilder s = new StringBuilder();
		for (String si : a)
		{
			s.append(si + c);
		}
		return s.toString();
	}

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
		String dseg, cseg;
		dseg = agregarDeclaracionesGlobales();

		cseg = "section .text\n";
		cseg += "\tglobal _start\n";

		cseg += procesarMetodos();

		cseg += agregarCodigoNativo();

		codigo = dseg + cseg;

		debug.imprimirLinea("----------------------");
		debug.imprimirLinea(" GENERACION DE CODIGO INTERMEDIA");
		debug.imprimirLinea("----------------------");

		String lineas [] = codigo.split("\n");

		for (int a = 0; a < lineas.length; a++)
		{
			debug.imprimirLinea(lineas[a]);
		}

		convertirMnemonicosFinales();

		return 0;
	}

	private void convertirMnemonicosFinales()
	{
		String lineas [] = codigo.split("\n");

		codigo = ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n";
		codigo += "; Generado con jComp\n\n";

		for (int a = 0; a < lineas.length; a++)
		{
			if (lineas[a].trim().startsWith(";"))
			{
				codigo += lineas[a]+"\n";
				continue;
			}

			if (lineas[a].indexOf("empujar ") != -1 )
			{
				lineas[a] = lineas[a].substring( 9 );
				lineas[a] = "\tpush "+lineas[a];
			}

			if (lineas[a].indexOf("empujarapuntador") != -1 )
			{
				lineas[a] = lineas[a].substring( 18 );
				lineas[a] = "\tpush dword ["+lineas[a] +"]";
			}

			if (lineas[a].indexOf("asignaAGlobal") != -1 )
			{
				String id = lineas[a].substring( 14 );
				lineas[a] = "\t; asignacion a global\n";
				lineas[a] += "\tpop eax\n";
				lineas[a] += "\tmov [";
				lineas[a] += id;
				lineas[a] += "], eax\n";
			}

			if (lineas[a].indexOf("SUMA") != -1 )
			{
				lineas[a] = "\t; suma\n";
				lineas[a] += "\tpop eax\n";
				lineas[a] += "\tpop ebx\n";
				lineas[a] += "\tadd eax, ebx\n";
				lineas[a] += "\tpush eax\n";
			}

			if (lineas[a].indexOf("RESTA") != -1 )
			{
				lineas[a] = "\t; resta\n";
				lineas[a] += "	pop eax\n";
				lineas[a] += "	pop ebx\n";
				lineas[a] += "	sub ebx, eax\n";
				lineas[a] += "	push ebx\n";
			}

			if (lineas[a].indexOf("MUL") != -1 )
			{
				lineas[a] = "\n	pop ax\n";
				lineas[a] += "	pop bx\n";
				lineas[a] += "	mul bx\n";
				lineas[a] += "	push ax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] = "\n\t; return explicito de funcion \n";
				lineas[a] += "	pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "	mov esp, ebp\n";
				lineas[a] += "	pop ebp\n";
				lineas[a] += "	ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main sinfica hacer syscall
				lineas[a] = "\n\t; return explicito \n";
				lineas[a] += "	mov eax, 1\n"; // Syscall para salir del proces (sys_exit)
				lineas[a] += "	pop ebx\n";    // El valor a regresar (exit value)
				lineas[a] += "	int 80h\n";
			}

			if (lineas[a].indexOf("MAYOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n	pop eax\n";
				lineas[a] += "	pop ebx\n";
		 		lineas[a] += "	cmp ebx, eax\n";
				lineas[a] += "	je while_" + id + "_fin\n";
				lineas[a] += "	jl while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("MENOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n	pop eax\n";
				lineas[a] += "	pop ebx\n";
				lineas[a] += "	cmp ebx, eax\n";
				lineas[a] += "	je while_" + id + "_fin\n";
				lineas[a] += "	jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n	jmp while_cond\n";
				lineas[a] += "while_fin:\n";
			}

			codigo += lineas[a]+"\n";
		} // for de cada linea
	} // metodo

	private String agregarCodigoNativo()
	{
		String [] asm = new String[]
		{
			"putc:",
			"	push ebp",
			"	mov ebp, esp",

			"	mov eax, ebp",
			"	mov ebx, 8",
			"	add eax, ebx",
			"	push eax            ; calcular la direccion del primer argumento",
			"	pop ecx             ; ebp+8 y ponerla en ecx ",

			"	mov eax,4           ; la system call para escribir en la pantalla (sys_write)",
			"	mov ebx,1           ; file descriptor 1 - standard output",
			"	mov edx,1           ; la longitud de bytes que queremos imprimir",
			"	int 80h             ; llamar al kernel",

			"	mov esp, ebp",
			"	pop ebp",
			"	ret",
			"	; fin de putc"
		};

		return join(asm, '\n');
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
				dataSegmentTmp += "\t"+s[3].substring(3, s[3].length()-1)+": dd 0\n";
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
		String localVarDeclaraciones = "";

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

			// Cambiar el nombre del metodo main
			if (nombre.equals("main"))
			{
				nombre = "_start";
			}

			// Prologo
			cseg += nombre +":\n";
			cseg += "\t; create the stack frame\n";
			cseg += "\tpush ebp \n";
			cseg += "\tmov ebp, esp\n";

			int espacioParaVariablesLocales = 0;
			String cseg_temp = "\n\t; inicializar las variables locales\n";

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

				for (String argumento : argumentos)
				{
					String variableName = argumento.trim().split(" ")[1];

					PseudoTag variableLocal = new PseudoTag("", true /*failSilently*/);
					variableLocal.set("id", variableName);
					variableLocal.set("stackpos", "+"  + argumentoActual);
					variableLocal.set("scope", "arg");

					argumentoActual += 4;

					mapaVariablesLocales.put(variableName, variableLocal);

				}
			}

			while (!tokens[++a].equals("</METODO>"))
			{
				if (tokens[a].startsWith("<declaracion tipo:"))
				{
					// por ahora todas las variables locales son de 4 bytes
					espacioParaVariablesLocales += 4;

					// Obtener el nombre de la variable
					String declaracionLocalSp [] = tokens[a].split(" ");
					String variableName = declaracionLocalSp[2].substring(3, declaracionLocalSp[2].length()-1);

					// Guardar la declaracion de las variables locales
					// localVarDeclaraciones += (variableName + "$ = -" +espacioParaVariablesLocales+ "\t\t\t\t\t\t; size = 4\n");

					// Escribir la inicializacion : mov	DWORD PTR _local_var$[ebp], 5
					cseg_temp += "\tmov DWORD [ebp-" + espacioParaVariablesLocales + "], 0 ; "+ variableName + "\n";

					PseudoTag variableLocal = new PseudoTag(tokens[a]);
					variableLocal.set("stackpos", "-" + espacioParaVariablesLocales);
					mapaVariablesLocales.put(variableName, variableLocal);

				}
			}

			// Hacer espacio para variables locales
			if (espacioParaVariablesLocales > 0)
			{
				cseg += "\tsub esp, " + espacioParaVariablesLocales + "\n";
			}

			cseg += cseg_temp;

			int fin = a-1;

			// Insertar el cuerpo
			cseg += convertirNomenclatura(inicio, fin, nombre.equals("_start"));

			// Fin
			if (nombre.equals("_start"))
			{
				cseg += "	mov eax, 1\n"; // Syscall para salir del proces (sys_exit)
				cseg += "	mov ebx, 0\n"; // El valor a regresar (exit value)
				cseg += "	int 80h\n";
			}
			else
			{
				// solo hay que hacer esto para metodos void
				cseg += "	mov esp, ebp\n";
				cseg += "	pop ebp\n";
				cseg += "	ret\n";
			}

			cseg += "\t; fin de "+nombre + "\n\n";
		}

		// Invertir el orden de localVarDeclaraciones
		String localInitParts [] = localVarDeclaraciones.split("\n");
		String localVarDeclaracionesBuff = "";
		for (int localDefI = localInitParts.length - 1;  localDefI >= 0 ; localDefI--)
		{
			localVarDeclaracionesBuff += localInitParts[ localDefI ] + "\n";
		}

		return localVarDeclaracionesBuff + cseg;
	}//procedimientos

	String convertirNomenclatura(int inicio, int fin, boolean metodoMain)
	{
		String [] tokens = codigo.split("\n");
		String cseg = "";
		String whileActual = "";

		for (int a = inicio; a < fin; a++)
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
					cseg += "\tempujarapuntador "
						+ variableTag.get("id") + "\n";
				}
				else if (variableTag.get("scope").equals("local")) // Empujar variable local
				{
					PseudoTag tokenTag = mapaVariablesLocales.get(variableTag.get("id"));
					cseg += "\t; empujar variable local \n"
						+ "\tpush DWORD [ebp"+tokenTag.get("stackpos")+"]\n";
				}
				else if (variableTag.get("scope").equals("arg")) // Empujar variable local
				{
					PseudoTag tokenTag = mapaVariablesLocales.get(variableTag.get("id"));
					cseg += "\t; empujar arg \n"
						+ "\tpush DWORD [ebp"+tokenTag.get("stackpos")+"]\n";
				}
				tokens[a] = "*";
			}

			// <INT valor:7> Una literal
			if (tokens[a].startsWith("<INT valor:"))
			{
				cseg += "\tempujar " + variableTag.get("valor") + "\n";
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

				if (!vacio)
				{
					if (tokens[a].indexOf("llamada tipo:") != -1 )
					{
						tokens[a] = "\tcall "+ tokens[a].substring( tokens[a].indexOf("id:") + 3, tokens[a].length()-1 )
							+ "\n\tpush eax\n";
					}

					if (tokens[a].indexOf("op tipo:") != -1 )
					{
						tokens[a] = "\t"+tokens[a].substring(tokens[a].indexOf("tipo:") + 5, tokens[a].length()-1);
					}

					if (tokens[a].indexOf("<retorno>") != -1 )
					{
						tokens[a] = metodoMain ? "\tsalir" : "\tretornar";
					}

					// Resolver los id's de la asignacion.
					// Si es una variable global:
					// 			mov     DWORD PTR _global_var, 45
					//
					// Si es una variable logal:
					//			mov     eax, DWORD PTR _local_var$[ebp]
					//
					PseudoTag tokenTag = new PseudoTag(tokens[a]);
					if (tokens[a].indexOf("asignacion tipo:INT") != -1)
					{
						tokenTag = mapaVariablesLocales.get(tokenTag.get("id"));
                		if (tokenTag == null)
						{
							tokenTag = new PseudoTag(tokens[a]);
							// si no esta en el mapa de variables locales, entonces
							// esta es una variable global
							tokens[a] = "\tasignaAGlobal "+ tokenTag.get("id") + "\n";
						}
						else
						{
							// Para las variables locales NO puedo usar el nombre
							tokens[a] = "\n\t; asignacion local a " + tokenTag.get("id") + "\n";
							tokens[a] += "\tpop eax\n";
							tokens[a] += "\tmov DWORD [ebp"+tokenTag.get("stackpos")+"], eax\n";
						}
					}

					if (tokens[a].indexOf("<llave") != -1 )
					{
						tokens[a] = "\n	jmp while_"+whileActual+"_cond\n";
						tokens[a] += "while_"+whileActual+"_fin:\n";
					}

					cseg += tokens[a] + "\n";

					tokens[a] = "*";
				}//if de metodo vacio
			}
		}//for de cada metodo
		return cseg;
	}
}//class ensambler

