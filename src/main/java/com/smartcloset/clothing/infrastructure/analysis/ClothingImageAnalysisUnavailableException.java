package com.smartcloset.clothing.infrastructure.analysis;

public class ClothingImageAnalysisUnavailableException extends RuntimeException {

    public ClothingImageAnalysisUnavailableException(String message) {
        super(message);
    }

    public ClothingImageAnalysisUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
