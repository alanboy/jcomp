package jcomp.util;

public class HashString
{
	public static String hash(String s)
	{
		if (s == null) return s;
		return s.replaceAll(" ", "");
	}

}
