package com.tyj.campuscircle.school;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@Profile("!redis")
public class NoOpNearbySchoolCacheStore implements NearbySchoolCacheStore {

    @Override
    public List<SchoolResponse> listNearbySchools(Long schoolId, double radiusKm, Supplier<List<SchoolResponse>> dbLoader) {
        return dbLoader.get();
    }
}
