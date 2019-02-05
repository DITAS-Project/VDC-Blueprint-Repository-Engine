/*
 * This file is part of VDC-Blueprint-Repository-Engine.
 * 
 * VDC-Blueprint-Repository-Engine is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 * 
 * VDC-Blueprint-Repository-Engine is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with VDC-Blueprint-Repository-Engine.  
 * If not, see <https://www.gnu.org/licenses/>.
 * 
 * VDC-Blueprint-Repository-Engine is being developed for the
 * DITAS Project: https://www.ditas-project.eu/
 */
package gr.ntua.ditas.repositoryengine.restheartcustomization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.metadata.checkers.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ElasticInsertionChecker implements Checker {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.checkers.Checker");
	
	@Override
	public boolean check(HttpServerExchange exchange, RequestContext context, BsonDocument contentToCheck, BsonValue args) {
		LOGGER.debug("ElasticInsertionChecker is started...");
		
		String id = contentToCheck.getObjectId("_id").getValue().toHexString();
		BsonDocument elastic_doc = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Overview");
		elastic_doc.remove("name");
		LOGGER.debug("blueprint for elastic : "+elastic_doc.toJson());
		
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("publicUser", "Resolution");
		provider.setCredentials(AuthScope.ANY, credentials);
		
		
		HttpClient httpClient = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				.build();
		
		
		
		JSONParser parser = new JSONParser();    
        
        File f = new File("app/config.json");
        FileReader conf = null;
            try {
                conf = new FileReader(f);
            } catch (FileNotFoundException ex) {
                java.util.logging.Logger.getLogger(ElasticInsertionChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
         
        
        JSONObject obj = new JSONObject();
            try {
                obj = (JSONObject) parser.parse(conf);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ElasticInsertionChecker.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(ElasticInsertionChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        
        String ip = obj.get("elastic_search_host").toString();
		
		
		
		
	   
		try {
			HttpPut request = new HttpPut("http://"+ip+":50014/ditas/blueprints/"+id);
			StringEntity params =new StringEntity(elastic_doc.toJson());
			request.addHeader("content-type", "application/json");
			request.setEntity(params);
			HttpResponse response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_OK) return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return false;
	}

    @Override
    public PHASE getPhase(RequestContext context) {
    	return PHASE.AFTER_WRITE;
    }

    @Override
    public boolean doesSupportRequests(RequestContext context) {
    	BsonDocument update = context.getContent().asDocument();
    	if (context.isPatch()) {
    		if (!(update.containsKey("INTERNAL_STRUCTURE")
    			|| update.containsKey("INTERNAL_STRUCTURE.Overview")
    			|| update.containsKey("INTERNAL_STRUCTURE.Overview.description")
    			||update.containsKey("INTERNAL_STRUCTURE.Overview.tags")))
    					return false;
    	}
    	return true;
    }

}
