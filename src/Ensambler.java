public class Ensambler{

private String codigo;
private String nuevo_codigo;
Debugger debug;

	Ensambler(){}

	void setCodigo(String codigo){
		this.codigo = codigo;
	}

	void setDebugger(Debugger debug){
		this.debug = debug;
	}


	String getCodigo(){

		return codigo;
	}


String sseg, dseg, cseg;

	int iniciar(){

		nuevo_codigo = ".686\n\n";

		sseg = "SSEG SEGMENT STACK\n";
		sseg += "	DB 256 DUP(\"pila\")\n";
		sseg += "SSEG ENDS\n\n";

		dseg = "DSEG SEGMENT\n";
			agregarDeclaracionesGlobales();
		dseg += "DSEG ENDS\n\n";

		cseg = 	"CSEG SEGMENT \'CODE\'\n";
		cseg += "	ASSUME	CS:CSEG,	SS:SSEG, 	DS:DSEG\n\n";
			agregarProcedimientos();
		cseg += "CSEG ENDS\n\n";

		nuevo_codigo += sseg + dseg + cseg;
		nuevo_codigo += "\nend main\n";

		codigo = nuevo_codigo;


		//aki el codigo ya esta con mnemonicos mios... imprimirlos pa ke se vea bonito
		debug.imprimirLinea("\n\n----------------------");
		debug.imprimirLinea(" CODIGO CON MNEMONICOS MIOS");
		debug.imprimirLinea("----------------------");


		String lineas [] = codigo.split("\n");
		for(int a = 0; a< lineas.length; a++)
			debug.imprimirLinea(lineas[a]);

		debug.imprimirLinea("");

		convertirMnemonicosFinales();

	return 0;
	}






	void convertirMnemonicosFinales(){
		String lineas [] = codigo.split("\n");

		codigo = "";
		for(int a = 0; a< lineas.length; a++)
		{
			if( lineas[a].indexOf("empujar") != -1 )
			{
				lineas[a] = lineas[a].substring( 9 );
				lineas[a] = "\n		push "+lineas[a];
			}


			if( lineas[a].indexOf("asigna") != -1 )
			{
				String f  = lineas[a].substring( 8 );
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		mov "+f+", ax\n";
			}


			if( lineas[a].indexOf("SUMA") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		pop bx\n";
				lineas[a] += "		add ax, bx\n";
				lineas[a] += "		push ax\n";
			}


			if( lineas[a].indexOf("RESTA") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		pop bx\n";
				lineas[a] += "		sub bx, ax\n";
				lineas[a] += "		push bx\n";
			}

			if( lineas[a].indexOf("MUL") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		pop bx\n";
				lineas[a] += "		mul bx\n";
				lineas[a] += "		push ax\n";
			}


			if( lineas[a].indexOf("retornar") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		mov _p_retorno, ax\n";
				lineas[a] += "		popa\n";
				lineas[a] += "		push _p_retorno\n";
				lineas[a] += "		push cx\n";
				lineas[a] += "		ret\n";
			}

			if( lineas[a].indexOf("MAYOR") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		pop bx\n";
				lineas[a] += "		cmp bx, ax\n";
				lineas[a] += "		je while_fin\n";
				lineas[a] += "		jl while_fin\n";
			}


			if( lineas[a].indexOf("MENOR") != -1 )
			{
				lineas[a] = "\n		pop ax\n";
				lineas[a] += "		pop bx\n";
				lineas[a] += "		cmp bx, ax\n";
				lineas[a] += "		je while_fin\n";
				lineas[a] += "		jg while_fin\n";
			}



			if( lineas[a].indexOf("while_fin:") != -1 )
			{
							lineas[a] = "\n		jmp while_cond\n";
							lineas[a] += "		while_fin:\n";

			}

		//agregar la linea ya modificada al codigo final
		codigo += lineas[a]+"\n";

		}//For de kada linea

	}//metodo












	void agregarDeclaracionesGlobales(){
		String tokens[] = codigo.split("\n");

		dseg += "	_p_retorno dw ?\n";

		for(int a = 0; a<tokens.length; a++)
		{

			if(tokens[a].startsWith("<declaracion global"))
			{
				String s [] = tokens[a].split(" ");
				dseg += "	"+s[3].substring(3, s[3].length()-1)+"	dw	?\n";
			}


			if(tokens[a].startsWith("<declaracion tipo:"))
			{
				String s [] = tokens[a].split(" ");
				dseg += "	"+s[2].substring(3, s[2].length()-1)+"	dw	?\n";
			}


			//<METODO id:hey args:INT a, INT b regresa:INT>

			//Guardar tambien las variables de los argumentos de kada metodo
			if(tokens[a].startsWith("<METODO id:"))
			{
				String args = tokens[a].substring(tokens[a].indexOf(" args:")+6, tokens[a].indexOf(" regresa"));
				if(!args.equals("NADA"))
					{
						String [] nargs = args.split(",");
						for(int z = 0; z<nargs.length; z++)
						{
							String nom = nargs[z].substring(nargs[z].indexOf("INT ")+4);
							dseg += "	"+nom+"	dw	?\n";
						}
					}//if de si hay argumentos
			}//if de si es metodo

		}
	}//declaraciones



	void agregarProcedimientos(){
		String tokens[] = codigo.split("\n");

		for(int a = 0; a<tokens.length; a++)
		{

			if(tokens[a].startsWith("<METODO"))
			{
				int inicio = a+1;

				String s [] = tokens[a].split(" ");
				String nombre = s[1].substring(3);
				cseg += "	"+nombre +" proc";

				//si es el main, entonces ponerle lo ke va de awebo
				if(nombre.equals("main")){
					cseg += " far \n";
					cseg += "		;----MAIN---\n";
					cseg += "		PUSH	DS\n";
					cseg += "		PUSH	0\n";
					cseg += "		MOV	AX, DSEG\n";
					cseg += "		MOV	DS, AX\n\n";
					}
				else
					cseg += "\n";

				//si es el de imprimir tons agregar las cosas ke lleva
				if(nombre.equals("imprimir")) agregarMetodoImprimir();


				//leer variables ke le pasan al metodo desde la pila
				//pop ax y eso

				while( !tokens[++a].equals("</METODO>")) ;

				int fin = a-1;

				if(!nombre.equals("imprimir"))convertirNomenclatura( inicio, fin );

				cseg += "		ret\n";
				cseg += "	"+nombre + " endp\n\n";


			}
		}
	}//procedimientos



	void convertirNomenclatura( int inicio, int fin ){


		String [] tokens = codigo.split("\n");

		//ke sake de la pila la instuccion de regreso y guardarla por ahi
		//a menos ke sea el main
		if( !tokens[inicio-1].equals("<METODO id:main args:NADA regresa:VOID>"))
			{
				cseg += "\n		pop cx\n";

					//sacar de la pila los argumentos ke recibe
					//<METODO id:hey args:INT a, INT b regresa:INT>
					//Guardar tambien las variables de los argumentos de kada metodo
					String args = tokens[inicio-1].substring(tokens[inicio-1].indexOf(" args:")+6, tokens[inicio-1].indexOf(" regresa"));
					if(!args.equals("NADA"))
					{
						String [] nargs = args.split(",");
						for(int z = nargs.length-1; z>=0; z--)
						{
							String nom = nargs[z].substring(nargs[z].indexOf("INT ")+4);
							cseg += "		pop "+nom+"\n";
						}
					}//if de si hay argumentos


				cseg += "		pusha\n";
			}



		for(int a = inicio; a<fin; a++){

			while(tokens[a].equals("<coma>") || tokens[a].startsWith("<declaracion"))
				{
					tokens[a++] = "*";
				}

			//empujar enteros
			if( tokens[a].startsWith("<INT ") )
			{
				String partes [] = tokens[a].split(" ");
				cseg += "		" + "empujar " + partes[1].substring( partes[1].indexOf(":")+1, partes[1].length()-1) + "\n";
				tokens[a] = "*";
			}


			if( tokens[a].startsWith("<while") )
			{

				cseg += "		while_cond:\n";
				tokens[a] = "*";
			}


			if( tokens[a].startsWith("</while") )
			{

				cseg += "		while_body:\n";
				tokens[a] = "*";
			}

			boolean vacio = false;

			if( tokens[a].startsWith("</") )
			{
				tokens[a] = "*";
				while( !tokens[--a].startsWith("<") )
					{ if ( a == inicio ) vacio = true; }

				if(!vacio)
				{
					if( tokens[a].indexOf("llamada tipo:") != -1 )
						tokens[a] = "		call "+ tokens[a].substring( tokens[a].indexOf("id:") + 3, tokens[a].length()-1 );

					if( tokens[a].indexOf("op tipo:") != -1 )
						tokens[a] = "		"+tokens[a].substring( tokens[a].indexOf("tipo:") + 5, tokens[a].length()-1 );


					if( tokens[a].indexOf("<retorno>") != -1 )
						tokens[a] = "		retornar";


					if( tokens[a].indexOf("asignacion tipo:INT id:") != -1 )
						tokens[a] = "		asigna "+tokens[a].substring( tokens[a].indexOf(" id:") + 4, tokens[a].length()-1 )+"\n";


					if( tokens[a].indexOf("<llave") != -1 )
						tokens[a] = "		while_fin: \n";

					cseg += tokens[a] + "\n";

					tokens[a] = "*";
				}//if de metodo vacio
			}

		}//for de kada metodo

		//al akabar, regresar el valor de return a la pila, a menos ke sea el main
		if( !tokens[inicio-1].equals("<METODO id:main args:NADA regresa:VOID>"))
			{
				cseg += "\n		popa\n";
				cseg += "\n		push cx\n";
			}
	}//convertirNomenclatura






	void agregarMetodoImprimir(){

		cseg += " 		pop dx\n";

		cseg += " 		pop ax\n";

		cseg += " 		pusha\n";

		cseg += " 		mov bl,10 	;dividir entre 10\n";
		cseg += " 		mov ah,0 	;\n";
		cseg += " 		div bl		;divide. AL is quotient, AH is Remainder\n";

		cseg += " 		add al,48d 	;adds 48 to generate ASCII character\n";
		cseg += " 		add ah,48d\n";

		cseg += " 		mov cl, ah\n";

		cseg += " 		mov ah,0eh 	;print to screen\n";
		cseg += " 		mov bx,7h\n";
		cseg += " 		int 10h\n";

		cseg += " 		mov ah,0eh 	;print to screen\n";
		cseg += " 		mov al,cl\n";
		cseg += " 		mov bx,7h\n";
		cseg += " 		int 10h\n";


		cseg += " 		mov ah,0eh 	;print to screen\n";
		cseg += " 		mov al,0ah\n";
		cseg += " 		mov bx,7h\n";
		cseg += " 		int 10h\n";

		cseg += " 		popa\n";

		cseg += " 		push dx\n";
	}


}//class ensambler




