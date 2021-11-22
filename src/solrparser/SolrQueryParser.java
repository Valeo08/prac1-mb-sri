package solrparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrQuery;

/**
 * @author Valeo
 */
public class SolrQueryParser {
    
    public static ArrayList<SolrQuery> parseQueries(final String path) {
        ArrayList<SolrQuery> queries = new ArrayList<>();
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            
            SolrQuery qry = null;
            
            // Bandera para indicar el estado en el que se encuentra
            // la consulta actual. Como si fuera una maquina de estados.
            /**
             * Estado 0 - Sin inicializar
             * Estado 1 - Leyendo ID
             * Estado 2 - Leyendo titulo
             * Estado 3 - Leyendo autor
             * Estado 4 - Leyendo texto
             * Estado 5 - Obviando lineas
             */
            int state = 0;
            
            // Para el texto del titulo, autor, y descripcion
            StringBuilder tempTitle = new StringBuilder();
            StringBuilder tempAuthor = new StringBuilder();
            StringBuilder tempText = new StringBuilder();
            
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;
                
                if (line.length() > 1) {
                    String init = line.substring(0,2);
                    
                    switch (init) {
                        case ".I": // Leyendo ID
                            if (qry != null) {
                                setQuery(qry, tempTitle, tempAuthor, tempText);
                                queries.add(qry);
                            }
                            
                            qry = new SolrQuery();
                            qry.addSort("score", SolrQuery.ORDER.desc);
                            qry.setRows(2000);
                            qry.addField("id");
                            qry.addField("title");
                            qry.addField("score");
                            
                            tempTitle = new StringBuilder();
                            tempAuthor = new StringBuilder();
                            tempText = new StringBuilder();
                            
                            state = 1;
                            continue;
                        case ".T": // Leyendo titulo
                            state = 2;
                            continue;
                        case ".A": // Leyendo autor
                            state = 3;
                            continue;
                        case ".W": // Leyendo texto
                            state = 4;
                            continue;
                        case ".B": // Obviando lineas - No considerado
                            state = 5;
                            continue;
                    }
                }
                
                switch (state) {
                    case 2: // Leyendo titulo
                    {
                        String[] tmp = 
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(line)).split(" ");
                        for (int i = 0; i < tmp.length && i != 6; i++)
                            tempTitle.append(tmp[i]).append(" ");
                    }
                    state = 5;
                        break;
                    case 3: // Leyendo autor
                    {
                        String[] tmp = 
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(line)).split(" ");
                        for (int i = 0; i < tmp.length && i != 6; i++)
                            tempAuthor.append(tmp[i]).append(" ");
                    }
                    state = 5;
                        break;
                    case 4: // Leyendo texto
                    {
                        String[] tmp = 
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(line)).split(" ");
                        for (int i = 0; i < tmp.length && i != 6; i++)
                            tempText.append(tmp[i]).append(" ");
                    }
                    state = 5;
                        break;
                    case 5: // Obviando lineas - No considerado
                        break;
                }
            }
            
            if (line == null && qry != null) {
                setQuery(qry, tempTitle, tempAuthor, tempText);
                queries.add(qry);
            }
            
        } catch (IOException ex) {
            System.out.println("An error occurred!");
            ex.printStackTrace();
        }
        
        return queries;
    }
    
    // Método para establecer la consulta adecuada dependiendo del contenido
    // leído del fichero correspondiente
    private static void setQuery(SolrQuery q, StringBuilder title,
            StringBuilder author, StringBuilder text) {
        
        if (title.length() != 0) {
            q.setQuery("title:" + 
                    remove_incorrect_whitespaces(title.toString()));
            return;
        }
        
        if (author.length() != 0) {
            q.setQuery("author:" + 
                    remove_incorrect_whitespaces(author.toString()));
            return;
        }
        
        if (text.length() != 0)
            q.setQuery("text_en:" + 
                    remove_incorrect_whitespaces(text.toString()));
        
    }
    
    // Método auxiliar para eliminar los espacios al principio
    // final de un String
    private static String remove_incorrect_whitespaces(final String base) {
        return base.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
    }
    
    private static String remove_incorrect_symbols(final String base) {
        return base.replaceAll("[^a-zA-Z0-9' ']", "");
    }
    
}
