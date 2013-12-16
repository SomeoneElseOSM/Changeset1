import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// See also https://github.com/SomeoneElseOSM/Changeset1 for more details

public class Changeset1 
{
	static final String live_api_path = "http://openstreetmap.org/api/0.6/";
	static final String dev_api_path = "http://api06.dev.openstreetmap.org/api/0.6/";
  
	final static int Log_Debug_Off = 0;			// Used to turn debug off
	final static int Log_Serious = 1;			// A serious error has occurred
	final static int Log_Error = 2;				// An error that we can work around has occurred
	final static int Log_Warning = 3;			// Not currently used
	final static int Log_Return = 4; 			// Return values from top-level subroutines
	final static int Log_Informational_1 = 5;	// Important informational stuff
	final static int Log_Top_Routine_Start = 6;	// top-level routine start code
	final static int Log_Low_Routine_Start = 7; // low-level routing start code
	final static int Log_Informational_2 = 8;	// Any other informational stuff

	final static String param_input = "-input=";
	final static String param_output = "-output=";
	final static String param_display_name = "-display_name=";
	final static String param_uid = "-user=";
	final static String param_time = "-time=";
	final static String param_dev = "-dev";
	final static String param_debug = "-debug=";
	final static String param_bbox = "-bbox=";		// Not passed to the API; we compare changesets in that box
	final static String param_download = "-download=";
	final static String param_building = "-building=";
	
	final static int Overlap_All   = 1;
	final static int Overlap_Yes   = 2;
	final static int Overlap_No    = 3;
	final static int Overlap_Error = 4;
	
	static String api_path = live_api_path;		// Set to dev_api_path for testing via -dev
	static String arg_in_file = "";				// -input=
	static String arg_out_file = "";			// -output=
	static int arg_debug = 0;					// -debug=
	static String arg_bbox = "";				// -bbox=
	static String arg_download = "0";			// -download
/* ------------------------------------------------------------------------------
 * This value is used to compare the number of ways in buildings (and shops) 
 * against.  The default is 2001 (more than the maximum number of nodes in a way)
 * so by default we won't flag any potentially "created by mistake" buildings.
 * 
 * The source of these erronous buildings is iD issue 542.  New mappers click in
 * a landuse area and set the details at the left, and then save, not realising
 * that they have changed the landuse to a building.
 * ------------------------------------------------------------------------------ */
	static String arg_building = "2001";		// -building
	
	static String arg_min_lat_string = "";
	static String arg_min_lon_string = "";
	static String arg_max_lat_string = "";
	static String arg_max_lon_string = "";

	static FileReader myFileReader;
	static BufferedReader myBufferedReader;
	static OutputStream myOutputStream;
	static PrintStream myPrintStream;
	
	static boolean check_lon_overlap( Double arg_min_lon, Double arg_max_lon,
			                          Double min_lon,     Double max_lon )
	{
		boolean return_value = false;
		
		if ( arg_min_lon < min_lon )
		{
			if ( arg_max_lon > min_lon )
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "In 1: " + arg_min_lon + " " + arg_max_lon + " " + min_lon + " " + max_lon);
				}

				return_value = true;
			}
			else
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Out because arg lon is too small: " + arg_min_lon + " " + arg_max_lon + " " + min_lon + " " + max_lon );
				}

				return_value = false;
			}
		}
		else
		{
			if ( arg_min_lon < max_lon )
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "In 2: " + arg_min_lon + " " + arg_max_lon + " " + min_lon + " " + max_lon);
				}

				return_value = true;
			}
			else
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Out because arg lon is too big: " + arg_min_lon + " " + arg_max_lon + " " + min_lon + " " + max_lon  );
				}

				return_value = false;
			}
		}

		return return_value;
	}
	
	
	static boolean check_overlap( Double arg_min_lon, Double arg_min_lat, Double arg_max_lat, Double arg_max_lon,
			                      Double min_lon,     Double min_lat,     Double max_lat,     Double max_lon )
	{
		boolean return_value = false;
		
		if ( arg_min_lat < min_lat )
		{
			if ( arg_max_lat > min_lat )
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Probably in 1; look at longitude: " + arg_min_lat + " " + arg_max_lat + " " + min_lat + " " + max_lat);
				}

				return_value = check_lon_overlap( arg_min_lon, arg_max_lon, min_lon, max_lon );
			}
			else
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Out because arg lat is too small: " + arg_min_lat + " " + arg_max_lat + " " + min_lat + " " + max_lat );
				}

				return_value = false;
			}
		}
		else
		{
			if ( arg_min_lat < max_lat )
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Probably in 2; look at longitude: " + arg_min_lat + " " + arg_max_lat + " " + min_lat + " " + max_lat );
				}

				return_value = check_lon_overlap( arg_min_lon, arg_max_lon, min_lon, max_lon );
			}
			else
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Out because arg lat is too big: " + arg_min_lat + " " + arg_max_lat + " " + min_lat + " " + max_lat  );
				}

				return_value = false;
			}
		}
		
		return return_value;
	}
	


	private static int check_bbox_interest( Node root_node, NamedNodeMap item_attributes, Node id_node, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string )
	{
		boolean it_overlaps = true;
		int return_value = 0;
		Double p_min_lat_d;
		Double p_min_lon_d;
		Double p_max_lat_d;
		Double p_max_lon_d;
		
		if ( passed_min_lat_string.length() == 0 )
		{
			if ( arg_debug >= Log_Informational_1 )
			{
				System.out.println( "We're interested in all changesets" );
			}

			return_value = Overlap_All;
		}
		else
		{
			Double min_lat_d;
			Double min_lon_d;
			Double max_lat_d;
			Double max_lon_d;
			
			try
			{
				min_lat_d = Double.valueOf( item_attributes.getNamedItem( "min_lat" ).getNodeValue() );
				min_lon_d = Double.valueOf( item_attributes.getNamedItem( "min_lon" ).getNodeValue() );
				max_lat_d = Double.valueOf( item_attributes.getNamedItem( "max_lat" ).getNodeValue() );
				max_lon_d = Double.valueOf( item_attributes.getNamedItem( "max_lon" ).getNodeValue() );
				
				p_min_lat_d = Double.valueOf( passed_min_lat_string ); 
				p_min_lon_d = Double.valueOf( passed_min_lon_string ); 
				p_max_lat_d = Double.valueOf( passed_max_lat_string ); 
				p_max_lon_d = Double.valueOf( passed_max_lon_string ); 

				it_overlaps = check_overlap( p_min_lon_d, p_min_lat_d, p_max_lat_d, p_max_lon_d,
			                                 min_lon_d, min_lat_d, max_lat_d, max_lon_d );

				if ( it_overlaps == true )
				{
					return_value = Overlap_Yes;

					if ( arg_debug >= Log_Informational_1 )
					{
						System.out.println( "We're interested in this changeset" );
					}
				}
				else
				{
					return_value = Overlap_No;

					if ( arg_debug >= Log_Informational_1 )
					{
						System.out.println( "We're not interested in this changeset" );
					}
				}
			}
			catch( Exception ex )
			{
				return_value = Overlap_Error;
				
				if ( arg_debug >= Log_Informational_1 )
				{
					System.out.println( "Error parsing lat or lon from this changeset, so we'll assume that we are interested in it.  Error: " + ex.getMessage() );
					
/* ------------------------------------------------------------------------------
 * This can happen if a changeset has no nodes inside it (since we're checking
 * node positions, not the changeset bounding box, if we choose to download the
 * changeset).  
 * ------------------------------------------------------------------------------ */
				}
			}
		}
		
		return return_value;
	}


