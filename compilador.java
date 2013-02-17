/*------------------------------------------------------------------------------------
 			PROGRAMACION DE SISTEMAS : COMPILADOR
-------------------------------------------------------------------------------------*/
import java.io.*;
import java.util.*;


/*------------------------------------------------------------------------------------
 				ANALISIS LEXICO
-------------------------------------------------------------------------------------*/
class analisis_lexico{

	String [][] TOKENS;
	char [] ALFABETO;
	String FILE_NAME;
	String PROGRAMA_FUENTE; //codigo final....
	Debugger debug;

	analisis_lexico(){
	}

	void setDebugger(Debugger debug){
		this.debug = debug;
	}

	void setCodigo(String FILE_NAME){
		//archivo a analizar
		this.FILE_NAME = FILE_NAME;
	}


	int iniciar(){
		//todos los metodos ke manipulan el codigo fuente,
		// lo leen de CODIGO_FUENTE y lo guardan manipulado
		// en CODIGO_FUENTE tambien

		//cargar archivos necesarios para analizar
		if(cargarConfiguracion() != 0) return 1;


		/*		funciones del lenguaje ke implicitas */
		// tenemos ke agregarlas aki tal vez....


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


	String getCodigo(){
		return PROGRAMA_FUENTE;
	}



	int cargarConfiguracion(){

			//cargar los tokens en un arreglo
			TOKENS = new String[][]{
				{"(","PARENTESIS_ABRE"},
				{")","PARENTESIS_CIERRA"},
				{"{","LLAVE_ABRE"},
				{"}","LLAVE_CIERRA"},
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


			//listo aki, ya ALFABETO[] es un vector con todos los caracteres


		//cargar el archivo fuente
		//guardarlo tal  y como es en PROGRAMA_FUENTE
		try{
			BufferedReader br = new BufferedReader(new FileReader(FILE_NAME));
			PROGRAMA_FUENTE = "";
			String k = "";
			while( (k = br.readLine()) != null ) PROGRAMA_FUENTE += (k+"\n");

		}catch(Exception e){
			//mandar el error de archivo en forma de texto
			System.out.println( "No he podido leer el archivo de entrada.");
			return 1;
		}

	return 0;
	}//fin metodo cargarConfiguracion()





	int eliminarComentarios(){

		String pf="";
		String [] result = PROGRAMA_FUENTE.split("\n");

		for (int x=0; x<result.length; x++)
		{
         	String linea = result[x];
		int d = linea.indexOf("//");

		if( d != -1) linea = linea.substring( 0, d );
		pf += (linea + "\n");
		}

	PROGRAMA_FUENTE = pf;
	return 0;
	}




	int verificarAlfabeto(){
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
			//System.out.println("He encontrado un caracter que no pertenece al alfabeto.");
			Help.notInAlphabet( c, linea );
			//Help.showLine((linea));
			//system.out.println("Linea: " + linea);
			//System.out.println("Caracter: [" + c +"]");
			return 1;
			}

		if(c == '\n')linea ++;

		}
	return 0;
	}//metodo verificarAlfabeto









	int limpiarCodigo(){
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
			while(cambio == true)
			{
				cambio = false;
				int a = linea.indexOf("  ");
				if( a != -1 ){ linea.replace(a,a+2, " "); cambio = true; }

				int b = linea.indexOf(String.valueOf((char)9));
				if( b != -1 ){ linea.setCharAt(b, ' '); cambio = true; }
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





	int tokenize(){
		//ya tenemos ke analizar kuales seran tokens

		String numero_linea;//el caracter 175 separa el numero de linea de la instruccion
		String linea;
		String pf="";	//aki se guarda el resultado intermedio de este pedo
		StringTokenizer separacion_linea;


		String lineas [] = PROGRAMA_FUENTE.split("\n");

		for(int a=0; a<lineas.length; a++)
		{
			separacion_linea = new StringTokenizer( lineas[a], String.valueOf((char)175) );

			//variables perras de cada linea, su numero y contenido
			try{
				numero_linea = separacion_linea.nextToken();
			}catch(Exception nsee){ 
				System.out.println("Archivo vacio");
			       	return 1; 
			}
			linea = separacion_linea.nextToken();

			//aki ya analizo linea por linea
			//el contenido de la linea actual
			//esta en el string linea
			//La linea se procesa y se concatena al string pf
			StringTokenizer st = new StringTokenizer(linea, "*/;{}#()[]<>!=+-\", ", true);

			//agregar en el programa fuente nuevo el numero de linea
			pf += "NUMERO_LINEA_"+numero_linea+"\n";


			String token="";
			boolean espacio = false;
			boolean cadena = false;

			while( st.hasMoreTokens() ){

				cadena = false;
				String s = st.nextToken();

				//Chekar si estoy analizando una cadena
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
						Help.unfinishedString ( Integer.parseInt( numero_linea ) );
						//System.out.println("Error, no encontre el fin de la cadena.");
						//System.out.println("Linea: "+numero_linea);
					return 1;
					}
				}


				//chekar si es un white_space
				if((s.length() == 1) && (s.charAt(0)==32))
					espacio = true; else espacio = false;

				//si no es un espacio tons agregar este nuevo token al codigo fuente
				if( !espacio )pf += s + "\n";

			}//while de cada token

		}//for de cada linea

		//aki el programa fuente ya tiene los tokens en cada linea
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
				try{
					int num = Integer.parseInt( token );
					pf += "VALOR_NUMERO_" + lineas[a] + "\n";
					yaloencontre = true;
				}catch(Exception e){}


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
					System.out.println("aki: "+ token);
					//TO DO: ke muestre la linea complera
					return 1;
				}
			}//if de si no es una palabra reservada
		}//For de cada linea

		pf += "FINAL\n";
		PROGRAMA_FUENTE = pf;

	return 0;
	}

}//fin de clase alanlisis_lexico







/*------------------------------------------------------------------------------------
 				ANALISIS SINTACTICO
-------------------------------------------------------------------------------------*/
class analisis_sintactico{

String codigo_fuente;
Debugger debug;

	analisis_sintactico(){
	}


	void setDebugger(Debugger debug){
		this.debug = debug;
	}

	void setCodigo(String cf){
		codigo_fuente = cf;
	}

	String getCodigo(){
		return codigo_fuente;
	}


