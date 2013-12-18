import java.io.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


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
	boolean process_download_node( String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			Node passed_l2_item, int passed_arg_debug, String passed_download_nodes )
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
			
			if ( id_node == null )
			{
				System.out.println( "Download node processing: No id found" );
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
						System.out.println( "No lat for id: " + id_node.getNodeValue() );
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
	void process_download_wayrelation( Node passed_l2_item, 
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