/* ------------------------------------------------------------------------------------------------------------
 * The XML node passed in here is the root node of the XML tree of the download of this changeset
 * The other parameters are used for comparisons and reporting.
 * The return value is "does this changeset have nodes within our area of interest".
 * ------------------------------------------------------------------------------------------------------------ */
	private static boolean process_download_xml( Node root_node, String passed_changeset_number, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_building )
	{
		boolean return_value = false;
		
		if ( root_node.getNodeType() == Node.ELEMENT_NODE ) 
		{
			NodeList level_1_xmlnodes = root_node.getChildNodes();
			int num_l1_xmlnodes = level_1_xmlnodes.getLength();
	
			if ( arg_debug >= Log_Informational_2 )
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
 * We're expecting "create", "modify" or "delete" here
 * ------------------------------------------------------------------------------------------------------------ */
				if ( !l1_item_type.equals( "#text" ))
				{
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "Found 1: " + l1_item_type );
					}

					NodeList level_2_xmlnodes = this_l1_item.getChildNodes();
					int num_l2_xmlnodes = level_2_xmlnodes.getLength();
	
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "Changeset L2 nodes found: " + num_l2_xmlnodes );
					}
	                    
					for ( int cntr_2 = 0; cntr_2 < num_l2_xmlnodes; ++cntr_2 ) 
					{
						Node this_l2_item = level_2_xmlnodes.item( cntr_2 );
						String l2_item_type = this_l2_item.getNodeName();
						boolean node_overlaps = false;
						String item_id = "";
						String item_user = "";
						String item_uid = "";
						String node_name = "";
						boolean building_or_shop_found = false;

/* ------------------------------------------------------------------------------------------------------------
 * We're expecting "node", "way" or "relation" here
 * ------------------------------------------------------------------------------------------------------------ */
						if ( !l2_item_type.equals( "#text" ))
						{
							if ( arg_debug >= Log_Informational_2 )
							{
								System.out.println( "Found 2: " + l2_item_type );
							}

/* ------------------------------------------------------------------------------------------------------------
 * Look at attributes first.  These vary depending on whether we've got a node or a way or relation.
 * ------------------------------------------------------------------------------------------------------------ */
							if ( l2_item_type.equals( "node" ))
							{
								OsmObjectInfo osm_node = new OsmObjectInfo();
								
								node_overlaps = osm_node.process_download_node( passed_min_lat_string, passed_min_lon_string, 
										passed_max_lat_string, passed_max_lon_string, 
										this_l2_item, arg_debug );
								
								item_id = osm_node.get_item_id();
								item_user = osm_node.get_item_user();
								item_uid = osm_node.get_item_uid();
								
							} //node
							else
							{
								if (( l2_item_type.equals( "way"      )) ||
								    ( l2_item_type.equals( "relation" )))
								{
									OsmObjectInfo osm_wayrelation = new OsmObjectInfo();
									
									osm_wayrelation.process_download_wayrelation( this_l2_item,  
											l1_item_type, l2_item_type, passed_changeset_number, 
											arg_debug, arg_out_file, myPrintStream );
									
									item_id = osm_wayrelation.get_item_id();
									item_user = osm_wayrelation.get_item_user();
									item_uid = osm_wayrelation.get_item_uid();

								} // way or relation
								else
								{
									System.out.println( "Unexpected l2_item_type: " + l2_item_type );
								} // unexpected item type
							} // !node

							NodeList level_3_xmlnodes = this_l2_item.getChildNodes();
							int num_l3_xmlnodes = level_3_xmlnodes.getLength();
							int relation_members = 0;
							int way_nodes = 0;
							int item_tags = 0;
			
							if ( arg_debug >= Log_Informational_2 )
							{
								System.out.println( "Changeset L3 nodes found: " + num_l3_xmlnodes );
							}
			                    
							for ( int cntr_3 = 0; cntr_3 < num_l3_xmlnodes; ++cntr_3 ) 
							{
								Node this_l3_item = level_3_xmlnodes.item( cntr_3 );
								String l3_item_type = this_l3_item.getNodeName();

/* ------------------------------------------------------------------------------------------------------------
 * Depending on l2_item_type ("node", "way" or "relation"), we're expecting "nd", "member" or "tag" here.
 * ------------------------------------------------------------------------------------------------------------ */
								if ( !l3_item_type.equals( "#text" ))
								{
									if ( arg_debug >= Log_Informational_2 )
									{
										System.out.println( "Found 3: " + l3_item_type );
									}
									
/* ------------------------------------------------------------------------------------------------------------
 * "nd" implies we're processing a way, so increment the counter for the number of nodes in the way.
 * ------------------------------------------------------------------------------------------------------------ */
									if ( l3_item_type.equals( "nd" ))
									{
										way_nodes++;
										
										if ( this_l3_item.hasAttributes() )
										{
											NamedNodeMap item_attributes = this_l3_item.getAttributes();
											Node ref_node = item_attributes.getNamedItem( "ref" );
	
											if ( ref_node == null )
											{
												System.out.println( "Download way member  processing: No nd ref found" );
											}
											else
											{
												if ( arg_debug >= Log_Informational_2 )
												{
													System.out.println( "nd ref: " + ref_node.getNodeValue() );
												}
	
// here would go some actual processing of the nd from the OSC file
												
											} // nd node not null
										} // nd attributes
									} // nd
									else
									{
										if ( l3_item_type.equals( "tag" ))
										{
											item_tags++;
											
											if ( this_l3_item.hasAttributes() )
											{
												NamedNodeMap item_attributes = this_l3_item.getAttributes();
												Node key_node = item_attributes.getNamedItem( "k" );
												Node value_node = item_attributes.getNamedItem( "v" );
		
												if ( key_node == null )
												{
													System.out.println( "Download tag/value processing: No key found" );
/* ------------------------------------------------------------------------------------------------------------
 * If we haven't found a tag name don't bother looking for a value. 
 * ------------------------------------------------------------------------------------------------------------ */
												}
												else
												{
													if ( arg_debug >= Log_Informational_2 )
													{
														System.out.println( "tag: " + key_node.getNodeValue() );
													}
		
/* ------------------------------------------------------------------------------------------------------------
 * We have found a tag name - check the value. 
 * ------------------------------------------------------------------------------------------------------------ */
													if ( value_node == null )
													{
														System.out.println( "Download tag/value processing: No value found" );
													}
													else
													{
														if ( arg_debug >= Log_Informational_2 )
														{
															System.out.println( "value: " + value_node.getNodeValue() );
														}
		
														if ( key_node.getNodeValue().equals( "name" ))
														{
															node_name = value_node.getNodeValue();
														}
														else
														{
															if ( key_node.getNodeValue().equals( "building" ) || key_node.getNodeValue().equals( "shop" ))
															{
																building_or_shop_found = true;
															}
															
// here would go any other processing of the tag / value from the OSC file
																													
														}
													}
												} // key node not null
											} // tag attributes
										} // tag
										else
										{
											if ( l3_item_type.equals( "member" ))
											{
												relation_members++;
												
												if ( this_l3_item.hasAttributes() )
												{
													NamedNodeMap item_attributes = this_l3_item.getAttributes();
													Node type_node = item_attributes.getNamedItem( "type" );
													Node ref_node = item_attributes.getNamedItem( "ref" );
													Node role_node = item_attributes.getNamedItem( "role" );
			
													if ( type_node == null )
													{
														System.out.println( "Download member processing: No type found" );
/* ------------------------------------------------------------------------------------------------------------
 * If we haven't found a member type; don't bother looking for a ref 
 * ------------------------------------------------------------------------------------------------------------ */
													}
													else
													{
														if ( arg_debug >= Log_Informational_2 )
														{
															System.out.println( "type: " + type_node.getNodeValue() );
														}
			
/* ------------------------------------------------------------------------------------------------------------
 * We have found a member type - check the ref 
 * ------------------------------------------------------------------------------------------------------------ */
		
														if ( ref_node == null )
														{
															System.out.println( "Download member processing: No ref found" );
/* ------------------------------------------------------------------------------------------------------------
 * If we haven't found a member ref; don't bother looking for a role 
 * ------------------------------------------------------------------------------------------------------------ */
														}
														else
														{
															if ( arg_debug >= Log_Informational_2 )
															{
																System.out.println( "ref: " + ref_node.getNodeValue() );
															}
/* ------------------------------------------------------------------------------------------------------------
 * We have found a member ref - check the role 
 * ------------------------------------------------------------------------------------------------------------ */
															if ( role_node == null )
															{
																System.out.println( "Download member processing: No role found" );
															}
															else
															{
																if ( arg_debug >= Log_Informational_2 )
																{
																	System.out.println( "role: " + role_node.getNodeValue() );
																}
																
// here would go some actual processing of the type / ref / role  from the OSC file
															}
														}
													} // type node not null
												} // member attributes
											} // member
											else
											{
												System.out.println( "Changeset other L3 item found: " + l3_item_type );
											}
										} // !tag
									} // !nd
								}
							} // for level 3 nodes

							if ( node_overlaps )
							{
								return_value = true;
								
								if ( arg_out_file != ""  )
								{
									myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Node " + item_id + " (" + node_name + ") overlaps" );
								}
							}

/* ------------------------------------------------------------------------------------------------------------
 * Report on relations without members etc.
 * Deleted relations are reported elsewhere - don't also report that they have no members. 
 * ------------------------------------------------------------------------------------------------------------ */

							if ( l2_item_type.equals( "relation" ))
							{
								if ( arg_debug >= Log_Informational_2 )
								{
									System.out.println( "Members: " + relation_members );
								}

								if ( relation_members == 0 )
								{
									if (( arg_out_file != ""                ) &&
										( !l1_item_type.equals( "delete"   )))
									{
										myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Relation " + item_id + " has no members" );
									}
								}
							}

/* ------------------------------------------------------------------------------------------------------------
 * Deleted ways are reported elsewhere - don't also report that they have no members.
 * We do report on single-node ways though. 
 * ------------------------------------------------------------------------------------------------------------ */
							if ( l2_item_type.equals( "way" ))
							{
								if ( arg_debug >= Log_Informational_2 )
								{
									System.out.println( "Nodes: " + way_nodes );
								}
								
								if ( way_nodes == 0 )
								{
									if (( arg_out_file != ""                ) &&
										( !l1_item_type.equals( "delete"   )))
									{
										myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + item_id + " has no nodes" );
									}
								}
								
								if ( way_nodes == 1 )
								{
									if ( arg_out_file != ""  )
									{
										myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + item_id + " has only 1 node" );
									}
								}
								
								try
								{
									if ( building_or_shop_found && ( way_nodes > Integer.valueOf( passed_building )))
									{
										myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + item_id + " is a huge building or shop" );
									}
								}
								catch( Exception ex )
								{
									myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + item_id + " - error evaluating way nodes: " + ex.getMessage() );
								}
							}

