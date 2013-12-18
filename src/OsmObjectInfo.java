import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class OsmObjectInfo {
	final static int Log_Debug_Off = 0;			// Used to turn debug off
	final static int Log_Serious = 1;			// A serious error has occurred
	final static int Log_Error = 2;				// An error that we can work around has occurred
	final static int Log_Warning = 3;			// Not currently used
	final static int Log_Return = 4; 			// Return values from top-level subroutines
	final static int Log_Informational_1 = 5;	// Important informational stuff
	final static int Log_Top_Routine_Start = 6;	// top-level routine start code
	final static int Log_Low_Routine_Start = 7; // low-level routing start code
	final static int Log_Informational_2 = 8;	// Any other informational stuff

	public String item_id;
	public String item_user;
	public String item_uid;

	OsmObjectInfo()
	{
		item_id = "";
		item_user = "";
		item_uid = "";
	}

	String get_item_id()
	{
		return item_id;
	}

	String get_item_user()
	{
		return item_user;
	}

	String get_item_uid()
	{
		return item_uid;
	}

	
	private static boolean download_node( String passed_number, String passed_version, 
			String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			String api_path, int passed_arg_debug ) throws Exception
	{
		boolean return_value = false;
		
		if ( passed_arg_debug >= Log_Informational_2 )
		{
			System.out.println( "We will try and download node: " + passed_number );
		}
		
		URL url = new URL( api_path + "node/" + passed_number + "/" + passed_version );
		InputStreamReader input;
		
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput( true );
		urlConn.setDoOutput( false );
		urlConn.setUseCaches( false );
	
		input = new InputStreamReader( urlConn.getInputStream() );
	
	    char[] data = new char[ 256 ];
	    int len = 0;
		StringBuffer sb = new StringBuffer();		
	
	    while ( -1 != ( len = input.read( data, 0, 255 )) )
	    {
	        sb.append( new String( data, 0, len ));
	    }   
	
	    DocumentBuilderFactory AJTfactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder AJTbuilder = AJTfactory.newDocumentBuilder();
	    ByteArrayInputStream inputStream = new ByteArrayInputStream( sb.toString().getBytes( "UTF-8" ));
	
	    Document AJTdocument = AJTbuilder.parse( inputStream );
	    Element AJTrootElement = AJTdocument.getDocumentElement();

	    //qqqdo
	    return_value = process_node_xml( AJTrootElement, 
	    		passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_arg_debug );
	
	    input.close();
		return return_value;
	}
	
	private static boolean process_node_xml( Node root_node, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			int passed_arg_debug )
	{
		boolean node_overlaps = false;
		
		if ( root_node.getNodeType() == Node.ELEMENT_NODE ) 
		{
			NodeList level_1_xmlnodes = root_node.getChildNodes();
			int num_l1_xmlnodes = level_1_xmlnodes.getLength();
	
			if ( passed_arg_debug >= Log_Informational_2 )
			{
				System.out.println( "Changeset L1 nodes found: " + num_l1_xmlnodes );
			}
	
/* ------------------------------------------------------------------------------------------------------------
 * Iterate through the L1 nodes
 * ------------------------------------------------------------------------------------------------------------ */
			for ( int cntr_1 = 0; cntr_1 < num_l1_xmlnodes; ++cntr_1 ) 
			{
				Node this_l1_item = level_1_xmlnodes.item( cntr_1 );
				String l1_item_type = this_l1_item.getNodeName();

/* ------------------------------------------------------------------------------------------------------------
 * We're expecting "node" here
 * ------------------------------------------------------------------------------------------------------------ */
				if ( !l1_item_type.equals( "#text" ))
				{
					if ( passed_arg_debug >= Log_Informational_2 )
					{
						System.out.println( "Found 1: " + l1_item_type );
					}

					if ( this_l1_item.hasAttributes() )
					{
						NamedNodeMap node_attributes = this_l1_item.getAttributes();
						//qqqdo
						Node id_node = node_attributes.getNamedItem( "id" );
//						Node user_node = node_attributes.getNamedItem( "user" );
//						Node uid_node = node_attributes.getNamedItem( "uid" );
						Node lat_node = node_attributes.getNamedItem( "lat" );
						Node lon_node = node_attributes.getNamedItem( "lon" );
						Node version_node = node_attributes.getNamedItem( "version" );
						
						if ( lat_node == null )
						{
/* ------------------------------------------------------------------------------------------------------------
 * We've read the version of a node before the deletion so we would expect a lat here.
 * ------------------------------------------------------------------------------------------------------------ */
							if ( passed_arg_debug >= Log_Informational_1 )
							{
								System.out.println( "No lat for id: " + id_node.getNodeValue() + " version: " + version_node.getNodeValue()  );
							}
						}
						else
						{
							if ( lon_node == null )
							{
								if ( passed_arg_debug >= Log_Informational_1 )
								{
									System.out.println( "No lon for id: " + id_node.getNodeValue() + " version: " + version_node.getNodeValue()  );
								}
							}
							else
							{
								node_overlaps = check_overlap( passed_min_lat_string, passed_min_lon_string, 
										passed_max_lat_string, passed_max_lon_string, 
										id_node, lat_node, lon_node );
								
								if ( passed_arg_debug >= Log_Informational_2 )
								{
									System.out.println( "id: " + id_node.getNodeValue() + " overlaps: " + node_overlaps  );
								}
							}
						}
					}
				}
			} // for L1 nodes
		}
		
		return node_overlaps;
	}
	

	static boolean check_overlap( String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			Node id_node, Node lat_node, Node lon_node )
	{
		boolean node_overlaps = false;
		try
		{
			Double min_lat_d;
			Double min_lon_d;
			Double max_lat_d;
			Double max_lon_d;

			Double lat_d;
			Double lon_d;

			min_lat_d = Double.valueOf( passed_min_lat_string ); 
			min_lon_d = Double.valueOf( passed_min_lon_string ); 
			max_lat_d = Double.valueOf( passed_max_lat_string ); 
			max_lon_d = Double.valueOf( passed_max_lon_string ); 

			lat_d = Double.valueOf( lat_node.getNodeValue() );
			lon_d = Double.valueOf( lon_node.getNodeValue() );
			
			if (( lat_d > min_lat_d ) &&
				( lat_d < max_lat_d ) &&
				( lon_d > min_lon_d ) &&
				( lon_d < max_lon_d ))
			{
/* ------------------------------------------------------------------------------------------------------------
* We've found a node within our area of interest - set the return value accordingly.
* ------------------------------------------------------------------------------------------------------------ */
				node_overlaps = true;
			}
		}
		catch( Exception ex )
		{
			System.out.println( "Exception in node lat/lon processing for id: " + id_node.getNodeValue() );
		}

		return node_overlaps;
	}
	
	/**
	 * process_download_node
	 * 
	 * We're processing the XML for a changeset, and have encountered a node in it. 
	 * Check to see whether the node overlaps with our area of interest.
	 * 
	 * @param passed_min_lat_string  The bounding box that we're interested in - node positions will be checked against this box.
	 * @param passed_min_lon_string
	 * @param passed_max_lat_string
	 * @param passed_max_lon_string
	 * 
	 * @param passed_l2_item  The XML Node in the tree that corresponds to an OSM Node that we're interested in.  
	 * It might be part of a create, a modify, or a delete. 
	 * @param passed_arg_debug
	 * 
	 * @return  returns "true" if this node overlaps the bounding box that we passed in.
	 */
	boolean process_downloaded_changeset_node( String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			Node passed_l2_item, int passed_arg_debug, String passed_download_nodes, String passed_api_path )
	{
		boolean node_overlaps = false;
		
		if ( passed_l2_item.hasAttributes() )
		{
			NamedNodeMap node_attributes = passed_l2_item.getAttributes();
			Node id_node = node_attributes.getNamedItem( "id" );
			Node user_node = node_attributes.getNamedItem( "user" );
			Node uid_node = node_attributes.getNamedItem( "uid" );
			Node lat_node = node_attributes.getNamedItem( "lat" );
			Node lon_node = node_attributes.getNamedItem( "lon" );
			Node version_node = node_attributes.getNamedItem( "version" );
			
			if ( id_node == null )
			{
				System.out.println( "Downloaded changeset node processing: No id found" );
			}
			else
			{
				item_id = id_node.getNodeValue();
				
				if ( passed_arg_debug >= Log_Informational_2 )
				{
					System.out.println( "id: " + id_node.getNodeValue() );
				}

				if ( user_node == null )
				{
					System.out.println( "Download node processing: No user found" );
				}
				else
				{
					item_user = user_node.getNodeValue();
					
					if ( passed_arg_debug >= Log_Informational_2 )
					{
						System.out.println( "user: " + user_node.getNodeValue() );
					}
				}

				if ( uid_node == null )
				{
					System.out.println( "Download node processing: No uid found" );
				}
				else
				{
					item_uid = uid_node.getNodeValue();
					
					if ( passed_arg_debug >= Log_Informational_2 )
					{
						System.out.println( "uid: " + uid_node.getNodeValue() );
					}
				}

				if ( lat_node == null )
				{
/* ------------------------------------------------------------------------------------------------------------
* Possibly a deletion - the lat and lon of deleted nodes are not returned in the OSC document.
* ------------------------------------------------------------------------------------------------------------ */
					if ( passed_arg_debug >= Log_Informational_2 )
					{
						System.out.println( "No lat for id: " + id_node.getNodeValue() + ", probably a deletion.");
					}
					
					if ( passed_download_nodes.equals( "1" ))
					{
						if ( version_node == null )
						{
							if ( passed_arg_debug >= Log_Informational_2 )
							{
								System.out.println( "No version for id: " + id_node.getNodeValue() );
							}
						}
						else
						{
							if ( passed_arg_debug >= Log_Informational_2 )
							{
								System.out.println( "id: " + id_node.getNodeValue() + " is version: " + version_node.getNodeValue() );
							}
							
							try
							{
								String previous_version = String.valueOf( Integer.valueOf( version_node.getNodeValue()) - 1 );
								
								if ( passed_arg_debug >= Log_Informational_2 )
								{
									System.out.println( "previous version: " + previous_version );
								}
								
								try
								{
									node_overlaps = download_node( item_id, previous_version, 
											passed_min_lat_string, passed_min_lon_string, 
											passed_max_lat_string, passed_max_lon_string, 
											passed_api_path, passed_arg_debug );
								}
								catch( Exception ex )
								{
									if ( passed_arg_debug >= Log_Informational_1 )
									{
										System.out.println( "Exception downloading node id: " + id_node.getNodeValue() + " version: " + version_node.getNodeValue() + " from API." );
									}
								}
							}
							catch( Exception ex )
							{
/* ------------------------------------------------------------------------------
 * I'm guessing that there's some obscure circumstance in which it's valid to
 * have a "delete node" reference in a changset with no previous version.
 * I can imagine it on a "redaction" changeset; not sure about others.
 * 
 * If this happens we don't assign "node_overlaps" as we don't know whether it
 * overlaps or not.
 * ------------------------------------------------------------------------------ */
								if ( passed_arg_debug >= Log_Informational_1 )
								{
									System.out.println( "Exception obtaining previous version for node id: " + id_node.getNodeValue() + " version: " + version_node.getNodeValue() );
								}
							}
						}
					}
				}
				else
				{
					if ( lon_node == null )
					{
						if ( passed_arg_debug >= Log_Informational_2 )
						{
							System.out.println( "No lon for id: " + id_node.getNodeValue() );
						}
					}
					else
					{
						if (( !passed_min_lat_string.equals( "" )) &&
							( !passed_min_lon_string.equals( "" )) &&
							( !passed_max_lat_string.equals( "" )) &&
							( !passed_max_lon_string.equals( "" )))
						{
							node_overlaps = check_overlap( passed_min_lat_string, passed_min_lon_string, 
									passed_max_lat_string, passed_max_lon_string, 
									id_node, lat_node, lon_node );
							//qqqdo
//							try
//							{
//								Double min_lat_d;
//								Double min_lon_d;
//								Double max_lat_d;
//								Double max_lon_d;
//
//								Double lat_d;
//								Double lon_d;
//
//								min_lat_d = Double.valueOf( passed_min_lat_string ); 
//								min_lon_d = Double.valueOf( passed_min_lon_string ); 
//								max_lat_d = Double.valueOf( passed_max_lat_string ); 
//								max_lon_d = Double.valueOf( passed_max_lon_string ); 
//
//								lat_d = Double.valueOf( lat_node.getNodeValue() );
//								lon_d = Double.valueOf( lon_node.getNodeValue() );
//								
//								if (( lat_d > min_lat_d ) &&
//									( lat_d < max_lat_d ) &&
//									( lon_d > min_lon_d ) &&
//									( lon_d < max_lon_d ))
//								{
///* ------------------------------------------------------------------------------------------------------------
//* We've found a node within our area of interest - set the return value accordingly.
//* ------------------------------------------------------------------------------------------------------------ */
//									node_overlaps = true;
//								}
//							}
//							catch( Exception ex )
//							{
//								System.out.println( "Exception in node lat/lon processing for id: " + id_node.getNodeValue() );
//							}
						}
						else
						{
							if ( passed_arg_debug >= Log_Informational_2 )
							{
								System.out.println( "Node lat/lon processing - we are missing a lat or long, probably because we're interested in all changesets" );
							}
/* ------------------------------------------------------------------------------------------------------------
* We don't set "node_overlaps = true;" here because there's no need to list every node in a changeset - there
* are other ways to get that.
* ------------------------------------------------------------------------------------------------------------ */
						}

//any other processing of the attributes of id, lat and lon of our created, modified or deleted node would go here.
						
					} // we have a lon
				} // we have a lat
				
//any other attribute processing that doesn't need lat or lon could go here.
				
			} // id node not null
		} // node attributes
		
		return node_overlaps;
	}
	
	
/* ------------------------------------------------------------------------------------------------------------
 * We're processing the XML for a changeset, and have encountered a way or relation in it. 
 * Currently we just check for deletions.
 * ------------------------------------------------------------------------------------------------------------ */
	void process_downloaded_changeset_wayrelation( Node passed_l2_item, 
			String passed_l1_item_type, String passed_l2_item_type, String passed_changeset_number, 
			int passed_arg_debug, String passed_arg_out_file, PrintStream passed_myPrintStream )
	{
		if ( passed_l2_item.hasAttributes() )
		{
			NamedNodeMap node_attributes = passed_l2_item.getAttributes();
			Node id_node = node_attributes.getNamedItem( "id" );
			Node user_node = node_attributes.getNamedItem( "user" );
			Node uid_node = node_attributes.getNamedItem( "uid" );

			if ( id_node == null )
			{
				System.out.println( "Download way/relation processing: No id found" );
			}
			else
			{
				if ( passed_arg_debug >= Log_Informational_2 )
				{
					System.out.println( "id: " + id_node.getNodeValue() );
				}

				item_id = id_node.getNodeValue();
				
				if ( user_node == null )
				{
					System.out.println( "Download way/relation processing: No user found" );
				}
				else
				{
					item_user = user_node.getNodeValue();
				}
				
				if ( uid_node == null )
				{
					System.out.println( "Download way/relation processing: No uid found" );
				}
				else
				{
					item_uid = uid_node.getNodeValue();
				}
				
/* ------------------------------------------------------------------------------------------------------------
* Initially we'll just look for deleted ways and relations here.
* ------------------------------------------------------------------------------------------------------------ */
				if ( passed_arg_out_file != ""  )
				{
					if ( passed_l1_item_type.equals( "delete"   ))
					{
						if ( passed_l2_item_type.equals( "way" ))
						{
							passed_myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + id_node.getNodeValue() + " deleted" );
						}

						if ( passed_l2_item_type.equals( "relation" ))
						{
							passed_myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Relation " + id_node.getNodeValue() + " deleted" );
						}
					}
				}

//other processing of the attributes of our created, modified or deleted way/relation would go here.


			} // id node not null
		} // way / relation attributes
	}

	
}
