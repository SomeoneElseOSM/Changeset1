import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// See also https://github.com/SomeoneElseOSM/Changeset1

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
	


	private static int check_interest( Node root_node, NamedNodeMap item_attributes, Node id_node, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string )
	{
		boolean it_overlaps = true;
		int return_value = 0;
		Double passed_min_lat;
		Double passed_min_lon;
		Double passed_max_lat;
		Double passed_max_lon;
		
		if ( passed_min_lat_string.length() == 0 )
		{
			if ( arg_debug >= Log_Informational_2 )
			{
				System.out.println( "We're interested in all changesets" );
			}

			return_value = Overlap_All;
		}
		else
		{
			Double min_lat;
			Double min_lon;
			Double max_lat;
			Double max_lon;
			
			try
			{
				min_lat = Double.valueOf( item_attributes.getNamedItem( "min_lat" ).getNodeValue() );
				min_lon = Double.valueOf( item_attributes.getNamedItem( "min_lon" ).getNodeValue() );
				max_lat = Double.valueOf( item_attributes.getNamedItem( "max_lat" ).getNodeValue() );
				max_lon = Double.valueOf( item_attributes.getNamedItem( "max_lon" ).getNodeValue() );
				
				passed_min_lat = Double.valueOf( passed_min_lat_string ); 
				passed_min_lon = Double.valueOf( passed_min_lon_string ); 
				passed_max_lat = Double.valueOf( passed_max_lat_string ); 
				passed_max_lon = Double.valueOf( passed_max_lon_string ); 

				it_overlaps = check_overlap( passed_min_lon, passed_min_lat, passed_max_lat, passed_max_lon,
			                                 min_lon,     min_lat,     max_lat,     max_lon );

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
					System.out.println( "Error parsing lat or lon from changeset: " + ex.getMessage() );
				}
			}
		}
		
		return return_value;
	}

/* ------------------------------------------------------------------------------------------------------------
 * The node passed in here is the root node of the XML tree of the download of this changeset
 * The other parameters are used for comparisons and reporting.
 * The return value is "does this changeset have nodes within our area of interest".
 * ------------------------------------------------------------------------------------------------------------ */
	private static boolean process_download_xml( Node root_node, String passed_changeset_number, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string )
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
								if ( this_l2_item.hasAttributes() )
								{
									NamedNodeMap node_attributes = this_l2_item.getAttributes();
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
										
										if ( arg_debug >= Log_Informational_2 )
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
											
											if ( arg_debug >= Log_Informational_2 )
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
											
											if ( arg_debug >= Log_Informational_2 )
											{
												System.out.println( "uid: " + uid_node.getNodeValue() );
											}
										}

										if ( lat_node == null )
										{
/* ------------------------------------------------------------------------------------------------------------
 * Possibly a deletion - the lat and lon of deleted nodes are not returned in the OSC
 * ------------------------------------------------------------------------------------------------------------ */
											if ( arg_debug >= Log_Informational_2 )
											{
												System.out.println( "No lat for id: " + id_node.getNodeValue() );
											}
										}
										else
										{
											if ( lon_node == null )
											{
												if ( arg_debug >= Log_Informational_2 )
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
														Double passed_min_lat;
														Double passed_min_lon;
														Double passed_max_lat;
														Double passed_max_lon;
		
														Double lat;
														Double lon;
		
														passed_min_lat = Double.valueOf( passed_min_lat_string ); 
														passed_min_lon = Double.valueOf( passed_min_lon_string ); 
														passed_max_lat = Double.valueOf( passed_max_lat_string ); 
														passed_max_lon = Double.valueOf( passed_max_lon_string ); 
		
														lat = Double.valueOf( lat_node.getNodeValue() );
														lon = Double.valueOf( lon_node.getNodeValue() );
														
														if (( lat > passed_min_lat ) &&
															( lat < passed_max_lat ) &&
															( lon > passed_min_lon ) &&
															( lon < passed_max_lon ))
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
													if ( arg_debug >= Log_Informational_2 )
													{
														System.out.println( "Node lat/lon processing - we are missing a lat or long, probably because we're interested in all changesets" );
													}
/* ------------------------------------------------------------------------------------------------------------
 * We don't set "node_overlaps = true;" here because there's no need to list every node in a changeset - there
 * are other ways to get that.
 * ------------------------------------------------------------------------------------------------------------ */
												}

// any other processing of the attributes of id, lat and lon of our created, modified or deleted node would go here.
												
											} // we have a lon
										} // we have a lat
										
