package solrparser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
            
            // Para el texto del titulo, el autor y la descripcion
            StringBuilder tempTitle = new StringBuilder();
            StringBuilder tempAuthor = new StringBuilder();
            StringBuilder tempText = new StringBuilder();
            
            // Para leer las anotaciones de GATE
            DocumentBuilder newDocumentBuilder;
            Document parse;
            NodeList root, nl;
            
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue; // Linea en blanco
                
                if (line.length() > 1) {
                    String init = line.substring(0,2);
                
                    switch (init) {
                        case ".I": {
                            if (doc != null) {
                                doc.addField("title",
                                        remove_incorrect_whitespaces(tempTitle.toString()));
                                doc.addField("author",
                                        remove_incorrect_whitespaces(tempAuthor.toString()));
                                doc.addField("text_en", 
                                        remove_incorrect_whitespaces(tempText.toString()));
                                docs.add(doc);
                            }
                            
                            doc = new SolrInputDocument();
                            tempTitle = new StringBuilder();
                            tempAuthor = new StringBuilder();
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
                }
                
                String texto;
                try {
                    // Tratar las anotaciones de GATE
                    String textP = "<root>" + line + "</root>";
                    newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    newDocumentBuilder.setErrorHandler(null);
                    parse = newDocumentBuilder.parse(new ByteArrayInputStream(textP.getBytes()));
                    root = parse.getChildNodes();
                    nl = root.item(0).getChildNodes();
                    texto = root.item(0).getTextContent();
                    
                    if (doc != null) {
                        for (int i = 0; i < nl.getLength(); i++) {
                            if (!nl.item(i).getNodeName().equals("#text")) {
                                String attNamePre = nl.item(i).getNodeName();
                                String attName = getAnnotationIndexName(attNamePre);
                                String attValue = nl.item(i).getTextContent();
                                doc.addField(attName, attValue);
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
                    case 2: // Agregando el titulo
                        tempTitle.append(
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(texto))).append(" ");
                        break;
                    case 3: // Agregando el autor
                        tempAuthor.append(
                                    remove_incorrect_symbols(
                                            remove_incorrect_whitespaces(texto))).append(" ");
                        break;
                    case 4: // Leyendo el texto de descripcion
                        tempText.append(
                                remove_incorrect_symbols(
                                        remove_incorrect_whitespaces(texto))).append(" ");
                        break;
                    case 5: // Obviando lineas
                        break;
                }
            }
            
            if (line == null && doc != null) {
                doc.addField("title",
                        remove_incorrect_whitespaces(tempTitle.toString()));
                doc.addField("author",
                        remove_incorrect_whitespaces(tempAuthor.toString()));
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
    
    // Método auxiliar para eliminar los espacios al principio
    // final de un String
    private static String remove_incorrect_whitespaces(final String base) {
        return base.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
    }
    
    private static String remove_incorrect_symbols(final String base) {
        return base.replaceAll("[^a-zA-Z0-9' ']", "");
    }
    
}
