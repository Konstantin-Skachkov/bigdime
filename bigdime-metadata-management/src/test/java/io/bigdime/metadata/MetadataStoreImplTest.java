/**
 * Copyright (C) 2015 Stubhub.
 */
package io.bigdime.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.bigdime.adaptor.metadata.MetadataAccessException;
import io.bigdime.adaptor.metadata.MetadataStore;
import io.bigdime.adaptor.metadata.ObjectEntityMapper;
import io.bigdime.adaptor.metadata.dto.EntiteeDTO;
import io.bigdime.adaptor.metadata.dto.MetasegmentDTO;
import io.bigdime.adaptor.metadata.impl.MetadataRepositoryService;
import io.bigdime.adaptor.metadata.impl.MetadataStoreImpl;
import io.bigdime.adaptor.metadata.model.Entitee;
import io.bigdime.adaptor.metadata.model.Metasegment;
import io.bigdime.adaptor.metadata.repositories.MetadataRepository;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.*;

/**
 * Class MetadataStoreImplUnitTest
 * 
 * @author Neeraj Jain, psabinikari
 * 
 */
public class MetadataStoreImplTest {

	MetadataStore metaStore;

	MetadataStoreImpl metaStoreImpl;

	@Mock
	MetadataStore mockMetadataStore;

	@Mock
	MetadataRepository repository;

	@Mock
	Metasegment mockMetasegment;

	@Mock
	MetasegmentDTO mockMetasegmentDTO;

	@Mock
	Metasegment metadata;

	@Mock
	Entitee mockEntity;

	@Mock
	EntiteeDTO mockEntityDTO;

	@Mock
	Set<Entitee> entitiesSet;

	@Mock
	Set<EntiteeDTO> entitiesSetDTO;
	@Mock
	Iterator<Entitee> entityIterator;

	@Mock
	Set<String> stringSet;

	@Mock
	MetadataRepositoryService mockMetadataRepositoryService;

	@Mock
	Map<String, Metasegment> mockCachedSchemaDetails;

	@Mock
	ObjectEntityMapper mockObjectEntityMapper;

	@Mock
	Map<String, Set<String>> mockAdaptorEntities;

	@Mock
	List<MetasegmentDTO> mockMetasegmentDTOList;

	@Mock
	List<Metasegment> mockMetasegmentList;

	/**
	 * Load the data for test
	 * 
	 * @throws Exception
	 */

	@BeforeTest
	public void init() {

		initMocks(this);
		metaStore = new MetadataStoreImpl();
		metaStoreImpl = new MetadataStoreImpl();

		ReflectionTestUtils.setField(metaStore, "repoService",
				mockMetadataRepositoryService);
		ReflectionTestUtils.setField(metaStore, "objectEntityMapper",
				mockObjectEntityMapper);
		ReflectionTestUtils.setField(metaStore, "cachedSchemaDetails",
				mockCachedSchemaDetails);
		ReflectionTestUtils.setField(metaStore, "adaptorEntities",
				mockAdaptorEntities);
		ReflectionTestUtils.setField(metaStoreImpl, "repoService",
				mockMetadataRepositoryService);
		ReflectionTestUtils.setField(metaStoreImpl, "adaptorEntities",
				mockAdaptorEntities);

		Mockito.when(mockMetasegment.getAdaptorName()).thenReturn(
				"TestApplicationName");
		Mockito.when(mockMetasegment.getSchemaType()).thenReturn(
				"TestSchemaType");
		Mockito.when(mockEntity.getEntityName()).thenReturn("testEntityName");
	}

	@Test(priority = 3, expectedExceptions = IllegalArgumentException.class)
	public void testPutSchemaException() throws MetadataAccessException {

		metaStore.put(null);
	}

	/**
	 * Test insert/update schema details.
	 * 
	 * @throws MetadataAccessException
	 */
	@Test(priority = 2)
	public void testPutSchemaCreate() throws MetadataAccessException {

		Mockito.when(
				mockMetadataRepositoryService.schemaExists(Mockito
						.any(MetasegmentDTO.class))).thenReturn(false);
		Mockito.doNothing()
				.when(mockMetadataRepositoryService)
				.createOrUpdateMetasegment(Mockito.any(MetasegmentDTO.class),
						Mockito.anyBoolean());

		metaStore.put(mockMetasegment);

	}

