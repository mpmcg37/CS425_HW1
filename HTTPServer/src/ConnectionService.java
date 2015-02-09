import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;


public class ConnectionService implements Runnable {
	
	//Constants used in HTML Spec CRLF:Carriage Return Line Feed and SP: Space, ContentLanguage is just to simplify returns later
	private final String CRLF = "\r\n", SP = " ", CONTENT_LANGUAGE = "Content-language: en"+CRLF, CONTENT_TYPE = "Content-Type: text/html"+CRLF;
	//The Start of the server ID, added onto by the constructor
	private String Server = "Server: ";
	//Number of connections, held static over all connectionservice objects, used by HTTP Server
	private static int numConnections;

	/*
	 * Connection Socket, input stream, output stream
	 */
	private Socket connection;
	private Scanner in;
	private PrintStream out;  

	/**
	 * Create a Connection using an existing socket.
	 * Increment the number of connections
	 * @param connection - Socket created by server creating this connection service object.
	 */
	public ConnectionService(Socket connection) {
		this.connection = connection;
		//increment the number of connections
		incr();
		//Format for ServerID //XXX.XXX.X.X \n
		Server+=connection.getInetAddress().toString()+CRLF;
		//debugConnectionInfo();
		try {
			//set the input and output streams for the socket
			in = new Scanner(connection.getInputStream());
			out = new PrintStream(connection.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Increment the number of connections
	 */
	public static synchronized void incr(){ numConnections++;}
	
	/**
	 * Decrement the number of connections
	 */
	public static synchronized void decr(){ numConnections--;}
	
	/**
	 * Get the number of open connections
	 * @return numConnections
	 */
	public static synchronized int getNumConnections(){ return numConnections;}

	//Debugging method to print my IP, my Port, Server IP, Server Port
	@SuppressWarnings("unused")
	private void debugConnectionInfo() {
		System.out.println("Local IP "+connection.getLocalAddress());
		System.out.println("Local Port "+connection.getLocalPort());
		System.out.println("Remote IP "+connection.getInetAddress());
		System.out.println("Remote Port "+connection.getPort());
	}
	
	/**
	 * Implementation of http's GET format is as follows from rfc2616
	 * Response      = Status-Line               ; Section 6.1
                       *(( general-header        ; Section 4.5
                        | response-header        ; Section 6.2
                        | entity-header ) CRLF)  ; Section 7.1
                       CRLF
                       [ message-body ]          ; Section 7.2
	 * @param URI - The path to the desired file
	 * @return retval - the string to be returned in the response
	 */
	public String httpGET(String URI){
		//create a handle to the www directory
		File f = new File("www");
		//Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		String statusLine = "HTTP/1.1"+SP;
		//Set the date
		String date = "Date: "+getServerTime()+CRLF+CRLF;
		//The file to read and return
		File index = null;
		//the contents of the read file
		String messageData = "";
		//Remove the GET and HTTP from the URI
		URI = trimURI(URI);
		
		System.out.println("The trimmed URI:");
		System.out.println(URI);
		
		//reject request to get documents outside of www folder
		if(URI.contains("..")){
			
			statusLine += invalidFile();
			
		}
		else{
			System.out.println("Before checking that f is a directory");
			//ensure that the www directory exists
			if(f.isDirectory()){
				//return index.html
				System.out.println("f is a directory");
				if(URI.equals("/")){
					System.out.println("Checked that the URI equals / ");
					index = new File(f.getPath()+URI+"index.html");
				}
				else
					try {
						//is this a valid file request
						if(isSubDirectory(f, new File(f.getPath()+URI)))
							index = new File(URI);
						else //invalid file request
							statusLine += invalidFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				//error checking for index, does it exist? does it have read permission? Set index to null to indicate error occurred
				if(index==null||!index.exists()|| !index.canRead()){
					statusLine += indexError();
					index = null;
				}
					
			}else
				//directory www does not exist return an error
				statusLine += dirError();
			//if index == null then an error has occurred
			if(index != null)
				try {
					//read the contents of the file in byte form
					System.out.println("Reading the contents of the file in byte form");
					byte[] tmp = Files.readAllBytes(index.toPath());
					//convert from bytes to string
					for(int i = 0; i<tmp.length; i++)
						if(tmp[i]!=0)
							messageData+=(char)tmp[i];
					//indicate successful file read
					statusLine += OKHTTP();
					System.out.println("Status should be successful");
				
				} catch (IOException e) {
					e.printStackTrace();
				}
			
		}
		
		
		return statusLine+Server+CONTENT_TYPE+CONTENT_LANGUAGE+date+messageData;
		
	}

	//return for the OK status response
	private String OKHTTP() {
		return "200"+SP+"OK"+CRLF;
	}

	//Return value for an invalid file request, maybe just close the connection??
	private String invalidFile() {
		//System.out.println("Stopped a potential invalid file breach");
		return null;
	}

	/**
	   * Checks, whether the child directory is a subdirectory of the base 
	   * directory.
	   *
	   * @param base the base directory.
	   * @param child the suspected child directory.
	   * @return true, if the child is a subdirectory of the base directory.
	   * @throws IOException if an IOError occured during the test.
	   */
	  public boolean isSubDirectory(File base, File child)
	      throws IOException {
	      base = base.getCanonicalFile();
	      child = child.getCanonicalFile();

	      File parentFile = child;
	      while (parentFile != null) {
	          if (base.equals(parentFile)) {
	              return true;
	          }
	          parentFile = parentFile.getParentFile();
	      }
	      return false;
	  }

	//Get the server time in the format Date: Tue, 15 Nov 1994 08:12:31 GMT
	private String getServerTime() {
		    Calendar calendar = Calendar.getInstance();
		    SimpleDateFormat dateFormat = new SimpleDateFormat(
		        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		    return dateFormat.format(calendar.getTime());
	}
	
	//Directory www does not exist, Server error 
	private String dirError() {
		// TODO Auto-generated method stub
		return serverIssueHTTP();
	}

	//Issue reading index.html, Server error
	private String indexError() {
		// TODO Auto-generated method stub
		return serverIssueHTTP();
	}

	//Removes the GET and HTTP/1.1 from a URI
	private String trimURI(String URI) {
		return URI.replace("GET ", "").replace(" HTTP/1.1", "");
	}

	//@Override
	/**
	 * Run method called by Server, responds to the client
	 */
	public void run() {
		// Read the input until the connection closes or there is no more input
		while(in.hasNext()){
			String s = in.nextLine();
			System.out.println();
			System.out.println(s);
			System.out.println();
			if(s.startsWith("GET")){
				//System.out.println(httpGET(s));
				//return the GET response
				out.print(httpGET(s));
				}
			else
				//Unsupported HTTP 1.1 request
				out.print(unimplementedHTTP());
		}
		
		closeConnection();
	}
	
	//Close the connection
	private void closeConnection() {
		try {
			//decrement the number of server connections
			decr();
			connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Return value for unsupported HTTP methods
	private String serverIssueHTTP() {
			// TODO Auto-generated method stub
			return "500"+SP+"Internal Server Error"+CRLF;
		}

	//Return value for unsupported HTTP methods
	private String unimplementedHTTP() {
		// TODO Auto-generated method stub
		return "501"+SP+"Not Implemented"+CRLF;
	}
	 

}