	int iniciar(){

		debug.imprimirLinea( " " );
		debug.imprimirLinea( "------------------------------" );
		debug.imprimirLinea( "ANALISIS SINTACTICO:" );
		debug.imprimirLinea( "------------------------------" );


		//primero chekar ke este bien, ya despues ornaizar el codigo para el semantico

		String s [] = codigo_fuente.split("\n");
		String cf = "";

		//pegar el codigo en una sola linea
		//y ya ke para el analizis sintactico
		//solo necesitamos saber ke es un id, o un numero
		//y no el numero en si, ps cambiarle este pedo

		for(int a=0; a<s.length; a++)
		{
		if(s[a].startsWith("IDENTIFICADOR_")) s[a]="<id>";
		if(s[a].startsWith("VALOR_NUMERO_")) s[a]="NUM";
		if(s[a].startsWith("TIPO_")) s[a]="TIPO";
		if(s[a].startsWith("STRING_")) s[a]="STRING";
		cf += s[a]+" ";
		}




		//crear las producciones para la gramatica
		//y despues se las pasare al automata
		Produccion [] prod =	{

		new Produccion("<PROGRAMA>","$ <instruccion_basica> FINAL"), //awebo...!!! 4:19 am pero salio

		new Produccion("<instruccion_basica>","<metodo_def>"),
		new Produccion("<instruccion_basica>","<def_global>"),
		new Produccion("<instruccion_basica>","<instruccion_basica> <instruccion_basica>"),

		new Produccion("<def_global>", "CONTROL_DEF <variable_declarator>"),
		//new Produccion("<def_global>", "CONTROL_DEF <variable_declaration>"),


		new Produccion("<casi_metodo_def>", "CONTROL_DEF TIPO <casi_llamada>"),
		new Produccion("<metodo_def>", "<casi_metodo_def> PARENTESIS_CIERRA <statement_block>"),
		new Produccion("<metodo_def>", "<casi_metodo_def> TIPO <id> PARENTESIS_CIERRA <statement_block>"),
		new Produccion("<metodo_def>", "<casi_metodo_def> <decl_p_met> PARENTESIS_CIERRA <statement_block>"),

		new Produccion("<decl_p_met>", "TIPO <id> PUNTUACION_COMA TIPO <id>"),
		new Produccion("<decl_p_met>", "<decl_p_met> PUNTUACION_COMA TIPO <id>"),


		new Produccion("<casi_if>", "CONTROL_IF PARENTESIS_ABRE"),
		new Produccion("<error_1>", "CONTROL_IF <id>"), //////////////////////////////////////
		new Produccion("<error_1>", "CONTROL_IF <op_bool>"),
		new Produccion("<if>", "<casi_if> <expression_booleana> PARENTESIS_CIERRA <statement_block>"),


		new Produccion("<casi_while>", "CONTROL_WHILE PARENTESIS_ABRE"),
		new Produccion("<while>", "<casi_while> <expression_booleana> PARENTESIS_CIERRA <statement_block>"),

		new Produccion("<expression_booleana>", "<id> <op_bool> <id>"),
		new Produccion("<expression_booleana>", "<id> <op_bool> <expression>"),
		new Produccion("<expression_booleana>", "<id> <op_bool> <expression_booleana>"),
		new Produccion("<expression_booleana>", "<expression_boleana> <op_bool> <id>"),
		new Produccion("<expression_booleana>", "<expression> <op_bool> <expression_booleana>"),
		new Produccion("<expression_booleana>", "<expression_boleana> <op_bool> <expression>"),
		new Produccion("<expression_booleana>", "<expression> <op_bool> <id>"),
		new Produccion("<expression_booleana>", "<expression> <op_bool> <expression>"),
		new Produccion("<expression_booleana>", "PARENTESIS_ABRE <expression_booleana> PARENTESIS_CIERRA"),
		new Produccion("<expression_booleana>", "<expression_booleana> <op_bool> <expression_booleana>"),




//nuevasss----------------//nuevasss----------------//nuevasss----------------//nuevasss----------------
		new Produccion("<expression_booleana>", "<llamada> <op_bool> <expression>"),
		new Produccion("<expression_booleana>", "<llamada> <op_bool> <llamada>"),
		new Produccion("<expression_booleana>", "<expression> <op_bool> <llamada>"),
		new Produccion("<expression_booleana>", "<expression_booleana> <op> <expression>"),
		new Produccion("<expression_booleana>", "<expression> <op> <expression_booleana>"),
		new Produccion("<expression_booleana>", "<id> <op_bool> <llamada>"),
		new Produccion("<expression_booleana>", "<llamada> <op_bool> <id>"),
		new Produccion("<expression_booleana>", "<expression_booleana> <op> <id>"),
		new Produccion("<expression_booleana>", "<id> <op> <expression_booleana>"),
		new Produccion("<id>", "PARENTESIS_ABRE <id> PARENTESIS_CIERRA"),
//nuevasss----------------//nuevasss----------------//nuevasss----------------//nuevasss----------------




		new Produccion("<op_bool>", "BOL_MENOR_QUE"),
		new Produccion("<op_bool>", "BOL_MAYOR_QUE"),
		new Produccion("<op_bool>", "BOL_MENOR_QUE ASIGNA"),//aki falta manosear
		new Produccion("<op_bool>", "BOL_MAYOR_QUE ASIGNA"),
		new Produccion("<op_bool>", "ASIGNA ASIGNA"),

		new Produccion("<asignacion>", "<id> ASIGNA <expression> PUNTUACION_PUNTO_COMA"),
		new Produccion("<asignacion>", "<id> ASIGNA <id> PUNTUACION_PUNTO_COMA"),
		new Produccion("<asignacion>", "<id> ASIGNA <llamada> PUNTUACION_PUNTO_COMA"), //--------

//		new Produccion("<variable_declaration>", "TIPO <asignacion>"),

		//new Produccion("<variable_declarator>", "TIPO CORCHETE_ABRE CORCHETE_CIERRA <id> PUNTUACION_PUNTO_COMA"),
		new Produccion("<variable_declarator>", "TIPO <id> PUNTUACION_PUNTO_COMA"),





		new Produccion("<statement_block>", "LLAVE_ABRE <statement> LLAVE_CIERRA"),
		new Produccion("<statement_block>", "LLAVE_ABRE LLAVE_CIERRA"),

		//new Produccion("<statement>", "<expression> PUNTUACION_PUNTO_COMA"),
		new Produccion("<statement>", "<statement> <statement>"),
		new Produccion("<statement>", "<variable_declaration>"),
		new Produccion("<statement>", "<variable_declarator>"),
		new Produccion("<statement>", "<asignacion>"),
		new Produccion("<statement>", "<if>"),
		new Produccion("<statement>", "<while>"),
		new Produccion("<statement>", "<retorno>"),

		new Produccion("<retorno>", "CONTROL_RETORNO <id> PUNTUACION_PUNTO_COMA"),
		new Produccion("<retorno>", "CONTROL_RETORNO <expression> PUNTUACION_PUNTO_COMA"),
		new Produccion("<retorno>", "CONTROL_RETORNO <llamada> PUNTUACION_PUNTO_COMA"),

		new Produccion("<expression>", "<literal_expression>"),
		//new Produccion("<expression>", "<llamada>"),

		new Produccion("<expression>", "<expression> <op> <expression>"),
		new Produccion("<expression>", "<id> <op> <expression>"),
		new Produccion("<expression>", "<expression> <op> <id>"),
		new Produccion("<expression>", "<id> <op> <id>"),

		new Produccion("<expression>", "<literal_expression>"),
		new Produccion("<expression>", "PARENTESIS_ABRE <expression> PARENTESIS_CIERRA"),


		new Produccion("<ARGS>", "<expression> PUNTUACION_COMA <expression>"),
		new Produccion("<ARGS>", "<expression> PUNTUACION_COMA <id>"),
		new Produccion("<ARGS>", "<id> PUNTUACION_COMA <expression>"),
		new Produccion("<ARGS>", "<id> PUNTUACION_COMA <id>"),
		new Produccion("<ARGS>", "<ARGS> PUNTUACION_COMA <expression>"),
		new Produccion("<ARGS>", "<ARGS> PUNTUACION_COMA <id>"),
		new Produccion("<ARGS>", "<ARGS> <op> <expression>"),
		new Produccion("<ARGS>", "<ARGS> <op> <id>"),


		new Produccion("<casi_llamada>", "PUNTUACION_GATO <id> PARENTESIS_ABRE"),
		new Produccion("<llamada>", "<casi_llamada> <expression> PARENTESIS_CIERRA"),
		new Produccion("<llamada>", "<casi_llamada> <id> PARENTESIS_CIERRA"),
		new Produccion("<llamada>", "<casi_llamada> PARENTESIS_CIERRA"),
		new Produccion("<llamada>", "<casi_llamada> <ARGS> PARENTESIS_CIERRA"),

		new Produccion("LLAVE_ABRE <statement>", "LLAVE_ABRE <llamada> PUNTUACION_PUNTO_COMA"),
		new Produccion("<statement>", "<statement> <llamada> PUNTUACION_PUNTO_COMA"),

		new Produccion("<op> <expression>", "<op> <llamada>"),
		new Produccion("<expression> <op>", "<llamada> <op>"),
		new Produccion("PUNTUACION_COMA <expression>", "PUNTUACION_COMA <llamada>"),
		new Produccion("<expression> PUNTUACION_COMA", "<llamada> PUNTUACION_COMA"),
		new Produccion("PARENTESIS_ABRE <expression>", "PARENTESIS_ABRE <llamada>"),
		new Produccion("<expression> PARENTESIS_CIERRA", "<llamada> PARENTESIS_CIERRA"),


		new Produccion("<op>", "OP_SUMA"),
		new Produccion("<op>", "OP_RESTA"),
		new Produccion("<op>", "OP_MULTIPLICACION"),
		new Produccion("<op>", "OP_DIVISION"),

		new Produccion("<literal_expression>", "NUM"),
		new Produccion("<literal_expression>", "STRING")

		};



		Automata aut = new Automata(prod, cf);
		aut.setDebugger(debug);
		String resultado = aut.iniciar();

		debug.imprimirLinea( resultado );

		if( !resultado.endsWith("<PROGRAMA> "))
			{
			System.out.println( "Error de Syntaxis." );
			return 1;
			}

		return 0;
	}//iniciar()

}//clase






/*------------------------------------------------------------------------------------
 				ANALISIS SEMANTICO
-------------------------------------------------------------------------------------*/
class analisis_semantico{

String codigo;
Debugger debug;
Variables [] vars;
Metodos [] metodos;


	analisis_semantico(){
	}

	void setDebugger(Debugger debug){
		this.debug = debug;
	}

	void setCodigo(String c){
		codigo = c;
	}

	String getCodigo(){
		return codigo;
	}

	int iniciar(){
		debug.imprimirLinea( "" );
		debug.imprimirLinea( "------------------------------" );
		debug.imprimirLinea( "      ANALISIS SEMANTICO" );
		debug.imprimirLinea( "------------------------------" );


		/*
		AWEBOOO SE ME PRENDIO EL FOCO A LA 1:32 AM
		utilizar xml para las llamadas dentro de las llamadas, es decir
		#met1( 43, #met2( a ) , t)
		se convierte en:
		<llamada met1> 43 , <llamada met2> a </llamada> , t</llamada>

		y asi se forma el arbol.... awebo

		YA SE KE EL ARBOL SE DEBERIA DE HACER EN EL SINTACTICO PERO
		ES MAS FACIL HACERLO EN EL SEMANTICO PARA AL MISMO TIEMPO IR CHECANDO ERRORES
		*/


		//primro convertimos los tokens ke me pasa el sintactico en
		//objetos, estos son Metodos y Variables
		crearObjetos();



		//primero revisaremos ke no se repitan los nombres de los metodos ni vars
		if(revisarDefiniciones() != 0) return 1;

		//revisar las declaraciones del cuerpo ke no sean iwuales
		if(revisarCuerpoVariables() != 0) return 1;

		//bueno, TODAS las declaraciones, han sido comprobadas, y se
		//han creado nuevos tokens

		//ahora...

		//convertir las asignaciones en la nueva nomenclatura xml
		if(convertirAsignaciones() != 0) return 1;

		//revisar ke las llamada a metodos existan...
		//osea ke exista una definicion para la llamada a ese metodo
		if(revisarExistenciaMetodos() != 0) return 1;

		//convertir llamdas a la nueva nomenclatura
		if(convertirLLamadas() != 0) return 1;

		//verificar ke existan las variables ke se estan usando
		//y ke se declaren antes de ser usadas
		if(revisarExistenciaVariables() != 0) return 1;


		//convertir el token IDENTIFICADOR_variable1
		//en <var-variable1>
		if(convertirVariables() != 0)return 1;

		//revisar ke las llamadas tengan el numero exacto de argumentos
		//ke la definicion del metodo recibe
		if(revisarArgumentosDeLLamadas() != 0) return 1;

		if(revisarReturn() != 0) return 1;

		//arreglar todo pa ke se vea bonito y simetrico
		if(arreglar() != 0) return 1;

		// wenooooo ahora ya ...
		// continuar construllendo el arbol...--------

		// para ahorrarme un for dentro de cada metodo..
		for(int f=0; f<metodos.length; f++)
		{
			//convertir las operaciones en la nueva nomenclatura
			metodos[f].setCuerpo( convertirOP( metodos[f].getCuerpo() ) );
			//no mames, mucho mas perro de lo ke imagine, pero salio

			//ahora eliminar los parentesis ke ya de nada sirven
			//ya todo esta chingon en el arbol 12:50am
			metodos[f].setCuerpo( eliminarParentesis( metodos[f].getCuerpo() ) );

			//ke los metodos tengan return
			if( revisarRetorno(metodos[f].getCuerpo(), f) != 0 ) return 1;

			//okay, ahora de este arbol hacer un analisis dimensional
			if( analisisDimensional(metodos[f].getCuerpo(), f) != 0 ) return 1;
		}


		//buscar un main
		if(buscarMain() != 0) return 1;


		imprimirObjetos();


		//una ultima cosa.... cambiar el nombre de las variables
		//... agregarles el nombre del metodo, para ke si se
		// ddeclaran variables con el mismo nombre en distintos metodos,
		//estas sean consideradas diferentes
		nombresDeVariables();



		//	LISTO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//      AHORA SOLO HAY KE
		//	GENERAR EL CODIGO INTERMEDIO, KE HAY KE PASAR A ENSAMBLADOR todavia :(
		generarCodigo();
		//	DESPUES DE ESTO... SE TERMINA EL ANALISIS DE CODIGO
		//	este metodo, pasa los objetos en una cadena de texto, y la guarda
		//	en la variable codigo... la cual se puede obtener con un get de esta
		//	clase

	return 0;
	}//inicio





