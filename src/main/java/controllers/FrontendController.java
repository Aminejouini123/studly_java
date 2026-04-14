package controllers;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import controllers.courses.BaseCourseController;

public class FrontendController extends BaseCourseController {

    @FXML
    public void initialize() {
        // Dashboard specific initialization if any
    }

    // Navigation methods are inherited from BaseCourseController
    // but we can override or add dashboard-specific ones here
}
