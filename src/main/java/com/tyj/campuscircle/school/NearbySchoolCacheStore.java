package com.tyj.campuscircle.school;

import java.util.List;
import java.util.function.Supplier;

public interface NearbySchoolCacheStore {

    List<SchoolResponse> listNearbySchools(Long schoolId, double radiusKm, Supplier<List<SchoolResponse>> dbLoader);
}
