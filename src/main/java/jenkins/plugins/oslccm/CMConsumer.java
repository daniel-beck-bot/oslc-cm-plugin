/**
 * This file is (c) Copyright 2011 by Madhumita DHAR, Institut TELECOM
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * This program has been developed in the frame of the COCLICO
 * project with financial support of its funders.
 *
 */

package jenkins.plugins.oslccm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.json.JSONStringer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;


public class CMConsumer extends Notifier {
	
	private static final Logger LOGGER = Logger.getLogger(CMConsumer.class.getName());
	
	private String token;
	private String tokenSecret;
	private boolean manual;
	private boolean automatic;
	private String url;
	private String delegUrl;
	private boolean eachBuildFailure;
	private boolean firstBuildFailure;
	private int width;
	private int height;
	
	//@DataBoundConstructor
	public CMConsumer(String token, String tokenSecret, boolean manual, boolean automatic, String url, String delegUrl, int width, int height, boolean eachBuildFailure, boolean firstBuildFailure)	{
		this.token = token;
		this.tokenSecret = tokenSecret;
		this.manual = manual;
		this.automatic = automatic;
		this.url = url;
		this.delegUrl = delegUrl;
		this.width = width;
		this.height = height;
		this.eachBuildFailure = eachBuildFailure;
		this.firstBuildFailure = firstBuildFailure;
	}
	
