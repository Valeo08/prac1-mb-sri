package miclientesolrj;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import solrdocparser.SolrDocParser;

/**
 * @author Valeo
 */
public class MiClienteAddDocsSolrj {
    
    public static void main(String[] args) throws IOException,
			SolrServerException {

        HttpSolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/gettingstarted").build();

        // Ruta al fichero de docuementos del corpus
        String path = "./docs_to_import/CISI.ALL";
        
        ArrayList<SolrInputDocument> docs = SolrDocParser.parseDocs(path);

        for (SolrInputDocument doc : docs)
            solr.add(doc);
        
        solr.commit();
    }
    
}