/* ------------------------------------------------------------------------------------------------------------
 * Deleted items are expected to have no tags; likewise nodes probably won't.
 * Ways and Relations probably will, though (although this ignores ways that might be part of relations)
 * ------------------------------------------------------------------------------------------------------------ */
							if ( arg_debug >= Log_Informational_2 )
							{
								System.out.println( "Tags: " + item_tags );
							}

							if (( arg_out_file != ""                ) &&
								( !l1_item_type.equals( "delete"   )) &&
								( !l2_item_type.equals( "node"     )) &&
								( item_tags == 0                    ))
							{
								if ( l2_item_type.equals( "way" ))
								{
									myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + item_id + " currently has no tags" );
								}
								else
								{
									myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Relation " + item_id + " currently has no tags" );
								}
							}
						}
					}
				}
			}
		}
		
		return return_value;
	}
	
	private static boolean download_changeset( String passed_number, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_building ) throws Exception
	{
		boolean return_value = false;
		
		if ( arg_debug >= Log_Informational_2 )
		{
			System.out.println( "We will try and download: " + passed_number );
		}
		
		URL url = new URL( api_path + "changeset/" + passed_number + "/download" );
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
	    return_value = process_download_xml( AJTrootElement, passed_number, 
	    		passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
	    		passed_building );
	
	    input.close();
	    return return_value;
	}
	
	
