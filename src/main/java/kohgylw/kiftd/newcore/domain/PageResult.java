package kohgylw.kiftd.newcore.domain;

import java.util.List;

public class PageResult<T> {

	private final List<T> list;
	private final long total;
	private final int pageNum;
	private final int pageSize;

	private PageResult(Builder<T> builder) {
		this.list = builder.list;
		this.total = builder.total;
		this.pageNum = builder.pageNum;
		this.pageSize = builder.pageSize;
	}

	public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
		return new Builder<T>()
				.list(list)
				.total(total)
				.pageNum(pageNum)
				.pageSize(pageSize)
				.build();
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public List<T> getList() {
		return list;
	}

	public long getTotal() {
		return total;
	}

	public int getPageNum() {
		return pageNum;
	}

	public int getPageSize() {
		return pageSize;
	}

	public static class Builder<T> {

		private List<T> list;
		private long total;
		private int pageNum;
		private int pageSize;

		private Builder() {
		}

		public Builder<T> list(List<T> list) {
			this.list = list;
			return this;
		}

		public Builder<T> total(long total) {
			this.total = total;
			return this;
		}

		public Builder<T> pageNum(int pageNum) {
			this.pageNum = pageNum;
			return this;
		}

		public Builder<T> pageSize(int pageSize) {
			this.pageSize = pageSize;
			return this;
		}

		public PageResult<T> build() {
			return new PageResult<>(this);
		}
	}
}
