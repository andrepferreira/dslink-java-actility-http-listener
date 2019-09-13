package org.dsa.iot.template;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects; 
import org.dsa.iot.dslink.util.handler.Handler; 
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

import org.json.JSONTokener;
import org.json.JSONObject;

/**
 * The main class that starts the DSLink. Typically it extends
 * {@link DSLinkHandler} and the main method extends into it.
 */
public class Main extends DSLinkHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static volatile int activeSocket;

	private Socket client = null;
	
	@Override
	public boolean isResponder() {
		return true;
	}

	@Override
	public void onResponderInitialized(DSLink link) {
		
		Node superRoot = link.getNodeManager().getSuperRoot();

		NodeBuilder builder3 = superRoot.createChild("JSONPoint");
		builder3.setDisplayName("JSON Http Mirror");
		builder3.setValueType(ValueType.DYNAMIC);
		final Node node3 = builder3.build();

		NodeBuilder builderSocket = superRoot.createChild("socket");
		builderSocket.setDisplayName("Listening on socket");
		builderSocket.setValueType(ValueType.DYNAMIC);
		final Node socketNode = builderSocket.build();
		
		
		final Runnable listeningTask = new Runnable() {
			@Override
			public void run() {

				try 
				{
				
					// Get the port to listen on
					int port = activeSocket;
					// Create a ServerSocket to listen on that port.					
					ServerSocket ss = new ServerSocket(port);
					// Now enter an infinite loop, waiting for & handling connections.
					for(;;)					
					{
						
						
						// Wait for a client to connect. The method will block;
						// when it returns the socket will be connected to the client
						client = ss.accept();						
						
						/*
						 * BELOW
						 * IS
						 * THE
						 * KEY
						 * !!!
						 * FOR
						 * CLOSING
						 * OLD
						 * SOCKETS
						 * !!!
						 */
						if(client.getLocalPort() != activeSocket)
						{
							client.close();
							ss.close();
							break;
						}

																		

						// Get input and output streams to talk to the client						
						BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(),"UTF8"));
						PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF8"), true);
					
						
						String line;
			            // looks for post data
			            int postDataI = -1;
			            while ((line = in.readLine()) != null && (line.length() != 0)) {
			                if (line.toUpperCase().indexOf("CONTENT-LENGTH:") > -1) {
			                    postDataI = Integer.parseInt(
			                            line.toUpperCase().substring(
			                                    line.indexOf("CONTENT-LENGTH:") + 16,
			                                    line.length()).trim());
			                }
			            }
			            
			            //content length header could not be found
			            if(postDataI == -1)
			            {
							out.print("HTTP/1.1 400 Bad Request\r\n"); // Version & status code
							out.print("Content-Type: text/plain\r\n"); // The type of data
							out.print("Connection: close\r\n"); // Will close stream
							out.print("\r\n");
							out.print("No content-length header could be found");
							out.print("\r\n"); // End of headers
							// Close socket, breaking the connection to the client, and
							// closing the input and output streams
				            out.close();
							in.close();
							client.close(); // Close the socket itself
			            }
			            else
			            {
			            	
						
				            String postData = "";
				            // read the post data
				            if (postDataI > 0) {
				                char[] charArray = new char[postDataI];
				                in.read(charArray, 0, postDataI);
				                postData = new String(charArray);
				            }			            					         								
							
				            String result = postData;
							
							LOGGER.debug(result);

							LOGGER.info("info: Message received");


							int i = -1;
							i = result.indexOf("{");
							if(i > -1) 
							{
								result = result.substring(i);

								JSONTokener tokener = new JSONTokener(result);
								JSONObject token = new JSONObject(tokener);
								JsonObject  recvd = new JsonObject(token.toString());

								Value joval = new Value(recvd); 
								joval.setSerializable(false); 
								node3.setValue(joval); 
							}
			            	
							// Start sending our reply, using the HTTP 1.1 protocol
							out.print("HTTP/1.1 200 \r\n"); // Version & status code
							out.print("Content-Type: text/plain\r\n"); // The type of data
							out.print("Connection: close\r\n"); // Will close stream
							out.print("\r\n"); // End of headers
							
							// Close socket, breaking the connection to the client, and
							// closing the input and output streams
				            out.close();
							in.close();
							client.close(); // Close the socket itself
				            
			            }			            			            				

					} // Now loop again, waiting for the next connection

				}
				catch (RuntimeException e) {
				    throw e;
				}
				// If anything goes wrong, print an error message
				catch (Exception e) {
					
					LOGGER.info("Could not complete action");
					LOGGER.info(e.getMessage());
				}
			}

		};
		
		//------ Action ---------		
		Action act = new Action(Permission.WRITE,
				new Handler<ActionResult>() {
			@Override
			public void handle(ActionResult event) {

				int num = Integer.parseInt(event.getParameter("Socket").toString());
				Value val = new Value(num);
				socketNode.setValue(val);
				activeSocket = num;
				LOGGER.info("Socket has been changed to : " + num);
				Objects.getDaemonThreadPool().execute(listeningTask);			
			}
		});
		act.addParameter(new Parameter("Socket", ValueType.NUMBER));
		//-----------------------


		NodeBuilder builder4 = superRoot.createChild("UpdateNum");
		builder4.setSerializable(false);
		builder4.setDisplayName("Set Socket Number");
		builder4.setValueType(ValueType.NUMBER);
		builder4.setAction(act); 
		builder4.build();		
		
		LOGGER.info("Initialized");
		
		//first get the port number if it was initialized previously
		Map<String,Node> childrenNodes = superRoot.getChildren();		
		if(childrenNodes.size() > 0)
		{
			try
			{
				Node[] nodeArray = childrenNodes.values().toArray(new Node[childrenNodes.values().size()]);
				for(int i = 0; i < nodeArray.length; i++)
				{
					//loop through nodes and look for 'socket'
					if(nodeArray[i].getName().equals("socket"))
					{
						int num = Integer.parseInt(nodeArray[i].getValue().getNumber().toString());
						Value val = new Value(num);
						socketNode.setValue(val);
						activeSocket = num;
						LOGGER.info("Listening on socket " + activeSocket);
					}
				}					
			}
			catch(Exception ex)
			{
				LOGGER.info("Could not load previous socket");
				LOGGER.debug("Error\n" + ex.getMessage());
			}
		}	
		
		Objects.getDaemonThreadPool().execute(listeningTask);
		
	}


	@Override
	public void onResponderConnected(DSLink link) {
		LOGGER.info("Connected");						
	}

	public static void main(String[] args) {
		DSLinkFactory.start(args, new Main());
	}
	
}