/* ------------------------------------------------------------------------------------------------------------
 * The node passed in here is the root node of the XML tree of the changesets returned in response to our query
 * ------------------------------------------------------------------------------------------------------------ */
	private static void process_changesets_xml( Node root_node, String passed_display_name, String passed_uid, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building )
	{
		int osm_changesets_found = 0;
		int osm_changesets_of_interest = 0;
	
		if ( root_node.getNodeType() == Node.ELEMENT_NODE ) 
		{
			NodeList level_1_xmlnodes = root_node.getChildNodes();
			int num_l1_xmlnodes = level_1_xmlnodes.getLength();
	
			if ( arg_debug >= Log_Informational_2 )
			{
				System.out.println( "Changesets L1 nodes found: " + num_l1_xmlnodes );
			}
	
/* ------------------------------------------------------------------------------------------------------------
 * Iterate through the changesets for a user. 
 * ------------------------------------------------------------------------------------------------------------ */
			for ( int cntr_1 = 0; cntr_1 < num_l1_xmlnodes; ++cntr_1 ) 
			{
				Node this_l1_item = level_1_xmlnodes.item( cntr_1 );
				String l1_item_type = this_l1_item.getNodeName();
	
				if ( l1_item_type.equals( "changeset" ))
				{
					int changeset_bbox_interest_flag = 0;
					Node id_node = null; 
					Node user_node = null;
					Node uid_node = null;
					String editor_name = "";
					String editor_version = "";
					String comment = "";
					
					osm_changesets_found++;
	
					NodeList level_2_xmlnodes = this_l1_item.getChildNodes();
					int num_l2_xmlnodes = level_2_xmlnodes.getLength();
	
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "L2 nodes found: " + num_l2_xmlnodes );
					}
	                    
/* ------------------------------------------------------------------------------------------------------------
 * Items can have both attributes (e.g. "id", "user") and tags (XML child nodes) - process the attributes first. 
 * ------------------------------------------------------------------------------------------------------------ */
					if ( this_l1_item.hasAttributes() )
					{
						NamedNodeMap item_attributes = this_l1_item.getAttributes();
						id_node = item_attributes.getNamedItem( "id" );
						user_node = item_attributes.getNamedItem( "user" );
						uid_node = item_attributes.getNamedItem( "uid" );
						
						if ( id_node == null )
						{
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "No changeset ID found" );
							}
			            }
						else
						{
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "Changeset: " + id_node.getNodeValue() );
							}
						}

						if ( user_node == null )
						{
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "No user name found" );
							}
			            }
						else
						{
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "User: " + user_node.getNodeValue() );
							}
						}

						if ( uid_node == null )
						{
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "No uid found" );
							}
			            }
						else
						{
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "Uid: " + uid_node.getNodeValue() );
							}
						}

/* ------------------------------------------------------------------------------------------------------------
 * Based on the changset XML only, decide whether we're interested in processing this changset. 
 * ------------------------------------------------------------------------------------------------------------ */
						changeset_bbox_interest_flag = check_bbox_interest( root_node, item_attributes, id_node, 
								passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string );
					} // attributes
					
					for ( int cntr_2 = 0; cntr_2 < num_l2_xmlnodes; ++cntr_2 ) 
					{
						Node this_l2_item = level_2_xmlnodes.item( cntr_2 );
						String l2_item_type = this_l2_item.getNodeName();

						if ( l2_item_type.equals( "tag" ))
						{
							if ( this_l2_item.hasAttributes() )
							{
								NamedNodeMap item_attributes = this_l2_item.getAttributes();
								Node key_node = item_attributes.getNamedItem( "k" );
								Node value_node = item_attributes.getNamedItem( "v" );

								if ( key_node == null )
								{
									System.out.println( "Changeset tag/value processing: No key found" );
/* ------------------------------------------------------------------------------------------------------------
 * If we haven't found a tag name don't bother looking for a value. 
 * ------------------------------------------------------------------------------------------------------------ */
								}
								else
								{
									if ( arg_debug >= Log_Informational_2 )
									{
										System.out.println( "tag: " + key_node.getNodeValue() );
									}

/* ------------------------------------------------------------------------------------------------------------
 * We have found a tag name - check the value. 
 * ------------------------------------------------------------------------------------------------------------ */

									if ( value_node == null )
									{
										System.out.println( "Changeset tag/value processing: No value found" );
									}
									else
									{
										if ( arg_debug >= Log_Informational_2 )
										{
											System.out.println( "value: " + value_node.getNodeValue() );
										}

										if ( key_node.getNodeValue().equals( "created_by" ))
										{
											editor_name = value_node.getNodeValue();
										}

										if ( key_node.getNodeValue().equals( "version" ))
										{
											editor_version = value_node.getNodeValue();
										}

										if ( key_node.getNodeValue().equals( "comment" ))
										{
											comment = value_node.getNodeValue();
										}
									}
								}

							} // tag item has attributes
						}
					}

