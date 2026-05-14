/**
 * File: src/Diver/DadsEAMonitorAllInOne.java
 * -------------------------------------------------------------------------------------------
 * Date			Author      	Changes
 * -------------------------------------------------------------------------------------------
 * 03/22/2019	Developer		created; for Dads Monitor
*/
package Dads;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import Dads.TaskTest.Task;
import QL.Qlearner;

import logging.Logger;
class MySocketServer extends Thread {   
	//private static final Logger logger=Logger.getLogger(DadsMonitor.class); 	
    @Override
    public void run() {
    	int socketPort=getPortNum();
        try {
        	AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();			
	        InetSocketAddress hostAddress = new InetSocketAddress("localhost", getPortNum());
	        serverChannel.bind(hostAddress);	         
	        System.out.println("NIO2Server channel bound to port: " + hostAddress.getPort());
       	    //logger.info("DadsMonitor NIO2Server channel bound to port: " + hostAddress.getPort());
            String query="";
            String impactSetStr="";
            while (true) {
                Future acceptResult = serverChannel.accept();
                AsynchronousSocketChannel clientChannel = (AsynchronousSocketChannel) acceptResult.get();
                ByteBuffer buffer = ByteBuffer.allocate(10240);
                Future result = clientChannel.read(buffer);
                result.get();
                while (! result.isDone()) {
                    // do nothing
                } 
                buffer.flip();
                query = new String(buffer.array()).trim();
                System.out.println("Client say : " + query); 
                if (query!=null && query.length()>1)
                {	
                	 impactSetStr=DadsMonitor.getImpactSetStr(query);
                	 System.out.println("impactSetStr = " + impactSetStr);
                	 //logger.info("DadsMonitor impactSetStr = " + impactSetStr);
          			buffer = ByteBuffer.wrap(impactSetStr.getBytes("UTF-8"));
         			clientChannel.write(buffer);
                }
                buffer.clear();
                clientChannel.close();
            }
        }catch (Exception e) {
            System.out.println("Exception:" + e);
        }finally{
//          serverSocket.close();
        }
    }
	public static String getProcessID() {
		return ManagementFactory.getRuntimeMXBean().getName()+'\0';
	}
	
	public static int getPortNum() {
		int portNum=2000;
		String processStr=getProcessID();
		//System.out.println("getProcessID()="+processStr); 
		int processID=Integer.parseInt(processStr.split("@")[0]);
		if (portNum<=2000)
		{
			portNum=2000+processID%10;
		}
		else	
			portNum++;
		//System.out.println("getPortNum()="+portNum);
		return portNum;
//		return 2000+(int)(Math.random()*10+1);
	}
}

public class DadsMonitor {
	protected static long CN_LIMIT = 1*100;
	/* the global counter for time-stamping each method event */
	protected static Integer g_counter = 0;	
	protected static long CN_LIMIT_QUEUE = 5*CN_LIMIT;
	/* the global counter for queueing time-stamping each method event */
	protected static Integer g_counter_queue = 0;
	protected static long TIME_SPAN = 60000;
	protected static DadsImpactAllInOne icAgent = null;
	public static void setICAgent(DadsImpactAllInOne _agent) {icAgent = _agent;}	
	protected static Integer preMethod = null;	
	protected static int g_eventCnt = 0;	
	/* a flag ensuring the initialization and termination are both executed exactly once and they are paired*/
	protected static boolean bInitialized = false;
	private static boolean active = false;	
	private static boolean start = false;	
	/* buffering working events */
	protected static List<Integer> B_Working = new LinkedList<Integer>();
	/* buffering queue events */
	protected static List<Integer> B_Queueing = new LinkedList<Integer>();			
	// a flag ensuring timeout of static graph create 
	static boolean isStaticCreateTimeOut = false;
	// timeOut time of static graph create 
	//static long staticCreateTimeOutTime=Long.MAX_VALUE/3;            // 	
	// a flag ensuring timeout of dynamic processEvents  
	static boolean isDynamicTimeOut = false;
	// timeOut time of dynamic processEvents
	static long dynamicTimeOutTime=Long.MAX_VALUE/3;            //
	// a flag ensuring timeout of static graph load 
	static boolean isStaticLoadTimeOut = false;
	// timeOut time of static graph load 
	static long staticLoadTimeOutTime=Long.MAX_VALUE/3;            // 	
//	// a flag ensuring static statement coverage  
 	static boolean staticStatementCoverage = false;	
	// a flag ensuring dynamic statement coverage  
	static String timeFileName= "Times"+getProcessIDString()+".txt";
	static String configurationFileName= "Configuration"+getProcessIDString()+".txt";
	static String mazeFileName= "Maze"+getProcessIDString()+".txt";
	static String allQueryResultFileName= "allQueryResult"+getProcessIDString()+".txt";
//	static String staticTimeFileName= "staticTimes"+getProcessIDString()+".txt";
//	static String staticConFigurationFileName= "staticConfiguration"+getProcessIDString()+".txt";//	
	static String staticConfigurations="";
//	static String dynamicConfigurations="";
	static String configurations="";
	static boolean staticUpdated=true;
//	static ArrayList<Long> dynamicTimes=new ArrayList<Long>();
//	static ArrayList<Long> staticTimes=new ArrayList<Long>();
////	static boolean staticFlowSensity = true;	
////	static boolean staticContextSensity = false;	
	static boolean[] staticDynamicSettings=new boolean[6];
//[0] staticContextSensity  [1] staticFlowSensity  	[2] dynamicMethodInstanceLevel [3] dynamicStatementCoverage [4] dynamicStaticGraph  [5]	dynamicMethodEvent	
	/* clean up internal data structures that should be done so for separate dumping of them, a typical such occasion is doing this per test case */
	
