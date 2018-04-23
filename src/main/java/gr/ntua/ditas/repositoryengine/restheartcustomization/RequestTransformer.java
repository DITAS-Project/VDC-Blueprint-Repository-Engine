package gr.ntua.ditas.repositoryengine.restheartcustomization;

import org.restheart.metadata.transformers.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;

  
public class RequestTransformer implements Transformer {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.examples.applogic.RequestTransformer");
	private static final List<String> SECTIONS = Arrays.asList(
			"INTERNAL_STRUCTURE",
			"DATA_MANAGEMENT",
			"ABSTRACT_PROPERTIES",
			"COOKBOOK_APPENDIX",
			"EXPOSED_API"
	);
	
	@Override
	public void transform(HttpServerExchange exchange, RequestContext context, BsonValue contentToTransform,
			BsonValue args) {
		
		if (context.getMethod() == METHOD.POST) {
		
			LOGGER.debug("POST request");
			//auto-generated fields
			if (contentToTransform.isArray()) 
				contentToTransform.asArray().forEach( 
						doc -> post_transform(doc)
				);
			if (contentToTransform.isDocument()) 
				post_transform(contentToTransform);
		
		}else if (context.getMethod() == METHOD.GET) {
			
			LOGGER.debug("GET request");
			//LOGGER.debug("Entry : "+ contentToTransform.asDocument().toJson());
			
			Map<String, Deque<String>> params = exchange.getQueryParameters();
			
			if (params.containsKey("section")) {
				LOGGER.debug("Section parameter found");
				Deque<String> qsections = params.get("section");
				Deque<String> d = new ArrayDeque<>();
				
				for(String qsection : qsections) {
					String section = parseSection(qsection);
					params.remove("section");
					if (!section.isEmpty()) {
						d.add("{'"+ section + "':1}");
						context.setKeys(d);
					}
				}
			}
			
			Deque<String> filter = context.getFilter();
			if (filter!=null) 
				filter.stream().forEach(
						(String f) -> {LOGGER.debug(f);}
				);
			else filter = new ArrayDeque<>();
			
			context.setFilter(filter);
		}
	}
	
	
	private void post_transform(BsonValue contentToTransform){
		
		if (contentToTransform.isDocument()) {
			BsonDocument data = contentToTransform.asDocument();
		
			BsonObjectId id = new BsonObjectId();
			String sid = id.getValue().toHexString();
			
			data.put("_id",id);
			data.put("UUID", new BsonString(sid));
			BsonDocument dmanagement = data.getDocument("DATA_MANAGEMENT");
			BsonDocument generalMetrics = new BsonDocument();
			
			generalMetrics.put("dataUtility",new BsonDocument());
			generalMetrics.put("security", new BsonDocument());
			generalMetrics.put("privacy", new BsonDocument());
			
			dmanagement.append("generalMetrics", generalMetrics);
			
			//LOGGER.debug(contentToTransform.asDocument().toJson());
		}
	}
	
	
	private String parseSection(String qsection) {
		
		if (SECTIONS.contains(qsection)) return qsection;
		try {
		      int index = Integer.parseInt(qsection);
		      return SECTIONS.get(index-1);
		} catch (IndexOutOfBoundsException | NumberFormatException e) {
			LOGGER.info("invalid section parameter");
			return "";
		}
	}
}   