/* ------------------------------------------------------------------------------------------------------------
 * We've processed all attributes and child nodes; write out what we know about this changeset if we are 
 * interested in it.
 * ------------------------------------------------------------------------------------------------------------ */
					if ( changeset_bbox_interest_flag == Overlap_All )
					{
/* ------------------------------------------------------------------------------------------------------------
 * We're interested in all changesets.  Although download_changeset will return true if a node within the 
 * changeset has a lat/lon within the lat/lon ranges that we are interested in, we don't care.  
 * ------------------------------------------------------------------------------------------------------------ */
						osm_changesets_of_interest++;

						if ( arg_out_file != "" )
						{
							myPrintStream.println( user_node.getNodeValue() + ";" + uid_node.getNodeValue() + ";" + id_node.getNodeValue() + ";" + editor_name + ";" + editor_version + ";" + comment + ";Changeset: all" );
						}

/* ------------------------------------------------------------------------------------------------------------
 * By default we don't download the changesets themselves - we only do that if explicitly requested to by the
 * user.  The "-building=X" value only makes sense if we are downloading changesets. 
 * ------------------------------------------------------------------------------------------------------------ */
						if ( passed_download.equals( "1") )
						{
							try
							{
								download_changeset( id_node.getNodeValue(), 
										passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
										passed_building );
							}
							catch( Exception ex )
							{
								System.out.println( "Exception downloading changeset" );
							}
						}
					} // Overlap_All
					else
					{
/* ------------------------------------------------------------------------------------------------------------
 * If we have "changeset_bbox_interest_flag == Overlap_Error" it's probably because a changeset has no bounding
 * box, which can happen if it has nothing at all or only relations in it.
 * 
 * Because we don't know, we'll assume that we are interested.
 * ------------------------------------------------------------------------------------------------------------ */
						if (( changeset_bbox_interest_flag == Overlap_Yes   ) ||
							( changeset_bbox_interest_flag == Overlap_Error ))
						{
/* ------------------------------------------------------------------------------------------------------------
 * We're interested in overlapping changesets.  If we're downloading it well set the "interested" counter based
 * on whether nodes in the changeset are in our area of interest.  If not, we'll use the changeset bbox (which
 * we already know because our_interest is set to Overlap_Yes).
 * 
 * Note that the "bbox" parameter wasn't passed to the API so we're reading through all changesets for a user
 * within our date range, not just those within the bbox.  The reason for this is so that we can tell whether
 * a user has (a) edited near us, (b) edited elsewhere or (c) not edited at all.
 * ------------------------------------------------------------------------------------------------------------ */

							if ( arg_out_file != "" )
							{
								myPrintStream.println( user_node.getNodeValue() + ";" + uid_node.getNodeValue() + ";" + id_node.getNodeValue() + ";" + editor_name + ";" + editor_version + ";" + comment + ";Changeset: bbox overlaps" );
							}

							if ( passed_download.equals( "1") )
							{
								try
								{
									boolean changeset_node_interest_flag = false;
									
/* ------------------------------------------------------------------------------------------------------------
 * changeset_node_interest_flag is set based on seeing if any nodes in the changeset overlap our bounding box.
 * If there weren't any nodes in the changeset it won't get set and we may undercount  
 * "osm_changesets_of_interest" below, even though we've actually processed the changeset looking for unnamed
 * ways etc.
 * ------------------------------------------------------------------------------------------------------------ */
									changeset_node_interest_flag = download_changeset( id_node.getNodeValue(), 
											passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
											passed_building );
									
									if ( changeset_node_interest_flag )
									{
										osm_changesets_of_interest++;
									}
								}
								catch( Exception ex )
								{
									System.out.println( "Exception downloading changeset" );
								}
							}
							else
							{
/* ------------------------------------------------------------------------------
 * We're not downloading each changeset's XML so assume we are interested based
 * on the fact that the changeset bbox overlaps.
 * ------------------------------------------------------------------------------ */
								osm_changesets_of_interest++;
							}
						} // Overlap_Yes or Overlap_Error
						else
						{
							if ( changeset_bbox_interest_flag != Overlap_No )
							{
/* ------------------------------------------------------------------------------
 * We're not expecting anything other than Overlap_No by this stage, but write
 * out an error if we get anything unexpected.
 * ------------------------------------------------------------------------------ */
								if ( arg_out_file != "" )
								{
									myPrintStream.println( user_node.getNodeValue() + ";" + uid_node.getNodeValue() + ";" + id_node.getNodeValue() + ";" + editor_name + ";" + editor_version + ";" + comment + ";changeset_bbox_interest_flag: " + changeset_bbox_interest_flag );
								}
							}
						}
					}
					
				} // changeset
				else
				{ // !changeset
					if ( l1_item_type != "#text" )
					{
						if ( arg_debug >= Log_Informational_1 )
						{
							System.out.println( "Node " + cntr_1 + ": " + l1_item_type );
						}
					}
				} // !changeset
			} // for L1 nodes
	            
			if ( arg_debug >= Log_Informational_1 )
			{
				System.out.println( "Changesets found: " + osm_changesets_found + ", of interest: " + osm_changesets_of_interest );
			}

			if (( arg_out_file != ""              ) &&
				( osm_changesets_of_interest == 0 ))
			{
				myPrintStream.println( passed_display_name + ";" + passed_uid + ";;;;;" + osm_changesets_found + " changesets, none of interest" );
			}
		}	
		else
		{
			if ( arg_debug >= Log_Error )
			{
				System.out.println( "XML Parsing Error - element node expected" );
			}
		}
	}
	
	static void process_changesets_url_common ( URL passed_url, String passed_display_name, String passed_uid, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building ) throws Exception
	{
		if ( arg_debug >= Log_Informational_2 )
		{
			System.out.println( "Url: " + passed_url );
		}
		
		InputStreamReader input;
	
		URLConnection urlConn = passed_url.openConnection();
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
	    
	    process_changesets_xml( AJTrootElement, passed_display_name, passed_uid, 
	    		passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
	    		passed_download, passed_building );
	
	    input.close();
	}
	
	
	static void process_display_name_and_time( String passed_display_name, String passed_time, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_display_name_and_time" );
		}

		URL url;
		url = new URL( api_path + "changesets?display_name=" + ( URLEncoder.encode( passed_display_name , "UTF-8" )) + "&time=" + passed_time );
		
		process_changesets_url_common( url, passed_display_name, "", 
				passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
				passed_download, passed_building );
	}
	
	
	static void process_uid_and_time( String passed_uid, String passed_time, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_uid_and_time" );
		}

		URL url;
		url = new URL( api_path + "changesets?user=" + ( URLEncoder.encode( passed_uid , "UTF-8" )) + "&time=" + passed_time );
		process_changesets_url_common( url, "", passed_uid, passed_min_lat_string, 
				passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
				passed_download, passed_building );
	}
	
	
	static void process_display_name( String passed_display_name, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_display_name" );
		}

		URL url;
		url = new URL( api_path + "changesets?display_name=" + ( URLEncoder.encode( passed_display_name , "UTF-8" )));
		process_changesets_url_common( url, passed_display_name, "", 
				passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
				passed_download, passed_building );
	}
	
	
	static void process_uid( String passed_uid, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_uid" );
		}

		URL url;
		url = new URL( api_path + "changesets?user=" + ( URLEncoder.encode( passed_uid , "UTF-8" )));
		process_changesets_url_common( url, "", passed_uid, 
				passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
				passed_download, passed_building );
	}
	
	
	static void process_time( String passed_time, 
			String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, 
			String passed_download, String passed_building ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_time" );
		}

		URL url;
		url = new URL( api_path + "changesets?time=" + passed_time );
		process_changesets_url_common( url, "All Users", "", 
				passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, 
				passed_download, passed_building );
	}
	
	static String get_line_param( String passed_param, String passed_in_line )
	{
		int param_start = 0;
		int param_end = 0;
		String line_param = "";
		
		try
		{
			param_start = passed_in_line.indexOf( passed_param );
			
			if ( param_start != -1 )
			{
				if ( passed_in_line.substring(( param_start + passed_param.length() ), ( param_start + passed_param.length() + 1 )).equals( "\"" ))
				{
					param_start++;
					param_end = passed_in_line.indexOf( "\"", ( param_start + passed_param.length() ));
	
					if ( param_end == -1 )
					{
						param_end = passed_in_line.length() + 1;
					}
	
					line_param = passed_in_line.substring( ( param_start + passed_param.length() ), param_end );
				}
				else
				{
					param_end = passed_in_line.indexOf( " ", ( param_start + passed_param.length() ));
					
					if ( param_end == -1 )
					{
						param_end = passed_in_line.length();
					}
					
					line_param = passed_in_line.substring( ( param_start + passed_param.length() ), param_end );
				}
			}
		}
		catch( Exception ex )
		{
			System.out.println( "Error parsing param: " + passed_in_line );
		}
		
		return line_param;
	}
	
