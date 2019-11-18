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
import org.restheart.metadata.checkers.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.undertow.server.HttpServerExchange;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ElasticInsertionChecker implements Checker {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.checkers.Checker");
	
	@Override
	public boolean check(HttpServerExchange exchange, RequestContext context, BsonDocument contentToCheck, BsonValue args) {

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


		String Esip = obj.get("elastic_search_host").toString();
		String Esauth = obj.get("elastic_search_auth").toString();
		String Esuser = obj.get("elastic_search_user").toString();
		String Espass = obj.get("elastic_search_pass").toString();
		String Esindex = obj.get("elastic_search_index").toString();

		LOGGER.debug("ElasticInsertionChecker is started...");
		
		String id = contentToCheck.getObjectId("_id").getValue().toHexString();
		BsonDocument elastic_doc = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Overview");
		elastic_doc.remove("name");
		
		
		JSONObject tempObj = new JSONObject();
		try {
			tempObj = (JSONObject) parser.parse(elastic_doc.toString());
		} catch (ParseException ex) {
			java.util.logging.Logger.getLogger(ElasticInsertionChecker.class.getName()).log(Level.SEVERE, null, ex);
		}
                
                
		String description = (String) tempObj.get("description");
		String[] descriptionWordsArray = description.trim().split("\\s+");
		Double descriptionFactor = new Double(descriptionWordsArray.length);
		descriptionFactor = 1/descriptionFactor;
		

		ArrayList<String> tagsArrayList = new ArrayList<String>();
		JSONArray tagsArray = (JSONArray) tempObj.get("tags");
                Double tagsArraySize = new Double(tagsArray.size());
		JSONObject tagsObject = new JSONObject();
		JSONArray methodTagsArray = new JSONArray();
		String methodTag;
		String[] methodTagWordsArray;
                
		for (int j=0; j<tagsArray.size(); j++) {
			tagsObject = (JSONObject) tagsArray.get(j);
			methodTagsArray = (JSONArray) tagsObject.get("tags");
			for (int k=0; k<methodTagsArray.size(); k++) {  
				methodTag = methodTagsArray.get(k).toString();
				methodTagWordsArray = methodTag.trim().split("\\s+");
				for (int l=0; l<methodTagWordsArray.length; l++) {            
					tagsArrayList.add(methodTagWordsArray[l]);
				}
			}        
		}
                
		Double tagsFactor = new Double(tagsArrayList.size());
                tagsFactor = 1/tagsArraySize;
		tagsFactor = 1/tagsFactor;
		tempObj.put("descriptionFactor",descriptionFactor);
		tempObj.put("tagsFactor",tagsFactor);
		
		
		LOGGER.debug("blueprint for elastic : "+elastic_doc.toJson());
		
		HttpClient httpClient;
		if(Esauth.equals("basic")) {
			CredentialsProvider provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(Esuser, Espass);
			provider.setCredentials(AuthScope.ANY, credentials);
			httpClient=HttpClientBuilder.create()
					.setDefaultCredentialsProvider(provider)
					.build();
		}else{
			httpClient=HttpClientBuilder.create()
					.build();
		}
	   
		try {
			HttpPut request = new HttpPut("http://"+Esip+":50014/"+Esindex+"/"+id);
			//StringEntity params =new StringEntity(elastic_doc.toJson());
			StringEntity params = new StringEntity(tempObj.toString());
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
