package checkerprinter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class Rise4FunPrinter extends Printer {

    // function: pass a Json Object to the printOutput method  which contains an exception message.
	// the Json object will follow the format of the /run response POST found on http://www.rise4fun.com/dev.
	// Input: String msg, the variable msg contains the message of the exception 
    @Override
    public void printException(String msg) {
    	// the variable message will contain the exception message and other string for formatting.
    	// rise4fun accepts Markdown syntax which the reason for the formatting of the String message
    	// more information about Mardown can be found on https://daringfireball.net/projects/markdown/syntax
    	String message = "***\n";
    	message += "type: expection | Description: " +msg+"\n";
    	       message+= "***\n";
    	
    	       
    	// Get the version of the of the tool by and save it inside the variable version.
    	// This is done by calling the method getVersion()
        String version=null;
		try {
			version = getVersion();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        
		// save the usercode inside the variable code if the usercode is null then code = [could not get user code]
        String code=null;
        if(usercode != null) {
            code=this.usercode;
        } else {
            code ="[could not get user code]";
        }
        
        // create a JsonObjectBuilder following the format found in the /run response POST found on http://www.rise4fun.com/dev.
        JsonObjectBuilder output = Json.createObjectBuilder()
                .add("Version", version)
                .add("Outputs", Json.createArrayBuilder()
                		.add(Json.createObjectBuilder()
                				.add("MimeType","text/plain")
                				.add("Value",message))
                		.add(Json.createObjectBuilder()
                				.add("MimeType","text/x-web-markdown")
                				.add("Value",message)));
                		
        // build the JsonObject and send it to printOutput method
        printOutput(output.build());
    }
    
    // function: pass a Json Object to the printOutput method  which contains the success message.
 	// the Json object will follow the format of the /run response POST found on http://www.rise4fun.com/dev.  
    @Override
    public void printSuccess() {
    	
    	// assert that usercode and execCmd does not equal null
    	assert this.usercode != null : " a success info should given based on non-null usercode";
        assert this.execCmd != null : "a success info should given based on non-null execute command";
        
        
        // the variable message will contain the success message and other string for formatting.
    	// rise4fun accepts Markdown syntax which the reason for the formatting of the String message
    	// more information about Mardown can be found on https://daringfireball.net/projects/markdown/syntax
    	
        String message = "***\n";
    	message += "type: Pass | Description: Nullness Checker Passed! \n";
    	       message+= "***\n";
     
    	// Get the version of the of the tool by and save it inside the variable version.
    	// This is done by calling the method getVersion()
    	String version=null;
    	try {
    		version = getVersion();
    	} catch (FileNotFoundException e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    	}
    	        
    	        
    	        
    	// create a JsonObjectBuilder following the format found in the /run response POST found on http://www.rise4fun.com/dev.        
    	JsonObjectBuilder output = Json.createObjectBuilder()
    			.add("Version", version)
    	        .add("Outputs", Json.createArrayBuilder()
    	        		.add(Json.createObjectBuilder()
    	        				.add("MimeType","text/plain")
    	                		.add("Value",message))
    	                .add(Json.createObjectBuilder()
    	                		.add("MimeType","text/x-web-markdown")
    	                		.add("Value",message)));
    	                		
    	// build the JsonObject and send it to printOutput method           
    	printOutput(output.build());
    }

    // function: pass a Json Object to the printOutput method  which contains the diagnostic message.
 	// the Json object will follow the format of the /run response POST found on http://www.rise4fun.com/dev.
 	// Input: String msg, the variable msg contains the message of the exception 
     @Override
    public void printDiagnosticReport(List<Diagnostic<? extends JavaFileObject>> diagnosticList) {
    	// assert that usercode and execCmd does not equal null
    	assert this.usercode != null : "a diagnostic report should given based on non-null usercode";
        assert this.execCmd != null : "a diagnostic report should given based on non-null execute command";

        //JsonArrayBuilder errorReportBuilder = Json.createArrayBuilder();
        
        // the variable message will contain the diagnostic report and other string for formatting.
    	// rise4fun accepts Markdown syntax which the reason for the formatting of the String message
    	// more information about Mardown can be found on https://daringfireball.net/projects/markdown/syntax
        String message = "***\n";
        int i=1;
        
        // the purpose of the this for loop is to create a table inside the the variable message containing
        // the type of error (ERROR, WARNING, MANDATORY_WARNING, or NOTE), the description of the error and the line and column number 
        // of where the error occurs in the usercode.
        for (Diagnostic<? extends JavaFileObject> err : diagnosticList) {
            Diagnostic.Kind errKind = err.getKind();
            String errHeader = "";
            String errType = "";
            if (errKind == Diagnostic.Kind.ERROR) {
                errHeader = "Error: ";
                errType = "error";
            } else if (errKind == Diagnostic.Kind.WARNING) {
                errHeader = "Warning: ";
                errType = "warning";
            } else if (errKind == Diagnostic.Kind.MANDATORY_WARNING) {
                errHeader = "Warning: ";
                errType = "warning";
            } else if (errKind == Diagnostic.Kind.NOTE) {
                errHeader = "Note: ";
                errType = "info";
            } else {
                this.printException("Error: Compiler doesn't work, please contact admin to report a bug.");
                return;
            }
            
            
            message += "No:"+i+" | Type: "+errType+ " | Description: "+ err.getMessage(null)+" | Line: "+err.getLineNumber()+" | Column: " +err.getColumnNumber()+" |\n";
            message+= "***\n";
            assert err.getMessage(null).length() > 0 : "bytecode is null or diagnosticList is not empty, but error message is empty.";
        }
       // System.out.println(message);
      

        // Get the version of the of the tool by and save it inside the variable version.
    	// This is done by calling the method getVersion()
        String version=null;
		try {
			version = getVersion();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        
		// create a JsonObjectBuilder following the format found in the /run response POST found on http://www.rise4fun.com/dev.
		JsonObjectBuilder output = Json.createObjectBuilder()
                .add("Version", version)
                .add("Outputs", Json.createArrayBuilder()
                		.add(Json.createObjectBuilder()
                				.add("MimeType","text/plain")
                				.add("Value",message))
                		.add(Json.createObjectBuilder()
                				.add("MimeType","text/x-web-markdown")
                				.add("Value",message)));
        
    	// build the JsonObject and send it to printOutput method
        printOutput(output.build());

    }
    
    
    public String getVersion() throws FileNotFoundException
    {
 	   	// get the location of Rise4funPrinter program. 
    	// this will be the location of where the program is compiled 
    	// and not where the file is actually located.
    	Path location = Paths.get(System.getProperty("user.dir"));
    	// get the parent of the location
    	Path parent = location.getParent();
    	
    	// create the path for the metadataInfo.json
    	String metadataPath = Paths.get(parent.toString(),"metadataInfo.json").toString();
    	
    	// assert if the file exits
    	File file = new File(metadataPath);
    	assert file.exists():"Something is wrong the witht the path to the metadatInfo.json file" ;
    	
    	
    	
    	// obtain an inputStream of the metadataInfo.json file
 	    InputStream fis = new FileInputStream(metadataPath);
		
		//create JsonObject out of the metadataInfo.json file
 	  	JsonReader jsonReader = Json.createReader(fis);
		JsonObject jsonObject = jsonReader.readObject();
		
		
		// return the version of the program
		return jsonObject.getString("Version");
       
    }
    
    void printOutput(JsonObject jsonObject) {
        try {
            PrintStream out = new PrintStream(System.out, true, "UTF-8");
            out.print(jsonObject);
            
        }
        catch (UnsupportedEncodingException e) { //fallback
            System.out.print(jsonObject);
        }
        
        
       
    }


}