/* ------------------------------------------------------------------------------
 * Data passed on the command line:
 * 
 * param_input = "-input=";
 * param_output = "-output=";
 * param_display_name = "-display_name=";
 * param_uid = "-user=";
 * param_time = "-time=";
 * param_dev = "-dev";
 * param_debug = "-debug=";
 * param_bbox = "-bbox=";
 * param_download = "-download=";
 * param_building = "-building=";
 * 
 * ------------------------------------------------------------------------------ */
/**
 * @param args
 */
	public static void main(String[] args) throws Exception 
	{
		String arg_display_name = "";
		String arg_uid = "";
		String arg_time = "";
		
		for ( int i=0; i<args.length; i++ )
		{
			if ( args[i].length() >= 2)
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "arg: " + i );
					System.out.println( "arg length: " + args[i].length() );
				}
				
/* ------------------------------------------------------------------------------
 * Input file
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_input ))
				{	
					arg_in_file = args[i].substring( param_input.length() );

					try
					{
						myFileReader = new FileReader( arg_in_file );
						myBufferedReader = new BufferedReader( myFileReader );
					}
					catch( Exception ex )
					{
/* ------------------------------------------------------------------------------
 * If there's an error opening the input file, don't pretend that it wasn't 
 * specified on the command line.
 * ------------------------------------------------------------------------------ */
						arg_in_file = "!file";
						
						if ( arg_debug >= Log_Informational_1 )
						{
							System.out.println( "Error opening input file: " + ex.getMessage() );
						}
					}
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_in_file: " + arg_in_file );
						System.out.println( "arg_in_file length: " + arg_in_file.length() );
					}
				} // -input
				
