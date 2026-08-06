package kohgylw.kiftd.newcore.repository.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.model.Folder;

@ExtendWith(MockitoExtension.class)
class FolderRepositoryImplTest {

	@Mock
	private FolderMapper folderMapper;

	private FolderRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		repository = new FolderRepositoryImpl(folderMapper);
	}

	@Test
	void testSelectById_DelegatesToMapper() {
		Folder folder = new Folder();
		when(folderMapper.selectById("d1")).thenReturn(folder);

		assertSame(folder, repository.selectById("d1"));
		verify(folderMapper).selectById("d1");
	}

	@Test
	void testSelectByParentId_DelegatesToMapper() {
		List<Folder> folders = List.of(new Folder());
		when(folderMapper.queryByParentId("p1")).thenReturn(folders);

		assertSame(folders, repository.selectByParentId("p1"));
	}

	@Test
	void testSelectByParentIdSection_DelegatesToMapper() {
		Map<String, Object> params = Map.of("offset", 0, "rows", 10);
		List<Folder> folders = List.of(new Folder());
		when(folderMapper.queryByParentIdSection(params)).thenReturn(folders);

		assertSame(folders, repository.selectByParentIdSection(params));
	}

	@Test
	void testSelectByParentIdAndFolderName_DelegatesToMapper() {
		Map<String, String> params = Map.of("pid", "p1", "folderName", "docs");
		Folder folder = new Folder();
		when(folderMapper.queryByParentIdAndFolderName(params)).thenReturn(folder);

		assertSame(folder, repository.selectByParentIdAndFolderName(params));
	}

	@Test
	void testInsert_DelegatesToMapper() {
		Folder folder = new Folder();
		when(folderMapper.insert(folder)).thenReturn(1);

		assertEquals(1, repository.insert(folder));
	}

	@Test
	void testUpdate_DelegatesToMapper() {
		Folder folder = new Folder();
		when(folderMapper.updateById(folder)).thenReturn(1);

		assertEquals(1, repository.update(folder));
	}

	@Test
	void testDeleteById_DelegatesToMapper() {
		when(folderMapper.deleteById("d1")).thenReturn(1);

		assertEquals(1, repository.deleteById("d1"));
	}

	@Test
	void testCountByParentId_DelegatesToMapper() {
		when(folderMapper.countByParentId("p1")).thenReturn(3L);

		assertEquals(3L, repository.countByParentId("p1"));
	}
}
