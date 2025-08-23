package io.nexstudios.nexus.bukkit.inv.pagination;

public final class NexPageSource {
    private final int totalItems;
    private final int pageSize;

    public NexPageSource(int totalItems, int pageSize) {
        this.totalItems = Math.max(0, totalItems);
        this.pageSize = Math.max(1, pageSize);
    }

    public int totalPages() {
        if (totalItems == 0) return 1;
        return (totalItems + pageSize - 1) / pageSize;
    }

    public int clampPage(int pageIndex) {
        if (pageIndex < 0) return 0;
        int last = totalPages() - 1;
        return Math.min(pageIndex, last);
    }

    public int itemsOnPage(int pageIndex) {
        pageIndex = clampPage(pageIndex);
        if (totalItems == 0) return 0;
        int start = pageIndex * pageSize;
        int end = Math.min(totalItems, start + pageSize);
        return Math.max(0, end - start);
    }

    public int pageSize() {
        return pageSize;
    }
}

