package com.tyj.campuscircle.ai;

import com.tyj.campuscircle.post.PostListItem;
import com.tyj.campuscircle.post.PostMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PostRetrievalService implements PostRetriever {

    private static final int MAX_QUERY_TERMS = 5;

    private final PostMapper postMapper;

    public PostRetrievalService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    public List<RetrievedPost> retrieve(RetrievalQuery query) {
        if (query.allowedSchoolIds().isEmpty()) {
            return List.of();
        }

        Map<Long, RetrievedPost> postsById = new LinkedHashMap<>();
        for (String keyword : extractKeywords(query.question())) {
            List<PostListItem> posts = postMapper.findPostsBySchoolIdsAndKeyword(
                    query.allowedSchoolIds(), keyword, query.limit());
            for (PostListItem post : posts) {
                postsById.putIfAbsent(post.id(), RetrievedPost.from(post));
                if (postsById.size() >= query.limit()) {
                    return new ArrayList<>(postsById.values());
                }
            }
        }
        return new ArrayList<>(postsById.values());
    }

    private List<String> extractKeywords(String question) {
        String compact = question == null ? "" : question.trim().toLowerCase()
                .replaceAll("[^\\p{IsHan}a-z0-9]", "");
        for (String phrase : List.of("请问", "附近", "最近", "有哪些", "什么", "哪里", "怎么", "如何", "适合", "可以", "一下", "的", "吗", "呢", "吧", "地方")) {
            compact = compact.replace(phrase, "");
        }
        if (compact.length() < 2) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        keywords.add(compact);
        for (int length = Math.min(4, compact.length()); length >= 2; length--) {
            for (int start = 0; start + length <= compact.length(); start++) {
                keywords.add(compact.substring(start, start + length));
                if (keywords.size() >= MAX_QUERY_TERMS) {
                    return new ArrayList<>(keywords);
                }
            }
        }
        return new ArrayList<>(keywords);
    }
}
