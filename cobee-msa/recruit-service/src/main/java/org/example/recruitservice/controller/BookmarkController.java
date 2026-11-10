package org.example.recruitservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.recruitservice.dto.bookmark.BookmarkListResponse;
import org.example.recruitservice.dto.bookmark.BookmarkResponse;
import org.example.recruitservice.service.BookmarkService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmark")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{postId}")
    public ApiResponse<BookmarkResponse> addBookmark(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @PathVariable(name = "postId") Long postId
    ) {
        BookmarkResponse bookmarkResponse = bookmarkService.addBookmark(memberId, postId);
        return ApiResponse.success("bookmark 추가 완료", "ADD_BOOKMARK", bookmarkResponse);
    }

    @GetMapping("")
    public ApiResponse<BookmarkListResponse> getBookmarkList(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId
    ){
        BookmarkListResponse bookmarkList = bookmarkService.getBookmarkList(memberId);
        return ApiResponse.success("bookmark 목록 반환 완료", "GET_BOOKMARK", bookmarkList);
    }

    @DeleteMapping("/{bookmarkId}")
    public ApiResponse<BookmarkResponse> deleteBookmark(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @PathVariable(name = "bookmarkId") Long bookmarkId
    ) {
        BookmarkResponse bookmarkResponse = bookmarkService.deleteBookmark(bookmarkId, memberId);
        return ApiResponse.success("bookmark 삭제", "DELETE_BOOKMARK", bookmarkResponse);
    }

    @DeleteMapping("/all")
    public ApiResponse<String> deleteAllBookmark(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId
    ){
        int bookmarkCount = bookmarkService.deleteAllBookmark(memberId);
        return ApiResponse.success("bookmark 전체 삭제", "DELETE_ALL_BOOKMARK", "bookmark 해제 개수 : " + bookmarkCount);
    }
}
