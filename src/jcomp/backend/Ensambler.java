package jcomp.backend;

import jcomp.util.Log;
import jcomp.util.PseudoTag;

public class Ensambler
{
	private String codigo;
	private String nuevo_codigo;
	Log debug;
	String sseg, dseg, cseg;

	public Ensambler()
	{
		this.debug = Log.getInstance();
	}

	public void setCodigo(String codigo)
	{
		this.codigo = codigo;
	}

	public String getCodigo()
	{
		return codigo;
	}

	public int iniciar()
	{
		nuevo_codigo = ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n";
		nuevo_codigo += "; Generado con jComp\n\n";
		nuevo_codigo += "	TITLE \n";
		nuevo_codigo += "	.686P\n";
		nuevo_codigo += "	.XMM\n";
		nuevo_codigo += "	include listing.inc\n";
		nuevo_codigo += "	.model\tflat\n\n";

		nuevo_codigo += "INCLUDELIB LIBCMT\n";
		nuevo_codigo += "INCLUDELIB OLDNAMES\n";

		dseg = agregarDeclaracionesGlobales();

		cseg = "PUBLIC\t_main\n";
		cseg += "_TEXT\tSEGMENT\n";
		cseg += agregarProcedimientos();

		nuevo_codigo = nuevo_codigo + dseg + cseg;
		nuevo_codigo += "_TEXT ENDS\n";
		nuevo_codigo += "END\n";

		codigo = nuevo_codigo;

		debug.imprimirLinea("\n\n----------------------");
		debug.imprimirLinea(" CODIGO CON MNEMONICOS MIOS");
		debug.imprimirLinea("----------------------");

		String lineas [] = codigo.split("\n");

		for (int a = 0; a< lineas.length; a++)
		{
			debug.imprimirLinea(lineas[a]);
		}

		debug.imprimirLinea("");

		convertirMnemonicosFinales();

		return 0;
	}

	private void convertirMnemonicosFinales()
	{
		String lineas [] = codigo.split("\n");

		codigo = "";
		for (int a = 0; a< lineas.length; a++)
		{
			if (lineas[a].indexOf("empujar") != -1 )
			{
				lineas[a] = lineas[a].substring( 9 );
				lineas[a] = "\tpush "+lineas[a];
			}

			if (lineas[a].indexOf("asigna") != -1 )
			{
				String f  = lineas[a].substring( 8 );
				lineas[a] = "\n\t; asignacion "+lineas[a]+"\n";
				lineas[a] += "\tpop eax\n";
				lineas[a] += "\tmov"+f+", eax\n";
			}

			if (lineas[a].indexOf("SUMA") != -1 )
			{
				lineas[a] = "\t; suma\n";
				lineas[a] += "\tpop\teax\n";
				lineas[a] += "\tpop\tebx\n";
				lineas[a] += "\tadd\teax, ebx\n";
				lineas[a] += "\tpush\teax\n";
			}

			if (lineas[a].indexOf("RESTA") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		pop bx\n";
				lineas[a] += "		sub bx, ax\n";
				lineas[a] += "		push bx\n";
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
				lineas[a] = "\t; implicit return \n";
				lineas[a] += "	pop\teax\n";
				//this is only needed when locals were 
                //in the stack i think
				lineas[a] += "	mov	esp, ebp\n";
				lineas[a] += "	pop\tebp\n";
				lineas[a] += "	ret\t0\n";
			}

			if (lineas[a].indexOf("MAYOR") != -1 )
			{
				lineas[a] = "\n	pop ax\n";
				lineas[a] += "	pop bx\n";
		 		lineas[a] += "	cmp bx, ax\n";
				lineas[a] += "	je while_fin\n";
				lineas[a] += "	jl while_fin\n";
			}

			if (lineas[a].indexOf("MENOR") != -1 )
			{
				lineas[a] = "\n	pop ax\n";
				lineas[a] += "	pop bx\n";
				lineas[a] += "	cmp bx, ax\n";
				lineas[a] += "	je while_fin\n";
				lineas[a] += "	jg while_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n	jmp while_cond\n";
				lineas[a] += "	while_fin:\n";
			}

			codigo += lineas[a]+"\n";
		}//For de cada linea
	}//metodo

	private String agregarDeclaracionesGlobales()
	{
		String tokens[] = codigo.split("\n");
		String dataSegmentTmp = "";

		for (int a = 0; a<tokens.length; a++)
		{
			if (tokens[a].startsWith("<declaracion global"))
			{
				String s [] = tokens[a].split(" ");
				dataSegmentTmp += "COMM\t"+s[3].substring(3, s[3].length()-1)+":DWORD\n";
			}
		}

		String temp = "";
		if (dataSegmentTmp.length() > 0)
		{
			temp = "_DATA\tSEGMENT\n";
			temp += dataSegmentTmp;
			temp += "_DATA\tENDS\n";
		}

		return temp + "\n";
	}//declaraciones

