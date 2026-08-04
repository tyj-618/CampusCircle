package com.tyj.campuscircle.ai;

import java.util.List;

/**
 * Retrieves posts that are already limited to the caller's permitted campus scope.
 */
public interface PostRetriever {

    List<RetrievedPost> retrieve(RetrievalQuery query);
}
