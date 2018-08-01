package gr.ntua.ditas.repositoryengine.restheartcustomization;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.metadata.checkers.Checker;
import org.restheart.metadata.checkers.CheckersUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;

public class LogicalValidator implements Checker {
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.metadata.checkers.Checker");
	
	@Override
	public boolean check(HttpServerExchange exchange, RequestContext context, BsonDocument contentToCheck, BsonValue args) {
		LOGGER.debug("Logical validator is started...");
		LOGGER.debug(contentToCheck.toJson());
		String warning;
		boolean errorFound = false;
		
		LOGGER.debug("Check for duplicate data sources (1)...");
		BsonArray data_sources = contentToCheck.getDocument("INTERNAL_STRUCTURE").getArray("Data_Sources");
		Set<String> ds = new TreeSet<String>();
		for (BsonValue data_source : data_sources) {
			String id = data_source.asDocument().getString("id").getValue();
			if (!ds.add(id)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Data_Sources with id " + id;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for duplicate api methods (2)...");
		LOGGER.debug("...and for invalid data sources inside api methods (3)...");
		BsonDocument pathsdoc = contentToCheck.getDocument("EXPOSED_API").getDocument("paths");
		Iterator<String> paths = pathsdoc.keySet().iterator();
		Set<String> ms = new TreeSet<String>();
		
		while (paths.hasNext()) {
			String pathURI = paths.next();
			BsonDocument path = pathsdoc.getDocument(pathURI);
			Iterator<String> ops = path.keySet().iterator();
			while (ops.hasNext()) {
				String opname = ops.next();
				BsonDocument method = path.getDocument(opname);
				
				String id = method.getString("operationId").getValue();
				if (!ms.add(id)) {
					warning = "Duplicate EXPOSED_API.Paths."+pathURI+"."+opname+".operationId : " + id;
					LOGGER.debug(warning);
					context.addWarning(warning);
					errorFound = true;
				}
				BsonArray method_ds = method.getArray("x-data-sources");
				for (BsonValue data_source : method_ds) {
					String ds_id = data_source.asString().getValue();
					if (!ds.contains(ds_id)) {
						warning = "Data source " + ds_id + " in EXPOSED_API.Paths."+pathURI+"."+opname+" is not declared in INTERNAL_STRUCTURE.Data_Sources";
						LOGGER.debug(warning);
						context.addWarning(warning);
						errorFound = true;
					}
				}
			}
		}
		
		
		LOGGER.debug("Check for invalid method_names of tags (4)...");
		BsonArray tags = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Overview").getArray("tags");
		Set<String> ts = new TreeSet<String>();
		for (BsonValue tag : tags ) {
			String name = tag.asDocument().getString("method_id").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in INTERNAL_STRUCTURE.Overview.tags is not declared in EXPOSES_API" ;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			if (!ts.add(name)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Overview.tags with method_id " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		
		LOGGER.debug("Check for invalid method_names of testing_output_data (5)...");
		BsonArray output = contentToCheck.getDocument("INTERNAL_STRUCTURE").getArray("Testing_Output_Data");
		Set<String> tod = new TreeSet<String>();
		for (BsonValue o : output ) {
			String name = o.asDocument().getString("method_id").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in INTERNAL_STRUCTURE.Testing_Output_Data is not declared in EXPOSES_API";
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			if (!tod.add(name)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Testing_Output_Data with method_id " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for invalid names of DATA_MANAGEMENT.methods (6)...");
		BsonArray dm = contentToCheck.getArray("DATA_MANAGEMENT");
		Set<String> dm_ms = new TreeSet<String>();
		for (BsonValue dm_method : dm) {
			String name = dm_method.asDocument().getString("method_id").getValue();
			if (!ms.contains(name)) {
				warning = "Method  " + name + " in DATA_MANAGEMENT is not declared in EXPOSES_API" ;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			if (!dm_ms.add(name)) {
				warning = "Duplicate DATA_MANAGEMENT.methods with method_id " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}

		
		return !errorFound;
	}

    @Override
    public PHASE getPhase(RequestContext context) {
    	if (context.getMethod() == METHOD.PATCH
                || CheckersUtils
                        .doesRequestUsesDotNotation(context.getContent())
                || CheckersUtils
                        .doesRequestUsesUpdateOperators(context.getContent())) {
    		return PHASE.AFTER_WRITE;
        } else {
            return PHASE.BEFORE_WRITE;
        }
    }

    @Override
    public boolean doesSupportRequests(RequestContext context) {
    	return !(CheckersUtils.isBulkRequest(context)
                && getPhase(context) == PHASE.AFTER_WRITE);
    }

}