	private String agregarProcedimientos()
	{
		String cseg = "";

		// Declarar variables locales
		String localVarDeclaraciones = "";

		String tokens[] = codigo.split("\n");

		for (int a = 0; a<tokens.length; a++)
		{
			if (tokens[a].startsWith("<METODO"))
			{
				int inicio = a+1;

				String s [] = tokens[a].split(" ");
				String nombre = s[1].substring(3);

				// Cambiar el nombre del metodo main
				if (nombre.equals("main"))
				{
					nombre = "_main";
				}

				// Prologo
				cseg += nombre +"\tPROC\n";
				cseg += "\tpush\tebp\n";
				cseg += "\tmov\tebp, esp\n";

				// leer variables que le pasan al metodo
				// desde la pila pop ax y eso
				int currentLocalSize = 0;
				String cseg_temp = "";
				while (!tokens[++a].equals("</METODO>"))
				{
					if (tokens[a].startsWith("<declaracion tipo:"))
					{
						currentLocalSize += 4;
						// Obtener el nombre de la variable
						String declaracionLocalSp [] = tokens[a].split(" ");
						String variableName = declaracionLocalSp[2].substring(3, declaracionLocalSp[2].length()-1);

						// Guardar la declaracion de las variables locales
						localVarDeclaraciones += (variableName + "$ = -" +currentLocalSize+ "\t\t\t\t\t\t; size = 4\n");

						// Escribir la inicializacion : mov	DWORD PTR _local_var$[ebp], 5
						cseg_temp += "\tmov\tDWORD PTR " + variableName + "$[ebp], 0\n";
					}
				}

				// Hacer espacio para variables locales
				if (currentLocalSize > 0)
				{
					cseg += "\tsub esp, " + currentLocalSize + "\n";
				}

				cseg += cseg_temp;

				int fin = a-1;

				// Insertar el cuerpo
				cseg += convertirNomenclatura(inicio, fin);

				// Fin
				cseg += "	xor\teax, eax\n";
				cseg += "	pop\tebp\n";
				cseg += "	ret\t0\n";
				cseg += ""+nombre + "\tENDP\n";
			}
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

	String convertirNomenclatura( int inicio, int fin )
	{
		String [] tokens = codigo.split("\n");
		String cseg = "";

		for (int a = inicio; a<fin; a++)
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
					cseg += "\t\tempujar "
						+ variableTag.get("id")
						+ "\n";
				}
				else if (variableTag.get("scope").equals("local")) // Empujar variable local
				{
					cseg += "\t\tempujar "
						+ variableTag.get("id")
						+ "$[ebp]\n";
				}
				tokens[a] = "*";
			}

			// <INT valor:7> Una literal
			if (tokens[a].startsWith("<INT valor:"))
			{
				cseg += "\tempujar "
						+ variableTag.get("valor")
						+ "\n";

				tokens[a] = "*";
			}

			if (tokens[a].startsWith("<while") )
			{
				cseg += "		while_cond:\n";
				tokens[a] = "*";
			}

			if (tokens[a].startsWith("</while") )
			{
				cseg += "		while_body:\n";
				tokens[a] = "*";
			}

			boolean vacio = false;

			if (tokens[a].startsWith("</") )
			{
				tokens[a] = "*";
				while( !tokens[--a].startsWith("<") )
				{
					if ( a == inicio ) vacio = true; 
				}

				if (!vacio)
				{
					if (tokens[a].indexOf("llamada tipo:") != -1 )
					{
						tokens[a] = "		call "+ tokens[a].substring( tokens[a].indexOf("id:") + 3, tokens[a].length()-1 );
					}

					if (tokens[a].indexOf("op tipo:") != -1 )
						tokens[a] = "		"+tokens[a].substring( tokens[a].indexOf("tipo:") + 5, tokens[a].length()-1 );

					if (tokens[a].indexOf("<retorno>") != -1 )
						tokens[a] = "		retornar";

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
						String varId = tokenTag.get("id");

						if (tokenTag.get("scope").equals("global"))
						{
							tokens[a] = "\tasigna "+ varId +"\n";
						}
						else
						{
							// Para las variables locales, puedo usar el nombre
							tokens[a] = "\tasigna DWORD PTR "+ varId +"$[ebp]\n";
						}
					}

					if (tokens[a].indexOf("<llave") != -1 )
					{
						tokens[a] = "\twhile_fin: \n";
					}

					cseg += tokens[a] + "\n";

					tokens[a] = "*";
				}//if de metodo vacio
			}
		}//for de cada metodo
		return cseg;
	}
}//class ensambler
