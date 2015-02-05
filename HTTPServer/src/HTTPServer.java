import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class HTTPServer {
	
	private static final int MAX_CONNECTIONS = 10;
	private static int port = 8080;
	private ServerSocket ss;
	
	public HTTPServer(int port) {
		try{
			ss = new ServerSocket(port);
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if(args.length>0)
			port  = Integer.parseInt(args[0]);
		HTTPServer myhttp = new HTTPServer(port);
		myhttp.start();
	}

	public void start() {
		try{
			while(true){
				//"Listening";
				if(ConnectionService.getNumConnections()<MAX_CONNECTIONS){
					Socket connection = ss.accept();
					//"Serving";
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
