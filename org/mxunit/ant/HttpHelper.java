package org.mxunit.ant;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.util.*;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.BuildException;

public class HttpHelper {

    private HttpClient client;
    private HttpMethod method;
    private String protocol = "http";
    private InputStream is;
    private String url;
    private boolean verbose = false;
    private String httpResponseString;
    private boolean doAuthentication = false;
    private static String  MXUNIT_COOKIE_NAME = "MXUNIT_SUMMARY";
    private String testResultSummary = "0,0,0,0";
    int totalTestRuns = 0;
    int totalFailures = 0;
    int totalErrors = 0;
    int totalTime = 0;


    public boolean testsAreClean(){
      return (this.totalFailures == 0 || this.totalErrors == 0);
    }

    public int getTotalTestRuns(){
      return this.totalTestRuns;
    }

    public int getTotalFailures(){
      return this.totalFailures;
    }

    public int getTotalErrors(){
      return this.totalErrors;
    }

    public int getTotalTime(){
      return this.totalTime;
    }


    public double getFailureRatio(){
      double prod = 0.0;
      double retVal = 0.0;
      if(this.totalTestRuns > 0){
      prod = ((double)this.totalFailures / (double)this.totalTestRuns);
      }
     if(prod == 0.0){
        retVal = 0.0;
      }
     else {
       retVal = prod;
     }
      return retVal;
    }

    public double getErrorRatio(){
        double prod = 0.0;
        double retVal = 0.0;
        if(this.totalTestRuns > 0){
        prod = ((double)this.totalErrors / (double)this.totalTestRuns);
        }
        if(prod == 0.0){
          retVal = 0.0;
        }
       else {
         retVal = prod;
       }
        return retVal;
      }

    public double getSuccessRatio(){
        double prod = 0.0;
        double errorsAndFailures = (double)this.totalErrors + (double)this.totalFailures;
        if(this.totalTestRuns > 0){
        prod = (errorsAndFailures / (double)this.totalTestRuns);
        }
        return (1-prod);
      }

  //Not used ...
  public void isVerbose(boolean verbose){
    this.verbose = verbose;
  }

  public boolean isVerbose(){
    return this.verbose;
  }


 public HttpHelper(){
       this.client = new HttpClient();
       this.client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

       this.doAuthentication = false;
      // System.out.println("[HttpHelper] NO authentication");
 }

  //When authentication is required
  public HttpHelper(String username, String password, String authMethod){
       this.client = new HttpClient();
       this.client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
       //System.out.println("[HttpHelper] Using authentication");
       //Set up authentication
       if(!authMethod.equals("no_auth")){
         List authPrefs = new ArrayList(3);
         authPrefs.add(AuthPolicy.DIGEST);
         authPrefs.add(AuthPolicy.BASIC);
         authPrefs.add(AuthPolicy.NTLM);
         this.client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
         this.client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
         this.doAuthentication = true;
       }
  }

  public void open() throws Exception{

  }

  public void close(){
    this.method.releaseConnection();

  }

  public int runTest(String server, int port, String path, String queryString) throws Exception {

    if(port == 443){
      this.protocol = "https";
    }

    try{
           //Build the URL for HttpClient
           this.url = this.protocol +"://" + server + ":" + port +  path + "?" + URIUtil.encodePath(queryString);
           System.out.println("[HttpHelper] Running URL : " + this.url);
           this.method = new GetMethod(this.url);
           this.method.setDoAuthentication( this.doAuthentication );
           }
           catch (Exception e){
             e.printStackTrace();
             throw new BuildException(e);
       }


    int httpStatusCode = this.client.executeMethod(method);
    //System.out.println("[HttpHelper] mxunit_summary response header value: " + method.getResponseHeader("mxunit_suammry").toString());
    this.is = method.getResponseBodyAsStream();


    //Get the cookies
    Header headers[] =  method.getResponseHeaders("Set-Cookie");

    for(int i = 0; i < headers.length; i++){
    Header header = headers[i];
    HeaderElement[] elements = header.getElements();
      //System.out.println("[HttpHelper] mxunit_summary response header FULL value: *" + header.toString() + "*");
      for(int j = 0; j < elements.length; j++){
        NameValuePair element = elements[j];
        if(element.getName().equals("MXUNIT_SUMMARY")){
         // System.out.println("[HttpHelper] HeaderElement: " + URIUtil.decode(element.getValue()));
          this.testResultSummary = URIUtil.decode(element.getValue());
        }

       }

      }

    return httpStatusCode;

  }


  public String getTestResultSummary(){
   StringTokenizer results = new StringTokenizer(this.testResultSummary, ",");
   String prettyResults = "";
   String runs = results.nextToken();
   String errors = results.nextToken();
   String failures = results.nextToken();
   String time = results.nextToken();

   prettyResults += "Test runs=" + runs + ". ";
   this.totalTestRuns += Integer.parseInt(runs);

   prettyResults += "Errors="  + errors + ". ";
   this.totalErrors +=  Integer.parseInt(errors);

   prettyResults += "Failures="  + failures + ". ";
   this.totalFailures +=  Integer.parseInt(failures);

   prettyResults += "Time="      + time + "ms ";
   this.totalTime +=  Integer.parseInt(time);

   return prettyResults;

  }

  public InputStream getHttpInputStream(){
    return this.is;
  }

  public String getHttpResponseString(){
    return this.httpResponseString;

  }


/**
*
* Convenience method for converting inputstream to a string.
* Not currently used. Client writes IS to file output stream.
*
*
*
*
**/
  public String inputStreamToString(java.io.InputStream is) {
         BufferedReader br = new BufferedReader(new InputStreamReader(is));
         StringBuffer sb = new StringBuffer();
         try {
         String line = null;
  //this also strips all leading and ending whitespace from each line,
  //and strips empty lines
          while ((line = br.readLine().trim()) != null) {
                if(line.length()>0){
                     sb.append(line + "\n");
                 }
             }
         } catch (Exception ex) {
             ex.getMessage();
         } finally {
             try {
                 is.close();
             } catch (Exception ex) {
                 //don't care if this throws an error
             }
            // br = null;
         }
       return sb.toString();
   }


}//end class
