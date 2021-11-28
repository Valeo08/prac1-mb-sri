package miclientesolrj;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import solrparser.SolrQueryParser;

/**
 * @author Valeo
 */
public class MiClienteGenTrecEvalSolrj {
    
    public static void main(String[] args) throws IOException,
            SolrServerException {
        
        HttpSolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/gettingstarted").build();

        // Primero generamos el fichero trec_solr_file con las consultas
        // Ruta al fichero de documentos del corpus
        String path = "./docs_to_import/CISI.QRY";

        QueryResponse rsp;
        ArrayList<SolrQuery> queries = SolrQueryParser.parseQueries(path);
        ArrayList<SolrDocumentList> results = new ArrayList<>();

        for (SolrQuery q : queries) {
            rsp = solr.query(q);
            results.add(rsp.getResults());
        }
        
        String trecFileName = "./trec_eval_gen/trec_solr_file";
        File trecFile = new File(trecFileName);
        
        if (trecFile.exists()) trecFile.delete();
        
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(trecFile));
            
            for (int i = 0; i < results.size(); i++) {
                for (int j = 0; j < results.get(i).size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    
                    // Número de consulta
                    sb.append(i+1).append(" ");
                    
                    // Q0 - ?????????
                    sb.append("Q0 ");
                    
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
        
        
        // Generamos a continuación el fichero trec_rel_file
        
    }
    
}
