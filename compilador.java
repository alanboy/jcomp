/*------------------------------------------------------------------------------------
 			PROGRAMACION DE SISTEMAS : COMPILADOR
-------------------------------------------------------------------------------------*/
import java.io.*;
import java.util.*;













/*------------------------------------------------------------------------------------
 				DEBUGGER
-------------------------------------------------------------------------------------*/
class Debugger{

PrintWriter pw;

	Debugger (){
		try{
		pw = new PrintWriter(new BufferedWriter(new FileWriter("salida.txt")));
		}catch(IOException ioe){
		System.out.println("error escribiendo archivo de debugger");
		}
	}

	void imprimir(String s){
		pw.print(s);
	}


	void imprimirLinea(String s){
		pw.println(s);
	}

	void closeFile(){
		pw.close();
	}
}//class Debugger























/*------------------------------------------------------------------------------------
 				ENSAMBLADOR
-------------------------------------------------------------------------------------*/
class Ensambler{

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


















/*------------------------------------------------------------------------------------
 				MAIN
-------------------------------------------------------------------------------------*/
class compilador{

	public static void main(String [] args){
			iniciar(args[0]);
	}


	static int iniciar(String file){

		String codigo;

		analisis_lexico a_lex = new analisis_lexico();
		analisis_sintactico a_sin = new analisis_sintactico();
		analisis_semantico a_sem = new analisis_semantico();
		Debugger debug = new Debugger();

		a_lex.setDebugger(debug);
		a_lex.setCodigo(file);
		if(a_lex.iniciar() != 0) {  debug.closeFile(); return 1; }

		codigo = a_lex.getCodigo();

		a_sin.setDebugger(debug);
		a_sin.setCodigo(codigo);
		if(a_sin.iniciar() != 0)  { debug.closeFile(); return 1; }
		codigo = a_sin.getCodigo();




		a_sem.setDebugger(debug);
		a_sem.setCodigo(codigo);
		if(a_sem.iniciar() != 0) {debug.closeFile(); return 1; }
		codigo = a_sem.getCodigo();


		//aki se termina el analisis del codigo....
		//ahora a ensamblar

		debug.imprimirLinea("");
		debug.imprimirLinea("");
		debug.imprimirLinea("----------------------");
		debug.imprimirLinea("CODIGO OBJETO !!");
		debug.imprimirLinea("----------------------");

		debug.imprimirLinea(codigo);


		Ensambler en = new Ensambler();
		en.setCodigo(codigo);
		en.setDebugger(debug);
		if(en.iniciar() != 0) {debug.closeFile(); return 1; }
		codigo = en.getCodigo();


		debug.imprimirLinea("");
		debug.imprimirLinea("");
		debug.imprimirLinea("----------------------");
		debug.imprimirLinea("CODIGO PARA ENSAMBLAR");
		debug.imprimirLinea("----------------------");

		debug.imprimirLinea(codigo);


		debug.imprimirLinea("");
		debug.imprimirLinea("");
		debug.imprimirLinea("----------------------");
		debug.imprimirLinea("	ENSAMBLADO");
		debug.imprimirLinea("----------------------");
		//listo ahora a ensamblar

		debug.imprimir("CREANDO .ASM ...");

		try{
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("p.asm")));
			pw.print(codigo);
			pw.close();

		}catch(IOException ioe){
			System.out.println("error creando archivo asm");

		}

		debug.imprimirLinea("OK");
		debug.imprimirLinea("");
		debug.imprimirLinea("ENSAMBLANDO ...");


        try{

			String cmd = "cmd.exe /C tasm /z /ml p.asm";

			Process proc = Runtime.getRuntime().exec(cmd);

  	        try
	        {
	            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null)
	            	{
					debug.imprimirLinea(line);

						if(line.indexOf("Error messages:") != -1)
							{
								System.out.println(line);
							}

					}
	        } catch (IOException ioe)     { ioe.printStackTrace(); }

