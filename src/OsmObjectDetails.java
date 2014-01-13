
public class OsmObjectDetails 
{
	final static byte Action_Unknown = 0;
	final static byte Action_Create = 1;
	final static byte Action_Modify = 2;
	final static byte Action_Delete = 3;

	private String item_user;
	private String item_uid;
	private String node_name;
	private boolean building_or_shop_found;
	private boolean overlaps_bbox;
	private int number_of_children;
	private int number_of_tags;
	private byte last_action;

	public OsmObjectDetails( String passed_item_user, String passed_item_uid, String passed_node_name, 
			boolean passed_building_or_shop_found, boolean passed_overlaps_bbox, 
			int passed_number_of_children, int passed_number_of_tags, byte passed_last_action ) 
	{
		item_user = passed_item_user;
		item_uid = passed_item_uid;
		node_name = passed_node_name;
		building_or_shop_found = passed_building_or_shop_found;
		overlaps_bbox = passed_overlaps_bbox;
		number_of_children = passed_number_of_children;
		number_of_tags = passed_number_of_tags;
		last_action = passed_last_action;
	}

	String get_item_user()
	{
		return item_user;
	}

	void set_item_user( String passed_item_user )
	{
		item_user = passed_item_user;
	}

	String get_item_uid()
	{
		return item_uid;
	}

	void set_item_uid( String passed_item_uid )
	{
		item_uid = passed_item_uid;
	}

	String get_node_name()
	{
		return node_name;
	}

	void set_node_name( String passed_node_name )
	{
		node_name = passed_node_name;
	}

	boolean get_building_or_shop_found()
	{
		return building_or_shop_found;
	}

	void set_building_or_shop_found( boolean passed_building_or_shop_found )
	{
		building_or_shop_found = passed_building_or_shop_found;
	}

	boolean get_overlaps_bbox()
	{
		return overlaps_bbox;
	}

	void set_overlaps_bbox( boolean passed_overlaps_bbox )
	{
		overlaps_bbox = passed_overlaps_bbox;
	}

	int get_number_of_children()
	{
		return number_of_children;
	}

	void inc_number_of_children()
	{
		number_of_children++;
	}

/* ------------------------------------------------------------------------------
 * There's no set method for number_of_children as we only ever expect to
 * increment it. If there was, here's what it would look like:
 * ------------------------------------------------------------------------------ */
//	void set_number_of_children( int passed_number_of_children )
//	{
//		number_of_children = passed_number_of_children;
//	}

	int get_number_of_tags()
	{
		return number_of_tags;
	}

	void inc_number_of_tags()
	{
		number_of_tags++;
	}

/* ------------------------------------------------------------------------------
 * There's no set method for number_of_tags as we only ever expect to
 * increment it.  If there was, here's what it would look like:
 * ------------------------------------------------------------------------------ */
//	void set_number_of_tags( int passed_number_of_tags )
//	{
//		number_of_tags = passed_number_of_tags;
//	}

	byte get_last_action()
	{
		return last_action;
	}

	void set_last_action( byte passed_last_action )
	{
		last_action = passed_last_action;
	}


}
