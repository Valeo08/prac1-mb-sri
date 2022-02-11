package solrparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Valeo
 */
public class SolrQueryParser {
    
    // Método para parsear las queries de un documento en el formato
    // que Solr acepta.
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
            
            // Para leer las anotaciones de GATE
            DocumentBuilder newDocumentBuilder;
            Document parse;
            NodeList root, nl;
            
            ArrayList<StringBuilder> annotations = null;
            
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;
                
                if (line.length() > 1) {
                    String init = line.substring(0,2);
                    
                    switch (init) {
                        case ".I": // Leyendo ID
                            if (qry != null) {
                                setQuery(qry, tempTitle, tempAuthor, 
                                        tempText, annotations);
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
                            
                            annotations = new ArrayList<>();
                            for (int i = 0; i < 8; i++)
                                annotations.add(new StringBuilder());
                            
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
                
                String texto;
                try {
                    // Tratar la anotaciones de GATE
                    String textP = "<root>" + line + "</root>";
                    newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    newDocumentBuilder.setErrorHandler(null);
                    parse = newDocumentBuilder.parse(new ByteArrayInputStream(textP.getBytes()));
                    root = parse.getChildNodes();
                    nl = root.item(0).getChildNodes();
                    texto = root.item(0).getTextContent();
                    
                    if (qry != null) {
                        for (int i = 0; i < nl.getLength(); i++) {
                            if (!nl.item(i).getNodeName().equals("#text")) {
                                String attNamePre = nl.item(i).getNodeName();
                                String attValue = nl.item(i).getTextContent();
                                String attName = getAnnotationIndexName(attNamePre);
                                int attInd = getAnnotationIndexNumber(attNamePre);
                                
                                if (annotations != null) {
                                    if (annotations.get(attInd).length() == 0)
                                        annotations.get(attInd).append(attName)
                                                .append(":").append(attValue);
                                    else
                                        annotations.get(attInd).append(" OR ")
                                                .append(attName).append(":").append(attValue);
                                }
                                
                            }
                        }
                    }
                } catch (ParserConfigurationException | SAXException ex) {
                    // Si hay un error al parsear se obvian las anotaciones
                    texto = line;
                    //System.out.println("An error occurred reading annotations!");
                }
                
                // Agregar el titulo, el autor y la descripcion
                switch (state) {
                    case 2: // Leyendo titulo
                        tempTitle.append(
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(texto))).append(" ");
                        break;
                    case 3: // Leyendo autor
                        tempAuthor.append(
                                    remove_incorrect_symbols(
                                            remove_incorrect_whitespaces(texto))).append(" ");
                        break;
                    case 4: // Leyendo texto
                        tempText.append(
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(texto))).append(" ");
                        break;
                    case 5: // Obviando lineas - No considerado
                        break;
                }
            }
            
            if (line == null && qry != null) {
                setQuery(qry, tempTitle, tempAuthor, tempText, annotations);
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
            StringBuilder author, StringBuilder text,
            ArrayList<StringBuilder> annotations) {
        StringBuilder qryText = new StringBuilder();
        
        // Agregar los campos de descripcion, autor y titulo a la consulta
        if (text.length() != 0)
            qryText.append("text_en:").append(remove_incorrect_whitespaces(text.toString()));
        
        if (author.length() != 0) {
            if (qryText.length() != 0)
                qryText.append(" OR author:").append(remove_incorrect_whitespaces(author.toString()));
            else qryText.append(remove_incorrect_whitespaces(author.toString()));
        }
         
        if (title.length() != 0) {
            if (qryText.length() != 0)
                qryText.append(" OR title:").append(remove_incorrect_whitespaces(title.toString()));
            else qryText.append(remove_incorrect_whitespaces(title.toString()));
        }
        
        // Agregar las anotaciones de GATE a la consulta
        for (StringBuilder sb : annotations) {
            if (sb.length() != 0)
                qryText.append(" OR ").append(sb.toString());
        }
        
        q.setQuery(qryText.toString());
    }
    
    // Método para generar un fichero de tipo trec_top_file para la
    // evaluación de nuestro SRI mediante TREC_EVAL
    public static void generateTrecEvalFile(final String trecFileName, 
            final ArrayList<SolrQuery> queries, final HttpSolrClient solr)
        throws SolrServerException, IOException {
        QueryResponse rsp;
        ArrayList<SolrDocumentList> results = new ArrayList<>();

        for (SolrQuery q : queries) {
            rsp = solr.query(q);
            results.add(rsp.getResults());
        }
        
        File trecFile = new File(trecFileName);
        
        if (trecFile.exists()) trecFile.delete();
        
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(trecFile));
            
            for (int i = 0; i < results.size(); i++) {
                for (int j = 0; j < results.get(i).size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    
                    // Número de consulta
                    sb.append(i+1).append(" ");
                    
                    // Iteración
                    sb.append("Q-01 ");
                    
                    // ID documento
                    sb.append(results.get(i).get(j)
                            .getFieldValue("id")).append(" ");
                    
                    // Ranking
                    sb.append(j).append(" ");
                    
                    // Score
                    sb.append(results.get(i).get(j)
                            .getFieldValue("score")).append(" ");
                    
                    // Equipo
                    sb.append("VALEO");
                    
                    sb.append("\n");
                    bw.write(sb.toString());
                }
            }
            
            bw.close();
        } catch (IOException ex) {
            System.out.println("An error occurred!");
            ex.printStackTrace();
        }
    }
    
    // Obtener el nombre para indexar la anotacion correspondiente
    private static String getAnnotationIndexName(String annotName) {
        String attName;
        switch (annotName) {
            case "Person":
                attName = "personas";
                break;
            case "Location":
                attName = "lugares";
                break;
            case "Organization":
                attName = "organizaciones";
                break;
            case "Date":
                attName = "fechas";
                break;
            case "Identifier":
                attName = "identificadores";
                break;
            case "Money":
                attName = "dinero";
                break;
            case "Percent":
                attName = "porcentajes";
                break;
            case "Unknown":
            default:
                attName = "tdesconocidos";
                break;
        }
        return attName;
    }
    
    // Obtener el nombre para indexar la anotacion correspondiente
    private static int getAnnotationIndexNumber(String annotName) {
        int attNumber;
        switch (annotName) {
            case "Person":
                attNumber = 0;
                break;
            case "Location":
                attNumber = 1;
                break;
            case "Organization":
                attNumber = 2;
                break;
            case "Date":
                attNumber = 3;
                break;
            case "Identifier":
                attNumber = 4;
                break;
            case "Money":
                attNumber = 5;
                break;
            case "Percent":
                attNumber = 6;
                break;
            case "Unknown":
            default:
                attNumber = 7;
                break;
        }
        return attNumber;
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
