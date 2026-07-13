package com.tyj.campuscircle.notice;

import com.tyj.campuscircle.common.ApiResponse;
import com.tyj.campuscircle.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NoticeResponse>> listNotices(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) Integer readStatus) {
        return ApiResponse.success(noticeService.listNotices(authorization, page, size, readStatus));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNoticeCountResponse> countUnreadNotices(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(noticeService.countUnreadNotices(authorization));
    }

    @PutMapping("/{noticeId}/read")
    public ApiResponse<Boolean> markNoticeRead(
            @PathVariable Long noticeId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        noticeService.markNoticeRead(noticeId, authorization);
        return ApiResponse.success(true);
    }

    @PutMapping("/read-all")
    public ApiResponse<UpdateNoticeCountResponse> markAllNoticesRead(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(noticeService.markAllNoticesRead(authorization));
    }
}