	@DataBoundConstructor
	public CMConsumer(String token, String tokenSecret)	{
		this.token = token;
		this.tokenSecret = tokenSecret;
	}
	
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new OslccmProjectAction(project);
	}
	
	public boolean getEachBuildFailure()	{
		return eachBuildFailure;
	}
	
	public boolean getFirstBuildFailure()	{
		return firstBuildFailure;
	}
	
	public String getToken()	{
		return token;
	}
	
	public String getTokenSecret()	{
		return tokenSecret;
	}
	
	public boolean getManual()	{
		return manual;
	}
	
	public boolean getAutomatic()	{
		return automatic;
	}
	
	public String getUrl()	{
		return url;
	}
	
	public String getDelegUrl()	{
		return delegUrl;
	}
	
	public int getWidth()	{
		return width;
	}
	
	public int getHeight()	{
		return height;
	}
	
	private static String createTinyUrl(String url) throws IOException {
		org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
		GetMethod gm = new GetMethod("http://tinyurl.com/api-create.php?url="
				+ url.replace(" ", "%20"));

		int status = client.executeMethod(gm);
		if (status == HttpStatus.SC_OK) {
			return gm.getResponseBodyAsString();
		} else {
			throw new IOException("Error in tinyurl: " + status);
		}

	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		LOGGER.info("Consumer Key: " + ((DescriptorImpl) getDescriptor()).getConsumerKey());
		LOGGER.info("Consumer Secret: " + ((DescriptorImpl) getDescriptor()).getConsumerSecret());
		LOGGER.info("Token: " + token);
		LOGGER.info("Token Secret: " + tokenSecret);
		LOGGER.info("Manual: " + manual);
		LOGGER.info("Automatic: " + automatic);
		LOGGER.info("URL: " + url);
		LOGGER.info("Delegated URL: " + delegUrl);
		LOGGER.info("Delegated URL width: " + width);
		LOGGER.info("Delegated URL height: " + height);
		LOGGER.info("On every failure: " + eachBuildFailure);
		LOGGER.info("On first failure: " + firstBuildFailure);
		
		if(manual)	{
			OAuthConsumer consumer = new CommonsHttpOAuthConsumer(((DescriptorImpl) getDescriptor()).getConsumerKey(), ((DescriptorImpl) getDescriptor()).getConsumerSecret());	        
	        consumer.setTokenWithSecret(getToken(), getTokenSecret());
	        String absoluteBuildURL = ((DescriptorImpl) getDescriptor()).getUrl() + build.getUrl();

	        /*String uiUrl = this.getDelegUrl();

	        try {
		        HttpPost post = new HttpPost("http://fftrunk/plugins/oslc/cm/project/6/tracker/101/ui/creation");
		        consumer.sign(post);
		        String hdr = post.getFirstHeader("Authorization").getValue().substring(6).replace(", ", "&");
		        uiUrl = uiUrl + "?" + hdr;
	        	
				//uiUrl = consumer.sign(this.getDelegUrl());
				uiUrl = uiUrl.replace("oauth", "auth");
				LOGGER.info("NEW URL: " + uiUrl);
			} catch (OAuthMessageSignerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			OslccmBuildAction bAction = new OslccmBuildAction(build, this.getDelegUrl(), this.width, this.height, consumer, absoluteBuildURL);
			build.addAction(bAction);
			LOGGER.info("Adding delegated create action");
		}
		
		if (shouldSendBugReport(build)) {
			try {
				String report = createBugReport(build);
				sendReport(report);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Unable to send bug report.", e);
			}
		}
		return true;
	}

	private String createBugReport(AbstractBuild<?, ?> build) {
		String projectName = build.getProject().getName();
		String result = build.getResult().toString();
		String tinyUrl = "";
		String absoluteBuildURL = ((DescriptorImpl) getDescriptor()).getUrl() + build.getUrl();
		try {
			tinyUrl = createTinyUrl(absoluteBuildURL);
		} catch (Exception e) {
			tinyUrl = "?";
		}
		return String.format("%s:%s $%d (%s)", projectName, result, build.number, tinyUrl);
	}
	
	public void sendReport(String message) throws Exception {
		LOGGER.info("Attempting to send bug report: " + message);
		
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(((DescriptorImpl) getDescriptor()).getConsumerKey(), ((DescriptorImpl) getDescriptor()).getConsumerSecret());
        
        consumer.setTokenWithSecret(getToken(), getTokenSecret());
        
        /*HttpPost request = new HttpPost("http://squeeze2/plugins/oauthprovider/echo.php");
        StringEntity body = new StringEntity("message=hello", "UTF-8"));
        body.setContentType("application/x-www-form-urlencoded");
        request.setEntity(body);*/
        
        JSONStringer js = new JSONStringer();
    	js.object().key("dcterms:title").value("Hudson Build Failure").
    	    key("dcterms:description").value(message).
    	    key("oslc_cm:status").value("Open").
    	    key("helios_bt:priority").value("3").
    	    key("helios_bt:assigned_to").value("Nobody").endObject();
    	
        HttpPost request = new HttpPost(getUrl());
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type","application/json");
        StringEntity body = new StringEntity(js.toString());
        //body.setContentType("application/json;charset=UTF-8");
        //body.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
        request.setEntity(body);
        
        

        consumer.sign(request);

        LOGGER.info("Sending bug report to Fusionforge...");
        
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);

        /*System.out.println("Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());*/
        LOGGER.info("Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());
        
        //System.out.println("Response: " + response.getEntity().getContent().toString());
        //response.getEntity().writeTo(LOGGER);
        LOGGER.info(EntityUtils.toString(response.getEntity()));

		
	}

	/**
	 * Determine if this build failure is the first failure in a series of
	 * build failures
	 *
	 * @param build the Build object
	 * @return true if this build is the first
	 */
	protected boolean isFirstFailure(AbstractBuild<?, ?> build) {
		if (build.getResult() == Result.FAILURE || build.getResult() == Result.UNSTABLE) {
			AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
			if (previousBuild != null && previousBuild.getResult() == Result.SUCCESS) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Determine if a bug report should be created and sent
	 *
	 * @param build the Build object
	 * @return true if we should report this build failure
	 */
	protected boolean shouldSendBugReport(AbstractBuild<?, ?> build) {
		if(this.getAutomatic())	{
			LOGGER.info("inside getautomatic");
			if (this.getEachBuildFailure())	{
				LOGGER.info("inside getEachBuildFailure");
				return true;
			}else if(this.getFirstBuildFailure())	{
				LOGGER.info("inside getFirstBuildFailure");
				if(this.isFirstFailure(build))	{
					LOGGER.info("inside isFirstFailure");
					return true;
				}else	{
					return false;
				}
			}else	{
				return false;
			}
		}else	{
			return false;
		}
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		
		public String hudsonUrl;
		public String consumerKey;
		public String consumerSecret;
		
		public DescriptorImpl() {
			super(CMConsumer.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			hudsonUrl = Mailer.descriptor().getUrl();
			req.bindParameters(this);
			save();
			return super.configure(req, formData);
		}

		@Override
		public String getDisplayName() {
			return "OSLC Consumer";
		}
		
		public String getConsumerKey()	{
			return consumerKey;
		}
		
		public String getConsumerSecret()	{
			return consumerSecret;
		}

		public String getUrl() {
			return hudsonUrl;
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public CMConsumer newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if (hudsonUrl == null) {
				// if Hudson URL is not configured yet, infer some default
				hudsonUrl = Functions.inferHudsonURL(req);
				save();
			}
			//return super.newInstance(req, formData);
			LOGGER.info("new Instance");
			return new CMConsumer(	
					req.getParameter("token"),
					req.getParameter("tokenSecret"),
					req.getParameter("manual")!=null,
					req.getParameter("automatic")!=null,
					req.getParameter("url"),
					req.getParameter("delegUrl"),
					Integer.parseInt(req.getParameter("width")),
					Integer.parseInt(req.getParameter("height")),
					req.getParameter("eachBuildFailure")!=null,
					req.getParameter("firstBuildFailure")!=null);
		}	
		
	}
	
	
}