	@Test(priority = 1)
	public void testPutSchema() throws MetadataAccessException {

		Mockito.when(
				mockObjectEntityMapper.mapMetasegmentEntity(mockMetasegment))
				.thenReturn(mockMetasegmentDTO);
		Mockito.when(
				mockObjectEntityMapper.mapMetasegmentObject(mockMetasegmentDTO))
				.thenReturn(mockMetasegment);
		Mockito.when(
				mockMetadataRepositoryService.schemaExists(mockMetasegmentDTO))
				.thenReturn(true);
		Mockito.when(
				mockMetadataRepositoryService.checkUpdateEligibility(Mockito
						.any(MetasegmentDTO.class))).thenReturn(true);
		Mockito.when(
				mockMetadataRepositoryService
						.checkAttributesAssociation(mockMetasegmentDTO))
				.thenReturn(mockMetasegmentDTO);

		Mockito.doNothing()
				.when(mockMetadataRepositoryService)
				.remove(Mockito.any(MetasegmentDTO.class), Mockito.anyBoolean());

		Mockito.doNothing()
				.when(mockMetadataRepositoryService)
				.createOrUpdateMetasegment(Mockito.any(MetasegmentDTO.class),
						Mockito.anyBoolean());
		metaStore.put(mockMetasegment);

	}

	@Test(priority = 4, expectedExceptions = IllegalArgumentException.class)
	public void testNullPut() throws MetadataAccessException {
		metaStore.put(null);
	}

	@Test(priority = 5)
	public void testPutVersionIneligibity() throws MetadataAccessException {

		Mockito.when(
				mockObjectEntityMapper.mapMetasegmentEntity(mockMetasegment))
				.thenReturn(mockMetasegmentDTO);
		Mockito.when(
				mockMetadataRepositoryService.schemaExists(mockMetasegmentDTO))
				.thenReturn(true);
		Mockito.when(
				mockMetadataRepositoryService.checkUpdateEligibility(Mockito
						.any(MetasegmentDTO.class))).thenReturn(false);
		metaStore.put(mockMetasegment);
	}

	/**
	 * Test to get the Metasegment details.
	 * 
	 * @throws MetadataAccessException
	 */
	@Test(priority = 6)
	public void testGetAdaptorMetasegmentFromCache()
			throws MetadataAccessException {

		when(mockCachedSchemaDetails.containsKey(Mockito.anyString()))
				.thenReturn(true);
		when(mockCachedSchemaDetails.get(anyString())).thenReturn(metadata);
		

		Metasegment metadata = metaStore.getAdaptorMetasegment(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType(), mockEntity.getEntityName());
		Assert.assertNotNull(metadata);

	}

	@Test(priority = 7)
	public void testGetAdaptorMetasegmentFromDB()
			throws MetadataAccessException {
		when(mockCachedSchemaDetails.containsKey(Mockito.anyString()))
				.thenReturn(false);

		Metasegment metadata = metaStore.getAdaptorMetasegment(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType(), mockEntity.getEntityName());
		Assert.assertNull(metadata);
	}

	@Test(priority = 8)
	public void testGetAdaptorEntity() throws MetadataAccessException {

		when(mockCachedSchemaDetails.containsKey(Mockito.anyString()))
				.thenReturn(true);
		when(mockCachedSchemaDetails.get(anyString())).thenReturn(
				mockMetasegment);

		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		when(entitiesSet.iterator()).thenReturn(entityIterator);
		when(entityIterator.hasNext()).thenReturn(true, false);
		when(entityIterator.next()).thenReturn(mockEntity);

		Entitee entity = metaStore.getAdaptorEntity(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType(), mockEntity.getEntityName());
		Assert.assertNotNull(entity);

	}

	@Test(priority = 9)
	public void testGetAdaptorEntityUnavailability()
			throws MetadataAccessException {
		when(mockCachedSchemaDetails.containsKey(Mockito.anyString()))
				.thenReturn(true);
		when(mockCachedSchemaDetails.get(anyString())).thenReturn(null);
		Assert.assertNull(metaStore.getAdaptorEntity(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType(), mockEntity.getEntityName()));
	}

	@Test(priority = 10)
	public void testGetAdaptorEntityNull() throws MetadataAccessException {
		Assert.assertNull(metaStore.getAdaptorEntity(null,
				mockMetasegment.getSchemaType(), mockEntity.getEntityName()));
	}

	/**
	 * Test: Get all the Entities associated to a given Adapter.
	 * 
	 * @throws MetadataAccessException
	 */
	@Test(priority = 11)
	public void testGetAdapterEntities() throws MetadataAccessException {

		when(
				mockMetadataRepositoryService.getAllEntites(
						mockMetasegment.getAdaptorName(),
						mockMetasegment.getSchemaType())).thenReturn(
				entitiesSetDTO);
		when(entitiesSetDTO.size()).thenReturn(1);
		@SuppressWarnings("unchecked")
		Iterator<EntiteeDTO> mockEntiteeDTOIterator = Mockito
				.mock(Iterator.class);
		when(entitiesSetDTO.iterator()).thenReturn(mockEntiteeDTOIterator);
		when(mockEntiteeDTOIterator.hasNext()).thenReturn(true, false);
		when(mockEntiteeDTOIterator.next()).thenReturn(
				Mockito.mock(EntiteeDTO.class));
		Assert.assertNotNull(metaStore.getAdaptorEntities(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType()));

	}

