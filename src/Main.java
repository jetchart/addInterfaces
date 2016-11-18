import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * La idea es agregar a las clases que se encuentren en un directorio las implementaciones de ciertas interfaces.
 * Se requieren 3 parámetros:<br>
 			1- Path donde están los .java<br>
			2- Path donde está la lista de interfaces a agregar (lista separada por enters)<br>
			3- Path donde está la lista de clases a no procesar (lista separada por enters)<br>
	En caso que la clase que se está procesando ya posee la interfaz, no la agrega.<br><br>
	ACLARACIÓN: Si una de las interfaces que se desea implementar es "Serializable", se requerirá generar manualmente el serial version ID
	
 * @author jetchart - 17/11/2016
 *
 */
public class Main {
	
	private static List<String> classesNotToProcess;
	private static List<String> interfaces;
	
	public static void main(String[] args) throws IOException {
		if (args.length < 3){
			System.out.println("Se requieren los siguientes argumentos:");
			System.out.println("\t1- Path donde están los .java");
			System.out.println("\t2- Path donde está la lista de interfaces a agregar (lista separada por enters)");
			System.out.println("\t3- Path donde está la lista de clases a no procesar (lista separada por enters)");
			return;
		}
		String pathJavaFiles = args[0];
		String pathInterfacesList = args[1];
		String pathClasesNotToProcessList = args[2];
		interfaces = Arrays.asList(leerArchivo(pathInterfacesList, " ").split(" "));
		classesNotToProcess = Arrays.asList(leerArchivo(pathClasesNotToProcessList, " ").split(" "));
		Collection<String> colFilesPath = getFilesFromFolder(new ArrayList<String>(),pathJavaFiles, Boolean.TRUE, "C");
		System.out.println("Cantidad de clases a procesar: " + colFilesPath.size());
		System.out.println("*************************");
		System.out.println("Inicio del procesamiento");
		System.out.println("*************************");
		for (String fullPath : colFilesPath){
			String className = fullPath.split("/")[fullPath.split("/").length-1].replace(".java", "");
			String contenidoOriginal = leerArchivo(fullPath,"\n");
			String contenidoNuevo = addInterfaces(className, contenidoOriginal);
			if (!contenidoOriginal.equals(contenidoNuevo)){
				writeFile(fullPath, contenidoNuevo);
			}
			
		}
		System.out.println("*************************");
		System.out.println("  Fin del procesamiento");
		System.out.println("*************************");
	}

	/**
	 * Obtiene todos los archivos a partir de un directorio raiz (si recursive=True 
	 * navega tambien sobre subdirectorios) que empiecen con el prefijo indicado
	 * 
	 * @param colFilesPath		Almacena los archivos encontrados (se requiere cuando recursive=TRUE, ya que irá acumulando los archivos)
	 * @param rootFolder		Ruta desde donde se inicia la búsqueda de archivos
	 * @param recursive 		Si es TRUE, busca tambien dentro de los subdirectorios
	 * @param prefix			Procesa los archivos .java que empiencen con el prefijo indicado
	 * 
	 * @return
	 */
	public static Collection<String> getFilesFromFolder(Collection<String> colFilesPath, String rootFolder, Boolean recursive, String prefix){
    	File f = new File(rootFolder);
    	File[] ficheros = f.listFiles();
    	for (File file : ficheros){
    		if (recursive && file.isDirectory()){
    			getFilesFromFolder(colFilesPath, file.getAbsolutePath(), recursive, prefix);
    		}else{
    			if (file.getName().startsWith(prefix) && file.getAbsolutePath().endsWith(".java")){
    				colFilesPath.add(file.getAbsolutePath());
    			}
    		}
    	}
    	return colFilesPath;
    }
	
