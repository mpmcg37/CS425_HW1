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
	
	private final String CRLF = "\r\n", SP = " ", CONTENT_LANGUAGE = "Content-language: en"+CRLF;
	private String Server = "Server: ";
	private static int numConnections;
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
		incr();
		Server+=connection.getInetAddress().toString()+CRLF;
		//debugConnectionInfo();
		try {
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
	 * @param string 
	 * @return retval
	 */
	public String httpGET(String URI){
		File f = new File("www");
		//Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		String statusLine = "HTTP/1.1"+SP;
		String date = "Date: "+getServerTime()+CRLF;
		File index = null;
		String messageData = null;
		
		URI = trimURI(URI);
		if(f.isDirectory()){
			//return index.html
			if(URI.equals("/"))
				index = new File(f.getPath()+URI+"index.html");
			else
				try {
					if(isSubDirectory(f, new File(f.getPath()+URI)))
						index = new File(URI);
					else //invalid file request
						statusLine += invalidFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//error checking for index, does it exist? does it have read permission?
			if(!index.exists()|| !index.canRead()){
				statusLine += indexError();
				index = null;
			}
				
		}else
			//directory www does not exist return an error
			statusLine += dirError();
		//if index == null then an error has occurred
		if(index != null)
			try {
			byte[] tmp = Files.readAllBytes(index.toPath());
			for(int i = 0; i<tmp.length; i++)
				if(tmp[i]!=0)
				messageData+=(char)tmp[i];
			statusLine += "200"+SP+"OK"+CRLF;
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		return statusLine+Server+CONTENT_LANGUAGE+date+"["+messageData+"]";
		
	}

	private String invalidFile() {
		// TODO Auto-generated method stub
		System.out.println("Stopped a potential invalid file breach");
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

	@Override
	public void run() {
		// Read the first line for HTTP request
		if(in.hasNext()){
			String s = in.nextLine();
			if(s.startsWith("GET")){
				System.out.println(httpGET(s));
				out.print(httpGET(s));
				}
			else
				//Unsupported HTTP 1.1 request
				out.print(unimplementedHTTP());
			
		//Read the rest of the request, maybe important later on
		while(in.hasNext()){
			s += in.nextLine();
			//System.out.println(s);
		}
		}
		try {
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
