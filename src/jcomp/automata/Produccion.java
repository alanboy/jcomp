package jcomp.automata;


public class Produccion
{
	String ladoIzquierdo;
	String ladoDerecho;

	public Produccion(String i, String d){
		ladoIzquierdo = i;
		ladoDerecho = d;
	}

	String getLadoIzq(){
		return ladoIzquierdo;
	}

	String getLadoDer(){
		return ladoDerecho;
	}
}//class Produccion

