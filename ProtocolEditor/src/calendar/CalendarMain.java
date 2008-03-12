
/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 *	author Will Moore will@lifesci.dundee.ac.uk
 */

package calendar;

import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JFrame;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tree.DataFieldConstants;
import ui.components.FileChooserReturnFile;
import util.XMLMethods;

public class CalendarMain {
	
	public static DateFormat timeFormat = DateFormat.getTimeInstance (DateFormat.DEFAULT);
	public static DateFormat dateFormat = DateFormat.getDateInstance( DateFormat.DEFAULT);
	
	/**
	 * For testing.
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException {
		
		testAddCalendar();
		
		//populateDB();
		
		//getMonthResults();
		
		// doesn't work!!
		//clearDBTables();
	}
	
	public static void testAddCalendar() throws SQLException{
		
		CalendarObject calendar = new CalendarObject("Home", "I do stuff at home");
		
		CalendarDataBase calDB = new CalendarDataBase();
		
		int calID = calDB.saveCalendar(calendar);
		
		
		CalendarEvent calEvent = new CalendarEvent(new Date());
		
		calDB.saveEvent(calEvent, calID);
		
		
		calDB.shutdown();
	}
	
	
	public static void clearDBTables() throws SQLException {
		
		CalendarDataBase calDB = new CalendarDataBase();
		calDB.clearTables();
		
		calDB.shutdown();
	}
	
	public static void getMonthResults() throws SQLException{
		
		 GregorianCalendar thisMonth = new GregorianCalendar();
			thisMonth.set(Calendar.DAY_OF_MONTH, 1);
			thisMonth.set(Calendar.HOUR_OF_DAY, 0);
			
			CalendarDataBase calDB = new CalendarDataBase();
			List<CalendarFile> calendarFiles = calDB.getCalendarFilesForMonth(thisMonth);
			calDB.shutdown();
			
			MonthView monthView = new MonthView();
			for (CalendarFile calFile: calendarFiles) {
				monthView.addCalendarFile(calFile);
			}
			
			JFrame frame = new JFrame("MonthView");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			frame.getContentPane().add(monthView);
			
			frame.pack();
			frame.setVisible(true);
			
			
	}
	
	public static void populateDB() throws SQLException {
		// Create a file chooser, get a file from user
		FileChooserReturnFile fc = new FileChooserReturnFile(new String[] {"xml"}, null);
		File file = fc.getFileFromUser();
		
		if (file == null)
			return;
		
		CalendarDataBase calDB = new CalendarDataBase();
		//calDB.clearTables();
		indexFilesToCalendar(file);
		calDB.shutdown();
	}
	
	/**
	 * Iterate through a file system (starting at root directory), adding files to the calendar.
	 * For each file, check to see if it contains DateTime Fields. Needs to contain at least one DateTime
	 * field in order to be added to the list of calendarFiles. 
	 * 
	 * @param rootDirectory
	 */
	public static void indexFilesToCalendar(File rootDirectory) {
		
		if (rootDirectory.isDirectory()) {
	        String[] files = rootDirectory.list();
	        // an IO error could occur
	        if (files != null) {
	          for (int i = 0; i < files.length; i++) {
	        	  indexFilesToCalendar(new File(rootDirectory, files[i]));
	          }
	        }
	      } else {
	        if (fileContainsDates(rootDirectory)) {
	        	CalendarFile calendarFile = new CalendarFile(rootDirectory);
	        	
	        	// DB not fully working yet!
	        	//saveCalendarFileToDB(calendarFile);
	        	
	        	// use this for now! 
	        	//calendarFiles.add(calendarFile);
	        }
	      }
	}
	
	/**
	 * Test to see if a file contains at least one DateTimeField
	 * ie. Does the XML file contain at least one Element named "DateTimeField" (DataFieldConstants.DATE_TIME_FIELD).
	 * 
	 * @param file		The XML file
	 * @return			True if at least one Element <DateTimeField /> exists in the XML file
	 * 					returns false if file can't be read. 
	 */
	public static boolean fileContainsDates(File file) {
		
		// if it isn't an XML file, forget it!
		String fileName = file.getName();
		if (!(fileName.endsWith(".xml"))) {
			return false;
		}
		
		// Try to open XML file, and see if it has any Elements named <DateTimeField>
		try {
			Document domDoc = XMLMethods.readXMLtoDOM(file);
			NodeList dateTimeNodes = domDoc.getElementsByTagName(DataFieldConstants.DATE_TIME_FIELD);
			return (dateTimeNodes.getLength() > 0);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
	}

}
