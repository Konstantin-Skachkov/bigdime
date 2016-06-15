/**
 * Copyright (C) 2015 Stubhub.
 */
package io.bigdime.metadata;

import java.lang.reflect.Field;

import io.bigdime.adaptor.metadata.model.Attribute;
import io.bigdime.adaptor.metadata.model.DataType;

import org.apache.commons.lang3.reflect.FieldUtils;

import io.bigdime.common.testutils.GetterSetterTestHelper;

import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.MockitoAnnotations.initMocks;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Class AttributeTest
 * 
 * @author Neeraj Jain, psabinikari
 * 
 */
public class AttributeTest {

	Attribute attribute;

	@Mock
	Attribute mockAttribute;

	@Mock
	DataType mockDataType;

	@BeforeClass
	public void init() {
		initMocks(this);
		attribute = new Attribute();
	}

	@Test
	public void testConstructor() {
		attribute = new Attribute("testAttributeName", "testAttribtueType",
				"testIntPart", "testFractionalPart", "testComment", "NotNull",
				"testColumn", "testTargetMappedName", "testDefaultValue");

		Assert.assertEquals(attribute.getAttributeName(), "testAttributeName");

	}

	@Test
	public void testGettersAndSetters() {
		Field[] fields = FieldUtils.getAllFields(Attribute.class);
		for (Field f : fields) {
			if (f.getType() == String.class) {
				ReflectionTestUtils.invokeSetterMethod(attribute, f.getName(), "UNIT-TEST-" + f.getName());
				Object gotValue = ReflectionTestUtils.invokeGetterMethod(attribute, f.getName());
				Assert.assertEquals("UNIT-TEST-" + f.getName(), gotValue);
			}

			if (f.getType() == Integer.class) {
				ReflectionTestUtils.invokeSetterMethod(attribute, f.getName(), 2);
				Object gotValue = ReflectionTestUtils.invokeGetterMethod(attribute, f.getName());
				Assert.assertEquals(2, gotValue);
			}

		}

	}

	@Test
	public void testDataType() {
		attribute.setDataType(mockDataType);
		Assert.assertEquals(mockDataType, attribute.getDataType());
		Mockito.when(mockDataType.getDataType()).thenReturn("test");
		
	}

}
