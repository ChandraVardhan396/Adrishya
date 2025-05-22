package model;

public class InpaintResult {
    private int id;
    private String inputImagePath;
    private String maskImagePath;
    private String outputImagePath;

    public InpaintResult(String inputImagePath, String maskImagePath, String outputImagePath) {
        this.inputImagePath = inputImagePath;
        this.maskImagePath = maskImagePath;
        this.outputImagePath = outputImagePath;
    }

    // Getter methods
    public String getInputImagePath() {
        return inputImagePath;
    }

    public String getMaskImagePath() {
        return maskImagePath;
    }

    public String getOutputImagePath() {
        return outputImagePath;
    }

    // (Optional) Setter methods and ID getter/setter if needed
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
