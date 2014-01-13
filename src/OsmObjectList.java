import java.util.ArrayList;
import java.util.Hashtable;
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
	private Hashtable<OsmObjectKey, OsmObjectDetails> osmObjectHash;
	
	/**
	 * 
	 */
	public OsmObjectList() 
	{
		osmObjectList = new ArrayList<OsmObjectKey>();
		osmObjectHash = new Hashtable<OsmObjectKey, OsmObjectDetails>();
	}

	public void add( OsmObjectKey passed_osmObjectKey, OsmObjectDetails passed_osmObjectDetails )
	{
		osmObjectList.add( passed_osmObjectKey );
		osmObjectHash.put( passed_osmObjectKey, passed_osmObjectDetails );
	}
	
	public void addOrUpdate( OsmObjectKey passed_osmObjectKey, OsmObjectDetails passed_osmObjectDetails )
	{
		if( osmObjectList.indexOf( passed_osmObjectKey ) == -1 )
		{
			osmObjectList.add( passed_osmObjectKey );
			osmObjectHash.put( passed_osmObjectKey, passed_osmObjectDetails );
		}
		else
		{
			osmObjectHash.put( passed_osmObjectKey, passed_osmObjectDetails );
		}
	}
	
	public int size()
	{
		return osmObjectList.size();
	}

	public OsmObjectInfo get( int i )
	{
		OsmObjectKey osmObjectKey = osmObjectList.get( i );
		OsmObjectInfo osmObjectInfo = new OsmObjectInfo( osmObjectKey, osmObjectHash.get( osmObjectKey ));
		return osmObjectInfo;
	}
}
