
public class MainJoin {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String binding_address = "127.0.0.1";
		int binding_port = 9000;

		String join_address = "127.0.0.1";
		int join_port = 8080;

		//Between 1 - 60000
		int stablize_time = 1000;
		//Between 1 - 60000
		int fix_fingers_time;
		//Between 1 - 60000
		int check_pred_time;
		//Between 1 - 32 (this is m?)
		int succ_count = 7;
		
		System.out.println("Joining ring");
		Node localNode = new Node(binding_address, binding_port, succ_count, true);
		localNode.join(new Node(join_address, join_port, succ_count, false));
		
		//Start the stabilize thread
		new Thread()
		{
			public void run() {
				while(true) {

					try {
						Thread.sleep(stablize_time);
						localNode.stabilize();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

}
