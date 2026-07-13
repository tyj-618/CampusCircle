package com.tyj.campuscircle.post;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@Profile("!redis")
public class NoOpHotPostRankStore implements HotPostRankStore {

    @Override
    public List<PostHotItemResponse> listHotPosts(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> dbLoader) {
        return dbLoader.get();
    }
}