        	int exitVal = proc.waitFor();

        } catch (Throwable t){ t.printStackTrace(); }


		debug.imprimirLinea("");
		debug.imprimirLinea("LINKEANDO ...");
        try{

			String cmd = "cmd.exe /C tlink p.obj";

			Process proc = Runtime.getRuntime().exec(cmd);

  	        try
	        {
	            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null)
	            	{
					debug.imprimirLinea(line);
					}
	        } catch (IOException ioe)     { ioe.printStackTrace(); }

        	int exitVal = proc.waitFor();

        } catch (Throwable t){ t.printStackTrace(); }


		debug.imprimirLinea("");
		debug.imprimirLinea("---------------------------COMPILACION COMPLETA !!-------------");

		debug.closeFile();


		//aweboooo PUTA MADREEEE
		//despues de muchas semanas...
		//la primera ejecucion de un exe el lunes 15 de diciembre del 2008 a las 12:19am


		System.out.println("Compilacion completa.");
	return 0;
	}
}//main


class Help{

	private static void header(  ){
		System.out.print("\n---------------------------------------\n");
		System.out.print("    Your program contained errors. ");
		System.out.print("\n---------------------------------------\n\n");


			
	}

	public static void notInAlphabet(char c, int line){

		header();
		showLine(line);
		System.out.println("The source file contains an unexpected ASCII character. To resolve the error, remove the character. Remeber there are invisible characters out there that your editor might not be showing. The character ASCII number is: " + (int)c +". Another thing you have to take into account is text encodig, read <this> for more info."  );
			
	}

	public static void unfinishedString(int line){
		header();
		showLine(line);
		System.out.println("You started a string but never closed it. A string constant cannot be continued on a second line unless you do the following: \n\tEnd the first line with a backslash. \n\tClose the string on the first line with a double quotation mark and open the string on the next line with another double quotation mark.\n\nEnding the first line with \\n is not sufficient");
	
	}


	public static void returnFromVoid(String m, int line){
		header();
		showLine(line);
		
		System.out.println("In method "+m+", you are returning something from a void function. If a function does not return a value, then a special \"TYPE\" is used to tell the computer this. The return type is \"void\" (all lower case). In this case the function is declared as void but returns a value.");	
	
	}
		
	public static void addingDifferentTypes(String op, String t1, String t2, int linea){
		header();	
		showLine(linea);

		System.out.println( "You are mixing " +t1 + " and " + t2 + ". They are diferent types. \n" );

		System.out.println("\nMost generally, \"strong typing\" implies that the programming language places severe restrictions on the intermixing that is permitted to occur, preventing the compiling or running of source code which uses data in what is considered to be an invalid way. For instance, an addition operation may not allow an integer to be added to a string value; a procedure which operates upon linked lists may not be used upon numbers. However, the nature and strength of these restrictions is highly variable.");	
	}


	public static void usingBeforDef(String var, int linea){
		header();
		showLine(linea);
		System.out.println("You are trying to use variable "+ var + " which you have not declared before. Remeber you have to declare variables before using them.");
			
	
	}

	public static void showLine(int line){

		int cl = line;
		
		try{
			BufferedReader br = new BufferedReader(new FileReader("pf.txt"));
		

			System.out.println("Here at line "+line+": ");	

			while( cl-- > 2 ){
				///System.out.printf(
				 br.readLine( ) 
				//)
				; 
			}
			line -= 1;
			for(int a = 3; a-- != 0 ; ){
				if(a == 1){
					System.out.println( ">>>" +  line + " : " + br.readLine() );
				}else{
					System.out.println( "   " +  line + " : " + br.readLine() );
				}
				
				line++;
			}

			System.out.println("\n");	
			
			br.close();

		}catch(Exception e){
			System.out.printf("Trouble showing the file.");
			return;
		
		}
		
	}

}
