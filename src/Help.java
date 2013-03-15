import java.io.*;


public class Help{

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

