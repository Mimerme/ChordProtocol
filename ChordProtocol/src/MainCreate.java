import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class MainCreate {

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addRequiredOption( "a", "address", true, "The IP address that the Chord client will bind to, as well as advertise to other nodes" );
		options.addRequiredOption( "p", "port",true, "The port that the Chord client will bind to and listen on." );
		options.addRequiredOption( "r", "successors",true, "The number of successors maintained by the Chord client." );

		options.addOption( "ja", "join_address", true, "The IP address of the machine running a Chord node. The Chord client will join this node’s ring.");
		options.addOption( "jp", "join_port", true, "The port that an existing Chord node is bound to and listening on. The Chord client will join this node’s ring.");

		options.addRequiredOption( "ts", "time_stab", true, " The time in milliseconds between invocations of ‘stabilize’.");
		options.addRequiredOption( "tff", "time_fix_fingers", true, " The time in milliseconds between invocations of ‘fix_fingers’.");
		options.addRequiredOption( "tcp", "time_check_pred", true, " The time in milliseconds between invocations of ‘check_predecessor’.");

		// parse the command line arguments
		CommandLine line = parser.parse( options, args );

		// TODO Auto-generated method stub
		String binding_address = null;
		int binding_port = 0;

		String join_address = null;
		int join_port = 0;

		//Between 1 - 60000
		int stablize_time = 0;
		//Between 1 - 60000
		int fix_fingers_time = 0;
		//Between 1 - 60000
		int check_pred_time;
		//Between 1 - 32 (this is m?) Nope.
		int succ_count = 7;

		// validate that block-size has been set
		if(line.hasOption( 'a' ))
			binding_address = line.getOptionValue('a');
		if(line.hasOption( 'p' ))
			binding_port = Integer.parseInt(line.getOptionValue('p'));
		if(line.hasOption( 'r' ))
			succ_count = Integer.parseInt(line.getOptionValue('r'));

		if(line.hasOption( "ja" ))
			join_address = line.getOptionValue("ja");
		if(line.hasOption( "jp" ))
			join_port = Integer.parseInt(line.getOptionValue("jp"));

		if(line.hasOption( "ts" ))
			stablize_time = Integer.parseInt(line.getOptionValue("ts"));
		if(line.hasOption( "tff" ))
			fix_fingers_time = Integer.parseInt(line.getOptionValue("tff"));
		if(line.hasOption( "tcp" ))
			check_pred_time = Integer.parseInt(line.getOptionValue("tcp"));

		Node localNode = new Node(binding_address, binding_port, succ_count, true);

		if(join_address == null) {
			//System.out.println("Creating ring");
			localNode.create();
		}
		else {
			//System.out.println("Joining ring");
			localNode.join(new Node(join_address, join_port, succ_count, false));
		}

		//System.out.println("Local Node ID: " + localNode.getID());

		class StabilizeThread extends Thread{
			private int delay;
			private Node localnode;

			public StabilizeThread(int delay, Node localnode) {
				this.delay = delay;
				this.localnode = localnode;
				this.setName("Stabilize Thread");
			}

			public void run() {
				while(true) {
					try {
						Thread.sleep(delay);
						if(localnode.getPredecessor() == null && localnode.getSuccessor().equals(localnode))
							continue;

						//System.out.println("Stabilizing");
						localnode.stabilize();
						//System.out.println("Stabilizing done");

						//System.out.println("Updating fingers");
						//System.out.println("Updating fingers done");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		class FixFingersThread extends Thread{
			private int delay;
			private Node localnode;

			public FixFingersThread(int delay, Node localnode) {
				this.delay = delay;
				this.localnode = localnode;
			}

			public void run() {
				while(true) {
					try {
						Thread.sleep(delay);
						if(localnode.getPredecessor() == null)
							continue;

						//System.out.println("Updating fingers");
						localnode.fix_fingers();
						//System.out.println("Updating fingers done");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}


		new StabilizeThread(stablize_time, localNode).start();
		new FixFingersThread(fix_fingers_time, localNode).start();

		Scanner sc = new Scanner(System.in); 
		try {
			while(true) {
				String command = sc.nextLine(); 
				String[] splits = command.split(" ");

				if(splits[0].equals("PrintStateTest")) {
					System.out.println("Self " + localNode.getID());
					System.out.println("Predecessor " + localNode.getPredecessor().getID());
					System.out.println("Successor " + localNode.getSuccessor().getID());
				}
				else if(splits[0].equals("PrintState")) {
					System.out.println("Self " + localNode.getHash());

					Node[] succs = localNode.succesors;
					for(int i = 0; i < succs.length; i++) {
						Node n = succs[i];
						if(n == null) {
							System.out.println("Successor [" + (i + 1) + "] null");
							continue;
						}

						System.out.println("Successor " + (i + 1) + "] " + n.getHash() + " " + n.getIP() + " " + n.getPort());
					}

					Node[] fingers = localNode.getFingers();
					for(int i = 0; i < fingers.length; i++) {
						Node n = fingers[i];
						if(n == null) {
							System.out.println("Finger [" + (i + 1) + "] null");
							continue;
						}

						System.out.println("Finger [" + (i + 1) + "] " + n.getHash() + " " + n.getIP() + " " + n.getPort());
					}
				}
				else if(splits[0].equals("Lookup")) {
					String hash = Utils.hash(splits[1]).toString(16);
					System.out.println(splits[1] + " " + hash);
					Node n = localNode.find_successor(hash);
					System.out.println(n.getHash() + " " + n.getIP() + " " + n.getPort());
				}
			}
		}
		catch(NoSuchElementException e) {
			System.exit(1);
		}

	}
}
