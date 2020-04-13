import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Node {
	//Variables for the local node
	static int local_count = 0;
	private boolean local;
	private int succ_count;

	private BigInteger id;
	private String ip;
	private int port;

	private Node predecessor;
	private Node successor;

	public Node(String ip, int port, int succ_count, boolean local) throws Exception {
		this.id = Utils.hash(ip, port, succ_count);
		this.ip = ip;
		this.port = port;
		this.succ_count = succ_count;

		if(local && local_count > 0) {
			throw new Exception("Cannot have more than 1 local node");
		}
		else if(local && local_count == 0) {
			this.local = true;
			local_count++;
		}
	}


	public void create() throws Exception {
		if(!local)
			throw new Exception("Cannot create() on a remote node");

		predecessor = null;
		successor = this;
		startRPCServer();
	}

	public void join(Node node) throws Exception {
		if(!local)
			throw new Exception("Cannot join() on a remote node");

		predecessor = null;
		successor = node;
		startRPCServer();
	}

	//Start an RPC server only on the local node
	private void startRPCServer() {
		System.out.println("Starting the RPC server...");

		new Thread()
		{
			public void run() {
				try {
					ServerSocket server = new ServerSocket(port);

					while(true) {
						//TODO: handle multiple clients
						Socket socket = server.accept();
						BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						DataOutputStream output = new DataOutputStream(socket.getOutputStream());
						
						while(!input.ready());
						
						String command = input.readLine();
						String[] splits = command.split(" ");

						if(splits[0].equals("predecessor")) {
							if(predecessor != null)
								output.writeBytes(predecessor.ip + " " + predecessor.port + "\n");
							else
								output.writeBytes("null\n");
						}
						else if(splits[0].equals("notify")) {
							String ip = splits[1];
							int port = Integer.parseInt(splits[2]);
							notify_node(new Node(ip, port, succ_count, false));
							output.writeBytes("Ok.");
						}
						
						output.flush();
						input.close();
						output.close();
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public BigInteger getID() {
		return id;
	}

	public String getIP() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public void setSuccessor(Node succ) throws Exception {
		if(!local)
			throw new Exception("Can't set the successor of a remote node");
		this.successor = succ;
	}

	public Node getSuccessor() throws Exception {
		if(!local)
			throw new Exception("Can't get the successor of a remote node");
		return this.successor;
	}

	public void setPredecessor(Node pred) throws Exception {
		if(!local)
			throw new Exception("Can't get the predecessor of a remote node");
		this.predecessor = pred;
	}

	public Node getPredecessor() throws Exception {
		if(local)
			return predecessor;

		String response = sendRPC("predecessor");
		String[] split = response.split(" ");
		
		if(split[0].equals("null"))
			return null;
		
		return new Node(split[0], Integer.parseInt(split[1]), succ_count, false);
	}

	public void stabilize() throws Exception {
		if(!local)
			throw new Exception("Can't stabilize a remote node");

		Node x = successor.getPredecessor();
		//Bruh the paper doesn't even specify the null check
		if(x != null && (x.getID().compareTo(this.getID()) == 1) && (x.getID().compareTo(successor.getID())) == -1)
			this.setSuccessor(x);
		this.getSuccessor().notify_node(this);
	}

	public void notify_node(Node n_prime) throws Exception {
		if(local) {
			if(this.getPredecessor() == null 
					|| ((n_prime.getID().compareTo(this.getPredecessor().getID()) == 1)
							&& (n_prime.getID().compareTo(this.getID()) == -1)))
				this.setPredecessor(n_prime);
		}
		else {
			String response = sendRPC("notify " + n_prime.getIP() + " " + n_prime.getPort());
		}
	}

	public String sendRPC(String command) throws Exception {
		if(local)
			throw new Exception("Cannot make an RPC call on a local node");

		//System.out.println("Sending: " + command);
		
		DataOutputStream output;
		BufferedReader input;

		Socket socket = new Socket(this.ip, this.port);
		output = new DataOutputStream(socket.getOutputStream());
		input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		output.writeBytes(command + "\n");
		output.flush();
		String res = input.readLine();
		input.close();
		output.close();
		return res;
	}
}