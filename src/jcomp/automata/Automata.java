 package jcomp.automata;

import java.io.*;
import java.util.*;
import jcomp.util.Log;

/*------------------------------------------------------------------------------------
 				AUTOMATA DE PILA
-------------------------------------------------------------------------------------*/
// su constructor recibe...Automata(Produccion [] prod, String cadena a probar)
// asi: Produccion [] prod = { new Produccion("<oracion>", "<sujeto> <predicado>") }
// el conssstructor de produccion es Produccion (String ladoIzquierdo, String ladoDerecho)
public class Automata
{
	private Produccion [] prods;
	private String cadena;
	private String [] tokens;
	private Stack <String> pila = new Stack<String>();
	private Log l;

	public Automata(Produccion [] prod, String cad)
	{
		prods = prod;
		cadena = cad;
		l = Log.getInstance();
	}

	public String iniciar()
	{
		tokens = cadena.split(" ");
		pila.push("$");

		int numero_linea =1;
		boolean cambio;

		for (int index = 0; index < tokens.length; index++)
		{
			pila.push(tokens[index]);

			// si lo ke encuentra en la pila es el numero de linea
			// guardarlo en la variable numero_linea...
			// pero bueno ya despues de algo servira

			if (pila.peek().startsWith("NUMERO_LINEA_"))
			{
				numero_linea = Integer.parseInt(pila.pop().substring(13));
				pila.push(tokens[++index]);
			}

			cambio = true;

			while (cambio)
			{
				verPila();

				cambio = false;

				for(int b=0; b < prods.length; b++)
				{
					String [] derecho = prods[b].getLadoDer().split(" ");
					String s = "";
					StringBuffer sb = new StringBuffer("");

					// Si el numero de tokens en la pila es menor
					// que el numero de tokens que estan del lado
					// derecho, entonces no hay manera posible de
					// que termine reduciendo, asi que saltarme a la
					// siguiente produccion.
					if (derecho.length > pila.size())
					{
						continue;
					}

					for(int c = 0; c < derecho.length; c++)
					{
						sb.insert(0, pila.pop()+" ");
					}

					sb.setLength(sb.length() - 1);
					s = String.valueOf(sb);

					// Analizando: s y prods[b].getLadoDer()

					if(s.equals(prods[b].getLadoDer()))
					{
						// Reduciendo: s por prods[b].getLadoIzq()
						StringTokenizer sb12 = new StringTokenizer(prods[b].getLadoIzq(), " ");
						while(sb12.hasMoreTokens())
						{
							pila.push(sb12.nextToken());
						}
						cambio=true;
						break;
					}
					else
					{
						//meter a la pila los ke sake ya ke no encontre
						//ninguna reduccion para estos tokens
						String r [] = s.split(" ");
						for(int f=0; f<r.length; f++)
						{
							pila.push(r[f]);
						}
					}
				}//for producciones
			}//while de volver a chekar si hubo cambio
		}//for de tokens

		Stack <String> pila2_a = new Stack<String>();
		String pilaString="";

		while(!pila.empty())
		{
			pila2_a.push(pila.pop());
		}

		while(!pila2_a.empty())
		{
			pilaString += (pila2_a.pop() +" ");
		}

		return pilaString;
	}

	void verPila()
	{
		Stack <String> pila2 = new Stack<String>();
		while(!pila.empty())
		{
			pila2.push(pila.pop());
		}

		while(!pila2.empty())
		{
			String s = pila2.pop();
			l.imprimir(s + " ");
			pila.push(s);
		}

		if(l != null)
		{
			l.imprimirLinea("");
		}
	}
}