	static Qlearner learner  = new Qlearner();
	static long lastProcessTime=0;
	
	//private static final Logger logger=Logger.getLogger(DadsMonitor.class); 
	public synchronized static void resetInternals() {
		preMethod = null;
		g_eventCnt = 0;
		start = false;
		g_counter = 0;
		B_Working.clear();
		B_Queueing.clear();
	}
	
	/* initialize the two maps and the global counter upon the program start event */		
	public synchronized static void initialize() throws Exception{
		System.out.println("DadsMonitor initialization 181 ");
		final long startTime = System.currentTimeMillis();
		resetInternals();
		bInitialized = true;
		Thread queryServer1 = new MySocketServer(); 
		queryServer1.start();  
		
		//DadsUtil.initialTimesFile("dynamicTimes.txt", 16 , " ");
		DadsUtil.createOrCopyFile("Times.txt","0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 ",timeFileName);
		DadsUtil.createOrCopyFile("Configuration.txt","111111",configurationFileName);  //001000  110011  001001
		DadsUtil.createOrCopyFile("Maze.txt"," 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n 0 0 0 0 \n",mazeFileName);
		getTimeoutsThresholds(); 	//getTimeouts(); 	
//		configurations=DadsUtil.readLastLine(configurationFileName);
//		staticConfigurations=configurations.substring(0, 2);
//		dynamicConfigurations=configurations.substring(2, 6);
    	getConfigurations(configurationFileName);
		try {
    		if (staticDynamicSettings[2])     // staticGraph
    		{	
    			initialStaticGraph(staticLoadTimeOutTime);
    		}
    		else
    		{
    			initialFunctionList(staticLoadTimeOutTime);
    		}
   		//System.out.println("DadsImpactAllInOne operationFlag="+DadsImpactAllInOne.operationFlag);
			//System.out.println("initialize()2 dynamicStaticGraph="+dynamicStaticGraph+" dynamicMethodEvent="+dynamicMethodEvent); 
			icAgent = new DadsImpactAllInOne();
			DadsMonitor.setICAgent(icAgent);
	   	    //icAgent.resetImpOPs();

			
			learner.gamma=0.9;
			learner.alpha=0.9;
			learner.epsilon=0.2;
			System.out.println("DadsMonitor initialization takes " + (System.currentTimeMillis() - startTime) + " ms");
			//logger.info("DadsMonitor initialization took " + (System.currentTimeMillis() - startTime) + " ms");
    	} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized static void enter(String methodname){
//		if (0 == g_counter) {
//			//System.out.println("buffering events ......");
//		}
		//System.out.println("enter: "+methodname);
		if (active) return;
		active = true;
		//System.out.println("enter(String methodname): "+methodname);
		if (methodname.indexOf("chord.")>=0 || methodname.indexOf("thrift.")>=0 ) 
		{
			CN_LIMIT= 1000;			
			CN_LIMIT_QUEUE = 5*1000;
			TIME_SPAN = 900000;
		}
		else if (methodname.indexOf("zookeeper.")>=0 || methodname.indexOf("netty.")>=0) 
		{
			CN_LIMIT= 1000;			
			CN_LIMIT_QUEUE = 5*1000;
			TIME_SPAN = 900000;
		}		
		else if (methodname.indexOf("voldemort.")>=0) 
		{
			CN_LIMIT= 1000;			
			CN_LIMIT_QUEUE = 5*1000;
			TIME_SPAN = 900000;
		}		
		try {
			
			Integer smidx = DadsImpactAllInOne.getMethodIdx(methodname);
			if (smidx==null || !DadsImpactAllInOne.isMethodInSVTG(smidx)) {
				return;
			}
			
			B_Queueing.add(smidx*-1);
			g_counter ++;
			g_counter_queue ++;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			active = false;
		}
	}
	/* the callee could be either an actual method called or a trap */
	public synchronized static void returnInto(String methodname, String calleeName) throws IOException, InterruptedException, ExecutionException{
		//System.out.println("returnInto: "+methodname);
		if (active) return;
		active = true;
		//System.out.println("returnInto(String methodname, String calleeName): "+methodname+" "+calleeName);
		try {
			Integer smidx = DadsImpactAllInOne.getMethodIdx(methodname);
			//System.out.println("returnInto smidx, svtg: "+methodname+" "+smidx+" DadsImpactAllInOne.svtg.edgeSet().size()="+DadsImpactAllInOne.svtg.edgeSet().size()+" DadsImpactAllInOne.svtg.nodeSet().size()="+DadsImpactAllInOne.svtg.nodeSet().size());
			//if (smidx==null || !DadsImpactAllInOne.isMethodInSVTG(smidx)) {
			if (smidx==null) {	
				return;
			}			
			B_Queueing.add(smidx);
			g_counter ++;
			g_counter_queue++;
			long timeSpan=System.currentTimeMillis()-lastProcessTime;
			//System.out.println("Before g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" timeSpan "+timeSpan +" TIME_SPAN"+TIME_SPAN+" CN_LIMIT "+CN_LIMIT +" CN_LIMIT_QUEUE"+CN_LIMIT_QUEUE);
			boolean ifProcess=(timeSpan>TIME_SPAN) & ((g_counter > CN_LIMIT && g_counter_queue > CN_LIMIT && B_Queueing.size()>=CN_LIMIT) || g_counter_queue > CN_LIMIT_QUEUE);
			//boolean ifProcess=!(methodname.indexOf("voldemort.")>=0 || methodname.indexOf(".zookeeper.")>0 || methodname.indexOf(".netty.")>0) & (timeSpan>TIME_SPAN) & ((g_counter > CN_LIMIT && g_counter_queue > CN_LIMIT && B_Queueing.size()>=CN_LIMIT) || g_counter_queue > CN_LIMIT_QUEUE);
			//ifProcess= ifProcess || ((methodname.indexOf("voldemort.")>=0 || methodname.indexOf(".zookeeper.")>0 || methodname.indexOf(".netty.")>0) & timeSpan>TIME_SPAN &((g_counter > CN_LIMIT && g_counter_queue > CN_LIMIT && B_Queueing.size()>=CN_LIMIT) || g_counter_queue > CN_LIMIT_QUEUE));
			//System.out.println("Before g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" CN_LIMIT "+CN_LIMIT +" CN_LIMIT_QUEUE"+CN_LIMIT_QUEUE);
			if (ifProcess) {
				//System.out.println("After g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" B_Working.size()"+B_Working.size()+" B_Queueing.size()"+B_Queueing.size()+" timeSpan="+timeSpan+" lastProcessTime="+lastProcessTime);
				//logger.info("DadsMonitor After g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" B_Working.size()"+B_Working.size()+" B_Queueing.size()"+B_Queueing.size());
				System.out.println("DadsMonitor timeSpan="+timeSpan+" TIME_SPAN"+TIME_SPAN+" CN_LIMIT "+CN_LIMIT +" CN_LIMIT_QUEUE"+CN_LIMIT_QUEUE);
				B_Working.clear();
				g_counter=0;
				//B_Working.addAll(B_Queueing);				
				//B_Queueing.clear();
				for (int i=0; i<CN_LIMIT; i++)
				{
					if (B_Queueing.size()>0)
					{	
						B_Working.add(B_Queueing.get(0));
						g_counter++;
						B_Queueing.remove(0);
						g_counter_queue--;
					}
				}
				processEvents((dynamicTimeOutTime+staticLoadTimeOutTime));
				lastProcessTime=System.currentTimeMillis();
				timeSpan=0;
				
			}
			if (g_counter_queue > CN_LIMIT_QUEUE) {
				B_Queueing.clear();
				g_counter_queue=0;
				B_Working.clear();
				g_counter=0;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			active = false;
		}		
		//System.out.println("returnInto methodname: " + methodname+" calleeName: " + calleeName);
        
	}
	
	public synchronized static void terminate(String where) throws Exception {
		if (bInitialized) {
			bInitialized = false;
		}
		else {
			return;
		}
	}
    
	public synchronized static String getImpactSetStr(String query) throws Exception {
		String resultStr="";
		processEvents((dynamicTimeOutTime+staticLoadTimeOutTime));
    	 DadsImpactAllInOne.setQuery(query);
    	 resultStr=icAgent.getDumpImpactSet(query);
    	 //System.out.println("impactSetStr=" + resultStr);
		return resultStr;
	}
	public static void getTimeouts(String configurationFile) throws Exception {
		long long1=DadsUtil.readTimeOutFromFile("StaticGraphLoad ", configurationFile);
		if (long1>0)
			staticLoadTimeOutTime=long1;
		//System.out.println("long1=" + long1);
		long long2=DadsUtil.readTimeOutFromFile("Dynamic ", configurationFile);
		if (long2>0)
			dynamicTimeOutTime=long2;
		//System.out.println("long2=" + long2);
		//System.out.println("staticLoadTimeOutTime=" + staticLoadTimeOutTime +" timeOutTimeDynamic=" + dynamicTimeOutTime+" staticLoadTimeOutTime=" + staticLoadTimeOutTime );
	}
	public static void getThresholds(String configurationFile) throws Exception {
		long long1=DadsUtil.readTimeOutFromFile("EventLimit ", configurationFile);
		if (long1>0)  {
			CN_LIMIT= long1;		
			CN_LIMIT_QUEUE = 5*long1;
		}	
		//System.out.println("long1=" + long1);
		long long2=DadsUtil.readTimeOutFromFile("TimeSpan ", configurationFile);
		if (long2>0)
			TIME_SPAN=long2;
		System.out.println("CN_LIMIT=" + CN_LIMIT + " CN_LIMIT_QUEUE=" + CN_LIMIT_QUEUE+ " TIME_SPAN=" + TIME_SPAN);
	}

	public static void getTimeoutsThresholds() throws Exception {
		getThresholds("timeoutsthresholds.txt"); 
		String budgetStr=DadsUtil.readToString("budget.txt").trim().replaceAll("[^\\d]", "");
		if (budgetStr.length()<1) 
			getTimeouts("timeoutsthresholds.txt"); 	
		//System.out.println("getTimeouts() budgetStr=" + budgetStr);
		try
		{			
			long budget=Long.parseLong(budgetStr);											 
	    	long timeStaticLoad=(long)(budget*0.5);
	    	long timeDynamic=(long)(budget*0.5);
	    	//System.out.println("getTimeouts() timeStaticCreate=" + timeStaticCreate +" timeStaticLoad=" + timeStaticLoad+" timeDynamic=" + timeDynamic );
	    	if (budget<10 || timeStaticLoad<5 || timeDynamic<5) {
	    		getTimeouts("timeoutsthresholds.txt");
	    	}
	    	else  {
	    		staticLoadTimeOutTime=timeStaticLoad;
	    		dynamicTimeOutTime=timeDynamic;
	    	}	
	    } catch (Exception e) {  
	    	getTimeouts("timeoutsthresholds.txt");	
        }  

    	System.out.println("getTimeouts() staticLoadTimeOutTime=" + staticLoadTimeOutTime +" timeOutTimeDynamic=" + dynamicTimeOutTime);
    	//logger.info("DadsMonitor getTimeouts() staticLoadTimeOutTime=" + staticLoadTimeOutTime +" timeOutTimeDynamic=" + dynamicTimeOutTime+" staticCreateTimeOutTime=" + staticCreateTimeOutTime);
	}	
	public static void getConfigurations(String configurationFile) throws Exception {
		configurations=DadsUtil.readLastLine(configurationFile);
		staticConfigurations=configurations.substring(0, 2);
		//logger.info("DadsMonitor configurations="+configurations);
		for (int i=0; i<staticDynamicSettings.length; i++)
		{
			staticDynamicSettings[i]=true;
		}	
		String configurationFlag="";
		int flagSize=staticDynamicSettings.length;
		if	(configurations.length()<flagSize)
			flagSize=configurations.length();
		for (int i=0; i<configurations.length(); i++)
		{
			configurationFlag=configurations.substring(i,i+1);
			if (configurationFlag.equals("0") || configurationFlag.equals("f") ||configurationFlag.equals("F")) {
    			staticDynamicSettings[i]=false;
    		}
    		else {
    			staticDynamicSettings[i]=true;
    		}
			////logger.info("DadsMonitor staticDynamicSettings["+i+"]="+staticDynamicSettings[i]);
		}
	
	}
	
	static class MyInitialStaticGraph implements Callable <Boolean>  {
		@Override
        public Boolean call() throws Exception {
			try {
                Thread.sleep(1);      
				long startTime = System.currentTimeMillis();
//	    		File file1=new File("staticVtg.dat");
//	    		if (staticUpdated || !file1.exists()) {  
//	               	Runtime.getRuntime().exec("./DadsStaticGraph.sh>DadsStaticGraph.log");
//	    			//System.out.println("Creating static graph successes"); 
//	        		System.out.println("Creating static graph took "+(System.currentTimeMillis() - startTime)+" ms");
//	    			//logger.info("DadsMonitor Creating static graph took "+(System.currentTimeMillis() - startTime)+" ms");
//	        		staticUpdated=false;
//	            } 
				if (staticUpdated)  {
					DadsUtil.CopyStaticGraph(staticConfigurations);
				}		
	    		String mainClass=DadsUtil.findMainClass();
				if (DadsImpactAllInOne.initializeClassGraph(mainClass,staticStatementCoverage) !=0)  {
					//System.out.println("MyInitialStaticGraph DadsImpactAllInOne.svtg.edgeSet().size()="+DadsImpactAllInOne.svtg.edgeSet().size()+" DadsImpactAllInOne.svtg.nodeSet().size()="+DadsImpactAllInOne.svtg.nodeSet());
					System.out.println("Unable to load satic graph");
	    			//logger.info("DadsMonitor Unable to load satic graph");
					return false;
				}
				else
					System.out.println("Loading static graph successes");      	
           	//return "" + (System.currentTimeMillis() - startTime);
			  } catch (InterruptedException e) {
				  System.out.println("MyInitialStaticGraph is interrupted when calculating, will stop...");
	    			//logger.info("DadsMonitor MyInitialStaticGraph is interrupted when calculating, will stop...");
				  return false; // 
			  }
			return true;
		}		
	}	
	static class MyInitialFunctionList implements Callable <Boolean>  {
		@Override
        public Boolean call() throws Exception {
			try {
                Thread.sleep(1);      
                if (DadsImpactAllInOne.initializeFunctionList(staticStatementCoverage) !=0)  {

    				//System.out.println("MyInitialStaticGraph DadsImpactAllInOne.svtg.edgeSet().size()="+DadsImpactAllInOne.svtg.edgeSet().size()+" DadsImpactAllInOne.svtg.nodeSet().size()="+DadsImpactAllInOne.svtg.nodeSet());
    				System.out.println("Unable to initialize function list");
	    			//logger.info("DadsMonitor Unable to initialize function list");   				
    				return false;
    			}
    			else
    			{	
    				System.out.println("Initialization function list successes");
	    			//logger.info("DadsMonitor Initialization function list successes");    		
    				//System.out.println("initialFunctionList4 dynamicStaticGraph="+dynamicStaticGraph+" dynamicMethodEvent="+dynamicMethodEvent); 
    			}
           	//return "" + (System.currentTimeMillis() - startTime);
			  } catch (InterruptedException e) {
				  System.out.println("MyInitialStaticGraph is interrupted when calculating, will stop...");
	    			//logger.info("DadsMonitor MyInitialStaticGraph is interrupted when calculating, will stop...");    		
				  return false; // 
			  }
			return true;
		}			
	}	
    public synchronized static void initialStaticGraph(long timeOutTime) throws  Exception{
    	MyInitialStaticGraph task0 = new DadsMonitor.MyInitialStaticGraph();
    	ExecutorService executor = Executors.newCachedThreadPool();
        Future<Boolean> future1=executor.submit(task0);

        try {
            if (future1.get(timeOutTime, TimeUnit.MILLISECONDS)) { // 
                System.out.println("initialStaticGraph completes successfully");
    			//logger.info("DadsMonitor initialStaticGraph completes successfully");   		
            }
        } catch (InterruptedException e) {
            System.out.println("initialStaticGraph was interrupted during the sleeping");
			//logger.info("DadsMonitor initialStaticGraph was interrupted during the sleeping"); 
            executor.shutdownNow();
        } catch (ExecutionException e) {
            System.out.println("initialStaticGraph has mistakes during getting the result");
			//logger.info("DadsMonitor initialStaticGraph has mistakes during getting the result");
            executor.shutdownNow();
        } catch (TimeoutException e) {
            System.out.println("initialStaticGraph is timeoouts");
			//logger.info("DadsMonitor initialStaticGraph is timeoouts"); 
            future1.cancel(true);
            // executor.shutdownNow();
            // executor.shutdown();
        } finally {
            executor.shutdownNow();
        }        
    }  
    public synchronized static void initialFunctionList(long timeOutTime) throws  Exception{
    	MyInitialFunctionList task0 = new DadsMonitor.MyInitialFunctionList();
        ExecutorService executor= Executors.newSingleThreadExecutor();
        Future<Boolean> future1=executor.submit(task0);

        try {
            if (future1.get(timeOutTime, TimeUnit.MILLISECONDS)) { // 
                System.out.println("initialFunctionList completes successfully");
                //logger.info("DadsMonitor initialFunctionList completes successfully"); 
            }
        } catch (InterruptedException e) {
            System.out.println("initialFunctionList was interrupted during the sleeping");
            //logger.info("DadsMonitor initialFunctionList was interrupted during the sleeping"); 
            executor.shutdownNow();
        } catch (ExecutionException e) {
            System.out.println("initialFunctionList has mistakes during getting the result");
            //logger.info("DadsMonitor initialFunctionList has mistakes during getting the result"); 
            executor.shutdownNow();
        } catch (TimeoutException e) {
            System.out.println("initialFunctionList is timeoouts");
            //logger.info("DadsMonitor initialFunctionList is timeoouts"); 
            future1.cancel(true);
            // executor.shutdownNow();
            // executor.shutdown();
        } finally {
            executor.shutdownNow();
        }
        //System.out.println("InitialFunctionList timeOutTime="+timeOutTime+" timeOutProcessEvents="+timeOutDynamic); 
    }	
	static void MyProcessEvents()  {
       	long startTime = System.currentTimeMillis();
    	System.out.println("start processing events in the buffer of " + B_Working.size() + " events...... ");
        //logger.info("DadsMonitor start processing events in the buffer of " + B_Working.size() + " events...... ");
    	//System.out.println("g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" B_Working.size()"+B_Working.size()+" B_Queueing.size()"+B_Queueing.size());
		try {
			getTimeoutsThresholds(); 	 	
			getConfigurations(configurationFileName);
        	//System.out.println("g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" B_Working.size()"+B_Working.size()+" B_Queueing.size()"+B_Queueing.size());
 	//[0] staticContextSensity  [1] staticFlowSensity  	[2] dynamicMethodInstanceLevel [3] dynamicStatementCoverage [4] staticGraph  [5]	dynamicMethodEvent	
            //staticDynamicSettings[2] staticGrap only for static graph existing
        	//[0]staticContextSensity  [1]staticFlowSensity  	[2]staticGraph     [3]dynamicMethodEvent	 [4]dynamicStatementCoverage  [5]dynamicMethodInstanceLevel	        	
        	//  Adaptation  
        	File file = new File("static.dat");  
            if (!file.exists()) {  
            	staticDynamicSettings[2]=false;
            }  
            
            System.out.println("staticDynamicSettings[5]="+staticDynamicSettings[5]+" before FirstLast B_Working.size()=" + B_Working.size());
        	if (staticDynamicSettings[5])                          //dynamicMethodInstanceLevel
    		{
    			B_Working=DadsUtil.returnFirstLastEvents(B_Working);
            	System.out.println("After FirstLast B_Working.size()=" + B_Working.size());
    		}
        	System.out.println("After B_Working.size()=" + B_Working.size());
        	if (staticDynamicSettings[4])                         //dynamicStatementCoverage
    			DadsImpactAllInOne.prunedByStmt("");
    		System.out.println("DadsMonitor MyProcessEvents after staticDynamicSettings[3]");
        	//logger.info("DadsMonitor MyProcessEvents after staticDynamicSettings[3]");
        	if (staticDynamicSettings[3])                         //dynamicMethodEvent
    		{	
            	for (int i=0; i<B_Working.size(); i++) {
            		Integer _idx=B_Working.get(i);                		
        			//System.out.println("i=" + i+" _idx=" + _idx);
        			//System.out.print(".");
        			Integer smidx = Math.abs(_idx);        			
        			if (null != preMethod && preMethod == smidx) {
        				continue;
        			}       			        			
        			if (!start) {
        				start = (DadsImpactAllInOne.getAllQueries()==null || DadsImpactAllInOne.getAllQueries().contains(smidx));
        				if (!start) {
        					continue;
        				}
        			}  
        			if (staticDynamicSettings[2])               //StaticGraph
        			{	// enter event
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
        			}
        			else
        			{        				
	        			if (_idx < 0) {
        				// trivially each method, once executed, is treated as impacted by itself
	        				if (!icAgent.getAllImpactSets().containsKey(smidx))
        					icAgent.add2ImpactSet(smidx, smidx);
        				}
        				for (int j=i+1; j<B_Working.size(); j++) {	  
        					Integer valueJ=B_Working.get(j);      	          		
                			//System.out.println("j=" + j+" valueJ=" + valueJ);
        					if (valueJ<0)
        					{
        						icAgent.add2ImpactSet(smidx, Math.abs(valueJ));
        					}
        					else
        						icAgent.add2ImpactSet(smidx, valueJ);   
        				}   // for
        				
        			}  //(dynamicStaticGraph)
        			preMethod = smidx;
        		}  //for 
    		}
        	else
        	{ 		  
        		if (staticDynamicSettings[2])                       //dynamicStaticGraph
    			{	
	            	for (int i=0; i<DadsImpactAllInOne.idx2method.size(); i++) {
	            		Integer _idx=i;  
	        			Integer smidx = Math.abs(_idx);        			
	        			if (null != preMethod && preMethod == smidx) {
	        				continue;
	        			}       			        			
	        			if (!start) {
	        				start = (DadsImpactAllInOne.getAllQueries()==null || DadsImpactAllInOne.getAllQueries().contains(smidx));
	        				if (!start) {
	        					continue;
	        				}
	        			} 
	    				icAgent.onMethodEntryEvent(smidx);			
	    				icAgent.onMethodReturnedIntoEvent(smidx);    				
	        			if (null != preMethod && preMethod != smidx) {
	        				// close some "open" source nodes
	        				icAgent.closeNodes(preMethod, smidx);
	        			}  // (null != preMethod && preMethod != smidx)
	        			preMethod = smidx;
	        		}   //for 
    			}   //if (dynamicStaticGraph)
        	}	
    		System.out.println("DadsMonitor MyProcessEvents after the computation");
        	//logger.info("DadsMonitor MyProcessEvents after the computation");
        	String allQuestResults="";
    		if (staticDynamicSettings[2])  {    		        //dynamicStaticGraph
    			//icAgent.dumpAllImpactSets();
    			System.out.println("DadsMonitor "+icAgent.getDumpAllImpactSetsSize());  
    			//logger.info("DadsMonitor "+icAgent.getDumpAllImpactSetsSize());    
    			allQuestResults=icAgent.getDumpAllImpactSets();
    		}
    		else  {
    			//icAgent.dumpAllImpactSetsWithoutStatic();
    			System.out.println("DadsMonitor "+icAgent.getDumpAllImpactSetsSizeWithoutStatic());      			
    			//logger.info("DadsMonitor "+icAgent.getDumpAllImpactSetsSizeWithoutStatic());        			  
    			allQuestResults=icAgent.getDumpAllImpactSetsWithoutStatic();
    		}
    		DadsUtil.writeStringToFile(allQuestResults, allQueryResultFileName);
    		System.out.println();
    		g_counter = 0;
    		B_Working.clear();
		  } catch (Exception e) {
			  System.out.println("MyProcessEvents is interrupted when calculating, will stop...");
    			//logger.info("DadsMonitor MyProcessEvents is interrupted when calculating, will stop...");    		
			  
		  }	    		
      		
	}

    public synchronized static long processEvents(long timeOutTime) throws  Exception{
    	final long startTime = System.currentTimeMillis();
  
		//configurations=DadsUtil.readLastLine(conFigurationFileName);
//		staticConfigurations=configurations.substring(0, 2);
//		dynamicConfigurations=configurations.substring(2, 6);
    	//getConfigurations(conFigurationFileName);
    	if	(staticUpdated)	
    	    {	
			if (staticDynamicSettings[2])
			{	
				initialStaticGraph(staticLoadTimeOutTime);
			}
			else
			{
				initialFunctionList(staticLoadTimeOutTime);
			}
	    }
    	//logger.info("DadsMonitor ProcessEvents after staticUpdated");
    	
//    	MyProcessEvents task1 = new DadsMonitor.MyProcessEvents();
//        ExecutorService executorService= Executors.newSingleThreadExecutor();
//        Future<String> future1=executorService.submit(task1);
//        long resultL=(long) 0;
//        try{
//            String result=future1.get(timeOutTime,TimeUnit.MILLISECONDS);
//            System.out.println("ProcessEvents took "+result+" ms");
//            isDynamicTimeOut = false;            
//            //dynamicStatementCoverage = false;  
//            resultL=Long.parseLong(result.replaceAll("[^0-9]",""));
//        }
//        catch (TimeoutException e){
//            System.out.println("Timeout!"); 
//            isDynamicTimeOut = true;            
//            //dynamicStatementCoverage = true;
//        }
//        finally  {
//        	future1.cancel(true);
//        }       
    	MyProcessEvents();
//        MyProcessEvents task0 = new DadsMonitor.MyProcessEvents();
//    	logger.info("DadsMonitor ProcessEvents after staticUpdated");
//    	ExecutorService executor = Executors.newCachedThreadPool();
//    	logger.info("DadsMonitor ProcessEvents after staticUpdated");
//        Future<Boolean> future1=executor.submit(task0);
//    	logger.info("DadsMonitor ProcessEvents after staticUpdated");
//
//        try {
//            if (future1.get(timeOutTime, TimeUnit.MILLISECONDS)) {  
//                System.out.println("ProcessEvents completes successfully");
//    			logger.info("DadsMonitor ProcessEvents completes successfully");   		
//            }
//        } catch (InterruptedException e) {
//            System.out.println("ProcessEvents was interrupted during the sleeping");
//			logger.info("DadsMonitor ProcessEvents was interrupted during the sleeping"); 
//            executor.shutdownNow();
//        } catch (ExecutionException e) {
//            System.out.println("ProcessEvents has mistakes during getting the result");
//			logger.info("DadsMonitor ProcessEvents has mistakes during getting the result");
//            executor.shutdownNow();
//        } catch (TimeoutException e) {
//            System.out.println("ProcessEvents is timeoouts");
//			logger.info("DadsMonitor ProcessEvents is timeoouts"); 
//            future1.cancel(true);
//             executor.shutdownNow();
//             executor.shutdown();
//        } finally {
//            executor.shutdownNow();
//        }   
        //System.out.println("ProcessEvents timeOutTime="+timeOutTime+" timeOutProcessEvents="+timeOutDynamic); 
        //System.out.println("ProcessEvents g_counter="+g_counter+" g_counter_queue="+g_counter_queue+" B_Working.size()"+B_Working.size()+" B_Queueing.size()"+B_Queueing.size());
        long resultL=System.currentTimeMillis()-startTime; 
        System.out.println("Event computation took " + resultL + " ms");
        
		//logger.info("DadsMonitor Event computation took " + resultL + " ms");
     
        //dynamicTimes=DadsUtil.updatedTimeFromConfigurationForced(dynamicConFigurationFileName, dynamicTimeFileName, resultL);
        DadsUtil.updateTimeFromConfigurationToFileForced(configurationFileName, timeFileName, resultL);        
        //System.out.println("DadsUtil.updateTimeFromConfigurationToFileForced(configurationFileName, timeFileName, resultL);");      
		//logger.info("DadsMonitor updateTimeFromConfigurationToFileForced configurationFileName="+configurationFileName+" timeFileName="+timeFileName+" resultL"+resultL);
        DadsUtil.writeStringToFileAppend(configurations+" Event computation time: "+ resultL + " ms\n", "TimeCosts.txt");
        
        
        DadsUtil.saveRewardPenaltyfromFiles(mazeFileName, timeFileName, staticLoadTimeOutTime, dynamicTimeOutTime, staticDynamicSettings[2], staticUpdated);
        //System.out.println("DadsUtil.saveRewardPenaltyfromFiles(");
        //logger.info("DadsUtil.saveRewardPenaltyfromFiles(");
        //dynamicConfigurations=DadsController.updatedConfigurationFromTimesFile(dynamicConFigurationFileName, dynamicTimeFileName, dynamicTimeOutTime,4);
        String oldStaticConfigurations=DadsUtil.readToString(configurationFileName).substring(0,2);
        //System.out.println("oldStaticConfigurations: " + oldStaticConfigurations);
        //logger.info("DadsMonitor oldStaticConfigurations: " + oldStaticConfigurations);
        //DadsController.updateConfigurationFromTimesFile(configurationFileName, timeFileName, dynamicTimeOutTime,6);
        
        // With or without adaptation
        DadsController.setNextConfigurationInFile(learner, mazeFileName, configurationFileName);        
        //System.out.println("DadsController.setNextConfigurationInFile(learner, mazeFileName, configurationFileName);");
        //logger.info("DadsController.setNextConfigurationInFile(learner, mazeFileName, configurationFileName);");
        
        configurations=DadsUtil.readLastLine(configurationFileName);		
        staticConfigurations=configurations.substring(0, 2);
		//dynamicConfigurations=configurations.substring(2, 6);
		getConfigurations(configurationFileName);
        System.out.println("oldStaticConfigurations: " + oldStaticConfigurations + " newStaticConfigurations: " + staticConfigurations);
        //logger.info("DadsMonitor oldStaticConfigurations: " + oldStaticConfigurations + " newStaticConfigurations: " + staticConfigurations);
        if (oldStaticConfigurations.equals(staticConfigurations))  {        	
        	staticUpdated=false;
        }
        else
        {
        	staticUpdated=true;
        	//DadsUtil.writeStringToFile(staticConfigurations, "staticConfiguration.txt");        	//[0]staticContextSensity  [1]staticFlowSensity  	[2] dynamicMethodInstanceLevel [3] dynamicStatementCoverage [4] dynamicStaticGraph  [5]	dynamicMethodEvent       	
        	if (staticDynamicSettings[4])  {
//        		final long createStartTime = System.currentTimeMillis();
//        		createStaticGraph(staticCreateTimeOutTime);
//        		System.out.println("Creating Static Graph took "+ (System.currentTimeMillis() - createStartTime)+" ms");
                //logger.info("DadsMonitor Creating Static Graph took "+ (System.currentTimeMillis() - createStartTime)+" ms");
        	}	
        }
        
        //DadsUtil.saveRewardPenaltyfromFiles(mazeFileName, timeFileName, staticLoadTimeOutTime, dynamicTimeOutTime, staticDynamicSettings[4]);
        //System.out.println("DadsUtil.saveRewardPenaltyfromFiles(mazeFileName, timeFileName, staticLoadTimeOutTime, dynamicTimeOutTime, staticDynamicSettings[4]);");
     
        return (System.currentTimeMillis() - startTime);
    }

//	static class MyCreateStaticGraph implements Callable <Boolean> {
//		@Override
//        public Boolean call() throws Exception {
//			try {
//                Thread.sleep(1000);          
//	           	//long startTime = System.currentTimeMillis();
//	           	Runtime.getRuntime().exec("./DadsStaticGraph.sh");
//				System.out.println("Creating static graph successes");         
//                //logger.info("DadsMonitor Creating static graph successes");  	
//           	//return "" + (System.currentTimeMillis() - startTime);
//			  } catch (InterruptedException e) {
//				  return false; // 
//			  }
//			return true;
//		}
//	}	
//	
//    public synchronized static void createStaticGraph(long timeOutTime) throws  Exception{
//
//        ExecutorService executor = Executors.newCachedThreadPool();
//        MyCreateStaticGraph task1 = new DadsMonitor.MyCreateStaticGraph();
//        Future<Boolean> f1 = executor.submit(task1);
//        try {
//            if (f1.get(staticCreateTimeOutTime, TimeUnit.MILLISECONDS)) { // 
//                System.out.println("createStaticGraph complete successfully");        
//                //logger.info("DadsMonitor Creating static graph successes");  
//            }
//        } catch (InterruptedException e) {
//            System.out.println("createStaticGraph was interrupted during the sleeping");        
//            //logger.info("DadsMonitor Creating static graph successes");  
//            executor.shutdownNow();
//        } catch (ExecutionException e) {
//            System.out.println("createStaticGraph has mistakes during getting the result");        
//            //logger.info("DadsMonitor Creating static graph successes");  
//            executor.shutdownNow();
//        } catch (TimeoutException e) {
//            System.out.println("createStaticGraph is timeoouts");        
//            //logger.info("DadsMonitor Creating static graph successes");  
//            f1.cancel(true);
//            // executor.shutdownNow();
//            // executor.shutdown();
//        } finally {
//            executor.shutdownNow();
//        }
//        //System.out.println("InitialStaticGraph timeOutTime="+timeOutTime+" timeOutProcessEvents="+timeOutDynamic); 
//    }  
	public static String getProcessIDString() {
		return ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^a-zA-Z0-9]", "");
	}
	
}
