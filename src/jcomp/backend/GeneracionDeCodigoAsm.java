
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

		try
		{
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(nombreDeArchivo)));
			pw.print(codigo);
			pw.close();
		}
		catch(IOException ioe)
		{
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
				logger.imprimir((i+1) + "  ");
			} else if ((i+1) < 100) {
				logger.imprimir((i+1) + " ");
			} else {
				logger.imprimir((i+1) + "");
			}

			for (int j = 0 ; j < 3; j++)
			{
				int len = 0;

				logger.imprimir("| ");

				if (multiCodigo[j].length <= i)
				{
					// Este codigo ya termino
				}
				else
				{
					len = multiCodigo[j][i].length();
					logger.imprimir(multiCodigo[j][i].substring(0, Math.min(len, caracteresParaCadaCodigo)));
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

			if (lineas[a].indexOf("UsosExternos") != -1 )
			{
				lineas[a] = "";
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
				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  imul eax, ebx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("DIV") != -1 )
			{
				lineas[a] = "\n  pop ecx\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  mov edx, 0\n";
				lineas[a] += "  div ecx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] =  "  ; return explicito\n";
				lineas[a] += "  pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "  mov esp, ebp\n";
				lineas[a] += "  pop ebp\n";
				lineas[a] += "  ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main sinfica hacer syscall
				lineas[a] =  "  ; return explicito \n";
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

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je while_" + id + "_fin\n";
				lineas[a] += "  jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("IGUAL_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  jl while_" + id + "_fin\n";
				lineas[a] += "  jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n  jmp while_cond\n";
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

		return codigoNativo + String.join("\n", asm);
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

			if (lineas[a].indexOf("UsosExternos") != -1 )
			{
				lineas[a] = "  extern  _GetStdHandle@4\n";
				lineas[a] += "  extern  _WriteFile@20\n";
				lineas[a] += "  extern  _ReadFile@20\n";
				lineas[a] += "  extern  _ExitProcess@4\n";
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
				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  imul eax, ebx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("DIV") != -1 )
			{
				lineas[a] = "\n  pop ecx\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  mov edx, 0\n";
				lineas[a] += "  div ecx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] =  "  ; return explicito\n";
				lineas[a] += "  pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "  mov esp, ebp\n";
				lineas[a] += "  pop ebp\n";
				lineas[a] += "  ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main signfica hacer syscall para terminar
				// el proceso
				lineas[a]  = "  ; ExitProcess(0)\n";
				lineas[a] += "  call    _ExitProcess@4\n";
			}

			if (lineas[a].indexOf("MAYOR_") != -1 )
			{
				String linea = lineas[a].trim();
				String id = linea.substring(linea.indexOf("_")+1, linea.length() - 1);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
		 		lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je " + id + "_fin\n";
				lineas[a] += "  jl " + id + "_fin\n";
			}

			if (lineas[a].indexOf("MENOR_") != -1 )
			{
				String linea = lineas[a].trim();
				String id = linea.substring(linea.indexOf("_")+1, linea.length() - 1);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  je " + id + "_fin\n";
				lineas[a] += "  jg " + id + "_fin\n";
			}

			if (lineas[a].indexOf("IGUAL_") != -1 )
			{
				String linea = lineas[a].trim();
				String id = linea.substring(linea.indexOf("_")+1, linea.length() - 1);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  jne " + id + "_fin\n";
			}

			codigoNativo += lineas[a]+"\n";
		} // for de cada linea


		// ahora hay que agregar los metodos pre-escritos, la "libreria"
		String [] asm = new String[]
		{
			"putc:",
			"  push ebp",
			"  mov ebp, esp",
			"",
			"  mov eax, ebp",
			"  mov ebx, 8",
			"  add eax, ebx             ; calcular la direccion del primer argumento",
			"  mov ecx, eax             ; ebp+8 y ponerla en ecx ",
			"",
			"  ;;; hStdOut = GetstdHandle(STD_OUTPUT_HANDLE)",
			"  push    -11",
			"  call    _GetStdHandle@4",
			"  mov     ebx, eax",
			"",
			"  ;;; WriteFile( hstdOut, message, length(message), &bytes, 0);",
			"  push    0    ; flags",
			"  push    0    ; bytes written? can be nullptr",
			"  push    1    ; (message_end - message)",
			"  push    ecx  ; print message",
			"  push    ebx  ; handle from GetstdHandle",
			"  call    _WriteFile@20",
			"",
			"  mov  esp, ebp",
			"  pop ebp",
			"",
			"  ret",
			"  ; fin de putc",
			"",
			"getc:",
			"  push ebp",
			"  mov ebp, esp",
			"",
			"  sub esp, 4   ; espacio para una variable local",
			"",
			"  mov eax, ebp",
			"  mov ebx, 4",
			"  sub eax, ebx ; ",
			"  mov ecx, eax ; ebp-4 y ponerla en ecx ",
			"",
			"  ;;; hstdIn = GetstdHandle(STD_INPUT_HANDLE)",
			"  push    -10",
			"  call    _GetStdHandle@4",
			"  mov     ebx, eax",
			"",
			"  ;;; ReadFile( hstdIn, lpBuffer, nNumberOfBytesToRead, lpNumberOfBytesRead, lpOverlapped );",
			"  push    0    ; lpOverlapped",
			"  push    0    ; lpNumberOfBytesRead",
			"  push    1    ; nNumberOfBytesToRead",
			"  push    ecx  ; ",
			"  push    ebx  ; ",
			"  call    _ReadFile@20",
			"",
			"  mov eax, DWORD [ebp-4]",
			"",
			"  mov  esp, ebp",
			"  pop ebp",
			"",
			"  ret",
			"  ; fin de putc"
		};

		return codigoNativo + String.join("\n", asm);
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

			if (lineas[a].indexOf("UsosExternos") != -1 )
			{
				lineas[a] = "";
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
				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  imul eax, ebx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("DIV") != -1 )
			{
				lineas[a] = "\n  pop ecx\n";
				lineas[a] += "  pop eax\n";
				lineas[a] += "  mov edx, 0\n";
				lineas[a] += "  div ecx\n";
				lineas[a] += "  push eax\n";
			}

			if (lineas[a].indexOf("retornar") != -1 )
			{
				// Retornar de otra function es RET
				lineas[a] =  "  ; return explicito\n";
				lineas[a] += "  pop eax\n";    // El valor a regresar (exit value)
				lineas[a] += "  mov esp, ebp\n";
				lineas[a] += "  pop ebp\n";
				lineas[a] += "  ret\n";
			}

			if (lineas[a].indexOf("salir") != -1 )
			{
				// Retornar de main sinfica hacer syscall
				lineas[a] =  "  ; return explicito \n";
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

			if (lineas[a].indexOf("IGUAL_") != -1 )
			{
				String id = lineas[a].trim().split("_")[1];
				id = id.substring(0, id.length() - 2);

				lineas[a] = "\n  pop eax\n";
				lineas[a] += "  pop ebx\n";
				lineas[a] += "  cmp ebx, eax\n";
				lineas[a] += "  jl while_" + id + "_fin\n";
				lineas[a] += "  jg while_" + id + "_fin\n";
			}

			if (lineas[a].indexOf("while_fin:") != -1 )
			{
				lineas[a] = "\n  jmp while_cond\n";
				lineas[a] += "while_fin:\n";
			}

			codigoNativo += lineas[a]+"\n";
		} // for de cada linea


		// ahora hay que agregar los metodos pre-escritos, la "libreria"
		String [] asm = new String[]
		{
			"",
			"putc:",
			"  push ebp",
			"  mov ebp, esp",

			"  push DWORD [ebp+8]  ; first argumetn is address ebp+8, dereference to",
			"                      ; get character to print and push it",

			"  pop ebx             ; pop it to ebx which contains char to print",
			"  mov eax, 1;         ; syscall 1 is print char",
			"  int 100;            ; return code is in eax - 0 means success",

			"  mov esp, ebp",
			"  pop ebp",
			"  ret",
			"  ; fin de putc",
			"",
			"",
			"getc:",
			"  push ebp",
			"  mov ebp, esp",
			"",
			"  mov eax, 2         ; syscall 2 is get char",
			"  int 100",
			"",
			"  mov eax, ebx",
			"",
			"  mov esp, ebp",
			"  pop ebp",
			"  ret",
			"  ; fin de getc"
		};

		return codigoNativo + String.join("\n", asm);
	}
}
