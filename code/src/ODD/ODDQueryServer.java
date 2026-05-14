/**
 * File: src/ODD/ODDQueryServer.java
 * -------------------------------------------------------------------------------------------
 * Date			Author      Changes
 * -------------------------------------------------------------------------------------------
 * 04/03/2019	Developer		created; for DistODD Socket Query Server receiving the queries from Query Client
*/
package ODD;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
 
public class ODDQueryServer {
	protected static final long CN_LIMIT = 1*1000*1000;
	/* the global counter for time-stamping each method event */
	protected static Integer g_counter = 0;
	
	protected static ODDImpactAllInOne icAgent = null;
	public static void setICAgent(ODDImpactAllInOne _agent) {icAgent = _agent;}
	
	protected static Integer preMethod = null;
	
	protected static int g_eventCnt = 0;
	
	/* a flag ensuring the initialization and termination are both executed exactly once and they are paired*/
	protected static boolean bInitialized = false;

	private static boolean active = false;
	
	private static boolean start = false;
	
	/* buffering events */
	protected static List<Integer> B = new LinkedList<Integer>();
   
 
    public static void main(String[] args)throws IOException, InterruptedException, ExecutionException {
		ODDImpactAllInOne.setSVTG("staticVtg.dat");
		ODDImpactAllInOne.initializeGraph(true);
		if (0 != ODDImpactAllInOne.initializeGraph(false)) {
			System.out.println("Unable to load the static value transfer graph, aborted now.");
			return;
		}
		icAgent = new ODDImpactAllInOne();
		ODDMonitor.setICAgent(icAgent);
    	go(2013);
    }
    
    private static void go(int portNum)
            throws IOException, InterruptedException, ExecutionException {
 
        AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", portNum);
        serverChannel.bind(hostAddress);
         
        System.out.println("Server channel bound to port: " + hostAddress.getPort());
        System.out.println("Waiting for client to connect... ");
         
        Future acceptResult = serverChannel.accept();
        AsynchronousSocketChannel clientChannel = (AsynchronousSocketChannel) acceptResult.get();
 
        //System.out.println("Messages from client: ");
 
        if ((clientChannel != null) && (clientChannel.isOpen())) {
        	String impactSetStr="";
            while (true) 
            {
            	
                ByteBuffer buffer = ByteBuffer.allocate(10240);
                Future result = clientChannel.read(buffer);
 
                while (! result.isDone()) {
                    // do nothing
                }
 
                buffer.flip();
                String message = new String(buffer.array()).trim();
                if (message!=null && message.startsWith("bye")) {
                	 
                    break; // while loop
                 }
                if (message!=null && message.length()>1)
                {	
                	 //icAgent.resetImpOPs();
                	 processEvents();
                     System.out.println("Client method: " + message);                	
                	 ODDImpactAllInOne.setQuery(message);
                	 impactSetStr=icAgent.getDumpImpactSet(message);
                	 System.out.println(impactSetStr);
                	 //((AsynchronousSocketChannel) result).write(ByteBuffer.wrap(impactSetStr.getBytes("utf-8")));

                	 //icAgent.dumpAllImpactSets();
                }

 
                buffer.clear();
 
            } // while()
 
            clientChannel.close();
         
        } // end-if
         
        serverChannel.close();
    }
    
	protected synchronized static void processEvents() {
		System.out.println("start processing events in the buffer of " + B.size() + " events...... ");
		for (Integer _idx : B) {
			//System.out.println("event no. " + g_eventCnt++);
			//System.out.print(".");
			Integer smidx = Math.abs(_idx);
			
			if (null != preMethod && preMethod == smidx) {
				continue;
			}
			
			
			if (!start) {
				start = (ODDImpactAllInOne.getAllQueries()==null || ODDImpactAllInOne.getAllQueries().contains(smidx));
				if (!start) {
					continue;
				}
			}
			
			// enter event
			if (_idx < 0) {
				// trivially each method, once executed, is treated as impacted by itself
				if (!icAgent.getAllImpactSets().containsKey(smidx)) {
					icAgent.add2ImpactSet(smidx, smidx);
				}
				
				icAgent.onMethodEntryEvent(smidx);
			}
			// return-into event
			else {
				icAgent.onMethodReturnedIntoEvent(smidx);
			}
			
			if (null != preMethod && preMethod != smidx) {
				// close some "open" source nodes
				icAgent.closeNodes(preMethod, smidx);
			}
			
			preMethod = smidx;
		}
		System.out.println();
		g_counter = 0;
		B.clear();
	}
}