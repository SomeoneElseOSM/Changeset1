/**
 * qqq03
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
	 * qqq03
	 */
	public OsmObjectKey( byte passed_item_type, String passed_item_id ) 
	{
		item_type = passed_item_type;
		item_id = passed_item_id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item_id == null) ? 0 : item_id.hashCode());
		result = prime * result + item_type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OsmObjectKey other = (OsmObjectKey) obj;
		if (item_id == null) {
			if (other.item_id != null)
				return false;
		} else if (!item_id.equals(other.item_id))
			return false;
		if (item_type != other.item_type)
			return false;
		return true;
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
