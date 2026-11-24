package system.com.lostfound;

public class HistoryEvent {

    private final int year;
    private final String title;
    private final String description;
    private final String imagePath;

    public HistoryEvent(int year, String title,
                        String description, String imagePath) {

        this.year = year;
        this.title = title;
        this.description = description;
        this.imagePath = imagePath;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public String toString() {
        return String.valueOf(year);
    }
}