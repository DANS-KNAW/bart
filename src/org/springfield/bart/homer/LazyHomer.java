/* 
* LazyHomer.java
* 
* Copyright (c) 2014 Noterik B.V.
* 
* This file is part of Bart, related to the Noterik Springfield project.
*
* Bart is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Bart is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Bart.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.springfield.bart.homer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.*;
import org.springfield.mojo.http.*;
import org.springfield.mojo.interfaces.ServiceInterface;
import org.springfield.mojo.interfaces.ServiceManager;


public class LazyHomer implements MargeObserver {
	
	private static Logger LOG = Logger.getLogger(LazyHomer.class);

	/** Noterik package root */
	public static final String PACKAGE_ROOT = "com.noterik";
	private static enum loglevels { all,info,warn,debug,trace,error,fatal,off; }
	public static String myip = "unknown";
	private static int port = -1;
	private static int bart_port = 8080;
	private static int smithers_port = 8080;
	static String group = "224.0.0.0";
	static String role = "production";
	static int ttl = 1;
	static boolean noreply = true;
	static LazyMarge marge;
	static SmithersProperties selectedsmithers = null;
	private static boolean running = false;
	private static String rootPath = null;
	
	private static Map<String, SmithersProperties> smithers = new HashMap<String, SmithersProperties>();
	private static Map<String, BartProperties> barts = new HashMap<String, BartProperties>();
	private static LazyHomer ins;

	private int retryCounter;
	
	/**
	 * Initializes the configuration
	 */
	public void init(String r) {
		rootPath = r;
		ins = this;
		retryCounter = 0;
		initLogger();
		initConfig();
		try{
			InetAddress mip=InetAddress.getLocalHost();
			myip = ""+mip.getHostAddress();
		}catch (Exception e){
			LOG.error("Exception ="+e.getMessage());
		}
		LOG.info("Bart init service name = bart on ipnumber = "+myip+" on marge port "+port);
		marge = new LazyMarge();
		
		// lets watch for changes in the service nodes in smithers
		marge.addObserver("/domain/internal/service/bart/nodes/"+myip, ins);
		marge.addTimedObserver("/smithers/downcheck",6,this);
		new DiscoveryThread();	
	}
	

	public static void addSmithers(String ipnumber,String port,String mport,String role) {
			int oldsize = smithers.size();
			if (!(""+LazyHomer.getPort()).equals(mport)) {
				LOG.warn("EXTREME WARNING CLUSTER COLLISION ("+LazyHomer.getPort()+") "+ipnumber+":"+port+":"+mport);
				return;
			}
			
			if (!role.equals(getRole())) {
				LOG.warn("Ignored this smithers ("+ipnumber+") its "+role+" and not "+getRole()+" like us");
				return;
			}
			
			SmithersProperties sp = smithers.get(ipnumber);
			if (sp==null) {
				sp = new SmithersProperties();
				smithers.put(ipnumber, sp);
				sp.setIpNumber(ipnumber);
				sp.setPort(port);
				sp.setAlive(true); // since talking its alive 
				noreply = false; // stop asking (minimum of 60 sec, delayed)
				LOG.info("Bart found smithers at = "+ipnumber+" port="+port+" multicast="+mport);
			} else {
				if (!sp.isAlive()) {
					sp.setAlive(true); // since talking its alive again !
					LOG.info("bart recovered smithers at = "+ipnumber);
				}
			}

		// so check if we are known 
		if (oldsize==0 && ins.checkKnown()) {
			
			// we are verified (has a name other than unknown) and status is on
			BartProperties mp = barts.get(myip);
			setLogLevel(mp.getDefaultLogLevel());
			if (mp!=null && mp.getStatus().equals("on")) {
				if (!running) {
					running = true;
					LOG.info("This bart will be started (on startup)");
				}
			} else {
				if (running) {
					running = false;
				} else {
					LOG.info("This bart is not turned on, use smithers todo this for ip "+myip);
				}
			}
		}
	}
	
	public static BartProperties getMyBartProperties() {
		return barts.get(myip);
	}
	
	public static int getMyBartPosition() {
		int i = 0;
		for(Iterator<BartProperties> iter = barts.values().iterator(); iter.hasNext(); ) {
			BartProperties m = (BartProperties)iter.next();
			i++;
			if (m.getIpNumber().equals(myip)) return i;
		}
		return -1;
	}
	
	public static int getNumberOfBarts() {
		return barts.size();
	}
	
	
	
	private Boolean checkKnown() {
		ServiceInterface smithers = ServiceManager.getService("smithers");
		if (smithers==null) return false;

		String xml = "<fsxml><properties><depth>1</depth></properties></fsxml>";
		String nodes = smithers.get("/domain/internal/service/bart/nodes",xml,"text/xml");

		boolean iamok = false;

		try { 
			boolean foundmynode = false;
			//System.out.println("NODES="+nodes);
			Document result = DocumentHelper.parseText(nodes);
			for(Iterator<Node> iter = result.getRootElement().nodeIterator(); iter.hasNext(); ) {
				Element child = (Element)iter.next();
				if (!child.getName().equals("properties")) {
					String ipnumber = child.attributeValue("id");
					String status = child.selectSingleNode("properties/status").getText();
					String name = child.selectSingleNode("properties/name").getText();

					// lets put all in our bart list
					BartProperties mp = barts.get(ipnumber);
					if (mp==null) {
						mp = new BartProperties();
						barts.put(ipnumber, mp);

					}
					mp.setIpNumber(ipnumber);
					mp.setName(name);
					mp.setStatus(status);
					mp.setDefaultLogLevel(child.selectSingleNode("properties/defaultloglevel").getText());
					mp.setPreferedSmithers(child.selectSingleNode("properties/preferedsmithers").getText());
					
					if (ipnumber.equals(myip)) {
						foundmynode = true;
						retryCounter = 0;
						if (name.equals("unknown")) {
							LOG.info("This bart is not verified change its name, use smithers todo this for ip "+myip);
						} else {
							// so we have a name (verified) return true
							iamok = true;
						}
					} else {
						//System.out.println("ip was not equal!");
					}
				}	
			}
			if (!foundmynode) {
				if (retryCounter < 30) {
					//retry 30 times (= 5 min) to handle temp smithers downtime (eg daily restarts)
					retryCounter++;
				} else {
					LOG.info("LazyHomer : Creating my processing node "+LazyHomer.getSmithersUrl()  + "/domain/internal/service/bart/properties");
					String os = "unknown"; // we assume windows ?
					try{
						  os = System.getProperty("os.name");
					} catch (Exception e){
						LOG.error("LazyHomer : "+e.getMessage());
					}
					
					String newbody = "<fsxml>";
		        	newbody+="<nodes id=\""+myip+"\"><properties>";
		        	newbody+="<name>unknown</name>";
		        	newbody+="<status>off</status>";
		        	newbody+="<lastseen>"+new Date().getTime()+"</lastseen>";
		        	newbody+="<preferedsmithers>"+myip+"</preferedsmithers>";
		        	newbody+="<activesmithers>"+selectedsmithers.getIpNumber()+"</activesmithers>";

		        	// i know this looks weird but left it for future extentions
		        	if (isWindows()) {
		        		newbody+="<defaultloglevel>info</defaultloglevel>";
		        	} if (isMac()) {
		        		newbody+="<defaultloglevel>info</defaultloglevel>";
		        	} if (isUnix()) {
		        		newbody+="<defaultloglevel>info</defaultloglevel>";
		        	} else {
		        		newbody+="<defaultloglevel>info</defaultloglevel>";
		        	}
		        	newbody+="</properties></nodes></fsxml>";	
					smithers.put("/domain/internal/service/bart/properties",newbody,"text/xml");
				}
			}
		} catch (Exception e) {
			LOG.warn("Eating exception and continuing", e);
		}
		return iamok;
	}
	
	public static void setLastSeen() {
		ServiceInterface smithers = ServiceManager.getService("smithers");
		if (smithers==null) return;
		Long value = new Date().getTime();
		smithers.put("/domain/internal/service/bart/nodes/"+myip+"/properties/lastseen", ""+value, "text/xml");
	}
	
	private void initConfig() {
		LOG.info("initializing configuration.");
		
		// properties
		Properties props = new Properties();
		
		// new loader to load from disk instead of war file
		String configfilename = "/springfield/homer/config.xml";
		if (isWindows()) {
			configfilename = "c:\\springfield\\homer\\config.xml";
		}
		
		// load from file
		try {
			System.out.println("BART: INFO: Loading config file from load : "+configfilename);
			File file = new File(configfilename);

			if (file.exists()) {
				props.loadFromXML(new BufferedInputStream(new FileInputStream(file)));
			} else { 
				System.out.println("BART: FATAL: Could not load config "+configfilename);
			}
		} catch (FileNotFoundException e) {
			LOG.warn("Eating exception and continuing", e);
		} catch (IOException e) {
			LOG.warn("Eating exception and continuing", e);
		}
		
		// only get the marge communication port unless we are a smithers
		port = Integer.parseInt(props.getProperty("marge-port"));
		bart_port = Integer.parseInt(props.getProperty("default-bart-port"));
		smithers_port = Integer.parseInt(props.getProperty("default-smithers-port"));
		role = props.getProperty("role");
		if (role==null) role = "production";
		LOG.info("SERVER ROLE="+role);
	}
	
	public static String getRole() {
		return role;
	}
	

	
	public static void send(String method, String uri) {
		try {
			MulticastSocket s = new MulticastSocket();
			String msg = myip+" "+method+" "+uri;
			byte[] buf = msg.getBytes();
			DatagramPacket pack = new DatagramPacket(buf, buf.length,InetAddress.getByName(group), port);
			s.send(pack,(byte)ttl);
			s.close();
		} catch(Exception e) {
			LOG.error("LazyHomer error "+e.getMessage());
		}
	}
	
	public static Boolean up() {
		if (smithers==null) return false;
		return true;
	}
	
	public static String getSmithersUrl() {
		if (selectedsmithers==null) {
			for(Iterator<SmithersProperties> iter = smithers.values().iterator(); iter.hasNext(); ) {
				SmithersProperties s = (SmithersProperties)iter.next();
				if (s.isAlive()) {
					selectedsmithers = s;
				}
			}
		}
		// *** warning normally we would return a reference to bart not smithers
		// this is only makes sense in bart itself ****
		return "http://"+selectedsmithers.getIpNumber()+":"+selectedsmithers.getPort()+"/smithers2";
	}
	
	public static int getPort() {
		return port;
	}
	
	public void remoteSignal(String from,String method,String url) {
		if (url.indexOf("/smithers/downcheck")!=-1) {
			for(Iterator<SmithersProperties> iter = smithers.values().iterator(); iter.hasNext(); ) {
				SmithersProperties sm = (SmithersProperties)iter.next();
				if (!sm.isAlive()) {
					LOG.info("One or more smithers down, try to recover it");
					LazyHomer.send("INFO","/domain/internal/service/getname");
				}
			}
		} else {
		// only one trigger is set for now so we know its for nodes :)
		if (ins.checkKnown()) {
			// we are verified (has a name other than unknown)		
			BartProperties mp = barts.get(myip);

			if (mp!=null && mp.getStatus().equals("on")) {
				if (!running) { 
					LOG.info("This bart will be started");
					running = true;
				}
				setLogLevel(mp.getDefaultLogLevel());
			} else {
				if (running) {
					LOG.info("This bart will be turned off");
					running = false;
				} else {
					LOG.info("This bart is not turned on, use smithers todo this for ip "+myip);
				}
			}
		}
		}
	}
	
	public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}
 
	public static boolean isMac() {
 		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("mac") >= 0);
 	}
 
	public static boolean isUnix() {
 		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
 	}

	
	
	/**
	 * get root path
	 */
	public static String getRootPath() {
		return rootPath;
	}
	

 
	/**
	 * Initializes logger
	 */
    private void initLogger() {
			System.out.println("BART: Initializing logging....");

			File xmlConfig = new File("/springfield/bart/log4j.xml");
			if (xmlConfig.exists()) {
				System.out.println("BART: Reading logging config from XML file at " + xmlConfig);
				DOMConfigurator.configure(xmlConfig.getAbsolutePath());
				LOG.info("Logging configured from file: " + xmlConfig);
			}
			else {
				System.out.println("BART: Could not find log config at " + xmlConfig);
			}
			LOG.info("Initializing logging done.");
    }

	
	
	private static void setLogLevel(String level) {
		Level logLevel = Level.INFO;
		Level oldlevel = LOG.getLogger(PACKAGE_ROOT).getLevel();
		switch (loglevels.valueOf(level)) {
			case all : logLevel = Level.ALL;break;
			case info : logLevel = Level.INFO;break;
			case warn : logLevel = Level.WARN;break;
			case debug : logLevel = Level.DEBUG;break;
			case trace : logLevel = Level.TRACE;break;
			case error: logLevel = Level.ERROR;break;
			case fatal: logLevel = Level.FATAL;break;
			case off: logLevel = Level.OFF;break;
		}
		if (logLevel.toInt()!=oldlevel.toInt()) {
			LOG.getLogger(PACKAGE_ROOT).setLevel(logLevel);
			LOG.info("logging level: " + logLevel);
		}
	}
	
	public static boolean isRunning() {
		return running;
	}
 

	
    /**
     * Shutdown
     */
	public static void destroy() {
		// destroy timer
		if (marge!=null) marge.destroy();
	}
	
	private class DiscoveryThread extends Thread {
	    DiscoveryThread() {
	      super("dthread");
	      start();
	    }

	    public void run() {
	     int counter = 0;
	      while (LazyHomer.noreply || counter<10) {
	    	//if (counter>4 && LazyHomer.noreply) LOG.info("Bart: Still looking for smithers on multicast port "+port+" ("+LazyHomer.noreply+")");
	    	LazyHomer.send("INFO","/domain/internal/service/getname");
	        try {
	          sleep(500+(counter*100));
	          counter++;
	        } catch (InterruptedException e) {
	          throw new RuntimeException(e);
	        }
	      }
	     // LOG.info("Bart: Stopped looking for new smithers");
	    }
	}
	
	public static  boolean isSelectedSmithers() {
		if (selectedsmithers!=null) {
			return true;
		} else {
			return false;
		}
	}


}
