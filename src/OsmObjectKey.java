/**
 * qqq
 */

/**
 * @author A.Townsend
 *
 */
public class OsmObjectKey 
{

	final static byte Item_Unknown = 0;
	final static byte Item_Node = 1;
	final static byte Item_Way = 2;
	final static byte Item_Relation = 3;

	private byte  item_type;
	private String item_id;

	/**
	 * qqq
	 */
	public OsmObjectKey( byte passed_item_type, String passed_item_id ) 
	{
		item_type = passed_item_type;
		item_id = passed_item_id;
	}

	byte get_item_type()
	{
		return item_type;
	}

	void set_item_type( byte passed_item_type )
	{
		item_type = passed_item_type;
	}

	String get_item_id()
	{
		return item_id;
	}

	void set_item_id( String passed_item_id )
	{
		item_id = passed_item_id;
	}

}
