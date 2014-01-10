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

	
	/**
	 * download_node
	 * 
	 * Download a SPECIFIC version of a specific node from the API, and compare its lat and lon with the bounding box that we're interested in.
	 * If we haven't got a bounding box we shouldn't have got this far.
	 * 
	 * @param passed_node_id  The node ID
	 * @param passed_version  The version of the node to get (we subtracted 1 from the version that was deleted to get this)
	 * 
	 * @param passed_min_lat_string  The bounding box
	 * @param passed_min_lon_string
	 * @param passed_max_lat_string
	 * @param passed_max_lon_string
	 * 
	 * @param api_path  Set by the "-dev" flag in the calling class
	 * @param passed_arg_debug  Integer debug value, passed through
	 * 
	 * @return  Returns true if the node is within our bounding box, false if not or if something goes wrong.
	 * 
	 * @throws Exception
	 */
	boolean download_node( String passed_node_id, String passed_version, 
			String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			String api_path, int passed_arg_debug ) throws Exception
	{
		boolean return_value = false;
		
		if ( passed_arg_debug >= Log_Informational_2 )
		{
			System.out.println( "We will try and download node: " + passed_node_id + " version: " + passed_version );
		}
		
		URL url = new URL( api_path + "node/" + passed_node_id + "/" + passed_version );
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

	    return_value = process_node_xml( AJTrootElement, 
	    		passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_arg_debug );
	
	    input.close();
		return return_value;
	}
	
	/**
	 * download_node
	 * 
	 * Download the LATEST version of a specific node from the API, and compare its lat and lon with the bounding box that we're interested in.
	 * If we haven't got a bounding box we shouldn't have got this far.
	 * 
	 * @param passed_node_id  The node ID
	 * 
	 * @param passed_min_lat_string  The bounding box
	 * @param passed_min_lon_string
	 * @param passed_max_lat_string
	 * @param passed_max_lon_string
	 * 
	 * @param api_path  Set by the "-dev" flag in the calling class
	 * @param passed_arg_debug  Integer debug value, passed through
	 * 
	 * @return  Returns true if the node is within our bounding box, false if not or if something goes wrong.
	 * 
	 * @throws Exception
	 */
	boolean download_node( String passed_node_id,  
			String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			String api_path, int passed_arg_debug ) throws Exception
	{
		boolean return_value = false;
		
		if ( passed_arg_debug >= Log_Informational_2 )
		{
			System.out.println( "We will try and download node: " + passed_node_id + " (latest version)");
		}
		
		URL url = new URL( api_path + "node/" + passed_node_id );
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

	    return_value = process_node_xml( AJTrootElement, 
	    		passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_arg_debug );
	
	    input.close();
		return return_value;
	}
	
	/**
	 * process_node_xml
	 * 
	 * @param root_node  The XML root node of the XML that we've got back from the "node" / "version" API call
	 * 
	 * @param passed_min_lat_string  The bounding box that we're interested in.
	 * @param passed_min_lon_string
	 * @param passed_max_lat_string
	 * @param passed_max_lon_string
	 * 
	 * @param passed_arg_debug  The integer debug level passed through.
	 *  
	 * @return  Returns true if within our bounding box, false if not or if something goes wrong.
	 */
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
						Node id_node = node_attributes.getNamedItem( "id" );
