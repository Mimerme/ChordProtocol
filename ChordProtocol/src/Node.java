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

	//Number of bits from the hash to truncate. Utils.hash doesn't consider this since it's all 160 bits in this project
	final int m = 160;

	private boolean local;
	private int succ_count;

	private BigInteger id;
	private String ip;
	private int port;

	public Node[] finger;
	private int next_fix_finger = 0;
	private Node predecessor;

	public Node(String ip, int port, int succ_count, boolean local) throws Exception {
		this.id = Utils.hash(ip, port, succ_count);
		this.ip = ip;
		this.port = port;
		this.succ_count = succ_count;
		this.finger = new Node[m];

		//System.out.println("New node of id " + this.id);

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
		finger[0] = this;
		startRPCServer();
	}

	public void join(Node n_prime) throws Exception {
		if(!local)
			throw new Exception("Cannot join() on a remote node");

		predecessor = null;
		//finger[0] = node;
		setSuccessor(n_prime.find_successor(this));
		startRPCServer();
	}

	//Start an RPC server only on the local node
	private void startRPCServer() {
		//System.out.println("Starting the RPC server...");

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
						else if(splits[0].equals("find_succ")) {
							String ip = splits[1];
							int port = Integer.parseInt(splits[2]);
							Node succ = find_successor(new Node(ip, port, succ_count, false));
							output.writeBytes(succ.ip + " " + succ.port + "\n");
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

		System.out.println("Set a new successor -> " + succ.getID());
		this.finger[0] = succ;
	}

	public Node getSuccessor() throws Exception {
		if(!local)
			throw new Exception("Can't get the successor of a remote node");
		return this.finger[0];
	}

	public void setPredecessor(Node pred) throws Exception {
		if(!local)
			throw new Exception("Can't get the predecessor of a remote node");

		System.out.println("Set a new predecessor -> " + pred.getID());
		this.predecessor = pred;
	}

	public synchronized Node getPredecessor() throws Exception {
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

		Node x = finger[0].getPredecessor();
		//Bruh the paper doesn't even specify the null check
		//TODO: Verify
/*		if(x != null && 
				(((x.getID().compareTo(this.getID()) == 1) && (x.getID().compareTo(finger[0].getID())) == -1) 
						|| this.equals(finger[0])))*/
		if(x != null && 
				(x.getID().compareTo(this.getID()) == 1) 
						|| this.equals(finger[0]))
			this.setSuccessor(x);
		this.getSuccessor().notify_node(this);
	}

	public synchronized void notify_node(Node n_prime) throws Exception {
		if(local) {
			//TODO: Verify
/*			if(this.getPredecessor() == null 
					|| ((n_prime.getID().compareTo(this.getPredecessor().getID()) == 1)
							&& (n_prime.getID().compareTo(this.getID()) == -1))) {*/
			if(this.getPredecessor() == null 
					|| (n_prime.getID().compareTo(this.getPredecessor().getID()) == 1)) {
				this.setPredecessor(n_prime);
			}
		}
		else {
			String response = sendRPC("notify " + n_prime.getIP() + " " + n_prime.getPort());
		}
	}

	public synchronized Node find_successor(Node node_id) throws Exception {
		
		if(local) {
			BigInteger id = node_id.getID();
			Node successor = this.getSuccessor();
			if (id.compareTo(this.id) == 1 && (id.compareTo(successor.id) == -1 || id.compareTo(successor.id) == 0)) {
				return successor;
			}
			else {
				Node n_prime = closest_preceding_node(id);
				
				//TODO: not in spec, verify correctness
				//if(this.getPredecessor() == null && this.getSuccessor().equals(this)) {
				if(n_prime.equals(this)) {
					return successor;
				}
				
				return successor.find_successor(node_id);
			}
		}
		else {
			String res = sendRPC("find_succ " + node_id.getIP() + " " + node_id.getPort());
			String[] splits = res.split(" ");
			return new Node(splits[0], Integer.parseInt(splits[1]), succ_count, false);
		}
	}

	public synchronized void fix_fingers() {
		if(this.next_fix_finger > m)
			this.next_fix_finger = 0;
		//finger[this.next_fix_finger] 
		this.next_fix_finger++;
	}
	
	public synchronized Node closest_preceding_node(BigInteger id) throws Exception {
		if(!local)
			throw new Exception("Cannot call closest_preceding_node() on a remote node");
		
		for (int i = m - 1; i >= 0; i--) {
			Node node_i = finger[i];
			
			if(node_i == null)
				continue;
			
			BigInteger i_id = node_i.getID();

			if(i_id.compareTo(this.getID()) == 1 && i_id.compareTo(id) == -1)
				return node_i;
		}

		return this;
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