	/**
	 * Leer el archivo indicado por parámetro. Cada linea que procesa la separa utilizando el "separator"
	 * 
	 * @param filePath
	 * @param separator
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static String leerArchivo(String filePath, String separator) throws UnsupportedEncodingException{
		StringBuffer retorno = new StringBuffer();
	    FileReader f;
	    BufferedReader b;
	    String cadena;
		try {
			/* Creo FileReader con la ruta completa del file recibido por parametro */
		    b = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "8859_1"));
		    /* Recorro el archivo */ 
		    while((cadena = b.readLine())!=null) {
		    	/* Agrego contenido al StringBuffer que será devuelto */
		    	retorno.append(new String(cadena)).append(separator);
		    }
		    b.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retorno.toString();
	}
	
	/**
	 * Dado el contenido de un .java, devuelve el mismo contenido con el agregado de la implementación de las clases 
	 * que se encuentran en la lista "interfaces". Además, no procesa las clases de la lista "classesNotToProcess". 
	 * 
	 * @param className
	 * @param contenidoOriginal
	 * @return
	 */
	private static String addInterfaces(String className, String contenidoOriginal) {

		if (classesNotToProcess.contains(className)){
			return contenidoOriginal;
		}
		
		Integer indexFrom = contenidoOriginal.indexOf("public class " + className);
		if (indexFrom.equals(-1)){
			return contenidoOriginal;
		}
		
		Integer indexTo = indexFrom + contenidoOriginal.substring(indexFrom).indexOf("{");
		String cabeceraOriginal = contenidoOriginal.substring(indexFrom, indexTo);
		String cabeceraNueva = null;
		Collection<String> intsAgregadas = new ArrayList<String>();
		/* Si se requiere agregar Serializable y no existe el import, se agrega */
		String importLine = !contenidoOriginal.contains("serialVersionUID") && interfaces.contains("Serializable") && !contenidoOriginal.contains("import java.io.Serializable;")?"import java.io.Serializable;":"";
		if (cabeceraOriginal.contains("implements")){
			Boolean huboCambio = Boolean.FALSE;
			String cabeceraOriginalSinTabs = cabeceraOriginal.replaceAll("\t", " ");
			Collection<String> intsOriginales = new ArrayList<>(Arrays.asList(cabeceraOriginalSinTabs.substring(cabeceraOriginalSinTabs.indexOf("implements")).replace("implements ", "").replace("implements", "").replace(" ", "").split(",")));
			for (String i : interfaces){
				if (!intsOriginales.contains(i)){
					/* Si se está intentando agregar la interfaz "Serializable" pero ya se definió un serialVersionUID no 
					 * debe agregarse, ya que el implements se hizo en otra clase que extiende o implementa esta misma.
					 * ¡Esto debería mejorarse! */
					if (!i.equals("Serializable") || (i.equals("Serializable") && !contenidoOriginal.contains("serialVersionUID"))){
						huboCambio = Boolean.TRUE;
						intsOriginales.add(i);
						intsAgregadas.add(i);
					}
				}
			}
			if (huboCambio){
				String ints = "implements ";
				for (String i : intsOriginales){
					ints += i + ", ";
				}
				ints = ints.substring(0,ints.length()-2);
				cabeceraNueva = cabeceraOriginal.replace(cabeceraOriginal.substring(cabeceraOriginal.indexOf("implements")), ints) + " ";
			}else{
				cabeceraNueva = cabeceraOriginal;
			}
		}else{
			String ints = " implements ";
			for (String i : interfaces){
				if (!i.equals("Serializable") || (i.equals("Serializable") && !contenidoOriginal.contains("serialVersionUID"))){
					ints += i + ", ";
					intsAgregadas.add(i);
				}
			}
			ints = ints.substring(0,ints.length()-2);
			cabeceraNueva = cabeceraOriginal + ints + " ";
		}
		if (!importLine.isEmpty()){
			Integer desde = contenidoOriginal.indexOf("package");
			Integer hasta = contenidoOriginal.indexOf(";")+1;
			String packageOriginal = contenidoOriginal.substring(desde,hasta);
			String packageNuevo = packageOriginal + "\n\n" + importLine;
			contenidoOriginal = contenidoOriginal.replace(packageOriginal, packageNuevo);
		}
		
		/* Muestro la clase y las interfaces agregadas */
		if (!intsAgregadas.isEmpty()){
			System.out.print(className + " --> ");
			for (String i : intsAgregadas){
				System.out.print(i + " ");
			}
			System.out.println("");
			return contenidoOriginal.replace(cabeceraOriginal, cabeceraNueva);
		}
		return contenidoOriginal;
	}
	
	/**
	 * Escribe el contenido sobre el archivo "filePath"
	 * 
	 * @param filePpath
	 * @param content
	 * @throws IOException
	 */
	private static void writeFile(String filePpath, String content) throws IOException{
		PrintWriter pw = new PrintWriter(new File(filePpath), "8859_1");
		pw.print(content);
		pw.close();
	}
}