	/**
	 * Test: Get all the Entity List associated to a given Adapter
	 * 
	 * @throws MetadataAccessException
	 */
	@Test(priority = 12)
	public void testGetAdapterEntityList() throws MetadataAccessException {

		when(mockAdaptorEntities.get(anyString())).thenReturn(stringSet);

		stringSet = metaStore.getAdaptorEntityList(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType());
		Assert.assertNotNull(stringSet);
		when(stringSet.size()).thenReturn(1);
		Assert.assertEquals(stringSet.size(), 1);

	}

	/**
	 * Test: Get distict data sources available in repository
	 * 
	 * @throws MetadataAccessException
	 */
	@Test(priority = 12)
	public void testGetDistictDataSource() throws MetadataAccessException {

		List<String> list = new ArrayList<String>() {
			{
				add("Test");
				add("Test1");
				add("Test1");
			}
		};

		when(metaStore.getDataSources()).thenReturn(new HashSet<String>(list));

		Assert.assertEquals(metaStore.getDataSources().size(), 2);

	}

	/**
	 * Unit Test: Get adatper Metasegments
	 * 
	 * @throws MetadataAccessException
	 */
	@Test(priority = 13)
	public void testCheckNullGetAdapterMetaSegments()
			throws MetadataAccessException {

		Mockito.when(mockAdaptorEntities.get(Mockito.anyString())).thenReturn(
				null);
		mockMetasegmentList = metaStore.getAdaptorMetasegments(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType());
		Assert.assertEquals(0, mockMetasegmentList.size());

	}

	@Test(priority = 13)
	public void testGetAdapterMetaSegments() throws MetadataAccessException {

		@SuppressWarnings("unchecked")
		Iterator<String> mockIterator = Mockito.mock(Iterator.class);
		Mockito.when(mockAdaptorEntities.get(Mockito.anyString())).thenReturn(
				stringSet);
		when(stringSet.iterator()).thenReturn(mockIterator);
		when(mockIterator.hasNext()).thenReturn(true, false);
		when(mockIterator.next()).thenReturn("testEntityName");
		mockMetasegmentList = metaStore.getAdaptorMetasegments(
				mockMetasegment.getAdaptorName(),
				mockMetasegment.getSchemaType());
		Assert.assertEquals(1, mockMetasegmentList.size());
	}

	@Test(priority = 15)
	public void testGetAdapterName() {
		Assert.assertNotNull(metaStoreImpl.getAdaptorName(mockMetasegment));
	}

	@Test(priority = 16)
	public void testGetKey() {
		Assert.assertNotNull(metaStoreImpl.getKey(mockMetasegment,
				"testEntityName"));
	}

	@Test(priority = 16)
	@SuppressWarnings("unchecked")
	public void testLoadCacheWithNoData() throws MetadataAccessException {

		when(mockMetadataRepositoryService.getAllSegments()).thenReturn(
				mockMetasegmentDTOList);

		when(mockMetasegmentDTOList.isEmpty()).thenReturn(true);

		metaStoreImpl.loadCache();
	}

	@Test(priority = 17)
	@SuppressWarnings("unchecked")
	public void testLoadCacheWithData() throws MetadataAccessException {

		ReflectionTestUtils.setField(metaStoreImpl, "objectEntityMapper",
				mockObjectEntityMapper);

		when(mockMetadataRepositoryService.getAllSegments()).thenReturn(
				mockMetasegmentDTOList);

		List<Metasegment> mockMetasegmentLists = Mockito.mock(List.class);
		when(
				mockObjectEntityMapper
						.mapMetasegmentListObject(mockMetasegmentDTOList))
				.thenReturn(mockMetasegmentLists);
		when(mockMetasegmentDTOList.isEmpty()).thenReturn(false);
		@SuppressWarnings("unchecked")
		Iterator<Metasegment> mockIterator = Mockito.mock(Iterator.class);
		when(mockMetasegmentLists.iterator()).thenReturn(mockIterator);
		when(mockIterator.hasNext()).thenReturn(true, false);
		when(mockIterator.next()).thenReturn(mockMetasegment);

		metaStoreImpl.loadCache();

	}

