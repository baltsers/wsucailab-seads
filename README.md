# wsucailab-seads

Project artifact for:

**Seads: Scalable and Cost-Effective Dynamic Dependence Analysis of Distributed Systems via Reinforcement Learning**

- Original artifact URL: <https://bitbucket.org/wsucailab/seads>
- Imported via `pubs2github` from the publications page
- Downloader: `git` — Cloned https://bitbucket.org/wsucailab/seads.git (1034 files)


This repository was created automatically. The contents under this
directory mirror what was downloaded from the original artifact link
above; refer to that source for the authoritative version, licensing,
and any updates.

---

## Original `README.md` (from the upstream artifact)

# SEADS: Scalable and Cost-Effective Dynamic Dependence Analysis of Distributed Services using Reinforcement Learning
-----------
Seads is a distributed, online, and cost-effective dynamic dependence analysis framework that aims at scaling to 
real-world distributed services. The analysis itself is distributed to exploit distributed computing resources 
(e.g., a cluster) of the service under analysis; it is online to overcome the problem with unbounded execution 
traces while running continuously with the service being analyzed to provide timely querying of analysis results
 (i.e., run-time dependence set of any given query). Most importantly, given a user-specified time budget, the 
analysis automatically adjusts itself to better cost-effectiveness tradeoffs (than otherwise) while respecting 
the budget by changing various analysis configurations according to the time being spent by the dependence analysis. 
At the core of the automatic adjustment is our application of a reinforcement learning method for the decision 
making—deciding which configurations to adjust to according to the current configuration and its associated analysis 
cost with respect to the user budget. We have implemented Seads for Java and applied it to eight real-world 
distributed systems with continuous executions.		

-----------
### Install SEADS:
-----------
- Download SEADS zip file from https://bitbucket.org/wsucailab/seads/src/master/	
- Unzip the file.
- Copy all library files from the directory ”tool” of SEADS.

-----------
### Download and install subjects:
-----------
- MultiChat https://code.google.com/p/multithread-chat-server/
- NIOEcho   http://rox-xmlrpc.sourceforge.net/niotut/index.html#The code
- OpenChord https://sourceforge.net/projects/open-chord/files/Open%20Chord%201.0/
- Thrift	  http://archive.apache.org/dist/thrift/
- xSocket	  https://mvnrepository.com/artifact/org.xsocket/xSocket
- ZooKeeper https://github.com/apache/zookeeper/releases
- Netty	  https://bintray.com/netty/downloads/netty/

-----------
### Analysis
-----------
#### 1. Select one subject.
#### 2. Use SEADS to compute taint flow paths. 
     2.1  Phase 1: Instrumentation                 Execute program/shell/#subject#/SEADSInstr.sh
	 2.2  Phase 2: Arbitration & Adjustment        Execute instrumented programs	
	 2.3  Phase 3: User Interaction				   Execute program/shell/#subject#/SEADSQueryClient.sh

-----------
### Execution operations of integration tests of instrumented programs
-----------
- MultiChat: We started one server and two clients. Then, two clients sent and exchanged random text messages through the server.

- NioEcho: We started one server and one client. The client sent random text messages to the server and waited for the echoing of each message from the server.

- OpenChord: We first created an overlay network on the first node; next, we joined the network on other two nodes, inserted a new data entry to the network on the third node, looked up and deleted the data entry on the first node; lastly, we listed all data entries on the second node. 

- Voldemort: The client operations were adding a key-value pair, querying the value of the key, deleting the key, and retrieving the pair again. 

- ZooKeeper: We first created two nodes, looked up for both, checked their attributes, changed the data association between them, and then deleted both nodes. 

- Thrift: We used its libraries to develop a calculator consisting of a server and a client component. (The Thrift file must be transferred to Java programs first.) We performed against the calculator (from its client) basic arithmetics (addition, subtraction, multiplication, and division). 

- xSocket: We firstly started one server instance and two client instances. Next, we sent one text message from one client, and a different message from the other client, to the server. 

- Netty: We started one server and one client instance, and then sent one message from the client to the server.

