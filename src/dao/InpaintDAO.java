package dao;

import model.InpaintResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InpaintDAO {
    public void saveResult(InpaintResult result) {
        String sql = "INSERT INTO inpaint_results (input_image_path, mask_image, output_image) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.getInputImagePath());
            stmt.setString(2, result.getMaskImagePath());
            stmt.setString(3, result.getOutputImagePath());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
