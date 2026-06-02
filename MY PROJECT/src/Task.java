import java.time.LocalDate;
class Task {
    int num;
    int id;
    String title;
    String category;
    int priority;
    LocalDate deadline;
    String status;

    public Task(int id, String title, String category, int priority, LocalDate deadline) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.deadline = deadline;
        this.status = "Pending";
    }
}
