/**
 * Copyright (C) 2015 Stubhub.
 */
package io.bigdime.metadata;

import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.util.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import io.bigdime.adaptor.metadata.dto.AttributeDTO;
import io.bigdime.adaptor.metadata.dto.DataTypeDTO;
import io.bigdime.adaptor.metadata.dto.EntiteeDTO;
import io.bigdime.adaptor.metadata.dto.MetasegmentDTO;
import io.bigdime.adaptor.metadata.impl.MetadataRepositoryService;
import io.bigdime.adaptor.metadata.model.Attribute;
import io.bigdime.adaptor.metadata.model.Entitee;
import io.bigdime.adaptor.metadata.repositories.DataTypeRepository;
import io.bigdime.adaptor.metadata.repositories.EntitiesRepository;
import io.bigdime.adaptor.metadata.repositories.MetadataRepository;

/**
 * Class MetadataRepositoryServiceUnitTest
 * 
 * @author Neeraj Jain, psabinikari
 * 
 */
public class MetadataRepositoryServiceTest {

	MetadataRepositoryService repoService;
	@Mock
	MetasegmentDTO metasegment;
	@Mock
	EntiteeDTO entities;

	@Mock
	AttributeDTO attribute;

	@Mock
	MetadataRepositoryService metadataRepositoryService;

	@Mock
	MetadataRepository mockMetadataRepository;
	
	@Mock
	DataTypeRepository mockDataTypeRepository;
	
	@Mock
	DataTypeDTO mockDataTypeDTO;

	@Mock
	Set<AttributeDTO> attributeSet;

	EntiteeDTO entity;
	// @Mock
	Set<EntiteeDTO> entitySet = new HashSet<EntiteeDTO>();

	@Mock
	Set<EntiteeDTO> entiteeSet;

	@Mock
	Iterator<EntiteeDTO> mockEntityIterator;

	@Mock
	Iterator<AttributeDTO> mockAttributeIterator;

	/**
	 * Loading the test data
	 * 
	 * @throws Exception
	 */

	@BeforeClass
	public void init() {

		initMocks(this);
		repoService = new MetadataRepositoryService();
		entity = new EntiteeDTO("testEntityName", "testEntityLocation", 1.0,
				"Description", attributeSet);
		entitySet.add(entity);

	}

