import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author <a href="mailto:jackkenlay@gmail.com">Jack Kenlay</a>
 */
public class MyCustomRule implements EnforcerRule {
    private static int totalpackages = 1000;
    private static String serverUrl = "http://localhost:4502/";
    private static String url = serverUrl + "bin/querybuilder.json?nodename=*.zip&orderby=@jcr:content%2Fjcr:lastModified&orderby.sort=desc&p.limit="+totalpackages+"&path=%2Fetc%2Fpackages&type=nt:file";
    public Log logger;
    ArrayList<AEMPackage> packages = new ArrayList();


    /*
    //how to read values ie packages
    //need to add in arr of packages needed
    https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

        2 classes, one for packages, the other for that bullshit bouncy castle settings

        package class
            - need to get an array of listed packages
            - make request to AEM to get packages.
            - ensure the versions match
            - throw any errors if versions do not match

        security conf
            - search for the settings file
            - get all packages from file
            - ensure the versions match/it exists
            - throw any errors if versions do not match

     */



    /**
     * Simple param. This rule will fail if the value is true.
     */
    private boolean shouldIfail = false;

    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        logger = helper.getLog();

        logger.info( "SUP GGGG");

        try {
            // get the various expressions out of the helper.
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            MavenSession session = (MavenSession) helper.evaluate( "${session}" );
            String target = (String) helper.evaluate( "${project.build.directory}" );
            String artifactId = (String) helper.evaluate( "${project.artifactId}" );

            // retrieve any component out of the session directly
            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );
            RuntimeInformation rti = (RuntimeInformation) helper.getComponent( RuntimeInformation.class );

            logger.info( "Retrieved Target Folder: " + target );
            logger.info( "Retrieved ArtifactId: " +artifactId );
            logger.info( "Retrieved Project: " + project );
            logger.info( "Retrieved RuntimeInfo: " + rti );
            logger.info( "Retrieved Session: " + session );
            logger.info( "Retrieved Resolver: " + resolver );

            this.packages = getInstalledPackages();

            logger.info("\n\n------------------Installed Packages---------------\n");
            for(int i = 0; i < this.packages.size(); i++){
                logger.info(this.packages.get(i).getValue("name"));
            }

            if ( this.shouldIfail ) {
                throw new EnforcerRuleException( "-------------------\n\nFailing because my param said so.\n\n---------------" );
            }
        } catch ( ComponentLookupException e ) {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        } catch ( ExpressionEvaluationException e ) {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions
     * change that would cause the result to be different. Multiple cached results are stored
     * based on their id.
     *
     * The easiest way to do this is to return a hash computed from the values of your parameters.
     *
     * If your rule is not cacheable, then the result here is not important, you may return anything.
     */
    public String getCacheId() {
        //no hash on boolean...only parameter so no hash is needed.
        return ""+this.shouldIfail;
    }

    /**
     * This tells the system if the results are cacheable at all. Keep in mind that during
     * forked builds and other things, a given rule may be executed more than once for the same
     * project. This means that even things that change from project to project may still
     * be cacheable in certain instances.
     */
    public boolean isCacheable() {
        return false;
    }

    /**
     * If the rule is cacheable and the same id is found in the cache, the stored results
     * are passed to this method to allow double checking of the results. Most of the time
     * this can be done by generating unique ids, but sometimes the results of objects returned
     * by the helper need to be queried. You may for example, store certain objects in your rule
     * and then query them later.
     */
    public boolean isResultValid(EnforcerRule arg0){
        return false;
    }

    public ArrayList<AEMPackage> getInstalledPackages() throws EnforcerRuleException {
        JSONObject response = sendGetRequest(url);
        JSONArray allpackages = (JSONArray) response.get("hits");

        ArrayList<AEMPackage> allPackagesList = new ArrayList<AEMPackage>();

        for(Object o: allpackages){
            if ( o instanceof JSONObject ) {
                JSONObject ptemppackage = (JSONObject) o;
                AEMPackage temp = new AEMPackage();
                temp.setValues(ptemppackage);
                allPackagesList.add(temp);
            }
        }

        return allPackagesList;
    }

    private JSONObject sendGetRequest(String query) throws EnforcerRuleException {
        HttpURLConnection connection = null;
        String responseAsString = "";
        try {
            URL url = new URL(query);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            //todo this is fucking shit need to be another way
            //todo add in 401 exception to say add in cookie shit
            connection.setRequestProperty("Cookie", "cq-authoring-mode=TOUCH; cq-editor-layer.page=Edit; _ga=GA1.1.1662644103.1520334888; cq-editor-layer.forms=Edit; cq-editor-sidepanel=open; wcmmode=edit; cq-assets-files=card; login-token=3e4f3094-5798-4c80-87f1-92db5e2f7d28%3a8acc48ff-1a25-4228-82de-06691f6a6be3_87906bec8fd699b4%3acrx.default%3bbfe33c29-ee4a-4a3d-8a36-b0b0e596a0c8%3a82ce8cdd-d43d-4461-a2d7-bd081d45169f_28ff04ac4159f55c%3acrx.default; daypsid=624d4ae4-d42a-4a30-8308-42b699370db1");
            //connection.setRequestProperty("OData-Version", "4.0");
            //connection.setRequestProperty("Accept", "application/json");
            logger.info( "Semnding get Req:");
            responseAsString = getRequestBody(connection);
        } catch (Exception e){
//            logger.error("Error sending GET request:");
//            logger.error(e.getMessage());
            logger.error("Error:",e);
            throw new EnforcerRuleException( "Cant get x packages from: " + url);
        }

        JSONObject body = null;
        JSONParser parser = new JSONParser();

        try {
            if((responseAsString!=null) && (!responseAsString.equals(""))){
                body = (JSONObject) parser.parse(responseAsString);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return body;
    }


    private String getRequestBody(HttpURLConnection connection) throws EnforcerRuleException {
        String body = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer responseBody = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                responseBody.append(inputLine);
            }
            in.close();
            body = responseBody.toString();
        } catch (Exception e) {
            logger.error("Can't get 2 packages: ", e);
            throw new EnforcerRuleException( "Cant get packages");
        }
        return body;
    }
}

class AEMPackage {
    /*
        Standard JSON (to my knowledge)
    	"path": "/etc/packages/portal-demo/cq-sample-content-1.0-SNAPSHOT.zip",
		"excerpt": "/etc/packages/portal-demo/cq-sample-content-1.0-SNAPSHOT.zip",
		"name": "cq-sample-content-1.0-SNAPSHOT.zip",
		"title": "cq-sample-content-1.0-SNAPSHOT.zip",
		"lastModified": "2018-03-23 21:38:23",
		"created": "2018-03-23 21:38:23",
		"size": "35 MB",
		"mimeType": "application/zip"
     */

    private HashMap<String, String> values = new HashMap<String, String>();

    public String getValue(String key){
        return this.values.get(key);
    }

    public void setValues(JSONObject inputJSON){
        for(Iterator iterator = inputJSON.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            String value = (String) inputJSON.get(key);
            this.values.put(key, value);
        }
    }
}