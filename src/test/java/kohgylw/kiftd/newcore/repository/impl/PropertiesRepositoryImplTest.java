package kohgylw.kiftd.newcore.repository.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.mapper.PropertiesMapper;
import kohgylw.kiftd.server.model.Property;

@ExtendWith(MockitoExtension.class)
class PropertiesRepositoryImplTest {

	@Mock
	private PropertiesMapper propertiesMapper;

	private PropertiesRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		repository = new PropertiesRepositoryImpl(propertiesMapper);
	}

	@Test
	void testSelectByKey_DelegatesToMapper() {
		Property property = new Property();
		when(propertiesMapper.selectByKey("k1")).thenReturn(property);

		assertSame(property, repository.selectByKey("k1"));
		verify(propertiesMapper).selectByKey("k1");
	}

	@Test
	void testInsert_DelegatesToMapper() {
		Property property = new Property();
		when(propertiesMapper.insert(property)).thenReturn(1);

		assertEquals(1, repository.insert(property));
	}

	@Test
	void testUpdate_DelegatesToMapper() {
		Property property = new Property();
		when(propertiesMapper.update(property)).thenReturn(1);

		assertEquals(1, repository.update(property));
	}

	@Test
	void testDeleteByKey_DelegatesToMapper() {
		when(propertiesMapper.deleteByKey("k1")).thenReturn(1);

		assertEquals(1, repository.deleteByKey("k1"));
	}
}
