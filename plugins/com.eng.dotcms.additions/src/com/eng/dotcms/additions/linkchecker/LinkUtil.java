package com.eng.dotcms.additions.linkchecker;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Config;

public class LinkUtil {
	
	@SuppressWarnings("deprecation")
    private static void loadProxy(HttpClient client) {
		if(Config.getBooleanProperty("urlcheck.connection.proxy", false)){
			client.getHostConfiguration().setProxy(Config.getStringProperty("urlcheck.connection.proxyHost"), Config.getIntProperty("urlcheck.connection.proxyPort"));
			HttpState state = new HttpState();
			if(Config.getBooleanProperty("urlcheck.connection.proxyRequiredAuth", false)){
				state.setProxyCredentials(null, null,
						new UsernamePasswordCredentials(Config.getStringProperty("urlcheck.connection.proxyUsername"), Config.getStringProperty("urlcheck.connection.proxyPassword")));
				client.setState(state);       
			}
		}
    }
	
	private static URL getURLByString(String href){
        try {
            java.net.URL url = new java.net.URL(href);
            URL urlBean = new URL();
            if(url.getProtocol().equals("https"))
                urlBean.setHttps(true);
            else
                urlBean.setHttps(false);
            urlBean.setHostname(url.getHost());         
            urlBean.setPort(url.getPort()<0?80:url.getPort());
            urlBean.setPath(url.getPath());
            if(url.getQuery()!=null){
                urlBean.setWithParameter(true);
                String[] query_string = null;
                if(url.getQuery().split("[&amp;]").length>0)
                    query_string = url.getQuery().split("[&amp;]");
                else
                    query_string = url.getQuery().split("[&]");
                NameValuePair[] params = new NameValuePair[query_string.length];
                for(int i=0; i<query_string.length; i++){
                    String[] parametro_arr = query_string[i].split("[=]");
                    if(parametro_arr.length==2)
                        params[i] = new NameValuePair(parametro_arr[0], parametro_arr[1]);
                    else
                        params[i] = new NameValuePair(parametro_arr[0], "");
                }
                urlBean.setQueryString(params);             
            }
            return urlBean;
        } catch (MalformedURLException e) {
            return null;
        }
    }

	public static InvalidLink findInvalidLink(Contentlet con) {
		InvalidLink il = null;
		String linkEsterno = con.getStringProperty("linkEsterno");
		List<Field> linkFields = FieldsCache.getFieldsByStructureInode(StructureCache.getStructureByInode(con.getStructureInode()).getInode());
		Field _linkEsterno = null;
		for(Field ff: linkFields)
			if(ff.getVelocityVarName().equals("linkEsterno"))
				_linkEsterno = ff;
		Anchor a = new Anchor();
		a.setExternalLink(getURLByString(linkEsterno));
        a.setTitle(con.getStringProperty("titolo"));
        a.setInternalLink(null);
        a.setInternal(false); 
		
        HttpConnectionParams params=new HttpConnectionParams();
        params.setConnectionTimeout(2000);
        HttpClient client = new HttpClient(new HttpClientParams(params));
        loadProxy(client);
        String url=a.getExternalLink().absoluteURL();
        HttpMethod method = new GetMethod(url);
        if(a.getExternalLink().isWithParameter())
            method.setQueryString(a.getExternalLink().getQueryString());
        int statusCode = -1;
        try{
            statusCode = client.executeMethod(method);
        } catch(Exception e){ }
        
        if(statusCode<200 || statusCode>=400){
            il = new InvalidLink();
            il.setUrl(a.getExternalLink().absoluteURL());
            il.setStatusCode(statusCode);
            il.setTitle(a.getTitle());
            il.setField(null!=_linkEsterno?_linkEsterno.getInode():"");            
        }
        return il;
	}
	
	protected static class Anchor {
        
        private URL externalLink;
        private String title;
        private String internalLink;
        private boolean isInternal;
        
        public URL getExternalLink() {
            return externalLink;
        }
        
        public void setExternalLink(URL href) {
            this.externalLink = href;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }

        public String getInternalLink() {
            return internalLink;
        }

        public void setInternalLink(String internalLink) {
            this.internalLink = internalLink;
        }

        public boolean isInternal() {
            return isInternal;
        }

        public void setInternal(boolean isInternal) {
            this.isInternal = isInternal;
        }
        
    }
    
    protected static class URL {
        
        private String hostname;
        private Integer port;
        private boolean https;
        private String path;
        private boolean withParameter;
        private NameValuePair[] queryString;
        
        public String getHostname() {
            return hostname;
        }
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
        public Integer getPort() {
            return port;
        }
        public void setPort(Integer port) {
            this.port = port;
        }
        public boolean isHttps() {
            return https;
        }
        public void setHttps(boolean https) {
            this.https = https;
        }
        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public boolean isWithParameter() {
            return withParameter;
        }
        public void setWithParameter(boolean withParameter) {
            this.withParameter = withParameter;
        }
        public NameValuePair[] getQueryString() {
            return queryString;
        }
        public void setQueryString(NameValuePair[] queryString) {
            this.queryString = queryString;
        }   
        
        public String completeURL(){
            StringBuilder sb = new StringBuilder(500);
            sb.append(https?"https://":"http://");
            sb.append(hostname);
            sb.append(port!=80?":"+port:"");
            sb.append(path);
            if(withParameter){
                sb.append("?");
                for(int i=0; i<queryString.length; i++){
                    sb.append(queryString[i].getName());
                    sb.append("=");
                    sb.append(queryString[i].getValue());
                    if(queryString.length-i>1)
                        sb.append("&");
                }
            }
            return sb.toString();
        }
        
        public String absoluteURL(){
            StringBuilder sb = new StringBuilder(500);
            sb.append(https?"https://":"http://");
            sb.append(hostname);
            sb.append(port!=80?":"+port:"");
            sb.append(path);
            return sb.toString();
        }
    }
}
