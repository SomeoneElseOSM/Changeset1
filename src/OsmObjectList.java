import java.util.ArrayList;
import java.util.List;

/**
 * 
 */

/**
 * @author A.Townsend
 *
 */
public class OsmObjectList 
{

	private List<OsmObjectKey> osmObjectList;
	
	/**
	 * 
	 */
	public OsmObjectList() 
	{
		List<OsmObjectKey> osmObjectList = new ArrayList<OsmObjectKey>();
	}

	public void osmObjectListAdd( OsmObjectKey passed_osmObjectKey )
	{
		osmObjectList.add( passed_osmObjectKey );
		//qqq and store in the hashmap as well
	}
	
	public void osmObjectListAddOrUpdate( OsmObjectKey passed_osmObjectKey )
	{
		if( osmObjectList.indexOf( passed_osmObjectKey ) == -1 )
		{
			osmObjectList.add( passed_osmObjectKey );
			//qqq and store in the hashmap as well
		}
		else
		{
			osmObjectList.set( osmObjectList.indexOf( passed_osmObjectKey ), passed_osmObjectKey );
			//qqq and update the details in the hashmap
		}
	}
}
