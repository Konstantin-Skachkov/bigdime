/**
 * Copyright (C) 2015 Stubhub.
 */
package io.bigdime.metadata;

import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.bigdime.adaptor.metadata.dto.DataTypeDTO;
import io.bigdime.common.testutils.GetterSetterTestHelper;

/**
 * Class DataTypeTest
 * 
 * @author Neeraj Jain, psabinikari
 * 
 */
public class DataTypeDTOTest {
	
	DataTypeDTO dataType;
	
	@Mock
	DataTypeDTO mockDataType;
	
	@BeforeClass
	public void init() {
		initMocks(this);
		dataType = new DataTypeDTO();
	}

	@Test
	public void testConstructor() {
		dataType = new DataTypeDTO("testDataType", "testDescription");

		Assert.assertEquals(dataType.getDataType(), "testDataType");
	}
	
	@Test
	public void testGettersAndSetters() {
		Field[] fields = FieldUtils.getAllFields(DataTypeDTO.class);
		for (Field f : fields) {
			if (f.getType() == String.class) {
				ReflectionTestUtils.invokeSetterMethod(dataType, f.getName(), "UNIT-TEST-" + f.getName());
				Object gotValue = ReflectionTestUtils.invokeGetterMethod(dataType, f.getName());
				Assert.assertEquals("UNIT-TEST-" + f.getName(), gotValue);
			}

			if (f.getType() == int.class) {
				ReflectionTestUtils.invokeSetterMethod(dataType, f.getName(), 2);
				Object gotValue = ReflectionTestUtils.invokeGetterMethod(dataType, f.getName());
				Assert.assertEquals(2, gotValue);
			}

		}

	}
	
}