// any other attribute processing that doesn't need lat or lon could go here.
										
									} // id node not null
								} // node attributes
							} //node
							else
							{
								if (( l2_item_type.equals( "way"      )) ||
								    ( l2_item_type.equals( "relation" )))
								{

									if ( this_l2_item.hasAttributes() )
									{
										NamedNodeMap node_attributes = this_l2_item.getAttributes();
										Node id_node = node_attributes.getNamedItem( "id" );
										Node user_node = node_attributes.getNamedItem( "user" );
										Node uid_node = node_attributes.getNamedItem( "uid" );

										if ( id_node == null )
										{
											System.out.println( "Download way/relation processing: No id found" );
										}
										else
										{
											if ( arg_debug >= Log_Informational_2 )
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
											if ( arg_out_file != ""  )
											{
												if ( l1_item_type.equals( "delete"   ))
												{
													if ( l2_item_type.equals( "way" ))
													{
														myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Way " + id_node.getNodeValue() + " deleted" );
													}

													if ( l2_item_type.equals( "relation" ))
													{
														myPrintStream.println( item_user + ";" + item_uid + ";" + passed_changeset_number + ";;;;Relation " + id_node.getNodeValue() + " deleted" );
													}
												}
											}

// other processing of the attributes of our created, modified or deleted way/relation would go here.


										} // id node not null
									} // way / relation attributes
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
 * Depending on l2_item_type, we're expecting "nd", "member" or "tag" here.
 * ------------------------------------------------------------------------------------------------------------ */
								if ( !l3_item_type.equals( "#text" ))
								{
									if ( arg_debug >= Log_Informational_2 )
									{
										System.out.println( "Found 3: " + l3_item_type );
									}
									
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
														
// here would go any other processing of the tag / value from the OSC file
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
	
	private static boolean download_changeset( String passed_number, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string ) throws Exception
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
	    return_value = process_download_xml( AJTrootElement, passed_number, passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string );
	
	    input.close();
	    return return_value;
	}
	
	
/* ------------------------------------------------------------------------------------------------------------
 * The node passed in here is the root node of the XML tree of the changesets returned in response to our query
 * ------------------------------------------------------------------------------------------------------------ */
	private static void process_changesets_xml( Node root_node, String passed_display_name, String passed_uid, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download )
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
					int our_interest = 0;
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

						our_interest = check_interest( root_node, item_attributes, id_node, passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string );
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
					if ( our_interest == Overlap_All )
					{
/* ------------------------------------------------------------------------------------------------------------
 * We're interested in all changsets.  Although download_changeset will return true if a node within the 
 * changset has a lat/lon within the lat/lon ranges that we are interested in, we don't care.  
 * ------------------------------------------------------------------------------------------------------------ */
						osm_changesets_of_interest++;

						if ( arg_out_file != "" )
						{
							myPrintStream.println( user_node.getNodeValue() + ";" + uid_node.getNodeValue() + ";" + id_node.getNodeValue() + ";" + editor_name + ";" + editor_version + ";" + comment + ";Changeset: all" );
						}

						if ( passed_download.equals( "1") )
						{
							try
							{
								download_changeset( id_node.getNodeValue(), passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string );
							}
							catch( Exception ex )
							{
								System.out.println( "Exception downloading changset" );
							}
						}
					}
					else
					{
						if ( our_interest == Overlap_Yes )
						{
/* ------------------------------------------------------------------------------------------------------------
 * We're interested in overlapping changsets.  If we're downloading it well set the "interested" counter based
 * on whether nodes in the changeset are in our area of interest.  If not, we'll use the changset bbox (which
 * we already know because our_interest is set to Overlap_Yes.
 * 
 * Note that the "bbox" parameter wasn't passed to the API so we're reading through all changesets for a user
 * within our date range, not just those within the bbox. 
 * ------------------------------------------------------------------------------------------------------------ */

							if ( arg_out_file != "" )
							{
								myPrintStream.println( user_node.getNodeValue() + ";" + uid_node.getNodeValue() + ";" + id_node.getNodeValue() + ";" + editor_name + ";" + editor_version + ";" + comment + ";Changeset: bbox overlaps" );
							}

							if ( passed_download.equals( "1") )
							{
								try
								{
									boolean were_interested = false;
									were_interested = download_changeset( id_node.getNodeValue(), passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string );
									
									if ( were_interested )
									{
										osm_changesets_of_interest++;
									}
								}
								catch( Exception ex )
								{
									System.out.println( "Exception downloading changset" );
								}
							}
							else
							{
								osm_changesets_of_interest++;
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
	
	static void process_changesets_url_common ( URL passed_url, String passed_display_name, String passed_uid, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download ) throws Exception
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
	    process_changesets_xml( AJTrootElement, passed_display_name, passed_uid, passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_download );
	
	    input.close();
	}
	
	
	static void process_display_name_and_time( String passed_display_name, String passed_time, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_display_name_and_time" );
		}

		URL url;
		url = new URL( api_path + "changesets?display_name=" + ( URLEncoder.encode( passed_display_name , "UTF-8" )) + "&time=" + passed_time );
		process_changesets_url_common( url, passed_display_name, "", passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_download );
	}
	
	
	static void process_uid_and_time( String passed_uid, String passed_time, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_uid_and_time" );
		}

		URL url;
		url = new URL( api_path + "changesets?user=" + ( URLEncoder.encode( passed_uid , "UTF-8" )) + "&time=" + passed_time );
		process_changesets_url_common( url, "", passed_uid, passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_download );
	}
	
	
	static void process_display_name( String passed_display_name, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_display_name" );
		}

		URL url;
		url = new URL( api_path + "changesets?display_name=" + ( URLEncoder.encode( passed_display_name , "UTF-8" )));
		process_changesets_url_common( url, passed_display_name, "", passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_download );
	}
	
	
	static void process_uid( String passed_uid, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_uid" );
		}

		URL url;
		url = new URL( api_path + "changesets?user=" + ( URLEncoder.encode( passed_uid , "UTF-8" )));
		process_changesets_url_common( url, "", passed_uid, passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_download );
	}
	
	
	static void process_time( String passed_time, String passed_min_lat_string, String passed_min_lon_string, String passed_max_lat_string, String passed_max_lon_string, String passed_download ) throws Exception 
	{
		if ( arg_debug >= Log_Low_Routine_Start )
		{
			System.out.println( "process_time" );
		}

		URL url;
		url = new URL( api_path + "changesets?time=" + passed_time );
		process_changesets_url_common( url, "All Users", "", passed_min_lat_string, passed_min_lon_string, passed_max_lat_string, passed_max_lon_string, passed_download );
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
 * Should we download changsets that we are interested in?
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
				} // -dev
				
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
						process_time( arg_time, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download );
					}
				}
				else
				{ // no display_name, but we do have a uid
					if ( arg_time.length() == 0 )
					{
						process_uid( arg_uid, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download );
					} // no time argument passed
					else
					{
						process_uid_and_time( arg_uid, arg_time, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download );
					}
				}
			} // no display_name argument passed
			else
			{ // display_name passed.  We'll not bother checking for a uid.
				if ( arg_time.length() == 0 )
				{
					process_display_name( arg_display_name, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download );
				} // no time argument passed
				else
				{
					process_display_name_and_time( arg_display_name, arg_time, arg_min_lat_string, arg_min_lon_string, arg_max_lat_string, arg_max_lon_string, arg_download );
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
				
				while(( in_line = myBufferedReader.readLine() ) != null )
				{
					line_display_name = get_line_param( param_display_name, in_line );
					line_uid          = get_line_param( param_uid, in_line );
					line_time         = get_line_param( param_time, in_line );
					line_bbox         = get_line_param( param_bbox, in_line );
					line_download     = get_line_param( param_download, in_line );

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
									process_uid( line_uid, line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, line_download );
								}
							}
							else
							{
								process_display_name( line_display_name, line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, line_download );
							}
						} // no time argument passed
						else
						{
							if ( line_display_name.length() == 0 )
							{
								if ( line_uid.length() == 0 )
								{
									process_time( line_time, line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, line_download );
								}
								else
								{
									process_uid_and_time( line_uid, line_time, line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, line_download );
								}
							}
							else
							{
								process_display_name_and_time( line_display_name, line_time, line_min_lat_string, line_min_lon_string, line_max_lat_string, line_max_lon_string, line_download );
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