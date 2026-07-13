package com.tyj.campuscircle.school;

import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.common.entity.SchoolEntity;
import com.tyj.campuscircle.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Service
public class SchoolService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_RADIUS_KM = 50.0;

    private final SchoolMapper schoolMapper;

    public SchoolService(SchoolMapper schoolMapper) {
        this.schoolMapper = schoolMapper;
    }

    public List<SchoolResponse> searchSchools(String keyword, int limit) {
        List<String> keywordCandidates = keywordCandidates(keyword == null ? "" : keyword.trim());
        String cleanedKeyword = keywordCandidates.get(0);
        if (cleanedKeyword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "搜索关键词不能为空");
        }

        return schoolMapper.findEnabledSchools().stream()
                .filter(school -> keywordCandidates.stream().anyMatch(candidate -> containsKeyword(school, candidate)))
                .limit(limit)
                .map(SchoolResponse::from)
                .toList();
    }

    public List<SchoolResponse> listNearbySchools(Long schoolId, double radiusKm) {
        SchoolEntity center = findEnabledSchool(schoolId);
        validateRadius(radiusKm);

        return schoolMapper.findEnabledByCity(center.getCity()).stream()
                .map(school -> SchoolResponse.from(school, distanceKm(center, school)))
                .filter(response -> response.distanceKm() <= radiusKm)
                .sorted(Comparator.comparing(SchoolResponse::distanceKm))
                .toList();
    }

    public List<Long> listNearbySchoolIds(Long schoolId, double radiusKm) {
        return listNearbySchools(schoolId, radiusKm).stream()
                .map(SchoolResponse::id)
                .toList();
    }

    public SchoolEntity findEnabledSchool(Long schoolId) {
        if (schoolId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学校不能为空");
        }
        return schoolMapper.findEnabledById(schoolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学校不存在或已禁用"));
    }

    private void validateRadius(double radiusKm) {
        if (radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "查看范围必须在 0 到 50 公里之间");
        }
    }

    private double distanceKm(SchoolEntity source, SchoolEntity target) {
        double lat1 = toDouble(source.getLatitude());
        double lng1 = toDouble(source.getLongitude());
        double lat2 = toDouble(target.getLatitude());
        double lng2 = toDouble(target.getLongitude());

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }

    private boolean containsKeyword(SchoolEntity school, String keyword) {
        return school.getName().contains(keyword)
                || school.getCity().contains(keyword)
                || school.getProvince().contains(keyword);
    }

    private List<String> keywordCandidates(String keyword) {
        String decodedKeyword = keyword.contains("%")
                ? URLDecoder.decode(keyword, StandardCharsets.UTF_8)
                : keyword;
        String repairedKeyword = new String(keyword.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return List.of(keyword, decodedKeyword, repairedKeyword).stream()
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .distinct()
                .toList();
    }

    private double toDouble(BigDecimal value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "学校坐标缺失");
        }
        return value.doubleValue();
    }
}
