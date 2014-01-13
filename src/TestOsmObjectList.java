import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 */

/**
 * @author A.Townsend
 *
 */
public class TestOsmObjectList 
{
	final static byte Item_Unknown = 0;
	final static byte Item_Node = 1;
	final static byte Item_Way = 2;
	final static byte Item_Relation = 3;

	final static byte Action_Unknown = 0;
	final static byte Action_Create = 1;
	final static byte Action_Modify = 2;
	final static byte Action_Delete = 3;

	@Test
	public void testKeyEquals() 
	{
		OsmObjectKey osmObjectKey1 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectKey osmObjectKey2 = new OsmObjectKey( Item_Node, "1" );

		assertTrue( osmObjectKey1.equals( osmObjectKey2 ) );
	}
	@Test
	public void testKeyNEquals1() 
	{
		OsmObjectKey osmObjectKey1 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectKey osmObjectKey2 = new OsmObjectKey( Item_Node, "2" );

		assertFalse( osmObjectKey1.equals( osmObjectKey2 ) );
	}
	@Test
	public void testKeyNEquals2() 
	{
		OsmObjectKey osmObjectKey1 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectKey osmObjectKey2 = new OsmObjectKey( Item_Way, "1" );

		assertFalse( osmObjectKey1.equals( osmObjectKey2 ) );
	}
	
	@Test
	public void testListInit() 
	{
		OsmObjectList osmObjectList = new OsmObjectList();
		assertTrue( osmObjectList.size() == 0 );
	}
	
	@Test
	public void test_add1() 
	{
		OsmObjectList osmObjectList = new OsmObjectList();
		
		OsmObjectKey osmObjectKey = new OsmObjectKey( Item_Node, "1" );
		OsmObjectDetails osmObjectDetails = new OsmObjectDetails( "user", "uid", "name", false, false,  0, 0, Action_Unknown );
		OsmObjectInfo osmObjectInfo = new OsmObjectInfo( osmObjectKey, osmObjectDetails );
		
		osmObjectList.add( osmObjectInfo.get_osmObjectKey(), osmObjectInfo.get_osmObjectDetails() );
		assertTrue( osmObjectList.size() == 1 );
	}

	@Test
	public void test_addandfind1() 
	{
		OsmObjectList osmObjectList = new OsmObjectList();
		
		OsmObjectKey osmObjectKey = new OsmObjectKey( Item_Node, "1" );
		OsmObjectDetails osmObjectDetails = new OsmObjectDetails( "user", "uid", "name", false, false,  0, 0, Action_Unknown );
		OsmObjectInfo osmObjectInfo1 = new OsmObjectInfo( osmObjectKey, osmObjectDetails );
		
		osmObjectList.add( osmObjectInfo1.get_osmObjectKey(), osmObjectInfo1.get_osmObjectDetails() );

		OsmObjectInfo osmObjectInfo2 = osmObjectList.get(0);
		
		assertTrue( osmObjectInfo2.get_osmObjectKey().get_item_type() == Item_Node );
		assertTrue( osmObjectInfo2.get_osmObjectKey().get_item_id().equals( "1" ));
		
		assertTrue( osmObjectInfo2.get_osmObjectDetails().get_item_user().equals( "user" ));
		assertTrue( osmObjectInfo2.get_osmObjectDetails().get_node_name().equals( "name" ));
		assertTrue( osmObjectInfo2.get_osmObjectDetails().get_building_or_shop_found() == false );
		assertTrue( osmObjectInfo2.get_osmObjectDetails().get_overlaps_bbox() == false );
		assertTrue( osmObjectInfo2.get_osmObjectDetails().get_number_of_children() == 0 );
		assertTrue( osmObjectInfo2.get_osmObjectDetails().get_number_of_tags() == 0 );

	}

	@Test
	public void test_addandupdate1() 
	{
		OsmObjectList osmObjectList = new OsmObjectList();
		
		OsmObjectKey osmObjectKey1 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectDetails osmObjectDetails1 = new OsmObjectDetails( "user1", "uid1", "name1", false, false,  0, 0, Action_Create );
		OsmObjectInfo osmObjectInfo1 = new OsmObjectInfo( osmObjectKey1, osmObjectDetails1 );

		OsmObjectKey osmObjectKey2 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectDetails osmObjectDetails2 = new OsmObjectDetails( "user2", "uid2", "name2", false, false,  0, 0, Action_Modify );
		OsmObjectInfo osmObjectInfo2 = new OsmObjectInfo( osmObjectKey2, osmObjectDetails2 );

		osmObjectList.add( osmObjectInfo1.get_osmObjectKey(), osmObjectInfo1.get_osmObjectDetails() );
		osmObjectList.addOrUpdate( osmObjectInfo2.get_osmObjectKey(), osmObjectInfo2.get_osmObjectDetails() );
		
		assertTrue( osmObjectList.size() == 1 );

	}
	
	@Test
	public void test_addandupdate2() 
	{
		OsmObjectList osmObjectList = new OsmObjectList();
		
		OsmObjectKey osmObjectKey1 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectDetails osmObjectDetails1 = new OsmObjectDetails( "user1", "uid1", "name1", false, false, 0, 0, Action_Create );
		OsmObjectInfo osmObjectInfo1 = new OsmObjectInfo( osmObjectKey1, osmObjectDetails1 );

		OsmObjectKey osmObjectKey2 = new OsmObjectKey( Item_Node, "1" );
		OsmObjectDetails osmObjectDetails2 = new OsmObjectDetails( "user2", "uid2", "name2", true, true, 1, 1, Action_Modify );
		OsmObjectInfo osmObjectInfo2 = new OsmObjectInfo( osmObjectKey2, osmObjectDetails2 );

		osmObjectList.add( osmObjectInfo1.get_osmObjectKey(), osmObjectInfo1.get_osmObjectDetails() );
		osmObjectList.addOrUpdate( osmObjectInfo2.get_osmObjectKey(), osmObjectInfo2.get_osmObjectDetails() );
		
		OsmObjectInfo osmObjectInfo3 = osmObjectList.get(0);
		
		assertTrue( osmObjectInfo3.get_osmObjectKey().get_item_type() == Item_Node );
		assertTrue( osmObjectInfo3.get_osmObjectKey().get_item_id().equals( "1" ));
		
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_item_user().equals( "user2" ));
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_node_name().equals( "name2" ));
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_building_or_shop_found() == true );
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_overlaps_bbox() == true );
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_number_of_children() == 1 );
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_number_of_tags() == 1 );
		assertTrue( osmObjectInfo3.get_osmObjectDetails().get_last_action() == Action_Modify );

	}
}
