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

import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;

@ExtendWith(MockitoExtension.class)
class FileNodeRepositoryImplTest {

	@Mock
	private NodeMapper nodeMapper;

	private FileNodeRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		repository = new FileNodeRepositoryImpl(nodeMapper);
	}

	@Test
	void testSelectById_DelegatesToMapper() {
		Node node = new Node();
		when(nodeMapper.selectById("f1")).thenReturn(node);

		assertSame(node, repository.selectById("f1"));
		verify(nodeMapper).selectById("f1");
	}

	@Test
	void testSelectByParentFolderId_DelegatesToMapper() {
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryByParentFolderId("p1")).thenReturn(nodes);

		assertSame(nodes, repository.selectByParentFolderId("p1"));
	}

	@Test
	void testSelectByParentFolderIds_DelegatesToMapper() {
		List<String> ids = List.of("a", "b");
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryByParentFolderIds(ids)).thenReturn(nodes);

		assertSame(nodes, repository.selectByParentFolderIds(ids));
	}

	@Test
	void testSelectByParentFolderIdsLimit_DelegatesToMapper() {
		List<String> ids = List.of("a");
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryByParentFolderIdsLimit(ids, 10)).thenReturn(nodes);

		assertSame(nodes, repository.selectByParentFolderIdsLimit(ids, 10));
	}

	@Test
	void testSelectByParentFolderIdSection_DelegatesToMapper() {
		Map<String, Object> params = Map.of("offset", 0, "rows", 10);
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryByParentFolderIdSection(params)).thenReturn(nodes);

		assertSame(nodes, repository.selectByParentFolderIdSection(params));
	}

	@Test
	void testSelectByPath_DelegatesToMapper() {
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryByPath("/a")).thenReturn(nodes);

		assertSame(nodes, repository.selectByPath("/a"));
	}

	@Test
	void testSelectByPathExcludeById_DelegatesToMapper() {
		Map<String, String> params = Map.of("path", "/a", "fileId", "f1");
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryByPathExcludeById(params)).thenReturn(nodes);

		assertSame(nodes, repository.selectByPathExcludeById(params));
	}

	@Test
	void testSelectBySomeFolder_DelegatesToMapper() {
		List<Node> nodes = List.of(new Node());
		when(nodeMapper.queryBySomeFolder("f1")).thenReturn(nodes);

		assertSame(nodes, repository.selectBySomeFolder("f1"));
	}

	@Test
	void testInsert_DelegatesToMapper() {
		Node node = new Node();
		when(nodeMapper.insert(node)).thenReturn(1);

		assertEquals(1, repository.insert(node));
	}

	@Test
	void testUpdate_DelegatesToMapper() {
		Node node = new Node();
		when(nodeMapper.updateById(node)).thenReturn(1);

		assertEquals(1, repository.update(node));
	}

	@Test
	void testDeleteById_DelegatesToMapper() {
		when(nodeMapper.deleteById("f1")).thenReturn(1);

		assertEquals(1, repository.deleteById("f1"));
	}

	@Test
	void testCountByParentFolderId_DelegatesToMapper() {
		when(nodeMapper.countByParentFolderId("p1")).thenReturn(5L);

		assertEquals(5L, repository.countByParentFolderId("p1"));
	}
}
