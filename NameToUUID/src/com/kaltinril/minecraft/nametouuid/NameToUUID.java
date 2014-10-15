package com.kaltinril.minecraft.nametouuid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NameToUUID {
	
	public static void main(String[] arg) {
		String inFilename = "";
		String outFilename = "";
		String fileSeperator = System.getProperty("file.separator");
		String lineSeperator = System.getProperty("line.separator");
		String failedUsernames = "";
		
		// Variables for use with the file access
		FileReader inFileReader = null;
		BufferedReader inBufferedReader = null;
		FileWriter outFileWriter = null;
		BufferedWriter outBufferedWriter = null;
		
		int row = 0;
		
		// Gather the path and filename of the source file, create the destination file
		// Be extra careful and print out friendly error messages if the filename was not identified
		if (arg.length == 1){
			inFilename = arg[0];
			int seperatorLocation = inFilename.lastIndexOf(fileSeperator);
			if (inFilename.length() > 0){
				if (seperatorLocation > 0){
					seperatorLocation ++;
					outFilename = inFilename.substring(0, seperatorLocation) + "whitelist.json";
				}else{
					outFilename = "whitelist.json";
				}
			}else{
				System.out.println("ERROR: Invalid filename supplied as input");
				System.exit(1);
			}
		}else{
			System.out.println("Usage: java NameToUUID.jar /path/to/file/white-list.txt");
			System.exit(1);
		}
		
		// Show the user what we identified as the input and output files.
		System.out.println("Input File: " + inFilename);
		System.out.println("Output File: " + outFilename);

		// Open the source and destination files for reading and writing.
		try{
			inFileReader = new FileReader(inFilename);
			inBufferedReader = new BufferedReader(inFileReader);
			
			outFileWriter = new FileWriter(outFilename);
			outBufferedWriter = new BufferedWriter(outFileWriter);
		}
		catch(IOException e){
			System.out.println("ERROR: Unable to open source or destinatoin file!" + e.toString());
			System.exit(1);
		}
		
		// Read the source file line by line, converting it, and storing in the output file
		try{
			String username = "";
			String UUIDCombined = "";

			outBufferedWriter.write("[" + lineSeperator);	// Write the opening JSON bracket to the file.
			while ((username = inBufferedReader.readLine()) != null){
				row++;
				System.out.println("Converting: [" + username + "]");
				
				// Lookup the UUID from the Minecraft API URL, passing the username gathered from the file
				UUIDCombined = getMinecraftUUID(username);
				
				if (UUIDCombined.contains("ERROR:")){
					System.out.println(" - Failed to Convert user [" + username + "]" + lineSeperator + UUIDCombined);
					failedUsernames = failedUsernames + username + ",";
					row--;
				}else{
					// Correct "id" to "uuid"
					UUIDCombined = UUIDCombined.replace("\"id\"", "\"uuid\"");
					
					// Correct the UUID format to include dashes
					UUIDCombined = UUIDCombined.substring(0, 18) + "-" +
								   UUIDCombined.substring(18, 22) + "-" +
								   UUIDCombined.substring(22, 26) + "-" +
								   UUIDCombined.substring(26, 30) + "-" +
								   UUIDCombined.substring(30); 
					
					// Display the converted username
					System.out.println("         " + UUIDCombined);
					
					// Write each UUID + Username JSON entry to the file
					if (row == 1)
						outBufferedWriter.write(" " + UUIDCombined + lineSeperator); //Add a space to the first one for visual ease when manually editing the file
					else {
						outBufferedWriter.write("," + UUIDCombined + lineSeperator);
					}
				}
			}
			outBufferedWriter.write("]");	// Write the closing JSON bracket to the file.
		}
		catch (IOException e){
			System.out.println("ERROR: Issue reading source or writing destination file!" + e.toString());
			System.exit(1);
		}
		finally{
			// Close the file handles
			try{
				inBufferedReader.close();
				inFileReader.close();
				outBufferedWriter.close();
				outFileWriter.close();
			}
			catch (IOException e){
				System.out.println("Error closing the files, ignoring.");
			}
		}
		
		// Pretty up the username list by removing the last comma
		if (failedUsernames.length() > 0){
			failedUsernames = failedUsernames.substring(0, failedUsernames.length() - 1);
			System.out.println("Completed " + row + " usernames successfully.");
			System.out.println(" - Errored Usernames: " + failedUsernames);
		}else{
			System.out.println("Completed " + row + " usernames successfully.");
		}
	}

	private static String getMinecraftUUID(String username) {
		String baseURL = "https://api.mojang.com/users/profiles/minecraft/";
		URL url;
		InputStream is = null;
		BufferedReader br;
		String line;
		String UUIDCombined = "";
		HttpURLConnection connection = null;
		
		// http://stackoverflow.com/questions/238547/how-do-you-programmatically-download-a-webpage-in-java
		// http://stackoverflow.com/questions/6467848/how-to-get-http-response-code-for-a-url-in-java
		try{
			url = new URL(baseURL + username);
			
			//Open a connection and check the Status Return code of the page.
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			
			int code = connection.getResponseCode();
			
			if (code == 200){
				is = connection.getInputStream();
				br = new BufferedReader(new InputStreamReader(is));
				
				//Loop through all lines, store in the UUIDCombined string
				while ((line = br.readLine()) != null) {
					UUIDCombined = UUIDCombined + line;
			    }
			}else{
				//Issue converting, send back "error:" to be printed
				UUIDCombined = "ERROR: " + code + " " + connection.getResponseMessage();
			}
		} catch (MalformedURLException mue) {
		     mue.printStackTrace();
		     UUIDCombined = "ERROR: Malformed URL Exception";
		} catch (IOException ioe) {
		     ioe.printStackTrace();
		     UUIDCombined = "ERROR: Input/Output Exception";
		} finally {
		    try {
		        if (is != null) is.close();
		        if (connection != null) connection.disconnect();
		    } catch (IOException ioe) {
		        // nothing to see here
		    }
		}
		return UUIDCombined;
	}
}
