package gr.ntua.ditas.repositoryengine.restheartcustomization;

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
			String name = data_source.asDocument().getString("name").getValue();
			if (!ds.add(name)) {
				warning = "Duplicate INTERNAL_STRUCTURE.Data_Sources with name " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for duplicate api methods (2)...");
		LOGGER.debug("...and for invalid data sources inside api methods (3)...");
		BsonArray methods = contentToCheck.getDocument("EXPOSED_API").getArray("Methods");
		Set<String> ms = new TreeSet<String>();
		for (BsonValue method : methods) {
			BsonDocument d_method = method.asDocument(); 
			String name = d_method.getString("name").getValue();
			if (!ms.add(name)) {
				warning = "Duplicate EXPOSED_API.Methods with name " + name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			BsonArray method_ds = d_method.getArray("data_sources");
			for (BsonValue data_source : method_ds) {
				String ds_name = data_source.asString().getValue();
				if (!ds.contains(ds_name)) {
					warning = "Data source " + ds_name + " in EXPOSED_API.Methods with name " + name + " is not declared in INTERNAL_STRUCTURE.Data_Sources";
					LOGGER.debug(warning);
					context.addWarning(warning);
					errorFound = true;
				}
			}
		}
		
		LOGGER.debug("Check for invalid method_names of tags (4)...");
		BsonArray tags = contentToCheck.getDocument("INTERNAL_STRUCTURE").getDocument("Overview").getArray("tags");
		for (BsonValue tag : tags ) {
			String name = tag.asDocument().getString("method_name").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in INTERNAL_STRUCTURE.Overview.tags is not declared in EXPOSES_API.Methods" ;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		
		LOGGER.debug("Check for invalid method_names of testing_output_data (5)...");
		BsonArray output = contentToCheck.getDocument("INTERNAL_STRUCTURE").getArray("Testing_Output_Data");
		for (BsonValue o : output ) {
			String name = o.asDocument().getString("method_name").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in INTERNAL_STRUCTURE.Testing_Output_Data is not declared in EXPOSES_API.Methods";
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for invalid names of DATA_MANAGEMENT.methods (6)...");
		BsonArray dm_methods = contentToCheck.getDocument("DATA_MANAGEMENT").getArray("methods");
		for (BsonValue dm_method : dm_methods) {
			String name = dm_method.asDocument().getString("name").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in DATA_MANAGEMENT.methods is not declared in EXPOSES_API.Methods" ;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		LOGGER.debug("Check for invalid names of ABSTRACT_PROPERTIES.methods (7)...");
		BsonArray ap_methods = contentToCheck.getDocument("ABSTRACT_PROPERTIES").getArray("methods");
		for (BsonValue ap_method : ap_methods ) {
			String name = ap_method.asDocument().getString("name").getValue();
			if (!ms.contains(name)) {
				warning = "Method " + name + " in ABSTRACT_PROPERTIES.methods is not declared in EXPOSES_API.Methods";
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
		}
		
		
		LOGGER.debug("Check for duplicate infrastructure at Cookbook appendix (8)...");
		LOGGER.debug("...and duplicate resources at Cookbook appendix infrastructure (9)...");
		BsonArray infrastructures = contentToCheck.getDocument("COOKBOOK_APPENDIX").getArray("infrastructure");
		Set<String> is = new TreeSet<String>();
		for (BsonValue infrastructure : infrastructures) {
			BsonDocument d_infrastructure = infrastructure.asDocument();
			String infra_name = d_infrastructure.getString("name").getValue();
			if (!is.add(infra_name)) {
				warning = "Duplicate COOKBOOK_APPENDIX.infrastructure with name " + infra_name;
				LOGGER.debug(warning);
				context.addWarning(warning);
				errorFound = true;
			}
			BsonArray resources = d_infrastructure.getArray("resources");
			Set<String> rs = new TreeSet<String>();
			for (BsonValue resource : resources) {
				String res_name = resource.asDocument().getString("name").getValue();
				if (!rs.add(res_name)) {
					warning = "Duplicate resource with name " + res_name + " in COOKBOOK_APPENDIX.infrastructure with name " + infra_name;
					LOGGER.debug(warning);
					context.addWarning(warning);
					errorFound = true;
				}
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
