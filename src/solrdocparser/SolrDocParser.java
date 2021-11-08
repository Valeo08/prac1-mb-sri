package solrdocparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.solr.common.SolrInputDocument;

/**
 * @author Valeo
 */
public class SolrDocParser {
    
    public static ArrayList<SolrInputDocument> parseDocs(final String path) {
        ArrayList<SolrInputDocument> docs = new ArrayList<>();
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            
            String line;
            SolrInputDocument doc = null;
            
            // Bandera para indicar el estado en el que se encuentra
            // el documento actual. Como si fuera una maquina de estados.
            /**
             * Estado 0 - Sin inicializar
             * Estado 1 - Inicializado
             * Estado 2 - Agregando titulo
             * Estado 3 - Agregando autor
             * Estado 4 - Agregando texto
             * Estado 5 - Obviando lineas
             */
            int state = 0;
            
            // Para el texto del titulo y descripcion
            StringBuilder tempTitle = new StringBuilder();
            StringBuilder tempText = new StringBuilder();
            
            while ((line = br.readLine()) != null) {
                if (line.length() > 1) {
                    String init = line.substring(0,2);
                
                    switch (init) {
                        case ".I": {
                            if (doc != null) {
                                doc.addField("title",
                                    remove_incorrect_whitespaces(tempTitle.toString()));
                                doc.addField("text_en", 
                                    remove_incorrect_whitespaces(tempText.toString()));
                                docs.add(doc);
                            }
                            
                            doc = new SolrInputDocument();
                            tempTitle = new StringBuilder();
                            tempText = new StringBuilder();
                            
                            doc.addField("id",
                                    line.replaceAll(" ", "").split("I")[1]);
                            state = 1;
                            break;
                        }
                        case ".T":
                            state = 2;
                            continue;
                        case ".A":
                            state = 3;
                            continue;
                        case ".W":
                            state = 4;
                            continue;
                        case ".X":
                            state = 5;
                            continue;
                    }

                    switch (state) {
                        case 2: // Agregando el titulo
                            tempTitle.append(line).append(" ");
                            break;
                        case 3: // Agregando el autor
                            doc.addField("author", line);
                            break;
                        case 4: // Leyendo el texto de descripcion
                            tempText.append(line).append(" ");
                            break;
                        case 5: // Obviando lineas
                            break;
                    }
                }
            }
            
            if (line == null && doc != null) {
                doc.addField("title",
                        remove_incorrect_whitespaces(tempTitle.toString()));
                doc.addField("text_en", 
                        remove_incorrect_whitespaces(tempText.toString()));
                docs.add(doc);
            }
        } catch (IOException ex) {
            System.out.println("An error occurred!");
            ex.printStackTrace();
        }
        
        return docs;
    }
    
    // Método auxiliar para eliminar los espacios al principio
    // final de un String
    private static String remove_incorrect_whitespaces(final String base) {
        return base.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
    }
    
}
