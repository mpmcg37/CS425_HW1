import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class HTTPServer {
	
	private static final int MAX_CONNECTIONS = 10;
	private ServerSocket ss;
	
	public HTTPServer(int port) {
		try{
			ss = new ServerSocket(port);
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		HTTPServer myhttp = new HTTPServer(8080);
		myhttp.start();
	}

	public void start() {
		try{
			while(true){
				//System.out.println("Listening");
				if(ConnectionService.getNumConnections()<MAX_CONNECTIONS){
					Socket connection = ss.accept();
					//System.out.println("Serving");
					(new Thread(new ConnectionService(connection))).start();
				}
				else
					try {
						//sleep for 3 seconds if max connections is exceeded
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}

}
