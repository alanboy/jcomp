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
	private String _raw;
	private Hashtable <String, String> _hashtable;
	private String _type;

	public PseudoTag(String raw)
	{
		this(raw, true);
	}

	public PseudoTag(String raw, boolean failSilently)
	{
		_raw = raw.trim();
		_hashtable = new Hashtable <String,String>();
		try{
			parse();
		}catch(Exception e){
			if (!failSilently)
			{
				System.out.println("Error al parsear pseudo-tag:" + _raw);
				System.out.println(e);
			}
		}
	}

	private void parse() throws Exception
	{
		// Test for < >
		int len = _raw.length()-1;

		if (_raw.charAt(0) != '<' || _raw.charAt(len) != '>')
		{
			throw new Exception("Malformed string.");
		}

		String parts [] = _raw.substring(1, len).split(" ");

		// First part is the type
		_type = parts[0];

		for(int i = 1; i < parts.length; i++)
		{
			String [] keyValue = parts[i].split(":");
			_hashtable.put(keyValue[0], keyValue[1]);
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

