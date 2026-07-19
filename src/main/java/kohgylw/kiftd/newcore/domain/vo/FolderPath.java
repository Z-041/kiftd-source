package kohgylw.kiftd.newcore.domain.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FolderPath {

	private static final String SEPARATOR = "/";
	private static final String ROOT = "root";

	private final List<String> segments;

	private FolderPath(List<String> segments) {
		this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
	}

	public static FolderPath root() {
		return new FolderPath(Collections.singletonList(ROOT));
	}

	public static FolderPath of(String path) {
		if (path == null || path.isEmpty() || path.equals(SEPARATOR) || path.equals(ROOT)) {
			return root();
		}
		String normalized = path.replaceAll("[\\\\/]+", SEPARATOR).replaceAll("^/|/$", "");
		if (normalized.isEmpty()) {
			return root();
		}
		List<String> segments = new ArrayList<>();
		segments.add(ROOT);
		segments.addAll(Arrays.asList(normalized.split(SEPARATOR)));
		segments.removeIf(String::isEmpty);
		if (segments.isEmpty()) {
			return root();
		}
		if (!ROOT.equals(segments.get(0))) {
			segments.add(0, ROOT);
		}
		return new FolderPath(segments);
	}

	public static FolderPath of(String... names) {
		if (names == null || names.length == 0) {
			return root();
		}
		List<String> segments = new ArrayList<>();
		segments.add(ROOT);
		for (String name : names) {
			if (name != null && !name.isEmpty() && !ROOT.equals(name)) {
				segments.add(name);
			}
		}
		return new FolderPath(segments);
	}

	public boolean isRoot() {
		return segments.size() == 1 && ROOT.equals(segments.get(0));
	}

	public int depth() {
		return segments.size() - 1;
	}

	public String getFolderName() {
		return segments.get(segments.size() - 1);
	}

	public FolderPath getParent() {
		if (isRoot()) {
			return this;
		}
		return new FolderPath(segments.subList(0, segments.size() - 1));
	}

	public FolderPath resolve(String folderName) {
		if (folderName == null || folderName.isEmpty()) {
			return this;
		}
		if ("..".equals(folderName)) {
			return getParent();
		}
		List<String> newSegments = new ArrayList<>(segments);
		newSegments.add(folderName);
		return new FolderPath(newSegments);
	}

	public boolean startsWith(FolderPath other) {
		if (other == null) {
			return false;
		}
		if (other.segments.size() > this.segments.size()) {
			return false;
		}
		for (int i = 0; i < other.segments.size(); i++) {
			if (!this.segments.get(i).equals(other.segments.get(i))) {
				return false;
			}
		}
		return true;
	}

	public List<String> getSegments() {
		return segments;
	}

	public List<String> getFolderNames() {
		if (isRoot()) {
			return Collections.emptyList();
		}
		return segments.subList(1, segments.size());
	}

	public String toPathString() {
		return String.join(SEPARATOR, segments);
	}

	public String toDisplayPath() {
		if (isRoot()) {
			return "/";
		}
		return SEPARATOR + String.join(SEPARATOR, segments.subList(1, segments.size()));
	}

	public List<FolderPath> getAncestors() {
		List<FolderPath> ancestors = new ArrayList<>();
		FolderPath current = this;
		while (!current.isRoot()) {
			current = current.getParent();
			ancestors.add(0, current);
		}
		return ancestors;
	}

	public List<FolderPath> getPathChain() {
		List<FolderPath> chain = new ArrayList<>();
		FolderPath current = root();
		chain.add(current);
		for (int i = 1; i < segments.size(); i++) {
			current = current.resolve(segments.get(i));
			chain.add(current);
		}
		return chain;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FolderPath that = (FolderPath) o;
		return segments.equals(that.segments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(segments);
	}

	@Override
	public String toString() {
		return toDisplayPath();
	}
}
