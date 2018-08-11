/* 
* BartContextListener.java
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
package org.springfield.bart;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springfield.bart.homer.*;

public class BartContextListener implements ServletContextListener {
	
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("BART: context initialized");
		ServletContext servletContext = event.getServletContext();
		
		// turn logging off
		Logger.getLogger("").setLevel(Level.SEVERE);
		
 		LazyHomer lh = new LazyHomer();
		lh.init(servletContext.getRealPath("/"));		
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("BART: context destroyed");
		LazyHomer.destroy();
	}

}