/* ------------------------------------------------------------------------------
 * Output file
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_output ))
				{	
					arg_out_file = args[i].substring( param_output.length() );

					try
					{
						myOutputStream = new FileOutputStream( arg_out_file );
						myPrintStream = new PrintStream( myOutputStream );
					}
					catch( Exception ex )
					{
						arg_out_file = "";
						
						if ( arg_debug >= Log_Informational_1 )
						{
							System.out.println( "Error opening output file: " + ex.getMessage() );
						}
					}
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_out_file: " + arg_out_file );
						System.out.println( "arg_out_file length: " + arg_out_file.length() );
					}
				} // -output
				
/* ------------------------------------------------------------------------------
 * The user that we're interested in changesets for - display name
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_display_name ))
				{	
					arg_display_name = args[i].substring( param_display_name.length() );
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_display_name: " + arg_display_name );
						System.out.println( "arg_display_name length: " + arg_display_name.length() );
					}
				} // -display_name
				
/* ------------------------------------------------------------------------------
 * The user that we're interested in changesets for - userid
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_uid ))
				{	
					arg_uid = args[i].substring( param_uid.length() );
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_uid: " + arg_uid );
						System.out.println( "arg_uid length: " + arg_uid.length() );
					}
				} // -uid
				
/* ------------------------------------------------------------------------------
 * The time to start looking for changesets from 
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_time ))
				{	
					arg_time = args[i].substring( param_time.length() );
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_time: " + arg_time );
						System.out.println( "arg_time length: " + arg_time.length() );
					}
				} // -time
				
/* ------------------------------------------------------------------------------
 * Should we user the dev API?
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_dev ))
				{	
					api_path = dev_api_path;
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "Dev server will be used" );
					}
				} // -dev
				
/* ------------------------------------------------------------------------------
 * Should we download changesets that we are interested in?
 * 
 * We do this if we want to look for e.g. deleted relations.
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_download ))
				{	
					arg_download = args[i].substring( param_download.length() );
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_download: " + arg_download );
						System.out.println( "arg_download length: " + arg_download.length() );
					}
				} // -download
				
/* ------------------------------------------------------------------------------
 * After how many nodes should we warn about buildings?
 * 
 * A building with a very large number of nodes is likely to have been converted 
 * from landuse because of iD bug 542.
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_building ))
				{	
					arg_building = args[i].substring( param_building.length() );
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_building: " + arg_building );
						System.out.println( "arg_building length: " + arg_building.length() );
					}
				} // -building
				
/* ------------------------------------------------------------------------------
 * Debug level
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_debug ))
				{	
					try
					{
						arg_debug = Integer.valueOf( args[i].substring( param_debug.length() ));
					}
					catch( Exception ex )
					{
/* ------------------------------------------------------------------------------
 * Any failure above just means that we leave arg_debug at 0
 * ------------------------------------------------------------------------------ */
					}
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_debug: " + arg_debug );
					}
				} // -debug
				
/* ------------------------------------------------------------------------------
 * A bbox that we're interesting in comparing changesets with.
 * This isn't passed to the API; we compare changesets with it later.
 * ------------------------------------------------------------------------------ */
				if ( args[i].startsWith( param_bbox ))
				{	
					arg_bbox = args[i].substring( param_bbox.length() );
					
					if ( arg_debug >= Log_Informational_2 )
					{
						System.out.println( "arg_bbox: " + arg_bbox );
						System.out.println( "arg_bbox length: " + arg_bbox.length() );
					}
					
					int comma_pos = arg_bbox.indexOf( "," );
					int old_comma_pos = 0;
					
					if ( comma_pos > 0 )
					{ // found min lon
						arg_min_lon_string = arg_bbox.substring( 0, comma_pos );
						
						if ( arg_debug >= Log_Informational_1 )
						{
							System.out.println( "arg_min_lon: " + arg_min_lon_string );
						}

						
						old_comma_pos = comma_pos;
						comma_pos = arg_bbox.indexOf( ",", comma_pos+1 );

						if ( comma_pos > 0 )
						{ // found min lat
							arg_min_lat_string = arg_bbox.substring( old_comma_pos+1, comma_pos );
							
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "arg_min_lat: " + arg_min_lat_string );
							}

							
							old_comma_pos = comma_pos;
							comma_pos = arg_bbox.indexOf( ",", comma_pos+1 );

							if ( comma_pos > 0 )
							{ // found max lon; what's left must be max lat
								arg_max_lon_string = arg_bbox.substring( old_comma_pos+1, comma_pos );
								
								if ( arg_debug >= Log_Informational_1 )
								{
									System.out.println( "arg_max_lon: " + arg_max_lon_string );
								}


								old_comma_pos = comma_pos;
								arg_max_lat_string = arg_bbox.substring( old_comma_pos+1 );
								
								if ( arg_debug >= Log_Informational_1 )
								{
									System.out.println( "arg_max_lat: " + arg_max_lat_string );
								}
							} // max lon found
							else
							{
								if ( arg_debug >= Log_Error )
								{
									System.out.println( "3rd comma_pos: " + comma_pos );
									arg_bbox = "";
								}
							} // no max lon
						} // min lat found
						else
						{
							if ( arg_debug >= Log_Error )
							{
								System.out.println( "2nd comma_pos: " + comma_pos );
								arg_bbox = "";
							}
						} // no min lat
					} // min lon found
					else
					{
						if ( arg_debug >= Log_Error )
						{
							System.out.println( "1st comma_pos: " + comma_pos );
							arg_bbox = "";
						}
					} // no min lon
				} // -bbox
			} // potentially valid argument
		} // for each thing on the command line

		
