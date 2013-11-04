
import java.io.*;
import java.util.*;
/*------------------------------------------------------------------------------------
 				AUTOMATA DE PILA
-------------------------------------------------------------------------------------*/
	//su constructor recibe...Automata( Produccion [] prod, String cadena a probar )
	//asi ke tambien necesita la clase produccion ke esta abajito
	//para usarlo primero hay ke krear el vector Producciones
	// asi: Produccion [] prod =	{ new Produccion("<oracion>", "<sujeto> <predicado>") }
	//el conssstructor de produccion es Produccion (String ladoIzquierdo, String ladoDerecho)
public class Automata{

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


