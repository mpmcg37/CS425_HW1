import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;


public class ConnectionService implements Runnable {
	
	private static int numConnections;
	private Socket connection;
	private Scanner in;
	private PrintStream out;

	public ConnectionService(Socket connection) {
		this.connection = connection;
		incr();
		debugConnectionInfo();
		try {
			in = new Scanner(connection.getInputStream());
			out = new PrintStream(connection.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static synchronized void incr(){ numConnections++;}
	public static synchronized void decr(){ numConnections--;}
	public static synchronized int getNumConnections(){ return numConnections;}

	private void debugConnectionInfo() {
		System.out.println("Local IP "+connection.getLocalAddress());
		System.out.println("Local Port "+connection.getLocalPort());
		System.out.println("Remote IP "+connection.getInetAddress());
		System.out.println("Remote Port "+connection.getPort());
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(in.hasNext()){
			String s = in.nextLine();
			System.out.println("Recieved string is: "+s);
			out.println(s);
		}
		
		try {
			decr();
			connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}