	@Test(priority = 18)
	@SuppressWarnings("unchecked")
	public void testLoadAdapterEntitiesMapWithNoData()
			throws MetadataAccessException {

		when(mockAdaptorEntities.containsKey(anyObject())).thenReturn(true);

		when(mockAdaptorEntities.get(anyObject())).thenReturn(stringSet);
		when(stringSet.isEmpty()).thenReturn(false);
		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		when(entitiesSet.iterator()).thenReturn(entityIterator);
		when(entityIterator.hasNext()).thenReturn(true, false);
		when(entityIterator.next()).thenReturn(mockEntity);
		when(mockEntity.getEntityName()).thenReturn("testEntityName");
		metaStoreImpl.loadAdaptorEntities(mockMetasegment);

	}

	@Test(priority = 19)
	@SuppressWarnings("unchecked")
	public void testLoadAdapterEntitiesMapWithData()
			throws MetadataAccessException {

		when(mockAdaptorEntities.containsKey(anyObject())).thenReturn(true);
		when(mockAdaptorEntities.get(anyObject())).thenReturn(stringSet);
		when(stringSet.isEmpty()).thenReturn(true);

		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		when(entitiesSet.iterator()).thenReturn(entityIterator);
		when(entityIterator.hasNext()).thenReturn(true, false);
		when(entityIterator.next()).thenReturn(mockEntity);
		when(mockEntity.getEntityName()).thenReturn("testEntityName");
		metaStoreImpl.loadAdaptorEntities(mockMetasegment);

	}

	@Test(priority = 20)
	@SuppressWarnings("unchecked")
	public void testRemoveFromCache() throws MetadataAccessException {

		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		when(entitiesSet.iterator()).thenReturn(entityIterator);
		when(entityIterator.hasNext()).thenReturn(true, false);
		when(entityIterator.next()).thenReturn(mockEntity);
		when(mockEntity.getEntityName()).thenReturn("testEntityName");

		Map<String, Metasegment> mockCachedSchemaDetails = (Map<String, Metasegment>) Mockito
				.mock(Map.class);
		ReflectionTestUtils.setField(metaStoreImpl, "cachedSchemaDetails",
				mockCachedSchemaDetails);
		when(mockCachedSchemaDetails.containsKey(Mockito.anyString()))
				.thenReturn(true);

		metaStoreImpl.removeFromCache(mockMetasegment, true);

	}

	@Test(priority = 21)
	@SuppressWarnings("unchecked")
	public void testRemoveFromCacheFalseBlock() throws MetadataAccessException {

		ReflectionTestUtils.setField(metaStoreImpl, "adaptorEntities",
				mockAdaptorEntities);
		when(mockAdaptorEntities.containsKey(anyObject())).thenReturn(true);
		when(mockAdaptorEntities.get(anyObject())).thenReturn(stringSet);
		when(stringSet.isEmpty()).thenReturn(false);

		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		when(entitiesSet.iterator()).thenReturn(entityIterator);
		when(entityIterator.hasNext()).thenReturn(true, false);
		when(entityIterator.next()).thenReturn(mockEntity);
		when(mockEntity.getEntityName()).thenReturn("testEntityName");
		when(stringSet.contains(any(String.class))).thenReturn(true);
		metaStoreImpl.removeFromCache(mockMetasegment, false);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testRemoveException() throws MetadataAccessException {
		metaStore.remove(null);
	}

	@Test
	public void testRemove() throws MetadataAccessException {

		Mockito.doNothing()
				.when(mockMetadataRepositoryService)
				.remove(Mockito.any(MetasegmentDTO.class), Mockito.anyBoolean());
		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		metaStore.remove(mockMetasegment);
	}

	@Test
	public void testLoadCacheWithSchemaDetailsWithOutEntityDetails()
			throws MetadataAccessException {
		when(mockMetasegment.getEntitees()).thenReturn(null);
		metaStoreImpl.loadCacheWithSchemaDetails(mockMetasegment);
	}

	@Test
	public void testLoadCacheWithSchemaDetailsWithEntityDetails()
			throws MetadataAccessException {
		when(mockMetasegment.getEntitees()).thenReturn(entitiesSet);
		when(entitiesSet.iterator()).thenReturn(entityIterator);
		when(entityIterator.hasNext()).thenReturn(true, false);
		when(entityIterator.next()).thenReturn(mockEntity);
		metaStoreImpl.loadCacheWithSchemaDetails(mockMetasegment);
	}

	@Test(priority = 22)
	public void testCreateDataSourceIfNotExist() throws MetadataAccessException {

		when(mockMetadataRepositoryService.getAllSegments("testAdaptorName"))
				.thenReturn(mockMetasegmentDTOList);
		when(mockMetasegmentDTOList.size()).thenReturn(0);
		when(
				mockObjectEntityMapper
						.mapMetasegmentObject(any(MetasegmentDTO.class)))
				.thenReturn(mockMetasegment);
		Assert.assertNotNull(metaStore.createDatasourceIfNotExist(
				"testAdaptorName", "testSchemaType"));
	}

}
