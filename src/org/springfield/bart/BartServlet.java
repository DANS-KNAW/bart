/* 
 * BartServlet.java
 *
 * Copyright (c) 2012 Noterik B.V.
 *
 * This file is part of Lou, related to the Noterik Springfield project.
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
 * along with Lou.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.springfield.bart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springfield.mojo.interfaces.ServiceInterface;
import org.springfield.mojo.interfaces.ServiceManager;

/**
 * Servlet implementation class ServletResource
 * 
 * @author Daniel Ockeloen
 * @copyright Copyright: Noterik B.V. 2012
 * @package org.springfield.lou.servlet
 */
@WebServlet("/BartServlet")
public class BartServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(BartServlet.class);
	private static final long serialVersionUID = 42L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public BartServlet() {
		super();
		System.out.println("BART: servlet object created");
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doOptions(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String account = null;
		String password = null;

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");

		/*
		 * String authorization = request.getHeader("Authorization"); if
		 * (authorization!=null && authorization.startsWith("Basic")) { try {
		 * String base64 = authorization.substring("Basic".length()).trim();
		 *
		 * String values = new
		 * String(Base64.decodeBase64(base64.getBytes()),Charset
		 * .forName("UTF-8")); final String[] p = values.split(":",2); account =
		 * p[0]; password = p[1]; } catch(Exception e) { e.printStackTrace();
		 * response.setContentType("text/xml; charset=UTF-8");
		 * response.setHeader("WWW-Authenticate",
		 * "BASIC realm=\"springfield\""); response.setStatus(401); return; } }
		 * else { response.setContentType("text/xml; charset=UTF-8");
		 * response.setHeader("WWW-Authenticate",
		 * "BASIC realm=\"springfield\""); response.setStatus(401); return; }
		 */

		String uri = request.getRequestURI();

		response.setContentType("text/xml; charset=UTF-8");
		OutputStream out = response.getOutputStream();
		ServiceInterface smithers = ServiceManager.getService("smithers");
		String body = "";
		if (smithers == null) {
			body = createFsxmlError("no active smithers", 500);
		} else if (getDomain(uri).equals("internal")
				&& !account.equals("admin")) {
			body = createFsxmlError(
					"internal domain is never allowed from bart", 401);
		} else {
			String ds = request.getParameter("depth");
			String ss = request.getParameter("start");
			String ls = request.getParameter("limit");

			// some checks on the depth setting
			int depth, start = 0;
			int limit = -1;
			try {
				depth = Integer.parseInt(ds); // parse it to get in secure
			} catch (Exception e) {
				depth = 0;
			}
			try {
				start = Integer.parseInt(ss); // parse it to get in secure
			} catch (Exception e) {
				start = 0;
			}
			try {
				limit = Integer.parseInt(ls); // parse it to get in secure
			} catch (Exception e) {
				limit = -1;
			}

			if (depth < 0 || depth > 10)
				depth = 0;

			/*
			 * ServiceInterface barney = ServiceManager.getService("barney"); if
			 * (barney!=null) { // perform a security check can we read the url
			 * including the depth ?
			 */
			String duri = getDomainString(uri);
			/*
			 * String allowed =
			 * barney.get("bartallowed(read,"+duri+","+depth+","
			 * +account+","+password+")",null,null); if (allowed.equals("200"))
			 * {
			 */
			String xml = "<fsxml><properties><depth>" + depth
					+ "</depth><start>" + start + "</start><limit>" + limit
					+ "</limit></properties></fsxml>";
			body = smithers.get(duri, xml, "text/xml");
			response.setStatus(200);
			/*
			 * } else if (allowed.equals("403")) { body =
			 * createFsxmlError("user : "
			 * +account+" not allowed on that uri or depth",403);
			 * response.setStatus(403); } else {
			 * response.setContentType("text/xml; charset=UTF-8");
			 * response.setHeader("WWW-Authenticate",
			 * "BASIC realm=\"springfield\""); response.setStatus(401); } } else
			 * { body =
			 * createFsxmlError("can't reach barney to check security",500); }
			 */
		}
		out.write(body.getBytes());
		out.flush();
		out.close();
		return;
	}
	
	/**
	 * Post request handles mainly external requests
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String account = null;
		String password = null;

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");

		/*
		 * String authorization = request.getHeader("Authorization"); if
		 * (authorization!=null && authorization.startsWith("Basic")) { try {
		 * String base64 = authorization.substring("Basic".length()).trim();
		 * String values = new
		 * String(Base64.decodeBase64(base64.getBytes()),Charset
		 * .forName("UTF-8")); final String[] p = values.split(":",2); account =
		 * p[0]; password = p[1]; } catch(Exception e) { e.printStackTrace();
		 * response.setContentType("text/xml; charset=UTF-8");
		 * response.setHeader("WWW-Authenticate",
		 * "BASIC realm=\"springfield\""); response.setStatus(401); return; } }
		 * else { response.setContentType("text/xml; charset=UTF-8");
		 * response.setHeader("WWW-Authenticate",
		 * "BASIC realm=\"springfield\""); response.setStatus(401); return; }
		 */

		String uri = request.getRequestURI();

		response.setContentType("text/xml; charset=UTF-8");
		OutputStream out = response.getOutputStream();
		ServiceInterface smithers = ServiceManager.getService("smithers");
		String body = "";
		if (smithers == null) {
			body = createFsxmlError("no active smithers", 500);
		} else if (getDomain(uri).equals("internal")
				&& !account.equals("admin")) {
			body = createFsxmlError(
					"internal domain is never allowed from bart", 401);
		} else {
			String ds = request.getParameter("depth");

			// some checks on the depth setting
			int depth = 0;
			try {
				depth = Integer.parseInt(ds); // parse it to get in secure
			} catch (Exception e) {
				depth = 0;
			}
			if (depth < 0 || depth > 10)
				depth = 0;

			/*
			 * ServiceInterface barney = ServiceManager.getService("barney"); if
			 * (barney!=null) { // perform a security check can we read the url
			 * including the depth ?
			 */
			String duri = getDomainString(uri);
			/*
			 * String allowed =
			 * barney.get("bartallowed(write,"+duri+","+depth+","
			 * +account+","+password+")",null,null); if (allowed.equals("200"))
			 * {
			 */
			InputStream inst = request.getInputStream();
			String data;

			java.util.Scanner s = new java.util.Scanner(inst)
					.useDelimiter("\\A");
			data = (s.hasNext()) ? s.next() : null;

			if (data == null) {
				return;
			}
			String xml = data;
			String mimetype = "text/xml";

			try {
				Document doc = DocumentHelper.parseText(xml);
				mimetype = doc.selectSingleNode("/fsxml/@mimetype") == null ? mimetype
						: doc.selectSingleNode("/fsxml/@mimetype").getText();
			} catch (DocumentException e) {
			}

			body = smithers.post(duri, xml, mimetype);
			response.setStatus(200);
			/*
			 * } else if (allowed.equals("403")) { body =
			 * createFsxmlError("user : "
			 * +account+" not allowed on that uri or depth",403);
			 * response.setStatus(403); } else {
			 * response.setContentType("text/xml; charset=UTF-8");
			 * response.setHeader("WWW-Authenticate",
			 * "BASIC realm=\"springfield\""); response.setStatus(401); } } else
			 * { body =
			 * createFsxmlError("can't reach barney to check security",500); }
			 */
		}
		out.write(body.getBytes());
		out.flush();
		out.close();
		return;
	}

	protected void doPut(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String account = null;
		String password = null;

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");

		/*
		 * String authorization = request.getHeader("Authorization"); if
		 * (authorization!=null && authorization.startsWith("Basic")) { try {
		 * String base64 = authorization.substring("Basic".length()).trim();
		 * String values = new
		 * String(Base64.decodeBase64(base64.getBytes()),Charset
		 * .forName("UTF-8")); final String[] p = values.split(":",2); account =
		 * p[0]; password = p[1]; } catch(Exception e) { e.printStackTrace();
		 * response.setContentType("text/xml; charset=UTF-8");
		 * response.setHeader("WWW-Authenticate",
		 * "BASIC realm=\"springfield\""); response.setStatus(401); return; } }
		 * else { response.setContentType("text/xml; charset=UTF-8");
		 * response.setHeader("WWW-Authenticate",
		 * "BASIC realm=\"springfield\""); response.setStatus(401); return; }
		 */

		String uri = request.getRequestURI();

		response.setContentType("text/xml; charset=UTF-8");
		OutputStream out = response.getOutputStream();
		ServiceInterface smithers = ServiceManager.getService("smithers");
		String body = "";
		if (smithers == null) {
			body = createFsxmlError("no active smithers", 500);
		} else if (getDomain(uri).equals("internal")
				&& !account.equals("admin")) {
			body = createFsxmlError(
					"internal domain is never allowed from bart", 401);
		} else {
			String ds = request.getParameter("depth");

			// some checks on the depth setting
			int depth = 0;
			try {
				depth = Integer.parseInt(ds); // parse it to get in secure
			} catch (Exception e) {
				depth = 0;
			}
			if (depth < 0 || depth > 10)
				depth = 0;

			/*
			 * ServiceInterface barney = ServiceManager.getService("barney"); if
			 * (barney!=null) { // perform a security check can we read the url
			 * including the depth ?
			 */
			String duri = getDomainString(uri);
			/*
			 * String allowed =
			 * barney.get("bartallowed(write,"+duri+","+depth+","
			 * +account+","+password+")",null,null); if (allowed.equals("200"))
			 * {
			 */

			InputStream inst = request.getInputStream();
			String data;

			java.util.Scanner s = new java.util.Scanner(inst)
					.useDelimiter("\\A");
			data = (s.hasNext()) ? s.next() : null;

			if (data == null) {
				return;
			}
			String xml = data;
			body = smithers.put(duri, xml, "text/xml");
			response.setStatus(200);
			/*
			 * } else if (allowed.equals("403")) { body =
			 * createFsxmlError("user : "
			 * +account+" not allowed on that uri or depth",403);
			 * response.setStatus(403); } else {
			 * response.setContentType("text/xml; charset=UTF-8");
			 * response.setHeader("WWW-Authenticate",
			 * "BASIC realm=\"springfield\""); response.setStatus(401); } } else
			 * { body =
			 * createFsxmlError("can't reach barney to check security",500); }
			 */
		}
		out.write(body.getBytes());
		out.flush();
		out.close();
		return;
	}

	protected void doDelete(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");

		String uri = request.getRequestURI();
		response.setContentType("text/xml; charset=UTF-8");
		OutputStream out = response.getOutputStream();

		ServiceInterface smithers = ServiceManager.getService("smithers");
		String body = "";
		if (smithers == null) {
			body = createFsxmlError("no active smithers", 500);
		} else {
			String duri = getDomainString(uri);
			body = smithers.delete(duri, null, null);
			response.setStatus(200);
		}

		out.write(body.getBytes());
		out.flush();
		out.close();
		return;
	}

	public String getDomain(String uri) {
		String result = uri.substring(uri.indexOf("/domain/") + 8);
		result = result.substring(0, result.indexOf('/'));
		return result;
	}

	public String getDomainString(String uri) {
		String result = uri.substring(uri.indexOf("/domain/"));
		return result;
	}

	private String createFsxmlError(String reason, int code) {
		return "<fsxml><error><properties><code>" + code + "</code><reason>"
				+ reason + "</reason></properties></error></fsxml>";
	}

}
