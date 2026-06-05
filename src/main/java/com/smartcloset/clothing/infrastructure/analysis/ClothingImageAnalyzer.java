package com.smartcloset.clothing.infrastructure.analysis;

/**
 * 옷 사진을 기존 옷 등록 DTO 후보로 변환하는 provider boundary다.
 *
 * <p>MVP10에서 AI 호출은 이 interface 뒤에만 위치한다. Application service는 provider 종류를
 * 알지 못하고, 분석 결과도 DB 저장이나 추천 계산으로 전달하지 않는다.</p>
 */
public interface ClothingImageAnalyzer {

    /**
     * 현재 설정에서 provider 호출을 시도할 수 있는지 반환한다.
     *
     * <p>기능 비활성이나 API key 미설정 상태는 분석 quota를 소모하기 전에 차단한다.</p>
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 검증이 끝난 이미지 bytes를 분석해 저장 전 후보값과 field confidence를 반환한다.
     *
     * @param image 저장하지 않고 provider에만 전달할 분석 이미지
     * @return HTTP DTO와 분리된 분석 결과 모델
     * @throws ClothingImageAnalysisDisabledException 기능 비활성 또는 provider 미설정 상태
     * @throws ClothingImageAnalysisUnavailableException provider 호출 실패, timeout, malformed output
     */
    ClothingAnalysisResult analyze(ClothingAnalysisImage image);
}
