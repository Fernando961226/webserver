/*****************************************************************************
InMemory: major entry of checkerprinter
This file is modified based on the traceprinter package from java_jail:https://github.com/daveagp/java_jail 
with  GNU AFFERO GENERAL PUBLIC LICENSE Version 3. 
Modified by Charles Chen (charleszhuochen@gmail.com) Mar 2016
===original doc as below===
traceprinter: a Java package to print traces of Java programs
David Pritchard (daveagp@gmail.com), created May 2013

The contents of this directory are released under the GNU Affero 
General Public License, versions 3 or later. See LICENSE or visit:
http://www.gnu.org/licenses/agpl.html

See README for documentation on this package.

This file was originally based on 
com.sun.tools.example.trace.Trace, written by Robert Field.

******************************************************************************/

package checkerprinter;

import java.util.regex.*;
import java.util.*;
import java.io.*;

import javax.tools.*;

import traceprinter.ramtools.*;

import javax.json.*;

public class InMemory {

    String mainClass;
    String exceptionMsg;
    List<String> checkerOptionsList;
    Printer checkerPrinter;

    //TODO: make key as an enum class to ahcieve type safety
    static final Map<String, String> checkerMap;
    static {
        HashMap<String, String> tempMap = new HashMap<String, String>();
        tempMap.put("nullness", "org.checkerframework.checker.nullness.NullnessChecker");
        //since generally don't directly call map key checker, I mapping it to nullness here
        tempMap.put("map_key", "org.checkerframework.checker.nullness.NullnessChecker");
        tempMap.put("regex", "org.checkerframework.checker.regex.RegexChecker");
        tempMap.put("interning", "org.checkerframework.checker.interning.InterningChecker");
        tempMap.put("aliasing", "org.checkerframework.common.aliasing.AliasingChecker");
        tempMap.put("lock", "org.checkerframework.checker.lock.LockChecker");
        tempMap.put("fake_enum", "org.checkerframework.checker.fenum.FenumChecker");
        tempMap.put("tainting", "org.checkerframework.checker.tainting.TaintingChecker");
        tempMap.put("format_string", "org.checkerframework.checker.formatter.FormatterChecker");
        tempMap.put("linear", "org.checkerframework.checker.linear.LinearChecker");
        tempMap.put("igj", "org.checkerframework.checker.igj.IGJChecker");
        tempMap.put("javari", "org.checkerframework.checker.javari.JavariChecker");
        tempMap.put("signature", "org.checkerframework.checker.signature.SignatureChecker");
        tempMap.put("gui_effect", "org.checkerframework.checker.guieffect.GuiEffectChecker");
        tempMap.put("units", "org.checkerframework.checker.units.UnitsChecker");
        tempMap.put("cons_value", "org.checkerframework.common.value.ValueChecker");
        checkerMap = Collections.unmodifiableMap(tempMap);
    }
    private final String CHECKER_FRAMEWORK;
    Map<String, byte[]> bytecode;

    public final static long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        assert args.length == 2 : "this program needs two command line arguments: "
                + "args[0]: location of checker framework"
                + "argd[1]: isRise4Fun, indicates whether should use Rise4FunPrinter";
            boolean isRise4Fun = Boolean.valueOf(args[1]);
            Printer checkerPrinter = null;
            JsonObject data=null;
            
            // Get the data send by the website as a JsonObject.
            // If we are unable to get the data, throw a IOException error and
            // set the checkerPrinter Usercode to null and printException Internal IOException.
            try {
            data =Json.createReader(new InputStreamReader(System.in, "UTF-8")).readObject();
            } 
            catch (IOException e) {
                checkerPrinter.setUsercode(null);
                checkerPrinter.printException("Internal IOException");
            }
            