	/**
	 * Unit Test: Create Metasegment
	 */
	@Test
	public void testCreateMetasegment() {

		ReflectionTestUtils.setField(repoService,
				"dynamicDataTypesConfiguration", true);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		when(metasegment.getAdaptorName()).thenReturn("TestApplicationName");
		when(metasegment.getSchemaType()).thenReturn("TestSchemaType");

		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						metasegment.getAdaptorName(),
						metasegment.getSchemaType())).thenReturn(null);

		when(metasegment.getEntitees()).thenReturn(entiteeSet);

		when(entiteeSet.iterator()).thenAnswer(
				new Answer<Iterator<EntiteeDTO>>() {

					public Iterator<EntiteeDTO> answer(
							InvocationOnMock invocation) throws Throwable {
						return Arrays.asList(Mockito.mock(EntiteeDTO.class),
								Mockito.mock(EntiteeDTO.class)).iterator();
					}

				});

		repoService.createOrUpdateMetasegment(metasegment, false);

	}

	@Test
	public void testUpdateMetasegment() {


		when(metasegment.getAdaptorName()).thenReturn("testAdaptorName");
		when(metasegment.getSchemaType()).thenReturn("testSchemaType");
		
		MetasegmentDTO metaDBDetails = Mockito.mock(MetasegmentDTO.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		when(mockMetadataRepository.findByAdaptorNameAndSchemaType(metasegment.getAdaptorName(), metasegment.getSchemaType())).thenReturn(metaDBDetails);
		when(metasegment.getEntitees()).thenReturn(entiteeSet);
		Set<EntiteeDTO> entityDTODBSet =  Mockito.mock(Set.class); 
		
		
		when(metaDBDetails.getEntitees()).thenReturn(entityDTODBSet);
		
		when(entiteeSet.iterator()).thenReturn(mockEntityIterator);
		when(mockEntityIterator.hasNext()).thenReturn(true,false);
		when(mockEntityIterator.next()).thenReturn(entities);
		when(entities.getEntityName()).thenReturn("testEntityName");
		when(entities.getEntityLocation()).thenReturn("testEntityLocation");
		when(entities.getAttributes()).thenReturn(attributeSet);
		
		when(metaDBDetails.getEntitees().size()).thenReturn(1);
		EntiteeDTO mockEntityDBDTO = Mockito.mock(EntiteeDTO.class);
		Iterator<EntiteeDTO> mockEntiteeDBIterator = Mockito.mock(Iterator.class);
		when(entityDTODBSet.iterator()).thenReturn(mockEntiteeDBIterator);
		when(mockEntiteeDBIterator.hasNext()).thenReturn(true,false);
		when(mockEntiteeDBIterator.next()).thenReturn(mockEntityDBDTO);
		when(mockEntityDBDTO.getEntityName()).thenReturn("testEntityName");
		repoService.createOrUpdateMetasegment(metasegment, true);

	}

	@Test
	public void testConfigureDynamicDataType() {

		DataTypeRepository mockDataTypeRepository = Mockito
				.mock(DataTypeRepository.class);
		ReflectionTestUtils.setField(repoService, "dataTypeRepository",
				mockDataTypeRepository);

		when(attribute.getAttributeType()).thenReturn("testAttributeType");
		when(
				mockDataTypeRepository.findByDataType(attribute
						.getAttributeType())).thenReturn(
				Mockito.mock(DataTypeDTO.class));
		when(entities.getAttributes()).thenReturn(attributeSet);

		when(attributeSet.iterator()).thenAnswer(
				new Answer<Iterator<AttributeDTO>>() {
					public Iterator<AttributeDTO> answer(
							InvocationOnMock invocation) throws Throwable {
						return Arrays.asList(Mockito.mock(AttributeDTO.class),
								Mockito.mock(AttributeDTO.class)).iterator();
					}
				});

		repoService.configureDyanamicDataType(entities);

	}

	/**
	 * Unit Test: remove metasegment
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testRemove() {

		MetasegmentDTO metsegmentDetails = Mockito.mock(MetasegmentDTO.class);

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		EntitiesRepository mockEntityRepository = Mockito
				.mock(EntitiesRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		ReflectionTestUtils.setField(repoService, "entityRepository",
				mockEntityRepository);

		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						metasegment.getAdaptorName(),
						metasegment.getSchemaType())).thenReturn(null);
		repoService.remove(metasegment, false);

		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						metasegment.getAdaptorName(),
						metasegment.getSchemaType())).thenReturn(
				metsegmentDetails);
		repoService.remove(metasegment, true);

		Set<EntiteeDTO> mockInputEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);
		Set<EntiteeDTO> mockRepoEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);
		Iterator<EntiteeDTO> inputEntityIterator = Mockito.mock(Iterator.class);
		Iterator<EntiteeDTO> repoEntityIterator = Mockito.mock(Iterator.class);
		EntiteeDTO ents = Mockito.mock(EntiteeDTO.class);
		EntiteeDTO repoEnts = Mockito.mock(EntiteeDTO.class);

		when(metasegment.getEntitees()).thenReturn(mockInputEntitySet);
		when(metsegmentDetails.getEntitees()).thenReturn(mockRepoEntitySet);

		when(mockInputEntitySet.iterator()).thenReturn(inputEntityIterator);
		when(inputEntityIterator.hasNext()).thenReturn(true, false);
		when(inputEntityIterator.next()).thenReturn(ents);

		when(mockRepoEntitySet.iterator()).thenReturn(repoEntityIterator);
		when(repoEntityIterator.hasNext()).thenReturn(true, false);
		when(repoEntityIterator.next()).thenReturn(repoEnts);

		when(ents.getEntityName()).thenReturn("testEntityName");
		when(repoEnts.getEntityName()).thenReturn("testEntityName");

		repoService.remove(metasegment, false);

	}
	
	@SuppressWarnings("unchecked")
	@Test(expectedExceptions=IllegalArgumentException.class)
	public void removeIllegalArgumentExceptionCheck() {
		repoService.remove(null, false);

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCheckUpdateEligibility() {

		MetasegmentDTO repoSegment = Mockito.mock(MetasegmentDTO.class);

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);

		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		when(metasegment.getAdaptorName()).thenReturn("testApplicationName");
		when(metasegment.getSchemaType()).thenReturn("testSchemaType");
		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						metasegment.getAdaptorName(),
						metasegment.getSchemaType())).thenReturn(repoSegment);

		Set<EntiteeDTO> mockInputEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);
		Set<EntiteeDTO> mockRepoEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);
		Iterator<EntiteeDTO> inputEntityIterator = Mockito.mock(Iterator.class);
		Iterator<EntiteeDTO> repoEntityIterator = Mockito.mock(Iterator.class);
		EntiteeDTO ents = Mockito.mock(EntiteeDTO.class);
		EntiteeDTO repoEnts = Mockito.mock(EntiteeDTO.class);

		when(metasegment.getEntitees()).thenReturn(mockInputEntitySet);
		when(repoSegment.getEntitees()).thenReturn(mockRepoEntitySet);

		when(mockInputEntitySet.iterator()).thenReturn(inputEntityIterator);
		when(inputEntityIterator.hasNext()).thenReturn(true, false);
		when(inputEntityIterator.next()).thenReturn(ents);

		when(mockRepoEntitySet.iterator()).thenReturn(repoEntityIterator);
		when(repoEntityIterator.hasNext()).thenReturn(true, false);
		when(repoEntityIterator.next()).thenReturn(repoEnts);

		when(ents.getVersion()).thenReturn(1.0);
		when(repoEnts.getVersion()).thenReturn(1.0);

		when(metasegment.getEntitees().size()).thenReturn(1);
		Assert.assertTrue(repoService.checkUpdateEligibility(metasegment));

		when(metasegment.getEntitees().size()).thenReturn(2);
		Assert.assertFalse(repoService.checkUpdateEligibility(metasegment));
		
	}

	/**
	 * Test: Schema existence check
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testSchemaExists() {

		MetasegmentDTO foundSegment = Mockito.mock(MetasegmentDTO.class);
		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		EntitiesRepository mockEntityRepository = Mockito
				.mock(EntitiesRepository.class);
		ReflectionTestUtils.setField(repoService, "entityRepository",
				mockEntityRepository);

		when(metasegment.getAdaptorName()).thenReturn("testApplicationName");
		when(metasegment.getSchemaType()).thenReturn("testSchemaType");
		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						metasegment.getAdaptorName(),
						metasegment.getSchemaType())).thenReturn(null);

		repoService.schemaExists(metasegment);

		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						metasegment.getAdaptorName(),
						metasegment.getSchemaType())).thenReturn(foundSegment);
		when(foundSegment.getAdaptorName()).thenReturn("testApplicationName");
		when(foundSegment.getSchemaType()).thenReturn("testSchemaType");

		Set<EntiteeDTO> mockInputEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);
		Set<EntiteeDTO> mockRepoEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);
		Iterator<EntiteeDTO> inputEntityIterator = Mockito.mock(Iterator.class);
		Iterator<EntiteeDTO> repoEntityIterator = Mockito.mock(Iterator.class);
		EntiteeDTO ents = Mockito.mock(EntiteeDTO.class);
		EntiteeDTO repoEnts = Mockito.mock(EntiteeDTO.class);

		when(metasegment.getEntitees()).thenReturn(mockInputEntitySet);
		when(foundSegment.getEntitees()).thenReturn(mockRepoEntitySet);

		when(mockInputEntitySet.iterator()).thenReturn(inputEntityIterator);
		when(inputEntityIterator.hasNext()).thenReturn(true, false);
		when(inputEntityIterator.next()).thenReturn(ents);

		when(mockRepoEntitySet.iterator()).thenReturn(repoEntityIterator);
		when(repoEntityIterator.hasNext()).thenReturn(true, false);
		when(repoEntityIterator.next()).thenReturn(repoEnts);

		when(repoEnts.getId()).thenReturn(1);
		when(ents.getEntityName()).thenReturn("testEntityName");
		when(mockEntityRepository.findByIdAndEntityName(anyInt(), anyString()))
				.thenReturn(entities);

		when(metasegment.getEntitees().size()).thenReturn(1);
		Assert.assertTrue(repoService.schemaExists(metasegment));

		when(metasegment.getEntitees().size()).thenReturn(2);
		Assert.assertFalse(repoService.schemaExists(metasegment));

	}

	/**
	 * Test: Get distinct data sources available in repository
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testGetDistinctdataSources() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		Set<String> mockStringSet =new HashSet<String>();
		mockStringSet.add("test");
		when(mockMetadataRepository.findAllDataSources()).thenReturn(
				mockStringSet);
       
		for(String dataSource:repoService.getDistinctDataSources()){
			Assert.assertEquals(dataSource, "test");
		}
	}
	
	/**
	 * Test: Get if distinct data sources available in repository is Empty
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testGetDistinctdataSourcesEmpty() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		Set<String> stringSet =new HashSet<String>();
		when(mockMetadataRepository.findAllDataSources()).thenReturn(
				stringSet);
		Assert.assertTrue(repoService.getDistinctDataSources().isEmpty());
	}

	/**
	 * Test: Get the Schema details associated with a give Metasegment
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testGetSchema() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		when(metasegment.getAdaptorName()).thenReturn("testApplicationName");
		when(metasegment.getSchemaType()).thenReturn("testSchemaType");
		when(entities.getEntityName()).thenReturn("testEntityName");

		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						anyString(), anyString())).thenReturn(null);
    	MetasegmentDTO repoSegment = Mockito.mock(MetasegmentDTO.class);
		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						anyString(), anyString())).thenReturn(repoSegment);

		Set<EntiteeDTO> mockRepoEntitySet = (Set<EntiteeDTO>) Mockito
				.mock(Set.class);

		Iterator<EntiteeDTO> repoEntityIterator = Mockito.mock(Iterator.class);

		EntiteeDTO ents = Mockito.mock(EntiteeDTO.class);

		when(repoSegment.getEntitees()).thenReturn(mockRepoEntitySet);

		when(mockRepoEntitySet.iterator()).thenReturn(repoEntityIterator);
		when(repoEntityIterator.hasNext()).thenReturn(true, false);
		when(repoEntityIterator.next()).thenReturn(ents);

		EntitiesRepository mockEntityRepository = Mockito
				.mock(EntitiesRepository.class);
		ReflectionTestUtils.setField(repoService, "entityRepository",
				mockEntityRepository);

		when(ents.getId()).thenReturn(1);
		when(repoSegment.getId()).thenReturn(1);
		when(repoSegment.getAdaptorName()).thenReturn("testApplicationName");
		when(repoSegment.getDatabaseName()).thenReturn("testDatabaseName");
		when(repoSegment.getDatabaseLocation()).thenReturn(
				"testDatabaseLocation");
		when(repoSegment.getRepositoryType()).thenReturn("testRepositoryType");
		when(repoSegment.getIsDataSource()).thenReturn("Y");
		when(repoSegment.getDescription()).thenReturn("testDescription");
		when(repoSegment.getCreatedAt()).thenReturn(Mockito.mock(Date.class));
		when(repoSegment.getCreatedBy()).thenReturn("testUser");
		when(repoSegment.getUpdatedAt()).thenReturn(Mockito.mock(Date.class));
		when(repoSegment.getUpdatedBy()).thenReturn("testUser");
		when(mockEntityRepository.findByIdAndEntityName(anyInt(), anyString()))
				.thenReturn(null);
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getAdaptorName(),"testApplicationName");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getDatabaseName(),"testDatabaseName");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getDatabaseLocation(),"testDatabaseLocation");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getRepositoryType(),"testRepositoryType");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getIsDataSource(),"Y");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getDescription(),"testDescription");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getCreatedBy(),"testUser");
		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getUpdatedBy(),"testUser");
		
		EntiteeDTO entits = Mockito.mock(EntiteeDTO.class);
		when(mockEntityRepository.findByIdAndEntityName(anyInt(), anyString()))
				.thenReturn(entits);

		Assert.assertEquals(repoService.getSchema(metasegment.getAdaptorName(),metasegment.getSchemaType(), entities.getEntityName()).getEntitees().isEmpty(), true);
	}

	/**
	 * Test: Get all Metasegments This gives all the Metasegments that are
	 * available in repository.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testGetAllSegments() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		List<MetasegmentDTO> metasegments =new ArrayList<MetasegmentDTO>();
		MetasegmentDTO metasegmentDTO=new MetasegmentDTO();
		metasegmentDTO.setAdaptorName("testAdaptorName");
		metasegmentDTO.setDatabaseName("testDBName");
		metasegmentDTO.setDatabaseLocation("testDBLocation");
		metasegmentDTO.setDescription("testDescription");
		metasegments.add(metasegmentDTO);
		when(mockMetadataRepository.findAll()).thenReturn(metasegments);
		
		for(MetasegmentDTO metasegmentsDTO:repoService.getAllSegments()){
			Assert.assertEquals(metasegmentsDTO.getAdaptorName(), "testAdaptorName");
			Assert.assertEquals(metasegmentsDTO.getDatabaseName(), "testDBName");
			Assert.assertEquals(metasegmentsDTO.getDatabaseLocation(), "testDBLocation");
			Assert.assertEquals(metasegmentsDTO.getDescription(), "testDescription");
		}
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testGetAllSegmentsEmpty() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);
		List<MetasegmentDTO> metasegments =new ArrayList<MetasegmentDTO>();
		when(mockMetadataRepository.findAll()).thenReturn(metasegments);
		Assert.assertTrue(repoService.getAllSegments().isEmpty());


	}


	/**
	 * Test: Get All Entities(Tables) associated to an Adaptor
	 */
	@Test
	public void testGetAllEntitesNull() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		when(metasegment.getAdaptorName()).thenReturn("testApplicationName");
		when(metasegment.getSchemaType()).thenReturn("testSchemaType");
		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						anyString(), anyString())).thenReturn(null);
		Assert.assertNull(repoService.getAllEntites(metasegment.getAdaptorName(),
				metasegment.getSchemaType()));

	}

	@Test
	public void testGetAllEntitiesNotNull() {

		MetadataRepositoryService repoService = new MetadataRepositoryService();
		MetasegmentDTO mockRepoSegment = Mockito.mock(MetasegmentDTO.class);
		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);
		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		when(metasegment.getAdaptorName()).thenReturn("testApplicationName");
		when(metasegment.getSchemaType()).thenReturn("testSchemaType");

		when(
				mockMetadataRepository.findByAdaptorNameAndSchemaType(
						anyString(), anyString())).thenReturn(mockRepoSegment);
		when(mockRepoSegment.getEntitees()).thenReturn(entiteeSet);
		when(entiteeSet.isEmpty()).thenReturn(false);
		Assert.assertNotNull(repoService.getAllEntites(metasegment.getAdaptorName(),
				metasegment.getSchemaType()));
	}

	@Test
	public void getAllSegmentsForAnAdaptor() {

		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);

		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		@SuppressWarnings("unchecked")
		List<MetasegmentDTO> mockMetasegmentDTOList = Mockito.mock(List.class);
		when(mockMetadataRepository.findByAdaptorName(Mockito.anyString()))
				.thenReturn(mockMetasegmentDTOList);
		when(mockMetasegmentDTOList.isEmpty()).thenReturn(false);
		Assert.assertNotNull(repoService.getAllSegments("testAdaptorName"));
	}

	@Test
	public void getAllSegmentsForAnAdaptorEmptyList() {
		MetadataRepository mockMetadataRepository = Mockito
				.mock(MetadataRepository.class);

		ReflectionTestUtils.setField(repoService, "repository",
				mockMetadataRepository);

		@SuppressWarnings("unchecked")
		List<MetasegmentDTO> mockMetasegmentDTOList = new ArrayList<MetasegmentDTO>();
		when(mockMetadataRepository.findByAdaptorName(Mockito.anyString()))
				.thenReturn(mockMetasegmentDTOList);
		Assert.assertTrue(repoService.getAllSegments("testAdaptorName").isEmpty());
	}

}