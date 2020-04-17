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
	
	public Node[] succesors;
	
	
	public Node(String ip, int port, int succ_count, boolean local) throws Exception {
		this.id = Utils.hash(ip, port, succ_count);
		this.ip = ip;
		this.port = port;
		this.succ_count = succ_count;
		this.finger = new Node[m];
		this.succesors = new Node[this.succ_count];
		
		//System.out.println("New node of id " + this.id);

		if(local && local_count > 0) {
			throw new Exception("Cannot have more than 1 local node");
		}
		else if(local && local_count == 0) {
			this.local = true;
			local_count++;
		}
	}

	public boolean isLocal() {
		return local;
	}
	
	public void create() throws Exception {
		if(!isLocal())
			throw new Exception("Cannot create() on a remote node");

		predecessor = null;
		finger[0] = this;
		startRPCServer();
	}
	
	public String getHash() {
		return this.getID().toString(16);
	}

	public void join(Node n_prime) throws Exception {
		if(!isLocal())
			throw new Exception("Cannot join() on a remote node");

		predecessor = null;
		//finger[0] = node;
		setSuccessor(n_prime.find_successor(this.getHash()));
		startRPCServer();
	}

	public Node[] getFingers() {
		return finger;
	}
	
	//Start an RPC server only on the local node
	private void startRPCServer() {
		//System.out.println("Starting the RPC server...");

		Thread t = new Thread()
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
							String key = splits[1];
							Node succ = find_successor(key);
							output.writeBytes(succ.ip + " " + succ.port + "\n");
						}
						else if(splits[0].equals("succ")) {
							output.writeBytes(finger[0].ip + " " + finger[0].port + "\n");
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
		};
		t.setName("RPC Thread");
		t.start();
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

	public synchronized void setSuccessor(Node succ) throws Exception {
		if(!isLocal())
			throw new Exception("Can't set the successor of a remote node");

		//System.out.println("Set a new successor -> " + succ.getID());
		this.finger[0] = succ;
	}

	public Node getSuccessor() throws Exception {
		if(isLocal())
			return this.finger[0];

		String response = sendRPC("succ");
		String[] split = response.split(" ");

		return new Node(split[0], Integer.parseInt(split[1]), succ_count, false);
	}

	public synchronized void setPredecessor(Node pred) throws Exception {
		if(!isLocal())
			throw new Exception("Can't get the predecessor of a remote node");

		//System.out.println("Set a new predecessor -> " + pred.getID());
		this.predecessor = pred;
	}

	public Node getPredecessor() throws Exception {
		if(isLocal())
			return predecessor;

		String response = sendRPC("predecessor");
		String[] split = response.split(" ");

		if(split[0].equals("null"))
			return null;

		return new Node(split[0], Integer.parseInt(split[1]), succ_count, false);
	}

	public void stabilize() throws Exception {
		if(!isLocal())
			throw new Exception("Can't stabilize a remote node");

		Node x = getSuccessor().getPredecessor();
		//Bruh the paper doesn't even specify the null check
		//TODO: Verify
/*		if(x != null && 
				(((x.getID().compareTo(this.getID()) == 1) && (x.getID().compareTo(finger[0].getID())) == -1) 
						|| this.equals(finger[0])))*/
		if(x != null && 
				(x.getID().compareTo(this.getID()) == 1) 
						|| this.equals(getSuccessor()))
			this.setSuccessor(x);
		this.getSuccessor().notify_node(this);
	}

	public void notify_node(Node n_prime) throws Exception {
		if(isLocal()) {
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

	public Node find_successor(String hash_id) throws Exception {
		if(isLocal()) {
			//System.out.println("Trying to find " +  hash_id);
			BigInteger id = new BigInteger(hash_id, 16);
			//System.out.println("Trying to find " +  id);

			Node successor = this.getSuccessor();
			if ((id.compareTo(this.id) == 1 && (id.compareTo(successor.id) == -1 || id.compareTo(successor.id) == 0))) {
				return successor;
			}
			else {
				//Node n_prime = closest_preceding_node(id);
				
				//TODO: not in spec, verify correctness
				//if(this.getPredecessor() == null && this.getSuccessor().equals(this)) {
				//First OR statement to prevent StackOverFlow, second or statement to handle finding successors while looping around the ring
				if(successor.equals(this) || (successor.getID().compareTo(this.getID()) == -1 && id.compareTo(successor.getSuccessor().getID()) == 1)) {
					return successor;
				}
				
				return successor.find_successor(hash_id);
			}
		}
		else {
			String res = sendRPC("find_succ " + hash_id);
			String[] splits = res.split(" ");
			//System.out.println("find_suc res: " + res);
			return new Node(splits[0], Integer.parseInt(splits[1]), succ_count, false);
		}
	}

	public void fix_fingers() throws Exception {
		this.next_fix_finger++;

		if(this.next_fix_finger >= m)
			this.next_fix_finger = 0;
		
		BigInteger power = new BigInteger("2").pow(this.next_fix_finger);
		BigInteger max_nodes = new BigInteger("2").pow(m);
		BigInteger target_id = this.getID()
				.add(power);
				//.mod(max_nodes);
		//System.out.println("Fixing: " + this.next_fix_finger);
		//System.out.println("Looking for: " + target_id);
		finger[this.next_fix_finger] = find_successor(target_id.toString(16));
		//System.out.println("fixed");
	}
	
	public Node closest_preceding_node(BigInteger id) throws Exception {
		if(!isLocal())
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
		if(isLocal())
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