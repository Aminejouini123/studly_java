/**
 * Compatibility shim for legacy/stale bytecode or FXML reflection that refers to a
 * `Course` class in the default package.
 *
 * The real model lives in {@code models.Course}.
 */
public class Course extends models.Course {
    public Course() {
        super();
    }
}