/* ------------------------------------------------------------------------------------------------------------
 * Other attributes available here include "user" and "uid", though we don't currently obtain or report on them. 
 * ------------------------------------------------------------------------------------------------------------ */
						Node lat_node = node_attributes.getNamedItem( "lat" );
						Node lon_node = node_attributes.getNamedItem( "lon" );
						Node version_node = node_attributes.getNamedItem( "version" );
						
						if ( lat_node == null )
						{
/* ------------------------------------------------------------------------------------------------------------
 * We've read the version of a node before the deletion so we would expect a lat here, hence an informational
 * rather than debug error.
 * ------------------------------------------------------------------------------------------------------------ */
							if ( passed_arg_debug >= Log_Informational_1 )
							{
								System.out.println( "No lat for id: " + id_node.getNodeValue() + " version: " + version_node.getNodeValue()  );
							}
						}
						else
						{ // valid lat
							if ( lon_node == null )
							{
								if ( passed_arg_debug >= Log_Informational_1 )
								{
									System.out.println( "No lon for id: " + id_node.getNodeValue() + " version: " + version_node.getNodeValue()  );
								}
							}
							else
							{ // valid lon
								node_overlaps = check_overlap( passed_min_lat_string, passed_min_lon_string, 
										passed_max_lat_string, passed_max_lon_string, 
										id_node, lat_node, lon_node );
								
								if ( passed_arg_debug >= Log_Informational_2 )
								{
									System.out.println( "id: " + id_node.getNodeValue() + " overlaps: " + node_overlaps  );
								}
							}
						}
					} // has attributes
				} // non-"#TEXT" nodes, i.e. the one called "node".
			} // for L1 nodes
		} // ELEMENT_NODE.  We're not expecting to need to look at anything else.
		
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
	 * process_downloaded_changeset_node_attributes
	 * 
	 * We're processing the XML for a changeset, and have encountered a node in it. 
	 * Check to see whether the node overlaps with our area of interest, if we've got one.
	 * If we don't have an area of interest "passed_min_lat_string" etc. won't be valid, and we won't 
	 * try and obtain lat and lon values even for deleted nodes (which requires an extra call to the server).
	 * 
	 * @param passed_min_lat_string  The bounding box that we're interested in - node positions will be checked against this box.
	 * @param passed_min_lon_string
	 * @param passed_max_lat_string
	 * @param passed_max_lon_string
	 * 
	 * @param passed_l2_item  The XML Node in the tree that corresponds to an OSM Node that we're interested in.  
	 * It might be part of a create, a modify, or a delete. 
	 * 
	 * @param passed_arg_debug
	 * 
	 * @param passed_overlapnodes  Set if we want to report on every overlapping node in the changeset.
	 * 
	 * @param nodes_overlap  Do we think, based on what we've seen of the changeset so far, that its nodes overlap?  If we do, 
	 * and passed_overlapnodes is not set (i.e. we don't need to report on every node) then we don't need to download
	 * the previous version of individual deleted nodes to see if they overlap, even if passed_download_nodes is set.
	 * 
	 * @param passed_download_nodes  Set to "1" if we're being asked to download individual nodes (such as the previous versions
	 * of deleted nodes) to see if they overlap.  Ignored if we don't have a valid bounding box, since we don't need to explicitly 
	 * output a list of overlapping nodes in the changeset - by definition we are interested in them all.
	 * 
	 * @param passed_api_path  The API path for the call, set in the calling class based on the "-dev" parameter. 
	 * 
	 * @return  returns "true" if this node overlaps the bounding box that we passed in.
	 */
	boolean process_downloaded_changeset_node_attributes( String passed_min_lat_string, String passed_min_lon_string, 
			String passed_max_lat_string, String passed_max_lon_string, 
			Node passed_l2_item, int passed_arg_debug, boolean passed_overlapnodes, boolean nodes_overlap, String passed_download_nodes, String passed_api_path )
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
					
/* ------------------------------------------------------------------------------------------------------------
 * We've been asked to actually try and download nodes to see if the lat / lon of the previous version of a 
 * node was in our bounding box, but we only do this if we HAVE got a bounding box passed through, and if (we 
 * don't yet know whether the changeset overlaps) or (we've been asked to report on all overlapping nodes).
 * ------------------------------------------------------------------------------------------------------------ */
					if ((  passed_download_nodes.equals( "1"   )) &&
					    ( !passed_min_lat_string.equals( ""    )) &&
					    ( !passed_min_lon_string.equals( ""    )) &&
					    ( !passed_max_lat_string.equals( ""    )) &&
					    ( !passed_max_lon_string.equals( ""    )) &&
					    ( !nodes_overlap || passed_overlapnodes )) 
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
				} // lat_node null
				else
				{ // lat_node NOT null
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
	
	
	/**
	 * process_downloaded_changeset_wayrelation_attributes
	 * 
	 * We're processing the XML for a changeset, and have encountered a way or relation in it. 
	 * Currently we just check for deletions.
	 * 
	 * @param passed_l2_item  The XML node corresponding to the current way or relation
	 * 
	 * @param passed_l1_item_type  "create", "modify" or "delete" 
	 * @param passed_l2_item_type  "way" or "relation"
	 * 
	 * @param passed_changeset_number
	 * @param passed_arg_debug
	 * @param passed_arg_out_file
	 * @param passed_myPrintStream
	 */
	void process_downloaded_changeset_wayrelation_attributes( Node passed_l2_item, 
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