	void nombresDeVariables(){
		for(int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split("\n");
			String linea = metodos[a].getLinea();

			String cuerpo = "";

			for(int b=0; b<token.length; b++)
			{


				if( token[b].indexOf( " id:" ) != -1 )
				{
				String s1 = token[b].substring(0, token[b].indexOf(" id:")+4);
				String s2 = token[b].substring( token[b].indexOf(" id:")+4 );
				token[b] = s1+"_"+metodos[a].getNombre().substring(14)+"_"+s2;
				}

				/*System.out.println("->"+token[b]+"<-");
				if( token[b].indexOf( "args:NADA" ) == -1 ) { 
					System.out.println("*");  
					if(token[b].startsWith("<METODO") )
						System.out.println("-"); 
				}*/


				if( (token[b].indexOf( "args:NADA" ) == -1 )&&(token[b].startsWith("<METODO") ))
				{
				//System.out.println(token[b]);
				String s1 = token[b].substring(0, token[b].indexOf(" args:")+6);
				String s2 = token[b].substring( token[b].indexOf(" regresa:") );

				String [] argus = token[b].substring(token[b].indexOf(" args:")+6, token[b].indexOf(" regresa:")).split(" ");
					for(int j = 1; j<argus.length; j = j+2)
					{
					argus[j] = "_"+metodos[a].getNombre().substring(14) + "_"+argus[j];
					}

					String argust = s1;
					for(int u=0; u<argus.length; u++){
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





	int revisarRetorno(String body, int f){

		boolean regresa = true;
		if(metodos[f].getTipoDeRetorno().equals("TIPO_VOID")) regresa=false;

		if( body.indexOf("<retorno") != -1 )
		{
			int u = body.indexOf("<retorno");

			String uu = body.substring( body.indexOf("<retorno linea:")+15, body.indexOf("<retorno linea:")+19);
			uu = uu.substring( 0, uu.indexOf(">"));



			//si hay return y no deberia
			if(!regresa){

				//System.out.println(" es TIPO_VOID.");
				//System.out.println("Imposible regresar algo de un metodo que es void.");
				Help.returnFromVoid( metodos[f].getNombre().substring(14), Integer.parseInt(uu) );
				//System.out.println("Linea: "+uu);
				//System.out.print("Metodo "+metodos[f].getNombre().substring(14));
				return 1;
			}

		}
		else
		{
			//si no hay return y deberia
			if(regresa){
				System.out.println("Linea: " + metodos[f].getLinea().substring(13, metodos[f].getLinea().length()-1) );
				System.out.print("Metodo "+metodos[f].getNombre().substring(14));
				System.out.println(" debe regresar alguna expresion.");
				return 1;
			}
		}

	return 0;
	}




	int analisisDimensional(String body, int metodo_){

	String token[]= body.split("\n");

	boolean CAMBIO = true;
	while(CAMBIO)
	{
	CAMBIO=false;

		//primero simplificar lo basico

		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");


		for(int a=0; a<token.length; a++)
		{
			if(token[a].startsWith("<declaracion tipo:VOID ") )
			{
			String linea = token[a].substring( token[a].indexOf("linea:")+6, token[a].length()-1);
			System.out.println("Linea: "+linea);
			System.out.println("Declaracion no valida.");
			return 1;
			}
		}



		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		boolean cambio = true;

		while(cambio)
		{
		cambio = false;
			for(int a=0; a<token.length; a++)
			{
				if(token[a].startsWith("<llamada") && token[a+1].equals("</llamada>"))
				{
				token[a] = "<"+token[a].substring( token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" id:"))+">";
				token[a+1] = "";
				cambio = true;
				CAMBIO = true;
				}
			}
		}


		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		cambio = true;

		while(cambio)
		{
		cambio = false;
			for(int a=0; a<token.length; a++)
			{
				if(token[a].startsWith("<INT ") )
					{ token[a] = "<INT>"; cambio = true; CAMBIO = true;}

				if(token[a].startsWith("<STRING ") )
					{ token[a] = "<STRING>"; cambio = true; CAMBIO = true;}
			}
		}


		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		cambio = true;

		while(cambio)
		{
		cambio = false;
			for(int a=0; a<token.length; a++)
			{
				if(token[a].startsWith("<llamada"))
				{
				int b=a+1;
				boolean not_good = false;

				while(!token[b].equals("</llamada>"))
				{
				if(token[b].startsWith("<op"))not_good = true;
				if(token[b].startsWith("<llamada"))not_good = true;
				b++;
				}

				if(!not_good)
				{


				String texto = "";
				for(int i=a; i<=b; i++)
					if(!token[i].equals(""))
						texto += token[i]+"%";


				//revisar esta llamada a metodo para ver si los tipos coniciden///////////////////////////////////////////////////////////////////////////// aki reviso algo importante, es mas facil llamar al metodo desde aki
				if( revisarTipoArgumentos( texto ) != 0)return 1;

				token[a] = "<"+token[a].substring( token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" id:"))+">";
				for(int i=a+1; i<=b; i++) token[i] = "";
				cambio=true;
				CAMBIO = true;
				}

				}
			}
		}


		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		cambio = true;



		while(cambio)
		{
		cambio = false;
			for(int a=0; a<token.length; a++)
			{
				if(token[a].startsWith("<op "))
				{
				int b=a+1;
				boolean not_good = false;

				while(!token[b].equals("</op>"))
				{
				if(token[b].startsWith("<op"))not_good = true;
				if(token[b].startsWith("<llamada"))not_good = true;
				b++;
				}

				if(!not_good)
				{
				String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
				String operacion = token[a].substring(token[a].indexOf("tipo:")+5, token[a].lastIndexOf(" "));

					if((!token[a+1].equals( token[a+2])) || ( token[a+1].equals( "<STRING>") || token[a+2].equals( "<STRING>")))
					{


					System.out.print("Operator "+operacion+" ");
					//System.out.println("can not be applied to: "+token[a+1]+" and  "+token[a+2]);
					Help.addingDifferentTypes( operacion, token[a+1], token[a+2], Integer.parseInt( linea ) );

					//System.out.println("Linea: "+linea);


					return 1;
					}

				token[a]=token[a+1];
				for(int i=a+1; i<=b; i++) token[i] = "";

				cambio=true;
				CAMBIO = true;
				}

				}
			}
		}

		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		cambio = true;

		while(cambio)
		{
		cambio = false;
			for(int a=0; a<token.length; a++)
			{
				if(token[a].startsWith("<asignacion ") )
				{
				int b=a+1;
				boolean not_good = false;

				while(!token[b].equals("</asignacion>"))
				{
				if(token[b].startsWith("<op"))not_good = true;
				if(token[b].startsWith("<llamada"))not_good = true;
				b++;
				}

				if(!not_good)
				{
				String linea = token[a].substring(token[a].indexOf("linea:")+6, token[a].length()-1);
				String recibe = token[a].substring(token[a].indexOf("tipo:")+5, token[a].indexOf(" id:"));
				String _id = token[a].substring(token[a].indexOf("id:")+3, token[a].lastIndexOf(" "));
				recibe = "<" + recibe + ">";

					if(!recibe.equals( token[a+1] ))
					{
					System.out.println("Linea: "+linea);
					System.out.println("Tipos incompatibles.");
					System.out.println("Encontrado:"+token[a+1]);
					System.out.println("Requerido:"+recibe);
					return 1;
					}

				token[a]=recibe;
				for(int i=a+1; i<=b; i++) token[i] = "";

				cambio=true;
				CAMBIO = true;
				}

				}
			}
		}





		body = "";for(int h=0; h<token.length; h++)if(!token[h].equals(""))body += token[h] + "\n";
		token = body.split("\n");

		cambio = true;

		while(cambio)
		{
		cambio = false;
			for(int a=0; a<token.length; a++)
			{
				if(token[a].startsWith("<retorno linea:") && token[a+2].equals("</retorno>"))
				{
				 String tipejo = "TIPO_"+token[a+1].substring(1,token[a+1].length()-1);
				 String ret = metodos[metodo_].getTipoDeRetorno();
					if(!tipejo.equals(ret))
					{
					System.out.print("Linea : ");
					System.out.println(token[a].substring(token[a].indexOf(" linea:")+7, token[a].length()-1));
					System.out.print("Metodo "+metodos[metodo_].getNombre().substring(14));
					System.out.print(" debe regresar " + ret);
					System.out.println(" pero " +tipejo+" encontrado. ");
					return 1;
					}

				}
			}
		}



	}//while super grandote


		body = "";
		for(int h=0; h<token.length; h++)
			if(!token[h].equals(""))
				body += token[h] + "\n";


	return 0;
	}






	int revisarTipoArgumentos(String s){

		String tokens[] = s.split("%");
		//-</llamada tipo:INT id:numeros4 linea:12>
		String id = tokens[0].substring( tokens[0].indexOf(" id:")+4, tokens[0].indexOf(" linea:"));
		String linea = tokens[0].substring( tokens[0].indexOf(" linea:")+7, tokens[0].length()-1);

		int a = tokens[0].length();
		s = s.substring( a +1 , s.length()-12);

		String argus_met = "";
		for(int b=0; b<metodos.length; b++)
			if(metodos[b].getNombre().substring(14).equals(id))
				argus_met = metodos[b].getArgumentos();

		String partes [] = s.split("%");
		s = "";
		for(int b=0; b<partes.length; b++)
			if(!partes[b].equals("<coma>")) s += partes[b]+" ";

		partes = argus_met.split( " " );
		argus_met = "";
		for(int b=0; b<partes.length; b++)
		{
			argus_met += partes[b].substring(0, partes[b].indexOf("-"))+"> ";
		}


		if( !argus_met.equals(s) )
		{
			System.out.println("Linea : " + linea);
			System.out.println("Error en argumentacion de metodo.");
			System.out.println("Requerido : #"+id+" ( "+argus_met+ ")");
			System.out.println("Encontrado: #"+id+" ( "+s+ ")");
			return 1;
		}
	return 0;
	}






	int buscarMain(){
		boolean found = false;
		for(int a=0; a<metodos.length; a++)
		{

			if(
				metodos[a].getNombre().substring(14).equals("main")
				&&
				metodos[a].getArgumentos().equals("NADA")
				&&
				metodos[a].getTipoDeRetorno().equals("TIPO_VOID")
			) found = true;
		}

		if(!found)
		{
		System.out.println("Metodo void #main() no existe.");
		return 1;
		}
	return 0;
	}


	void generarCodigo(){
		codigo = "";
		for(int a=0; a<vars.length; a++)
			codigo += "<declaracion global tipo:"+vars[a].getTipo().substring(5)
				+ " id:" +vars[a].getNombre().substring(14)+">\n";



		for(int a=0; a<metodos.length; a++)
		{

		codigo += "<METODO id:"+metodos[a].getNombre().substring(14)+" ";

		codigo += "args:";

		if(!metodos[a].getArgumentos().equals("NADA"))
		{
			String [] args = metodos[a].getArgumentos().split(" ");
			for(int c=0; c<args.length; c++)
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


		String tokens [] = metodos[a].getCuerpo().split("\n");
		for(int t=0; t<tokens.length; t++)
		{

			if(tokens[t].indexOf(" linea:") != -1)
				tokens[t] = tokens[t].substring(0, tokens[t].indexOf(" linea:"))+">";

		codigo += tokens[t]+"\n";
		}


		codigo += "</METODO>\n";

		}//for de cada metodo

	}//metodo








	String eliminarParentesis(String body){
		String [] token = body.split("\n");

		for(int a=0; a<token.length; a++)
			if(token[a].equals("<parentesis>") || token[a].equals("</parentesis>"))
				token[a]="";

		body="";
		for(int a=0; a<token.length; a++)
			if(!token[a].equals("")) body += token[a] + "\n";

	return body;
	}








	String convertirOP( String body ){
			//<INT valor:5 linea:19>
			//<operacion tipo:MUL linea:19>
			//<INT id:a linea:19>
			//convertir eso en ....
			//
			//	<op tipo:MUL linea:19>
			//		<INT valor:5 linea:19>
			//		<INT id:a linea:19>
			//	</op>

		boolean cambio = true;
		//aki va el while grandote de si hay cambios
		while(cambio)
		{

		cambio = !cambio;
		String [] token = body.split("\n");

		//primero ver donde esta la operacion mas profunda...
		int tabs = 0;
		int mayor = 0;
		int mayor_token=0;

		int lugar_operacion=0;//donde esta la operacion
		int lugar_inicio_a = 0;
		int lugar_fin_a = 0;
		int lugar_inicio_b = 0;
		int lugar_fin_b = 0;


		for(int a=0; a<token.length; a++)
		{
			if( token[a].startsWith("</")) tabs--;

			if( token[a].startsWith("<operacion") )
				{
				if(tabs>mayor) { mayor=tabs; mayor_token=a; }
				}
			if( token[a].startsWith("<llamada")) tabs++;
			if( token[a].startsWith("<llave>")) tabs++;
			if( token[a].startsWith("<asignacion")) tabs++;
			if( token[a].startsWith("<parentesis>")) tabs++;
			if( token[a].startsWith("<if")) tabs++;
			if( token[a].startsWith("<while")) tabs++;
			if( token[a].startsWith("<op")) tabs++; //aguas akiii
		}

		if(mayor==0)return body;

		cambio = true;
		lugar_operacion = mayor_token;

		//System.out.println("la op mas profunda es ="+token[mayor_token]);

		String argumento_a = token[mayor_token-1];
		String argumento_b = token[mayor_token+1];

		//ahora guardar los argumentos ke recibe la operacion

		//si el ke esta antes es una llamada o parentesis
		int _a = mayor_token-1;
		if(argumento_a.startsWith("</llamada>") || argumento_a.startsWith("</parentesis>") || argumento_a.startsWith("</op>"))
		{

			String buscando="";
			if(argumento_a.equals("</parentesis>")) buscando = "<parentesis>";
			if(argumento_a.startsWith("</llamada")) buscando = "<llamada";
			if(argumento_a.startsWith("</op>")) buscando = "<op ";

			int closure_m = 0;
			int closure_p = 0;
			int closure_o = 0;
			_a--;

			while( true )
			{
			if(token[_a].startsWith(buscando) && closure_m == 0 && closure_p==0 && closure_o==0) break;

			if(token[_a].equals("</llamada>")) closure_m++;
			if(token[_a].startsWith("<llamada")) closure_m--;
			if(token[_a].equals("</parentesis>")) closure_p++;
			if(token[_a].equals("<parentesis>")) closure_p--;
			if(token[_a].equals("</op>")) closure_o++;
			if(token[_a].startsWith("<op ")) closure_o--;

			--_a;
			}

			argumento_a = "";
			for(int _b = _a; _b<mayor_token; _b++) argumento_a += token[_b]+"\n";

			lugar_inicio_a = _a;
			lugar_fin_a = mayor_token-1;
		}
		else
		{
			lugar_inicio_a = _a;
			lugar_fin_a = lugar_inicio_a;
			argumento_a += "\n";
		}





		//si el ke esta despues es una llamada o parentesis
		_a = mayor_token+1;
		if(argumento_b.startsWith("<llamada") || argumento_b.equals("<parentesis>") || argumento_b.startsWith("<op "))
		{
			String buscando="";
			if(argumento_b.equals("<parentesis>")) buscando = "</parentesis>";
			if(argumento_b.startsWith("<llamada")) buscando = "</llamada";
			if(argumento_b.startsWith("<op ")) buscando = "</op>";


			int aperture_m = 0;
			int aperture_p = 0;
			int aperture_o = 0;
			_a++;
			while( true )
			{
			if(token[_a].startsWith(buscando) && aperture_m == 0 && aperture_p==0 && aperture_o==0) break;

			if(token[_a].equals("</llamada>")) aperture_m--;
			if(token[_a].startsWith("<llamada")) aperture_m++;
			if(token[_a].equals("</parentesis>")) aperture_p--;
			if(token[_a].equals("<parentesis>")) aperture_p++;
			if(token[_a].equals("</op>")) aperture_p--;
			if(token[_a].startsWith("<op ")) aperture_p++;
			++_a;
			}

			argumento_b = "";
			for(int _b = mayor_token+1; _b<=_a; _b++) argumento_b += token[_b]+"\n";

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

		for(int b=lugar_inicio_a; b<=lugar_fin_b; b++)
			token[b] = "";

		token[lugar_inicio_a] = "<op "+ope.substring(11)+"\n" + argumento_a + argumento_b + "</op>";

		body = "";
		for(int b=0; b<token.length; b++)
			if(!token[b].equals(""))body += token[b]+ "\n";


		}//while de si hubo cambios


	return body;
	}//convertir OPeraciones a la nueva nomenclatura





	int arreglar(){
		for(int a=0; a<metodos.length; a++)
		{
			//------------------------
			//primero voy a hacer un vector con las varibles, para poder ponerle de ke tipo es
			//------------------------
			String [] token = metodos[a].getCuerpo().split("<declaracion-");
			String [] args = metodos[a].getArgumentos().split(" ");

			int total = token.length + args.length - 1 + vars.length;
			if(metodos[a].getArgumentos().equals("NADA")) total--;

			String [][] variables = new String[total][2];

			for(int u=0; u<variables.length; u++)
				 { variables[u][0] = ""; variables[u][1] = ""; }

			int declaracion_actual = 0;

			for(int z=0; z<vars.length; z++)
			{
				variables[declaracion_actual][0] = vars[z].getNombre().substring(14);
				variables[declaracion_actual][1] = vars[z].getTipo().substring(5);
				declaracion_actual++;
			}

			args = metodos[a].getArgumentos().split(" ");

			if( !metodos[a].getArgumentos().equals("NADA") )
			{
				for(int z=0; z<args.length; z++)
				{
				String [] _b = args[z].split("-");
				variables[declaracion_actual][0] = _b[1].substring(0, _b[1].length()-1);
				variables[declaracion_actual][1] = _b[0].substring(1);
				declaracion_actual++;
				}
			}

			token = metodos[a].getCuerpo().split(" ");
			for(int b=0; b<token.length; b++)
			{
				if(token[b].startsWith("<declaracion-"))
				{
				String [] _a = token[b].split("-");
				variables[declaracion_actual][0] = _a[3].substring(0, _a[3].length()-1);
				variables[declaracion_actual][1] = _a[2]; //.substring(0, _a[3].length()-1);
				declaracion_actual++;
				}
			}

			//System.out.println("Variables: "+variables.length);
			//for(int _a=0; _a<variables.length; _a++)
			//	System.out.println("-"+variables[_a][1]+" "+variables[_a][0]);


			//------------------------
			//list las variables estan en variables[numero_de_var][id, tipo]
			//------------------------


			/////////////////////////////////////
			//ES TIEMPO DE NUEVOS COMIENZOS ESTO SE ESTA LLENDO A LA MIERDA
			// HAY QUE REORGANIZAR LOS TOKENS 		1:40am
			/////////////////////////////////////
			/*

			ahora los tokens seran separados por el caracter '\n'

			<declaracion tipo:int8 id:alan linea:4>
			<llamada tipo:int id:metodo1 linea:4> </llamada>
			<int id:var1 linea:5>
			<int valor:125 linea:5>
			<string id:var1 linea:5>
			<string valor:hola linea:5>
			<asignacion tipo:string id:cadena linea:12> </asignacion>
			<parentesis> </parentesis>
			<llave> </llave>
			<operacion tipo:suma linea:32> </operacion>
			<operacion tipo:resta linea:32> </operacion>
			<operacion tipo:mul linea:32> </operacion>
			<linea linea:5>
			<if linea:43> </if>
			<while linea:54> </while>
			<operacion tipo:bol_mayor linea:3>
			<operacion tipo:bol_menor linea:3>
			<operacion tipo:bol_igual linea:3>
			<coma>
			<retorno> </retorno>
			*/


			String tokens[] = metodos[a].getCuerpo().split(" ");

			String cuerpo = "";
			for(int g=0; g<tokens.length; g++) cuerpo += tokens[g]+"\n";


			cuerpo = reorganizarDeclaraciones(cuerpo, variables);

			cuerpo = reorganizarLLamadas(cuerpo);

			cuerpo = reorganizarVars(cuerpo, variables);

			cuerpo = reorganizarNumeros(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarStrings(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarAsignacion(cuerpo, variables);

			cuerpo = reorganizarParYLLaves(cuerpo);

			cuerpo = reorganizarOperaciones(cuerpo, metodos[a].getLinea());

			cuerpo = reorganizarLineas(cuerpo);

			metodos[a].setLinea( "<linea linea:"+metodos[a].getLinea().substring(13)+">" );

			cuerpo = reorganizarIf(cuerpo, metodos[a].getLinea());

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





	String quitarPuntoComaYLinea(String cuerpo){
		String [] token = cuerpo.split("\n");
		cuerpo = "";
		for(int a = 0; a<token.length; a++)
			if(!(token[a].equals("PUNTUACION_PUNTO_COMA") || token[a].startsWith("<linea")))
				cuerpo += token[a] + "\n";

	return cuerpo;
	}



	String reorganizarReturn(String body){
		String [] token = body.split("\n");
		body = "";
		for(int a = 0; a<token.length; a++)
			{
				if(token[a].startsWith("<retorno"))
				{
				token[a] = "<retorno "+token[a].substring(9);
				}
			body += token[a] + "\n";
			}

	return body;
	}



	String reorganizarComa(String body){
		String [] token = body.split("\n");
		body = "";
		for(int a = 0; a<token.length; a++)
			{
			if(token[a].equals("PUNTUACION_COMA")) token[a] = "<coma>";
			body += token[a] + "\n";
			}

	return body;
	}




	String reorganizarOpBoleanas(String body, String linea){
		//<operacion tipo:igual linea:3>
		//<operacion tipo:mayor linea:3>
		//<operacion tipo:menor linea:3>
		//BOL_MAYOR_QUE ASIGNA ASIGNA BOL_MENOR_QUE

		String [] token = body.split("\n");
		body = "";
		String nuevo;

		for(int a=0; a<token.length; a++)
		{
			nuevo = "";
			if(token[a].startsWith("<linea"))linea = token[a];

			if(token[a].startsWith("BOL_"))
			{
			String g [] = token[a].split("_");
			nuevo += "<operacion tipo:" + g[1] + " linea:" + linea.substring(13, linea.length()-1)+">";
			token[a] = nuevo;
			}

			if(token[a].startsWith("ASIGNA") && token[a+1].startsWith("ASIGNA"))
			{
			token[a] = "<operacion tipo:IGUAL linea:"+ linea.substring(13, linea.length()-1)+">";
			token[a+1] = "";
			}

		if(!token[a].equals("")) body += token[a]+ "\n";
		}

	return body;
	}



	String reorganizarWhile(String body, String linea){
		String [] token = body.split("\n");
		body = "";
		for(int  a = 0; a<token.length; a++)
		{

			if(token[a].startsWith("<linea")) linea = token[a];

			if(token[a].equals("CONTROL_WHILE"))
			{
				linea = linea.substring( 13, linea.length() -1 );
				token[a] = "<~while linea:"+linea+">";
				token[a+1] = "";
			}
		}


		boolean cambio=true;
		while(cambio)
		{
			int r = token.length-1;

			for(;;r--){
				if( r<0 ) { cambio=false; break; }
				if( token[r].startsWith("<~while linea:") ) { cambio=true; break; }
			}

			if(!cambio) break;

			token[r] = "<while "+token[r].substring(8);

			int parentesis=0;
			while( true )
			{
				if(token[r].equals("</parentesis>") && parentesis ==0) break;
				if(token[r].equals("<parentesis>")) parentesis++;
				if(token[r].equals("</parentesis>")) parentesis--;
				r++;
			}
			token[r] = "</while>";
		}

		body = "";
		for(int b=0; b<token.length; b++)
			if(!token[b].equals("")) body += token[b]+"\n";

	return body;
	}//metodo



	String reorganizarIf(String body, String linea){
		String [] token = body.split("\n");
		body = "";
		for(int  a = 0; a<token.length; a++)
		{

			if(token[a].startsWith("<linea")) linea = token[a];

			if(token[a].equals("CONTROL_IF"))
			{
				linea = linea.substring( 13, linea.length() -1 );
				token[a] = "<~if linea:"+linea+">";
				token[a+1] = "";
			}
		}


		boolean cambio=true;
		while(cambio)
		{
			int r = token.length-1;

			for(;;r--){
				if( r<0 ) { cambio=false; break; }
				if( token[r].startsWith("<~if linea:") ) { cambio=true; break; }
			}

			if(!cambio) break;

			token[r] = "<if "+token[r].substring(5);

			int parentesis=0;
			while( true )
			{
				if(token[r].equals("</parentesis>") && parentesis ==0) break;
				if(token[r].equals("<parentesis>")) parentesis++;
				if(token[r].equals("</parentesis>")) parentesis--;
				r++;
			}
			token[r] = "</if>";
		}

		body = "";
		for(int b=0; b<token.length; b++)
			if(!token[b].equals("")) body += token[b]+"\n";

	return body;
	}//metodo


	String reorganizarLineas(String body){
		String token [] = body.split("\n");
		body = "";
		for(int a=0; a<token.length; a++)
		{
			if(token[a].startsWith("NUMERO_LINEA_"))
				token[a] = "<linea linea:" + token[a].substring(13) + ">";

		body += token[a]+ "\n";
		}


	return body;
	}


	String reorganizarOperaciones(String body, String linea){
		String [] token = body.split("\n");
		body = "";
		String nuevo;
		for(int a=0; a<token.length; a++)
		{
			nuevo = "";
			if(token[a].startsWith("NUMERO_LINEA_"))linea = token[a];

			if(token[a].startsWith("OP_"))
			{
			String g [] = token[a].split("_");
			nuevo += "<operacion tipo:";
			if(g[1].equals("MULTIPLICACION"))g[1] = "MUL";
			if(g[1].equals("DIVISION"))g[1] = "DIV";
			nuevo += g[1] + " linea:" + linea.substring(13)+">";
			token[a] = nuevo;
			}

		body += token[a]+ "\n";
		}
	return body;
	}//metodo

	String reorganizarParYLLaves(String body){
		//	<parentesis> </parentesis>
		//	<llave> </llave>
		String [] token = body.split("\n");
		body = "";
		for(int a=0; a<token.length; a++)
		{
			if(token[a].equals("PARENTESIS_ABRE"))token[a]="<parentesis>";
			if(token[a].equals("PARENTESIS_CIERRA"))token[a]="</parentesis>";
			if(token[a].equals("LLAVE_ABRE"))token[a]="<llave>";
			if(token[a].equals("LLAVE_CIERRA"))token[a]="</llave>";

			body += token[a]+ "\n";
		}
	return body;
	}//metodo






	String reorganizarAsignacion(String body, String [][] variables){
		//new <asignacion tipo:string id:cadena linea:12> </asignacion>
		//old <asignacion-12-cadena>

		String [] token = body.split("\n");
		body = "";
		String nuevo;
		for(int a=0; a<token.length; a++)
		{
			nuevo = "";
			if(token[a].startsWith("<asignacion-"))
			{
			token[a] = token[a].substring( 1, token[a].length()-1);
			String g [] = token[a].split("-");
			nuevo += "<asignacion tipo:";

				for(int b=0; b<variables.length; b++)
					if(variables[b][0].equals(g[2])) nuevo += variables[b][1];

			nuevo += " id:" + g[2] + " linea:" + g[1]+">";
			token[a] = nuevo;
			}

		body += token[a]+ "\n";
		}



	return body;
	}





	String reorganizarStrings(String body, String linea){
	//STRING_"dfasd"
		String [] token = body.split("\n");
		body = "";

		for(int a=0; a<token.length; a++)
		{
			String nuevo = "";

			if(token[a].startsWith("NUMERO_LINEA"))linea = token[a];

			if(token[a].startsWith("STRING_"))
			{
				nuevo += "<STRING valor:"+token[a].substring(7);
				nuevo += " linea:"+linea.substring(13)+">";
				token[a] = nuevo;
			}

			body += token[a]+"\n";
		}

	return body;
	}


	String reorganizarNumeros(String body, String linea){
		String [] token = body.split("\n");
		body = "";

		for(int a=0; a<token.length; a++)
		{
			String nuevo = "";

			if(token[a].startsWith("NUMERO_LINEA"))linea = token[a];

			if(token[a].startsWith("VALOR_NUMERO_"))
			{
				nuevo += "<INT valor:"+token[a].substring(13);
				nuevo += " linea:"+linea.substring(13)+">";
				token[a] = nuevo;
			}

			body += token[a]+"\n";
		}

	return body;
	}





	String reorganizarVars(String body, String variables[][]){
	//old <var-8-a>
	//<int8 id:var1 linea:5>
		String [] token = body.split("\n");
		body = "";
		for(int a=0; a<token.length; a++)
		{
			String nuevo = "";
			if(token[a].startsWith("<var"))
			{
			String f = token[a].substring(1, token[a].length()-1);
			String [] ff = f.split("-");
			nuevo += "<";

			for(int z=0; z<variables.length; z++)
				if(variables[z][0].equals( ff[2] )) nuevo += variables[z][1];

			nuevo += " id:" + ff[2] + " linea:" + ff[1] + ">";

			token[a] = nuevo;
			}
		body += token[a]+"\n";
		}

	return body;
	}



	String reorganizarLLamadas(String body){
		//old  <llamada-8-m3> </llamada>
		//new one <llamada tipo:int id:metodo1 linea:4> </llamada>
		String [] token = body.split("\n");
		body = "";
		for(int a=0; a<token.length; a++)
		{
			String nuevo = "";
			if(token[a].startsWith("<llamada"))
			{
			String f = token[a].substring(1, token[a].length()-1);
			String [] ff = f.split("-");
			nuevo += "<llamada tipo:";
				for(int _fff=0; _fff<metodos.length; _fff++)
				{
					if(ff[2].equals(metodos[_fff].getNombre().substring(14)))
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




	String reorganizarDeclaraciones(String body, String[][]variables){
		//old  <declaracion-7-INT16-a>
		//new one <declaracion tipo:int8 id:alan linea:4>
		String [] token = body.split("\n");
		body = "";
		for(int a=0; a<token.length; a++)
		{
			String nuevo = "";
			if(token[a].startsWith("<declaracion-"))
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








	int convertirVariables(){
		for(int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();
			String cuerpo = "";
			for(int b=0; b<token.length; b++)
			{
				if(token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if(token[b].startsWith("IDENTIFICADOR_"))
				{

					token[b] = "<var-"+linea.substring(13)+"-"+token[b].substring(14)+">";
				}

				cuerpo += token[b] + " ";
			}

			metodos[a].setCuerpo(cuerpo);
		}//cada metodo

	return 0;
	}//convertirVariables



	int revisarReturn(){
		for(int a = 0; a<metodos.length; a++)
		{

		String [] token = metodos[a].getCuerpo().split(" ");
		String linea = metodos[a].getLinea();
			for(int b=0; b<token.length; b++)
			{

				if(token[b].startsWith("NUMERO_LINEA_")) linea = token[b];

				if(token[b].equals("CONTROL_RETORNO"))
				{
					token[b] = "<retorno-linea:"+linea.substring(13)+">";

					while(!token[b].equals("PUNTUACION_PUNTO_COMA")) b++;

					token[b] = "</retorno>";
				}

			}//for de kada token


		String body = "";
			for(int b=0; b<token.length; b++)
			{
				body += token[b] + " ";
			}

		metodos[a].setCuerpo( body );

		}//for de kada metodo

	return 0;
	}


	int revisarArgumentosDeLLamadas(){
		Metodos [] metodos_test = metodos;

		for(int a=0; a<metodos_test.length; a++)
		{
			String token[] = metodos_test[a].getCuerpo().split(" ");

			String __g = "";
			for(int g=0; g<token.length; g++)
				if(!token[g].startsWith("NUMERO_LINEA")) __g += token[g]+" ";

			token = __g.split(" ");

			int argu=0;
			boolean cambio=true;
			while(cambio)
			{
				cambio=false;

				int r = token.length-1;

				for(;;r--)
				{
					if( r<0 ) break;
					if( token[r].startsWith("<llamada-") ) { cambio=true; break; }
				}

				if(!cambio) break;

				String llamada_a = token[r].substring( token[r].lastIndexOf("-")+1, token[r].length()-1 );
				String linea = token[r].substring(9, token[r].lastIndexOf("-"));

				token[r] = "<~llamada-"+token[r].substring(9);

				r++;
				argu=0;
				while( true )
				{
					if(token[r].equals("</llamada>")) break;

					if(token[r-1].startsWith("<~llamada"))	 argu++;

					if(token[r].equals("PUNTUACION_COMA")&&argu>0) argu++;

					token[r]="";
					r++;
				}

				token[r] = "<~/llamada>";


				boolean encontrado=false;
				int deberian = 0;
				for(int h=0; h<metodos.length; h++)
				{
					if(metodos[h].getNombre().substring(14).equals(llamada_a))
					{
					String [] t = metodos[h].getArgumentos().split(" ");

					deberian = t.length;
					if( metodos[h].getArgumentos().equals("NADA") ) deberian=0;
					if( deberian == argu) encontrado=true;

					}
				}

				if(!encontrado)
				{
				System.out.println("Linea: "+linea);
				System.out.println("Metodo "+llamada_a+" deberia recibir "+deberian+" argumentos.");
				return 1;
				}

			}//por cada cambio hecho
		}//for de cada objeto
	return 0;
	}//revisarArgumentosDeLLamadas()








	int convertirAsignaciones(){
		for(int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();
			for(int b=0; b<token.length; b++)
			{
				if(token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if(token[b].startsWith("IDENTIFICADOR_") && token[b+1].equals("ASIGNA") && !token[b+2].equals("ASIGNA"))
				{
				token[b] = "<asignacion-"+linea.substring(13)+"-"+token[b].substring(14)+">";
				token[++b] = "";
					while(!token[b].equals("PUNTUACION_PUNTO_COMA")) b++;
				token[b] = "</asignacion>";
				}
			}

			String nuevo_cuerpo = "";
			for(int b=0; b<token.length; b++)
				if(!token[b].equals("")) nuevo_cuerpo += token[b] + " ";

			metodos[a].setCuerpo(nuevo_cuerpo);
		}

	return 0;
	}


	int convertirLLamadas(){
		for(int a=0; a<metodos.length; a++)
		{
			String [] token = metodos[a].getCuerpo().split(" ");
			String linea = metodos[a].getLinea();

			for(int b=0; b<token.length; b++)
			{
				if(token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if(token[b].equals("PUNTUACION_GATO"))
				{

				token[b] = "<~llamada-"+linea.substring(13)+"-"+token[b+1].substring(14)+">";

				while(!token[b].startsWith("IDENTIFICADOR")) b++;
					token[b] = "";

				while(!token[b].equals("PARENTESIS_ABRE")) b++;
					token[b] = "";
				}
			}



			String cuerpo = "";
			for(int b=0; b<token.length; b++)
				if(!token[b].equals("")) cuerpo += token[b]+" ";

			metodos[a].setCuerpo( cuerpo );


			token = metodos[a].getCuerpo().split(" ");

			boolean cambio=true;
			while(cambio)
			{
				cambio=false;

				int r = token.length-1;

				for(;;r--)
				{
					if( r<0 ) break;
					if( token[r].startsWith("<~llamada-") ) { cambio=true; break; }
				}

				if(!cambio) break;

				token[r] = "<llamada-"+token[r].substring(10);

				int parentesis=0;
				while( true )
				{
					if(token[r].equals("PARENTESIS_CIERRA") && parentesis ==0) break;
					if(token[r].equals("PARENTESIS_ABRE")) parentesis++;
					if(token[r].equals("PARENTESIS_CIERRA")) parentesis--;
					r++;
				}

				token[r] = "</llamada>";
			}



			cuerpo = "";
			for(int b=0; b<token.length; b++)
				if(!token[b].equals("")) cuerpo += token[b]+" ";

			metodos[a].setCuerpo( cuerpo );

		}//for de kada metodo

	return 0;
	}



	int crearObjetos(){
		String [] token = codigo.split("\n");

		int total_definiciones = 0;
		for(int d = 0; d<token.length; d++) if(token[d].equals("CONTROL_DEF"))total_definiciones ++;

		String [] definiciones = new String[total_definiciones];

		int definicion=-1;
		String linea="";

		for(int d = 0; d<token.length; d++)
		{
			if(token[d].equals("CONTROL_DEF"))
			{
			definicion++;
			definiciones[definicion] = linea+" ";
			}

			try{
			if((definicion > -1)&&(!token[d+1].equals("CONTROL_DEF"))) definiciones[definicion] += (token[d]+" ");
			}catch(Exception e){}


			if(token[d].startsWith("NUMERO_LINEA")) linea = token[d];
		}

		//listo aki el vector definiciones sale con todas las definiciones incluyendo el numero de linea


		//en fin las definiciones de instrucciones basicas se kedan en
		//un  vector de strings ke se llama definiciones[]
		//ahora contar si son metodos o variables
		int num_de_metodos=0, num_de_variables=0;

		for(int x=0; x< total_definiciones; x++)
		{
			if(definiciones[x].indexOf("PUNTUACION_GATO") != -1) num_de_metodos++; else num_de_variables++;
		}


		//pero antes para mayor comodidad, mejor hacer dos vectores,
		//def_met[] y def_var[]
		String [] def_met = new String[num_de_metodos];
		String [] def_var = new String[num_de_variables];

		int v=0, m=0;

		for(int b=0; b<total_definiciones; b++)
		{
			if(definiciones[b].indexOf("PUNTUACION_GATO") == -1)
				def_var[v++]=definiciones[b];
			else
				def_met[m++]=definiciones[b];
		}

		//ahora si crear bien los objetos de la clase Metodos
		metodos = new Metodos[num_de_metodos];
		for( int a = 0; a<metodos.length; a++ ) metodos[a] = new Metodos();

		int met = 0;
		for(int a=0; a<num_de_metodos; a++)
		{

			String [] _a = def_met[a].split(" ");

			metodos[met].setLinea( _a[0] );

			String [] _b = new String[_a.length];
			int _d = 0;
			for(int _c = 0; _c < _a.length; _c++)
				if( ! _a[_c].startsWith("NUMERO_LINEA") ) _b[_d++] = _a[_c];

			String _e = "";
			for(int _c = 0; _c < _d; _c++) _e += _b[_c]+" ";

			String _f = "";
			for(int _c = 0; _c < _a.length; _c++) _f += _a[_c]+" ";

			metodos[met].setTipoDeRetorno( _b[1] );
			metodos[met].setNombre( _b[3] );

			metodos[met].setArgumentos( _e.substring( _e.indexOf("PARENTESIS_ABRE") + 15 , _e.indexOf("PARENTESIS_CIERRA") ) );
			metodos[met].setCuerpo( _f.substring( _f.indexOf("LLAVE_ABRE")  ) );

		met++;
		}//for de cada metodo



		//agregar el metodo de impresion



		//ahora crear los objetos de la clase Variables
		vars = new Variables[num_de_variables];
		for( int a = 0; a<vars.length; a++ ) vars[a] = new Variables();

		int nvar = 0;
		for(int a=0; a<num_de_variables; a++)
		{
			String [] _a = def_var[a].split(" ");

			vars[nvar].setLinea( _a[0] );

			String [] _b = new String[_a.length];
			int _d = 0;
			for(int _c = 0; _c < _a.length; _c++)
				if( ! _a[_c].startsWith("NUMERO_LINEA") ) _b[_d++] = _a[_c];

			vars[nvar].setTipo( _b[1] );
			vars[nvar].setNombre( _b[2] );
		nvar++;
		}//for de cada variable


		//listo vars[] y metodos[] ya estan llenos ... 1:14pm


		//----------------RIP------------------
		//aki yacian mas de cien lineas de codigo ke tuvieron ke ser borradas,
		//ya ke no pense de manera orientada a objetos.... esas 120 lineas se convirtieron
		//en 50 lineas mucho mas eficientes
		//----------------RIP------------------

		return 0;
		}//fin metodo crearObjetos()


	int revisarDefiniciones(){

		//Revisar metodos con el mismo nombre
		for(int a = 0; a<metodos.length; a++)
		{
		String comp = metodos[a].getNombre();
			for(int b=0; b<metodos.length; b++)
			{
				if(a==b) b++;
				if(b==metodos.length) break;
				if( comp.equals( metodos[b].getNombre() ) )
				{
				System.out.println("Linea: "+metodos[b].getLinea().substring(13));
				System.out.println("Metodo "+comp.substring(14)+" ya esta definido en este programa.");
				debug.imprimirLinea("\nMetodo ya definido !!!");
				return 1;
				}
			}
		}

		//Revisar variables con el mismo nombre
		for(int a = 0; a<vars.length; a++)
		{
		String comp = vars[a].getNombre();
			for(int b=0; b<vars.length; b++)
			{
				if(a==b) b++;
				if(b==vars.length) break;
				if( comp.equals( vars[b].getNombre() ) )
				{
				System.out.println("Linea: "+vars[b].getLinea().substring(13));
				System.out.println("Variable "+comp.substring(14)+" ya esta definido en este programa.");
				debug.imprimirLinea("\nVariable ya definida !!!");
				return 1;
				}
			}
		}

	return 0;
	}//revisarDefiniciones()






	//revisar ke dentro del cuerpo de los metodos no se declaren las variables dos veces
	//acuerdate ke los argumentos ke recibe el metodo son tambien declaraciones
	//y ke no pueden tener el mismo nombre ke declaraciones globales def
	int revisarCuerpoVariables(){

	String cuerpo="";
	String linea="";
	String declaraciones [][];
	int declaracion;

	for(int d=0; d<metodos.length; d++)
	{
		cuerpo = metodos[d].getCuerpo();
		String [] tokens = cuerpo.split(" ");
		declaracion=0;

		//contar cuantas declaraciones en los argumentos ke recibe
		String s = metodos[d].getArgumentos();
		String ss [] = s.split(" ");
		for(int h=0; h<ss.length; h++)
		{
			if( ss[h].startsWith("TIPO_") && ss[h+1].startsWith("IDENTIFICADOR_"))
			declaracion++;
		}

		int argumentos = declaracion;

		//contar cuantas declaraciones hay en el cuerpo--
		for(int a=0; a<tokens.length; a++)
			if( tokens[a].startsWith("TIPO_") && tokens[a+1].startsWith("IDENTIFICADOR_") && tokens[a+2].equals("PUNTUACION_PUNTO_COMA"))
				declaracion++;


		//crear un vector bidimensional.. declaracion-> linea, tipo, id
		declaraciones = new String[declaracion][3];

		int inicio=0;

		for(int h=0; h<ss.length; h++)
		{
			if( ss[h].startsWith("TIPO_") && ss[h+1].startsWith("IDENTIFICADOR_"))
			{
			declaraciones[inicio][0] = metodos[d].getLinea();
			declaraciones[inicio][1] = ss[h];
			declaraciones[inicio][2] = ss[h+1];
			inicio++;
			}
		}

		linea = metodos[d].getLinea();
		for(int a=0; a<tokens.length; a++)
		{
		if( tokens[a].startsWith("NUMERO_LINEA"))linea = tokens[a];

			if( tokens[a].startsWith("TIPO_") && tokens[a+1].startsWith("IDENTIFICADOR_") && tokens[a+2].equals("PUNTUACION_PUNTO_COMA"))
			{
			declaraciones[inicio][0] = linea;
			declaraciones[inicio][1] = tokens[a];
			declaraciones[inicio][2] = tokens[a+1];
			inicio++;
			}
		}//kada token


		//por fin !! ya tengo la declaraciones de este metodo... ahora ver ke no sean iwuales
		//o ke no sean iwuales a alguna definicion global
		//las declaraciones globales estan en el el arreglo vars[].getNombre()
		for(int p=0; p<declaraciones.length; p++)
		{
		//System.out.println("- "+declaraciones[p][0]+" "+declaraciones[p][1]+" "+declaraciones[p][2]);
		String id = declaraciones[p][2];
			for(int q=0; q<declaraciones.length; q++)
			{
			if( p == q) q++;
			if( q == declaraciones.length) break;
				if( id.equals( declaraciones[q][2] ) )
				{
				System.out.println("Linea: "+declaraciones[q][0].substring(13));
				System.out.println("Variable "+ declaraciones[p][2].substring(14)+" ya esta definida localmente.");
				return 1;
				}
			}

			for(int f=0; f<vars.length; f++)
			{
				if( id.equals( vars[f].getNombre() ) )
				{
				System.out.println("Linea: "+declaraciones[p][0].substring(13));
				System.out.println("Variable "+ declaraciones[p][2].substring(14)+" ya esta definida globalmente, en la linea "+vars[f].getLinea().substring(13)+".");
				return 1;
				}
			}
		}//for de comprobaciones... fiuuu


		//ya seeeee... !!! en vez de tener 3 tokens, hacer unos solo
		//ke contenga <declaracion-linea-tipo-id>
		//declaraciones[p][0,1,2]

		//pido perdon a los dioses por tanto if (ya vez omar, son necesarios jaja)
		linea = metodos[d].getLinea();
		for(int x=0; x<tokens.length; x++)
		{
		   if(tokens[x].startsWith("NUMERO_LINEA_")) linea = tokens[x].substring(13);

		   if(tokens[x].startsWith("TIPO_"))
		   {
			if(tokens[x+1].startsWith("IDENTIFICADOR_") && tokens[x+2].equals("PUNTUACION_PUNTO_COMA") )
			{
			tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+1].substring(14)+">";
			tokens[x+1] = "";
			tokens[x+2] = "";
			}

			if(tokens[x+1].startsWith("IDENTIFICADOR_") && tokens[x+2].startsWith("NUMERO_LINEA_") && tokens[x+3].equals("PUNTUACION_PUNTO_COMA") )
			{
			tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+1].substring(14)+">";
			tokens[x+1] = tokens[x+2];
			tokens[x+2] = "";
			tokens[x+3] = "";
			}

			if(tokens[x+1].startsWith("NUMERO_LINEA_") && tokens[x+2].startsWith("IDENTIFICADOR_") && tokens[x+3].equals("PUNTUACION_PUNTO_COMA") )
			{
			tokens[x] = "<declaracion-"+linea+"-"+tokens[x].substring(5)+"-"+tokens[x+2].substring(14)+">";
			tokens[x+1] = tokens[x+1];
			tokens[x+2] = "";
			tokens[x+3] = "";
			}

			if(tokens[x+1].startsWith("NUMERO_LINEA_") && tokens[x+2].startsWith("IDENTIFICADOR_") && tokens[x+3].startsWith("NUMERO_LINEA_") && tokens[x+4].equals("PUNTUACION_PUNTO_COMA") )
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
		for(int y=0; y<tokens.length; y++)if(!tokens[y].equals(""))cuerpo2 += tokens[y] + " ";

		metodos[d].setCuerpo(cuerpo2);

	}//for de cada cuerpo de cada metodo

	return 0;
	}//revisarCuerpoVariables()




	int revisarExistenciaMetodos(){
		//Antes de empezar kiero limpiar los argumentos
		//argumentos en el formato   <tipo-id>
		for( int b=0; b<metodos.length; b++ )
		{
			String [] args = metodos[b].getArgumentos().split(" ");

			for(int c=0; c<args.length; c++)
			{
				if(args[c].startsWith("TIPO_"))
				{
				args[c] = "<" + args[c].substring(5) + "-" + args[c+1].substring(14)+">";
				args[c+1] = "";
				}

				if(args[c].equals("PUNTUACION_COMA")) args[c] = "";
			}

			String new_args = "";
			for(int d=0; d<args.length; d++)
				if(!args[d].equals(""))new_args += args[d]+" ";

			if(new_args.equals(""))new_args = "NADA";

			metodos[b].setArgumentos(new_args);
		}//okay con los argumentos


		//ahora si revisar ke exista el id del metodo al que se esta llamando
		for( int b=0; b<metodos.length; b++ )
		{
			String [] token = metodos[b].getCuerpo().split(" ");
			String linea = metodos[b].getLinea();
			for(int c=0; c<token.length; c++)
			{

				if(token[c].startsWith("NUMERO_LINEA_")) linea = token[c];

				if(token[c].equals("PUNTUACION_GATO"))
				{
				String id = token[c+1];
				boolean found = false;

					for(int d=0; d<metodos.length; d++)
						if( metodos[d].getNombre().equals(id))found=true;

					if(!found)
					{
						System.out.println("Linea: "+linea.substring(13));
						System.out.println("Metodo "+id.substring(14)+" no ha sido definido.");
						return 1;
					}
				}//si encuentra llamada a algun metodo
			}
		}//for de cada cupero de kada metodo

	return 0;
	}//revisarMetodos()



	int revisarExistenciaVariables(){
		for(int a = 0; a<metodos.length; a++)
		{


		//contar cuantas declaraciones hay en TOTAL
		String [] token = metodos[a].getCuerpo().split("<declaracion-");
		String [] args = metodos[a].getArgumentos().split(" ");

		int total = token.length + args.length - 1 + vars.length;
		if(metodos[a].getArgumentos().equals("NADA")) total--;

		//hacer un recorrido por todos lo tokens
		//ir guardando las declaraciones en el vector variables
		//luego si encuentro algun uso de variable, checar en el vector, si si existe
		//si no, es porke, o aun no ha sido declarada, o nunca ha sido declarada
		String [] variables = new String[total];

		for(int u=0; u<variables.length; u++) variables[u] = "";

		int declaracion_actual = 0;
		String linea = metodos[a].getLinea();

		token = metodos[a].getCuerpo().split(" ");

			for(int z=0; z<vars.length; z++)
				variables[declaracion_actual++] = vars[z].getNombre().substring(14);

			args = metodos[a].getArgumentos().split(" ");

			if( !metodos[a].getArgumentos().equals("NADA") )
			{
				for(int z=0; z<args.length; z++)
				{
				String [] _b = args[z].split("-");
				variables[declaracion_actual++] = _b[1].substring(0, _b[1].length()-1);///// error cuando no hay argumentos
				}
			}


			for(int b=0; b<token.length; b++)
			{
				if(token[b].startsWith("NUMERO_LINEA")) linea = token[b];

				if(token[b].startsWith("<declaracion-"))
				{
					String [] _a = token[b].split("-");
					variables[declaracion_actual++] = _a[3].substring(0, _a[3].length()-1);
				}

				if(token[b].startsWith("IDENTIFICADOR_"))
				{
					boolean found = false;
					for(int h=0; h<variables.length; h++)
						if(variables[h].equals(token[b].substring(14))) found=true;

					if(!found)
					{
					Help.usingBeforDef( token[b].substring(14), Integer.parseInt(linea.substring(13)) );
					/*System.out.println("Linea: " + linea.substring(13));
					System.out.println("Variable " + token[b].substring(14) + " es utilizada antes de ser declarada.");*/
					return 1;
					}
				}



				if(token[b].startsWith("<asignacion-"))
				{
					String [] _t = token[b].split("-");
					String _id = _t[2].substring(0, _t[2].length() - 1);

					boolean found = false;
					for(int h=0; h<variables.length; h++)
						if(variables[h].equals(_id)) found=true;

					if(!found)
					{
					Help.usingBeforDef( _id, Integer.parseInt(linea.substring(13)) );						
//					System.out.println("Linea: " + linea.substring(13));
//					System.out.println("Variable " + _id + " es utilizada antes de ser declarada.");
					return 1;
					}

				}
			}//for de kada token
		}//for de metodos
	return 0;
	}//revisarExistenciaVariables
























	void imprimirObjetos(){
		//imprimir lo ke tienen los objetos nomas pa probar
		for(int z=0; z<metodos.length; z++)
		{
		debug.imprimirLinea( "\nMetodo "+z );
		debug.imprimirLinea( "linea: " + metodos[z].getLinea() );
		debug.imprimirLinea( "nombre: " + metodos[z].getNombre() );
		debug.imprimirLinea( "retorno: " + metodos[z].getTipoDeRetorno() );
		debug.imprimirLinea( "recibe: " + metodos[z].getArgumentos() );
		debug.imprimirLinea( "cuerpo: ");
			String _g [] = metodos[z].getCuerpo().split("\n");
			int tabs = 0;
			for(int _h = 0; _h < _g.length; _h++)
			{
				if( _g[_h].startsWith("</")) tabs--;

				for(int b=0; b<tabs; b++) debug.imprimir("	");
				debug.imprimirLinea( _g[_h]);

				if( _g[_h].startsWith("<llamada")) tabs++;
				if( _g[_h].startsWith("<llave>")) tabs++;
				if( _g[_h].startsWith("<asignacion")) tabs++;
				if( _g[_h].startsWith("<parentesis>")) tabs++;
				if( _g[_h].startsWith("<if")) tabs++;
				if( _g[_h].startsWith("<while")) tabs++;
				if( _g[_h].startsWith("<op ")) tabs++;
				if( _g[_h].startsWith("<retorno")) tabs++;
			}
		}

		for(int z=0; z<vars.length; z++)
		{
		debug.imprimirLinea( "\nVariable "+z );
		debug.imprimirLinea( "linea: " + vars[z].getLinea() );
		debug.imprimirLinea( "nombre: " + vars[z].getNombre() );
		debug.imprimirLinea( "tipo: " + vars[z].getTipo() );
		}
	}//imprimir objetos






}//clase analisis_semantico



/*------------------------------------------------------------------------------------
 				Variables
-------------------------------------------------------------------------------------*/
//las variables ke se encuentran en el programa, son un objeto de esta clase
class Variables{

	private String Linea;
	private String Nombre;
	private String Tipo;

	Variables(){
		this.Linea = "ns";
		this.Nombre = "ns";
		this.Tipo = "ns";
	}

	void setLinea(String s){ Linea = s; }
	void setNombre(String s){ Nombre = s; }
	void setTipo(String s){ Tipo = s; }

	String getLinea(){return Linea;}
	String getNombre(){return Nombre;}
	String getTipo(){ return Tipo; }
}


/*------------------------------------------------------------------------------------
 				Metodo
-------------------------------------------------------------------------------------*/
//los metodos ke se encuentran en el programa, son un objeto de esta clase
class Metodos{

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








/*------------------------------------------------------------------------------------
 				AUTOMATA DE PILA
-------------------------------------------------------------------------------------*/
	//su constructor recibe...Automata( Produccion [] prod, String cadena a probar )
	//asi ke tambien necesita la clase produccion ke esta abajito
	//para usarlo primero hay ke krear el vector Producciones
	// asi: Produccion [] prod =	{ new Produccion("<oracion>", "<sujeto> <predicado>") }
	//el conssstructor de produccion es Produccion (String ladoIzquierdo, String ladoDerecho)
class Automata{

private Produccion [] prods;
private String cadena;
private String [] tokens;
Stack <String> pila = new Stack<String>();
Debugger debug;


	Automata(Produccion [] prod, String cad){
		prods = prod;
		cadena = cad;
	}


	void setDebugger(Debugger debug){
		this.debug = debug;
	}

	String iniciar(){

		tokens = cadena.split(" ");

		pila.push("$");
		pila.push("$");
		pila.push("$");
		pila.push("$");
		pila.push("$");
		pila.push("$");

		int numero_linea=1;
		boolean cambio;
		for(int index = 0; index < tokens.length; index++)
		{
			verPila();

			//si lo ke encuentra en la pila es el numero de linea
			//guardarlo en la variable numero_linea... jajaja ke idiota
			//pero bueno ya despues de algo servira
			pila.push( tokens[index] );

			if(pila.peek().startsWith("NUMERO_LINEA_"))
				{
				numero_linea = Integer.parseInt( pila.pop().substring(13) );
				pila.push( tokens[++index] );
				}


			cambio=true;

			while(cambio)
			{


			if(pila.peek().startsWith("<error_")) //errores
				{
				System.out.println("ERROR");
				System.out.println("en linea : " + numero_linea);
				return "error";
				}



			verPila();
			cambio=false;

				for(int b=0; b<prods.length; b++)
				{

				String [] derecho = prods[b].getLadoDer().split(" ");
				String s = "";
				StringBuffer sb = new StringBuffer("");

				for(int c=0; c<derecho.length; c++) sb.insert(0, pila.pop()+" ");

				sb.setLength( sb.length() - 1 );
				s = String.valueOf( sb );

				//pl("Analizando: "+s+" y "+prods[b].getLadoDer());

				if(s.equals( prods[b].getLadoDer() ))
					{
					//pl("reduciendo: "+s+" por "+prods[b].getLadoIzq());

					StringTokenizer sb12 = new StringTokenizer(prods[b].getLadoIzq(), " ");
					while(sb12.hasMoreTokens())pila.push( sb12.nextToken() );
					cambio=true;
					break;
					}
				else
					{
					//meter a la pila los ke sake ya ke no encontre
					//ninguna reduccion para estos tokens
					String r [] = s.split(" ");
					for(int f=0; f<r.length; f++) { pila.push( r[f] ); }
					}
				}//for producciones
			}//while de volver a chekar si hubo cambio
		}//for de tokens

	//verPila();

	Stack <String> pila2_a = new Stack<String>();
	String pilaString="";
	while(!pila.empty()) pila2_a.push( pila.pop() );

	while(!pila2_a.empty()) pilaString += (pila2_a.pop() +" ");

	return pilaString;
	}//metodo




 	void verPila(){
		Stack <String> pila2 = new Stack<String>();
		while(!pila.empty()) pila2.push( pila.pop() );

		while(!pila2.empty())
		{
		String s = pila2.pop();
		//p(s+" ");
		try{ debug.imprimir( s+" " ); }catch(Exception e){ } //ponerlo en un try por si no se ha definido ningun debugger
		pila.push( s );
		}
	try{ debug.imprimirLinea( " " ); }catch(Exception e){ }
 	}//ver Pila()


	void p(String a){ System.out.print(a); }
	void pl(String a){ System.out.println(a); }

}//clase

class Produccion{

String ladoIzquierdo;
String ladoDerecho;

	Produccion(String i, String d){
		ladoIzquierdo = i;
		ladoDerecho = d;
	}

	String getLadoIzq(){
		return ladoIzquierdo;
	}

	String getLadoDer(){
		return ladoDerecho;
	}
}//fin clase produccion



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

		nuevo_codigo = ".286\n\n";

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
			iniciar("pf.txt");
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