           // If the data that we received is from rise4fun then we need to change the format of it 
           // to make is work with our InMermory class and the make our checkerPrinter the Rise4FunPrinter.
            if(isRise4Fun) {
                checkerPrinter = new Rise4FunPrinter();
               
                // Modify the data to meet the specification of the InMemory class.
                data=rise4funModifyData(data);
                
            } else {
                checkerPrinter = new JsonPrinter();
            }

      
            new InMemory(data, args[0],checkerPrinter);
         
        
    }
    // Method purpose: to modify the data given by rise4fun in order to meet the requirements of the InMemory class.
    // Input: JsonObject.
    // Output: JsonObject.
   static  protected JsonObject rise4funModifyData(JsonObject rise4funData)
    {  
	   // Get the value of the key "Source" inside the JsonObject rise4funData
	   // and save it in the variable usercode as a String.
	   String usercode =rise4funData.get("Source").toString();
		
	   // construct a new JsonObject containing the variable usercode, and and an options key 
	   // containing the checker to be used.
	   JsonObject data =Json.createObjectBuilder()
    	        .add("usercode", usercode)
    	        .add("options", Json.createObjectBuilder()
    	        		.add("checker","nullness")
    	        		.add("has_cfg", false)
    	        		.add("verbose",false)).build();
    	   return data;
    }
    
    
    
    protected boolean initCheckerArgs(JsonObject optionsObject) {
        String checkerKey = optionsObject.getString("checker");
        List<String> options = new ArrayList<> ();

        if (optionsObject.getBoolean("has_cfg")) {
            // String cfgLevel = optionsObject.getString("cfg_level");
            // options.add(cfgLevel);
            // TODO: add CFG Visualization
        }
        return initCheckerArgs(checkerKey, options.toArray(new String[options.size()]));
    }

    /**TODO: currently options still not been used. In future it is useful for extending cfg visualization
    *
    */
    protected boolean initCheckerArgs(final String checkerKey, String... options) {
        String checkerQualifiedName = InMemory.checkerMap.get(checkerKey);
        if (checkerQualifiedName == null || checkerQualifiedName == "") {
            this.exceptionMsg = "Error: Cannot find indicated checker.";
            return false;
        }
        this.checkerOptionsList = Arrays.asList( "-Xbootclasspath/p:" +
                this.CHECKER_FRAMEWORK + "/checker/dist/jdk8.jar",
                "-processor",
                checkerQualifiedName);
        return true;
    }

    // figure out the class name, then compile and run main([])
    InMemory(JsonObject frontend_data, String enabled_cf, Printer checkerPrinter) {
        String usercode = frontend_data.getJsonString("usercode").getString();
        this.CHECKER_FRAMEWORK = enabled_cf;
        this.checkerPrinter = checkerPrinter;
        this.checkerPrinter.setUsercode(usercode);
        if (!initCheckerArgs(frontend_data.getJsonObject("options"))) {
            this.checkerPrinter.printException(this.exceptionMsg);
            return;
        }
        // not 100% accurate, if people have multiple top-level classes + public inner classes
        // first search public class, to avoid wrong catching a classname from  comments in usercode
        // as the public class name (if the comment before the public class declaration contains a "class" word)
        Pattern p = Pattern.compile("public\\s+class\\s+([a-zA-Z0-9_]+)\\b");
        Matcher m = p.matcher(usercode);
        if (!m.find()) {
            // if usercode does not have a public class, then is safe to using looser rgex to catch class name
            p = Pattern.compile("class\\s+([a-zA-Z0-9_]+)\\b");
            m = p.matcher(usercode);
            if(!m.find()) {
                this.exceptionMsg = "Error: Make sure your code includes at least one 'class \u00ABClassName\u00BB'";
                this.checkerPrinter.printException(this.exceptionMsg);
                return;
            }
        }
        mainClass = m.group(1);
        System.err.println(mainClass);
        CompileToBytes c2b = new CompileToBytes();

        c2b.compilerOutput = new StringWriter();
        c2b.options = this.checkerOptionsList;

        DiagnosticCollector<JavaFileObject> errorCollector = new DiagnosticCollector<>();
        c2b.diagnosticListener = errorCollector;

        bytecode = c2b.compileFile(mainClass, usercode);

        List<Diagnostic<? extends JavaFileObject>> diagnosticList = errorCollector.getDiagnostics();

        assert this.checkerOptionsList.size() > 1 : "at least should have -Xbootclasspath/p: flag";

        this.checkerPrinter.setExecCmd(this.checkerOptionsList
                .subList(1, this.checkerOptionsList.size()));

        if(bytecode != null && diagnosticList.size() == 0){
            this.checkerPrinter.printSuccess();
        } else {
            this.checkerPrinter.printDiagnosticReport(diagnosticList);
        }
    } 
}
