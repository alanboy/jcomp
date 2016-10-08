
package jcomp.backend;

import java.io.*;
import jcomp.util.Log;
import jcomp.util.PseudoTag;

public class GeneracionDeCodigoAsm
{
	private Log logger;
	final private String codigoIntermedio;

	public GeneracionDeCodigoAsm(String codigo)
	{
		this.logger = Log.getInstance();
		this.codigoIntermedio = codigo;
	}

	private void escribirArchivo(String codigo, String nombreDeArchivo)
	{

		logger.imprimirLinea("Escribiendo " + nombreDeArchivo + " ...");

		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(nombreDeArchivo)));
			pw.print(codigo);
			pw.close();

		} catch(IOException ioe) {
			System.out.println("Error creando archivo " + nombreDeArchivo);

		}
	}

	public int iniciar()
	{
		// El codigo intermedio esta en this.codigoIntermedio
		// hay que convertlo en cada una de los 3 
		// diferentes Sistemas Operativos soportados
		//    Windows
		//    Linux
		//    Valdosta

		logger.imprimirEncabezado("Generacion de codigo dependiente de maquina/SO");
		
		String codigoX86Windows = x86Windows();
		String codigoX86Linux = x86Linux();
		String codigoX86Valdosta = x86Valdosta();


		// imprimir los 3 codigos en el log
		String [][] multiCodigo = new String[3][];
		multiCodigo[0] = codigoX86Windows.split("\n");
		multiCodigo[1] = codigoX86Linux.split("\n");
		multiCodigo[2] = codigoX86Valdosta.split("\n");

		// imprimir cada codigo linea por linea
		int caracteresParaCadaCodigo = 30;
		for (int i  = 0; ;i++)
		{
			// revisar si aun hay mas lineas en por lo menos uno de los codigos
			boolean porLoMenos1 = false;
			for (int j = 0 ; j < 3; j++)
			{
				if (multiCodigo[j].length > i)
				{
					porLoMenos1 = true;
				}
			}

			if (!porLoMenos1)
			{
				break;
			}

			// imprimir el numero de linea
			if ((i+1) < 10) {
				logger.imprimir((i+1) + " :");
			} else {
				logger.imprimir((i+1) + ":");
			}

			for (int j = 0 ; j < 3; j++)
			{
				int len = 0;

				if (multiCodigo[j].length <= i)
				{
					// Este codigo ya termino

				}
				else
				{
					len = multiCodigo[j][i].length();
					logger.imprimir("| " + multiCodigo[j][i].substring(0, Math.min(len, caracteresParaCadaCodigo)));
				}

				String padding = "";
				while (len < caracteresParaCadaCodigo)
				{
					padding += " ";
					len++;
				}
				logger.imprimir(padding);
			}

			logger.imprimir(" |\n");
		}

		logger.imprimir("");

		escribirArchivo(codigoX86Windows, "out.windows.x86.asm");
		escribirArchivo(codigoX86Linux, "out.linux.x86.asm");
		escribirArchivo(codigoX86Valdosta, "out.valdosta.x86.asm");

		return 0;
	}

	private String x86Linux()
	{
		String lineas [] = this.codigoIntermedio.split("\n");
		String codigoNativo = "; Codigo para x86 linux\n";

		for (int a = 0; a < lineas.length; a++)
		{
			if (lineas[a].trim().startsWith(";"))
			{
				codigoNativo += lineas[a]+"\n";
				continue;
			}

			if (lineas[a].indexOf("empujar ") != -1 )
			{
				lineas[a] = lineas[a].substring( 9 );
				lineas[a] = "  push "+lineas[a];
			}

			if (lineas[a].indexOf("empujarapuntador") != -1 )
			{
				lineas[a] = lineas[a].substring( 18 );
				lineas[a] = "  push dword ["+lineas[a] +"]";
			}

			if (lineas[a].indexOf("asignaAGlobal") != -1 )
			{
				String id = lineas[a].trim().substring(lineas[a].trim().indexOf(" ")+1);
				lineas[a] = "  ; asignacion a global\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  mov [";
				lineas[a] += id;
				lineas[a] += "], eax\n";
			}

			if (lineas[a].indexOf("SUMA") != -1 )
			{
				lineas[a] = "  ; suma\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  add eax, ebx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("RESTA") != -1 )
			{
				lineas[a] = "  ; resta\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  sub ebx, eax\n";
				lineas[a] += "  push ebx\n";
			}

			if (lineas[a].indexOf("MUL") != -1 )
			{
				lineas[a] = "\n	pop ax\n";
				lineas[a] += "  pop bx\n";
				lineas[a] += "  mul bx\n";
				lineas[a] += "  push ax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] = "\n\t; return explicito de funcion \n";
				lineas[a] += "  pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "  mov esp, ebp\n";
				lineas[a] += "  pop ebp\n";
				lineas[a] += "  ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main sinfica hacer syscall
				lineas[a] = "\n\t; return explicito \n";
				lineas[a] += "  mov eax, 1\n"; // Syscall para salir del proces (sys_exit)
				lineas[a] += "  pop ebx\n";    // El valor a regresar (exit value)
				lineas[a] += "  int 80h\n";
			}

			if (lineas[a].indexOf("MAYOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jl while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("MENOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n	pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n	jmp while_cond\n";
				lineas[a] += "while_fin:\n";
			}

			codigoNativo += lineas[a]+"\n";
		} // for de cada linea


		// ahora hay que agregar los metodos pre-escritos, la "libreria"
		String [] asm = new String[]
		{
			"putc:",
			"  push ebp",
			"  mov ebp, esp",

			"  mov eax, ebp",
			"  mov ebx, 8",
			"  add eax, ebx",
			"  push eax            ; calcular la direccion del primer argumento",
			"  pop ecx             ; ebp+8 y ponerla en ecx ",

			"  mov eax,4           ; la system call para escribir en la pantalla (sys_write)",
			"  mov ebx,1           ; file descriptor 1 - standard output",
			"  mov edx,1           ; la longitud de bytes que queremos imprimir",
			"  int 80h             ; llamar al kernel",

			"  mov esp, ebp",
			"  pop ebp",
			"  ret",
			"  ; fin de putc"
		};

		return codigoNativo + join(asm, '\n');
	}

	private String x86Windows()
	{
		String lineas [] = this.codigoIntermedio.split("\n");
		String codigoNativo = "; Codigo para x86 Windows\n";

		for (int a = 0; a < lineas.length; a++)
		{
			if (lineas[a].trim().startsWith(";"))
			{
				codigoNativo += lineas[a]+"\n";
				continue;
			}

			if (lineas[a].indexOf("empujar ") != -1 )
			{
				lineas[a] = lineas[a].substring( 9 );
				lineas[a] = "  push "+lineas[a];
			}

			if (lineas[a].indexOf("empujarapuntador") != -1 )
			{
				lineas[a] = lineas[a].substring( 18 );
				lineas[a] = "  push dword ["+lineas[a] +"]";
			}

			if (lineas[a].indexOf("asignaAGlobal") != -1 )
			{
				String id = lineas[a].trim().substring(lineas[a].trim().indexOf(" ")+1);
				lineas[a] = "  ; asignacion a global\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  mov [";
				lineas[a] += id;
				lineas[a] += "], eax\n";
			}

			if (lineas[a].indexOf("SUMA") != -1 )
			{
				lineas[a] = "  ; suma\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  add eax, ebx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("RESTA") != -1 )
			{
				lineas[a] = "  ; resta\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  sub ebx, eax\n";
				lineas[a] += "  push ebx\n";
			}

			if (lineas[a].indexOf("MUL") != -1 )
			{
				lineas[a] = "\n	pop ax\n";
				lineas[a] += "  pop bx\n";
				lineas[a] += "  mul bx\n";
				lineas[a] += "  push ax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] = "\n\t; return explicito de funcion \n";
				lineas[a] += "  pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "  mov esp, ebp\n";
				lineas[a] += "  pop ebp\n";
				lineas[a] += "  ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main sinfica hacer syscall
				lineas[a] = "\n\t; return explicito \n";
				lineas[a] += "  mov eax, 1\n"; // Syscall para salir del proces (sys_exit)
				lineas[a] += "  pop ebx\n";    // El valor a regresar (exit value)
				lineas[a] += "  int 80h\n";
			}

			if (lineas[a].indexOf("MAYOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n	pop eax\n";
				lineas[a] += "  pop ebx\n";
		 		lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jl while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("MENOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n	pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n	jmp while_cond\n";
				lineas[a] += "while_fin:\n";
			}

			codigoNativo += lineas[a]+"\n";
		} // for de cada linea


		// ahora hay que agregar los metodos pre-escritos, la "libreria"
		String [] asm = new String[]
		{
			"putc:",
			"  push ebp",
			"  mov ebp, esp",

			"  mov eax, ebp",
			"  mov ebx, 8",
			"  add eax, ebx",
			"  push eax            ; calcular la direccion del primer argumento",
			"  pop ecx             ; ebp+8 y ponerla en ecx ",

			"  mov eax,4           ; la system call para escribir en la pantalla (sys_write)",
			"  mov ebx,1           ; file descriptor 1 - standard output",
			"  mov edx,1           ; la longitud de bytes que queremos imprimir",
			"  int 80h             ; llamar al kernel",

			"  mov esp, ebp",
			"  pop ebp",
			"  ret",
			"  ; fin de putc"
		};

		return codigoNativo + join(asm, '\n');
	}

	private String x86Valdosta()
	{
		String lineas [] = this.codigoIntermedio.split("\n");
		String codigoNativo = "; Codigo para x86 Valdosta\n";

		for (int a = 0; a < lineas.length; a++)
		{
			if (lineas[a].trim().startsWith(";"))
			{
				codigoNativo += lineas[a]+"\n";
				continue;
			}

			if (lineas[a].indexOf("empujar ") != -1 )
			{
				lineas[a] = lineas[a].substring( 9 );
				lineas[a] = "  push "+lineas[a];
			}

			if (lineas[a].indexOf("empujarapuntador") != -1 )
			{
				lineas[a] = lineas[a].substring( 18 );
				lineas[a] = "  push dword ["+lineas[a] +"]";
			}

			if (lineas[a].indexOf("asignaAGlobal") != -1 )
			{
				String id = lineas[a].trim().substring(lineas[a].trim().indexOf(" ")+1);
				lineas[a] = "  ; asignacion a global\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  mov [";
				lineas[a] += id;
				lineas[a] += "], eax\n";
			}

			if (lineas[a].indexOf("SUMA") != -1 )
			{
				lineas[a] = "  ; suma\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  add eax, ebx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("RESTA") != -1 )
			{
				lineas[a] = "  ; resta\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  sub ebx, eax\n";
				lineas[a] += "  push ebx\n";
			}

			if (lineas[a].indexOf("MUL") != -1 )
			{
				lineas[a] = "\n  pop ax\n";
				lineas[a] += "  pop bx\n";
				lineas[a] += "  mul bx\n";
				lineas[a] += "  push ax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] = "\n\t; return explicito de funcion \n";
				lineas[a] += "  pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "  mov esp, ebp\n";
				lineas[a] += "  pop ebp\n";
				lineas[a] += "  ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main sinfica hacer syscall
				lineas[a] = "\n\t; return explicito \n";
				lineas[a] += "  mov eax, 1\n"; // Syscall para salir del proces (sys_exit)
				lineas[a] += "  pop ebx\n";    // El valor a regresar (exit value)
				lineas[a] += "  int 80h\n";
			}

			if (lineas[a].indexOf("MAYOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jl while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("MENOR_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n	pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n	jmp while_cond\n";
				lineas[a] += "while_fin:\n";
			}

			codigoNativo += lineas[a]+"\n";
		} // for de cada linea


		// ahora hay que agregar los metodos pre-escritos, la "libreria"
		String [] asm = new String[]
		{
			"putc:",
			"  push ebp",
			"  mov ebp, esp",

			"  mov eax, ebp",
			"  mov ebx, 8",
			"  add eax, ebx",
			"  push eax            ; calcular la direccion del primer argumento",
			"  pop ecx             ; ebp+8 y ponerla en ecx ",

			"  mov eax,4           ; la system call para escribir en la pantalla (sys_write)",
			"  mov ebx,1           ; file descriptor 1 - standard output",
			"  mov edx,1           ; la longitud de bytes que queremos imprimir",
			"  int 80h             ; llamar al kernel",

			"  mov esp, ebp",
			"  pop ebp",
			"  ret",
			"  ; fin de putc"
		};

		return codigoNativo + join(asm, '\n');
	}

	private static String join(String [] a, char c)
	{
		StringBuilder s = new StringBuilder();
		for (String si : a)
		{
			s.append(si + c);
		}
		return s.toString();
	}
}
