# DADS: Dynamic Slicing Continuously-Running Distributed Programs with Budget Constraints
-----------
Dynamic slicers help developers improve software security, reliability, quality, and performance. Yet traditional dynamic slicers have applicability, scalability, and cost-effectiveness challenges for common continuous running distributed systems. DADS is a distributed, online, scalable, and cost-effective dynamic slicer for distributed systems. It is distributed to exploit distributed and parallel computing resources. It is online to avoid expensive execution tracing (space and I/O costs) costs to overcome the applicability challenge. It automatically adjusts itself to resolve scalability and cost-effectiveness challenges. Our evaluation of DADS on eight real-world distributed programs revealed its promising efficiency, scalability, cost-effectiveness, and superiority to the online version of a state-of-the-art dynamic slicer.  

This is the tool demo package for Seads. 
A virtual machine can be downloaded as the DADSVM.rar file from https://drive.google.com/drive/folders/1SD0G65ZKC_HtZZsqZG_DVwBSw7oV81AM?usp=sharing
and the demo video can be viewed at https://youtu.be/pRR-us9puSw online.			
										
-----------
### Install DADS
-----------
      
- Download DADS zip file.

- Unzip the file.

- Copy all library files from the directory ”tool” of DADS to a directory (e.g., "lib") defined by the user.

-----------
### Download and install subjects
-----------
- MultiChat https://code.google.com/p/multithread-chat-server/

- NIOEcho   http://rox-xmlrpc.sourceforge.net/niotut/index.html#The code

- OpenChord https://sourceforge.net/projects/open-chord/files/Open%20Chord%201.0/

- Thrift	  http://archive.apache.org/dist/thrift/

- xSocket	  https://mvnrepository.com/artifact/org.xsocket/xSocket

- ZooKeeper https://github.com/apache/zookeeper/releases

- Netty	  https://bintray.com/netty/downloads/netty/


-----------
### Compute slices
-----------
#### 1. Select one subject.

Apache Thrift is an application development framework with a code generation engine for developing scalable cross-language services. 
We used the framework to develop a calculator consisting of a server and a client. 
And then we use Thrift as our working example to demonstrate our tool.
			 
#### 2. Use DADS to compute slices.
      
- 2.1	 Step 1 (Phase 1): Instrumentation:

  We execute program/shell/subject/DADSInstr.sh   
		 
- 2.2  Step 2 (Phase 2): Arbitration & Adjustment:	  
						
  First, we set milliseconds (e.g., 4,000) for a user budget constraint in the file "budget.txt".
			
  Second, we separately execute "./serverDADS.sh" and "./clientDADS.sh" to start a server and a client of the instrumented program. The client automatically sends some numbers and basic arithmetic operations (i.e., addition, subtraction, multiplication, and division) to the server and gets the calculation results from the server.
			
  Finally, analysis configurations change according to a Q-learning strategy.
		 
- 2.3  Step 3 (Phase 3): User Interaction:  

  First, we execute program/shell/subject/DADSQueryClient.sh to start a querying client.

  Then, we input a slicing query (i.e., method name) such as <org.apache.thrift.transport.TSocket: void open()>

  Eventually, we get corresponding dependencies as the slice.
	