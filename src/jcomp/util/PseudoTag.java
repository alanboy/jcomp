package jcomp.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

// Parser for pseudo xml tag I use:
//
// Ejemplos de PseudoTag:
//  <INT id:_local_var scope:local>
//      Type=INT
//      id=_local_var
//      scope=local
//
//  <asignacion tipo:INT id:a[0] scope:local>
//      getType()   asignacion
//      get("tipo") INT
//      get("id")   a[0]
//      get("scope") local
//
public class PseudoTag
{
	private String raw;
	private Hashtable <String, String> _hashtable;
	private String _type;

	public PseudoTag(String raw)
	{
		this(raw, true);
	}

	public PseudoTag(String raw, boolean failSilently)
	{
		this.raw = raw.trim();
		_hashtable = new Hashtable <String,String>();

		try
		{
			parse();
		}
		catch(Exception e)
		{
			if (!failSilently)
			{
				System.out.println("Error al parsear pseudo-tag:" + raw);
				System.out.println(e);
			}
		}
	}

	private void parse() throws Exception
	{
		if (raw.charAt(0) != '<' || raw.charAt(raw.length() - 1) != '>')
		{
			throw new Exception("Malformed string");
		}
		
		_type = raw.substring(1, raw.indexOf(' '));

		String propiedades = raw.substring(raw.indexOf(' ') + 1, raw.length() - 1);
		int inicio = 0;
		int idx = 0;

		while (idx < propiedades.length())
		{
			while (propiedades.charAt(idx) != ':')
			{
				idx++;
			}

			String llave = propiedades.substring(inicio, idx);
			idx++;
			boolean esCadena = propiedades.charAt(idx) == '\"';
			char busqueda = esCadena ? '\"' : ' ';

			if (esCadena) idx++;
			
			inicio = idx++;

			while (idx < propiedades.length() && propiedades.charAt(idx) != busqueda)
			{
				idx++;
			}

			_hashtable.put(llave, propiedades.substring(inicio, idx));

			inicio = idx + 1;
		}
	}

	public String get(String key)
	{
		return _hashtable.get(key);
	}

	public String set(String key, int val)
	{
		return _hashtable.put(key, String.valueOf(val));
	}

	public String set(String key, String val)
	{
		return _hashtable.put(key, val);
	}
}

