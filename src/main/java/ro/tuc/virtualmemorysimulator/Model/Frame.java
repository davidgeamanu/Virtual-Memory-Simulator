package ro.tuc.virtualmemorysimulator.Model;

public class Frame {
    private final int id;
    private Page currentPage;

    public Frame (int id) {
        this.id = id;
        this.currentPage = null;
    }

    public int getFrameId() {
        return id;
    }

    public Page getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Page page) {
        this.currentPage = page;
    }

    public boolean isFree() {
        return currentPage == null;
    }

}