/* ------------------------------------------------------------------------------
 * Actually do what we've been asked to do.
 * ------------------------------------------------------------------------------ */
		if ( arg_in_file.equals( "" ))
		{
 /* ------------------------------------------------------------------------------
 * If we're processing users and/or time we need one or both of those arguments.
 * ------------------------------------------------------------------------------ */
			if ( arg_display_name.length() == 0 )
			{
				if ( arg_uid.length() == 0 )
				{
					if ( arg_time.length() == 0 )
					{
						if ( arg_debug >= Log_Informational_2 )
						{
							System.out.println( "None of display_name, user or time passed" );
						}
					}
					else
					{
						process_time( arg_time, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download, arg_building );
					}
				}
				else
				{ // no display_name, but we do have a uid
					if ( arg_time.length() == 0 )
					{
						process_uid( arg_uid, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download, arg_building );
					} // no time argument passed
					else
					{
						process_uid_and_time( arg_uid, arg_time, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download, arg_building );
					}
				}
			} // no display_name argument passed
			else
			{ // display_name passed.  We'll not bother checking for a uid.
				if ( arg_time.length() == 0 )
				{
					process_display_name( arg_display_name, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download, arg_building );
				} // no time argument passed
				else
				{
					process_display_name_and_time( arg_display_name, arg_time, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download, arg_building );
				}
			} // user argument passed
		} // no "in" file
		else
		{
			if ( arg_in_file.equals( "!file" ))
			{
				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Input file could not be opened" );
				}
			}
			else
			{
/* ------------------------------------------------------------------------------
 * We do have an input file defined and we have been able to open it.
 * ------------------------------------------------------------------------------ */

				if ( arg_debug >= Log_Informational_2 )
				{
					System.out.println( "Input file: " + arg_in_file );
				}

				String in_line = "";
				String line_display_name = "";
				String line_uid = "";
				String line_time = "";
				String line_bbox = "";
				String line_min_lat_string = "";
				String line_min_lon_string = "";
				String line_max_lat_string = "";
				String line_max_lon_string = "";
				String line_download = "";
				String line_building = "";
				
				while(( in_line = myBufferedReader.readLine() ) != null )
				{
/* ------------------------------------------------------------------------------
 * The "line_" values default to "".  If any of these values aren't set from
 * the line, set to the comment line "arg_" values. 
 * ------------------------------------------------------------------------------ */
					line_display_name = get_line_param( param_display_name, in_line );
					
					if ( line_display_name.equals( "" ))
					{
						line_display_name = arg_display_name;
					}
					
					line_uid          = get_line_param( param_uid, in_line );
					
					if ( line_uid.equals( "" ))
					{
						line_uid = arg_uid;
					}
					
					line_time         = get_line_param( param_time, in_line );
					
					if ( line_time.equals( "" ))
					{
						line_time = arg_time;
					}
					
					line_bbox         = get_line_param( param_bbox, in_line );
					
					if ( line_bbox.equals( "" ))
					{
						line_bbox = arg_bbox;
					}
					
					line_download     = get_line_param( param_download, in_line );
					
					if ( line_download.equals( "" ))
					{
						line_download = arg_download;
					}
					
					line_building     = get_line_param( param_building, in_line );

					if ( line_building.equals( "" ))
					{
						line_building = arg_building;
					}
					
					int comma_pos = line_bbox.indexOf( "," );
					int old_comma_pos = 0;
					
					if ( comma_pos > 0 )
					{ // found min lon
						line_min_lon_string = line_bbox.substring( 0, comma_pos );
						
						if ( arg_debug >= Log_Informational_1 )
						{
							System.out.println( "line_min_lon: " + line_min_lon_string );
						}

						old_comma_pos = comma_pos;
						comma_pos = line_bbox.indexOf( ",", comma_pos+1 );

						if ( comma_pos > 0 )
						{ // found min lat
							line_min_lat_string = line_bbox.substring( old_comma_pos+1, comma_pos );
							
							if ( arg_debug >= Log_Informational_1 )
							{
								System.out.println( "line_min_lat: " + line_min_lat_string );
							}

							
							old_comma_pos = comma_pos;
							comma_pos = line_bbox.indexOf( ",", comma_pos+1 );

							if ( comma_pos > 0 )
							{ // found max lon; what's left must be max lat
								line_max_lon_string = line_bbox.substring( old_comma_pos+1, comma_pos );
								
								if ( arg_debug >= Log_Informational_1 )
								{
									System.out.println( "line_max_lon: " + line_max_lon_string );
								}


								old_comma_pos = comma_pos;
								line_max_lat_string = line_bbox.substring( old_comma_pos+1 );
								
								if ( arg_debug >= Log_Informational_1 )
								{
									System.out.println( "line_max_lat: " + line_max_lat_string );
								}
							} // max lon found
							else
							{
								if ( arg_debug >= Log_Error )
								{
									System.out.println( "3rd comma_pos: " + comma_pos );
									line_bbox = "";
								}
							} // no max lon
						} // min lat found
						else
						{
							if ( arg_debug >= Log_Error )
							{
								System.out.println( "2nd comma_pos: " + comma_pos );
								line_bbox = "";
							}
						} // no min lat
					} // min lon found
					else
					{
						if ( arg_debug >= Log_Error )
						{
							System.out.println( "1st comma_pos: " + comma_pos );
							line_bbox = "";
						}
					} // no min lon

/* ------------------------------------------------------------------------------
 * Check that the user has entered something on a line - otherwise a blank line
 * will just try and download "all changesets"
 * ------------------------------------------------------------------------------ */
					if (( line_display_name.length() != 0 ) ||
					    ( line_uid.length()          != 0 ) ||
					    ( line_time.length()         != 0 ) ||
					    ( line_bbox.length()         != 0 ))
					{
						if ( line_display_name.length() == 0 )
						{
							line_display_name = arg_display_name;
						}
						
						if ( line_uid.length() == 0 )
						{
							line_uid = arg_uid;
						}
						
						if ( line_time.length() == 0 )
						{
							line_time = arg_time;
						}
	
						if ( line_bbox.length() == 0 )
						{
							line_bbox = arg_bbox;
							line_min_lat_string = arg_min_lat_string; 
							line_min_lon_string = arg_min_lon_string;
							line_max_lat_string = arg_max_lat_string;
							line_max_lon_string = arg_max_lon_string;
						}
	
						if ( line_download.length() == 0 )
						{
							line_download = arg_download;
						}
	
/* ------------------------------------------------------------------------------------------------------------
 * Now call the API with whatever parameters we have from this line in the input file or the command line 
 * ------------------------------------------------------------------------------------------------------------ */
						if ( line_time.length() == 0 )
						{
							if ( line_display_name.length() == 0 )
							{
								if ( line_uid.length() == 0 )
								{
									if ( arg_debug >= Log_Informational_2 )
									{
										System.out.println( "None of display_name, uid or time passed on this line" );
									}
								}
								else
								{
									process_uid( line_uid, 
											line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, 
											line_download, line_building );
								}
							}
							else
							{
								process_display_name( line_display_name, 
										line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, 
										line_download, line_building );
							}
						} // no time argument passed
						else
						{
							if ( line_display_name.length() == 0 )
							{
								if ( line_uid.length() == 0 )
								{
									process_time( line_time, 
											line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, 
											line_download, line_building );
								}
								else
								{
									process_uid_and_time( line_uid, line_time, 
											line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, 
											line_download, line_building );
								}
							}
							else
							{
								process_display_name_and_time( line_display_name, line_time, 
										line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, 
										line_download, line_building );
							}
						}
					}
				}
			}
		}
		

		
/* ------------------------------------------------------------------------------
 * If we've been writing to an output file, close it.
 * ------------------------------------------------------------------------------ */
		if ( !arg_out_file.equals( "" ))
		{
			myOutputStream.close();
		}
	} // main
}
