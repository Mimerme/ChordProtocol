import java.text.ParseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class MainCreate {

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
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
		int fix_fingers_time;
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
			System.out.println("Creating ring");
			localNode.create();
		}
		else {
			System.out.println("Joining ring");
			localNode.join(new Node(join_address, join_port, succ_count, false));
		}

		
		while(true) {

			try {
				Thread.sleep(stablize_time);
				System.out.println("stabilizing...");
				localNode.stabilize();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//Start the stabilize thread
/*		new Thread()
		{
			public void run() {

				}
			}
		}.start();*/
	}

}
